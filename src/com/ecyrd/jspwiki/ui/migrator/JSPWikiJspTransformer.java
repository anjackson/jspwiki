package com.ecyrd.jspwiki.ui.migrator;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.stripes.action.ActionBean;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.action.WikiContextFactory;
import com.ecyrd.jspwiki.ui.stripes.HandlerInfo;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 * Known limitations: will not modify Java code inside of tag attributes.
 */
public class JSPWikiJspTransformer extends AbstractJspTransformer
{
    private static final Pattern CONTEXT_PATTERN = Pattern.compile( "\\.createContext\\(.*?WikiContext.([A-Z]*?)\\s*\\);" );

    private static final Pattern HASACCESS_PATTERN = Pattern
        .compile( "if\\s*\\(\\s*\\!(wikiContext|context|ctx)\\.hasAccess\\(.*?\\)\\s*\\)\\s*return;" );

    private static final Pattern PAGE_GETNAME_PATTERN = Pattern.compile( "(wikiContext|context|ctx)\\.getName\\(\\)" );

    private static final Pattern FINDCONTEXT_PATTERN = Pattern.compile( "WikiContext\\.findContext\\((.*?)\\)" );

    private Map<String, HandlerInfo> m_contextMap = new HashMap<String, HandlerInfo>();

    /**
     * {@inheritDoc}
     */
    public void initialize( JspMigrator migrator, Set<Class<? extends ActionBean>> beanClasses, Map<String, Object> sharedState )
    {
        m_contextMap = cacheRequestContexts( beanClasses );
        System.out.println( "Initialized JSPWikiJspTransformer." );
    }

    /**
     * {@inheritDoc}
     */
    public void transform( Map<String, Object> sharedState, JspDocument doc )
    {
        List<Node> nodes = doc.getNodes();

        for( Node node : nodes )
        {
            // For all HTML tags...
            if( node.isHtmlNode() )
            {
                Tag tag = (Tag) node;

                // Check any form or stripes:form elements
                if( "form".equals( tag.getName() ) || "stripes:form".equals( tag.getName() ) )
                {
                    processFormTag( tag );
                }
                else if( "fmt:setBundle".equals( tag.getName() ) )
                {
                    removeSetBundle( tag );
                }

                // Advise user about <input type="hidden"> or <stripes:hidden>
                // tags
                boolean isTypeHidden = false;
                if( tag.getType() != NodeType.END_TAG )
                {
                    isTypeHidden = "stripes:hidden".equals( tag.getName() );
                    if( "input".equals( tag.getName() ) )
                    {
                        Attribute attribute = tag.getAttribute( "type" );
                        isTypeHidden = "hidden".equals( attribute.getValue() );
                    }
                    if( isTypeHidden )
                    {
                        String paramName = tag.hasAttribute( "name" ) ? tag.getAttribute( "name" ).getValue() : null;
                        String paramValue = tag.hasAttribute( "value" ) ? tag.getAttribute( "value" ).getValue() : null;
                        if( paramName != null && paramValue != null )
                        {
                            message( tag, "NOTE: hidden form input sets parameter " + paramName + "=\"" + paramValue
                                          + "\". This should probably correspond to a Stripes ActionBean getter/settter. Refactor?" );
                        }
                    }
                }

                // Tell user about <wiki:Messages> tags.
                if( "wiki:Messages".equals( tag.getName() ) )
                {
                    message( tag,
                             "Consider using <stripes:errors> tags instead of <wiki:Messages> for displaying validation errors." );
                }
            }

            // Look for WikiEngine.createContext() statements, and add matching
            // <stripes:useActionBean> tag
            else if( node.getType() == NodeType.JSP_DECLARATION || 
                         node.getType() == NodeType.SCRIPTLET || 
                         node.getType() == NodeType.JSP_EXPRESSION ||
                         node.getType() == NodeType.CDATA )
            {
                String scriptlet = node.getValue();
                Matcher m = CONTEXT_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String context = m.group( 1 ).trim(); // EDIT, COMMENT
                                                            // etc.
                    HandlerInfo handler = m_contextMap.get( context );
                    if( handler != null )
                    {
                        // Add the <stripes:useActionBean> tag
                        addUseActionBeanTag( doc, handler.getActionBeanClass(), handler.getEventName() );

                        // Now add the Stripes taglib declaration
                        if( StripesJspTransformer.addStripesTaglib( doc ) )
                        {
                            message( doc.getRoot(), "Added Stripes taglib directive." );
                        }
                    }
                }

                // Remove any WikiContext.hasAccess() statements
                m = HASACCESS_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String hasAccess = m.group( 0 );
                    scriptlet = scriptlet.replace( hasAccess, "" );
                    node.setValue( scriptlet );
                    message( node, "Removed WikiContext.hasAccess() statement." );
                }

                // Change WikiContext.getName() to
                // WikiContext.getPage().getName();
                m = PAGE_GETNAME_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String getName = m.group( 0 );
                    String ctx = m.group( 1 ).trim();
                    scriptlet = scriptlet.replace( getName, ctx + ".getPage().getName()" );
                    node.setValue( scriptlet );
                    message( node, "Changed WikiContext.getName() statement to WikiContext.getPage().getName()." );
                }

