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
import org.apache.log4j.*;
import org.apache.oro.text.perl.Perl5Util;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import com.ecyrd.jspwiki.plugin.PluginManager;
import com.ecyrd.jspwiki.rss.RSSGenerator;
import com.ecyrd.jspwiki.providers.WikiPageProvider;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.attachment.AttachmentManager;
import com.ecyrd.jspwiki.auth.*;
import com.ecyrd.jspwiki.attachment.Attachment;

/**
 *  Provides Wiki services to the JSP page.
 *
 *  <P>
 *  This is the main interface through which everything should go.
 *
 *  <P>
 *  Using this class:  Always get yourself an instance from JSP page
 *  by using the WikiEngine.getInstance() method.  Never create a new
 * WikiEngine() from scratch, unless you're writing tests.
 *
 *  @author Janne Jalkanen
 */
public class WikiEngine
{
    private static final Category   log = Category.getInstance(WikiEngine.class);

    /** True, if log4j has been configured. */
    // FIXME: If you run multiple applications, the first application
    // to run defines where the log goes.  Not what we want.
    private static boolean   c_configured = false;

    /** Stores properties. */
    private Properties       m_properties;

    public static final String PROP_INTERWIKIREF = "jspwiki.interWikiRef.";

    /** If true, then the user name will be stored with the page data.*/
    public static final String PROP_STOREUSERNAME= "jspwiki.storeUserName";

    /** If true, logs the IP address of the editor on saving. */
    public static final String PROP_STOREIPADDRESS= "jspwiki.storeIPAddress";

    /** Define the used encoding.  Currently supported are ISO-8859-1 and UTF-8 */
    public static final String PROP_ENCODING     = "jspwiki.encoding";

    /** The name for the base URL to use in all references. */
    public static final String PROP_BASEURL      = "jspwiki.baseURL";

    /** Property name for the "spaces in titles" -hack. */
    public static final String PROP_BEAUTIFYTITLE = "jspwiki.breakTitleWithSpaces";

    /** The name of the cookie that gets stored to the user browser. */
    public static final String PREFS_COOKIE_NAME = "JSPWikiUserProfile";

    /** Property name for the "match english plurals" -hack. */
    public static final String PROP_MATCHPLURALS     = "jspwiki.translatorReader.matchEnglishPlurals";
    /** Property name for the template that is used. */
    public static final String PROP_TEMPLATEDIR  = "jspwiki.templateDir";

    /** Property name for the default front page. */
    public static final String PROP_FRONTPAGE    = "jspwiki.frontPage";

    /** The name the UserProfile is stored in a Session by. */
    public static final String WIKIUSER = "wup";   

    /** Stores an internal list of engines per each ServletContext */
    private static Hashtable c_engines = new Hashtable();

    /** Should the user info be saved with the page data as well? */
    private boolean          m_saveUserInfo = true;

    /** If true, logs the IP address of the editor */
    private boolean          m_storeIPAddress = true;

    /** If true, uses UTF8 encoding for all data */
    private boolean          m_useUTF8      = true;

    /** If true, we'll also consider english plurals (+s) a match. */
    private boolean          m_matchEnglishPlurals = true;

    /** Stores the base URL. */
    private String           m_baseURL;

    /** Store the file path to the basic URL.  When we're not running as
        a servlet, it defaults to the user's current directory. */
    private String           m_rootPath = System.getProperty("user.dir");

    /** Stores references between wikipages. */
    private ReferenceManager m_referenceManager = null;

    /** Stores the Plugin manager */
    private PluginManager    m_pluginManager;

    /** Stores the Variable manager */
    private VariableManager  m_variableManager;

    /** Stores the Attachment manager */
    private AttachmentManager m_attachmentManager = null;

    /** Stores the Authenticator */
    private Authenticator m_authenticator = null;

    /** Stores the Authorizer */
    private Authorizer m_authorizer = null;

    /** Stores the Page manager */
    private PageManager      m_pageManager = null;

    /** Does all our diffs for us. */
    private DifferenceEngine m_differenceEngine;

    /** Generates RSS feed when requested. */
    private RSSGenerator     m_rssGenerator;

    /** Stores the relative URL to the global RSS feed. */
    private String           m_rssURL;

    /** Store the ServletContext that we're in.  This may be null if WikiEngine
        is not running inside a servlet container (i.e. when testing). */
    private ServletContext   m_servletContext = null;

    /** If true, all titles will be cleaned. */
    private boolean          m_beautifyTitle = false;

    /** Stores the template path.  This is relative to "templates". */
    private String           m_templateDir;

    /** The default front page name.  Defaults to "Main". */
    private String           m_frontPage;

    /** The default flags used when running data through a TranslatorReader. */
    private static final int DEFAULT_TRANSLATION_FLAGS = 0x0;



    /**
     *  Gets a WikiEngine related to this servlet.  Since this method
     *  is only called from JSP pages (and JspInit()) to be specific,
     *  we throw a RuntimeException if things don't work.
     *  
     *  @throws InternalWikiException in case something fails.  This
     *          is a RuntimeException, so be prepared for it.
     */

    // FIXME: It seems that this does not work too well, jspInit()
    // does not react to RuntimeExceptions, or something...

