/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki;

import java.io.*;
import java.util.*;
import java.text.*;

import org.apache.log4j.Category;
import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;

import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.plugin.PluginException;

/**
 *  Handles conversion from Wiki format into fully featured HTML.
 *  This is where all the magic happens.  It is CRITICAL that this
 *  class is tested, or all Wikis might die horribly.
 *  <P>
 *  The output of the HTML has not yet been validated against
 *  the HTML DTD.  However, it is very simple.
 *
 *  @author Janne Jalkanen
 */
// FIXME: Class still has problems with {{{: all conversion on that line where the {{{
//        appears is done, but after that, conversion is not done.  The only real solution
//        is to move away from a line-based system into a pure stream-based system.
public class TranslatorReader extends Reader
{
    public  static final int              READ          = 0;
    public  static final int              EDIT          = 1;
    private static final int              EMPTY         = 2;  // Empty message
    private static final int              LOCAL         = 3;
    private static final int              LOCALREF      = 4;
    private static final int              IMAGE         = 5;
    private static final int              EXTERNAL      = 6;
    private static final int              INTERWIKI     = 7;
    private static final int              IMAGELINK     = 8;
    private static final int              IMAGEWIKILINK = 9;

    private BufferedReader m_in;

    private StringReader   m_data = new StringReader("");

    private static Category log = Category.getInstance( TranslatorReader.class );

    private boolean        m_iscode       = false;
    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_isTypedText  = false;
    private boolean        m_istable      = false;
    private int            m_listlevel    = 0;
    private int            m_numlistlevel = 0;

    private WikiEngine     m_engine;
    private WikiContext    m_context;
    
    /** Optionally stores internal wikilinks */
    private ArrayList      m_localLinkMutatorChain    = new ArrayList();
    private ArrayList      m_externalLinkMutatorChain = new ArrayList();

    /** Keeps image regexp Patterns */
    private ArrayList      m_inlineImagePatterns;

    private PatternMatcher m_inlineMatcher = new Perl5Matcher();

    private ArrayList      m_linkMutators = new ArrayList();

    /**
     *  This property defines the inline image pattern.  It's current value
     *  is jspwiki.translatorReader.inlinePattern
     */
    public static final String     PROP_INLINEIMAGEPTRN  = "jspwiki.translatorReader.inlinePattern";

    /** If true, consider CamelCase hyperlinks as well. */
    public static final String     PROP_CAMELCASELINKS   = "jspwiki.translatorReader.camelCaseLinks";

    /** If true, all hyperlinks are translated as well, regardless whether they
        are surrounded by brackets. */
    public static final String     PROP_PLAINURIS        = "jspwiki.translatorReader.plainUris";

    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, consider URIs that have no brackets as well. */
    private boolean                m_plainUris           = false;

    private PatternMatcher         m_matcher  = new Perl5Matcher();
    private PatternCompiler        m_compiler = new Perl5Compiler();
    private Pattern                m_camelCasePtrn;

    /**
     *  The default inlining pattern.  Currently "*.png"
     */
    public static final String     DEFAULT_INLINEPATTERN = "*.png";

    /**
     *  @param engine The WikiEngine this reader is attached to.  Is
     * used to figure out of a page exits.
     */

    // FIXME: TranslatorReaders should be pooled for better performance.
    public TranslatorReader( WikiContext context, Reader in )
    {
        PatternCompiler compiler         = new GlobCompiler();
        ArrayList       compiledpatterns = new ArrayList();

        m_in     = new BufferedReader( in );
        m_engine = context.getEngine();
        m_context = context;
        
        Collection ptrns = getImagePatterns( m_engine );

        //
        //  Make them into Regexp Patterns.  Unknown patterns
        //  are ignored.
        //
        for( Iterator i = ptrns.iterator(); i.hasNext(); )
        {
            try
            {       
                compiledpatterns.add( compiler.compile( (String)i.next() ) );
            }
            catch( MalformedPatternException e )
            {
                log.error("Malformed pattern in properties: ", e );
            }
        }

        m_inlineImagePatterns = compiledpatterns;

        try
        {
            m_camelCasePtrn       = m_compiler.compile( "(^|\\W)([A-Z][a-z]+([A-Z][a-z]+)+)" );
        }
        catch( MalformedPatternException e )
        {
            log.fatal("Internal error: Someone put in a faulty pattern.",e);
            throw new InternalWikiException("Faulty camelcasepattern in TranslatorReader");
        }

        //
        //  Set the properties.
        //
        Properties props      = m_engine.getWikiProperties();
        m_camelCaseLinks      = "true".equals( props.getProperty( PROP_CAMELCASELINKS, "false" ) );
        m_plainUris           = "true".equals( props.getProperty( PROP_PLAINURIS, "false" ) );
    }

