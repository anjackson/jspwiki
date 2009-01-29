package com.ecyrd.jspwiki.content;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.jspwiki.api.WikiException;
import org.apache.jspwiki.api.WikiPage;

import com.ecyrd.jspwiki.TestEngine;

public class ContentManagerTest extends TestCase
{
    ContentManager m_mgr;
    TestEngine     m_engine;
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        
        m_engine = new TestEngine(props);
        
        m_mgr = m_engine.getContentManager();
    }

    @Override
    protected void tearDown() throws Exception
    {
        WikiPage p = m_mgr.getPage( WikiName.valueOf("Main:TestPage") );
        
        if( p != null ) m_mgr.deletePage( p );
        
        super.tearDown();
    }

    public void testContentManagerGet() throws WikiException
    {
        assertNotNull(m_mgr);
    }
    
    public void testBasicGet() throws Exception
    {
        String content = "Test Content";
        WikiPage page = m_mgr.addPage( WikiName.valueOf("Main:TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        assertNotNull("WikiPage create", page);
        
        page.setContent( content );
        
        page.save();
        
        WikiPage page2 = m_mgr.getPage( WikiName.valueOf("Main:TestPage") );
        
        assertNotNull( "page2", page2 );
        
        assertEquals("Content", page.getContentAsString(), page2.getContentAsString() );
        assertEquals("Content 2", content, page2.getContentAsString() );
    }
    
    public void testBasicGetDefaultSpace() throws Exception
    {
        String content = "Test Content";
        WikiPage page = m_mgr.addPage( WikiName.valueOf("TestPage"), ContentManager.JSPWIKI_CONTENT_TYPE );
        
        assertNotNull("WikiPage create", page);
        
        page.setContent( content );
        
        page.save();
        
        WikiPage page2 = m_mgr.getPage( WikiName.valueOf("TestPage") );
        
        assertNotNull( "page2", page2 );
        
        assertEquals("Content", page.getContentAsString(), page2.getContentAsString() );
        assertEquals("Content 2", content, page2.getContentAsString() );
    }
    
    public void testPaths() throws Exception
    {
        assertEquals( "One", "/pages/Main/MainPage", ContentManager.getJCRPath( WikiName.valueOf("Main:MainPage") ) );
        
        assertEquals( "Back", WikiName.valueOf("Main:MainPage"), ContentManager.getWikiPath( "/pages/Main/MainPage" ) );
    }
    
    public static Test suite()
    {
        return new TestSuite( ContentManagerTest.class );
    }
    
}
