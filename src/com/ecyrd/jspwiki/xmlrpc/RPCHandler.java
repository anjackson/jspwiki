/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.xmlrpc;

import java.io.*;
import org.apache.log4j.Category;
import com.ecyrd.jspwiki.*;
import java.util.*;
import org.apache.xmlrpc.XmlRpcException;

/**
 *  Provides handlers for all RPC routines.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.6
 */
// We could use WikiEngine directly, but because of introspection it would
// show just too many methods to be safe.

public class RPCHandler
{
    /** Error code: no such page. */
    public static final int ERR_NOPAGE = 1;

    private WikiEngine m_engine;

    /**
     *  This is the currently implemented JSPWiki XML-RPC code revision.
     */
    public static final int RPC_VERSION = 1;

    Category log = Category.getInstance( RPCHandler.class ); 

    public RPCHandler( WikiEngine engine )
    {
        m_engine = engine;
    }

    /**
     *  Converts Java string into RPC string.
     */
    private String toRPCString( String src )
    {
        return TextUtil.urlEncodeUTF8( src );
    }

    /**
     *  Converts RPC string (UTF-8, url encoded) into Java string.
     */
    private String fromRPCString( String src )
    {
        return TextUtil.urlDecodeUTF8( src );
    }

    /**
     *  Transforms a Java string into UTF-8.
     */
    private byte[] toRPCBase64( String src )
    {
        try
        {
            return src.getBytes("UTF-8");
        }
        catch( UnsupportedEncodingException e )
        {
            //
            //  You shouldn't be running JSPWiki on a platform that does not
            //  use UTF-8.  We revert to platform default, so that the other
            //  end might have a chance of getting something.
            //
            log.fatal("Platform does not support UTF-8, reverting to platform default");
            return src.getBytes();
        }
    }

    public String getApplicationName()
    {
        return toRPCString(m_engine.getApplicationName());
    }

    /**
     *  Returns the current supported JSPWiki XML-RPC API.
     */
    public int getRPCVersionSupported()
    {
        return RPC_VERSION;
    }

    public Vector getAllPages()
    {
        Collection pages = m_engine.getRecentChanges();
        Vector result = new Vector();
        int count = 0;

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            result.add( toRPCString(((WikiPage)i.next()).getName()) );
        }

        return result;
    }

    /**
     *  Encodes a single wiki page info into a Hashtable.
     */
    private Hashtable encodeWikiPage( WikiPage page )
    {
        Hashtable ht = new Hashtable();

        ht.put( "name", toRPCString(page.getName()) );

        Date d = page.getLastModified();

        //
        //  Here we reset the DST and TIMEZONE offsets of the
        //  calendar.  Unfortunately, I haven't thought of a better
        //  way to ensure that we're getting the proper date
        //  from the XML-RPC thingy, except to manually adjust the date.
        //

        Calendar cal = Calendar.getInstance();
        cal.setTime( d );
        cal.set( Calendar.MILLISECOND, 
                 - (cal.get( Calendar.ZONE_OFFSET ) + 
                    (cal.getTimeZone().inDaylightTime( d ) ? cal.get( Calendar.DST_OFFSET ) : 0 )) );

        ht.put( "lastModified", cal.getTime() );
        ht.put( "version", new Integer(page.getVersion()) );
        ht.put( "author", toRPCString(page.getAuthor()) );

        return ht;
    }

    // FIXME: It is not certain if date is really UTC?
    public Vector getRecentChanges( Date since )
    {
        Collection pages = m_engine.getRecentChanges();
        Vector result = new Vector();

        Calendar cal = Calendar.getInstance();
        cal.setTime( since );
        cal.setTimeZone( TimeZone.getDefault() );

        for( Iterator i = pages.iterator(); i.hasNext(); )
        {
            WikiPage page = (WikiPage)i.next();

            if( page.getLastModified().after( since ) )
            {
                result.add( encodeWikiPage( page ) );
            }
        }

        return result;
    }

    /**
     *  Simple helper method, turns the incoming page name into
     *  normal Java string, then checks page condition.
     *
     *  @param pagename Page Name as an RPC string (URL-encoded UTF-8)
     *  @return Real page name, as Java string.
     *  @throws XmlRpcException, if there is something wrong with the page.
     */
    private String parsePageCheckCondition( String pagename )
        throws XmlRpcException
    {
        pagename = fromRPCString( pagename );

        if( !m_engine.pageExists(pagename) )
        {
            throw new XmlRpcException( ERR_NOPAGE, "No such page '"+pagename+"' found, o master." );
        }

        return pagename;
    }

    public Hashtable getPageInfo( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );
        return encodeWikiPage( m_engine.getPage(pagename) );
    }

    public Hashtable getPageInfoVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return encodeWikiPage( m_engine.getPage( pagename, version ) );
    }

    public byte[] getPage( String pagename )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        String text = m_engine.getPureText( pagename, -1 );

        return toRPCBase64( text );
    }

    public byte[] getPageVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getPureText( pagename, version ) );
    }

    public byte[] getPageHTML( String pagename )
        throws XmlRpcException    
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getHTML( pagename ) );
    }

    public byte[] getPageHTMLVersion( String pagename, int version )
        throws XmlRpcException
    {
        pagename = parsePageCheckCondition( pagename );

        return toRPCBase64( m_engine.getHTML( pagename, version ) );
    }
}