    /**
     *  Adds a hook for processing link texts.  This hook is called
     *  when the link text is written into the output stream, and
     *  you may use it to modify the text.  It does not affect the
     *  actual link, only the user-visible text.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addLinkTransmutator( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_linkMutators.add( mutator );
        }
    }

    /**
     *  Adds a hook for processing local links.  The engine
     *  transforms both non-existing and existing page links.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addLocalLinkHook( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_localLinkMutatorChain.add( mutator );
        }
    }

    /**
     *  Adds a hook for processing external links.  This includes
     *  all http:// ftp://, etc. links, including inlined images.
     *
     *  @param mutator The hook to call.  Null is safe.
     */
    public void addExternalLinkHook( StringTransmutator mutator )
    {
        if( mutator != null )
        {
            m_externalLinkMutatorChain.add( mutator );
        }
    }

    /**
     *  Figure out which image suffixes should be inlined.
     *  @return Collection of Strings with patterns.
     */

    protected static Collection getImagePatterns( WikiEngine engine )
    {
        Properties props    = engine.getWikiProperties();
        ArrayList  ptrnlist = new ArrayList();

        for( Enumeration e = props.propertyNames(); e.hasMoreElements(); )
        {
            String name = (String) e.nextElement();

            if( name.startsWith( PROP_INLINEIMAGEPTRN ) )
            {
                String ptrn = props.getProperty( name );

                ptrnlist.add( ptrn );
            }
        }

        if( ptrnlist.size() == 0 )
        {
            ptrnlist.add( DEFAULT_INLINEPATTERN );
        }

        return ptrnlist;
    }

    /**
     *  Returns link name, if it exists; otherwise it returns null.
     */
    private String linkExists( String page )
    {
        return m_engine.getFinalPageName( page );
    }

    /**
     *  Calls a transmutator chain.
     *
     *  @param list Chain to call
     *  @param text Text that should be passed to the mutate() method
     *              of each of the mutators in the chain.
     *  @return The result of the mutation.
     */

    private String callMutatorChain( Collection list, String text )
    {
        if( list == null || list.size() == 0 )
        {
            return text;
        }

        for( Iterator i = list.iterator(); i.hasNext(); )
        {
            StringTransmutator m = (StringTransmutator) i.next();

            text = m.mutate( m_context, text );
        }

        return text;
    }

    /**
     *  Write a HTMLized link depending on its type.
     *  The link mutator chain is processed.
     *
     *  @param type Type of the link.
     *  @param link The actual link.
     *  @param text The user-visible text for the link.
     */
    public String makeLink( int type, String link, String text )
    {
        String result;

        if( text == null ) text = link;

        // Make sure we make a link name that can be accepted
        // as a valid URL.

        String encodedlink = m_engine.encodeName( link );

        if( encodedlink.length() == 0 )
        {
            type = EMPTY;
        }

        text = callMutatorChain( m_linkMutators, text );

        switch(type)
        {
          case READ:
            result = "<A CLASS=\"wikipage\" HREF=\""+m_engine.getBaseURL()+"Wiki.jsp?page="+encodedlink+"\">"+text+"</A>";
            break;

          case EDIT:
            result = "<U>"+text+"</U><A HREF=\""+m_engine.getBaseURL()+"Edit.jsp?page="+encodedlink+"\">?</A>";
            break;

          case EMPTY:
            result = "<U>"+text+"</U>";
            break;

            //
            //  These two are for local references - footnotes and 
            //  references to footnotes.
            //  We embed the page name (or whatever WikiContext gives us)
            //  to make sure the links are unique across Wiki.
            //
          case LOCALREF:
            result = "<A CLASS=\"footnoteref\" HREF=\"#ref-"+
                m_context.getPage().getName()+"-"+
                link+"\">["+text+"]</A>";
            break;

          case LOCAL:
            result = "<A CLASS=\"footnote\" NAME=\"ref-"+
                m_context.getPage().getName()+"-"+
                link.substring(1)+"\">["+text+"]</A>";
            break;

            //
            //  With the image, external and interwiki types we need to
            //  make sure nobody can put in Javascript or something else
            //  annoying into the links themselves.  We do this by preventing
            //  a haxor from stopping the link name short with quotes in 
            //  fillBuffer().
            //
          case IMAGE:
            result = "<IMG CLASS=\"inline\" SRC=\""+link+"\" ALT=\""+text+"\">";
            break;

          case IMAGELINK:
            result = "<A HREF=\""+text+"\"><IMG CLASS=\"inline\" SRC=\""+link+"\"></A>";
            break;

          case IMAGEWIKILINK:
            String pagelink = m_engine.getBaseURL()+"Wiki.jsp?page="+text;
            result = "<A CLASS=\"wikipage\" HREF=\""+pagelink+"\"><IMG CLASS=\"inline\" SRC=\""+link+"\" ALT=\""+text+"\"></A>";
            break;

          case EXTERNAL:
            result = "<A CLASS=\"external\" HREF=\""+link+"\">"+text+"</A>";
            break;

          case INTERWIKI:
            result = "<A CLASS=\"interwiki\" HREF=\""+link+"\">"+text+"</A>";
            break;

          default:
            result = "";
            break;
        }

        return result;
    }


