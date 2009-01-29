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
package com.ecyrd.jspwiki.content;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class WikiNameTest extends TestCase
{
    public void testParse1()
    {
        WikiName wn = WikiName.valueOf( "Foo:Bar/Blob 2" );
        
        assertEquals("space", "Foo", wn.getSpace() );
        assertEquals("path", "Bar/Blob 2", wn.getPath() );
    }

    public void testParse2()
    {
        WikiName wn = WikiName.valueOf( "BarBrian" );
        
        assertEquals("space", ContentManager.DEFAULT_SPACE, wn.getSpace() );
        assertEquals("path", "BarBrian", wn.getPath() );
    }

    public void testResolve1()
    {
        WikiName wn = new WikiName("Test","TestPage");
        
        WikiName newname = wn.resolve("Barbapapa");
        
        assertEquals( "Test:Barbapapa", newname.toString() );
    }

    public void testResolveAbsolute()
    {
        WikiName wn = new WikiName("Test","TestPage");
        
        WikiName newname = wn.resolve("Foo:Barbapapa");
        
        assertEquals( "Foo:Barbapapa", newname.toString() );
    }

    public static Test suite()
    {
        return new TestSuite(WikiNameTest.class);
    }

}
