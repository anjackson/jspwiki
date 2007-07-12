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
package com.ecyrd.jspwiki.plugin;

import java.util.Map;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.rpc.RPCCallable;
import com.ecyrd.jspwiki.rpc.json.JSONRPCManager;

/**
 *  Simple plugin which shows how to add JSON calls to your plugin.
 * 
 *  @author Janne Jalkanen
 *  @since  2.5.4
 */
public class RPCSamplePlugin implements WikiPlugin, RPCCallable
{  
    /**
     *  This method is called when the Javascript is encountered by
     *  the browser.
     *  @param echo
     *  @return the string <code>JSON says:</code>, plus the value 
     *  supplied by the <code>echo</code> parameter
     */
    public String myFunction(String echo)
    {
        return "JSON says: "+echo;
    }
    

    
    public String execute(WikiContext context, Map params) throws PluginException
    {
        JSONRPCManager.registerJSONObject( context, this );
        
        String s = JSONRPCManager.emitJSONCall( context, this, "myFunction", "'foo'" );
        
        return s;
    }

}
