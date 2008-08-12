/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package com.ecyrd.jspwiki.ui;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.apache.log4j.NDC;

import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.action.WikiActionBeanFactory;
import com.ecyrd.jspwiki.event.*;
import com.ecyrd.jspwiki.util.WatchDog;

/**
 * This filter goes through the generated page response prior and
 * places requested resources at the appropriate inclusion markers.
 * This is done to let dynamic content (e.g. plugins, editors) 
 * include custom resources, even after the HTML head section is
 * in fact built. This filter is typically the last filter to execute,
 * and it <em>must</em> run after servlet or JSP code that performs
 * redirections or sends error codes (such as access control methods).
 * <p>
 * Inclusion markers are placed by the IncludeResourcesTag; the
 * defult content templates (see .../templates/default/commonheader.jsp)
 * are configured to do this. As an example, a JavaScript resource marker
 * is added like this:
 * <pre>
 * &lt;wiki:IncludeResources type="script"/&gt;
 * </pre>
 * Any code that requires special resources must register a resource
 * request with the TemplateManager. For example:
 * <pre>
 * &lt;wiki:RequestResource type="script" path="scripts/custom.js" /&gt;
 * </pre>
 * or programmatically,
 * <pre>
 * TemplateManager.addResourceRequest( context, TemplateManager.RESOURCE_SCRIPT, "scripts/customresource.js" );
 * </pre>
 * 
 * @see TemplateManager
 * @see com.ecyrd.jspwiki.tags.RequestResourceTag
 */
public class WikiJSPFilter extends WikiServletFilter
{
    /** {@inheritDoc} */
    public void doFilter( ServletRequest  request,
                          ServletResponse response,
                          FilterChain     chain )
        throws ServletException, IOException
    {
        WatchDog w = m_engine.getCurrentWatchDog();
        try
        {
            NDC.push( m_engine.getApplicationName()+":"+((HttpServletRequest)request).getRequestURI() );

            w.enterState("Filtering for URL "+((HttpServletRequest)request).getRequestURI(), 90 );
          
            HttpServletResponseWrapper responseWrapper = new MyServletResponseWrapper( (HttpServletResponse)response );
        
            // fire PAGE_REQUESTED event
            WikiActionBean wikiContext = WikiActionBeanFactory.findActionBean( request );
            boolean isWikiContext = ( wikiContext instanceof WikiContext );
            if ( isWikiContext )
            {
                String pageName = ((WikiContext)wikiContext).getPage().getName();
                fireEvent( WikiPageEvent.PAGE_REQUESTED, pageName );
            }

            super.doFilter( request, responseWrapper, chain );

            // The response is now complete. Lets replace the markers now.
        
            // WikiContext is only available after doFilter! (That is after
            //   interpreting the jsp)

            try
            {
                w.enterState( "Delivering response", 30 );
                String r = filter( wikiContext, responseWrapper );
        
                //String encoding = "UTF-8";
                //if( wikiContext != null ) encoding = wikiContext.getEngine().getContentEncoding();
        
                // Only now write the (real) response to the client.
                // response.setContentLength(r.length());
                // response.setContentType(encoding);
                
                response.getWriter().write(r);
            
                // Clean up the UI messages and loggers
                if( wikiContext != null )
                {
                    // fire PAGE_DELIVERED event
                    wikiContext.getWikiSession().clearMessages();
                    if ( isWikiContext )
                    {
                        String pageName = ((WikiContext)wikiContext).getPage().getName();
                        fireEvent( WikiPageEvent.PAGE_DELIVERED, pageName );
                    }
                }
            }
            finally
            {
                w.exitState();
            }
        }
        finally
        {
            w.exitState();
            NDC.pop();
            NDC.remove();
        }
    }

