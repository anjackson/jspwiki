<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
        "http://www.w3.org/TR/html4/loose.dtd">

<HTML>

<HEAD>
  <TITLE><wiki:ApplicationName />: Info on <wiki:PageName /></TITLE>
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

      <%@ include file="PageHeader.jsp" %>

      <%
         if( wiki.pageExists( pagereq ) )
         {
             %>
             <table cellspacing="4">
                <tr>
                   <td><B>Page name</B></td>
                   <td><wiki:PageName /></td>
                </tr>

                <tr>
                   <td><B>Page last modified</B></td>
                   <td><%=wiki.pageLastChanged( pagereq ) %></td>
                </tr>

                <tr>
                   <td><B>Current page version</B></td>
                   <td>
                   <%
                      int version = wiki.getVersion( pagereq );
                      if( version == -1 )
                          out.println("No versioning support.");
                      else
                          out.println( version );
                    %>
                    </td>
                </tr>

                <tr>
                   <td valign="top"><b>Page revision history</b></td>
                   <td>
                       <table border="1" cellpadding="4">
                           <tr>
                               <th>Version</th>
                               <th>Date (and differences to current)</th>
                               <th>Author</th>
                               <th>Changes from previous</th>
                           </tr>

                           <%
                           Collection versions = wiki.getVersionHistory( pagereq );

                           for( Iterator i = versions.iterator(); i.hasNext(); )
                           {
                               WikiPage p = (WikiPage) i.next();

                               %>
                               <tr>
                                   <td>
                                   <A HREF="<%=wiki.getBaseURL()%>Wiki.jsp?page=<%=pageurl%>&version=<%=p.getVersion()%>"><%=p.getVersion()%></A>
                                   </td>
                                   <td>
                                   <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=-1&r2=<%=p.getVersion()%>"><%=p.getLastModified()%></A>
                                   </td>
                                   <td><%=p.getAuthor() != null ? p.getAuthor() : "unknown"%></td>
                                   <td>
                                   <% if( p.getVersion() > 1 ) { %>
                                       <A HREF="<%=wiki.getBaseURL()%>Diff.jsp?page=<%=pageurl%>&r1=<%=p.getVersion()%>&r2=<%=p.getVersion()-1%>">changes from version <%=p.getVersion()-1%> to <%=p.getVersion()%></A>
                                   <% } %>
                                   </td>
                               </tr>
                               <%
                           }
                           %>
                       </table>
                   </td>
                </tr>
             </table>
             
             <BR>
             <wiki:LinkTo>Back to <%=pagereq%></wiki:LinkTo>
             <%
         }
         %>
         <wiki:NoSuchPage>
             This page does not exist.  Why don't you go and
             <wiki:EditLink>create it</wiki:EditLink>?
         </wiki:NoSuchPage>

      <P><HR>
    </TD>
  </TR>

</TABLE>

</BODY>

</HTML>