    /**
     *  Cleans a Wiki name.
     *  <P>
     *  [ This is a link ] -&gt; ThisIsALink
     *
     *  @since 2.0
     */
    public static String cleanLink( String link )
    {
        StringBuffer clean = new StringBuffer();

        //
        //  Compress away all whitespace and capitalize
        //  all words in between.
        //

        StringTokenizer st = new StringTokenizer( link, " -" );

        while( st.hasMoreTokens() )
        {
            StringBuffer component = new StringBuffer(st.nextToken());

            component.setCharAt(0, Character.toUpperCase( component.charAt(0) ) );

            clean.append( component );
        }

        //
        //  Remove non-alphanumeric characters that should not
        //  be put inside WikiNames.  Note that all valid
        //  Unicode letters are considered okay for WikiNames.
        //  It is the problem of the WikiPageProvider to take
        //  care of actually storing that information.
        //

        for( int i = 0; i < clean.length(); i++ )
        {
            if( !(Character.isLetterOrDigit(clean.charAt(i)) ||
                  clean.charAt(i) == '_' ||
                  clean.charAt(i) == '.') )
            {
                clean.deleteCharAt(i);
                --i; // We just shortened this buffer.
            }
        }

        return clean.toString();
    }

    /**
     *  Figures out if a link is an off-site link.  This recognizes
     *  the most common protocols by checking how it starts.
     */
    private boolean isExternalLink( String link )
    {
        return link.startsWith("http:") || link.startsWith("ftp:") ||
            link.startsWith("https:") || link.startsWith("mailto:") ||
            link.startsWith("news:") || link.startsWith("file:");
    }

    /**
     *  Matches the given link to the list of image name patterns
     *  to determine whether it should be treated as an inline image
     *  or not.
     */
    private boolean isImageLink( String link )
    {
        for( Iterator i = m_inlineImagePatterns.iterator(); i.hasNext(); )
        {
            if( m_inlineMatcher.matches( link, (Pattern) i.next() ) )
                return true;
        }

        return false;
    }

    /**
     *  Returns true, if the argument contains a number, otherwise false.
     *  In a quick test this is roughly the same speed as Integer.parseInt()
     *  if the argument is a number, and roughly ten times the speed, if
     *  the argument is NOT a number.
     */

    private boolean isNumber( String s )
    {
        if( s == null ) return false;

        if( s.length() > 1 && s.charAt(0) == '-' )
            s = s.substring(1);

        for( int i = 0; i < s.length(); i++ )
        {
            if( !Character.isDigit(s.charAt(i)) )
                return false;
        }

        return true;
    }

