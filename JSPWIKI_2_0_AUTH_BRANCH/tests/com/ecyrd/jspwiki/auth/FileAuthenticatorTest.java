
package com.ecyrd.jspwiki.auth;

import junit.framework.*;
import java.io.*;
import java.util.*;

import org.apache.log4j.*;

import com.ecyrd.jspwiki.UserProfile;

public class FileAuthenticatorTest 
    extends TestCase
{
    Properties props = new Properties();

    private String[] lines = {
        "# A comment line",
        "# A non-parsed line:", 
        "default READ ALLOW ALL", 
        "# A user definition, password is xyzzy:", 
        "user ebu oTtKwkqR5Zd4k a,b,c,d,e",
        "# A user definition with extra spaces", 
        " user\tube\toTtKwkqR5Zd4k     line,with,whitespace "
    };

    public static final String testFileName = "authtest.tmp";
    private FileAuthenticator m_auth;

    public FileAuthenticatorTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
        PropertyConfigurator.configure(props);

        // Create a file to parse.
        File testAuth = new File( testFileName );
        if( testAuth.exists() )
            testAuth.delete();

        PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter( testAuth ) ) );
        for( int i = 0; i < lines.length; i++ )
            out.println( lines[i] );
        out.close();

        props.setProperty( FileAuthenticator.PROP_AUTHFILE, testFileName );
        m_auth = new FileAuthenticator();
        m_auth.initialize( props );
    }

    public void tearDown()
    {
        File testAuth = new File( testFileName );
        if( testAuth.exists() )
            testAuth.delete();
    }

    public void testPasswordParsing()
    {
        String passwd = m_auth.parseHashedPassword( "nothing should be found here" );
        assertTrue( passwd == null );

        passwd = m_auth.parseHashedPassword( "user testuser oTtKwkqR5Zd4k passwd,xyzzy,should,match" );
        assertTrue( Crypt.match( "xyzzy", passwd ) );
    }


    public void testValidUserName()
        throws Exception
    {
        // From line 1 in lines:
        UserProfile wup = new UserProfile();
        wup.setName( "ebu" );
        wup.setPassword( "xyzzy" );
        boolean rval = m_auth.authenticate( wup );
        assertTrue( rval );
        assertTrue( wup.isValidated() );
    }


    public void testFakeLine()
        throws Exception
    {
        // From line 2 in lines:
        UserProfile wup = new UserProfile();
        wup.setName( "fake" );
        wup.setPassword( "" );
        boolean rval = m_auth.authenticate( wup );
        assertFalse( rval );
        assertFalse( wup.isValidated() );
    }

    public void testWhitespacedEntry()
        throws Exception
    {
        // From line 3 in lines:
        UserProfile wup = new UserProfile();
        wup.setName( "ube" );
        wup.setPassword( "xyzzy" );
        boolean rval = m_auth.authenticate( wup );
        assertTrue( rval );
        assertTrue( wup.isValidated() );
    }


    public static Test suite()
    {
        return new TestSuite( FileAuthenticatorTest.class );
    }

}
