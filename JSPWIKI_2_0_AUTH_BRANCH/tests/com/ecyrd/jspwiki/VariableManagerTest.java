
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;
import org.apache.log4j.*;

public class VariableManagerTest extends TestCase
{
    VariableManager m_variableManager;
    WikiContext     m_context;

    static final String PAGE_NAME = "TestPage";

    public VariableManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        Properties props = new Properties();
        try
        {
            props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );
            PropertyConfigurator.configure(props);

            m_variableManager = new VariableManager( props );
            TestEngine testEngine = new TestEngine( props );
            m_context = new WikiContext( testEngine,
                                         PAGE_NAME );

        }
        catch( IOException e ) {}
    }

    public void tearDown()
    {
    }

    public void testIllegalInsert1()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testIllegalInsert2()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testIllegalInsert3()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$pagename" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testIllegalInsert4()
        throws Exception
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$}" );
            fail( "Did not fail" );
        }
        catch( IllegalArgumentException e )
        {
            // OK.
        }
    }

    public void testNonExistantVariable()
    {
        try
        {
            m_variableManager.parseAndGetValue( m_context, "{$no_such_variable}" );
            fail( "Did not fail" );
        }
        catch( NoSuchVariableException e )
        {
            // OK.
        }
    }

    public void testPageName()
        throws Exception
    {
        String res = m_variableManager.getValue( m_context, "pagename" );

        assertEquals( PAGE_NAME, res );
    }

    public void testPageName2()
        throws Exception
    {
        String res =  m_variableManager.parseAndGetValue( m_context, "{$  pagename  }" );

        assertEquals( PAGE_NAME, res );
    }

    public void testMixedCase()
        throws Exception
    {
        String res =  m_variableManager.parseAndGetValue( m_context, "{$PAGeNamE}" );

        assertEquals( PAGE_NAME, res );
    }

    public static Test suite()
    {
        return new TestSuite( VariableManagerTest.class );
    }
}