    public static synchronized WikiEngine getInstance( ServletConfig config )
        throws InternalWikiException
    {
        ServletContext context = config.getServletContext();        
        String appid = Integer.toString(context.hashCode()); //FIXME: Kludge, use real type.

        config.getServletContext().log( "Application "+appid+" requests WikiEngine.");

        WikiEngine engine = (WikiEngine) c_engines.get( appid );

        if( engine == null )
        {
            context.log(" Assigning new log to "+appid);
            try
            {
                engine = new WikiEngine( config.getServletContext() );
            }
            catch( Exception e )
            {
                context.log( "ERROR: Failed to create a Wiki engine" );
                throw new InternalWikiException( "No wiki engine, check logs." );
            }

            c_engines.put( appid, engine );
        }

        return engine;
    }

    /**
     *  Instantiate the WikiEngine using a given set of properties.
     *  Use this constructor for testing purposes only.
     */
    public WikiEngine( Properties properties )
        throws WikiException
    {
        initialize( properties );
    }

    /**
     *  Instantiate using this method when you're running as a servlet and
     *  WikiEngine will figure out where to look for the property
     *  file.
     *  Do not use this method - use WikiEngine.getInstance() instead.
     */
    protected WikiEngine( ServletContext context )
        throws WikiException
    {
        m_servletContext = context;

        InputStream propertyStream = context.getResourceAsStream("/WEB-INF/jspwiki.properties");

        Properties props = new Properties();

        try
        {
            //
            //  Note: May be null, if JSPWiki has been deployed in a WAR file.
            //
            m_rootPath = context.getRealPath("/");

            props.load( propertyStream );

            initialize( props );

            log.debug("Root path for this Wiki is: '"+m_rootPath+"'");
        }
        catch( Exception e )
        {
            context.log( Release.APPNAME+": Unable to load and setup properties from jspwiki.properties. "+e.getMessage() );
        }
    }

    /**
     *  Does all the real initialization.
     */
    private void initialize( Properties props )
        throws WikiException
    {
        m_properties = props;
        //
        //  Initialized log4j.  However, make sure that
        //  we don't initialize it multiple times.  Also, if
        //  all of the log4j statements have been removed from
        //  the property file, we do not do any property setting
        //  either.q
        //
        if( !c_configured )
        {
            if( props.getProperty("log4j.rootCategory") != null )
            {
                PropertyConfigurator.configure( props );
            }
            c_configured = true;
        }

        log.info("*******************************************");
        log.info("JSPWiki "+Release.VERSTR+" starting. Whee!");

        log.debug("Configuring WikiEngine...");

        m_saveUserInfo   = "true".equals( props.getProperty( PROP_STOREUSERNAME, "true" ) );
        m_storeIPAddress = "true".equals( props.getProperty( PROP_STOREIPADDRESS, "true" ) );

        m_useUTF8        = "UTF-8".equals( props.getProperty( PROP_ENCODING, "ISO-8859-1" ) );
        m_baseURL        = props.getProperty( PROP_BASEURL, "" );

        m_beautifyTitle  = "true".equals( props.getProperty( PROP_BEAUTIFYTITLE, "false" ) );

        m_matchEnglishPlurals = "true".equals( props.getProperty( PROP_MATCHPLURALS, "false" ) );
        m_templateDir    = props.getProperty( PROP_TEMPLATEDIR, "default" );
        m_frontPage      = props.getProperty( PROP_FRONTPAGE,   "Main" );

        //
        //  Initialize the important modules.  Any exception thrown by the
        //  managers means that we will not start up.
        //
        try
        {
            m_pageManager       = new PageManager( props );
            m_pluginManager     = new PluginManager( props );
            m_differenceEngine  = new DifferenceEngine( props, getContentEncoding() );
            m_attachmentManager = new AttachmentManager( props );
            m_variableManager   = new VariableManager( props );
            m_authenticator     = new Authenticator( props );
            m_authorizer        = new Authorizer( props );

            initReferenceManager();            
        }
        catch( Exception e )
        {
            // RuntimeExceptions may occur here, even if they shouldn't.
            log.error( "Failed to start managers.", e );
            throw new WikiException( "Failed to start managers: "+e.getMessage() );
        }

        //
        //  Initialize the good-to-have-but-not-fatal modules.
        //
        try
        {
            if( props.getProperty( RSSGenerator.PROP_GENERATE_RSS, "false" ).equalsIgnoreCase("true") )
            {
                m_rssGenerator = new RSSGenerator( this, props );
            }
        }
        catch( Exception e )
        {
            log.error( "Unable to start RSS generator - JSPWiki will still work, "+
                       "but there will be no RSS feed.", e );
        }

        // FIXME: I wonder if this should be somewhere else.
        if( m_rssGenerator != null )
        {
            new RSSThread().start();
        }

        log.info("WikiEngine configured.");
    }

    /**
     *  Initializes the reference manager. Scans all existing WikiPages for
     *  internal links and adds them to the ReferenceManager object.
     */
    private void initReferenceManager()
    {
        long start = System.currentTimeMillis();
        log.info( "Starting cross reference scan of WikiPages" );

        try
        {
            Collection pages = m_pageManager.getAllPages();

            // Build a new manager with default key lists.
            if( m_referenceManager == null )
            {
                m_referenceManager = new ReferenceManager( this, pages );
            }
        
            // Scan the existing pages from disk and update references in the manager.
            Iterator it = pages.iterator();
            while( it.hasNext() )
            {
                WikiPage page  = (WikiPage)it.next();
                String content = m_pageManager.getPageText( page.getName(), 
                                                            WikiPageProvider.LATEST_VERSION );
                m_referenceManager.updateReferences( page.getName(), 
                                                     scanWikiLinks( content ) );
            }
        }
        catch( ProviderException e )
        {
            log.fatal("PageProvider is unable to list pages: ", e);
        }

        log.info( "Cross reference scan done (" +
                  (System.currentTimeMillis()-start) +
                  " ms)" );
    }



