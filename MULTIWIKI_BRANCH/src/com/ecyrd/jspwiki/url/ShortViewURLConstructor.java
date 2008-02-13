/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2005 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki.url;

import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;

/**
 *  A specific URL constructor that returns easy-to-grok URLs for
 *  VIEW and ATTACH contexts, but goes through JSP pages otherwise.
 * 
 *  @author jalkanen
 *
 *  @since 2.2
 */
public class ShortViewURLConstructor 
    extends ShortURLConstructor
{
    /**
     *  {@inheritDoc}
     */
    public void initialize( WikiEngine engine, 
                            Properties properties )
    {
        super.initialize( engine, properties );
    }
    
    private String makeURL( String context,
                            String name,
                            boolean absolute )
    {
        String viewurl = "%p"+m_urlPrefix+"%n";

        if( absolute ) 
            viewurl = "%u"+m_urlPrefix+"%n";

        if( context.equals(WikiContext.VIEW) )
        {
            if( name == null ) return doReplacement("%u","",absolute);
            return doReplacement( viewurl, name, absolute );
        }

        return doReplacement( DefaultURLConstructor.getURLPattern(context,name),
                              name,
                              absolute );
    }

    /**
     * {@inheritDoc}
     */
    public String makeURL( String context,
                           String name,
                           boolean absolute,
                           String parameters )
    {
        if( parameters != null && parameters.length() > 0 )
        {            
            if( context.equals(WikiContext.ATTACH) || context.equals(WikiContext.VIEW) || name == null )
            {
                parameters = "?"+parameters;
            }
            else if( context.equals(WikiContext.NONE) )
            {
                parameters = (name.indexOf('?') != -1 ) ? "&amp;" : "?" + parameters;
            }
            else
            {
                parameters = "&amp;"+parameters;
            }
        }
        else
        {
            parameters = "";
        }
        return makeURL( context, name, absolute )+parameters;
    }
    
    /**
     *   Since we're only called from WikiServlet, where we get the VIEW requests,
     *   we can safely return this.
     *   
     *   @param {@inheritDoc}
     *   @return {@inheritDoc}
     */
    public String getForwardPage( HttpServletRequest req )
    {        
        return "Wiki.jsp";
    }
}