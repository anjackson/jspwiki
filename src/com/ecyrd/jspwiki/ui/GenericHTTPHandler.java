/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 JSPWiki development group

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
package com.ecyrd.jspwiki.ui;

import com.ecyrd.jspwiki.WikiContext;

/**
 *  Provides a generic HTTP handler interface.
 *  
 *  @author jalkanen
 *
 */
public interface GenericHTTPHandler
{
    
    /**
     *  Get an identifier for this particular AdminBean.  This id MUST
     *  conform to URI rules.  The ID must also be unique across all HTTPHandlers.
     *  
     *  @return the identifier for the bean
     */
    public String getId();
    
    /**
     *  Return basic HTML.
     *  
     *  @param context
     *  @return the HTML for the bean
     */
    public String doGet( WikiContext context );
    
    /**
     *  Handles a POST response.
     *  @param context
     *  @return the response string resulting from the POST
     */
    public String doPost( WikiContext context );
}
