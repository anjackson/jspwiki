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
package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.PluginContent;
import com.ecyrd.jspwiki.parser.WikiDocument;

/**
 *  Implements DOM-to-Creole rendering.
 *  <p>
 *  FIXME: This class is not yet completely done.
 *  
 *  @author Janne Jalkanen
 */
public class CreoleRenderer extends WikiRenderer
{
    private static final String IMG_START = "{{";
    private static final String IMG_END = "}}";
    private static final String PLUGIN_START = "<<";
    private static final String PLUGIN_END = ">>";
    private static final String HREF_START = "[[";
    private static final String HREF_DELIMITER = "|";
    private static final String HREF_END = "]]";
    private static final String PRE_START = "{{{";
    private static final String PRE_END = "}}}";
    private static final String PLUGIN_IMAGE = "Image";
    private static final String PARAM_SRC = "src";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String ONE_SPACE = " ";
    private static final String EMPTY_STRING = "";
    private static final String LINEBREAK = "\n";
    private static final String LI = "li";
    private static final String UL = "ul";
    private static final String OL = "ol";
    private static final String P  = "p";
    private static final String A  = "a";
    private static final String PRE = "pre";
    
    /**
     * Contains element, start markup, end markup
     */
    private static final String[] ELEMENTS = {
       "i" , "//"    , "//",
       "b" , "**"    , "**",
       "h2", "== "   , " ==",
       "h3", "=== "  , " ===",
       "h4", "==== " , " ====",
       "hr", "----"  , EMPTY_STRING,
       "tt", "<<{{>>", "<<}}>>"
    };
    
    private int m_listCount = 0;
    private char m_listChar = 'x';

    private List m_plugins = new ArrayList();

    public CreoleRenderer( WikiContext ctx, WikiDocument doc )
    {
        super( ctx, doc );
    }
    
    /**
     * Renders an element into the StringBuffer given
     * @param ce
     * @param sb
     */
    private void renderElement( Element ce, StringBuffer sb )
    {
        String endEl = EMPTY_STRING;
        for( int i = 0; i < ELEMENTS.length; i+=3 )
        {
            if( ELEMENTS[i].equals(ce.getName()) )
            {
                sb.append( ELEMENTS[i+1] );
                endEl = ELEMENTS[i+2];
            }
        }
        
        if( UL.equals(ce.getName()) )
        {
            m_listCount++;
            m_listChar = '*';
        }
        else if( OL.equals(ce.getName()) )
        {
            m_listCount++;
            m_listChar = '#';
        }
        else if( LI.equals(ce.getName()) )
        {
            for(int i = 0; i < m_listCount; i++ ) sb.append( m_listChar );
            sb.append( ONE_SPACE );
        }
        else if( A.equals(ce.getName()) )
        {
            String href = ce.getAttributeValue( HREF_ATTRIBUTE );
            String text = ce.getText();
            
            if( href.equals(text) )
            {
                sb.append( HREF_START + href + HREF_END );
            }
            else
            {
                sb.append( HREF_START + href+ HREF_DELIMITER + text +HREF_END);
            }
            // Do not render anything else 
            return;
        }
        else if( PRE.equals(ce.getName()) )
        {
            sb.append( PRE_START );
            sb.append( ce.getText() );
            sb.append( PRE_END );
            
            return;
        }
        
        //
        //  Go through the children
        //
        for( Iterator i = ce.getContent().iterator(); i.hasNext(); )
        {
            Content c = (Content)i.next();
            
            if( c instanceof PluginContent )
            {
                PluginContent pc = (PluginContent)c;
                
                if( pc.getPluginName().equals( PLUGIN_IMAGE ) )
                {
                    sb.append( IMG_START + pc.getParameter( PARAM_SRC ) + IMG_END );
                }
                else
                {
                    m_plugins.add(pc);
                    sb.append( PLUGIN_START + pc.getPluginName() + ONE_SPACE + m_plugins.size() + PLUGIN_END );
                }
            }
            else if( c instanceof Text )
            {
                sb.append( ((Text)c).getText() );
            }
            else if( c instanceof Element )
            {
                renderElement( (Element)c, sb );
            }
        }

        if( UL.equals( ce.getName() ) || OL.equals( ce.getName() ) )
        {
            m_listCount--;
        }
        else if( P.equals( ce.getName() ) )
        {
            sb.append( LINEBREAK );
        }
        
        sb.append(endEl);
    }
    
    public String getString() throws IOException
    {
        StringBuffer sb = new StringBuffer(1000);
        
        Element ce = m_document.getRootElement();
        
        //
        //  Traverse through the entire tree of everything.
        //
        
        renderElement( ce, sb );
        
        return sb.toString();
    }

}
