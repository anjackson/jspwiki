
package com.ecyrd.jspwiki;
import java.util.Properties;
import javax.servlet.*;
import java.io.*;

import org.apache.log4j.Category;

import com.ecyrd.jspwiki.providers.*;

/**
 *  Simple test engine that always assumes pages are found.
 */
public class TestEngine extends WikiEngine
{
    static Category log = Category.getInstance( TestEngine.class );

    public TestEngine( Properties props )
        throws WikiException
    {
        super( props );
    }

    /**
     *  Deletes all files under this directory, and does them recursively.
     */
    public static void deleteAll( File file )
    {
        if( file.isDirectory() )
        {
            File[] files = file.listFiles();

            for( int i = 0; i < files.length; i++ )
            {
                if( files[i].isDirectory() )
                {
                    deleteAll(files[i]);                
                }

                files[i].delete();
            }
        }

        file.delete();
    }

    /**
     *  Copied from FileSystemProvider
     */
    protected String mangleName( String pagename )
    {
        // FIXME: Horrible kludge, very slow, etc.
        if( "UTF-8".equals( getContentEncoding() ) )
            return TextUtil.urlEncodeUTF8( pagename );

        return java.net.URLEncoder.encode( pagename );
    }

    public void deletePage( String name )
    {
        String files = getWikiProperties().getProperty( FileSystemProvider.PROP_PAGEDIR );

        try
        {
            File f = new File( files, mangleName(name)+FileSystemProvider.FILE_EXT );

            f.delete();
        }
        catch( Exception e ) 
        {
            log.error("Couldn't delete "+name, e );
        }
    }

}