                // Change WikiContext.findContext() to
                // WikiContextFactory.findContext()
                m = FINDCONTEXT_PATTERN.matcher( scriptlet );
                if( m.find() )
                {
                    String findContext = m.group( 0 );
                    String ctx = m.group( 1 ).trim();
                    scriptlet = scriptlet.replace( findContext, "WikiContextFactory.findContext( " + ctx + " )" );
                    node.setValue( scriptlet );
                    message( node, "Changed WikiContext.findContext() statement to WikiContextFactory.findContext()." );

                    // Make sure we have a page import statement!
                    List<Tag> imports = doc.getPageImport( WikiContextFactory.class.getName() );
                    if( imports.size() == 0 )
                    {
                        doc.addPageImportDirective( WikiContextFactory.class.getName() );
                        message( node, "Added page import for WikiContextFactory." );
                    }
                }
            }
        }
    }

    private void addUseActionBeanTag( JspDocument doc, Class<? extends ActionBean> beanClass, String event )
    {
        // If UseActionBean tag already added, bail
        List<Node> nodes = doc.getNodes();
        for( Node node : nodes )
        {
            if( "stripes:useActionBean".equals( node.getName() ) )
            {
                return;
            }
        }

        // Create Tag
        Tag tag = new Tag( doc, NodeType.EMPTY_ELEMENT_TAG );
        tag.setName( "stripes:useActionBean" );
        tag.addAttribute( new Attribute( doc, "beanclass", beanClass.getName() ) );
        if( event != null )
        {
            tag.addAttribute( new Attribute( doc, "event", event ) );
        }

        // Create linebreak
        Text linebreak = new Text( doc );
        linebreak.setValue( System.getProperty( "line.separator" ) );
        Node root = doc.getRoot();
        linebreak.setParent( root );

        // Figure out where to put it
        List<Node> directives = doc.getNodes( NodeType.JSP_DIRECTIVE );
        if( directives.size() == 0 )
        {
            root.addChild( linebreak, 0 );
            root.addChild( tag, 0 );
        }
        else
        {
            Node lastDirective = directives.get( directives.size() - 1 );
            lastDirective.addSibling( tag );
            lastDirective.addSibling( linebreak );
        }
        message( doc.getRoot(), "Added <stripes:useActionBean beanclass=\"" + beanClass.getName() + "\" event=\"" + event + "\" />" );
    }

    /**
     * Removes the &lt;fmt:setBundle&gt; tag and advises the user.
     * 
     * @param tag the tag to remove
     */
    private void removeSetBundle( Tag tag )
    {
        Node parent = tag.getParent();
        parent.removeChild( tag );
        message( tag, "Removed <fmt:setBundle> tag because it is automatically set in web.xml." );
    }

    /**
     * For &lt;form&gt; and &lt;stripes:form&gt; tags, changes
     * <code>accept-charset</code> or <code>acceptcharset</code> attribute
     * value to "UTF-8", and removes any <code>onsubmit</code> function calls.
     * 
     * @param tag the form tag
     */
    private void processFormTag( Tag tag )
    {
        // Change "accept-charset" or "acceptcharset" values to UTF-8
        Attribute attribute = tag.getAttribute( "accept-charset" );
        if( attribute == null )
        {
            attribute = tag.getAttribute( "acceptcharset" );
        }
        if( attribute != null )
        {
            message( attribute, "Changed value to \"UTF-8\"." );
            attribute.setValue( "UTF-8" );
        }

        // Remove onsubmit() attribute and warn the user
        attribute = tag.getAttribute( "onsubmit" );
        if( attribute != null )
        {
            String value = attribute.getValue();
            message( attribute, "Removed JavaScript call \"" + value + "\". REASON: it probably does not work with Stripes." );
            tag.removeAttribute( attribute );
        }
    }

    /**
     * Using introspection, creates a cached Map of with request context field
     * names as keys, and ActionBean classes as values.
     */
    @SuppressWarnings( "unchecked" )
    private Map<String, HandlerInfo> cacheRequestContexts( Set<Class<? extends ActionBean>> beanClasses )
    {
        // Create a map with of all String constant; key: constant value, value:
        // constant name
        // e.g., "login", "LOGIN"
        Map<String, String> fields = new HashMap<String, String>();
        for( Field field : WikiContext.class.getDeclaredFields() )
        {
            if( String.class.equals( field.getType() ) )
            {
                String fieldName = field.getName();
                String fieldValue = null;
                try
                {
                    fieldValue = (String) field.get( null );
                    fields.put( fieldValue, fieldName );
                }
                catch( Exception e )
                {
                    e.printStackTrace();
                }
            }
        }

        // Match WikiRequestContext annotations with WikiContext field values
        Map<String, HandlerInfo> contextMap = new HashMap<String, HandlerInfo>();
        for( Class<? extends ActionBean> beanClass : beanClasses )
        {
            Collection<HandlerInfo> handlers = HandlerInfo.getHandlerInfoCollection( (Class<? extends WikiActionBean>) beanClass )
                .values();

            for( HandlerInfo handler : handlers )
            {
                String eventName = handler.getRequestContext();
                String fieldName = fields.get( eventName );
                if( fieldName != null )
                {
                    contextMap.put( fieldName, handler );
                }
            }
        }
        return contextMap;
    }

}
