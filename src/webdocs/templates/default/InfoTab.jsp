<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.*" %>
<%@ page import="com.ecyrd.jspwiki.auth.permissions.*" %>
<%@ page import="java.security.Permission" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.default"/>

<%!
  int MAXATTACHNAMELENGTH = 30;

  // FIXME: this should better be something like a wiki:Pagination TLD tag
  // FIXME: how to i18n
  //
  // 0 20 40 60
  // 0 20 40 60 80 next
  // previous 20 40 *60* 80 100 next
  // previous 40 60 80 100 120

  /* makePagination : return html string with pagination links
   *    (eg:  previous 1 2 3 next)
   * startitem  : cursor
   * itemcount  : total number of items
   * pagesize   : number of items per page
   * maxpages   : number of pages directly accessible via a pagination link
   * linkAttr : html attributes of the generated links: use '%s' to replace with item offset
   */
  String wiki_Pagination( int startitem, int itemcount, int pagesize, int maxpages, String linkAttr, PageContext pageContext )
  {
    if( itemcount <= pagesize ) return null;

    int maxs = pagesize * maxpages;
    int mids = pagesize * ( maxpages / 2 );

    StringBuffer pagination = new StringBuffer();
    pagination.append( "<div class='pagination'>");
    pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination") );


    int cursor = 0;
    int cursormax = itemcount;

    if( itemcount > maxs )   //need to calculate real window ends
    {
      if( startitem > mids ) cursor = startitem - mids;
      if( (cursor + maxs) > itemcount )
        cursor = ( ( 1 + itemcount/pagesize ) * pagesize ) - maxs ;

      cursormax = cursor + maxs;
    }

    if( (startitem == -1) || (cursor > 0) )
      appendLink ( pagination, linkAttr, 0, pagesize,
                   LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.first"), pageContext );
    if( (startitem != -1 ) && (startitem-pagesize >= 0) )
      appendLink( pagination, linkAttr, startitem-pagesize, pagesize,
                  LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.previous"), pageContext );

    if( startitem != -1 )
    {
      while( cursor < cursormax )
      {
        if( cursor == startitem )
        {
          pagination.append( "<span class='cursor'>" + (1+cursor/pagesize)+ "</span>&nbsp;&nbsp;" );
        }
        else
        {
          appendLink( pagination, linkAttr, cursor, pagesize, Integer.toString(1+cursor/pagesize), pageContext );
        }
        cursor += pagesize;
      }
    }

    if( (startitem != -1) && (startitem + pagesize < itemcount) )
      appendLink( pagination, linkAttr, startitem+pagesize, pagesize,
                  LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.next"), pageContext );

    if( (startitem == -1) || (cursormax < itemcount) )
      appendLink ( pagination, linkAttr, ( (itemcount/pagesize) * pagesize ), pagesize,
                   LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.last"), pageContext );

    if( startitem == -1 )
    {
      pagination.append( "<span class='cursor'>" );
      pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.all") );
      pagination.append( "</span>&nbsp;&nbsp;" );
    }
    else
    {
      appendLink ( pagination, linkAttr, -1 , -1,
                   LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.all"), pageContext );
    }

    //pagination.append( " (Total items: " + itemcount + ")</div>" );
    pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.total" ) );
    //pagination.append( LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.total", [itemcount] ) );
    pagination.append( "</div>" );

    return pagination.toString();
  }

  // linkAttr : use '%s' to replace with cursor offset
  // eg :
  // linkAttr = "href='#' title='%s' onclick='$(start).value= %s; updateSearchResult();'";
  void appendLink( StringBuffer sb, String linkAttr, int linkFrom, int pagesize, String linkText, PageContext pageContext )
  {
    String title =  LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.title.showall");
    //if( linkFrom > -1 ) title = "Show page from " + (linkFrom+1) + " to "+ (linkFrom+pagesize) ;
    if( linkFrom > -1 )
      title = LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.title.show" );
   //   title = LocaleSupport.getLocalizedMessage(pageContext, "info.pagination.title.show", [linkFrom+1, linkFrom+pagesize] );

    sb.append( "<a title=\"" + title + "\" " );
    sb.append( TextUtil.replaceString( linkAttr, "%s", Integer.toString(linkFrom) ) );
    sb.append( ">" + linkText + "</a>&nbsp;&nbsp;" );
  } ;

%>

<%
  /* see commonheader.jsp */
  String prefDateFormat = (String) session.getAttribute("prefDateFormat");
  String prefTimeZone   = (String) session.getAttribute("prefTimeZone");

  int pagesize  = 20; //default #revisions shown per page
  int maxpages  = 9;  //max #paginations links -- choose odd figure
  int itemcount = 0;  //number of page versions

  WikiContext wikiContext = WikiContext.findContext(pageContext);
  WikiPage wikiPage = wikiContext.getPage();

  String creationDate   ="";
  String creationAuthor ="";
  //FIXME -- seems not to work correctly for attachments !!
  WikiPage firstPage = wikiContext.getEngine().getPage( wikiPage.getName(), 1 );
  if( firstPage != null )
  {
    creationAuthor = firstPage.getAuthor();
    //creationDate   = wiki_PageDate( firstPage, prefDateFormat, prefTimeZone );
  }

  try
  {
    itemcount = wikiPage.getVersion(); /* highest version */
  }
  catch( Exception  e )  { /* dont care */ }

  int startitem = itemcount;

  String parm_start = (String)request.getParameter( "start" );
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;
  if( startitem > itemcount ) startitem = itemcount;
  if( startitem < -1 ) startitem = 0;

  String parm_pagesize = (String)request.getParameter( "pagesize" );
  if( parm_pagesize != null ) pagesize = Integer.parseInt( parm_pagesize ) ;

  if( startitem > -1 ) startitem = ( (startitem/pagesize) * pagesize );

  String linkAttr = "href='#' onclick='location.href= $(\"moreinfo\").href + \"&start=%s\"; ' ";
  String pagination = wiki_Pagination(startitem, itemcount, pagesize, maxpages, linkAttr, pageContext);

%>

<wiki:PageExists>

<%-- part 1 : normal wiki pages --%>
<wiki:PageType type="page">

  <p>
  <fmt:message key='info.lastmodified'>
    <fmt:param><wiki:PageVersion >1</wiki:PageVersion></fmt:param>
    <fmt:param>
      <a href="<wiki:DiffLink format='url' version='latest' newVersion='previous' />"
        title="<fmt:message key='info.pagediff.title' />" >
        <fmt:formatDate value="<%= wikiPage.getLastModified() %>" pattern="${prefDateFormat}" />
      </a>
    </fmt:param>
    <fmt:param><wiki:Author /></fmt:param>
  </fmt:message>

  <a href="<wiki:Link format='url' jsp='rss.jsp'>
             <wiki:Param name='page' value='<%=wikiPage.getName()%>'/>
             <wiki:Param name='mode' value='wiki'/>
           </wiki:Link>"
    title="<fmt:message key='info.rsspagefeed.title'>
             <fmt:param><wiki:PageName /></fmt:param>
           </fmt:message>" >
    <img src="<wiki:Link jsp='images/xml.png' format='url'/>" border="0" alt="[RSS]"/>
  </a>

  <wiki:CheckVersion mode="notfirst">
    <br />
    <fmt:message key='info.createdon'>
      <%--<fmt:param><wiki:Link version="1"><%= creationDate %></wiki:Link></fmt:param>--%>
      <fmt:param>
        <wiki:Link version="1">
          <fmt:formatDate value="<%= firstPage.getLastModified() %>" pattern="${prefDateFormat}" />
        </wiki:Link>
      </fmt:param>
      <fmt:param><%= creationAuthor %></fmt:param>
    </fmt:message>
  </wiki:CheckVersion>
  </p>


  <wiki:Permission permission="rename">
    <form action="<wiki:Link format='url' jsp='Rename.jsp'/>"
           class="wikiform"
            name="renameform"
        onsubmit="return Wiki.submitOnce(this);"
          method="post" accept-charset="<wiki:ContentEncoding />" >

      <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" >
      <input type="submit" name="rename" value="Rename Page" style="display:none;"/>
      <input type="button" name="renamx" value="<fmt:message key='info.rename.submit' />"
          onclick="this.form.rename.click();" />
      <input type="text" name="renameto" value="<wiki:Variable var='pagename' />" size="40" >&nbsp;&nbsp;
      <input type="checkbox" name="references" checked="checked" >
      <fmt:message key="info.updatereferrers"/>

    </form>
  </wiki:Permission>


  <wiki:Permission permission="delete">
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="wikiform"
            name="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') && Wiki.submitOnce(this) );">

      <input type="submit" name="delete-all" value="Delete" id="delete-all" style="display:none;"/>
      <input type="button" name="delete-alx" value="<fmt:message key='info.delete.submit'/>"
          onclick="$('delete-all').click();"
          <wiki:HasAttachments>disabled</wiki:HasAttachments> />
      <wiki:HasAttachments><fmt:message key='info.delete.attachmentwarning'/></wiki:HasAttachments>

    </form>
  </wiki:Permission>


  <%-- link to full page info, when not yet in info context --%>
  <p>
    <wiki:CheckRequestContext context='!info'>
      <a href="<wiki:PageInfoLink format='url' />" id="moreinfo" ><fmt:message key='info.moreinfo'/></a>
    </wiki:CheckRequestContext>

    <wiki:CheckRequestContext context='info'>
      <a href="<wiki:PageInfoLink format='url' />" id="moreinfo" style="display:none;" ></a>
      <fmt:message key='info.backtomainpage'>
        <fmt:param><wiki:Link><wiki:PageName/></wiki:Link></fmt:param>
      </fmt:message>
    </wiki:CheckRequestContext>
  </p>


  <%-- page version history goes here --%>
  <wiki:TabbedSection>

  <wiki:Tab id="versionHistory" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.history")%>" >

    <wiki:CheckVersion mode="first"><fmt:message key="info.noversions"/></wiki:CheckVersion>
    <wiki:CheckVersion mode="notfirst">
    <%-- if( itemcount > 1 ) { --%>

    <%= (pagination == null) ? "" : pagination %>

    <div class="zebra-table <wiki:CheckRequestContext context='info'>sortable table-filter</wiki:CheckRequestContext>">
    <table class="wikitable" >
      <tr>
        <th><fmt:message key="info.version"/></th>
        <th><fmt:message key="info.date"/></th>
        <th><fmt:message key="info.size"/></th>
        <th><fmt:message key="info.author"/></th>
        <th><fmt:message key="info.changes"/></th>
      <th width="30%"><fmt:message key="info.changenote"/></th>
      </tr>

      <wiki:HistoryIterator id="currentPage">
      <% if( ( startitem == -1 ) ||
             (  ( currentPage.getVersion() >= startitem )
             && ( currentPage.getVersion() < startitem + pagesize ) ) )
         {
       %>
      <tr>
        <td>
          <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
            <wiki:PageVersion/>
          </wiki:LinkTo>
        </td>

        <td><fmt:formatDate value="<%= currentPage.getLastModified() %>" pattern="${prefDateFormat}" /></td>
        <td>
          <%--<fmt:formatNumber value='<%=((float)currentPage.getSize())/1000 %>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;Kb--%>
          <wiki:PageSize />
        </td>
        <td><wiki:Author /></td>

        <td>
          <wiki:CheckVersion mode="notfirst">
            <wiki:DiffLink version="current" newVersion="previous"><fmt:message key="info.difftoprev"/></wiki:DiffLink>
            <wiki:CheckVersion mode="notlatest"> | </wiki:CheckVersion>
          </wiki:CheckVersion>

          <wiki:CheckVersion mode="notlatest">
            <wiki:DiffLink version="latest" newVersion="current"><fmt:message key="info.difftolast"/></wiki:DiffLink>
          </wiki:CheckVersion>
        </td>

         <td class="changenote">
           <%
              String changeNote = (String)currentPage.getAttribute( WikiPage.CHANGENOTE );
              changeNote = (changeNote != null) ? TextUtil.replaceEntities( changeNote ) : "" ;
           %>
           <%= changeNote %>
         </td>

      </tr>
      <% } %>
      </wiki:HistoryIterator>

    </table>
    </div>
    <%= (pagination == null) ? "" : pagination %>

    <%-- } /* itemcount > 1 */ --%>
    </wiki:CheckVersion>
  </wiki:Tab>

  <wiki:Tab id="incomingLinks" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.incoming")%>">
    <wiki:LinkTo><wiki:PageName /></wiki:LinkTo>
    <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " />
  </wiki:Tab>

  <wiki:Tab id="outgoingLinks" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab.outgoing")%>">
      <wiki:Plugin plugin="ReferredPagesPlugin" args="depth='1' type='local'" />
  </wiki:Tab>

  </wiki:TabbedSection> <%-- end of .tabs --%>


</wiki:PageType>



<%-- part 2 : attachments --%>
<wiki:PageType type="attachment">

  <h3><fmt:message key="info.uploadnew"/></h3>

  <%-- FIXME <wiki:Permission permission="upload"> --%>
  <form action="<wiki:Link context='att' format='url' absolute='true'/>"
         class="wikiform"
          name="uploadform"
      onsubmit="return Wiki.submitOnce( this );"
        method="post" accept-charset="<wiki:ContentEncoding/>"
       enctype="multipart/form-data" >

  <%-- Do NOT change the order of wikiname and content, otherwise the
       servlet won't find its parts. --%>


  <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
  <table>
  <tr>
    <td colspan="2"><div class="formhelp"><fmt:message key="info.uploadnew.help" /></div></td>
  </tr>
  <tr>
    <td><label for="content"><fmt:message key="info.uploadnew.filename" /></label></td>
    <td><input type="file" name="content" size="60"/></td>
  </tr>
  <tr>
    <td><label for="changenote"><fmt:message key="info.uploadnew.changenote" /></label></td>
    <td>
    <input type="text" name="changenote" maxlength="80" size="60" />
    </td>
  </tr>
  <tr>
    <td></td>
    <td>
    <input type="submit" name="upload" value="Upload" style="display:none;"/>
    <input type="button" name="uploax" value="<fmt:message key='info.uploadnew.submit' />" onclick="this.form.upload.click();"/>
    <input type="hidden" name="action"  value="upload" />
    <input type="hidden" name="nextpage" value="<wiki:PageInfoLink format='url'/>" />
    </td>
  </tr>
  </table>

  </form>
  <%-- </wiki:Permission>

<%
  /* java hacking coz cant check for negative permission */
  AuthorizationManager mgr = wikiContext.getEngine().getAuthorizationManager();
  WikiPage   p             = wikiContext.getPage();
  Permission permission    = new PagePermission( p, "upload" );
  WikiSession s            = wikiContext.getWikiSession();
  if( ! mgr.checkPermission( s, permission ) ) {
 %>
  <div class="formhelp"><fmt:message key="info.uploadnew.nopermission"/></div>
<% } %>

--%>

  <wiki:Permission permission="delete">

    <h3><fmt:message key="info.deleteattachment"/></h3>
    <form action="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
           class="wikiform"
            name="deleteForm"
          method="post" accept-charset="<wiki:ContentEncoding />"
        onsubmit="return( confirm('<fmt:message key="info.confirmdelete"/>') && Wiki.submitOnce(this) );" >

     <input type="submit" name="delete-all" value="Delete" id="delete-all" style="display:none;"/>
     <input type="button" name="delete-alx" value="<fmt:message key='info.deleteattachment.submit' />"
          onclick="$('delete-all').click();" />

    </form>

  </wiki:Permission>

  <p>
  <fmt:message key='info.backtoparentpage'>
    <fmt:param><a href="<wiki:LinkToParent format='url' />&tab=attachments"><wiki:ParentPageName /></a></fmt:param>
  </fmt:message>
  </p>


  <%-- FIXME why not add pagination here - no need for large amounts of attach versions on one page --%>
  <h3><fmt:message key='info.attachment.history' /></h3>

  <div class="zebra-table"><div class="slimbox-img sortable">
  <table class="wikitable">
    <tr>
      <th><fmt:message key="info.attachment.type"/></th>
      <%--<th><fmt:message key="info.attachment.name"/></th>--%>
      <th><fmt:message key="info.version"/></th>
      <th><fmt:message key="info.size"/></th>
      <th><fmt:message key="info.date"/></th>
      <th><fmt:message key="info.author"/></th>
      <%--<th style="display:none;">Actions</th>--%>
      <th width="30%"><fmt:message key="info.changenote"/></th>
    </tr>

    <wiki:HistoryIterator id="att"><%-- <wiki:AttachmentsIterator id="att"> --%>
    <%
      String name = att.getName(); //att.getFileName();
      int dot = name.lastIndexOf(".");
      String attachtype = ( dot != -1 ) ? name.substring(dot+1) : "";

      String sname = name;
      if( sname.length() > MAXATTACHNAMELENGTH ) sname = sname.substring(0,MAXATTACHNAMELENGTH) + "...";
    %>

    <tr>
      <td><div id="attach-<%= attachtype %>" class="attachtype"><%= attachtype %></div></td>
      <%--<td><wiki:LinkTo title="<%= name %>" ><%= sname %></wiki:LinkTo></td>--%>
      <%--FIXME classs parameter throws java exception
      <td><wiki:Link version='<%=Integer.toString(att.getVersion())%>'
                       title="<%= name %>"
                       class="attachment" ><wiki:PageVersion /></wiki:Link></td>
      --%>
      <td><a href="<wiki:Link version='<%=Integer.toString(att.getVersion())%>' format='url' />"
                       title="<%= name %>"
                       class="attachment" ><wiki:PageVersion /></a></td>
      <td style="text-align:right;">
        <fmt:formatNumber value='<%=((float)att.getSize())/1000 %>' groupingUsed='false' maxFractionDigits='1' minFractionDigits='1'/>&nbsp;Kb
      </td>
	  <td><fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefDateFormat}" /></td>
      <td><wiki:Author /></td>
      <%--<td>  Deletion of a single version is not yet supported? FIXME
        &nbsp;&nbsp;
        <wiki:Permission permission="delete">
            <input type="button"
                value="Delete"
                url="<wiki:Link format='url' context='<%=WikiContext.DELETE%>' />"
            onclick="AttachTable.remove(this.url);" />
        </wiki:Permission>
      </td>--%>
      <td>
      <%
         String changeNote = (String)att.getAttribute(WikiPage.CHANGENOTE);
         if( changeNote != null ) {
         %><%=changeNote%><%
         }
      %>
      </td>
    </tr>
    </wiki:HistoryIterator><%-- </wiki:AttachmentsIterator> --%>

  </table>
  </div></div>

</wiki:PageType>

</wiki:PageExists>


<wiki:NoSuchPage>
  <fmt:message key="common.nopage">
    <fmt:param><wiki:EditLink><fmt:message key="common.createit"/></wiki:EditLink></fmt:param>
  </fmt:message>
</wiki:NoSuchPage>