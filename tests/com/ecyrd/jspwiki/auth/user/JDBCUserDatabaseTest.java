package com.ecyrd.jspwiki.auth.user;

import java.io.File;
import java.security.Principal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

import junit.framework.TestCase;

import com.ecyrd.jspwiki.TestJDBCDataSource;
import com.ecyrd.jspwiki.TestJNDIContext;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.util.CryptoUtil;

/**
 * @author Andrew Jaquith
 */
public class JDBCUserDatabaseTest extends TestCase
{
    private JDBCUserDatabase m_db   = null;

    private static final String INSERT_JANNE = "INSERT INTO users (" +
          JDBCUserDatabase.DEFAULT_DB_EMAIL + "," +
          JDBCUserDatabase.DEFAULT_DB_FULL_NAME + "," +
          JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME + "," +
          JDBCUserDatabase.DEFAULT_DB_PASSWORD + "," +
          JDBCUserDatabase.DEFAULT_DB_WIKI_NAME + "," +
          JDBCUserDatabase.DEFAULT_DB_CREATED + ") VALUES (" +
          "'janne@ecyrd.com'," + "'Janne Jalkanen'," + "'janne'," +
          "'{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee'," +
          "'JanneJalkanen'," +
          "'" + new Timestamp( new Timestamp( System.currentTimeMillis() ).getTime() ).toString() + "'" + ");";

    private static final String INSERT_USER = "INSERT INTO users (" +
        JDBCUserDatabase.DEFAULT_DB_EMAIL + "," +
        JDBCUserDatabase.DEFAULT_DB_LOGIN_NAME + "," +
        JDBCUserDatabase.DEFAULT_DB_PASSWORD + "," +
        JDBCUserDatabase.DEFAULT_DB_CREATED + ") VALUES (" +
        "'user@example.com'," + "'user'," +
        "'{SHA}5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8'," +
        "'" + new Timestamp( new Timestamp( System.currentTimeMillis() ).getTime() ).toString() + "'" + ");";

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception
    {
        super.setUp();

        // Set up the mock JNDI initial context
        TestJNDIContext.initialize();
        Context initCtx = new InitialContext();
        initCtx.bind( "java:comp/env", new TestJNDIContext() );
        Context ctx = (Context) initCtx.lookup( "java:comp/env" );
        DataSource ds = new TestJDBCDataSource( new File( "build.properties" ) );
        ctx.bind( JDBCUserDatabase.DEFAULT_DB_JNDI_NAME, ds );

        // Get the JDBC connection and init tables
        try
        {
            Connection conn = ds.getConnection();
            Statement stmt = conn.createStatement();
            String sql;

            sql = "DELETE FROM " + JDBCUserDatabase.DEFAULT_DB_TABLE + ";";
            stmt.executeUpdate( sql );

            // Create a new test user 'janne'
            stmt.executeUpdate( INSERT_JANNE );

            // Create a new test user 'user'
            stmt.executeUpdate( INSERT_USER );
            stmt.close();

            conn.close();

            // Initialize the user database
            m_db = new JDBCUserDatabase();
            m_db.initialize( null, new Properties() );
        }
        catch( SQLException e )
        {
            System.err.println("Looks like your database could not be connected to - "+
                               "please make sure that you have started your database "+
                               "(e.g. by running ant hsql-start)");

            throw (SQLException) e.fillInStackTrace();
        }
    }

    public void testDeleteByLoginName() throws WikiSecurityException
    {
        // First, count the number of users in the db now.
        int oldUserCount = m_db.getWikiNames().length;

        // Create a new user with random name
        String loginName = "TestUser" + String.valueOf( System.currentTimeMillis() );
        UserProfile profile = new DefaultUserProfile();
        profile.setEmail("testuser@testville.com");
        profile.setLoginName( loginName );
        profile.setFullname( "FullName"+loginName );
        profile.setPassword("password");
        m_db.save(profile);

        // Make sure the profile saved successfully
        profile = m_db.findByLoginName( loginName );
        assertEquals( loginName, profile.getLoginName() );
        assertEquals( oldUserCount+1, m_db.getWikiNames().length );

        // Now delete the profile; should be back to old count
        m_db.deleteByLoginName( loginName );
        assertEquals( oldUserCount, m_db.getWikiNames().length );
    }

