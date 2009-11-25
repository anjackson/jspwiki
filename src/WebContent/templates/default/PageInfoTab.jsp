<%--
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
--%>
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki" %>
<%@ page import="org.apache.wiki.*" %>
<%@ page import="org.apache.wiki.attachment.*" %>
<%@ page import="org.apache.wiki.i18n.InternationalizationManager" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="org.apache.wiki.action.WikiContextFactory" %>
<%@ page import="org.apache.wiki.util.TextUtil" %>
<%@ page import="org.apache.wiki.api.WikiPage" %>
<%
  WikiContext c = WikiContextFactory.findContext( pageContext );
  WikiPage wikiPage = c.getPage();
  int attCount = c.getEngine().getAttachmentManager().listAttachments( c.getPage() ).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";

  String creationAuthor ="";

  //FIXME -- seems not to work correctly for attachments !!
  WikiPage firstPage = c.getEngine().getPage( wikiPage.getName(), 1 );
  if( firstPage != null )
  {
    creationAuthor = firstPage.getAuthor();

    if( creationAuthor != null && creationAuthor.length() > 0 )
    {
      creationAuthor = TextUtil.replaceEntities(creationAuthor);
    }
    else
    {
      creationAuthor = c.getBundle( InternationalizationManager.CORE_BUNDLE ).getString( "common.unknownauthor" );
    }
  }

  int itemcount = 0;  //number of page versions
  try
  {
    itemcount = wikiPage.getVersion(); /* highest version */
  }
  catch( Exception  e )  { /* dont care */ }

  int pagesize = 20;
  int startitem = itemcount-1; /* itemcount==1-20 -> startitem=0-19 ... */

  String parm_start = (String)request.getParameter( "start" );
  if( parm_start != null ) startitem = Integer.parseInt( parm_start ) ;

  /* round to start of block: 0-19 becomes 0; 20-39 becomes 20 ... */
  if( startitem > -1 ) startitem = ((startitem)/pagesize) * pagesize;

  /* startitem drives the pagination logic */
  /* startitem=-1:show all; startitem=0:show block 1-20; startitem=20:block 21-40 ... */