    /**
     *  Throws an exception if a property is not found.
     */
    public static String getRequiredProperty( Properties props, String key )
        throws NoRequiredPropertyException
    {
        String value = props.getProperty(key);

        if( value == null )
        {
            throw new NoRequiredPropertyException( "Required property not found",
                                                   key );
        }

        return value;
    }

    /**
     *  Internal method for getting a property.  This is used by the
     *  TranslatorReader for example.
     */
    public Properties getWikiProperties()
    {
        return m_properties;
    }

    /**
     *  @since 1.8.0
     */
    public String getPluginSearchPath()
    {
        // FIXME: This method should not be here, probably.
        return m_properties.getProperty( PluginManager.PROP_SEARCHPATH );
    }

    /**
     *  Returns the current template directory.
     *
     *  @since 1.9.20
     */
    public String getTemplateDir()
    {
        return m_templateDir;
    }

    /**
     *  Returns the base URL.  Always prepend this to any reference
     *  you make.
     *
     *  @since 1.6.1
     */

    public String getBaseURL()
    {
        return m_baseURL;
    }

    /**
     *  Returns the default front page, if no page is used.
     */

    public String getFrontPage()
    {
        return m_frontPage;
    }

    /**
     *  Returns the ServletContext that this particular WikiEngine was
     *  initialized with.  <B>It may return null</B>, if the WikiEngine is not
     *  running inside a servlet container!
     *
     *  @since 1.7.10
     *  @return ServletContext of the WikiEngine, or null.
     */

    public ServletContext getServletContext()
    {
        return m_servletContext;
    }

    /**
     *  This is a safe version of the Servlet.Request.getParameter() routine.
     *  Unfortunately, the default version always assumes that the incoming
     *  character set is ISO-8859-1, even though it was something else.
     *  This means that we need to make a new string using the correct
     *  encoding.
     *  <P>
     *  For more information, see:
     *     <A HREF="http://www.jguru.com/faq/view.jsp?EID=137049">JGuru FAQ</A>.
     *  <P>
     *  Incidentally, this is almost the same as encodeName(), below.
     *  I am not yet entirely sure if it's safe to merge the code.
     *
     *  @since 1.5.3
     */

    public String safeGetParameter( ServletRequest request, String name )
    {
        try
        {
            String res = request.getParameter( name );
            if( res != null ) 
            {
                res = new String(res.getBytes("ISO-8859-1"),
                                 getContentEncoding() );
            }

            return res;
        }
        catch( UnsupportedEncodingException e )
        {
            log.fatal( "Unsupported encoding", e );
            return "";
        }

    }


    /**
     *  Returns an URL to some other Wiki that we know.
     *
     *  @return null, if no such reference was found.
     */
    public String getInterWikiURL( String wikiName )
    {
        return m_properties.getProperty(PROP_INTERWIKIREF+wikiName);
    }

    /**
     *  Returns a collection of all supported InterWiki links.
     */
    public Collection getAllInterWikiLinks()
    {
        Vector v = new Vector();

        for( Enumeration i = m_properties.propertyNames(); i.hasMoreElements(); )
        {
            String prop = (String) i.nextElement();

            if( prop.startsWith( PROP_INTERWIKIREF ) )
            {
                v.add( prop.substring( prop.lastIndexOf(".")+1 ) );
            }
        }

        return v;
    }

    /**
     *  Returns a collection of all image types that get inlined.
     */

    public Collection getAllInlinedImagePatterns()
    {
        return TranslatorReader.getImagePatterns( this );
    }

    /**
     *  If the page is a special page, then returns a direct URL
     *  to that page.  Otherwise returns null.
     *  <P>
     *  Special pages are non-existant references to other pages.
     *  For example, you could define a special page reference
     *  "RecentChanges" which would always be redirected to "RecentChanges.jsp"
     *  instead of trying to find a Wiki page called "RecentChanges".
     */
    public String getSpecialPageReference( String original )
    {
        String propname = "jspwiki.specialPage."+original;
        String specialpage = m_properties.getProperty( propname );

        return specialpage;
    }

    /**
     *  Returns the name of the application.
     */

    // FIXME: Should use servlet context as a default instead of a constant.
    public String getApplicationName()
    {
        String appName = m_properties.getProperty("jspwiki.applicationName");

        if( appName == null )
            return Release.APPNAME;

        return appName;
    }

    /**
     *  Beautifies the title of the page.
     *
     *  @since 1.7.11
     */
    public String beautifyTitle( String title )
    {
        if( m_beautifyTitle )
        {
            StringBuffer result = new StringBuffer();

            for( int i = 0; i < title.length(); i++ )
            {
                // No space in front of the first line.
                if( Character.isUpperCase(title.charAt(i)) && i > 0 )
                {
                    result.append(' ');
                }

                result.append( title.charAt(i) );
            }
            return result.toString();
            /*
              // FIXME: Should really use a nice regexp for translating
              // JSPWiki into JSP Wiki.

            Perl5Util util = new Perl5Util();
            return util.substitute("s/[:upper:]{1,2}/foo/",title);
            */
        }

        return title;
    }

