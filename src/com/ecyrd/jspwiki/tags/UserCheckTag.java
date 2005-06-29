/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;

/**
 *  Includes the content if an user check validates.  This has
 *  been considerably enhanced for 2.2.  The possibilities for the "status"-argument are:
 *
 * <ul>
 * <li>"anonymous"     - the body of the tag is included 
 *                       if the user is completely unknown (no cookie, no password)
 * <li>"asserted"      - the body of the tag is included 
 *                       if the user has either been named by a cookie, but
 *                       not been authenticated.
 * <li>"authenticated" - the body of the tag is included 
 *                       if the user is validated either through the container,
 *                       or by our own authentication.
 * </ul>
 *
 *  If the old "exists" -argument is used, it corresponds as follows:
 *  <p>
 *  <tt>exists="true" ==> status="known"<br>
 *  <tt>exists="false" ==> status="unknown"<br>
 *
 *  It is NOT a good idea to use BOTH of the arguments.
 *
 *  @author Janne Jalkanen
 *  @author Erik Bunn
 *  @since 2.0
 */
public class UserCheckTag
    extends WikiTagBase
{
    private String m_status;

    public String getStatus()
    {
        return( m_status );
    }

    public void setStatus( String arg )
    {
        m_status = arg;
    }


    /**
     *  Sets the "exists" attribute, which is converted on-the-fly into
     *  an equivalent "status" -attribute.  This is only for backwards compatibility.
     *
     *  @deprecated
     */
    public void setExists( String arg )
    {
        if("true".equals(arg))
        {
            m_status = "authenticated";
        }
        else
        {
            m_status = "anonymous";
        }
    }


    /**
     * ARJ: This method is somewhat re-written. I am not sure I got this 
     * right...
     * @see com.ecyrd.jspwiki.tags.WikiTagBase#doWikiStartTag()
     */
    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine  engine = m_wikiContext.getEngine();
        WikiSession session = m_wikiContext.getWikiSession();
        String status = session.getStatus();

        if( m_status != null )
        {
            if ( "anonymous".equals( m_status )) {
              if (status.equals(WikiSession.ANONYMOUS)) {
                return EVAL_BODY_INCLUDE;
              }
            }
            else if( "authenticated".equals( m_status )) { 
              if (status.equals(WikiSession.AUTHENTICATED)) {
                return EVAL_BODY_INCLUDE;
              }
            }
            else if( "asserted".equals( m_status )) { 
                if (status.equals(WikiSession.ASSERTED)) {
                  return EVAL_BODY_INCLUDE;
                }
            }
        }

        return SKIP_BODY;
    }

}