    /**
     *  Attempts to set traditional, CamelCase style WikiLinks.
     */
    private String setCamelCaseLinks( String line )
    {
        PatternMatcherInput input;

        input = new PatternMatcherInput( line );

        while( m_matcher.contains( input, m_camelCasePtrn ) )
        {
            //
            //  Weed out links that will be matched later on.
            //

            MatchResult res = m_matcher.getMatch();
            int lastOpen  = line.substring(0,res.endOffset(2)).lastIndexOf('[');
            int lastClose = line.substring(0,res.endOffset(2)).lastIndexOf(']');

            if( (lastOpen < lastClose && lastOpen >= 0) || // Links closed ok
                lastOpen < 0 )                             // No links yet
            {
                int start = res.beginOffset(2);
                int end   = res.endOffset(2);

                String link = res.group(2);
                String matchedLink;

                // System.out.println("LINE="+line);
                // System.out.println("  Replacing: "+link);
                // System.out.println("  open="+lastOpen+", close="+lastClose);

                callMutatorChain( m_localLinkMutatorChain, link );

                if( (matchedLink = linkExists( link )) != null )
                {
                    link = makeLink( READ, matchedLink, link );
                }
                else
                {
                    link = makeLink( EDIT, link, link );
                }

                line = TextUtil.replaceString( line, 
                                               start, 
                                               end, 
                                               link );

                input.setInput( line );
                input.setCurrentOffset( start+link.length() );
            } // if()
        } // while

        return line;
    }

    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    /*
      OBSOLETE
    private String setHyperLinksOld( String line )
    {
        int start, end = 0;
        
        while( ( start = line.indexOf('[', end) ) != -1 )
        {
            // Words starting with multiple [[s are not hyperlinks.
            if( line.charAt( start+1 ) == '[' )
            {
                for( end = start; end < line.length() && line.charAt(end) == '['; end++ );
                line = TextUtil.replaceString( line, start, start+1, "" );
                continue;
            }

            end = line.indexOf( ']', start );

            if( end != -1 )
            {
                // Everything between these two is a link

                String link = line.substring( start+1, end );
                String reallink;
                int cutpoint;

                if( (cutpoint = link.indexOf('|')) != -1 )
                {                    
                    reallink = link.substring( cutpoint+1 ).trim();
                    link = link.substring( 0, cutpoint );
                }
                else
                {
                    reallink = link.trim();
                }

                int interwikipoint = -1;

                //
                //  Yes, we now have the components separated.
                //  link     = the text the link should have
                //  reallink = the url or page name.
                //
                //  In many cases these are the same.  [link|reallink].
                //  
                if( PluginManager.isPluginLink( link ) )
                {
                    String included;
                    try
                    {
                        included = m_engine.getPluginManager().execute( m_context, link );
                    }
                    catch( PluginException e )
                    {
                        log.error( "Failed to insert plugin", e );
                        log.error( "Root cause:",e.getRootThrowable() );
                        included = "<FONT COLOR=\"#FF0000\">Plugin insertion failed: "+e.getMessage()+"</FONT>";
                    }
                            
                    line = TextUtil.replaceString( line, start, end+1,
                                                   included );
                }                
                else if( VariableManager.isVariableLink( link ) )
                {
                    String value;

                    try
                    {
                        value = m_engine.getVariableManager().parseAndGetValue( m_context, link );
                    }
                    catch( NoSuchVariableException e )
                    {
                        value = "<FONT COLOR=\"#FF0000\">"+e.getMessage()+"</FONT>";
                    }
                    catch( IllegalArgumentException e )
                    {
                        value = "<FONT COLOR=\"#FF0000\">"+e.getMessage()+"</FONT>";
                    }

                    line = TextUtil.replaceString( line, start, end+1,
                                                   value );
                }
                else if( isExternalLink( reallink ) )
                {
                    // It's an external link, out of this Wiki

                    callMutatorChain( m_externalLinkMutatorChain, reallink );

                    if( isImageLink( reallink ) )
                    {
                        //
                        // Image links are handled differently:
                        // 1. If the text is a WikiName of an existing page,
                        //    it gets linked.
                        // 2. If the text is an external link, then it is inlined.  
                        // 3. Otherwise it becomes an ALT text.
                        //

                        String possiblePage = cleanLink( link );
                        String matchedLink;

                        if( isExternalLink( link ) && (cutpoint != -1) )
                        {
                            line = TextUtil.replaceString( line, start, end+1,
                                                           makeLink( IMAGELINK, reallink, link ) );
                        }
                        else if( (matchedLink = linkExists( possiblePage )) != null &&
                                 (cutpoint != -1) )
                        {
                            // System.out.println("Orig="+link+", Matched: "+matchedLink);
                            callMutatorChain( m_localLinkMutatorChain, possiblePage );

                            line = TextUtil.replaceString( line, start, end+1,
                                                           makeLink( IMAGEWIKILINK, reallink, link ) );
                        }
                        else
                        {
                            line = TextUtil.replaceString( line, start, end+1,
                                                           makeLink( IMAGE, reallink, link ) );
                        }
                    }
                    else
                    {
                        line = TextUtil.replaceString( line, start, end+1, 
                                                       makeLink( EXTERNAL, reallink, link ) );
                    }
                }
                else if( (interwikipoint = reallink.indexOf(":")) != -1 )
                {
                    // It's an interwiki link
                    // InterWiki links also get added to external link chain
                    // after the links have been resolved.

                    // FIXME: There is an interesting issue here:  We probably should
                    //        URLEncode the wikiPage, but we can't since some of the
                    //        Wikis use slashes (/), which won't survive URLEncoding.
                    //        Besides, we don't know which character set the other Wiki
                    //        is using, so you'll have to write the entire name as it appears
                    //        in the URL.  Bugger.

                    String extWiki = reallink.substring( 0, interwikipoint );
                    String wikiPage = reallink.substring( interwikipoint+1 );

                    String urlReference = m_engine.getInterWikiURL( extWiki );

                    if( urlReference != null )
                    {
                        urlReference = TextUtil.replaceString( urlReference, "%s", wikiPage );
                        callMutatorChain( m_externalLinkMutatorChain, urlReference );

                        line = TextUtil.replaceString( line, start, end+1,
                                                       makeLink( INTERWIKI, urlReference, link ) );
                    }
                    else
                    {
                        line = TextUtil.replaceString( line, start, end+1, 
                                                       link+" <FONT COLOR=\"#FF0000\">(No InterWiki reference defined in properties for Wiki called '"+extWiki+"'!)</FONT>");
                    }
                }
                else if( reallink.startsWith("#") )
                {
                    // It defines a local footnote
                    line = TextUtil.replaceString( line, start, end+1, 
                                                   makeLink( LOCAL, reallink, link ) );
                }
                else if( isNumber( reallink ) )
                {
                    // It defines a reference to a local footnote
                    line = TextUtil.replaceString( line, start, end+1, 
                                                   makeLink( LOCALREF, reallink, link ) );
                }
                else
                {
                    // It's an internal Wiki link
                    reallink = cleanLink( reallink );

                    callMutatorChain( m_localLinkMutatorChain, reallink );

                    String matchedLink;
                    if( (matchedLink = linkExists( reallink )) != null )
                    {
                        line = TextUtil.replaceString( line, start, end+1, 
                                                       makeLink( READ, matchedLink, link ) );
                    }
                    else
                    {
                        line = TextUtil.replaceString( line, start, end+1, makeLink( EDIT, reallink, link ) );
                    }
                }
            }
            else
            {
                log.error("Unterminated link");
                break;
            }
        }

        return line;
    }
    */
    
    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    private String setHyperLinks( String line )
    {
        int start, end = 0;
        StringBuffer sb = new StringBuffer();

        int idx = 0;
        while( idx < line.length() )
        {
            start = line.indexOf('[', end);

            if( start < 0 )
            {
                // No more hyperlinks.
                sb.append( line.substring( idx ) );
                break;
            }
            
            sb.append( line.substring( idx, start ) );
            
            // Words starting with multiple [[s are not hyperlinks.
            if( line.charAt( start+1 ) == '[' )
            {
                for( end = start; end < line.length() && line.charAt(end) == '['; end++ );
                sb.append( line.substring( start, end ) );
                
                continue;
            }

            end = line.indexOf( ']', start );

            if( end != -1 )
            {
                // Everything between these two is a link

                String link = line.substring( start+1, end );
                String reallink;
                int cutpoint;

                if( (cutpoint = link.indexOf('|')) != -1 )
                {                    
                    reallink = link.substring( cutpoint+1 ).trim();
                    link = link.substring( 0, cutpoint );
                }
                else
                {
                    reallink = link.trim();
                }

                int interwikipoint = -1;

                //
                //  Yes, we now have the components separated.
                //  link     = the text the link should have
                //  reallink = the url or page name.
                //
                //  In many cases these are the same.  [link|reallink].
                //  
                if( PluginManager.isPluginLink( link ) )
                {
                    String included;
                    try
                    {
                        included = m_engine.getPluginManager().execute( m_context, link );
                    }
                    catch( PluginException e )
                    {
                        log.error( "Failed to insert plugin", e );
                        log.error( "Root cause:",e.getRootThrowable() );
                        included = "<FONT COLOR=\"#FF0000\">Plugin insertion failed: "+e.getMessage()+"</FONT>";
                    }
                            
                    sb.append( included );
                }                
                else if( VariableManager.isVariableLink( link ) )
                {
                    String value;

                    try
                    {
                        value = m_engine.getVariableManager().parseAndGetValue( m_context, link );
                    }
                    catch( NoSuchVariableException e )
                    {
                        value = "<FONT COLOR=\"#FF0000\">"+e.getMessage()+"</FONT>";
                    }
                    catch( IllegalArgumentException e )
                    {
                        value = "<FONT COLOR=\"#FF0000\">"+e.getMessage()+"</FONT>";
                    }

                    sb.append( value );
                }
                else if( isExternalLink( reallink ) )
                {
                    // It's an external link, out of this Wiki

                    callMutatorChain( m_externalLinkMutatorChain, reallink );

                    if( isImageLink( reallink ) )
                    {
                        //
                        // Image links are handled differently:
                        // 1. If the text is a WikiName of an existing page,
                        //    it gets linked.
                        // 2. If the text is an external link, then it is inlined.  
                        // 3. Otherwise it becomes an ALT text.
                        //

                        String possiblePage = cleanLink( link );
                        String matchedLink;

                        if( isExternalLink( link ) && (cutpoint != -1) )
                        {
                            sb.append( makeLink( IMAGELINK, reallink, link ) );
                        }
                        else if( (matchedLink = linkExists( possiblePage )) != null &&
                                 (cutpoint != -1) )
                        {
                            // System.out.println("Orig="+link+", Matched: "+matchedLink);
                            callMutatorChain( m_localLinkMutatorChain, possiblePage );

                            sb.append( makeLink( IMAGEWIKILINK, reallink, link ) );
                        }
                        else
                        {
                            sb.append( makeLink( IMAGE, reallink, link ) );
                        }
                    }
                    else
                    {
                        sb.append( makeLink( EXTERNAL, reallink, link ) );
                    }
                }
                else if( (interwikipoint = reallink.indexOf(":")) != -1 )
                {
                    // It's an interwiki link
                    // InterWiki links also get added to external link chain
                    // after the links have been resolved.

                    // FIXME: There is an interesting issue here:  We probably should
                    //        URLEncode the wikiPage, but we can't since some of the
                    //        Wikis use slashes (/), which won't survive URLEncoding.
                    //        Besides, we don't know which character set the other Wiki
                    //        is using, so you'll have to write the entire name as it appears
                    //        in the URL.  Bugger.

                    String extWiki = reallink.substring( 0, interwikipoint );
                    String wikiPage = reallink.substring( interwikipoint+1 );

                    String urlReference = m_engine.getInterWikiURL( extWiki );

                    if( urlReference != null )
                    {
                        urlReference = TextUtil.replaceString( urlReference, "%s", wikiPage );
                        callMutatorChain( m_externalLinkMutatorChain, urlReference );

                        sb.append( makeLink( INTERWIKI, urlReference, link ) );
                    }
                    else
                    {
                        sb.append( link+" <FONT COLOR=\"#FF0000\">(No InterWiki reference defined in properties for Wiki called '"+extWiki+"'!)</FONT>");
                    }
                }
                else if( reallink.startsWith("#") )
                {
                    // It defines a local footnote
                    sb.append( makeLink( LOCAL, reallink, link ) );
                }
                else if( isNumber( reallink ) )
                {
                    // It defines a reference to a local footnote
                    sb.append( makeLink( LOCALREF, reallink, link ) );
                }
                else
                {
                    // It's an internal Wiki link
                    reallink = cleanLink( reallink );

                    callMutatorChain( m_localLinkMutatorChain, reallink );

                    String matchedLink;
                    if( (matchedLink = linkExists( reallink )) != null )
                    {
                        sb.append( makeLink( READ, matchedLink, link ) );
                    }
                    else
                    {
                        sb.append( makeLink( EDIT, reallink, link ) );
                    }
                }
            }
            else
            {
                log.error("Unterminated link");
                break;
            }

            idx = end+1;
        }

        return sb.toString();
    }

