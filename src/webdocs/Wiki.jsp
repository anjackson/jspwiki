<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }
    Category log = Category.getInstance("JSPWiki"); 
    WikiEngine wiki;
%>

<%
    String pagereq = wiki.safeGetParameter( request, "page" );
    String headerTitle = "";

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );
    
    log.info("Request for page '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser() );

    String specialpage = wiki.getSpecialPageReference( pagereq );

    if( specialpage != null )
    {
        response.sendRedirect( specialpage );
        return;        
    }

    //
    //  Determine requested version.  If version == -1,
    //  then fetch current version.
    //
    int version          = -1;
    String rev           = request.getParameter("version");
    String pageReference = "this page";
    String versionInfo   = "";

    if( rev != null )
    {
        version = Integer.parseInt( rev );
        pageReference = "current version";
    }

    WikiPage wikipage = wiki.getPage( pagereq, version );

    // In the future, user access permits affect this
    boolean isEditable = (version < 0);

    String rssURL = wiki.getGlobalRSSURL();
    //
    //  Alright, then start responding.
    //

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: <%=pagereq%><%=versionInfo%></TITLE>
  <%@ include file="cssinclude.js" %>
  <%if( rssURL != null ) { %>
      <link rel="alternate" type="application/rss+xml" title="RSS feed" href="<%=rssURL%>" />
  <% } %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <% if( isEditable ) { %>
          <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Edit <%=pageReference%></A>
       <% } %>
       </P>
       <%@ include file="LeftMenuFooter.jsp" %>
       <%if(rssURL != null) { %>
       <P>
           <DIV ALIGN="center">
           <A HREF="<%=rssURL%>"><IMG SRC="<%=wiki.getBaseURL()%>images/xml.png" BORDER="0" title="Aggregate the RSS feed!"/></A><BR/>
           <A HREF="http://127.0.0.1:5335/system/pages/subscriptions/?url=<%=rssURL%>"><IMG SRC="<%=wiki.getBaseURL()%>images/xmlCoffeeCup.png" BORDER="0" title="Aggregate the RSS feed in Radio Userland!"/></A>
           
           </DIV>
       </P>
       <% } %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

      <% if( version > 0 ) { %>
         <FONT COLOR="red">
            <P CLASS="versionnote">This is version <%=version%>.  It is not the current version,
            and thus it cannot be edited.  <A HREF="<%=wiki.getBaseURL()%>Wiki.jsp?page=<%=pageurl%>">(Back to current version)</A></P> 
         </FONT>
      <% } %>

      <%
         if( wiki.pageExists( pagereq ) )
         {
             // if version == -1, the current page is returned.
             out.println(wiki.getHTML(pagereq, version));
         }
         else
         {
             if(version == -1)
             {
             %>
                This page does not exist.  Why don't you go and
                <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">create it</A>?
             <%
             }
             else
             {
             %>
                This version of the page does not seem to exist.
             <%
             }
         }
      %>

      <P><HR>
      <table border="0" width="100%">
        <tr>
          <td align="left">
             <% if( isEditable ) { %>
                 <A HREF="<%=wiki.getBaseURL()%>Edit.jsp?page=<%=pageurl%>">Edit <%=pageReference%></A>.
                 &nbsp;&nbsp;
             <% } %>
             <% if( wikipage != null ) { %>
                 <A HREF="<%=wiki.getBaseURL()%>PageInfo.jsp?page=<%=pageurl%>">More info...</A></I><BR>
             <% } %>
          </td>
        </tr>
        <tr>
          <td align="left">
             <FONT size="-1">
	     <%
             if( wikipage != null )
             {
                 java.util.Date lastchange = wikipage.getLastModified();

                 String author = wikipage.getAuthor();
                 if( author == null ) author = "unknown";

                 if( version == -1 )
                 {
                     %>                
                     <I>This page last changed on <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=<%=version%>"><%=lastchange%></A> by <%=author%>.</I>
                     <%
                 } else {
                     %>
                     <I>This particular version was published on <%=lastchange%> by <%=author%></I>.
                     <%
                 }
             } else {
                 %>
                 <I>Page not created yet.</I>
                 <%
             }
             %>
             </FONT>
          </td>
        </tr>
      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>

<%
    NDC.pop();
    NDC.remove();
%>