    /**
     *  Returns true, if the requested page (or an alias) exists.  Will consider
     *  any version as existing.  Will also consider attachments.
     *
     *  @param page WikiName of the page.
     */
    public boolean pageExists( String page )
    {
        if( getSpecialPageReference(page) != null ) return true;

        if( getFinalPageName( page ) != null )
        {
            return true;
        }

        Attachment att = null;

        try
        {
            att = getAttachmentManager().getAttachmentInfo( (WikiContext)null, page );
        }
        catch( ProviderException e )
        {
            log.debug("pageExists() failed to find attachments",e);
        }

        return att != null;
    }

    /**
     *  Returns true, if the requested page (or an alias) exists with the
     *  requested version.
     *
     *  @param page Page name
     */
    public boolean pageExists( String page, int version )
        throws ProviderException
    {
        if( getSpecialPageReference(page) != null ) return true;

        String finalName = getFinalPageName( page );
        WikiPage p = null;

        if( finalName != null )
        {
            //
            //  Go and check if this particular version of this page
            //  exists.
            //
            p = m_pageManager.getPageInfo( finalName, version );
        }

        if( p == null )
        {
            try
            {
                p = getAttachmentManager().getAttachmentInfo( (WikiContext)null, page, version );
            }
            catch( ProviderException e )
            {
                log.debug("pageExists() failed to find attachments",e);
            }
        }

        return (p != null);
    }

    /**
     *  Returns true, if the requested page (or an alias) exists, with the
     *  specified version in the WikiPage.
     *
     *  @since 2.0
     */
    public boolean pageExists( WikiPage page )
        throws ProviderException
    {
        if( page != null )
        {
            return pageExists( page.getName(), page.getVersion() );
        }
        return false;
    }

    /**
     *  Returns the correct page name, or null, if no such
     *  page can be found.  Aliases are considered.
     *  <P>
     *  In some cases, page names can refer to other pages.  For example,
     *  when you have matchEnglishPlurals set, then a page name "Foobars"
     *  will be transformed into "Foobar", should a page "Foobars" not exist,
     *  but the page "Foobar" would.  This method gives you the correct
     *  page name to refer to.
     *  <P>
     *  This facility can also be used to rewrite any page name, for example,
     *  by using aliases.  It can also be used to check the existence of any
     *  page.
     *
     *  @since 2.0
     *  @param page Page name.
     *  @return The rewritten page name.
     */

    public String getFinalPageName( String page )
    {
        boolean isThere = simplePageExists( page );

        if( !isThere && m_matchEnglishPlurals )
        {
            if( page.endsWith("s") )
            {
                page = page.substring( 0, page.length()-1 );
            }
            else
            {
                page += "s";
            }

            isThere = simplePageExists( page );
        }

        return isThere ? page : null ;
    }

    /**
     *  Just queries the existing pages directly from the page manager.
     *  We also check overridden pages from jspwiki.properties
     */
    private boolean simplePageExists( String page )
    {
        if( getSpecialPageReference(page) != null ) return true;

        return m_pageManager.pageExists( page );
    }

    /**
     *  Turns a WikiName into something that can be 
     *  called through using an URL.
     *
     *  @since 1.4.1
     */
    public String encodeName( String pagename )
    {
        if( m_useUTF8 )
            return TextUtil.urlEncodeUTF8( pagename );
        else
            return java.net.URLEncoder.encode( pagename );
    }

    public String decodeName( String pagerequest )
    {
        if( m_useUTF8 )
            return TextUtil.urlDecodeUTF8( pagerequest );

        else
            return java.net.URLDecoder.decode( pagerequest );
    }

    /**
     *  Returns the IANA name of the character set encoding we're
     *  supposed to be using right now.
     *
     *  @since 1.5.3
     */
    public String getContentEncoding()
    {
        if( m_useUTF8 ) 
            return "UTF-8";

        return "ISO-8859-1";
    }

    /**
     *  Returns the unconverted text of a page.
     *
     *  @param page WikiName of the page to fetch.
     */
    public String getText( String page )
    {
        return getText( page, -1 );
    }

    /**
     * Returns the unconverted text of the given version of a page,
     * if it exists.  This method also replaces the HTML entities.
     *
     * @param page WikiName of the page to fetch
     * @param version  Version of the page to fetch
     */
    public String getText( String page, int version )
    {
        String result = getPureText( page, version );

        //
        //  Replace ampersand first, or else all quotes and stuff
        //  get replaced as well with &quot; etc.
        //
        result = TextUtil.replaceString( result, "&", "&amp;" );

        result = TextUtil.replaceEntities( result );

        return result;
    }

    /**
     *  Returns the pure text of a page, no conversions.
     *
     *  @param version If WikiPageProvider.LATEST_VERSION, then uses the 
     *  latest version.
     */
    // FIXME: Should throw an exception on unknown page/version?
    public String getPureText( String page, int version )
    {
        String result = null;

        try
        {
            result = m_pageManager.getPageText( page, version );
        }
        catch( ProviderException e )
        {
            // FIXME
        }
        finally
        {
            if( result == null )
                result = "";
        }

        return result;
    }

    /**
     *  Returns plain text (with HTML entities replaced). This should
     *  be the main entry point for getText().
     *
     *  @since 1.9.15.
     */
    public String getText( WikiContext context, WikiPage page )
    {
        return getText( page.getName(), page.getVersion() );
    }

