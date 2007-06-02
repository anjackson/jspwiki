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
package com.ecyrd.jspwiki.render;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Iterator;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.parser.WikiDocument;

import com.ecyrd.jspwiki.htmltowiki.XHtmlToWikiConfig;

/**
 *  Implements a WikiRendered that outputs XHTML in a format that is suitable
 *  for use by a WYSIWYG XHTML editor.
 *   
 *  @author David Au
 *  @since  2.5
 */
public class WysiwygEditingRenderer
    extends WikiRenderer
{
    
    private static final String A_ELEMENT = "a";
    private static final String PRE_ELEMENT = "pre";
    private static final String CLASS_ATTRIBUTE = "class";
    private static final String HREF_ATTRIBUTE = "href";
    private static final String TITLE_ATTRIBUTE = "title";
    private static final String EDITPAGE = "editpage";
    private static final String WIKIPAGE = "wikipage";
    private static final String LINEBREAK = "\n";
    private static final String LINKS_TRANSLATION = "$1#$2";
    private static final String LINKS_SOURCE = "(.+)#section-.+-(.+)";

    public WysiwygEditingRenderer( WikiContext context, WikiDocument doc )
    {
        super( context, doc );
    }
    
    /*
     * Recursively walk the XHTML DOM tree and manipulate specific elements to
     * make them better for WYSIWYG editing.
     */
    private void processChildren(Element baseElement)
    {
        for( Iterator itr = baseElement.getChildren().iterator(); itr.hasNext(); )
        {
            Object childElement = itr.next();
            if( childElement instanceof Element )
            {
                Element element = (Element)childElement;
                String elementName = element.getName().toLowerCase();
                Attribute classAttr = element.getAttribute( CLASS_ATTRIBUTE );

                if( elementName.equals( A_ELEMENT ) )
                {
                    if( classAttr != null )
                    {
                        String classValue = classAttr.getValue();
                        Attribute hrefAttr = element.getAttribute( HREF_ATTRIBUTE );
                        
                        XHtmlToWikiConfig wikiConfig = new XHtmlToWikiConfig( m_context );
                        
                        // Get the url for wiki page link - it's typically "Wiki.jsp?page=MyPage"
                        // or when using the ShortURLConstructor option, it's "wiki/MyPage" .
                        String wikiPageLinkUrl = wikiConfig.getWikiJspPage();
                        String editPageLinkUrl = wikiConfig.getEditJspPage();
                        
                        if( classValue.equals( WIKIPAGE )
                            || ( hrefAttr != null && hrefAttr.getValue().startsWith( wikiPageLinkUrl ) ) )
                        {
                            // Remove the leading url string so that users will only see the
                            // wikipage's name when editing an existing wiki link.
                            // For example, change "Wiki.jsp?page=MyPage" to just "MyPage".
                            String newHref = hrefAttr.getValue().substring( wikiPageLinkUrl.length() );

                            // Convert "This%20Pagename%20Has%20Spaces" to "This Pagename Has Spaces"
                            newHref = m_context.getEngine().decodeName( newHref );
                                
                            // Handle links with section anchors.
                            // For example, we need to translate the html string "TargetPage#section-TargetPage-Heading2"
                            // to this wiki string: "TargetPage#Heading2".
                            hrefAttr.setValue( newHref.replaceFirst( LINKS_SOURCE, LINKS_TRANSLATION ) );
                        }                        
                        else if ( classValue.equals( EDITPAGE ) 
                            || ( hrefAttr != null && hrefAttr.getValue().startsWith( editPageLinkUrl ) ) )
                        {
                            Attribute titleAttr = element.getAttribute( TITLE_ATTRIBUTE );
                            if( titleAttr != null )
                            {
                                // remove the title since we don't want to eventually save the default undefined page title.
                                titleAttr.detach();
                            }
                            
                            String newHref = hrefAttr.getValue().substring( editPageLinkUrl.length() );
                            newHref = m_context.getEngine().decodeName( newHref );
                            
                            hrefAttr.setValue( newHref );
                        }
                    }
                } // end of check for "a" element
                else if( elementName.equals( PRE_ELEMENT ) )
                {
                    // We need to trim the surrounding whitespace to accomodate a FCK bug: when the first line 
                    // of a <pre> tag contains only whitespace, then all the linebreaks in the <pre>
                    // tag will be lost due to FCK's html tidying.
                    String text = element.getTextTrim();
                    element.setText( text );
                }

                processChildren( element );
            }
        }
    }
    
    public String getString()
        throws IOException
    {
        Element rootElement = m_document.getRootElement();
        processChildren( rootElement );
        
        m_document.setContext( m_context );

        XMLOutputter output = new XMLOutputter();
        
        StringWriter out = new StringWriter();
        
        Format fmt = Format.getRawFormat();
        fmt.setExpandEmptyElements( false );
        fmt.setLineSeparator( LINEBREAK );

        output.setFormat( fmt );
        output.outputElementContent( m_document.getRootElement(), out );
        
        return out.toString();
    }
}
