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
package com.ecyrd.jspwiki.plugin;

import org.apache.oro.text.*;
import org.apache.oro.text.regex.*;
import org.apache.log4j.Category;
//import java.util.StringTokenizer;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Map;
import java.util.HashMap;
import com.ecyrd.jspwiki.WikiContext;

/**
 *  Manages plugin classes.
 *
 *  @author Janne Jalkanen
 *  @since 1.6.1
 */
public class PluginManager
{
    private static Category log = Category.getInstance( PluginManager.class );

    /**
     *  This is the default package to try in case the instantiation
     *  fails.
     */
    public static final String DEFAULT_PACKAGE = "com.ecyrd.jspwiki.plugin";

    public PluginManager()
    {
    }

    /**
     *  Returns true if the link is really command to insert
     *  a plugin.
     */
    public static boolean isPluginLink( String link )
    {
        return link.startsWith("{INSERT");
    }

    /**
     *  Executes a plugin class in the given context.
     */
    private String execute( WikiContext context,
                            String classname,
                            Map params )
        throws PluginException
    {
        try
        {
            ClassLoader loader = getClass().getClassLoader();
            WikiPlugin plugin;

            //
            //  Attempt to instantiate new plugin, trying first with explicit
            //  class name, and then with the JSPWiki default plugin package.
            //
            try
            {
                plugin = (WikiPlugin)loader.loadClass( classname ).newInstance();
            }
            catch( ClassNotFoundException e )
            {
                log.debug("Did not find class "+classname+", trying with default.");
                plugin = (WikiPlugin)loader.loadClass( DEFAULT_PACKAGE+"."+classname ).newInstance();
            }

            return plugin.execute( context, params );
        }
        catch( InstantiationException e )
        {
            throw new PluginException( "Cannot instantiate plugin "+classname, e );
        }
        catch( ClassNotFoundException e )
        {
            throw new PluginException( "Could not find plugin "+classname, e );
        }
        catch( IllegalAccessException e )
        {
            throw new PluginException( "Not allowed to access plugin "+classname, e );
        }
        catch( ClassCastException e )
        {
            throw new PluginException( "Class "+classname+" is not a Wiki plugin.", e );
        }
    }
    
    /**
     *  Parses a plugin.  Plugin commands are of the form:
     *  [{INSERT myplugin WHERE param1=value1, param2=value2}]
     *  myplugin may either be a class name or a plugin alias.
     */
    public String execute( WikiContext context,
                           String commandline )
        throws PluginException
    {
        PatternMatcher  matcher  = new Perl5Matcher();
        PatternCompiler compiler = new Perl5Compiler();

        try
        {
            Pattern ptrn = compiler.compile( "\\{?INSERT\\s*([\\w\\._]+)\\s*(WHERE)?\\s*([^\\}]*)\\}?$" );

            if( matcher.contains( commandline, ptrn ) )
            {
                MatchResult res = matcher.getMatch();

                String plugin   = res.group(1);                
                String args     = res.group(3);
                HashMap arglist = new HashMap();
                
                //
                //  Go through the whole thing and handle quotes, etc.
                //

                StreamTokenizer tok = new StreamTokenizer(new StringReader(args));
                int type;

                String param = null, value = null;

                while( (type = tok.nextToken() ) != StreamTokenizer.TT_EOF )
                {
                    String s;

                    switch( type )
                    {
                      case StreamTokenizer.TT_WORD:
                        s = tok.sval;
                        break;
                      case StreamTokenizer.TT_NUMBER:
                        s = Integer.toString( new Double(tok.nval).intValue() );
                        break;
                      case '"':
                        s = tok.sval;
                        break;
                      default:
                        s = null;
                    }

                    //
                    //  Assume that alternate words on the line are
                    //  parameter and value, respectively.
                    //
                    if( s != null )
                    {
                        if( param == null ) 
                        {
                            param = s;
                        }
                        else
                        {
                            value = s;
                            
                            arglist.put( param, value );
                            param = null;
                        }
                    }
                }

                return execute( context, plugin, arglist );
            }
        }
        catch( MalformedPatternException e )
        {
            log.fatal("Internal error: someone messed with pluginmanager patterns.", e );
            throw new PluginException( "PluginManager patterns are broken", e );
        }
        catch( NoSuchElementException e )
        {
            String msg =  "Missing parameter in plugin definition: "+commandline;
            log.warn( msg, e );
            throw new PluginException( msg );
        }
        catch( IOException e )
        {
            String msg = "Zyrf.  Problems with parsing arguments: "+commandline;
            log.warn( msg, e );
            throw new PluginException( msg );
        }

        // FIXME: We could either return an empty string "", or
        // the original line.  If we want unsuccessful requests
        // to be invisible, then we should return an empty string.
        return commandline;
    }
}
