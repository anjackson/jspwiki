<%@ page import="org.apache.log4j.*" %>
<%@ page import="org.apache.commons.lang.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
<%@ page import="java.util.Calendar,java.util.Date" %>
<%@ page import="com.ecyrd.jspwiki.tags.WikiTagBase" %>
<%@ page errorPage="/Error.jsp" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    Logger log = Logger.getLogger("JSPWiki");
    WikiEngine wiki;

%>

<%
    // Create wiki context and check for authorization
    WikiContext wikiContext = wiki.createContext( request, WikiContext.CONFLICT );
    if(!wikiContext.hasAccess( response )) return;
    String pagereq = wikiContext.getPage().getName();
    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String usertext = request.getParameter( EditorManager.REQ_EDITEDTEXT );

    // Make the user and conflicting text presentable for display.
    usertext = StringEscapeUtils.escapeXml( usertext );
    usertext = TextUtil.replaceString( usertext, "\n", "<br />" );

    String conflicttext = wiki.getText(pagereq);
    conflicttext = StringEscapeUtils.escapeXml( conflicttext );
    conflicttext = TextUtil.replaceString( conflicttext, "\n", "<br />" );

    pageContext.setAttribute( "conflicttext",
                              conflicttext,
                              PageContext.REQUEST_SCOPE );

    log.info("Page concurrently modified "+pagereq);
    pageContext.setAttribute( "usertext",
                              usertext,
                              PageContext.REQUEST_SCOPE );

    // Stash the wiki context
    pageContext.setAttribute( WikiTagBase.ATTR_CONTEXT,
                              wikiContext,
                              PageContext.REQUEST_SCOPE );
    
    // Set the content type and include the response content
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
    String contentPage = wiki.getTemplateManager().findJSP( pageContext,
                                                            wikiContext.getTemplate(),
                                                            "ViewTemplate.jsp" );
%><wiki:Include page="<%=contentPage%>" /><%
    // Clean up the logger and clear UI messages
    NDC.pop();
    NDC.remove();
    wikiContext.getWikiSession().clearMessages();
%>
