package com.ecyrd.jspwiki.parser;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;
import org.apache.oro.text.GlobCompiler;
import org.apache.oro.text.regex.*;
import org.jdom.Content;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.Text;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.acl.Acl;
import com.ecyrd.jspwiki.plugin.PluginException;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.render.CleanTextRenderer;

/**
 *  This is a new class which replaces the TranslatorReader at some point.
 *  It is not yet functional, but it's getting there.  The aim is to produce
 *  a parser class to an internal DOM tree; then cache this tree and only
 *  evaluate it at output.
 *  
 * @author jalkanen
 *
 */
public class JSPWikiMarkupParser
    extends MarkupParser
{
    private static final int              READ          = 0;
    private static final int              EDIT          = 1;
    private static final int              EMPTY         = 2;  // Empty message
    private static final int              LOCAL         = 3;
    private static final int              LOCALREF      = 4;
    private static final int              IMAGE         = 5;
    private static final int              EXTERNAL      = 6;
    private static final int              INTERWIKI     = 7;
    private static final int              IMAGELINK     = 8;
    private static final int              IMAGEWIKILINK = 9;
    public  static final int              ATTACHMENT    = 10;
    // private static final int              ATTACHMENTIMAGE = 11;

    /** Lists all punctuation characters allowed in WikiMarkup. These
        will not be cleaned away. */

    private static final String           PUNCTUATION_CHARS_ALLOWED = "._";

    private static Logger log = Logger.getLogger( JSPWikiMarkupParser.class );

    //private boolean        m_iscode       = false;
    private boolean        m_isbold       = false;
    private boolean        m_isitalic     = false;
    private boolean        m_isTypedText  = false;
    private boolean        m_istable      = false;
    private boolean        m_isPre        = false;
    private boolean        m_isEscaping   = false;
    private boolean        m_isdefinition = false;
    private boolean        m_isPreBlock   = false;

    /** Contains style information, in multiple forms. */
    private Stack          m_styleStack   = new Stack();
    
     // general list handling
    private int            m_genlistlevel = 0;
    private StringBuffer   m_genlistBulletBuffer = new StringBuffer();  // stores the # and * pattern
    private boolean        m_allowPHPWikiStyleLists = true;


    private boolean        m_isOpenParagraph = false;

    /** Tag that gets closed at EOL. */
    private String         m_closeTag     = null; 

    

    /** Keeps image regexp Patterns */
    private ArrayList      m_inlineImagePatterns;

    private PatternMatcher m_inlineMatcher = new Perl5Matcher();

    /** Keeps track of any plain text that gets put in the Text nodes */
    private StringBuffer   m_plainTextBuf = new StringBuffer();
    
    private Document       m_document = new Document();
    private Element        m_currentElement;
    
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

    /** If true, all outward links (external links) have a small link image appended. */
    public static final String     PROP_USEOUTLINKIMAGE  = "jspwiki.translatorReader.useOutlinkImage";

    /** If set to "true", allows using raw HTML within Wiki text.  Be warned,
        this is a VERY dangerous option to set - never turn this on in a publicly
        allowable Wiki, unless you are absolutely certain of what you're doing. */
    public static final String     PROP_ALLOWHTML        = "jspwiki.translatorReader.allowHTML";

    /** If set to "true", all external links are tagged with 'rel="nofollow"' */
    public static final String     PROP_USERELNOFOLLOW   = "jspwiki.translatorReader.useRelNofollow";

    /** If set to "true", enables plugins during parsing */
    public static final String     PROP_RUNPLUGINS       = "jspwiki.translatorReader.runPlugins";
    
    /** If true, then considers CamelCase links as well. */
    private boolean                m_camelCaseLinks      = false;

    /** If true, consider URIs that have no brackets as well. */
    // FIXME: Currently reserved, but not used.
    private boolean                m_plainUris           = false;

    /** If true, all outward links use a small link image. */
    private boolean                m_useOutlinkImage     = true;

    /** If true, allows raw HTML. */
    private boolean                m_allowHTML           = false;

    private boolean                m_useRelNofollow      = false;

    private PatternMatcher         m_matcher  = new Perl5Matcher();
    private PatternCompiler        m_compiler = new Perl5Compiler();
    private Pattern                m_camelCasePtrn;

    private TextRenderer           m_renderer;

    /**
     *  The default inlining pattern.  Currently "*.png"
     */
    public static final String     DEFAULT_INLINEPATTERN = "*.png";

    /**
     *  These characters constitute word separators when trying
     *  to find CamelCase links.
     */
    private static final String    WORD_SEPARATORS = ",.|;+=&()";

    protected static final int BOLD           = 0;
    protected static final int ITALIC         = 1;
    protected static final int TYPED          = 2;
    
    /**
     *  This list contains all IANA registered URI protocol
     *  types as of September 2004 + a few well-known extra types.
     *
     *  JSPWiki recognises all of them as external links.
     *  
     *  This array is sorted during class load, so you can just dump
     *  here whatever you want in whatever order you want.
     */
    static final String[] c_externalLinks = {
        "http:", "ftp:", "https:", "mailto:",
        "news:", "file:", "rtsp:", "mms:", "ldap:",
        "gopher:", "nntp:", "telnet:", "wais:",
        "prospero:", "z39.50s", "z39.50r", "vemmi:",
        "imap:", "nfs:", "acap:", "tip:", "pop:",
        "dav:", "opaquelocktoken:", "sip:", "sips:",
        "tel:", "fax:", "modem:", "soap.beep:", "soap.beeps",
        "xmlrpc.beep", "xmlrpc.beeps", "urn:", "go:",
        "h323:", "ipp:", "tftp:", "mupdate:", "pres:",
        "im:", "mtqp", "smb:" };

    /**
     *  This Comparator is used to find an external link from c_externalLinks.  It
     *  checks if the link starts with the other arraythingie.
     */
    private static Comparator c_startingComparator = new StartingComparator();

    static
    {
        Arrays.sort( c_externalLinks );
    }
    
    /**
     *  Creates a markup parser.
     */
    public JSPWikiMarkupParser( WikiContext context, Reader in )
    {
        super( context, in );
        initialize( new HTMLRenderer() );
    }

    /**
     * @deprecated
     */
    public JSPWikiMarkupParser( WikiContext context, Reader in, TextRenderer renderer )
    {
        super( context, in );
        initialize( renderer );
    }


    /**
     *  @param engine The WikiEngine this reader is attached to.  Is
     * used to figure out of a page exits.
     */

    // FIXME: parsers should be pooled for better performance.
    private void initialize( TextRenderer renderer )
    {
        PatternCompiler compiler         = new GlobCompiler();
        ArrayList       compiledpatterns = new ArrayList();

        m_renderer = renderer;

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
            m_camelCasePtrn = m_compiler.compile( "^([[:^alnum:]]*)([[:upper:]]+[[:lower:]]+[[:upper:]]+[[:alnum:]]*)[[:^alnum:]]*$" );
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

        String cclinks = (String)m_context.getPage().getAttribute( PROP_CAMELCASELINKS );

        if( cclinks != null )
        {
            m_camelCaseLinks = TextUtil.isPositive( cclinks );
        }
        else
        {
            m_camelCaseLinks  = TextUtil.getBooleanProperty( props,
                                                             PROP_CAMELCASELINKS, 
                                                             m_camelCaseLinks );
        }

        m_plainUris           = TextUtil.getBooleanProperty( props,
                                                             PROP_PLAINURIS,
                                                             m_plainUris );
        m_useOutlinkImage     = TextUtil.getBooleanProperty( props,
                                                             PROP_USEOUTLINKIMAGE, 
                                                             m_useOutlinkImage );
        m_allowHTML           = TextUtil.getBooleanProperty( props,
                                                             PROP_ALLOWHTML, 
                                                             m_allowHTML );

        m_useRelNofollow      = TextUtil.getBooleanProperty( props,
                                                             PROP_USERELNOFOLLOW,
                                                             m_useRelNofollow );
    
        if( m_engine.getUserDatabase() == null || m_engine.getAuthorizationManager() == null )
        {
            disableAccessRules();
        }   
        
        m_context.getPage().setHasMetadata();
    }

    /**
     *  Sets the currently used renderer.  This method is protected because
     *  we only want to use it internally for now.  The renderer interface
     *  is not yet set to stone, so it's not expected that third parties
     *  would use this.
     */
    protected void setRenderer( TextRenderer renderer )
    {
        m_renderer = renderer;
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
        try
        {
            if( page == null || page.length() == 0 ) return null;
            
            return m_engine.getFinalPageName( page );
        }
        catch( ProviderException e )
        {
            log.warn("TranslatorReader got a faulty page name!",e);

            return page;  // FIXME: What would be the correct way to go back?
        }
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

    private void callHeadingListenerChain( TranslatorReader.Heading param )
    {
        List list = m_headingListenerChain;

        for( Iterator i = list.iterator(); i.hasNext(); )
        {
            HeadingListener h = (HeadingListener) i.next();
            
            h.headingAdded( m_context, param );
        }
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
        if( text == null ) text = link;

        text = callMutatorChain( m_linkMutators, text );

        return m_renderer.makeLink( type, link, text );
    }
    
    private Element makeLink( int type, String link, String text, String section )
    {
        Element el = null;
        
        if( text == null ) text = link;

        section = (section != null) ? ("#"+section) : "";

        // Make sure we make a link name that can be accepted
        // as a valid URL.

        String encodedlink = m_engine.encodeName( link );

        if( encodedlink.length() == 0 )
        {
            type = EMPTY;
        }

        switch(type)
        {
            case READ:
                el = new Element("a").setAttribute("class", "wikipage");
                el.setAttribute("href",m_context.getURL(WikiContext.VIEW, link)+section);
                el.addContent(text);
                break;

            case EDIT:
                el = new Element("a").setAttribute("class", "editpage");
                el.setAttribute("title","Create '"+link+"'");
                el.setAttribute("href", m_context.getURL(WikiContext.EDIT,link));
                el.addContent(text);
                break;

            case EMPTY:
                el = new Element("u").addContent(text);
                break;

                //
                //  These two are for local references - footnotes and 
                //  references to footnotes.
                //  We embed the page name (or whatever WikiContext gives us)
                //  to make sure the links are unique across Wiki.
                //
            case LOCALREF:
                el = new Element("a").setAttribute("class","footnoteref");
                el.setAttribute("href","#ref-"+m_context.getPage().getName()+"-"+link);
                el.addContent("["+text+"]");
                break;

            case LOCAL:
                el = new Element("a").setAttribute("class","footnote");
                el.setAttribute("name", "ref-"+m_context.getPage().getName()+"-"+link.substring(1));
                el.addContent("["+text+"]");
                break;

                //
                //  With the image, external and interwiki types we need to
                //  make sure nobody can put in Javascript or something else
                //  annoying into the links themselves.  We do this by preventing
                //  a haxor from stopping the link name short with quotes in 
                //  fillBuffer().
                //
            case IMAGE:
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                break;

            case IMAGELINK:
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                el = new Element("a").setAttribute("href",text).addContent(el);
                break;

            case IMAGEWIKILINK:
                String pagelink = m_context.getURL(WikiContext.VIEW,text);
                el = new Element("img").setAttribute("class","inline");
                el.setAttribute("src",link);
                el.setAttribute("alt",text);
                el = new Element("a").setAttribute("href",text).setAttribute("class","wikipage").addContent(el);
                break;

            case EXTERNAL:
                el = new Element("a").setAttribute("class","external");
                if( m_useRelNofollow ) el.setAttribute("rel","nofollow");
                el.setAttribute("href",link+section);
                el.addContent(text);
                break;
                
            case INTERWIKI:
                el = new Element("a").setAttribute("class","interwiki");
                el.setAttribute("href",link+section);
                el.addContent(text);
                break;

            case ATTACHMENT:
                String attlink = m_context.getURL( WikiContext.ATTACH,
                                                   link );

                String infolink = m_context.getURL( WikiContext.INFO,
                                                    link );

                String imglink = m_context.getURL( WikiContext.NONE,
                                                   "images/attachment_small.png" );

                el = new Element("a").setAttribute("class","attachment");
                el.setAttribute("href",attlink);
                el.addContent(text);
                
                pushElement(el);
                popElement(el.getName());
                
                el = new Element("img").setAttribute("src",imglink);
                el.setAttribute("border","0");
                el.setAttribute("alt","(info)");
                
                el = new Element("a").setAttribute("href",infolink).addContent(el);
                break;

            default:
                break;
        }

        if( el != null )
        {
            flushPlainText();
            m_currentElement.addContent( el );
        }
        return el;
    }


    /**
     *  Cleans a Wiki name.
     *  <P>
     *  [ This is a link ] -&gt; ThisIsALink
     *
     *  @param link Link to be cleared. Null is safe, and causes this to return null.
     *  @return A cleaned link.
     *
     *  @since 2.0
     */
    public static String cleanLink( String link )
    {
        StringBuffer clean = new StringBuffer();

        if( link == null ) return null;

        //
        //  Compress away all whitespace and capitalize
        //  all words in between.
        //

        StringTokenizer st = new StringTokenizer( link, " -" );

        while( st.hasMoreTokens() )
        {
            StringBuffer component = new StringBuffer(st.nextToken());

            component.setCharAt(0, Character.toUpperCase( component.charAt(0) ) );

            //
            //  We must do this, because otherwise compiling on JDK 1.4 causes
            //  a downwards incompatibility to JDK 1.3.
            //
            clean.append( component.toString() );
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
            char ch = clean.charAt(i);

            if( !(Character.isLetterOrDigit(ch) ||
                  PUNCTUATION_CHARS_ALLOWED.indexOf(ch) != -1 ))
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
        int idx = Arrays.binarySearch( c_externalLinks, link, 
                                       c_startingComparator );

        //
        //  We need to check here once again; otherwise we might
        //  get a match for something like "h".
        //
        if( idx >= 0 && link.startsWith(c_externalLinks[idx]) ) return true;
        
        return false;
    }

    /**
     *  Returns true, if the link in question is an access
     *  rule.
     */
    private static boolean isAccessRule( String link )
    {
        return link.startsWith("{ALLOW") || link.startsWith("{DENY");
    }

    /**
     *  Matches the given link to the list of image name patterns
     *  to determine whether it should be treated as an inline image
     *  or not.
     */
    private boolean isImageLink( String link )
    {
        if( m_inlineImages )
        {
            for( Iterator i = m_inlineImagePatterns.iterator(); i.hasNext(); )
            {
                if( m_inlineMatcher.matches( link, (Pattern) i.next() ) )
                    return true;
            }
        }

        return false;
    }

    private static boolean isMetadata( String link )
    {
        return link.startsWith("{SET");
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
     *  This method peeks ahead in the stream until EOL and returns the result.
     *  It will keep the buffers untouched.
     *
     *  @return The string from the current position to the end of line.
     */

    // FIXME: Always returns an empty line, even if the stream is full.
    private String peekAheadLine()
        throws IOException
    {
        String s = readUntilEOL().toString();
        pushBack( s );

        return s;
    }
    

    /**
     *  Writes HTML for error message.
     */

    public static Element makeError( String error )
    {
        return new Element("span").setAttribute("class","error").addContent(error);
    }

    private void flushPlainText()
    {
        if( m_plainTextBuf.length() > 0 )
        {
            m_currentElement.addContent( m_plainTextBuf.toString() );
            m_plainTextBuf = new StringBuffer();
        }        
    }
    
    private Element pushElement( Element e )
    {
        flushPlainText();
        m_currentElement.addContent( e );
        m_currentElement = e;
        
        return e;
    }
    
    private Element addElement( Content e )
    {
        flushPlainText();
        m_currentElement.addContent( e );
        
        return m_currentElement;
    }
    
    private Element popElement( String s )
    {
        flushPlainText();
        
        Element currEl = m_currentElement;
        
        while( currEl.getParentElement() != null )
        {
            if( currEl.getName().equals(s) && !currEl.isRootElement() )
            {
                m_currentElement = currEl.getParentElement();
                return m_currentElement;
            }
            
            currEl = currEl.getParentElement();
        }
        
        return m_currentElement;
    }


    /**
     *  Reads the stream until it meets one of the specified
     *  ending characters, or stream end.  The ending character will be left
     *  in the stream.
     */
    private String readUntil( String endChars )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch = nextToken();

        while( ch != -1 )
        {
            if( ch == '\\' ) 
            {
                ch = nextToken(); 
                if( ch == -1 ) 
                {
                    break;
                }
            }
            else
            {
                if( endChars.indexOf((char)ch) != -1 )
                {
                    pushBack( ch );
                    break;
                }
            }
            sb.append( (char) ch );
            ch = nextToken();
        }

        return sb.toString();
    }

    /**
     *  Reads the stream while the characters that have been specified are
     *  in the stream, returning then the result as a String.
     */
    private String readWhile( String endChars )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch = nextToken();

        while( ch != -1 )
        {
            if( endChars.indexOf((char)ch) == -1 )
            {
                pushBack( ch );
                break;
            }
            
            sb.append( (char) ch );
            ch = nextToken();
        }

        return sb.toString();
    }

    private JSPWikiMarkupParser m_cleanTranslator;

    /**
     *  Does a lazy init.  Otherwise, we would get into a situation
     *  where HTMLRenderer would try and boot a TranslatorReader before
     *  the TranslatorReader it is contained by is up.
     */
    private JSPWikiMarkupParser getCleanTranslator()
    {
        if( m_cleanTranslator == null )
        {
            WikiContext dummyContext = new WikiContext( m_engine, 
                                                        m_context.getPage() );
            
            m_cleanTranslator = new JSPWikiMarkupParser( dummyContext, null );

            m_cleanTranslator.m_allowHTML = true;
        }

        return m_cleanTranslator;
    }
    /**
     *  Modifies the "hd" parameter to contain proper values.  Because
     *  an "id" tag may only contain [a-zA-Z0-9:_-], we'll replace the
     *  % after url encoding with '_'.
     */
    private String makeHeadingAnchor( String baseName, String title, TranslatorReader.Heading hd )
    {
        hd.m_titleText = title;
        title = cleanLink( title );
        hd.m_titleSection = m_engine.encodeName(title);
        hd.m_titleAnchor = "section-"+m_engine.encodeName(baseName)+
                           "-"+hd.m_titleSection;
        
        hd.m_titleAnchor = hd.m_titleAnchor.replace( '%', '_' );
        return hd.m_titleAnchor;
    }

    private String makeSectionTitle( String title )
    {
        title = title.trim();
        String outTitle;
        
        try
        {
            JSPWikiMarkupParser dtr = getCleanTranslator();
            dtr.setInputReader( new StringReader(title) );

            CleanTextRenderer ctt = new CleanTextRenderer(m_context, dtr.parse());
            
            outTitle = ctt.getString();
        }
        catch( IOException e )
        {
            log.fatal("CleanTranslator not working", e);
            throw new InternalWikiException("CleanTranslator not working as expected, when cleaning title"+ e.getMessage() );
        }

        return outTitle;
    }
    
    /**
     *  Returns XHTML for the start of the heading.  Also sets the
     *  line-end emitter.
     *  @param level 
     *  @param headings A List to which heading should be added.
     */ 
    public Element makeHeading( int level, String title, TranslatorReader.Heading hd )
    {
        Element el = null;
        
        String pageName = m_context.getPage().getName();

        String outTitle = makeSectionTitle( title );

        hd.m_level = level;

        switch( level )
        {
          case TranslatorReader.Heading.HEADING_SMALL:
            el = new Element("h4").setAttribute("id",makeHeadingAnchor( pageName, outTitle, hd ));
            break;

          case TranslatorReader.Heading.HEADING_MEDIUM:
            el = new Element("h3").setAttribute("id",makeHeadingAnchor( pageName, outTitle, hd ));
            break;

          case TranslatorReader.Heading.HEADING_LARGE:
            el = new Element("h2").setAttribute("id",makeHeadingAnchor( pageName, outTitle, hd ));
            break;
        }
        
        return el;
    }

    /**
     *  Checks for the existence of a traditional style CamelCase link.
     *  <P>
     *  We separate all white-space -separated words, and feed it to this
     *  routine to find if there are any possible camelcase links.
     *  For example, if "word" is "__HyperLink__" we return "HyperLink".
     *
     *  @param word A phrase to search in.
     *  @return The match within the phrase.  Returns null, if no CamelCase
     *          hyperlink exists within this phrase.
     */
    private String checkForCamelCaseLink( String word )
    {
        PatternMatcherInput input;

        input = new PatternMatcherInput( word );

        if( m_matcher.contains( input, m_camelCasePtrn ) )
        {
            MatchResult res = m_matcher.getMatch();
  
            String link = res.group(2);

            if( res.group(1) != null )
            {
                if( res.group(1).endsWith("~") ||
                    res.group(1).indexOf('[') != -1 )
                {
                    // Delete the (~) from beginning.
                    // We'll make '~' the generic kill-processing-character from
                    // now on.
                    return null;
                }
            }

            return link;
        } // if match

        return null;
    }

    /**
     *  When given a link to a WikiName, we just return
     *  a proper HTML link for it.  The local link mutator
     *  chain is also called.
     */
    private String makeCamelCaseLink( String wikiname )
    {
        String matchedLink;
        String link;

        callMutatorChain( m_localLinkMutatorChain, wikiname );

        if( (matchedLink = linkExists( wikiname )) != null )
        {
            link = makeLink( READ, matchedLink, wikiname );
        }
        else
        {
            link = makeLink( EDIT, wikiname, wikiname );
        }

        return link;
    }

    private Element makeDirectURILink( String url )
    {
        Element result;
        String last = null;
        
        if( url.endsWith(",") || url.endsWith(".") )
        {
            last = url.substring( url.length()-1 );
            url  = url.substring( 0, url.length()-1 );
        }

        callMutatorChain( m_externalLinkMutatorChain, url );

        if( isImageLink( url ) )
        {
            result = handleImageLink( url, url, false );
        }
        else
        {
            result = new Element("span");
            result.addContent(makeLink( EXTERNAL, url, url,null ));
            // result.addContent( m_renderer.outlinkImage() );
        }

        if( last != null )
            m_plainTextBuf.append(last);
        
        return result;
    }

    /**
     *  Image links are handled differently:
     *  1. If the text is a WikiName of an existing page,
     *     it gets linked.
     *  2. If the text is an external link, then it is inlined.  
     *  3. Otherwise it becomes an ALT text.
     *
     *  @param reallink The link to the image.
     *  @param link     Link text portion, may be a link to somewhere else.
     *  @param hasLinkText If true, then the defined link had a link text available.
     *                  This means that the link text may be a link to a wiki page,
     *                  or an external resource.
     */
    
    private Element handleImageLink( String reallink, String link, boolean hasLinkText )
    {
        String possiblePage = cleanLink( link );
        String matchedLink;

        if( isExternalLink( link ) && hasLinkText )
        {
            return makeLink( IMAGELINK, reallink, link, null );
        }
        else if( (matchedLink = linkExists( possiblePage )) != null &&
                 hasLinkText )
        {
            // System.out.println("Orig="+link+", Matched: "+matchedLink);
            callMutatorChain( m_localLinkMutatorChain, possiblePage );
            
            return makeLink( IMAGEWIKILINK, reallink, link, null );
        }
        else
        {
            return makeLink( IMAGE, reallink, link, null );
        }
    }

    private Element handleAccessRule( String ruleLine )
    {
        if( !m_parseAccessRules ) return m_currentElement;
        Acl acl;
        WikiPage          page = m_context.getPage();
        // UserDatabase      db = m_context.getEngine().getUserDatabase();

        if( ruleLine.startsWith( "{" ) )
            ruleLine = ruleLine.substring( 1 );
        if( ruleLine.endsWith( "}" ) )
            ruleLine = ruleLine.substring( 0, ruleLine.length() - 1 );

        log.debug("page="+page.getName()+", ACL = "+ruleLine);
        
        try
        {
            acl = m_engine.getAclManager().parseAcl( page, ruleLine );

            page.setAcl( acl );

            log.debug( acl.toString() );
        }
        catch( WikiSecurityException wse )
        {
            return makeError( wse.getMessage() );
        }

        return m_currentElement;
    }

    /**
     *  Handles metadata setting [{SET foo=bar}]
     */
    private Element handleMetadata( String link )
    {
        try
        {
            String args = link.substring( link.indexOf(' '), link.length()-1 );
            
            String name = args.substring( 0, args.indexOf('=') );
            String val  = args.substring( args.indexOf('=')+1, args.length() );

            name = name.trim();
            val  = val.trim();

            if( val.startsWith("'") ) val = val.substring( 1 );
            if( val.endsWith("'") )   val = val.substring( 0, val.length()-1 );

            // log.debug("SET name='"+name+"', value='"+val+"'.");

            if( name.length() > 0 && val.length() > 0 )
            {
                val = m_engine.getVariableManager().expandVariables( m_context,
                                                                     val );
            
                m_context.getPage().setAttribute( name, val );
            }
        }
        catch( Exception e )
        {
            return makeError(" Invalid SET found: "+link);
        }

        return m_currentElement;
    }

    /**
     *  Gobbles up all hyperlinks that are encased in square brackets.
     */
    private Element handleHyperlinks( String link )
    {
        StringBuffer sb        = new StringBuffer();
        String       reallink;
        int          cutpoint;

        if( isAccessRule( link ) )
        {
            return handleAccessRule( link );
        }

        if( isMetadata( link ) )
        {
            return handleMetadata( link );
        }

        if( PluginManager.isPluginLink( link ) )
        {
            try
            {
                Content pluginContent = m_engine.getPluginManager().parsePluginLine( m_context, link );
     
                m_currentElement.addContent( pluginContent );
            }
            catch( PluginException e )
            {
                log.info( "Failed to insert plugin", e );
                log.info( "Root cause:",e.getRootThrowable() );
                m_currentElement.addContent( makeError("Plugin insertion failed: "+e.getMessage()) );
                return m_currentElement;
            }
            
            return m_currentElement;
        }

        // link = TextUtil.replaceEntities( link );

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
        if( VariableManager.isVariableLink( link ) )
        {
            Content value;
            
            // FIXME: Must also evaluate lazily
            try
            {
                value = new Text(m_engine.getVariableManager().parseAndGetValue( m_context, link ));
            }
            catch( NoSuchVariableException e )
            {
                value = makeError(e.getMessage());
            }
            catch( IllegalArgumentException e )
            {
                value = makeError(e.getMessage());
            }

            m_currentElement.addContent( value );
        }
        else if( isExternalLink( reallink ) )
        {
            // It's an external link, out of this Wiki

            callMutatorChain( m_externalLinkMutatorChain, reallink );

            if( isImageLink( reallink ) )
            {
                addElement( handleImageLink( reallink, link, (cutpoint != -1) ) );
            }
            else
            {
                makeLink( EXTERNAL, reallink, link, null );
                sb.append( m_renderer.outlinkImage() );
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

                makeLink( INTERWIKI, urlReference, link, null );

                if( isExternalLink(urlReference) )
                {
                    sb.append( m_renderer.outlinkImage() );
                }
            }
            else
            {
                sb.append( link+" "+m_renderer.makeError("No InterWiki reference defined in properties for Wiki called '"+extWiki+"'!)") );
            }
        }
        else if( reallink.startsWith("#") )
        {
            // It defines a local footnote
            makeLink( LOCAL, reallink, link, null );
        }
        else if( isNumber( reallink ) )
        {
            // It defines a reference to a local footnote
            makeLink( LOCALREF, reallink, link, null );
        }
        else
        {
            int hashMark = -1;

            //
            //  Internal wiki link, but is it an attachment link?
            //
            String attachment = findAttachment( reallink );
            if( attachment != null )
            {
                callMutatorChain( m_attachmentLinkMutatorChain, attachment );

                if( isImageLink( reallink ) )
                {
                    attachment = m_context.getURL( WikiContext.ATTACH, attachment );
                    sb.append( handleImageLink( attachment, link, (cutpoint != -1) ) );
                }
                else
                {
                    makeLink( ATTACHMENT, attachment, link, null );
                }
            }
            else if( (hashMark = reallink.indexOf('#')) != -1 )
            {
                // It's an internal Wiki link, but to a named section

                String namedSection = reallink.substring( hashMark+1 );
                reallink = reallink.substring( 0, hashMark );

                reallink     = cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink;
                if( (matchedLink = linkExists( reallink )) != null )
                {
                    String sectref = "section-"+m_engine.encodeName(matchedLink)+"-"+namedSection;
                    sectref = sectref.replace('%', '_');
                    makeLink( READ, matchedLink, link, sectref );
                }
                else
                {
                    makeLink( EDIT, reallink, link, null );
                }
            }
            else
            {
                // It's an internal Wiki link
                reallink = cleanLink( reallink );

                callMutatorChain( m_localLinkMutatorChain, reallink );

                String matchedLink = linkExists( reallink );
                
                if( matchedLink != null )
                {
                    makeLink( READ, matchedLink, link, null );
                }
                else
                {
                    makeLink( EDIT, reallink, link, null );
                }
            }
        }

        return m_currentElement;
    }

    private String findAttachment( String link )
    {
        AttachmentManager mgr = m_engine.getAttachmentManager();
        Attachment att = null;

        try
        {
            att = mgr.getAttachmentInfo( m_context, link );
        }
        catch( ProviderException e )
        {
            log.warn("Finding attachments failed: ",e);
            return null;
        }

        if( att != null )
        {
            return att.getName();
        }
        else if( link.indexOf('/') != -1 )
        {
            return link;
        }

        return null;
    }


    private int nextToken()
        throws IOException
    {
        if( m_in == null ) return -1;
        return m_in.read();
    }

    /**
     *  Push back any character to the current input.  Does not
     *  push back a read EOF, though.
     */
    private void pushBack( int c )
        throws IOException
    {        
        if( c != -1 && m_in != null )
        {
            m_in.unread( c );
        }
    }

    /**
     *  Pushes back any string that has been read.  It will obviously
     *  be pushed back in a reverse order.
     *
     *  @since 2.1.77
     */
    private void pushBack( String s )
        throws IOException
    {
        for( int i = s.length()-1; i >= 0; i-- )
        {
            pushBack( s.charAt(i) );
        }
    }

    private Element handleBackslash()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '\\' )
        {
            int ch2 = nextToken();

            if( ch2 == '\\' )
            {
                pushElement( new Element("br").setAttribute("clear","all"));
                return popElement("br");
            }
           
            pushBack( ch2 );

            pushElement( new Element("br") );
            return popElement("br");
        }

        pushBack( ch );

        return null;
    }

    private Element handleUnderscore()
        throws IOException
    {
        int ch = nextToken();
        Element el = null;
        
        if( ch == '_' )
        {
            if( m_isbold )
            {
                el = popElement("b");
            }
            else
            {
                el = pushElement( new Element("b") );
            }
            m_isbold = !m_isbold;
        }
        else
        {
            pushBack( ch );
        }

        return el;
    }


    /**
     *  For example: italics.
     */
    private Element handleApostrophe()
        throws IOException
    {
        int ch = nextToken();
        Element el = null;
        
        if( ch == '\'' )
        {
            if( m_isitalic )
            {
                el = popElement("i");
            }
            else
            {
                el = pushElement( new Element("i") );
            }
            m_isitalic = !m_isitalic;
        }
        else
        {
            pushBack( ch );
        }

        return el;
    }

    private Element handleOpenbrace( boolean isBlock )
        throws IOException
    {
        int ch = nextToken();

        if( ch == '{' )
        {
            int ch2 = nextToken();

            if( ch2 == '{' )
            {
                startBlockLevel();
                m_isPre = true;
                m_isEscaping = true;
                m_isPreBlock = isBlock;
                
                if( isBlock )
                {
                    return pushElement( new Element("pre") );
                }
                else
                {
                    return pushElement( new Element("span").setAttribute("style","font-family:monospace; whitespace:pre;") );
                }
            }
            else
            {
                pushBack( ch2 );
                
                m_isTypedText = true;
                
                return pushElement( new Element("tt") );
           }
        }
        else
        {
            pushBack( ch );
        }

        return null;
    }

    /**
     *  Handles both }} and }}}
     */
    private Element handleClosebrace()
        throws IOException
    {
        int ch2 = nextToken();

        if( ch2 == '}' )
        {
            int ch3 = nextToken();

            if( ch3 == '}' )
            {
                if( m_isPre )
                {
                    m_isPre = false;
                    m_isEscaping = false;
                    if( m_isPreBlock )
                    {
                        return popElement( "pre" );
                    }
                    else
                    {
                        return popElement( "span" );
                    }
                }
                else
                {
                    m_plainTextBuf.append("}}}");
                    return m_currentElement;
                }
            }
            else
            {
                pushBack( ch3 );

                if( !m_isEscaping )
                {
                    m_isTypedText = false;
                    return popElement("tt");
                }
                else
                {
                    pushBack( ch2 );
                }
            }
        }
        else
        {
            pushBack( ch2 );
        }

        return null;
    }

    private Element handleDash()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '-' )
        {
            int ch2 = nextToken();

            if( ch2 == '-' )
            {
                int ch3 = nextToken();

                if( ch3 == '-' ) 
                {
                    // Empty away all the rest of the dashes.
                    // Do not forget to return the first non-match back.
                    while( (ch = nextToken()) == '-' );
                    
                    pushBack(ch);
                    startBlockLevel();
                    pushElement( new Element("hr") );
                    return popElement( "hr" );
                }
        
                pushBack( ch3 );
            }
            pushBack( ch2 );
        }

        pushBack( ch );

        return null;
    }

    private Element handleHeading()
        throws IOException
    {
        Element el = null;
        
        int ch  = nextToken();

        TranslatorReader.Heading hd = new TranslatorReader.Heading();

        if( ch == '!' )
        {
            int ch2 = nextToken();

            if( ch2 == '!' )
            {
                String title = peekAheadLine();
                
                el = makeHeading( TranslatorReader.Heading.HEADING_LARGE, title, hd);
            }
            else
            {
                pushBack( ch2 );
                String title = peekAheadLine();
                el = makeHeading( TranslatorReader.Heading.HEADING_MEDIUM, title, hd );
            }
        }
        else
        {
            pushBack( ch );
            String title = peekAheadLine();
            el = makeHeading( TranslatorReader.Heading.HEADING_SMALL, title, hd );
        }

        callHeadingListenerChain( hd );

        if( el != null ) pushElement(el);
        
        return el;
    }

    /**
     *  Reads the stream until the next EOL or EOF.  Note that it will also read the
     *  EOL from the stream.
     */
    private StringBuffer readUntilEOL()
        throws IOException
    {
        int ch;
        StringBuffer buf = new StringBuffer();

        while( true )
        {
            ch = nextToken();

            if( ch == -1 )
                break;

            buf.append( (char) ch );

            if( ch == '\n' ) 
                break;
        }

        return buf;
    }

    /**
     *  Starts a block level element, therefore closing the
     *  a potential open paragraph tag.
     */
    private String startBlockLevel()
    {
        if( m_isOpenParagraph )
        {
            m_isOpenParagraph = false;
            popElement("p");
            m_plainTextBuf.append("\n");
        }

        return "";
    }

    /**
     *  Like original handleOrderedList() and handleUnorderedList()
     *  however handles both ordered ('#') and unordered ('*') mixed together.
     */

    // FIXME: Refactor this; it's a bit messy.

    private String handleGeneralList()
        throws IOException
    {
         StringBuffer buf = new StringBuffer();

         buf.append( startBlockLevel() );

         String strBullets = readWhile( "*#" );
         // String strBulletsRaw = strBullets;      // to know what was original before phpwiki style substitution
         int numBullets = strBullets.length();

         // override the beginning portion of bullet pattern to be like the previous
         // to simulate PHPWiki style lists

         if(m_allowPHPWikiStyleLists)
         {
             // only substitute if different
             if(!( strBullets.substring(0,Math.min(numBullets,m_genlistlevel)).equals
                   (m_genlistBulletBuffer.substring(0,Math.min(numBullets,m_genlistlevel)) ) ) )
             {
                 if(numBullets <= m_genlistlevel)
                 {
                     // Substitute all but the last character (keep the expressed bullet preference)
                     strBullets  = (numBullets > 1 ? m_genlistBulletBuffer.substring(0, numBullets-1) : "")
                                   + strBullets.substring(numBullets-1, numBullets);
                 }
                 else
                 {
                     strBullets = m_genlistBulletBuffer + strBullets.substring(m_genlistlevel, numBullets);
                 }
             }
         }

         //
         //  Check if this is still of the same type
         //
         if( strBullets.substring(0,Math.min(numBullets,m_genlistlevel)).equals
            (m_genlistBulletBuffer.substring(0,Math.min(numBullets,m_genlistlevel)) ) )
         {
             if( numBullets > m_genlistlevel )
             {
                 buf.append( m_renderer.openList(strBullets.charAt(m_genlistlevel++)) );

                 for( ; m_genlistlevel < numBullets; m_genlistlevel++ )
                 {
                     // bullets are growing, get from new bullet list
                     buf.append( m_renderer.openListItem() );
                     buf.append( m_renderer.openList(strBullets.charAt(m_genlistlevel)) );
                 }
             }
             else if( numBullets < m_genlistlevel )
             {
                 //  Close the previous list item.
                 buf.append( m_renderer.closeListItem() );

                 for( ; m_genlistlevel > numBullets; m_genlistlevel-- )
                 {
                     // bullets are shrinking, get from old bullet list
                     buf.append( m_renderer.closeList(m_genlistBulletBuffer.charAt(m_genlistlevel - 1)) );
                     if( m_genlistlevel > 0 ) buf.append( m_renderer.closeListItem() );

                 }
             }
             else
             {
                 if( m_genlistlevel > 0 ) buf.append( m_renderer.closeListItem() );
             }
         }
         else
         {
             //
             //  The pattern has changed, unwind and restart
             //
             int  numEqualBullets;
             int  numCheckBullets;

             // find out how much is the same
             numEqualBullets = 0;
             numCheckBullets = Math.min(numBullets,m_genlistlevel);

             while( numEqualBullets < numCheckBullets )
             {
                 // if the bullets are equal so far, keep going
                 if( strBullets.charAt(numEqualBullets) == m_genlistBulletBuffer.charAt(numEqualBullets))
                     numEqualBullets++;
                 // otherwise giveup, we have found how many are equal
                 else
                     break;
             }

             //unwind
             for( ; m_genlistlevel > numEqualBullets; m_genlistlevel-- )
             {
                 buf.append( m_renderer.closeList( m_genlistBulletBuffer.charAt(m_genlistlevel - 1) ) );
                 if( m_genlistlevel > 0 ) buf.append( m_renderer.closeListItem() );
             }

             //rewind
             buf.append( m_renderer.openList( strBullets.charAt(numEqualBullets++) ) );
             for(int i = numEqualBullets; i < numBullets; i++)
             {
                 buf.append( m_renderer.openListItem() );
                 buf.append( m_renderer.openList( strBullets.charAt(i) ) );
             }
             m_genlistlevel = numBullets;
         }
         buf.append( m_renderer.openListItem() );

         // work done, remember the new bullet list (in place of old one)
         m_genlistBulletBuffer.setLength(0);
         m_genlistBulletBuffer.append(strBullets);

         return buf.toString();
    }

    private String unwindGeneralList()
    {
        // String cStrShortName = "unwindGeneralList()";

        StringBuffer buf = new StringBuffer();

        //unwind
        for( ; m_genlistlevel > 0; m_genlistlevel-- )
        {
            buf.append(m_renderer.closeListItem());
            buf.append( m_renderer.closeList( m_genlistBulletBuffer.charAt(m_genlistlevel - 1) ) );
        }

        m_genlistBulletBuffer.setLength(0);

        return buf.toString();
    }


    private Element handleDefinitionList()
        throws IOException
    {
        if( !m_isdefinition )
        {
            m_isdefinition = true;

            startBlockLevel();
            
            pushElement( new Element("dl") );
            return pushElement( new Element("dt") ); 
        }

        return null;
    }

    private Element handleOpenbracket()
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int ch;
        boolean isPlugin = false;

        while( (ch = nextToken()) == '[' )
        {
            sb.append( (char)ch );
        }

        if( ch == '{' )
        {
            isPlugin = true;
        }

        pushBack( ch );

        if( sb.length() > 0 )
        {
            return addElement( new Text(sb.toString()) );
        }

        //
        //  Find end of hyperlink
        //

        ch = nextToken();

        while( ch != -1 )
        {
            if( ch == ']' && (!isPlugin || sb.charAt( sb.length()-1 ) == '}' ) )
            {
                break;
            }

            sb.append( (char) ch );

            ch = nextToken();
        }

        if( ch == -1 )
        {
            log.debug("Warning: unterminated link detected!");
            return null;
        }

        return handleHyperlinks( sb.toString() );
    }

    /**
     *  Reads the stream until the current brace is closed or stream end. 
     */
    private String readBraceContent( char opening, char closing )
        throws IOException
    {
        StringBuffer sb = new StringBuffer();
        int braceLevel = 1;    
        int ch;        
        while(( ch = nextToken() ) != -1 )
        {
            if( ch == '\\' ) 
            {
                continue;
            }
            else if ( ch == opening ) 
            {
                braceLevel++;
            }
            else if ( ch == closing ) 
            {
                braceLevel--;
                if (braceLevel==0) 
                {
                  break;
                }
            }
            sb.append( (char)ch );
        }    
        return sb.toString();
    }

    
    /**
     *  Handles constructs of type %%(style) and %%class
     * @param newLine
     * @return
     * @throws IOException
     */
    private Element handleDiv( boolean newLine )
        throws IOException
    {
        int ch = nextToken();
        Element el = null;
        
        if( ch == '%' )
        {
            String style = null;
            String clazz = null;

            ch = nextToken();
            
            //
            //  Style or class?
            //
            if( ch == '(' )
            {                
                style = readBraceContent('(',')');
            }
            else if( Character.isLetter( (char) ch ) )
            {
                pushBack( ch );
                clazz = readUntil( " \t\n\r" );
                ch = nextToken();
                
                //
                //  Pop out only spaces, so that the upcoming EOL check does not check the
                //  next line.
                //
                if( ch == '\n' || ch == '\r' )
                {
                    pushBack(ch);
                }
            }
            else
            {
                //
                // Anything else stops.
                //

                pushBack(ch);
                
                try
                {
                    Boolean isSpan = (Boolean)m_styleStack.pop();
                
                    if( isSpan == null )
                    {
                        // Fail quietly
                    }
                    else if( isSpan.booleanValue() )
                    {
                        el = popElement( "span" );
                    }
                    else
                    {
                        el = popElement( "div" );
                    }
                }
                catch( EmptyStackException e )
                {
                    log.debug("Page '"+m_context.getPage().getName()+"' closes a %%-block that has not been opened.");
                }
                
                return el;
            }

            //
            //  Decide if we should open a div or a span?
            //
            String eol = peekAheadLine();
            
            if( eol.trim().length() > 0 )
            {
                // There is stuff after the class
                
                el = new Element("span");
                
                m_styleStack.push( Boolean.TRUE );
            }
            else
            {
                startBlockLevel();
                el = new Element("div");
                m_styleStack.push( Boolean.FALSE );
            }

            if( style != null ) el.setAttribute("style", style);
            if( clazz != null ) el.setAttribute("class", clazz );
            el = pushElement( el );
            
            return el;
        }

        pushBack(ch);

        return el;
    }

    private Element handleBar( boolean newLine )
        throws IOException
    {
        Element el = null;
        
        if( !m_istable && !newLine )
        {
            return null;
        }

        if( newLine )
        {
            if( !m_istable )
            {
                startBlockLevel();
                el = pushElement( new Element("table").setAttribute("class","wikitable").setAttribute("border","1") );
                m_istable = true;
            }

            el = pushElement( new Element("tr") );
            // m_closeTag = m_renderer.closeTableItem()+m_renderer.closeTableRow();
        }
        
        int ch = nextToken();

        if( ch == '|' )
        {
            if( !newLine ) 
            {
                el = popElement("th");
            }
            el = pushElement( new Element("th") );
        }
        else
        {
            if( !newLine ) 
            {
                el = popElement("td");
            }
            
            el = pushElement( new Element("td") );

            pushBack( ch );
        }

        return el;
    }

    /**
     *  Generic escape of next character or entity.
     */
    private Element handleTilde()
        throws IOException
    {
        int ch = nextToken();

        if( ch == '|' || ch == '~' || ch == '\\' || ch == '*' || ch == '#' || 
            ch == '-' || ch == '!' || ch == '\'' || ch == '_' || ch == '[' ||
            ch == '{' || ch == ']' || ch == '}' )
        {
            m_plainTextBuf.append( (char)ch );
            m_plainTextBuf.append(readWhile( ""+(char)ch ));
            return m_currentElement;
        }
        
        if( Character.isUpperCase( (char) ch ) )
        {
            pushBack( ch );
            return m_currentElement;
        }

        // No escape.
        pushBack( ch );

        return null;
    }
   
    private void fillBuffer( Element startElement )
        throws IOException
    {
        StringBuffer word = null;
        int previousCh = -2;
        int start = 0;
        
        m_currentElement = startElement;
        
        boolean quitReading = false;
        boolean newLine     = true; // FIXME: not true if reading starts in middle of buffer

        while(!quitReading)
        {
            int ch = nextToken();
            String s = null;
            Element el = null;
            
            //
            //  Check if we're actually ending the preformatted mode.
            //  We still must do an entity transformation here.
            //
            if( m_isEscaping )
            {
                if( ch == '}' )
                {
                    if( handleClosebrace() == null ) m_plainTextBuf.append( (char) ch );
                }
                else if( ch == -1 )
                {
                    quitReading = true;
                }
                else 
                {
                    m_plainTextBuf.append( (char) ch );
                }

                continue;
            }

            //
            //  CamelCase detection, a non-trivial endeavour.
            //  We keep track of all white-space separated entities, which we
            //  hereby refer to as "words".  We then check for an existence
            //  of a CamelCase format text string inside the "word", and
            //  if one exists, we replace it with a proper link.
            //
            
            if( m_camelCaseLinks )
            {
                // Quick parse of start of a word boundary.

                if( word == null &&                    
                    (Character.isWhitespace( (char)previousCh ) ||
                     WORD_SEPARATORS.indexOf( (char)previousCh ) != -1 ||
                     newLine ) &&
                    !Character.isWhitespace( (char) ch ) )
                {
                    word = new StringBuffer();
                }

                // Are we currently tracking a word?
                if( word != null )
                {
                    //
                    //  Check for the end of the word.
                    //

                    if( Character.isWhitespace( (char)ch ) || 
                        ch == -1 ||
                        WORD_SEPARATORS.indexOf( (char) ch ) != -1 )
                    {
                        String potentialLink = word.toString();

                        String camelCase = checkForCamelCaseLink(potentialLink);

                        if( camelCase != null )
                        {
                            // System.out.println("Buffer is "+buf);

                            // System.out.println("  Replacing "+camelCase+" with proper link.");
                            start = m_plainTextBuf.toString().lastIndexOf( camelCase );
                            m_plainTextBuf.replace(start,
                                                   start+camelCase.length(),
                                                   makeCamelCaseLink(camelCase) );
                            // FIXME: This strategy does not work
                            // System.out.println("  Resulting with "+buf);
                        }
                        else
                        {
                            // System.out.println("Checking for potential URI: "+potentialLink);
                            if( isExternalLink( potentialLink ) )
                            {
                                // System.out.println("buf="+buf);
                                start = m_plainTextBuf.toString().lastIndexOf( potentialLink );

                                if( start >= 0 )
                                {
                                    String link = readUntil(" \t()[]{}!\"'\n|");

                                    link = potentialLink + (char)ch + link; // Do not forget the start.

                                    // System.out.println("start="+start+", pl="+potentialLink);

                                    /*
                                     FIXME!
                                    m_plainTextBuf.replace( start,
                                                            start + potentialLink.length(),
                                                            makeDirectURILink( link ) );
*/
                                    // System.out.println("Resulting with "+buf);

                                    ch = nextToken();
                                }
                            }
                        }

                        // We've ended a word boundary, so time to reset.
                        word = null;
                    }
                    else
                    {
                        // This should only be appending letters and digits.
                        word.append( (char)ch );
                    } // if end of word
                } // if word's not null

                // Always set the previous character to test for word starts.
                previousCh = ch;
         
            } // if m_camelCaseLinks

            //
            //  An empty line stops a list
            //
            if( newLine && ch != '*' && ch != '#' && ch != ' ' && m_genlistlevel > 0 )
            {
                m_plainTextBuf.append(unwindGeneralList());
            }

            if( newLine && ch != '|' && m_istable )
            {
                el = popElement("table");
                m_istable = false;
                m_closeTag = null;
            }

            //
            //  Now, check the incoming token.
            //
            switch( ch )
            {
              case '\r':
                // DOS linefeeds we forget
                s = null;
                el = m_currentElement;
                break;

              case '\n':
                //
                //  Close things like headings, etc.
                //
                if( m_closeTag != null ) 
                {
                    m_plainTextBuf.append( m_closeTag );
                    m_closeTag = null;
                }

                // FIXME: This is not really very fast
                popElement("dl"); // Close definition lists.
                popElement("h2");
                popElement("h3");
                popElement("h4");
                if( m_istable ) 
                { 
                    popElement("tr");
                }
                
                m_isdefinition = false;

                if( newLine )
                {
                    // Paragraph change.
                    startBlockLevel();

                    //
                    //  Figure out which elements cannot be enclosed inside
                    //  a <p></p> pair according to XHTML rules.
                    //
                    String nextLine = peekAheadLine();
                    if( nextLine.length() == 0 || 
                        (nextLine.length() > 0 &&
                         !nextLine.startsWith("{{{") &&
                         !nextLine.startsWith("----") &&
                         !nextLine.startsWith("%%") &&
                         "*#!;".indexOf( nextLine.charAt(0) ) == -1) )
                    {
                        pushElement( new Element("p") );
                        m_isOpenParagraph = true;
                    }
                }
                else
                {
                    m_plainTextBuf.append("\n");
                    newLine = true;
                }
                continue;
                

              case '\\':
                el = handleBackslash();
                break;

              case '_':
                el = handleUnderscore();
                break;
                
              case '\'':
                el = handleApostrophe();
                break;

              case '{':
                el = handleOpenbrace( newLine );
                break;

              case '}':
                el = handleClosebrace();
                break;

              case '-':
                el = handleDash();
                break;

              case '!':
                if( newLine )
                {
                    el = handleHeading();
                }
                else
                {
                    s = "!";
                }
                break;

              case ';':
                if( newLine )
                {
                    el = handleDefinitionList();
                }
                /*
                else
                {
                    s = ";";
                }
                */
                break;

              case ':':
                if( m_isdefinition )
                {
                    popElement("dt");
                    el = pushElement( new Element("dd") );
                    m_isdefinition = false;
                }
                /*
                else
                {
                    s = ":";
                }
                */
                break;

              case '[':
                el = handleOpenbracket();
                break;

              case '*':
                if( newLine )
                {
                    pushBack('*');
                    s = handleGeneralList();
                }
                /*
                else
                {
                    s = "*";
                }
                */
                break;

              case '#':
                if( newLine )
                {
                    pushBack('#');
                    s = handleGeneralList();
                }
                /*
                else
                {
                    s = "#";
                }
                */
                break;

              case '|':
                el = handleBar( newLine );
                break;
/*
              case '<':
                s = m_allowHTML ? "<" : "&lt;";
                break;

              case '>':
                s = m_allowHTML ? ">" : "&gt;";
                break;

              case '\"':
                s = m_allowHTML ? "\"" : "&quot;";
                break;
*/
                /*
              case '&':
                s = "&amp;";
                break;
                */
              case '~':
                el = handleTilde();
                break;

              case '%':
                el = handleDiv( newLine );
                break;

              case -1:
                if( m_closeTag != null )
                {
                    m_plainTextBuf.append( m_closeTag );
                    m_closeTag = null;
                }
                quitReading = true;
                continue;
/*
              default:
                m_plainTextBuf.append( (char)ch );
                newLine = false;
                break;
*/
            }

            //
            //   The idea is as follows:  If the handler method returns
            //   an element (el != null), it is assumed that it has been
            //   added in the stack.  Otherwise the character is added
            //   as is to the plaintext buffer.
            //
            //   For the transition phase, if s != null, it also gets
            //   added in the plaintext buffer.
            //
            if( el != null )
            {
                newLine = false;
            }
            else
            {
                m_plainTextBuf.append( (char) ch );
                newLine = false;
            }
            
            if( s != null )
            {
                m_plainTextBuf.append( s );
                newLine = false;
            }
        }
        
        popElement("domroot");
    }

    public WikiDocument parse()
        throws IOException
    {
        Element rootElement = new Element("domroot");
        
        m_document.setRootElement( rootElement );
        fillBuffer( rootElement );
        
        WikiDocument d = new WikiDocument( m_context.getPage(), m_document );
        
        return d;
    }
    

    /**
     *  All HTML output stuff is here.  This class is a helper class, and will
     *  be spawned later on with a proper API of its own so that we can have
     *  different kinds of renderers.
     */

    // FIXME: Not everything is yet, and in the future this class will be spawned
    //        out to be its own class.
    private class HTMLRenderer
        extends TextRenderer
    {
        private TranslatorReader m_cleanTranslator;

        /*
           FIXME: It's relatively slow to create two TranslatorReaders each time.
        */
        public HTMLRenderer()
        {
        }

        /**
         *  Does a lazy init.  Otherwise, we would get into a situation
         *  where HTMLRenderer would try and boot a TranslatorReader before
         *  the TranslatorReader it is contained by is up.
         */
        private TranslatorReader getCleanTranslator()
        {
            if( m_cleanTranslator == null )
            {
                WikiContext dummyContext = new WikiContext( m_engine, 
                                                            m_context.getPage() );
                m_cleanTranslator = new TranslatorReader( dummyContext, 
                                                          null );
                // m_cleanTranslator.m_allowHTML = true;
            }

            return m_cleanTranslator;
        }

        public void doChar( StringBuffer buf, char ch )
        {
            if( ch == '<' )
            {
                buf.append("&lt;");
            }
            else if( ch == '>' )
            {
                buf.append("&gt;");
            }
            else if( ch == '&' )
            {
                buf.append("&amp;");
            }
            else
            {
                buf.append( ch );
            }
        }

        


        /**
         *  Write a HTMLized link depending on its type.
         *
         *  <p>This jsut calls makeLink() with "section" set to null.
         */
        public String makeLink( int type, String link, String text )
        {
            return makeLink( type, link, text, null );
        }

        private final String getURL( String context, String link )
        {
            return m_context.getURL( context,
                                     link,
                                     null );
        }

        /**
         *  Writes HTML for error message.
         */

        public String makeError( String error )
        {
            return "<span class=\"error\">"+error+"</span>";
        }

        /**
         *  Emits a vertical line.
         */

        public String makeRuler()
        {
            return "<hr />";
        }

        /**
         *  Modifies the "hd" parameter to contain proper values.  Because
         *  an "id" tag may only contain [a-zA-Z0-9:_-], we'll replace the
         *  % after url encoding with '_'.
         */
        private String makeHeadingAnchor( String baseName, String title, TranslatorReader.Heading hd )
        {
            hd.m_titleText = title;
            title = cleanLink( title );
            hd.m_titleSection = m_engine.encodeName(title);
            hd.m_titleAnchor = "section-"+m_engine.encodeName(baseName)+
                               "-"+hd.m_titleSection;
            
            hd.m_titleAnchor = hd.m_titleAnchor.replace( '%', '_' );
            return hd.m_titleAnchor;
        }

        private String makeSectionTitle( String title )
        {
            title = title.trim();

            StringWriter outTitle = new StringWriter();

            try
            {
                TranslatorReader read = getCleanTranslator();
                read.setInputReader( new StringReader(title) );
                FileUtil.copyContents( read, outTitle );
            }
            catch( IOException e )
            {
                log.fatal("CleanTranslator not working", e);
                throw new InternalWikiException("CleanTranslator not working as expected, when cleaning title"+ e.getMessage() );
            }

            return outTitle.toString();
        }
        
        /**
         *  Returns XHTML for the start of the heading.  Also sets the
         *  line-end emitter.
         *  @param level 
         *  @param headings A List to which heading should be added.
         */ 
        public String makeHeading( int level, String title, TranslatorReader.Heading hd )
        {
            String res = "";

            String pageName = m_context.getPage().getName();

            String outTitle = makeSectionTitle( title );

            hd.m_level = level;

            switch( level )
            {
              case TranslatorReader.Heading.HEADING_SMALL:
                res = "<h4 id='"+makeHeadingAnchor( pageName, outTitle, hd )+"'>";
                m_closeTag = "</h4>";
                break;

              case TranslatorReader.Heading.HEADING_MEDIUM:
                res = "<h3 id='"+makeHeadingAnchor( pageName, outTitle, hd )+"'>";
                m_closeTag = "</h3>";
                break;

              case TranslatorReader.Heading.HEADING_LARGE:
                res = "<h2 id='"+makeHeadingAnchor( pageName, outTitle, hd )+"'>";
                m_closeTag = "</h2>";
                break;
            }

            return res;
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        public String openList( char bullet )
        {
            String res = "";

            if( bullet == '#' )
                res = "<ol>\n";
            else if( bullet == '*' )
                res = "<ul>\n";
            else
                log.info("Warning: unknown bullet character '" + bullet + "' at (+)" );

            return res;
        }

        public String openListItem()
        {
            return "<li>";
        }

        public String closeListItem()
        {
            return "</li>\n";
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        public String closeList( char bullet )
        {
            String res = "";

            if( bullet == '#' )
            {
                res = "</ol>\n";
            }
            else if( bullet == '*' )
            {
                res = "</ul>\n";
            }
            else
            {
                //FIXME unknown character -> error
                log.info("Warning: unknown character in unwind '" + bullet + "'" );
            }

            return res;
        }


        public String openPreformatted( boolean isBlock )
        {
            m_isPreBlock = isBlock;

            if( isBlock )
            {
                return "<pre>";
            }
            
            return "<span style=\"font-family:monospace; whitespace:pre;\">";
        }

        public String closePreformatted()
        {
            if( m_isPreBlock )
                return "</pre>\n";
            
            return "</span>";
        }

        /**
         *  If outlink images are turned on, returns a link to the outward
         *  linking image.
         */
        public String outlinkImage()
        {
            if( m_useOutlinkImage )
            {
                return "<img class=\"outlink\" src=\""+
                       getURL( WikiContext.NONE,"images/out.png" )+"\" alt=\"\" />";
            }

            return "";
        }

        /**
         *  @param clear If true, then flushes all thingies.
         */
        public String lineBreak( boolean clear )
        {
            if( clear )
                return "<br clear=\"all\" />";
            
            return "<br />";
        }

    } // HTMLRenderer

    /**
     *  A very simple class for outputting plain text with no
     *  formatting.
     */
    private class TextRenderer
    {
        public void doChar( StringBuffer buf, char ch )
        {
            buf.append( ch );
        }
        

        /**
         *  Write a HTMLized link depending on its type.
         *
         *  <p>This jsut calls makeLink() with "section" set to null.
         */
        public String makeLink( int type, String link, String text )
        {
            return text;
        }

        public String makeLink( int type, String link, String text, String section )
        {
            return text;
        }

        /**
         *  Writes HTML for error message.
         */

        public String makeError( String error )
        {
            return "ERROR: "+error;
        }

        /**
         *  Emits a vertical line.
         */

        public String makeRuler()
        {
            return "----------------------------------";
        }

        /**
         *  Returns XHTML for the start of the heading.  Also sets the
         *  line-end emitter.
         *  @param level 
         */ 
        public String makeHeading( int level, String title, TranslatorReader.Heading hd )
        {
            String res = "";

            title = title.trim();

            hd.m_level = level;
            hd.m_titleText = title;
            hd.m_titleSection = "";
            hd.m_titleAnchor = "";

            switch( level )
            {
              case TranslatorReader.Heading.HEADING_SMALL:
                res = title;
                m_closeTag = "\n\n";
                break;

              case TranslatorReader.Heading.HEADING_MEDIUM:
                res = title;                
                m_closeTag = "\n"+TextUtil.repeatString("-",title.length())+"\n\n";
                break;

              case TranslatorReader.Heading.HEADING_LARGE:
                res = title.toUpperCase();
                m_closeTag= "\n"+TextUtil.repeatString("=",title.length())+"\n\n";
                break;
            }

            return res;
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        // FIXME: Should really start a different kind of list depending
        //        on the bullet type
        public String openList( char bullet )
        {
            return "\n";
        }

        public String openListItem()
        {
            return "- "; 
        }

        public String closeListItem()
        {
            return "\n";
        }

        /**
         *  @param bullet A character detailing which kind of a list
         *  we are dealing with here.  Options are '#' and '*'.
         */
        public String closeList( char bullet )
        {
            return "\n\n";
        }

        public String openPreformatted( boolean isBlock )
        {
            return "";
        }

        public String closePreformatted()
        {
            return "\n";
        }

        /**
         *  If outlink images are turned on, returns a link to the outward
         *  linking image.
         */
        public String outlinkImage()
        {
            return "";
        }


    } // TextRenderer

    /**
     *  Compares two Strings, and if one starts with the other, then
     *  returns null.  Otherwise just like the normal Comparator
     *  for strings.
     *  
     *  @author jalkanen
     *
     *  @since
     */
    private static class StartingComparator implements Comparator
    {
        public int compare( Object arg0, Object arg1 )
        {
            String s1 = (String)arg0;
            String s2 = (String)arg1;
            
            if( s1.length() > s2.length() )
            {
                if( s1.startsWith(s2) && s2.length() > 1 ) return 0;
            }
            else
            {
                if( s2.startsWith(s1) && s1.length() > 1 ) return 0;
            }
                
            return s1.compareTo( s2 );
        }
        
    }
    

}

