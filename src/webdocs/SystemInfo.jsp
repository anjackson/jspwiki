<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>

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
    if( pagereq == null )
        pagereq = "SystemInfo";
    String pageurl = wiki.encodeName( pagereq );
    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%>: SystemInfo</TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD CLASS="leftmenu" WIDTH="10%" VALIGN="top" NOWRAP="true">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD CLASS="page" WIDTH="85%" VALIGN="top">

      <H1>System Information</H1>

      <table border="1" cellspacing="4">
        <tr>
          <td><b>Application name<b></td>
          <td><%=wiki.getApplicationName()%></td>
        </tr>

        <tr>
          <td><b>JSPWiki engine version</b></td>
          <td><%=Release.VERSTR%></td>
        </tr>

        <tr>
          <td><b>Character encoding</b></td>
          <td><%=wiki.getContentEncoding()%></td>
        </tr>

        <tr>
          <td><b>Total number of pages</b></td>
          <td><%=wiki.getPageCount()%></td>
        </tr>

        <tr>
          <td><b>Current page provider</b></td>
          <td><%=wiki.getCurrentProvider()%></td>
        </tr>

        <tr>
          <td valign="top"><b>Available InterWiki links</b></td>
          <td>
            <%
               for( Iterator i = wiki.getAllInterWikiLinks().iterator(); i.hasNext(); )
               {
                   String link = (String) i.next();
                   %>
                   <%=link%>, <I>links to <%=wiki.getInterWikiURL(link)%></I><BR>
                   <%
               }
            %>
          </td>
        </tr>

        <tr>
           <td valign="top"><b>Inlined images are</b></td>
           <td>
             <%
                for( Iterator i = wiki.getAllInlinedImagePatterns().iterator(); i.hasNext(); )
                {
                    %>
                    <%=i.next()%><BR>
                    <%
                }
             %>
           </td>
        </td>

      </table>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>


