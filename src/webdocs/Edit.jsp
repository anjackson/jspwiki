<%@ page import="org.apache.log4j.*" %>
<%@ page import="com.ecyrd.jspwiki.*" %>

<%! 
    public void jspInit()
    {
        wiki = new WikiEngine( getServletContext() );
    }

    Category log = Category.getInstance("JSPWiki");
    WikiEngine wiki;
%>


<%
    String pagereq = request.getParameter("page");

    if( pagereq == null )
    {
        throw new ServletException("No page defined");
    }

    String action = request.getParameter("action");

    if( action != null && action.equals("save") )
    {
        log.info("Saving page "+pagereq);

        wiki.saveText( pagereq, request.getParameter("text") );

        response.sendRedirect("Wiki.jsp?page="+pagereq);
        return;
    }

    log.info("Editing page "+pagereq);
%>

<HTML>

<HEAD>
  <TITLE>JSPWiki Editor</TITLE>
</HEAD>

<BODY BGCOLOR="#FFD0FF">

<TABLE BORDER="0" CELLSPACING="8">

  <TR>
    <TD WIDTH="15%" VALIGN="top">
       <%@ include file="LeftMenu.jsp" %>
       <P>
       <A HREF="Wiki.jsp?page=HelpOnEditing">Help on editing</A>
       </P>
    </TD>
    <TD WIDTH="85%" VALIGN="top">
      <H1>Edit page <%=pagereq%></H1>

      <FORM action="Edit.jsp?page=<%=pagereq%>&action=save" method="POST">

      <INPUT type="hidden" name="page" value="<%=pagereq%>">
      <INPUT type="hidden" name="action" value="save">

      <TEXTAREA name="text" rows="25" cols="80"><%=wiki.getText(pagereq)%></TEXTAREA>

      <P>
      <input type="submit" name="ok" value="Save" />
      &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
      <A HREF="Wiki.jsp?page=<%=pagereq%>">Cancel</A>
      <P>
      Here's a short reminder on what elements you have at your disposal:
      
      <table cellspacing = "4">
        <tr>
          <td>---</td><td>Horizontal ruler</td>
        </tr>
        <tr>
          <td>{{{, }}}</td><td>Begin/end code block</td>
        </tr>
        <tr>
          <td>\\</td><td>Forced line break</td>
        </tr>
        <tr>
          <td>[link]</td><td>Create hyperlink to "link", where "link"
          can be either an internal <A HREF="Wiki.jsp?page=WikiName">WikiName</A>
          or an external link (http://)</td>
        </tr>
        <tr>
          <td>[text|link]</td>
          <td>Create a hyperlink where the link text is different from the actual
          hyperlink link.</td>
        </tr>
        <tr>
          <td>*</td><td>Make a bulleted list (must be in first column).  Use more (**) for 
          deeper indentations.</td>
        </tr>
        <tr>
          <td>__text__</td><td>Makes text bold</td>
        </tr>

        <tr>
          <td>''text''</td><td>Makes text in italics</td>
        </tr>

      </table>

      Don't try to use HTML, since it just won't work.

      </FORM>

    </TD>
  </TR>

</TABLE>


<DIV ALIGN="right">
<FONT SIZE="-1">
  <I>JSPWiki by <A HREF="mailto:jalkanen+jspwiki@regex.fi">Janne Jalkanen</A>
</FONT>
</DIV>

</BODY>

</HTML>