%>
<wiki:TabbedSection defaultTab="info">

  <wiki:Tab id="pagecontent" titleKey="view.tab" accesskey="v" url="Wiki.jsp?page=${wikiActionBean.page.name}"/>

  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a" url="Attachments.jsp?page=${wikiActionBean.page.name}" />

  <wiki:Tab id="info" titleKey="info.tab" accesskey="i">
    <p>
    <fmt:message key='info.lastmodified'>
      <fmt:param><wiki:PageVersion>1</wiki:PageVersion></fmt:param>
      <fmt:param>
        <a href="<wiki:DiffLink format='url' version='latest' newVersion='previous' />" title="<fmt:message key='info.pagediff.title' />">
          <fmt:formatDate value="<%= wikiPage.getLastModified() %>" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
        </a>
      </fmt:param>
      <fmt:param><wiki:Author/></fmt:param>
    </fmt:message>
    <wiki:RSSImageLink mode="wiki" />
    </p>

    <wiki:CheckVersion mode="notfirst">
    <p>
    <fmt:message key='info.createdon'>
      <fmt:param>
        <wiki:Link version="1">
          <fmt:formatDate value="<%= firstPage.getLastModified() %>" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" />
        </wiki:Link>
      </fmt:param>
      <fmt:param><%= creationAuthor %></fmt:param>
    </fmt:message>
    </p>
    </wiki:CheckVersion>

    <wiki:Permission permission="rename">
      <div class="formhelp">
        <wiki:Messages div="error" topic="rename" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"prefs.errorprefix.rename")%>' />
      </div>

      <s:form beanclass="org.apache.wiki.action.RenameActionBean" class="wikiform" id="renameform" method="post" acceptcharset="UTF-8" >
        <p>
          <s:hidden name="page" />
          <s:submit name="rename" />
          <s:text name="renameTo" size="40" />
          &nbsp;
          <s:checkbox name="changeReferences" />
          <fmt:message key="info.updatereferrers" />
        </p>
      </s:form>
    </wiki:Permission>
    <wiki:Permission permission="!rename">
      <p><fmt:message key="info.rename.permission" /></p>
    </wiki:Permission>

    <wiki:Permission permission="delete">
      <s:form beanclass="org.apache.wiki.action.DeleteActionBean" class="wikiform" id="deleteForm" method="post" acceptcharset="UTF-8">
        <p><s:submit name="delete" id="delete-all" /></p>
      </s:form>
    </wiki:Permission>
    <wiki:Permission permission="!delete">
      <p><fmt:message key="info.delete.permission" /></p>
    </wiki:Permission>

    <div class='line'>
      <div class="collapsebox-closed unit size1of2" id="incomingLinks">
        <h4><fmt:message key="info.tab.incoming" /></h4>
        <wiki:LinkTo><wiki:PageName/></wiki:LinkTo>
        <wiki:Plugin plugin="ReferringPagesPlugin" args="before='*' after='\n' " />
      </div>

      <div class="collapsebox-closed lastUnit size1of2 " id="outgoingLinks">
        <h4><fmt:message key="info.tab.outgoing" /></h4>
        <wiki:Plugin plugin="ReferredPagesPlugin" args="depth='1' type='local'" />
      </div>
    </div>

    <%-- DIFF section --%>
    <wiki:CheckRequestContext context='diff'>
      <s:form beanclass="org.apache.wiki.action.DiffActionBean" method="get" acceptcharset="UTF-8">
        <div class="collapsebox" id="diffcontent">
          <h4>
            <s:hidden name="page" value="${wikiActionBean.page.name}" />
            <fmt:message key="diff.difference">
              <fmt:param>
                <s:select id="r1" name="r1" value="${wikiActionBean.r1}" onchange="this.form.submit();">
                  <s:options-collection collection="${wikiActionBean.history}" value="version" label="version" />
                </s:select>
              </fmt:param>
              <fmt:param>
                <s:select id="r2" name="r2" value="${wikiActionBean.r2}" onchange="this.form.submit();">
                  <s:options-collection collection="${wikiActionBean.history}" value="version" label="version" />
                </s:select>
              </fmt:param>
            </fmt:message>
          </h4>
        
          <c:if test='${diffprovider eq "ContextualDiffProvider"}'>
            <div class="diffnote">
              <a href="#change-1" title="<fmt:message key='diff.gotofirst.title' />" class="diff-nextprev">
                 <fmt:message key="diff.gotofirst" />
              </a>&raquo;&raquo;
            </div>
          </c:if>
        
          <div class="diffbody">
            <wiki:InsertDiff><i><fmt:message key="diff.nodiff" /></i></wiki:InsertDiff> 
          </div>
        </div>
      </s:form>
    </wiki:CheckRequestContext>
    <%-- DIFF section --%>

    <wiki:CheckVersion mode="first"><fmt:message key="info.noversions" /></wiki:CheckVersion>
    <wiki:CheckVersion mode="notfirst">
      <%-- if( itemcount > 1 ) { --%>

      <wiki:SetPagination start="<%=startitem%>" total="<%=itemcount%>" pagesize="<%=pagesize%>" maxlinks="9" fmtkey="info.pagination" href='<%=c.getURL(WikiContext.INFO, c.getPage().getName(), "start=%s")%>' />

      <div class="zebra-table sortable table-filter">
      <table class="wikitable">
        <tr>
          <th><fmt:message key="info.version" /></th>
          <th><fmt:message key="info.date" /></th>
          <th><fmt:message key="info.size" /></th>
          <th><fmt:message key="info.author" /></th>
          <th><fmt:message key="info.changes" /></th>
          <th class='changenote'><fmt:message key="info.changenote" /></th>
        </tr>

        <wiki:HistoryIterator id="currentPage">
        <% if( ( startitem == -1 ) ||
                (  ( currentPage.getVersion() > startitem )
                && ( currentPage.getVersion() <= startitem + pagesize ) ) )
            {
          %>
        <tr>
          <td>
            <wiki:LinkTo version="<%=Integer.toString(currentPage.getVersion())%>">
              <wiki:PageVersion/>
            </wiki:LinkTo>
          </td>

          <td><fmt:formatDate value="<%= currentPage.getLastModified() %>" pattern="${prefs.TimeFormat}" timeZone="${prefs.TimeZone}" /></td>
          <td style="white-space:nowrap;text-align:right;">
            <c:set var="ff"><wiki:PageSize/></c:set>
            <fmt:formatNumber value='${ff/1000}' maxFractionDigits='3' minFractionDigits='1' />&nbsp;<fmt:message key="info.kilobytes" />
          </td>

          <td><wiki:Author/></td>

          <td>
            <wiki:CheckVersion mode="notfirst">
              <wiki:DiffLink version="current" newVersion="previous"><fmt:message key="info.difftoprev" /></wiki:DiffLink>
              <wiki:CheckVersion mode="notlatest"> | </wiki:CheckVersion>
            </wiki:CheckVersion>
            <wiki:CheckVersion mode="notlatest">
              <wiki:DiffLink version="latest" newVersion="current"><fmt:message key="info.difftolast" /></wiki:DiffLink>
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
      ${pagination}
      <%-- } /* itemcount > 1 */ --%>
    </wiki:CheckVersion>
  </wiki:Tab>

</wiki:TabbedSection>
