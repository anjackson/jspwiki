<H3 CLASS="leftmenuheading"><A HREF="SystemInfo.jsp"><wiki:ApplicationName /></A></H3>

<!-- LeftMenu is automatically generated from a Wiki page called "LeftMenu" -->

<P>
    <wiki:InsertPage page="LeftMenu" />
    <wiki:NoSuchPage page="LeftMenu">
        <HR><P>
        <P ALIGN="center">
        <I>No LeftMenu!</I><BR>
        <wiki:EditLink page="LeftMenu">Please make one.</wiki:EditLink><BR>
        </P>
        <P><HR>
    </wiki:NoSuchPage>
</P>
<P>
<DIV ALIGN="center" CLASS="username">
<%
    String leftMenuUser = wiki.getUserName(request);
    if( leftMenuUser != null )
    {
        %>
        <B>G'day,</B><BR>
        <%=wiki.textToHTML( wikiContext, "["+leftMenuUser+"]" )%>
        <%
    }
    else
    {
        %><TT>
        Set your name in<BR>
        <%=wiki.textToHTML( wikiContext, "[UserPreferences]!" )%>
        </TT>
        <%
    }
%>
</DIV>

<!-- End of automatically generated page -->