    public void testFindByEmail()
    {
        try
        {
            UserProfile profile = m_db.findByEmail( "janne@ecyrd.com" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByFullName()
    {
        try
        {
            UserProfile profile = m_db.findByFullName( "Janne Jalkanen" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo@bar.org" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByWikiName()
    {
        try
        {
            UserProfile profile = m_db.findByWikiName( "JanneJalkanen" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "foo" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testFindByLoginName()
    {
        try
        {
            UserProfile profile = m_db.findByLoginName( "janne" );
            assertEquals( "janne", profile.getLoginName() );
            assertEquals( "Janne Jalkanen", profile.getFullname() );
            assertEquals( "JanneJalkanen", profile.getWikiName() );
            assertEquals( "{SHA}457b08e825da547c3b77fbc1ff906a1d00a7daee", profile.getPassword() );
            assertEquals( "janne@ecyrd.com", profile.getEmail() );
            assertNotNull( profile.getCreated() );
            assertNull( profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        try
        {
            m_db.findByEmail( "FooBar" );
            // We should never get here
            assertTrue( false );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( true );
        }
    }

    public void testGetWikiName() throws WikiSecurityException
    {
        Principal[] principals = m_db.getWikiNames();
        assertEquals( 1, principals.length );
    }

    public void testRename() throws Exception
    {
        // Try renaming a non-existent profile; it should fail
        try
        {
            m_db.rename( "nonexistentname", "renameduser" );
            fail( "Should not have allowed rename..." );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Cool; that's what we expect
        }

        // Create new user & verify it saved ok
        UserProfile profile = new DefaultUserProfile();
        profile.setEmail( "renamed@example.com" );
        profile.setFullname( "Renamed User" );
        profile.setLoginName( "olduser" );
        profile.setPassword( "password" );
        m_db.save( profile );
        profile = m_db.findByLoginName( "olduser" );
        assertNotNull( profile );

        // Try renaming to a login name that's already taken; it should fail
        try
        {
            m_db.rename( "olduser", "janne" );
            fail( "Should not have allowed rename..." );
        }
        catch ( DuplicateUserException e )
        {
            // Cool; that's what we expect
        }

        // Now, rename it to an unused name
        m_db.rename( "olduser", "renameduser" );

        // The old user shouldn't be found
        try
        {
            profile = m_db.findByLoginName( "olduser" );
            fail( "Old user was found, but it shouldn't have been." );
        }
        catch ( NoSuchPrincipalException e )
        {
            // Cool, it's gone
        }

        // The new profile should be found, and its properties should match the old ones
        profile = m_db.findByLoginName( "renameduser" );
        assertEquals( "renamed@example.com", profile.getEmail() );
        assertEquals( "Renamed User", profile.getFullname() );
        assertEquals( "renameduser", profile.getLoginName() );
        assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );

        // Delete the user
        m_db.deleteByLoginName( "renameduser" );
    }

    public void testSave() throws Exception
    {
        try
        {
            // Overwrite existing user
            UserProfile profile = new DefaultUserProfile();
            profile.setEmail( "user@example.com" );
            profile.setFullname( "Test User" );
            profile.setLoginName( "user" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "user@example.com" );
            assertEquals( "user@example.com", profile.getEmail() );
            assertEquals( "Test User", profile.getFullname() );
            assertEquals( "user", profile.getLoginName() );
            assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
            assertEquals( "TestUser", profile.getWikiName() );
            assertNotNull( profile.getCreated() );
            assertNotNull( profile.getLastModified() );
            assertNotSame( profile.getCreated(), profile.getLastModified() );

            // Create new user
            profile = new DefaultUserProfile();
            profile.setEmail( "user2@example.com" );
            profile.setFullname( "Test User 2" );
            profile.setLoginName( "user2" );
            profile.setPassword( "password" );
            m_db.save( profile );
            profile = m_db.findByEmail( "user2@example.com" );
            assertEquals( "user2@example.com", profile.getEmail() );
            assertEquals( "Test User 2", profile.getFullname() );
            assertEquals( "user2", profile.getLoginName() );
            assertTrue( CryptoUtil.verifySaltedPassword( "password".getBytes(), profile.getPassword() ) );
            assertEquals( "TestUser2", profile.getWikiName() );
            assertNotNull( profile.getCreated() );
            assertNotNull( profile.getLastModified() );
            assertEquals( profile.getCreated(), profile.getLastModified() );
        }
        catch( NoSuchPrincipalException e )
        {
            assertTrue( false );
        }
        catch( WikiSecurityException e )
        {
            assertTrue( false );
        }
    }

    public void testValidatePassword()
    {
        assertFalse( m_db.validatePassword( "janne", "test" ) );
        assertTrue( m_db.validatePassword( "janne", "myP@5sw0rd" ) );
        assertTrue( m_db.validatePassword( "user", "password" ) );
    }

}
