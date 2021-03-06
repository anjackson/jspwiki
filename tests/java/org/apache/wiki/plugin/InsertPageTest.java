/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.    
 */
package org.apache.wiki.plugin;

import java.util.Properties;

import org.apache.wiki.TestEngine;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class InsertPageTest extends TestCase
{
    protected TestEngine m_testEngine;
    protected Properties m_props = new Properties();
    
    protected void setUp() throws Exception
    {
        m_props.load( TestEngine.findTestProperties() );

        m_testEngine = new TestEngine(m_props);
    }

    protected void tearDown() throws Exception
    {
        m_testEngine.emptyRepository();
        
        m_testEngine.shutdown();
    }

    public void testRecursive() throws Exception
    {
        String src = "[{InsertPage page='ThisPage'}] [{ALLOW view Anonymous}]";
        
        m_testEngine.saveText("ThisPage",src);
        
        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        String res = m_testEngine.getHTML("ThisPage");
        assertTrue( res.indexOf("Circular reference") != -1 );
    }

    public void testRecursive2() throws Exception
    {
        String src  = "[{InsertPage page='ThisPage2'}]";
        String src2 = "[{InsertPage page='ThisPage'}]";
        
        m_testEngine.saveText("ThisPage",src);
        m_testEngine.saveText("ThisPage2",src2);
               
        // Just check that it contains a proper error message; don't bother do HTML
        // checking.
        assertTrue( m_testEngine.getHTML("ThisPage").indexOf("Circular reference") != -1 );
    }

    public void testMultiInvocation() throws Exception
    {
        String src  = "[{InsertPage page='ThisPage2'}] [{InsertPage page='ThisPage2'}]";
        String src2 = "foo[{ALLOW view Anonymous}]";

        m_testEngine.saveText("ThisPage",src);
        m_testEngine.saveText("ThisPage2",src2);

        assertTrue( "got circ ref", m_testEngine.getHTML("ThisPage").indexOf("Circular reference") == -1 );
        
        assertEquals( "found != 2", "<div style=\"\">foo\n</div> <div style=\"\">foo\n</div>\n", m_testEngine.getHTML("ThisPage") );
        
    }
    
    public void testUnderscore() throws Exception
    {
        String src  = "[{InsertPage page='Test_Page'}]";
        String src2 = "foo[{ALLOW view Anonymous}]";

        m_testEngine.saveText("ThisPage",src);
        m_testEngine.saveText("Test_Page",src2);

        assertTrue( "got circ ref", m_testEngine.getHTML("ThisPage").indexOf("Circular reference") == -1 );
        
        assertEquals( "found != 1", "<div style=\"\">foo\n</div>\n", m_testEngine.getHTML("ThisPage") );    
    }
    
    
    /**
     * a link containing a blank should work if there is a page with exact the
     * same name ('Test Page')
     */
    public void testWithBlanks1() throws Exception
    {
        m_testEngine.saveText( "ThisPage", "[{InsertPage page='Test Page'}]" );
        m_testEngine.saveText( "Test Page", "foo[{ALLOW view Anonymous}]" );

        assertEquals( "found != 1", "<div style=\"\">foo\n</div>\n", m_testEngine.getHTML( "ThisPage" ) );
    }

    /**
     * same as testWithBlanks1, but it should still work if the page does not
     * have the blank in it ( 'Test Page' should work if the included page is
     * called 'TestPage')
     */
    public void testWithBlanks2() throws Exception
    {
        m_testEngine.saveText( "ThisPage", "[{InsertPage page='Test Page'}]" );
        m_testEngine.saveText( "TestPage", "foo[{ALLOW view Anonymous}]" );

        assertEquals( "found != 1", "<div style=\"\">foo\n</div>\n", m_testEngine.getHTML( "ThisPage" ) );
    }
    
    public static Test suite()
    {
        return new TestSuite( InsertPageTest.class );
    }
}