    /**
     *  Checks if this line is a heading line.
     */
    private String setHeadings( String line )
    {
        if( line.startsWith("!!!") )
        {
            line = TextUtil.replaceString( line, 0, 3, "<H2>" ) + "</H2>";
        }
        else if( line.startsWith("!!") )
        {
            line = TextUtil.replaceString( line, 0, 2, "<H3>" ) + "</H3>";
        }
        else if( line.startsWith("!") )
        {
            line = TextUtil.replaceString( line, 0, 1, "<H4>" ) + "</H4>";
        }
        
        return line;
    }

    private String setDefinitionList( String line )
    {
        if( line.startsWith(";") )
        {
            int breakIndex = line.indexOf(':');

            if( breakIndex > 0 )
            {
                StringBuffer res = new StringBuffer();

                res.append("<DL>\n<DT>"+line.substring(1,breakIndex)+"</DT>");
                res.append("<DD>"+line.substring(breakIndex+1)+"</DD>");
                res.append("\n</DL>");

                line = res.toString();
            }
        }

        return line;
    }
    /**
     *  Translates horizontal rulers.
     */
    private String setHR( String line )
    {
        StringBuffer buf = new StringBuffer();
        int start = line.indexOf("----");

        if( start != -1 )
        {
            int i;
            buf.append( line.substring( 0, start ) );
            for( i = start; i<line.length() && line.charAt(i) == '-'; i++ )
            {
            }
            buf.append("<HR>");
            buf.append( line.substring( i ) );

            return buf.toString();
        }

        return line;
    }

