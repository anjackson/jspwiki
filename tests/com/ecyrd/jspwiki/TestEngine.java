
package com.ecyrd.jspwiki;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.io.*;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.*;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.attachment.Attachment;
import com.ecyrd.jspwiki.auth.Users;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.providers.*;
import com.ecyrd.jspwiki.ui.WikiServletFilter;

/**
 *  <p>Simple test engine that always assumes pages are found. The version of TestEngine that is part of JSPWiki 3.0
 *  differs slightly from earlier versions. In particular, it integrates the Stripes framework's mock objects to simulate
 *  servlet testing.</p>
 *  <p>Because of its use of Stripes mock objects, TestEngine needs to be able to find the various ActionBean
 *  implementations provided in JSPWiki. Therefore, it is <em>extremely</em> sensitive to changes in the build
 *  path. In particular, the mock servlet filter used by TestEngine hard-wires in the relative location
 *  <code>build</code> for finding ActionBeans. This is the directory (relative to the project root) that the
 *  Ant build scripts use for placing generated Java class files. The Eclipse project configuration must configure
 *  itself the same way. To run unit tests in Eclipse, the <code>build</code> directory absolutely <em>must</em>
 *  place generated class files in this directory, rather than the Eclipse default of <code>classes</code>. If
 *  unit tests do not run in Eclipse for some reason, this is the likeliest culprit.
 */
public class TestEngine extends WikiEngine
{
    static Logger log = Logger.getLogger( TestEngine.class );

    private HttpSession m_adminSession;
    private HttpSession m_janneSession;
    private WikiSession m_adminWikiSession;
    private WikiSession m_janneWikiSession;
    private WikiSession m_guestWikiSession;

    /**
     * Creates WikiSession with the privileges of the administrative user.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession adminSession()
    {
        return m_adminWikiSession;
    }

    /**
     * Creates guest WikiSession with the no privileges.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession guestSession()
    {
        return m_guestWikiSession;
    }

    /**
     * Creates WikiSession with the privileges of the Janne.
     * For testing purposes, obviously.
     * @return the wiki session
     */
    public WikiSession janneSession()
    {
        return m_janneWikiSession;
    }

    public TestEngine( Properties props )
        throws WikiException
    {
        super( new MockServletContext( "test" ), "test", props );
        
        // Stash the WikiEngine in the servlet context
        MockServletContext servletContext = (MockServletContext)this.getServletContext();
        servletContext.setAttribute("com.ecyrd.jspwiki.WikiEngine", this);
        
        // Add mock StripesFilter and WikiServletfilter and to servlet config
        Map<String,String> filterParams = new HashMap<String,String>();
        filterParams.put("ActionResolver.Packages", "com.ecyrd.jspwiki.action");
        filterParams.put("Extension.Packages", "com.ecyrd.jspwiki.action");
        filterParams.put( "ExceptionHandler.Class", "com.ecyrd.jspwiki.action.WikiExceptionHandler" );
        servletContext.addFilter(StripesFilter.class, "StripesFilter", filterParams);
        servletContext.addFilter( WikiServletFilter.class, "WikiServletFilter", new HashMap<String,String>() );
        servletContext.setServlet(DispatcherServlet.class, "StripesDispatcher", null);

        // Set up long-running admin session
        HttpServletRequest request = newHttpRequest();
        m_adminWikiSession = WikiSession.getWikiSession( this, request );
        this.getAuthenticationManager().login( m_adminWikiSession,
                Users.ADMIN,
                Users.ADMIN_PASS );
        m_adminSession = request.getSession();

        // Set up a test Janne session
        request = newHttpRequest();
        m_janneWikiSession = WikiSession.getWikiSession( this, request );
        this.getAuthenticationManager().login( m_janneWikiSession,
                Users.JANNE,
                Users.JANNE_PASS );
        m_janneSession = request.getSession();

        // Set up guest session
        request = newHttpRequest();
        m_guestWikiSession = WikiSession.getWikiSession( this, request );
    }
    
    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession.
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest()
    {
        return newHttpRequest( "/Wiki.jsp" );
    }