    /**
     * Goes through all types and writes the appropriate response.
     * 
     * @param wikiContext The usual processing context
     * @param string The source string
     * @return The modified string with all the insertions in place.
     */
    private String filter(WikiActionBean wikiContext, HttpServletResponse response )
    {
        String string = response.toString();

        if( wikiContext != null )
        {
            String[] resourceTypes = TemplateManager.getResourceTypes( wikiContext );

            for( int i = 0; i < resourceTypes.length; i++ )
            {
                string = insertResources( wikiContext, string, resourceTypes[i] );
            }
        
            //
            //  Add HTTP header Resource Requests
            //
            String[] headers = TemplateManager.getResourceRequests( wikiContext,
                                                                    TemplateManager.RESOURCE_HTTPHEADER );
        
            for( int i = 0; i < headers.length; i++ )
            {
                String key = headers[i];
                String value = "";
                int split = headers[i].indexOf(':');
                if( split > 0 && split < headers[i].length()-1 )
                {
                    key = headers[i].substring( 0, split );
                    value = headers[i].substring( split+1 );
                }
            
                response.addHeader( key.trim(), value.trim() );
            }
        }

        return string;
    }

    /**
     *  Inserts whatever resources
     *  were requested by any plugins or other components for this particular
     *  type.
     *  
     *  @param wikiContext The usual processing context
     *  @param string The source string
     *  @param type Type identifier for insertion
     *  @return The filtered string.
     */
    private String insertResources(WikiActionBean wikiContext, String string, String type )
    {
        if( wikiContext == null )
        {
            return string;
        }

        String marker = TemplateManager.getMarker( wikiContext, type );
        int idx = string.indexOf( marker );
        
        if( idx == -1 )
        {
            return string;
        }
        
        log.debug("...Inserting...");
        
        String[] resources = TemplateManager.getResourceRequests( wikiContext, type );
        
        StringBuffer concat = new StringBuffer( resources.length * 40 );
        
        for( int i = 0; i < resources.length; i++  )
        {
            log.debug("...:::"+resources[i]);
            concat.append( resources[i] );
        }

        string = TextUtil.replaceString( string, 
                                         idx, 
                                         idx+marker.length(), 
                                         concat.toString() );
        
        return string;
    }
    
    /**
     *  Simple response wrapper that just allows us to gobble through the entire
     *  response before it's output.
     */
    private static class MyServletResponseWrapper
        extends HttpServletResponseWrapper
    {
        private CharArrayWriter m_output;
      
        /** 
         *  How large the initial buffer should be.  This should be tuned to achieve
         *  a balance in speed and memory consumption.
         */
        private static final int INIT_BUFFER_SIZE = 4096;
        
        public MyServletResponseWrapper( HttpServletResponse r )
        {
            super(r);
            m_output = new CharArrayWriter( INIT_BUFFER_SIZE );
        }

        /**
         *  Returns a writer for output; this wraps the internal buffer
         *  into a PrintWriter.
         */
        public PrintWriter getWriter()
        {
            return new PrintWriter( m_output );
        }

        public ServletOutputStream getOutputStream()
        {
            return new MyServletOutputStream( m_output );
        }

        static class MyServletOutputStream extends ServletOutputStream
        {
            CharArrayWriter m_buffer;

            public MyServletOutputStream(CharArrayWriter aCharArrayWriter)
            {
                super();
                m_buffer = aCharArrayWriter;
            }

            public void write(int aInt)
            {
                m_buffer.write( aInt );
            }

        }
        
        /**
         *  Returns whatever was written so far into the Writer.
         */
        public String toString()
        {
            return m_output.toString();
        }
    }


    // events processing .......................................................


    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners of the current WikiEngine.
     *
     * @see com.ecyrd.jspwiki.event.WikiPageEvent
     * @param type       the event type to be fired
     * @param pagename   the wiki page name as a String
     */
    protected final void fireEvent( int type, String pagename )
    {
        if ( WikiEventManager.isListening(m_engine) )
        {
            WikiEventManager.fireEvent(m_engine,new WikiPageEvent(m_engine,type,pagename));
        }
    }

}
