
package com.ecyrd.jspwiki.attachment;

import junit.framework.*;
import java.io.*;
import java.util.*;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.providers.*;

public class AttachmentManagerTest extends TestCase
{
    public static final String NAME1 = "TestPage";

    Properties props = new Properties();

    TestEngine m_engine;
    AttachmentManager m_manager;

    static String c_fileContents = "ABCDEFGHIJKLMNOPQRSTUVWxyz";

    public AttachmentManagerTest( String s )
    {
        super( s );
    }

    public void setUp()
        throws Exception
    {
        props.load( getClass().getClassLoader().getResourceAsStream("/jspwiki.properties") );

        m_engine  = new TestEngine(props);
        m_manager = m_engine.getAttachmentManager();

        m_engine.saveText( NAME1, "Foobar" );
    }

    private File makeAttachmentFile()
        throws Exception
    {
        File tmpFile = File.createTempFile("test","txt");
        tmpFile.deleteOnExit();

        FileWriter out = new FileWriter( tmpFile );
        
        FileUtil.copyContents( new StringReader( c_fileContents ), out );

        out.close();
        
        return tmpFile;
    }

    public void tearDown()
    {
        m_engine.deletePage( NAME1 );

        String tmpfiles = props.getProperty( BasicAttachmentProvider.PROP_STORAGEDIR );

        File f = new File( tmpfiles, NAME1+BasicAttachmentProvider.DIR_EXTENSION );

        m_engine.deleteAll( f );
    }

    public void testEnabled()        
    {
        assertTrue( "not enabled", m_manager.attachmentsEnabled() );
    }

    public void testSimpleStore()
        throws Exception
    {
        Attachment att = new Attachment( NAME1 );

        att.setFileName( "test1.txt" );
        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Attachment att2 = m_manager.getAttachmentInfo( new WikiPage(NAME1), "test1.txt" );

        assertNotNull( "attachment disappeared", att2 );
        assertEquals( "name", att.getName(), att2.getName() );
        assertEquals( "author", att.getAuthor(), att2.getAuthor() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        sout.close();

        assertEquals( "contents", c_fileContents, sout.toString() );
    }

    public void testMultipleStore()
        throws Exception
    {
        Attachment att = new Attachment( NAME1 );

        att.setFileName( "test1.txt" );
        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        att.setAuthor( "FooBar" );
        m_manager.storeAttachment( att, makeAttachmentFile() );        

        Attachment att2 = m_manager.getAttachmentInfo( new WikiPage(NAME1), "test1.txt" );

        assertNotNull( "attachment disappeared", att2 );
        assertEquals( "name", att.getName(), att2.getName() );
        assertEquals( "author", att.getAuthor(), att2.getAuthor() );
        assertEquals( "version", 2, att2.getVersion() );

        InputStream in = m_manager.getAttachmentStream( att2 );

        assertNotNull( "stream", in );

        StringWriter sout = new StringWriter();
        FileUtil.copyContents( new InputStreamReader(in), sout );

        sout.close();

        assertEquals( "contents", c_fileContents, sout.toString() );
    }

    public void testListAttachments()
        throws Exception
    {
        Attachment att = new Attachment( NAME1 );

        att.setFileName( "test1.txt" );
        att.setAuthor( "FirstPost" );

        m_manager.storeAttachment( att, makeAttachmentFile() );

        Collection c = m_manager.listAttachments( new WikiPage(NAME1) );

        assertEquals( "Length", c.size(), 1 );

        Attachment att2 = (Attachment) c.toArray()[0];

        assertEquals( "name", att.getName(), att2.getName() );
        assertEquals( "author", att.getAuthor(), att2.getAuthor() );        
    }

    public static Test suite()
    {
        return new TestSuite( AttachmentManagerTest.class );
    }


}
