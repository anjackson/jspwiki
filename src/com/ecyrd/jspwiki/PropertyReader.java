/*
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2007 Janne Jalkanen (Janne.Jalkanen@iki.fi)

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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.servlet.ServletContext;

/**
 * Property Reader for the WikiEngine. Reads the properties for the WikiEngine
 * and implements the feature of cascading properties and variable substitution,
 * which come in handy in a multi wiki installation environment: It reduces the
 * need for (shell) scripting in order to generate different jspwiki.properties
 * to a minimum.
 *
 * @author Christoph Sauer
 * @since 2.5.x
 */
public final class PropertyReader
{

    /** The web.xml parameter that defines where the config file is to be found.
     *  If it is not defined, uses the default as defined by DEFAULT_PROPERTYFILE.
     *  {@value #DEFAULT_PROPERTYFILE}
     */
    public static final String PARAM_PROPERTYFILE = "jspwiki.propertyfile";

    public static final String PARAM_PROPERTYFILE_CASCADEPREFIX = "jspwiki.propertyfile.cascade.";

    /** Path to the default property file.
     * {@value #DEFAULT_PROPERTYFILE}
     */
    public static final String  DEFAULT_PROPERTYFILE = "/WEB-INF/jspwiki.properties";

    public static final String PARAM_VAR_DECLARATION = "var.";
    public static final String PARAM_VAR_IDENTIFIER = "$";


    /**
     *  Contains the default properties for JSPWiki.
     */
    private static final String[] DEFAULT_PROPERTIES =
    { "jspwiki.specialPage.Login",           "Login.jsp",
      "jspwiki.specialPage.Logout",          "Logout.jsp",
      "jspwiki.specialPage.CreateGroup",     "NewGroup.jsp",
      "jspwiki.specialPage.CreateProfile",   "Register.jsp",
      "jspwiki.specialPage.EditProfile",     "UserPreferences.jsp",
      "jspwiki.specialPage.Preferences",     "UserPreferences.jsp",
      "jspwiki.specialPage.Search",          "Search.jsp",
      "jspwiki.specialPage.FindPage",        "FindPage.jsp"};


    /**
     *  Private constructor to prevent instantiation.
     */
    private PropertyReader()
    {}

    /**
     *  Loads the webapp properties based on servlet context information.
     *  Returns a Properties object containing the settings, or null if unable
     *  to load it. (The default file is WEB-INF/jspwiki.properties, and can
     *  be overridden by setting PARAM_PROPERTYFILE in the server or webapp
     *  configuration.)
     *
     *  <h3>Cascading Properties</h3>
     *  <p>
     *  You can define additional property files and merge them into the default
     *  properties file in a similar process to how you define cascading style
     *  sheets; hence we call this <i>cascading property files</i>. This way you
     *  can overwrite the default values and only specify the properties you
     *  need to change in a multiple wiki environment.
     *  <p>
     *  You define a cascade in the context mapping of your servlet container.
     *  <pre>
     *  jspwiki.properties.cascade.1
     *  jspwiki.properties.cascade.2
     *  jspwiki.properties.cascade.3
     *  </pre>
     *  and so on. You have to number your cascade in a descending way starting
     *  with "1". This means you cannot leave out numbers in your cascade. This
     *  method is based on an idea by Olaf Kaus, see [JSPWiki:MultipleWikis].
     */
    public static Properties loadWebAppProps( ServletContext context )
    {
        String      propertyFile   = context.getInitParameter(PARAM_PROPERTYFILE);
        InputStream propertyStream = null;

        try
        {
            //
            //  Figure out where our properties lie.
            //
            if( propertyFile == null )
            {
                context.log("No "+PARAM_PROPERTYFILE
                        +" defined for this context, using default from "+DEFAULT_PROPERTYFILE);
                //  Use the default property file.
                propertyStream = context.getResourceAsStream(DEFAULT_PROPERTYFILE);
            }
            else
            {
                context.log("Reading properties from "+propertyFile+" instead of default.");
                propertyStream = new FileInputStream( new File(propertyFile) );
            }

            if( propertyStream == null )
            {
                throw new WikiException("Property file cannot be found!"+propertyFile);
            }

            Properties props = getDefaultProperties();
            props.load( propertyStream );

            //this will add additional properties to the default ones:
            context.log("Loading cascading properties...");

            //now load the cascade (new in 2.5)
            loadWebAppPropsCascade(context, props);

            //finally expand the variables (new in 2.5)
            expandVars(props);

            return props;
        }
        catch( Exception e )
        {
            context.log( Release.APPNAME
                    +": Unable to load and setup properties from jspwiki.properties. "+e.getMessage() );
        }
        finally
        {
            try
            {
                if( propertyStream != null ) propertyStream.close();
            }
            catch( IOException e )
            {
                context.log("Unable to close property stream - something must be seriously wrong.");
            }
        }

        return null;
    }


