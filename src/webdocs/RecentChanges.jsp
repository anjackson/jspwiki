<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ page import="java.text.*" %>
<%@ taglib uri="/WEB-INF/tlds/taglib.tld" prefix="jspwiki" %>

<%! 
    public void jspInit()
    {
        wiki = WikiEngine.getInstance( getServletConfig() );
    }

    public boolean isSameDay( Date a, Date b )
    {
        Calendar aa = Calendar.getInstance(); aa.setTime(a);
        Calendar bb = Calendar.getInstance(); bb.setTime(b);

        return( aa.get( Calendar.YEAR ) == bb.get( Calendar.YEAR ) &&
                aa.get( Calendar.DAY_OF_YEAR ) == bb.get( Calendar.DAY_OF_YEAR ) );
    }

    Category log = Category.getInstance("JSPWiki.RecentChanges");
    WikiEngine wiki;
%>


<%
    Collection list = null;

    // This is required by PageHeader.jsp
    String pagereq = "Recent Changes";

    NDC.push( wiki.getApplicationName()+":"+pagereq );

    String pageurl = wiki.encodeName( pagereq );

    list = wiki.getRecentChanges();

    response.setContentType("text/html; charset="+wiki.getContentEncoding() );
%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><%=wiki.getApplicationName()%> Recent Changes</TITLE>
  <%@ include file="cssinclude.js" %>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD CLASS="leftmenu" WIDTH="15%" VALIGN="top" NOWRAP="nowrap">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>
    <TD CLASS="page" WIDTH="85%" VALIGN="top">
      <%@ include file="PageHeader.jsp" %>

      <% if( list != null ) 
      {
      %>
          <table border="0" cellpadding="4">
          <%
          
          Date olddate = new Date(0);
          SimpleDateFormat fmt  = new SimpleDateFormat( "dd.MM.yyyy" );
          SimpleDateFormat tfmt = new SimpleDateFormat( "HH:mm:ss" );

          for( Iterator i = list.iterator(); i.hasNext(); )
          {
              WikiPage pageref = (WikiPage) i.next();

              Date lastmod = pageref.getLastModified();

              if( !isSameDay( lastmod, olddate ) )
              {
                  %>
                  <TR>
                     <TD COLSPAN="2"><B><%=fmt.format(lastmod)%></B></TD>
                  </TR>
                  <%
                  olddate = lastmod;
              }

              %>
              <TR>
                  <TD WIDTH="30%"><A HREF="Wiki.jsp?page=<%=wiki.encodeName(pageref.getName())%>"><%=pageref.getName()%></A></TD>
                  <TD><%=tfmt.format(lastmod)%></TD>
              </TR>
              <%
          }
          %>
          </table>
      <%
      }
      %>

      <P>

    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
<%
    NDC.pop();
    NDC.remove();
%>
