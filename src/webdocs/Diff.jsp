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
    String pagereq = request.getParameter("page");

    if( pagereq == null )
    {
        pagereq = "Main";
    }

    String srev1 = request.getParameter("r1");
    String srev2 = request.getParameter("r2");

    if( srev1 == null || srev2 == null )
    {
        throw new ServletException("Empty parameters given to Diff.jsp");
    }    

    int ver1 = Integer.parseInt( srev1 );
    int ver2 = Integer.parseInt( srev2 );

    log.debug("Request for page diff for '"+pagereq+"' from "+request.getRemoteHost()+" by "+request.getRemoteUser()+".  R1="+ver1+", R2="+ver2 );

%>

<HTML>

<HEAD>
  <TITLE><%=Release.APPNAME%>: Diff <%=pagereq%></TITLE>
</HEAD>

<BODY BGCOLOR="#FFFFFF">

<TABLE BORDER="0" CELLSPACING="8" width="95%">

  <TR>
    <TD WIDTH="10%" VALIGN="top">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <%@ include file="LeftMenuFooter.jsp" %>
    </TD>

    <TD WIDTH="85%" VALIGN="top">

      <%@ include file="PageHeader.jsp" %>

      <%
         if( wiki.pageExists( pagereq ) )
         {
             %>
             Difference between revision <%=ver1%> and <%=ver2%>
             <P>
             <PRE>
<%=wiki.getDiff( pagereq, ver2, ver1 )%>
             </PRE>
             <%
         }
         else
         {
         %>
             This page does not exist.  Why don't you go and
             <A HREF="Edit.jsp?page=<%=pagereq%>">create it</A>?
         <%
         }
      %>

      <P><HR>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>