    /**
     *  Closes all annoying lists and things that the user might've
     *  left open.
     */
    private String closeAll()
    {
        StringBuffer buf = new StringBuffer();

        if( m_isbold )
        {
            buf.append("</B>");
            m_isbold = false;
        }

        if( m_isitalic )
        {
            buf.append("</I>");
            m_isitalic = false;
        }

        if( m_isTypedText )
        {
            buf.append("</TT>");
            m_isTypedText = false;
        }

        for( ; m_listlevel > 0; m_listlevel-- )
        {
            buf.append( "</UL>\n" );
        }

        for( ; m_numlistlevel > 0; m_numlistlevel-- )
        {
            buf.append( "</OL>\n" );
        }

        if( m_iscode ) 
        {
            buf.append("</PRE>\n");
            m_iscode = false;
        }

        if( m_istable )
        {
            buf.append( "</TABLE>" );
            m_istable = false;
        }

        return buf.toString();
    }

    /**
     *  Sets bold text.
     */
    private String setBold( String line )
    {
        StringBuffer buf = new StringBuffer();

        for( int i = 0; i < line.length(); i++ )
        {
            if( line.charAt(i) == '_' && i < line.length()-1 )
            {
                if( line.charAt(i+1) == '_' )
                {
                    buf.append( m_isbold ? "</B>" : "<B>" );
                    m_isbold = !m_isbold;
                    i++;
                }
                else buf.append( "_" );
            }
            else buf.append( line.charAt(i) );
        }

        return buf.toString();
    }