    /**
     *  Returns the converted HTML of the page using a different
     *  context than the default context.
     */

    public String getHTML( WikiContext context, WikiPage page )
    {
	String pagedata = null;

        pagedata = getPureText( page.getName(), page.getVersion() );

        String res = textToHTML( context, pagedata );

	return res;
    }
    
    /**
     *  Returns the converted HTML of the page.
     *
     *  @param page WikiName of the page to convert.
     */
    public String getHTML( String page )
    {
        return getHTML( page, -1 );
    }

    /**
     *  Returns the converted HTML of the page's specific version.
     *  The version must be a positive integer, otherwise the current
     *  version is returned.
     *
     *  @param pagename WikiName of the page to convert.
     *  @param version Version number to fetch
     */
    public String getHTML( String pagename, int version )
    {
        WikiContext context = new WikiContext( this,
                                               pagename );
        WikiPage page = new WikiPage( pagename );
        page.setVersion( version );
        
        String res = getHTML( context, page );

	return res;
    }

    /**
     *  Converts raw page data to HTML.
     *
     *  @param pagedata Raw page data to convert to HTML
     */
    public String textToHTML( WikiContext context, String pagedata )
    {
        return textToHTML( context, pagedata, null, null );
    }

    /**
     *  Reads a WikiPageful of data from a String and returns all links
     *  internal to this Wiki in a Collection.
     */
    protected Collection scanWikiLinks( String pagedata )
    {
        LinkCollector localCollector = new LinkCollector();        
        TranslatorReader in = new TranslatorReader( new WikiContext(this,""), 
                                                    new StringReader( pagedata ) );
        in.enablePlugins( false );
        in.addLocalLinkHook( localCollector );
        String translation = translatePageText( in );
        return( localCollector.getLinks() );
    }


    /**
     *  Helper method for combining simple  textToHTML() and scanWikiLinks().
     */
    public String textToHTML( WikiContext context, 
                              String pagedata, 
                              StringTransmutator localLinkHook,
                              StringTransmutator extLinkHook )
    {
        String result = "";

        if( pagedata == null ) 
        {
            log.error("NULL pagedata to textToHTML()");
            return null;
        }

        TranslatorReader in = new TranslatorReader( context, 
                                                    new StringReader( pagedata ) );
        in.addLocalLinkHook( localLinkHook );
        in.addExternalLinkHook( extLinkHook );
        result = translatePageText( in );

        return( result );
    }

    /**
     * Simple wrapper for the TranslatorReader trnasformation process,
     * with default translation flags.
     */
    private String translatePageText( TranslatorReader transformer )
    {
        return( translatePageText( transformer, DEFAULT_TRANSLATION_FLAGS ) );
    }

    /**
     * Simple wrapper for the TranslatorReader trnasformation process.
     * Flags are not used currently; may be activated later. 
     */
    private String translatePageText( TranslatorReader transformer, int flags )
    {
        String result = "";
        TranslatorReader in = null;

        try
        {
            result = FileUtil.readContents( transformer );
        }
        catch( IOException e )
        {
            log.error("Failed to scan page data: ", e);
        }
        finally
        {
            try
            {
                if( in  != null ) in.close();
            }
            catch( Exception e ) 
            {
                log.fatal("Closing failed",e);
            }
        }

        return( result );
    }


    /**
     *  Writes the WikiText of a page into the
     *  page repository.
     *
     *  @param page Page name
     *  @param text The Wiki markup for the page.
     */
    public void saveText( String page, String text )
    {
        text = TextUtil.normalizePostData(text);

        // Hook into cross reference collection.
        m_referenceManager.updateReferences( page, scanWikiLinks( text ) );

        try
        {
            m_pageManager.putPageText( new WikiPage(page), text );
        }
        catch( ProviderException e )
        {
            log.error( "Unable to put page", e );
        }
    }


    /**
     *  @param request The HTTP Servlet request associated with this
     *                 transaction.
     *  @since 1.5.1
     */
    public void saveText( String page, String text, HttpServletRequest request )
    {
        text = TextUtil.normalizePostData(text);

        // Error protection or if the user info has been disabled.
        if( request == null || m_saveUserInfo == false ) 
        {
            saveText( page, text );
        }
        else
        {
            // Hook into cross reference collection.
            // Notice that this is definitely after the saveText() call above, 
            // since it can be called externally and we only want this done once.
            m_referenceManager.updateReferences( page, scanWikiLinks( text ) );

            WikiPage p = new WikiPage( page );

            UserProfile wup = getUserProfile( request );
            String author = wup.getName();

            p.setAuthor( author );

            try
            {
                m_pageManager.putPageText( p, text );
            }
            catch( ProviderException e )
            {
                log.error("Unable to put page: ", e);
            }
        }
    }

    /**
     *  Returns the number of pages in this Wiki
     */
    public int getPageCount()
    {
        return m_pageManager.getTotalPageCount();
    }

    /**
     *  Returns the provider name
     */

    public String getCurrentProvider()
    {
        return m_pageManager.getProvider().getClass().getName();
    }

    /**
     *  return information about current provider.
     *  @since 1.6.4
     */
    public String getCurrentProviderInfo()
    {
        return m_pageManager.getProviderDescription();
    }