    /**
     *  Returns the default property set as a Properties object.
     */
    public static final Properties getDefaultProperties()
    {
        return new Properties( TextUtil.createProperties( DEFAULT_PROPERTIES ) );
    }


    /**
     *  Returns the ServletContext Init parameter if has been set, otherwise
     *  checks for a System property of the same name. If neither are defined,
     *  returns null. This permits both Servlet- and System-defined cascading
     *  properties.
     */
    private static String getInitParameter( ServletContext context, String name )
    {
        String value = context.getInitParameter(name);
        return ( value != null )
                ? value
                : System.getProperty(name) ;
    }


    /**
     *  Implement the cascade functionality.
     *
     * @param context             where to read the cascade from
     * @param defaultProperties   properties to merge the cascading properties to
     * @author Christoph Sauer
     * @since 2.5.x
     */
    private static void loadWebAppPropsCascade(ServletContext context, Properties defaultProperties)
    {
        if( getInitParameter(context,PARAM_PROPERTYFILE_CASCADEPREFIX + "1") == null )
        {
            context.log(" No cascading properties defined for this context");
            return;
        }

        // get into cascade...
        int depth = 0;
        boolean more = true;
        InputStream propertyStream = null;
        while (more)
        {
            depth++;
            String propertyFile = getInitParameter(context,PARAM_PROPERTYFILE_CASCADEPREFIX + depth);

            if (propertyFile == null)
            {
                more = false;
                break;
            }

            try
            {
                context.log(" Reading additional properties from " + propertyFile + " and merge to cascade.");
                Properties additionalProps = new Properties();
                propertyStream = new FileInputStream(new File(propertyFile));
                additionalProps.load(propertyStream);
                defaultProperties.putAll(additionalProps);
            }
            catch (Exception e)
            {
                context.log(" " + Release.APPNAME
                        + ": Unable to load and setup properties from " + propertyFile + "."
                        + e.getMessage());
            }
            finally
            {
                try
                {
                    if( propertyStream != null )
                    {
                        propertyStream.close();
                    }
                }
                catch (IOException e)
                {
                    context.log(" Unable to close property stream - something must be seriously wrong.");
                }
            }
        }

        return;
    }

    /**
     *  You define a property variable by using the prefix "var.x" as a
     *  property. In property values you can then use the "$x" identifier
     *  to use this variable.
     *
     *  For example you could declare a base directory for all your files
     *  like this and use it in all your other property definitions with
     *  a "$basedir". Note that it does not matter if you define the
     *  variable before its usage.
     *  <pre>
     *  var.basedir = /p/mywiki;
     *  jspwiki.fileSystemProvider.pageDir =         $basedir/www/
     *  jspwiki.basicAttachmentProvider.storageDir = $basedir/www/
     *  jspwiki.workDir =                            $basedir/wrk/
     *  </pre>
     *
     * @param properties - properties to expand;
     */
    public static void expandVars(Properties properties)
    {
        //get variable name/values from properties...
        Map vars = new HashMap();
        Enumeration propertyList = properties.propertyNames();
        while( propertyList.hasMoreElements() )
        {
            String propertyName = (String)propertyList.nextElement();
            String propertyValue = properties.getProperty(propertyName);

            if ( propertyName.startsWith(PARAM_VAR_DECLARATION) )
            {
                String varName = propertyName.substring(4, propertyName.length()).trim();
                String varValue = propertyValue.trim();
                vars.put(varName, varValue);
            }
        }

        //now, substitute $ values in property values with vars...
        propertyList = properties.propertyNames();
        while( propertyList.hasMoreElements() )
        {
            String propertyName = (String)propertyList.nextElement();
            String propertyValue = properties.getProperty(propertyName);

            //skip var properties itself...
            if ( propertyName.startsWith(PARAM_VAR_DECLARATION) )
            {
                continue;
            }

            Iterator iter = vars.entrySet().iterator();
            while ( iter.hasNext() )
            {
                Map.Entry entry = (Map.Entry)iter.next();
                String varName = (String)entry.getKey();
                String varValue = (String)entry.getValue();

                //replace old property value, using the same variabe. If we don't overwrite
                //the same one the next loop works with the original one again and
                //multiple var expansion won't work...
                propertyValue =
                    TextUtil.replaceString( propertyValue, PARAM_VAR_IDENTIFIER + varName, varValue );

                //add the new PropertyValue to the properties
                properties.put(propertyName, propertyValue);

            }
        }
    }

}