    /**
     * Creates a correctly-instantiated mock HttpServletRequest with an associated
     * HttpSession and path.
     * @param path the path relative to the wiki context, for example "/Wiki.jsp"
     * @return the new request
     */
    public MockHttpServletRequest newHttpRequest( String path )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( "/JSPWiki", path );
        request.setSession( new MockHttpSession( this.getServletContext() ) );
        return request;
    }
    
    public static void emptyWorkDir()
    {
        Properties properties = new Properties();

        try
        {
            properties.load( findTestProperties() );

            String workdir = properties.getProperty( WikiEngine.PROP_WORKDIR );
            if( workdir != null )
            {
                File f = new File( workdir );

                if( f.exists() && f.isDirectory() && new File( f, "refmgr.ser" ).exists() )
                {
                    deleteAll( f );
                }
            }
        }
        catch( IOException e ) {} // Fine
    }

    public static final InputStream findTestProperties()
    {
        return findTestProperties( "/jspwiki.properties" );
    }

    public static final InputStream findTestProperties( String properties )
    {
        InputStream in = TestEngine.class.getResourceAsStream( properties );

        if( in == null ) throw new InternalWikiException("Unable to locate test property resource: "+properties);

        return in;
    }

    /**
     *  Deletes all files under this directory, and does them recursively.
     */
    public static void deleteAll( File file )
    {
        if( file != null )
        {
            if( file.isDirectory() )
            {
                File[] files = file.listFiles();

                if( files != null )
                {
                    for( int i = 0; i < files.length; i++ )
                    {
                        if( files[i].isDirectory() )
                        {
                            deleteAll(files[i]);
                        }

                        files[i].delete();
                    }
                }
            }

            file.delete();
        }
    }

    /**
     *  Copied from FileSystemProvider
     */
    protected static String mangleName( String pagename )
        throws IOException
    {
        Properties properties = new Properties();
        String m_encoding = properties.getProperty( WikiEngine.PROP_ENCODING,
                                                    AbstractFileProvider.DEFAULT_ENCODING );

        pagename = TextUtil.urlEncode( pagename, m_encoding );
        pagename = TextUtil.replaceString( pagename, "/", "%2F" );
        return pagename;
    }

    /**
     *  Removes a page, but not any auxiliary information.  Works only
     *  with FileSystemProvider.
     */
    public static void deleteTestPage( String name )
    {
        Properties properties = new Properties();

        try
        {
            properties.load( findTestProperties() );
            String files = properties.getProperty( FileSystemProvider.PROP_PAGEDIR );

            File f = new File( files, mangleName(name)+FileSystemProvider.FILE_EXT );

            f.delete();

            // Remove the property file, too
            f = new File( files, mangleName(name)+".properties" );

            if( f.exists() )
                f.delete();
        }
        catch( Exception e )
        {
            log.error("Couldn't delete "+name, e );
        }
    }

    /**
     *  Deletes all attachments related to the given page.
     */
    public void deleteAttachments( String page )
    {
        try
        {
            String files = getWikiProperties().getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );

            File f = new File( files, TextUtil.urlEncodeUTF8( page ) + BasicAttachmentProvider.DIR_EXTENSION );

            deleteAll( f );
        }
        catch( Exception e )
        {
            log.error("Could not remove attachments.",e);
        }
    }

    /**
     *  Makes a temporary file with some content, and returns a handle to it.
     */
    public File makeAttachmentFile()
        throws Exception
    {
        File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        FileWriter out = new FileWriter( tmpFile );

        FileUtil.copyContents( new StringReader( "asdfa???dfzbvasdjkfbwfkUg783gqdwog" ), out );

        out.close();

        return tmpFile;
    }

    /**
     *  Adds an attachment to a page for testing purposes.
     * @param pageName
     * @param attachmentName
     * @param data
     */
    public void addAttachment( String pageName, String attachmentName, byte[] data )
        throws ProviderException, IOException
    {
        Attachment att = new Attachment(this,pageName,attachmentName);

        getAttachmentManager().storeAttachment(att, new ByteArrayInputStream(data));
    }

    /**
     * Convenience method that saves a wiki page by constructing a fake
     * WikiContext and HttpServletRequest. We always want to do this using a
     * WikiContext whose subject contains Role.ADMIN.
     * @param pageName
     * @param content
     * @throws WikiException
     */
    public void saveText( String pageName, String content )
        throws WikiException
    {
        // Build new request and associate our admin session
        MockHttpServletRequest request = new MockHttpServletRequest( "/JSPWiki", "/Wiki.jsp" );
        request.setSession( m_adminSession );

        // Create page and wiki context
        WikiPage page = new WikiPage( this, pageName );
        WikiContext context = getWikiActionBeanFactory().newViewActionBean( request, (HttpServletResponse)null, page );
        saveText( context, content );
    }

    public void saveTextAsJanne( String pageName, String content )
        throws WikiException
    {
        // Build new request and associate our Janne session
        MockHttpServletRequest request = new MockHttpServletRequest( "/JSPWiki", "/Wiki.jsp" );
        request.setSession( m_janneSession );

        // Create page and wiki context
        WikiPage page = new WikiPage( this, pageName );
        WikiContext context = getWikiActionBeanFactory().newViewActionBean( request, (HttpServletResponse)null, page );
        saveText( context, content );
    }

    public static void trace()
    {
        try
        {
            throw new Exception("Foo");
        }
        catch( Exception e )
        {
            e.printStackTrace();
        }
    }
    
    /**
     * Creates a guest "round trip" object that initializes itself with the TestEngine's mock servlet context,
     * plus a new mock request, mock response and action bean of type {@link com.ecyrd.jspwiki.action.ViewActionBean}.
     * This method is the preferred way to instantiate request and response objects, which can be
     * obtained by calling {@link net.sourceforge.stripes.mock.MockRoundtrip#getRequest()} and
     * {@link net.sourceforge.stripes.mock.MockRoundtrip#getResponse()}.
     * @param beanClass the Stripes action bean to start with
     * @return the mock rountrip
     */
    public MockRoundtrip guestTrip( Class<? extends WikiActionBean> beanClass )
    {
        return new MockRoundtrip( (MockServletContext)getServletContext(), beanClass );
    }

    /**
     * Creates a "round trip" object initialized with a supplied set of credentials. The WikiSession
     * associated with the created MockRoundtrip object will have privileges appropriate for
     * the credentials supplied.
     * @param user the login name
     * @param password the password
     * @param beanClass the Stripes action bean to start with
     * @return the initialized round trip
     * @throws WikiSecurityException
     */
    public MockRoundtrip authenticatedTrip( String user, String password, Class<? extends WikiActionBean> beanClass ) throws WikiSecurityException
    {
        MockRoundtrip trip = new MockRoundtrip( (MockServletContext)getServletContext(), beanClass );
        MockHttpServletRequest request = trip.getRequest();
        WikiSession session = WikiSession.getWikiSession( this, request);
        this.getAuthenticationManager().login( session, Users.ADMIN, Users.ADMIN_PASS );
        return trip;
    }
    
    /**
     * Static single instance of the mock servlet.
     */
    private static final Servlet MOCK_SERVLET = new MockServlet();

    /**
     * Captive servlet class that does absolutely nothing. Used by
     * MockRoundtrip.
     */
    protected static class MockServlet extends HttpServlet
    {
        private static final long serialVersionUID = 1L;
        
        private ServletConfig m_config;
        
        public MockServlet()
        {
        }

        public ServletConfig getServletConfig()
        {
            return m_config;
        }

        public String getServletInfo()
        {
            return "Mock servlet";
        }

        public void init( ServletConfig config ) throws ServletException
        {
            m_config = config;
        }
    }

}