    /**
     *  Counts how many consecutive characters of a certain type exists on the line.
     *  @param line String of chars to check.
     *  @param startPos Position to start reading from.
     *  @param char Character to check for.
     */
    private int countChar( String line, int startPos, char c )
    {
        int count;

        for( count = 0; (startPos+count < line.length()) && (line.charAt(count+startPos) == c); count++ );

        return count;
    }

    /**
     *  Returns a new String that has char c n times.
     */
    private String repeatChar( char c, int n )
    {
        StringBuffer sb = new StringBuffer();
        for( int i = 0; i < n; i++ ) sb.append(c);

        return sb.toString();
    }

    /**
     *  {{text}} = <TT>text</TT>
     */
    private String setTT( String line )
    {
        StringBuffer buf = new StringBuffer();

        for( int i = 0; i < line.length(); i++ )
        {
            if( line.charAt(i) == '{' && !m_isTypedText )
            {
                int count = countChar( line, i, '{' );

                if( count == 2 )
                {
                    buf.append( "<TT>" );
                    m_isTypedText = true;
                }
                else 
                {
                    buf.append( repeatChar( '{', count ) );
                }
                i += count-1;
            }
            else if( line.charAt(i) == '}' && m_isTypedText )
            {
                int count = countChar( line, i, '}' );

                if( count == 2 )
                {
                    buf.append( "</TT>" );
                    m_isTypedText = false;
                }
                else 
                {
                    buf.append( repeatChar( '}', count ) );
                }
                i += count-1;
            }
            else
            { 
                buf.append( line.charAt(i) );
            }
        }

        return buf.toString();
    }

    private String setItalic( String line )
    {
        StringBuffer buf = new StringBuffer();

        for( int i = 0; i < line.length(); i++ )
        {
            if( line.charAt(i) == '\'' && i < line.length()-1 )
            {
                if( line.charAt(i+1) == '\'' )
                {
                    buf.append( m_isitalic ? "</I>" : "<I>" );
                    m_isitalic = !m_isitalic;
                    i++;
                }
                else buf.append( "'" );
            }
            else buf.append( line.charAt(i) );
        }

        return buf.toString();
    }

