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
package com.ecyrd.jspwiki.tags;

import java.io.IOException;
import javax.servlet.jsp.JspWriter;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiPage;

/**
 *  Writes an edit link.  Body of the link becomes the link text.
 *  <P><B>Attributes<B></P>
 *  <UL>
 *    <LI>page - Page name to refer to.  Default is the current page.
 *    <LI>format - Format, either "anchor" or "url".
 *    <LI>version - Version number of the page to refer to.  Possible values
 *        are "this", meaning the version of the current page; or a version
 *        number.  Default is always to point at the latest version of the page.
 *  </UL>
 *
 *  @author Janne Jalkanen
 *  @since 2.0
 */
public class EditLinkTag
    extends WikiLinkTag
{
    public String m_version = null;

    public void setVersion( String vers )
    {
        m_version = vers;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiEngine engine   = m_wikiContext.getEngine();
        WikiPage   page;
        String     versionString = "";

        if( m_pageName == null )
        {
            if( m_wikiContext.getPage() != null )
            {
                page = m_wikiContext.getPage();
            }
            else
            {
                return SKIP_BODY;
            }
        }
        else
        {
            page = engine.getPage( m_pageName );
        }

        if( m_version != null )
        {
            if( "this".equalsIgnoreCase(m_version) )
            {
                versionString = "&version="+page.getVersion();
            }
            else
            {
                versionString = "&version="+m_version;
            }
        }

        JspWriter out = pageContext.getOut();
        String encodedlink = engine.encodeName( page.getName() );

        switch( m_format )
        {
          case ANCHOR:
            out.print("<A HREF=\""+engine.getBaseURL()+"Edit.jsp?page="+encodedlink+versionString+"\">");
            break;

          case URL:
            out.print( engine.getBaseURL()+"Edit.jsp?page="+encodedlink+versionString );
            break;
        }

        return EVAL_BODY_INCLUDE;
    }
}
