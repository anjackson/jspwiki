
package com.ecyrd.jspwiki;

import junit.framework.*;
import java.io.*;
import java.util.*;
import javax.servlet.*;

public class TranslatorReaderTest extends TestCase
{
    Properties props = new Properties();
    Vector     created = new Vector();

    TestEngine testEngine;

    public TranslatorReaderTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        props.setProperty( "jspwiki.translatorReader.matchEnglishPlurals", "true" );
        testEngine = new TestEngine( props );
    }

    public void tearDown()
    {
        deleteCreatedPages();
    }

    private void newPage( String name )
    {
        testEngine.saveText( name, "<test>" );

        created.addElement( name );
    }

    private void deleteCreatedPages()
    {
        for( Iterator i = created.iterator(); i.hasNext(); )
        {
            String name = (String) i.next();

            testEngine.deletePage(name);
        }

        created.clear();
    }

    private String translate( String src )
        throws IOException,
               NoRequiredPropertyException,
               ServletException
    {
        WikiContext context = new WikiContext( testEngine,
                                               "testpage" );
        Reader r = new TranslatorReader( context, 
                                         new BufferedReader( new StringReader(src)) );
        StringWriter out = new StringWriter();
        int c;

        while( ( c=r.read()) != -1 )
        {
            out.write( c );
        }

        return out.toString();
    }

    public void testHyperlinks2()
        throws Exception
    {
        newPage("Hyperlink");

        String src = "This should be a [hyperlink]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=Hyperlink\">hyperlink</A>\n",
                      translate(src) );
    }

    public void testHyperlinks3()
        throws Exception
    {
        newPage("HyperlinkToo");

        String src = "This should be a [hyperlink too]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperlinkToo\">hyperlink too</A>\n",
                      translate(src) );
    }

    public void testHyperlinks4()
        throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [HyperLink]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>\n",
                      translate(src) );
    }

    public void testHyperlinks5()
        throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [here|HyperLink]";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">here</A>\n",
                      translate(src) );
    }

    /** When using CC links, this seems to crash 1.9.2. */

    public void testHyperLinks6()
        throws Exception
    {
        newPage("DiscussionAboutWiki");
        newPage("WikiMarkupDevelopment");

        String src = "[DiscussionAboutWiki] [WikiMarkupDevelopment].";

        assertEquals( "<A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=DiscussionAboutWiki\">DiscussionAboutWiki</A> <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=WikiMarkupDevelopment\">WikiMarkupDevelopment</A>.\n",
                      translate(src) );       
    }


    public void testHyperlinksCC()
        throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a HyperLink.";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>.\n",
                      translate(src) );
    }

    public void testHyperlinksCCNonExistant()
        throws Exception
    {
        String src = "This should be a HyperLink.";

        assertEquals( "This should be a <U>HyperLink</U><A HREF=\"Edit.jsp?page=HyperLink\">?</A>.\n",
                      translate(src) );
    }

    /**
     *  Check if the CC hyperlink translator gets confused with
     *  unorthodox bracketed links.
     */
    public void testHyperlinksCC2()
        throws Exception
    {
        newPage("HyperLink");

        String src = "This should be a [  HyperLink  ].";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">  HyperLink  </A>.\n",
                      translate(src) );
    }

    public void testHyperlinksCC3()
        throws Exception
    {
        String src = "This should be a nonHyperLink.";

        assertEquals( "This should be a nonHyperLink.\n",
                      translate(src) );
    }

    /** Two links on same line. */
    public void testHyperlinksCC4()
        throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "This should be a HyperLink, and ThisToo.";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>, and <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ThisToo\">ThisToo</A>.\n",
                      translate(src) );
    }

    /** Two mixed links on same line. */
    public void testHyperlinksCC5()
        throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "This should be a [HyperLink], and ThisToo.";

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>, and <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ThisToo\">ThisToo</A>.\n",
                      translate(src) );
    }

    /** Closing tags only. */
    public void testHyperlinksCC6()
        throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "] This ] should be a HyperLink], and ThisToo.";

        assertEquals( "] This ] should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>], and <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ThisToo\">ThisToo</A>.\n",
                      translate(src) );
    }

    /** First and last words on line. */
    public void testHyperlinksCCFirstAndLast()
        throws Exception
    {
        newPage("HyperLink");
        newPage("ThisToo");

        String src = "HyperLink, and ThisToo";

        assertEquals( "<A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLink</A>, and <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ThisToo\">ThisToo</A>\n",
                      translate(src) );
    }

    /** Hyperlinks inside URIs. */
    public void testHyperlinksCCURLs()
        throws Exception
    {
        String src = "http://www.foo.bar/ANewHope/";

        assertEquals( "http://www.foo.bar/ANewHope/\n",
                      translate(src) );
    }

    public void testHyperlinksExt()
        throws Exception
    {
        String src = "This should be a [http://www.regex.fi/]";

        assertEquals( "This should be a <A CLASS=\"external\" HREF=\"http://www.regex.fi/\">http://www.regex.fi/</A>\n",
                      translate(src) );
    }

    public void testHyperlinksExt2()
        throws Exception
    {
        String src = "This should be a [link|http://www.regex.fi/]";

        assertEquals( "This should be a <A CLASS=\"external\" HREF=\"http://www.regex.fi/\">link</A>\n",
                      translate(src) );
    }

    public void testHyperlinksPluralMatch()
        throws Exception
    {
        String src = "This should be a [HyperLinks]";

        newPage("HyperLink");

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLink\">HyperLinks</A>\n",
                      translate(src) );
    }

    public void testHyperlinksPluralMatch2()
        throws Exception
    {
        String src = "This should be a [HyperLinks]";

        assertEquals( "This should be a <U>HyperLinks</U><A HREF=\"Edit.jsp?page=HyperLinks\">?</A>\n",
                      translate(src) );
    }

    public void testHyperlinksPluralMatch3()
        throws Exception
    {
        String src = "This should be a [HyperLink]";

        newPage("HyperLinks");

        assertEquals( "This should be a <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=HyperLinks\">HyperLink</A>\n",
                      translate(src) );
    }


    public void testHyperlinkJS1()
        throws Exception
    {
        String src = "This should be a [link|http://www.haxored.com/\" onMouseOver=\"alert('Hahhaa');\"]";

        assertEquals( "This should be a <A CLASS=\"external\" HREF=\"http://www.haxored.com/&quot; onMouseOver=&quot;alert('Hahhaa');&quot;\">link</A>\n",
                      translate(src) );
    }

    public void testHyperlinksInterWiki1()
        throws Exception
    {
        String src = "This should be a [link|JSPWiki:HyperLink]";

        assertEquals( "This should be a <A CLASS=\"interwiki\" HREF=\"http://www.ecyrd.com/JSPWiki/Wiki.jsp?page=HyperLink\">link</A>\n",
                      translate(src) );
    }

    public void testExtraPagename1()
        throws Exception
    {
        String src = "Link [test_page]";

        newPage("Test_page");

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=Test_page\">test_page</A>\n",
                     translate(src) );
    }

    public void testExtraPagename2()
        throws Exception
    {
        String src = "Link [test.page]";

        newPage("Test.page");

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=Test.page\">test.page</A>\n",
                     translate(src) );
    }

    public void testExtraPagename3()
        throws Exception
    {
        String src = "Link [.testpage_]";

        newPage(".testpage_");

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=.testpage_\">.testpage_</A>\n",
                     translate(src) );
    }

    public void testInlineImages()
        throws Exception
    {
        String src = "Link [test|http://www.ecyrd.com/test.png]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://www.ecyrd.com/test.png\" ALT=\"test\">\n",
                     translate(src) );
    }

    public void testInlineImages2()
        throws Exception
    {
        String src = "Link [test|http://www.ecyrd.com/test.ppm]";

        assertEquals("Link <A CLASS=\"external\" HREF=\"http://www.ecyrd.com/test.ppm\">test</A>\n",
                     translate(src) );
    }

    public void testInlineImages3()
        throws Exception
    {
        String src = "Link [test|http://images.com/testi]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://images.com/testi\" ALT=\"test\">\n",
                     translate(src) );
    }

    public void testInlineImages4()
        throws Exception
    {
        String src = "Link [test|http://foobar.jpg]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://foobar.jpg\" ALT=\"test\">\n",
                     translate(src) );
    }

    // No link text should be just embedded link.
    public void testInlineImagesLink2()
        throws Exception
    {
        String src = "Link [http://foobar.jpg]";

        assertEquals("Link <IMG CLASS=\"inline\" SRC=\"http://foobar.jpg\" ALT=\"http://foobar.jpg\">\n",
                     translate(src) );
    }

    public void testInlineImagesLink()
        throws Exception
    {
        String src = "Link [http://link.to/|http://foobar.jpg]";

        assertEquals("Link <A HREF=\"http://link.to/\"><IMG CLASS=\"inline\" SRC=\"http://foobar.jpg\"></A>\n",
                     translate(src) );
    }

    public void testInlineImagesLink3()
        throws Exception
    {
        String src = "Link [SandBox|http://foobar.jpg]";

        newPage("SandBox");

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=SandBox\"><IMG CLASS=\"inline\" SRC=\"http://foobar.jpg\" ALT=\"SandBox\"></A>\n",
                     translate(src) );
    }

    public void testScandicPagename1()
        throws Exception
    {
        String src = "Link [��Test]";

        newPage("��Test"); // FIXME: Should be capital �

        assertEquals("Link <A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=%C5%E4Test\">��Test</A>\n",
                     translate(src));
    }

    public void testParagraph()
        throws Exception
    {
        String src = "1\n\n2\n\n3";

        assertEquals( "1\n<P>\n2\n<P>\n3\n", translate(src) );
    }



    public void testLinebreak()
        throws Exception
    {
        String src = "1\\\\2";

        assertEquals( "1<BR>2\n", translate(src) );
    }

    public void testTT()
        throws Exception
    {
        String src = "1{{2345}}6";

        assertEquals( "1<TT>2345</TT>6\n", translate(src) );
    }

    public void testTTAcrossLines()
        throws Exception
    {
        String src = "1{{\n2345\n}}6";

        assertEquals( "1<TT>\n2345\n</TT>6\n", translate(src) );
    }

    public void testTTLinks()
        throws Exception
    {
        String src = "1{{\n2345\n[a link]\n}}6";

        newPage("ALink");

        assertEquals( "1<TT>\n2345\n<A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ALink\">a link</A>\n</TT>6\n", translate(src) );
    }

    public void testPre()
        throws Exception
    {
        String src = "1{{{2345}}}6";

        assertEquals( "1<PRE>2345</PRE>6\n", translate(src) );
    }

    public void testPre2()
        throws Exception
    {
        String src = "1 {{{ {{{ 2345 }}} }}} 6";

        assertEquals( "1 <PRE> {{{ 2345 </PRE> }}} 6\n", translate(src) );
    }

    public void testList1()
        throws Exception
    {
        String src = "A list:\n* One\n* Two\n* Three\n";

        assertEquals( "A list:\n<UL>\n<LI> One\n<LI> Two\n<LI> Three\n</UL>\n", 
                      translate(src) );
    }

    /** Plain multi line testing:
        <pre>
        * One
          continuing
        * Two
        * Three
        </pre>
     */
    public void testMultilineList1()
        throws Exception
    {
        String src = "A list:\n* One\n continuing.\n* Two\n* Three\n";

        assertEquals( "A list:\n<UL>\n<LI> One\n continuing.\n<LI> Two\n<LI> Three\n</UL>\n", 
                      translate(src) );
    }

    public void testMultilineList2()
        throws Exception
    {
        String src = "A list:\n* One\n continuing.\n* Two\n* Three\nShould be normal.";

        assertEquals( "A list:\n<UL>\n<LI> One\n continuing.\n<LI> Two\n<LI> Three\n</UL>\nShould be normal.\n", 
                      translate(src) );
    }

    public void testHTML()
        throws Exception
    {
        String src = "<B>Test</B>";

        assertEquals( "&lt;B&gt;Test&lt;/B&gt;\n", translate(src) );
    }

    public void testHTML2()
        throws Exception
    {
        String src = "<P>";

        assertEquals( "&lt;P&gt;\n", translate(src) );
    }

    public void testQuotes()
        throws Exception
    {
        String src = "\"Test\"\"";

        assertEquals( "&quot;Test&quot;&quot;\n", translate(src) );
    }

    public void testItalicAcrossLinebreak()
        throws Exception
    {
        String src="''This is a\ntest.''";

        assertEquals( "<I>This is a\ntest.</I>\n", translate(src) );
    }

    public void testBoldAcrossLinebreak()
        throws Exception
    {
        String src="__This is a\ntest.__";

        assertEquals( "<B>This is a\ntest.</B>\n", translate(src) );
    }

    public void testBoldItalic()
        throws Exception
    {
        String src="__This ''is'' a test.__";

        assertEquals( "<B>This <I>is</I> a test.</B>\n", translate(src) );
    }

    public void testFootnote1()
        throws Exception
    {
        String src="Footnote[1]";

        assertEquals( "Footnote<A CLASS=\"footnoteref\" HREF=\"#ref-testpage-1\">[1]</A>\n", 
                      translate(src) );
    }

    public void testFootnote2()
        throws Exception
    {
        String src="[#2356] Footnote.";

        assertEquals( "<A CLASS=\"footnote\" NAME=\"ref-testpage-2356\">[#2356]</A> Footnote.\n", 
                      translate(src) );
    }

    /** Check an reported error condition where empty list items could cause crashes */

    public void testEmptySecondLevelList()
        throws Exception
    {
        String src="A\n\n**\n\nB";

        assertEquals( "A\n<P>\n<UL>\n<UL>\n<LI>\n</UL>\n</UL>\n<P>\nB\n", 
                      translate(src) );
    }

    public void testEmptySecondLevelList2()
        throws Exception
    {
        String src="A\n\n##\n\nB";

        assertEquals( "A\n<P>\n<OL>\n<OL>\n<LI>\n</OL>\n</OL>\n<P>\nB\n", 
                      translate(src) );
    }

    /**
     * <pre>
     *   *Item A
     *   ##Numbered 1
     *   ##Numbered 2
     *   *Item B
     * </pre>
     *
     * would come out as:
     *<ul>
     * <li>Item A
     * </ul>
     * <ol>
     * <ol>
     * <li>Numbered 1
     * <li>Numbered 2
     * <ul>
     * <li></ol>
     * </ol>
     * Item B
     * </ul>
     *
     *  (by Mahlen Morris).
     */

    // FIXME: does not run - code base is too screwed for that.

    /*
    public void testMixedList()
        throws Exception
    {
        String src="*Item A\n##Numbered 1\n##Numbered 2\n*Item B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<UL><LI>Item A"+
                      "<OL><OL><LI>Numbered 1"+
                      "<LI>Numbered 2"+
                      "</OL></OL>"+
                      "<LI>Item B"+
                      "</UL>",
                      result );
    }
    */
    /**
     *  Like testMixedList() but the list types have been reversed.
     */
    // FIXME: does not run - code base is too screwed for that.
    /*
    public void testMixedList2()
        throws Exception
    {
        String src="#Item A\n**Numbered 1\n**Numbered 2\n#Item B\n";

        String result = translate(src);

        // Remove newlines for easier parsing.
        result = TextUtil.replaceString( result, "\n", "" );

        assertEquals( "<OL><LI>Item A"+
                      "<UL><UL><LI>Numbered 1"+
                      "<LI>Numbered 2"+
                      "</UL></UL>"+
                      "<LI>Item B"+
                      "</OL>",
                      result );
    }
    */

    public void testPluginInsert()
        throws Exception
    {
        String src="[{INSERT com.ecyrd.jspwiki.plugin.SamplePlugin WHERE text=test}]";

        assertEquals( "test\n", translate(src) );
    }

    public void testPluginInsertJS()
        throws Exception
    {
        String src="Today: [{INSERT JavaScriptPlugin}] ''day''.";

        assertEquals( "Today: <script language=\"JavaScript\"><!--\nfoo='';\n--></script>\n <I>day</I>.\n", translate(src) );
    }

    public void testShortPluginInsert()
        throws Exception
    {
        String src="[{INSERT SamplePlugin WHERE text=test}]";

        assertEquals( "test\n", translate(src) );
    }

    public void testTable1()
        throws Exception
    {
        String src="|| heading || heading2 \n| Cell 1 | Cell 2 \n| Cell 3 | Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TH> heading <TH> heading2 </TR>\n"+
                      "<TR><TD> Cell 1 <TD> Cell 2 </TR>\n"+
                      "<TR><TD> Cell 3 <TD> Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testTable2()
        throws Exception
    {
        String src="||heading||heading2\n|Cell 1| Cell 2\n| Cell 3 |Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TH>heading<TH>heading2</TR>\n"+
                      "<TR><TD>Cell 1<TD> Cell 2</TR>\n"+
                      "<TR><TD> Cell 3 <TD>Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testTable3()
        throws Exception
    {
        String src="|Cell 1| Cell 2\n| Cell 3 |Cell 4\n\n";

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TD>Cell 1<TD> Cell 2</TR>\n"+
                      "<TR><TD> Cell 3 <TD>Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testTableLink()
        throws Exception
    {
        String src="|Cell 1| Cell 2\n|[Cell 3|ReallyALink]|Cell 4\n\n";

        newPage("ReallyALink");

        assertEquals( "<TABLE CLASS=\"wikitable\" BORDER=\"1\">\n"+
                      "<TR><TD>Cell 1<TD> Cell 2</TR>\n"+
                      "<TR><TD><A CLASS=\"wikipage\" HREF=\"Wiki.jsp?page=ReallyALink\">Cell 3</A><TD>Cell 4</TR>\n"+
                      "</TABLE><P>\n",
                      translate(src) );
    }

    public void testDescription()
        throws Exception
    {
        String src=";:Foo";

        assertEquals( "<DL>\n<DT></DT><DD>Foo</DD>\n</DL>\n",
                      translate(src) );
    }

    public void testDescription2()
        throws Exception
    {
        String src=";Bar:Foo";

        assertEquals( "<DL>\n<DT>Bar</DT><DD>Foo</DD>\n</DL>\n",
                      translate(src) );
    }

    public void testDescription3()
        throws Exception
    {
        String src=";:";

        assertEquals( "<DL>\n<DT></DT><DD></DD>\n</DL>\n",
                      translate(src) );
    }

    public static Test suite()
    {
        return new TestSuite( TranslatorReaderTest.class );
    }
}
