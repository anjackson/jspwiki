/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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
package com.ecyrd.jspwiki;

import java.security.Principal;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.filters.PageFilter;
import com.ecyrd.jspwiki.modules.InternalModule;

/**
 *  Manages variables.  Variables are case-insensitive.  A list of all
 *  available variables is on a Wiki page called "WikiVariables".
 *
 *  @author Janne Jalkanen
 *  @since 1.9.20.
 */
public class VariableManager
{
    //private static Logger log = Logger.getLogger( VariableManager.class );
   
    // FIXME: These are probably obsolete.
    public static final String VAR_ERROR = "error";
    public static final String VAR_MSG   = "msg";
    
    /**
     *  Contains a list of those properties that shall never be shown.
     *  Put names here in lower case.
     */
    
    static final String[] THE_BIG_NO_NO_LIST = {
        "jspwiki.auth.masterpassword"
    };
    
    /**
     *  Creates a VariableManager object using the property list given.
     *  @param props The properties.
     */
    public VariableManager( Properties props )
    {
    }

    /**
     *  Returns true if the link is really command to insert
     *  a variable.
     *  <P>
     *  Currently we just check if the link starts with "{$".
     *  
     *  @param link The link text
     *  @return true, if this represents a variable link.
     */
    public static boolean isVariableLink( String link )
    {
        return link.startsWith("{$");
    }

    /**
     *  Parses the link and finds a value.  This is essentially used
     *  once {@link #isVariableLink(String)} has found that the link text
     *  actually contains a variable.  For example, you could pass in
     *  "{$username}" and get back "JanneJalkanen".
     *
     *  @param  context The WikiContext
     *  @param  link    The link text containing the variable name.
     *  @return The variable value.
     *  @throws IllegalArgumentException If the format is not valid (does not 
     *          start with "{$", is zero length, etc.)
     *  @throws NoSuchVariableException If a variable is not known.
     */
    public String parseAndGetValue( WikiContext context,
                                    String link )
        throws IllegalArgumentException,
               NoSuchVariableException
    {
        if( !link.startsWith("{$") )
            throw new IllegalArgumentException( "Link does not start with {$" );

        if( !link.endsWith("}") )
            throw new IllegalArgumentException( "Link does not end with }" );

        String varName = link.substring(2,link.length()-1);

        return getValue( context, varName.trim() );
    }

    /**
     *  This method does in-place expansion of any variables.  However,
     *  the expansion is not done twice, that is, a variable containing text $variable
     *  will not be expanded.
     *  <P>
     *  The variables should be in the same format ({$variablename} as in the web 
     *  pages.
     *
     *  @param context The WikiContext of the current page.
     *  @param source  The source string.
     *  @return The source string with variables expanded.
     */
    // FIXME: somewhat slow.
    public String expandVariables( WikiContext context,
                                   String      source )
    {
        StringBuffer result = new StringBuffer();

        for( int i = 0; i < source.length(); i++ )
        {
            if( source.charAt(i) == '{' )
            {
                if( i < source.length()-2 && source.charAt(i+1) == '$' )
                {
                    int end = source.indexOf( '}', i );

                    if( end != -1 )
                    {
                        String varname = source.substring( i+2, end );
                        String value;

                        try
                        {
                            value = getValue( context, varname );
                        }
                        catch( NoSuchVariableException e )
                        {
                            value = e.getMessage();
                        }
                        catch( IllegalArgumentException e )
                        {
                            value = e.getMessage();
                        }

                        result.append( value );
                        i = end;
                        continue;
                    }
                }
                else
                {
                    result.append( '{' );
                }
            }
            else
            {
                result.append( source.charAt(i) );
            }
        }

        return result.toString();
    }

