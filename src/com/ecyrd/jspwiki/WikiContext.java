/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.util.HashMap;
import java.util.Map;

public class WikiContext
{
    WikiPage   m_page;
    WikiEngine m_engine;
    String     m_requestContext = VIEW;

    Map        m_variableMap = new HashMap();

    public static final String    VIEW     = "view";
    public static final String    EDIT     = "edit";
    public static final String    DIFF     = "diff";
    public static final String    INFO     = "info";
    public static final String    PREVIEW  = "preview";
    public static final String    CONFLICT = "conflict";

    public WikiContext( WikiEngine engine, String pagename )
    {
        m_page   = new WikiPage( pagename );
        m_engine = engine;
    }

    public WikiContext( WikiEngine engine, WikiPage page )
    {
        m_page   = page;
        m_engine = engine;
    }

    public WikiEngine getEngine()
    {
        return m_engine;
    }

    public WikiPage getPage()
    {
        return m_page;
    }


    public String getRequestContext()
    {
        return m_requestContext;
    }

    /**
     *  Sets the request context.
     */
    public void setRequestContext( String arg )
    {
        m_requestContext = arg;
    }

    /**
     *  Gets a previously set variable.
     */
    public Object getVariable( String key )
    {
        return m_variableMap.get( key );
    }

    /**
     *  Sets a variable.  The variable is valid while the WikiContext is valid,
     *  i.e. while page processing continues.  The variable data is discarded
     *  once the page processing is finished.
     */
    public void setVariable( String key, Object data )
    {
        m_variableMap.put( key, data );
    }

}