    private void fillBuffer()
        throws IOException
    {
        int pre;
        String postScript = ""; // Gets added at the end of line.

        StringBuffer buf = new StringBuffer();

        String line = m_in.readLine();

        if( line == null ) 
        {
            m_data = new StringReader("");
            return;
        }

        String trimmed = line.trim();

        //
        //  Replace the most obvious items that could possibly
        //  break the resulting HTML code.
        //

        line = TextUtil.replaceEntities( line );

        if( !m_iscode )
        {

            //
            //  Tables
            //
            if( trimmed.startsWith("|") )
            {
                StringBuffer tableLine = new StringBuffer();

                if( !m_istable )
                {
                    buf.append( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n" );
                    m_istable = true;
                }

                buf.append( "<TR>" );

                //
                //  The following piece of code will go through the line character
                //  by character, and replace all references to the table markers (|)
                //  by a <TD>, EXCEPT when '|' can be found inside a link.
                //
                boolean islink = false;
                for( int i = 0; i < line.length(); i++ )
                {
                    char c = line.charAt(i);
                    switch( c )
                    {
                      case '|':
                        if( !islink )
                        {
                            if( i < line.length()-1 && line.charAt(i+1) == '|' )
                            {
                                // It's a heading.
                                tableLine.append( "<TH>" );
                                i++;
                            }
                            else
                            {
                                // It's a normal thingy.
                                tableLine.append( "<TD>" );
                            }
                        }
                        else
                        {
                            tableLine.append( c );
                        }
                        break;

                      case '[':
                        islink = true;
                        tableLine.append( c );
                        break;

                      case ']':
                        islink = false;
                        tableLine.append( c );
                        break;

                      default:
                        tableLine.append( c );
                        break;
                    }
                } // for

                line = tableLine.toString();

                postScript = "</TR>";
            }
            else if( !trimmed.startsWith("|") && m_istable )
            {
                buf.append( "</TABLE>" );
                m_istable = false;
            }

            //
            //  FIXME: The following two blocks of code are temporary
            //  solutions for code going all wonky if you do multiple #*:s inside
            //  each other.
            //  A real solution is needed - this only closes down the other list
            //  before the other one gets started.
            //
            if( line.startsWith("*") )
            {
                for( ; m_numlistlevel > 0; m_numlistlevel-- )
                {
                    buf.append("</OL>\n");
                }

            }

            if( line.startsWith("#") )
            {
                for( ; m_listlevel > 0; m_listlevel-- )
                {
                    buf.append("</UL>\n");
                }
            }

            //
            // Make a bulleted list
            //
            if( line.startsWith("*") )
            {
                // Close all other lists down.

                int numBullets = countChar( line, 0, '*' );
                
                if( numBullets > m_listlevel )
                {
                    for( ; m_listlevel < numBullets; m_listlevel++ )
                        buf.append("<UL>\n");
                }
                else if( numBullets < m_listlevel )
                {
                    for( ; m_listlevel > numBullets; m_listlevel-- )
                        buf.append("</UL>\n");
                }
                
                buf.append("<LI>");
                line = line.substring( numBullets );
            }
            else if( line.startsWith(" ") && m_listlevel > 0 && trimmed.length() != 0 )
            {
                // This is a continuation of a previous line.
            }
            else if( line.startsWith("#") && m_listlevel > 0 )
            {
                // We don't close all things for the other list type.
            }
            else
            {
                // Close all lists down.
                for( ; m_listlevel > 0; m_listlevel-- )
                {
                    buf.append("</UL>\n");
                }
            }

            //
            //  Ordered list
            //
            if( line.startsWith("#") )
            {
                // Close all other lists down.
                if( m_numlistlevel == 0 )
                {
                    for( ; m_listlevel > 0; m_listlevel-- )
                    {
                        buf.append("</UL>\n");
                    }
                }

                int numBullets = countChar( line, 0, '#' );
                
                if( numBullets > m_numlistlevel )
                {
                    for( ; m_numlistlevel < numBullets; m_numlistlevel++ )
                        buf.append("<OL>\n");
                }
                else if( numBullets < m_numlistlevel )
                {
                    for( ; m_numlistlevel > numBullets; m_numlistlevel -- )
                        buf.append("</OL>\n");
                }
                
                buf.append("<LI>");
                line = line.substring( numBullets );
            }
            else if( line.startsWith(" ") && m_numlistlevel > 0 && trimmed.length() != 0 )
            {
                // This is a continuation of a previous line.
            }
            else if( line.startsWith("*") && m_numlistlevel > 0 )
            {
                // We don't close things for the other list type.
            }
            else
            {
                // Close all lists down.
                for( ; m_numlistlevel > 0; m_numlistlevel-- )
                {
                    buf.append("</OL>\n");
                }
            }

            // Do the standard settings

            if( m_camelCaseLinks )
            {
                line = setCamelCaseLinks( line );
            }

            line = setDefinitionList( line );
            line = setHeadings( line );
            line = setHR( line );
            line = setBold( line );
            line = setItalic( line );

            line = setTT( line );
            line = TextUtil.replaceString( line, "\\\\", "<BR>" );

            line = setHyperLinks( line );

            // Is this an empty line?
            if( trimmed.length() == 0 )
            {
                buf.append( "<P>" );
            }

            if( (pre = line.indexOf("{{{")) != -1 )
            {
                line = TextUtil.replaceString( line, pre, pre+3, "<PRE>" );
                m_iscode = true;
            }

        }
            
        if( (pre = line.indexOf("}}}")) != -1 )
        {
            line = TextUtil.replaceString( line, pre, pre+3, "</PRE>" );
            m_iscode = false;
        }

        buf.append( line );
        buf.append( postScript );
        buf.append( "\n" );
        
        m_data = new StringReader( buf.toString() );
    }

    public int read()
        throws IOException
    {
        int val = m_data.read();

        if( val == -1 )
        {
            fillBuffer();
            val = m_data.read();

            if( val == -1 )
            {
                m_data = new StringReader( closeAll() );

                val = m_data.read();
            }
        }

        return val;
    }

    public int read( char[] buf, int off, int len )
        throws IOException
    {
        return m_data.read( buf, off, len );
    }

    public boolean ready()
        throws IOException
    {
        log.debug("ready ? "+m_data.ready() );
        if(!m_data.ready())
        {
            fillBuffer();
        }

        return m_data.ready();
    }

    public void close()
    {
    }
}