    /**
     *  Returns the value of a named variable.  See {@link #getValue(WikiContext, String)}.
     *  The only difference is that this method does not throw an exception, but it
     *  returns the given default value instead.
     *  
     *  @param context WikiContext
     *  @param varName The name of the variable
     *  @param defValue A default value.
     *  @return The variable value, or if not found, the default value.
     */
    public String getValue( WikiActionBean actionBean, String varName, String defValue )
    {
        try
        {
            return getValue( actionBean, varName );
        }
        catch( NoSuchVariableException e )
        {
            return defValue;
        }
    }
    
    /**
     *  Returns a value of the named variable.  The resolving order is
     *  <ol>
     *    <li>Known "constant" name, such as "pagename", etc.  This is so
     *        that pages could not override certain constants.
     *    <li>WikiContext local variable.  This allows a programmer to
     *        set a parameter which cannot be overridden by user.
     *    <li>HTTP Session
     *    <li>HTTP Request parameters
     *    <li>WikiPage variable.  As set by the user with the SET directive.
     *    <li>jspwiki.properties
     *  </ol>
     *
     *  Use this method only whenever you really need to have a parameter that
     *  can be overridden by anyone using the wiki.
     *  
     *  @param context The WikiContext
     *  @param varName Name of the variable.
     *
     *  @return The variable value.
     *  
     *  @throws IllegalArgumentException If the name is somehow broken.
     *  @throws NoSuchVariableException If a variable is not known.
     */
    // FIXME: Currently a bit complicated.  Perhaps should use reflection
    //        or something to make an easy way of doing stuff.
    public String getValue( WikiActionBean actionBean,
                            String      varName )
        throws IllegalArgumentException,
               NoSuchVariableException
    {
        if( varName == null )
            throw new IllegalArgumentException( "Null variable name." );

        if( varName.length() == 0 )
            throw new IllegalArgumentException( "Zero length variable name." );

        // Faster than doing equalsIgnoreCase()
        String name = varName.toLowerCase();

        for( int i = 0; i < THE_BIG_NO_NO_LIST.length; i++ )
        {
            if( name.equals(THE_BIG_NO_NO_LIST[i]) )
                return ""; // FIXME: Should this be something different?
        }
        
        if( name.equals("pagename") && actionBean instanceof WikiContext )
        {
            WikiPage page = ((WikiContext)actionBean).getPage();
            return ( page == null ? null : ((WikiContext)actionBean).getPage().getName() );
        }
        else if( name.equals("applicationname") )
        {
            return actionBean.getEngine().getApplicationName();
        }
        else if( name.equals("jspwikiversion") )
        {
            return Release.getVersionString();
        }
        else if( name.equals("encoding") )
        {
            return actionBean.getEngine().getContentEncoding();
        }
        else if( name.equals("totalpages") )
        {
            return Integer.toString(actionBean.getEngine().getPageCount());
        }
        else if( name.equals("pageprovider") )
        {
            return actionBean.getEngine().getCurrentProvider();
        }
        else if( name.equals("pageproviderdescription") )
        {
            return actionBean.getEngine().getCurrentProviderInfo();
        }
        else if( name.equals("attachmentprovider") )
        {
            WikiProvider p = actionBean.getEngine().getAttachmentManager().getCurrentProvider();
            return (p != null) ? p.getClass().getName() : "-";
        }
        else if( name.equals("attachmentproviderdescription") )
        {
            WikiProvider p = actionBean.getEngine().getAttachmentManager().getCurrentProvider();

            return (p != null) ? p.getProviderInfo() : "-";
        }
        else if( name.equals("interwikilinks") )
        {
            StringBuffer res = new StringBuffer();

            for( Iterator i = actionBean.getEngine().getAllInterWikiLinks().iterator(); i.hasNext(); )
            {
                if( res.length() > 0 ) res.append(", ");
                String link = (String) i.next();
                res.append( link );
                res.append( " --> " );
                res.append( actionBean.getEngine().getInterWikiURL(link) );
            }
            return res.toString();
        }
        else if( name.equals("inlinedimages") )
        {
            StringBuffer res = new StringBuffer();

            for( Iterator i = actionBean.getEngine().getAllInlinedImagePatterns().iterator(); i.hasNext(); )
            {
                if( res.length() > 0 ) res.append(", ");
                
                String ptrn = (String) i.next();
                res.append(ptrn);
            }
            
            return res.toString();
        }
        else if( name.equals("pluginpath") )
        {
            String s = actionBean.getEngine().getPluginSearchPath();

            return (s == null) ? "-" : s;
        }
        else if( name.equals("baseurl") )
        {
            return actionBean.getEngine().getBaseURL();
        }
        else if( name.equals("uptime") )
        {
            Date now = new Date();
            long secondsRunning = (now.getTime() - actionBean.getEngine().getStartTime().getTime())/1000L;

            long seconds = secondsRunning % 60;
            long minutes = (secondsRunning /= 60) % 60;
            long hours   = (secondsRunning /= 60) % 24;
            long days    = secondsRunning /= 24;

            return days+"d, "+hours+"h "+minutes+"m "+seconds+"s";
        }
        else if( name.equals("loginstatus") )
        {
            WikiSession session = actionBean.getWikiSession();
            return session.getStatus();
        }
        else if( name.equals("username") )
        {
            Principal wup = actionBean.getCurrentUser();

            return wup != null ? wup.getName() : "not logged in";
        }
        else if( name.equals("requestcontext") )
        {
            return actionBean.getRequestContext();
        }
        else if( name.equals("pagefilters") )
        {
            List filters = actionBean.getEngine().getFilterManager().getFilterList();
            StringBuffer sb = new StringBuffer();

            for( Iterator i = filters.iterator(); i.hasNext(); )
            {
                PageFilter pf = (PageFilter)i.next();
                String f = pf.getClass().getName();

                if( pf instanceof InternalModule )
                    continue;

                if( sb.length() > 0 ) sb.append(", ");
                sb.append( f );
            }

            return sb.toString();
        }
        else
        {
            // 
            // Check if such a context variable exists,
            // returning its string representation.
            //
            if( (actionBean.getVariable( varName )) != null )
            {
                return actionBean.getVariable( varName ).toString();
            }

            //
            //  Well, I guess it wasn't a final straw.  We also allow 
            //  variables from the session and the request (in this order).
            //

            HttpServletRequest req = actionBean.getContext().getRequest();
            if( req != null && req.getSession() != null )
            {
                HttpSession session = req.getSession();

                try
                {
                    String s;
                    
                    if( (s = (String)session.getAttribute( varName )) != null )
                        return s;

                    if( (s = actionBean.getContext().getRequest().getParameter( varName )) != null )
                        return s;
                }
                catch( ClassCastException e ) {}
            }

            // And the final straw: see if the current page has named metadata.
            
            if ( actionBean instanceof WikiContext )
            {
                WikiPage pg = ((WikiContext)actionBean).getPage();
                if( pg != null )
                {
                    Object metadata = pg.getAttribute( varName );
                    if( metadata != null )
                        return( metadata.toString() );
                }
            
	            // And the final straw part 2: see if the "real" current page has
    	        // named metadata. This allows a parent page to control a inserted
        	    // page through defining variables
         	   WikiPage rpg = ((WikiContext)actionBean).getRealPage();
   				if( rpg != null )
            	{
                	Object metadata = rpg.getAttribute( varName );
                	if( metadata != null )
                    	return metadata.toString();
            	}
            }
            
            // Next-to-final straw: attempt to fetch using property name
            // We don't allow fetching any other properties than those starting
            // with "jspwiki.".  I know my own code, but I can't vouch for bugs
            // in other people's code... :-)
            
            if( varName.startsWith("jspwiki.") )
            {
                Properties props = actionBean.getEngine().getWikiProperties();

                String s = props.getProperty( varName );
                if( s != null )
                {
                    return s;
                }
            }
            
            //
            //  Final defaults for some known quantities.
            //

            if( varName.equals( VAR_ERROR ) || varName.equals( VAR_MSG ) )
                return "";
  
            throw new NoSuchVariableException( "No variable "+varName+" defined." );
        }
    }
}
