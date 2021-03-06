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
package org.apache.wiki.diff;

import java.util.Properties;

import org.apache.wiki.*;
import org.apache.wiki.diff.ContextualDiffProvider;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;


public class ContextualDiffProviderTest extends TestCase
{
    private TestEngine m_engine = null;
    
    public void setUp() throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        m_engine = new TestEngine(props);
    }

    protected void tearDown() throws Exception
    {
        super.tearDown();
        m_engine.shutdown();
    }

    
    
    /**
     * Sets up some shorthand notation for writing test cases.
     * <p>
     * The quick |^Brown Fox^-Blue Monster-| jumped over |^the^| moon.
     * <p>
     * Get it?
     */
    private void specializedNotation(ContextualDiffProvider diff)
    {
        diff.m_changeEndHtml = "|";
        diff.m_changeStartHtml = "|";

        diff.m_deletionEndHtml = "-";
        diff.m_deletionStartHtml = "-";

        diff.m_diffEnd = "";
        diff.m_diffStart = "";

        diff.m_elidedHeadIndicatorHtml = "...";
        diff.m_elidedTailIndicatorHtml = "...";

        diff.m_emitChangeNextPreviousHyperlinks = false;

        diff.m_insertionEndHtml = "^";
        diff.m_insertionStartHtml = "^";

        diff.m_lineBreakHtml = "";
        diff.m_alternatingSpaceHtml = "_";
    }



    public void testNoChanges() throws Exception
    {
        diffTest(null, "", "", "");
        diffTest(null, "A", "A", "A");
        diffTest(null, "A B", "A B", "A B");

        diffTest(null, "      ", "      ", " _ _ _");
        diffTest(null, "A B  C", "A B  C", "A B _C");
        diffTest(null, "A B   C", "A B   C", "A B _ C");
    }



    public void testSimpleInsertions() throws Exception
    {
        // Ah, the white space trailing an insertion is tacked onto the insertion, this is fair, the
        // alternative would be to greedily take the leading whitespace before the insertion as part
        // of it instead, and that doesn't make any more or less sense. just remember this behaviour
        // when writing tests.

        // Simple inserts...
        diffTest(null, "A C", "A B C", "A |^B ^|C");
        diffTest(null, "A D", "A B C D", "A |^B C ^|D");

        // Simple inserts with spaces...
        diffTest(null, "A C", "A B  C", "A |^B _^|C");
        diffTest(null, "A C", "A B   C", "A |^B _ ^|C");
        diffTest(null, "A C", "A B    C", "A |^B _ _^|C");

        // Just inserted spaces...
        diffTest(null, "A B", "A  B", "A |^_^|B");
        diffTest(null, "A B", "A   B", "A |^_ ^|B");
        diffTest(null, "A B", "A    B", "A |^_ _^|B");
        diffTest(null, "A B", "A     B", "A |^_ _ ^|B");
    }



    public void testSimpleDeletions() throws Exception
    {
        // Simple deletes...
        diffTest(null, "A B C", "A C", "A |-B -|C");
        diffTest(null, "A B C D", "A D", "A |-B C -|D");

        // Simple deletes with spaces...
        diffTest(null, "A B  C", "A C", "A |-B _-|C");
        diffTest(null, "A B   C", "A C", "A |-B _ -|C");

        // Just deleted spaces...
        diffTest(null, "A  B", "A B", "A |-_-|B");
        diffTest(null, "A   B", "A B", "A |-_ -|B");
        diffTest(null, "A    B", "A B", "A |-_ _-|B");
    }



    public void testContextLimits() throws Exception
    {
        // No change
        diffTest("1", "A B C D E F G H I", "A B C D E F G H I", "A...");
        //TODO Hmm, should the diff provider instead return the string, "No Changes"?
        
        // Bad property value, should default to huge context limit and return entire string.
        diffTest("foobar", "A B C D E F G H I", "A B C D F G H I", "A B C D |-E -|F G H I");

        // One simple deletion, limit context to 2...
        diffTest("2", "A B C D E F G H I", "A B C D F G H I", "...D |-E -|F ...");

        // Deletion of first element, limit context to 2...
        diffTest("2", "A B C D E", "B C D E", "|-A -|B ...");
        
        // Deletion of last element, limit context to 2...
        diffTest("2", "A B C D E", "A B C D ", "...D |-E-|");
        
        // Two simple deletions, limit context to 2...
        diffTest("2", "A B C D E F G H I J K L M N O P", "A B C E F G H I J K M N O P",
            "...C |-D -|E ......K |-L -|M ...");
                
    }

    public void testMultiples() throws Exception
    {
        diffTest(null, "A F", "A B C D E F", "A |^B C D E ^|F");
        diffTest(null, "A B C D E F", "A F", "A |-B C D E -|F");
        
    }

    public void testSimpleChanges() throws Exception
    {
        // *changes* are actually an insert and a delete in the output...

        //single change
        diffTest(null, "A B C", "A b C", "A |^b^-B-| C");

        //non-consequtive changes...
        diffTest(null, "A B C D E", "A b C d E", "A |^b^-B-| C |^d^-D-| E");

    }

    // FIXME: This test fails; must be enabled again asap.
    /*
    public void testKnownProblemCases() throws NoRequiredPropertyException, IOException
    {
        //These all fail...
        
        //make two consequtive changes
        diffTest(null, "A B C D", "A b c D", "A |^b c^-B C-| D");
        //acually returns ->                 "A |^b^-B-| |^c^-C-| D"

        //collapse adjacent elements...
        diffTest(null, "A B C D", "A BC D", "A |^BC^-B C-| D");
        //acually returns ->                "A |^BC^-B-| |-C -|D"
        
        
        //These failures are all due to how we process the diff results, we need to collapse 
        //adjacent edits into one...
        
    }
     */
    
    private void diffTest(String contextLimit, String oldText, String newText, String expectedDiff)
        throws Exception
    {
        ContextualDiffProvider diff = new ContextualDiffProvider();

        specializedNotation(diff);

        Properties props = new Properties();
        if (null != contextLimit)
            props.put(ContextualDiffProvider.PROP_UNCHANGED_CONTEXT_LIMIT, contextLimit);

        diff.initialize(null, props);

        props.load( TestEngine.findTestProperties() );
        m_engine.shutdown();
        m_engine = new TestEngine(props);
        
        m_engine.deletePage( "Dummy" );
        WikiContext ctx = m_engine.getWikiContextFactory().newViewContext( m_engine.createPage( "Dummy" ) );
        String actualDiff = diff.makeDiffHtml( ctx, oldText, newText);

        assertEquals(expectedDiff, actualDiff);
    }

    public static Test suite()
    {
        return new TestSuite( ContextualDiffProviderTest.class );
    }

}
