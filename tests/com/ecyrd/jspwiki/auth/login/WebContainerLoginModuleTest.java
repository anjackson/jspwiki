package com.ecyrd.jspwiki.auth.login;

import java.security.Principal;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TestHttpServletRequest;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.authorize.Role;
import com.ecyrd.jspwiki.auth.user.UserDatabase;
import com.ecyrd.jspwiki.auth.user.XMLUserDatabase;

/**
 * @author Andrew R. Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-09-17 18:35:29 $
 */
public class WebContainerLoginModuleTest extends TestCase
{

    UserDatabase db;

    Subject      subject;

    public final void testLogin()
    {
        Principal principal = new WikiPrincipal( "Andrew Jaquith" ); 
        Principal wrapper = new PrincipalWrapper( principal );
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setUserPrincipal( principal );
        try
        {
            // Test using Principal (WebContainerLoginModule succeeds)
            CallbackHandler handler = new WebContainerCallbackHandler( request, db );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( wrapper ) );
            assertTrue( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );

            // Test using remote user (WebContainerLoginModule succeeds)
            subject = new Subject();
            request = new TestHttpServletRequest();
            request.setRemoteUser( "Andrew Jaquith" );
            handler = new WebContainerCallbackHandler( request, db );
            context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( wrapper ) );
            assertTrue( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );

            // Test using IP address (AnonymousLoginModule succeeds)
            subject = new Subject();
            request = new TestHttpServletRequest();
            request.setRemoteAddr( "53.33.128.9" );
            handler = new WebContainerCallbackHandler( request, db );
            context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertFalse( principals.contains( principal ) );
            assertFalse( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    public final void testLogout()
    {
        Principal principal = new WikiPrincipal( "Andrew Jaquith" ); 
        Principal wrapper = new PrincipalWrapper( principal );
        TestHttpServletRequest request = new TestHttpServletRequest();
        request.setUserPrincipal( principal );
        try
        {
            CallbackHandler handler = new WebContainerCallbackHandler( request, db );
            LoginContext context = new LoginContext( "JSPWiki-container", subject, handler );
            context.login();
            Set principals = subject.getPrincipals();
            assertEquals( 3, principals.size() );
            assertTrue( principals.contains( wrapper ) );
            assertTrue( principals.contains( Role.AUTHENTICATED ) );
            assertTrue( principals.contains( Role.ALL ) );
            context.logout();
            assertEquals( 0, principals.size() );
        }
        catch( LoginException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        Properties props = new Properties();
        props.put( XMLUserDatabase.PROP_USERDATABASE, "./etc/userdatabase.xml" );
        db = new XMLUserDatabase();
        subject = new Subject();
        try
        {
            db.initialize( null, props );
        }
        catch( NoRequiredPropertyException e )
        {
            System.err.println( e.getMessage() );
            assertTrue( false );
        }
    }

}