package com.ecyrd.jspwiki.ui.stripes;

import java.util.List;
import java.util.Map;

/**
 * Transforms a JspDocument from standard JSP markup to Stripes markup.
 */
public class StripesJspTransformer extends AbstractJspTransformer
{

    public void transform( Map<String, Object> sharedState, JspDocument doc )
    {
        // Add Stripes taglib entry
        verifyStripesTaglib( doc );
        
        // Process HTML nodes
        List<Node> nodes = doc.getNodes();
        for( Node node : nodes )
        {
            // For all HTML tags...
            if( node.isHtmlNode() )
            {
                Tag tag = (Tag)node;
                
                // Change <form> to <stripes:form>
                if( "form".equals( tag.getName() ) )
                {
                    migrateFormTag( tag );
                }

                // Change <input type="*"> tags to <stripes:*>
                else if( "input".equals( tag.getName() ) )
                {
                    migrateInputTag( tag );
                }
            }
        }
    }

    private void verifyStripesTaglib( JspDocument doc )
    {
        // Add the Stripes taglib declaration if it's not there already
        List<Node> nodes = doc.getNodes( NodeType.JSP_DIRECTIVE );
        boolean declaresStripesTaglib = false;
        Node lastTaglib = null;
        for ( Node node : nodes )
        {
            JspDirective directive = (JspDirective)node;
            if ( "taglib".equals( node.getName() ) )
            {
                lastTaglib = node;
                Attribute attribute = directive.getAttribute( "prefix" );
                if ( attribute != null && "stripes".equals( attribute.getValue() ) )
                {
                    declaresStripesTaglib = true;
                    break;
                }
            }
        }
        if ( !declaresStripesTaglib )
        {
            Text linebreak = new Text( doc );
            linebreak.setValue( System.getProperty( "line.separator" ) );
            JspDirective directive = new JspDirective( doc );
            directive.setName( "taglib" );
            Attribute attribute = new Attribute( doc );
            attribute.setName( "uri" );
            attribute.setValue( "/WEB-INF/stripes.tld" );
            directive.addAttribute( attribute );
            attribute = new Attribute( doc );
            attribute.setName( "prefix" );
            attribute.setValue( "stripes" );
            directive.addAttribute( attribute );
            if ( lastTaglib == null )
            {
                doc.getRoot().addChild( directive, 0 );
                directive.addSibling( linebreak );
            }
            else
            {
                linebreak.addSibling( directive );
                lastTaglib.addSibling( linebreak );
            }
            message( doc.getRoot(), "Added Stripes taglib directive." );
        }
        
    }

    /**
     * Migrates the &lt;input&gt; tag.
     * @param tag the AbstractNode that represents the form tag being processed.
     */
    private void migrateInputTag( Tag tag )
    {

        // Move 'type' attribute value to the localname
        Attribute attribute = tag.getAttribute( "type" );
        if( attribute != null )
        {
            // If a submit input, tell user to change the "name"
            // value to something useful for Stripes
            if( "submit".equals( attribute.getValue() ) )
            {
                Node nameAttribute = tag.getAttribute( "name" );
                String nameValue = nameAttribute == null ? "(not set)" : nameAttribute.getName();
                message( nameAttribute, "NOTE: the \"name\" attribute of <input type=\"submit\" is \"" + nameValue
                                        + "\"" );
            }

            // Move type attribute to qname
            String type = attribute.getValue();
            message( attribute, "Changed <input type=\"" + type + "\"> to <stripes:" + type + ">." );
            tag.setName( "stripes:" + type );
            tag.removeAttribute( attribute );
        }

        // If embedded markup in "value" attribute, move to child
        // nodes
        attribute = tag.getAttribute( "value" );
        if( attribute != null )
        {
            List<Node> children = attribute.getChildren();
            if( children.size() > 1 || (children.size() == 1 && children.get( 0 ).getType() != NodeType.TEXT) )
            {
                // Remove the attribute
                tag.removeAttribute( attribute );
                // Move all of the attribute's nodes to the
                // children nodes
                for( Node valueNode : attribute.getChildren() )
                {
                    tag.addChild( valueNode );
                }
                message( attribute,
                         "Moved embedded tag(s) in <input> \"value\" attribute to the tag body. These are now child element(s) of <input>." );
            }
        }
    }

    /**
     * Migrates the &lt;form&gt; tag.
     * @param tag the AbstractNode that represents the form tag being processed.
     */
    private void migrateFormTag( Tag tag )
    {
        message( tag, "Changed name to <stripes:form>." );
        tag.setName( "stripes:form" );

        // Change "accept-charset" attribute to acceptcharset
        Node attribute = tag.getAttribute( "accept-charset" );
        if( attribute != null )
        {
            message( attribute, "Changed name to \"acceptcharset\"." );
            attribute.setName( "acceptcharset" );
        }

        // If URL has parameters, add them as child <stripes:param>
        // elements
        // e.g., param in <form action="Login.jsp?tab=profile>
        // becomes <stripes:param name="tab" value="profile">
        attribute = tag.getAttribute( "action" );
        if( attribute != null )
        {
            String actionUrl = attribute.getValue();
            if( actionUrl != null )
            {
                int qmark = actionUrl.indexOf( '?' );
                if( qmark < actionUrl.length() - 1 )
                {
                    // Change "action" attribute"
                    String trimmedPath = actionUrl.substring( 0, qmark );
                    message( attribute, "Trimmed value to \"" + trimmedPath + "\"");
                    attribute.setValue( trimmedPath );

                    // Split the parameters and add a new
                    // <stripes:param> child element for each
                    String[] params = actionUrl.substring( qmark + 1 ).split( "&" );
                    for( String param : params )
                    {
                        if( param.length() > 1 )
                        {
                            JspDocument doc = tag.getJspDocument();
                            String name = param.substring( 0, param.indexOf( '=' ) );
                            String value = param.substring( name.length() +1 );
                            Tag stripesParam = new Tag( doc, NodeType.HTML_COMBINED_TAG );
                            stripesParam.setName( "stripes:param" );
                            Attribute nameAttribute = new Attribute( doc );
                            nameAttribute.setName( "name" );
                            nameAttribute.setValue( name );
                            stripesParam.addAttribute( nameAttribute );
                            Attribute valueAttribute = new Attribute( doc );
                            valueAttribute.setName( "value" );
                            valueAttribute.setValue( value );
                            stripesParam.addAttribute( valueAttribute );
                            tag.addChild( stripesParam );
                            message( tag, "Created <stripes:form> child element <stripes:param name=\"" + name + "\""
                                           + " value=\"" + value + "\"/>." );
                        }
                    }
                }
            }
        }
    }

}
