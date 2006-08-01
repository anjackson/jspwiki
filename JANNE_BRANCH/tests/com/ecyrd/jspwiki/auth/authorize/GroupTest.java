package com.ecyrd.jspwiki.auth.authorize;

import java.security.Principal;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.ecyrd.jspwiki.TestEngine;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.SecurityEventTrap;
import com.ecyrd.jspwiki.auth.WikiPrincipal;
import com.ecyrd.jspwiki.auth.WikiSecurityEvent;

public class GroupTest extends TestCase
{
    Group m_group;
    SecurityEventTrap m_trap;
    String m_wiki;
    
    public GroupTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        WikiEngine engine  = new TestEngine( props );
        m_wiki = engine.getApplicationName();
        
        m_group = new Group( "TestGroup", m_wiki );
        m_trap = new SecurityEventTrap();
        m_group.addWikiEventListener( m_trap );
    }
    
    public void testAdd1()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        m_group.add( u1 );
        assertTrue( m_group.isMember( u1 ) );
        
        // Test that our event listener works too
        WikiSecurityEvent event = m_trap.lastEvent();
        assertEquals( event.getSource(), m_group );
        assertEquals( event.getType(), WikiSecurityEvent.GROUP_ADD_MEMBER );
        assertEquals( event.getTarget(), u1 );
    }

    public void testAdd2()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        assertTrue( "adding alice", m_group.add( u1 ) );
        WikiSecurityEvent event = m_trap.lastEvent();
        assertEquals( event.getSource(), m_group );
        assertEquals( event.getType(), WikiSecurityEvent.GROUP_ADD_MEMBER );
        assertEquals( event.getTarget(), u1 );

        assertTrue( "adding bob", m_group.add( u2 ) );
        event = m_trap.lastEvent();
        assertEquals( event.getSource(), m_group );
        assertEquals( event.getType(), WikiSecurityEvent.GROUP_ADD_MEMBER );
        assertEquals( event.getTarget(), u2 );

        assertTrue( "Alice", m_group.isMember( u1 ) );
        assertTrue( "Bob", m_group.isMember( u2 ) );
    }

    /**
     * Check that different objects match as well.
     */
    public void testAdd3()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        Principal u3 = new WikiPrincipal( "Bob" );

        assertTrue( "adding alice", m_group.add( u1 ) );
        assertTrue( "adding bob", m_group.add( u2 ) );

        assertTrue( "Alice", m_group.isMember( u1 ) );
        assertTrue( "Bob", m_group.isMember( u3 ) );
    }

    public void testRemove()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        Principal u3 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        m_group.remove( u3 );
        WikiSecurityEvent event = m_trap.lastEvent();
        assertEquals( event.getSource(), m_group );
        assertEquals( event.getType(), WikiSecurityEvent.GROUP_REMOVE_MEMBER );
        assertEquals( event.getTarget(), u3 );

        assertTrue( "Alice", m_group.isMember( u1 ) );
        assertFalse( "Bob", m_group.isMember( u2 ) );
        assertFalse( "Bob 2", m_group.isMember( u3 ) );
    }

    public void testEquals1()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        Group group2 = new Group( "TestGroup", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Bob" );

        group2.add( u3 );
        group2.add( u4 );

        assertTrue( m_group.equals( group2 ) );
    }

    public void testEquals2()
    {
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );

        m_group.add( u1 );
        m_group.add( u2 );

        Group group2 = new Group( "Group2", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Charlie" );

        group2.add( u3 );
        group2.add( u4 );

        assertFalse( m_group.equals( group2 ) );
    }

    public void testEquals3()
    {
        Group group1 = new Group( "Blib", m_wiki );
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        group1.add( u1 );
        group1.add( u2 );

        Group group2 = new Group( "Blib", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Bob" );
        group2.add( u3 );
        group2.add( u4 );

        assertTrue( group1.equals( group2 ) );
    }

    public void testEquals4()
    {
        Group group1 = new Group( "BlibBlab", m_wiki );
        Principal u1 = new WikiPrincipal( "Alice" );
        Principal u2 = new WikiPrincipal( "Bob" );
        group1.add( u1 );
        group1.add( u2 );

        Group group2 = new Group( "Blib", m_wiki );
        Principal u3 = new WikiPrincipal( "Alice" );
        Principal u4 = new WikiPrincipal( "Bob" );
        group2.add( u3 );
        group2.add( u4 );

        assertFalse( m_group.equals( group2 ) );
    }

    public static Test suite()
    {
        return new TestSuite( GroupTest.class );
    }

}