    /**
     *  Returns a Collection of WikiPages, sorted in time
     *  order of last change.
     */
    public Collection getRecentChanges()
    {
        try
        {
            Collection pages = m_pageManager.getAllPages();

            TreeSet sortedPages = new TreeSet( new PageTimeComparator() );

            sortedPages.addAll( pages );

            return sortedPages;
        }
        catch( ProviderException e )
        {
            log.error( "Unable to fetch all pages: ",e);
            return null;
        }
    }

    /**
     *  Parses an incoming search request, then
     *  does a search.
     *  <P>
     *  Search language is simple: prepend a word
     *  with a + to force a word to be included (all files
     *  not containing that word are automatically rejected),
     *  '-' to cause the rejection of all those files that contain
     *  that word.
     */

    // FIXME: does not support phrase searches yet, but for them
    // we need a version which reads the whole page into the memory
    // once.

    //
    // FIXME: Should also have attributes attached.
    //
    public Collection findPages( String query )
    {
        StringTokenizer st = new StringTokenizer( query, " \t," );

        QueryItem[] items = new QueryItem[st.countTokens()];
        int word = 0;

        log.debug("Expecting "+items.length+" items");

        //
        //  Parse incoming search string
        //

        while( st.hasMoreTokens() )
        {
            log.debug("Item "+word);
            String token = st.nextToken().toLowerCase();

            items[word] = new QueryItem();

            switch( token.charAt(0) )
            {
              case '+':
                items[word].type = QueryItem.REQUIRED;
                token = token.substring(1);
                log.debug("Required word: "+token);
                break;
                
              case '-':
                items[word].type = QueryItem.FORBIDDEN;
                token = token.substring(1);
                log.debug("Forbidden word: "+token);
                break;

              default:
                items[word].type = QueryItem.REQUESTED;
                log.debug("Requested word: "+token);
                break;
            }

            items[word++].word = token;
        }

        Collection results = m_pageManager.findPages( items );
        
        return results;
    }


    /**
     *  Return a bunch of information from the web page.
     */
    public WikiPage getPage( String pagereq )
    {
        return getPage( pagereq, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Returns specific information about a Wiki page.
     *  @since 1.6.7.
     */
    public WikiPage getPage( String pagereq, int version )
    {
        try
        {
            WikiPage p = m_pageManager.getPageInfo( pagereq, version );
            if( p == null )
            {
                p = m_attachmentManager.getAttachmentInfo( (WikiContext)null, pagereq );
            }
            checkPermissions( p );
            return p;
        }
        catch( ProviderException e )
        {
            log.error( "Unable to fetch page info",e);
            return null;
        }
    }

    
    /**
     * Checks whether the WikiPage has access rules loaded.
     * If not, does an extra round through the TranslatorReader and
     * requests the rules from it, and attaches them to the page object.
     * Uses an empty rule set if none other is available.
     * 
     * <P>Permissions are always determined from the LATEST version of the page.
     * 
     * <P>An attachment's permissions are the same as their parent's.
     *
     * <p>Permission checks have been spliced into the WikiEngine.getPage()
     * methods as an afterthought. A better technique would be good. FIX!
     */
    public void checkPermissions( WikiPage p )
    {
        if( p != null )
        {
            String permissionPage = null;
            if( p instanceof Attachment )
                permissionPage = ((Attachment)p).getParentName();
            else
                permissionPage = p.getName();

            if( p.getAccessRules().isEmpty() )
            {
                WikiContext context = new WikiContext( this, permissionPage );
                String pagedata = getPureText( permissionPage, WikiProvider.LATEST_VERSION );
                TranslatorReader in = new TranslatorReader( context, new StringReader( pagedata ) );
                in.enablePlugins( false );
                String translation = translatePageText( in );
                AccessRuleSet rules = m_authorizer.getDefaultPermissions();

                if( rules != null )
                    rules.add( in.getAccessRules() );
                else
                    rules = in.getAccessRules();

                if( rules != null )
                    p.setAccessRules( rules );
                else
                    p.setAccessRules( new AccessRuleSet() );
            }
        }
    }


    /**
     *  Returns the date the page was last changed.
     *  If the page does not exist, returns null.
     *  @deprecated
     */
    /*
    public Date pageLastChanged( String page )
    {
        try
        {
            WikiPage p = m_pageManager.getPageInfo( page, WikiPageProvider.LATEST_VERSION );

            if( p != null )
                return p.getLastModified();
        }
        catch( ProviderException e )
        {
            log.error( "Unable to fetch last modification date", e );
        }

        return null;
    }
    */

    /**
     *  Returns the current version of the page.
     *  @deprecated
     */
    /*
    public int getVersion( String page )
    {
        try
        {
            WikiPage p = m_pageManager.getPageInfo( page, WikiPageProvider.LATEST_VERSION );

            if( p != null )
                return p.getVersion();
        }
        catch( ProviderException e )
        {
            log.error("FIXME");
        }
        return -1;
    }
    */

    /**
     *  Returns a Collection of WikiPages containing the
     *  version history of a page.
     */
    public Collection getVersionHistory( String page )
    {
        Collection c = null;

        try
        {
            c = m_pageManager.getVersionHistory( page );

            if( c == null )
            {
                c = m_attachmentManager.getVersionHistory( page );
            }
        }
        catch( ProviderException e )
        {
            log.error("FIXME");
        }

        return c;
    }

    /**
     *  Returns a diff of two versions of a page.
     *
     *  @param page Page to return
     *  @param version1 Version number of the old page.  If -1, then uses current page.
     *  @param version2 Version number of the new page.  If -1, then uses current page.
     *
     *  @return A HTML-ized difference between two pages.  If there is no difference,
     *          returns an empty string.
     */
    public String getDiff( String page, int version1, int version2 )
    {
        String page1 = getPureText( page, version1 );
        String page2 = getPureText( page, version2 );

        // Kludge to make diffs for new pages to work this way.

        if( version1 == -1 )
        {
            page1 = "";
        }

        String diff  = m_differenceEngine.makeDiff( page1, page2 );

        diff = TextUtil.replaceEntities( diff );
        
        try
        {
            if( diff.length() > 0 )
            {
                diff = m_differenceEngine.colorizeDiff( diff );
            }
        }
        catch( IOException e )
        {
            log.error("Failed to colorize diff result.", e);
        }

        return diff;
    }

    /**
     *  Attempts to locate a Wiki class, defaulting to the defaultPackage
     *  in case the actual class could not be located.
     *
     *  @param className Class to search for.
     *  @param defaultPackage A default package to try if the class 
     *                        cannot be directly located.  May be null.
     *  @throws ClassNotFoundException if the class could not be located.
     */
    public static Class findWikiClass( String className, String defaultPackage )
        throws ClassNotFoundException
    {
        Class tryClass;

        if( className == null )
        {
            throw new ClassNotFoundException("Null className!");
        }

        //
        //  Attempt to use a shortcut, if possible.
        //
        try
        {
            tryClass = Class.forName( className );
        }
        catch( ClassNotFoundException e )
        {
            // FIXME: This causes "null" names to be searched for twice, which
            //        is a performance penalty and not very nice.
            if( defaultPackage == null ) 
                defaultPackage = "";

            if( !defaultPackage.endsWith(".") )
                defaultPackage += ".";

            tryClass = Class.forName( defaultPackage+className );
        }

        return tryClass;
    }


    /**
     *  Returns this object's ReferenceManager.
     *  @since 1.6.1
     */
    // (FIXME: We may want to protect this, though...)
    public ReferenceManager getReferenceManager()
    {
        return m_referenceManager;
    }

    /**      
     *  Returns the current plugin manager.
     *  @since 1.6.1
     */

    public PluginManager getPluginManager()
    {
        return m_pluginManager;
    }

    public VariableManager getVariableManager()
    {
        return m_variableManager;
    }

    /**
     *  Returns the current PageManager.
     */
    public PageManager getPageManager()
    {
        return m_pageManager;
    }

    /**
     *  Returns the current AttachmentManager.
     *  @since 1.9.31.
     */
    public AttachmentManager getAttachmentManager()
    {
        return m_attachmentManager;
    }

    /**
     *  Returns the URL of the global RSS file.  May be null, if the
     *  RSS file generation is not operational.
     *  @since 1.7.10
     */
    public String getGlobalRSSURL()
    {
        if( m_rssURL != null )
        {
            return getBaseURL()+m_rssURL;
        }

        return null;
    }

    /**
     *  Runs the RSS generation thread.
     *  FIXME: MUST be somewhere else, this is not a good place.
     */
    private class RSSThread extends Thread
    {
        public void run()
        {
            try
            {
                String fileName = m_properties.getProperty( RSSGenerator.PROP_RSSFILE,
                                                            "rss.rdf" );
                int rssInterval = TextUtil.parseIntParameter( m_properties.getProperty( RSSGenerator.PROP_INTERVAL ),
                                                              3600 );

                log.debug("RSS file will be at "+fileName);
                log.debug("RSS refresh interval (seconds): "+rssInterval);

                while(true)
                {
                    Writer out = null;
                    Reader in  = null;

                    try
                    {
                        //
                        //  Generate RSS file, output it to
                        //  default "rss.rdf".
                        //
                        log.info("Regenerating RSS feed to "+fileName);

                        String feed = m_rssGenerator.generate();

                        File file = new File( m_rootPath, fileName );

                        in  = new StringReader(feed);
                        out = new BufferedWriter( new OutputStreamWriter( new FileOutputStream(file), "UTF-8") );

                        FileUtil.copyContents( in, out );

                        m_rssURL = fileName;
                    }
                    catch( IOException e )
                    {
                        log.error("Cannot generate RSS feed to "+fileName, e );
                        m_rssURL = null;
                    }
                    finally
                    {
                        try
                        {
                            if( in != null )  in.close();
                            if( out != null ) out.close();
                        }
                        catch( IOException e )
                        {
                            log.fatal("Could not close I/O for RSS", e );
                            break;
                        }
                    }

                    Thread.sleep(rssInterval*1000L);
                } // while
                
            }
            catch(InterruptedException e)
            {
                log.error("RSS thread interrupted, no more RSS feeds", e);
            }
            
            //
            // Signal: no more RSS feeds.
            //
            m_rssURL = null;
        }
    }


    /**
     * Given a user name and password, calls the configured Authenticator
     * and validates the user. Inserts a WikiUserProfile for the user into
     * the session; this WUP can then be used to add and query the user's 
     * permissions and check the user ID, both in JSP pages and Wiki code.
     */
    public void login( String uid, String passwd, HttpSession session )
    {
        if( session == null )
        {
            log.error( "No session provided by caller. Unable to login " + uid + "." );
            return;
        }

        UserProfile wup = new UserProfile();
        wup.setName( uid );
        wup.setPassword( passwd );
        boolean isValid = m_authenticator.authenticate( wup );
        if( isValid )
        {
            m_authorizer.loadPermissions( wup );
            session.setAttribute( WIKIUSER, wup );
            log.debug( "Added WUP " + wup + " for " + uid );
        }
    }

    /**
     * Performs a "limited" login: sniffs for a user name from a cookie or the
     * client, and creates a limited user profile based on it. 
     */
    protected UserProfile limitedLogin( HttpServletRequest request )
    {
        UserProfile wup = null;
        String role = null;

        // See if a cookie exists, and create a 'preferred guest' account if so.
        String storedProfile = retrieveCookieValue( request, PREFS_COOKIE_NAME );

        if( storedProfile != null )
        {
            wup = new UserProfile( storedProfile );
            wup.setStatus( UserProfile.NAMED );
            m_authorizer.addRole( wup, Authorizer.AUTH_ROLE_PARTICIPANT );
        }
        else
        {
            String uid = request.getRemoteUser();
            if( uid == null && m_storeIPAddress )
            {
                uid = request.getRemoteAddr();
            }
            if( uid == null )
            {
                uid = Authorizer.AUTH_UNKNOWN_UID;
            }
            wup = new UserProfile();
            wup.setName( uid );
            wup.setStatus( UserProfile.UNKNOWN );
            m_authorizer.addRole( wup, Authorizer.AUTH_ROLE_GUEST );
        }

        // Limited login hasn't been authenticated. Just to emphasize the point:
        wup.setPassword( null ); 

        HttpSession session = request.getSession( true );
        session.setAttribute( WIKIUSER, wup );

        return( wup );
    }


    
    /**
     * Removes a UserProfile from a session.
     */
    public void logout( HttpSession session )
    {
        log.debug( "Logging out." );
        UserProfile wup = (UserProfile)session.getAttribute( WIKIUSER );
        if( wup != null )
        {
            log.debug( "logged out user " + wup.getName() );
            session.setAttribute( WIKIUSER, null );
        }
    }


    /**
     * Gets a UserProfile, either from the request (presumably 
     * authenticated and with auth information) or a new one
     * (with default permissions).
     */
    public UserProfile getUserProfile( HttpServletRequest request )
    {
        // First, see if we already have a user profile.
        HttpSession session = request.getSession( true );
        UserProfile wup = (UserProfile)session.getAttribute( WIKIUSER );
        if( wup != null )
            return( wup );

        // Try to get a limited login. This will be inserted into the request.
        wup = limitedLogin( request );
        if( wup != null )
        {
            return( wup );
        }

        log.error( "Unable to get a default UserProfile!" );
        return( null );
    }

    
    /**
     * Returns the user name of a logged in user. Returns null if login has not been done.
     *
     * <p>(This method replaces the old, cookie-based username facility.)
     */
    public String getUserName( HttpServletRequest request )
    {
        UserProfile wup = getUserProfile( request );
        if( wup != null )
        {
            return( wup.getName() );
        }

        return( null );
    }

    /**
     *  If no author name has been set, then use the 
     *  IP address, if allowed.  If no IP address is allowed,
     *  then returns a default.
     *
     *  NOTE: this is a compatibility method to the main branch.
     *  It should not be needed with the new auth system. FIX!
     *
     *  @return Returns any sort of user name.  Never returns null.
     */
    public String getValidUserName( HttpServletRequest request )
    {
        return( getUserName( request ) );
    }

    /**
     *  Retrieves the user name.  It will check if user has been authenticated,
     *  or he has a cookie with his username set.  The cookie takes precedence, 
     *  which allows the wiki master to set up a site with a single master
     *  password.  Note that this behaviour will be changed in 2.1 or thereabouts.
     *  Earlier versions than 1.9.42 preferred the authenticated name over
     *  the cookie name.
     *
     *  @param request HttpServletRequest that was called.
     *  @return The WikiName of the user, or null, if no username was set.
     */
    // FIXME: This is a terrible time waster, parsing the 
    // textual data every time it is used.
    /*
    public String getUserName( HttpServletRequest request )
    {
        if( request == null )
        {
            return null;
        }

        // Get the user authentication - if he's not been authenticated
        // we then check from cookies.

        String author = request.getRemoteUser();

        //
        //  Try to fetch something from a cookie.
        //

        Cookie[] cookies = request.getCookies();

        if( cookies != null )
        {
            for( int i = 0; i < cookies.length; i++ )
            {
                if( cookies[i].getName().equals( PREFS_COOKIE_NAME ) )
                {
                    UserProfile p = new UserProfile(cookies[i].getValue());

                    author = p.getName();

                    break;
                }
            }
        }

        return author;
    }
    */

    /**
     * Attempts to retrieve the given cookie value from the request.
     * Returns the string value (which may or may not be decoded 
     * correctly, depending on browser!), or null if the cookie is
     * not found.
     */
    public String retrieveCookieValue( HttpServletRequest request, String cookieName )
    {
        Cookie[] cookies = request.getCookies();

        if( cookies != null )
        {
            for( int i = 0; i < cookies.length; i++ )
            {
                if( cookies[i].getName().equals( cookieName ) )
                {
                    return( cookies[i].getValue() );
                }
            }
        }

        return( null );
    }

}
