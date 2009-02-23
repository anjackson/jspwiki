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
<%
  int MAXATTACHNAMELENGTH = 30;
  String progressId = c.getEngine().getProgressManager().getNewProgressIdentifier();

  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>
<wiki:TabbedSection defaultTab="info">

  <wiki:Tab id="pagecontent" titleKey="view.tab" accesskey="v" url="Wiki.jsp?page=${wikiActionBean.page.name}"/>
      
  <wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a" url="Attachments.jsp?page=${wikiActionBean.page.name}" />
  
  <wiki:Tab id="info" titleKey="info.tab" accesskey="i">
    <h3><fmt:message key="info.uploadnew" /></h3>
    <wiki:Permission permission="upload">
      <form action="<wiki:Link jsp='attach' format='url' absolute='true'><wiki:Param name='progressid' value='<%=progressId%>'/></wiki:Link>"
              class="wikiform"
                id="uploadform"
          onsubmit="return Wiki.submitUpload(this, '<%=progressId%>');"
            method="post" accept-charset="<wiki:ContentEncoding/>"
            enctype="multipart/form-data" >
    
        <%-- Do NOT change the order of wikiname and content, otherwise the
            servlet won't find its parts. --%>
    
        <table>
          <tr>
            <td colspan="2"><div class="formhelp"><fmt:message key="info.uploadnew.help" /></div></td>
          </tr>
          <tr>
            <td><label for="content"><fmt:message key="info.uploadnew.filename" /></label></td>
            <td><input type="file" name="content" size="60" /></td>
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
            <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
            <input type="submit" name="upload" value="<fmt:message key='attach.add.submit' />" id="upload" /> <input type="hidden" name="action" value="upload" />
            <input type="hidden" name="nextpage" value="<wiki:PageInfoLink format='url' />" />
                <div id="progressbar"><div class="ajaxprogress"></div></div>
            </td>
          </tr>
        </table>
      </form>
    </wiki:Permission>
    
    <wiki:Permission permission="!upload">
      <div class="formhelp"><fmt:message key="attach.add.permission" /></div>
    </wiki:Permission>
    
    <wiki:Permission permission="delete">
      <h3><fmt:message key="info.deleteattachment"/></h3>
      <s:form beanclass="org.apache.wiki.action.ViewActionBean" class="wikiform" id="deleteForm" method="post" acceptcharset="UTF-8">
        <div>
          <s:submit name="deleteAttachment" id="delete-all" />
        </div>
      </s:form>
    </wiki:Permission>
    
    <%-- FIXME why not add pagination here - no need for large amounts of attach versions on one page --%>
    <h3><fmt:message key='info.attachment.history' /></h3>
    <div class="zebra-table">
      <div class="slimbox-img sortable">
        <table class="wikitable">
          <tr>
            <th><fmt:message key="info.attachment.type" /></th>
            <%--<th><fmt:message key="info.attachment.name"/></th>--%>
            <th><fmt:message key="info.version" /></th>
            <th><fmt:message key="info.size" /></th>
            <th><fmt:message key="info.date" /></th>
            <th><fmt:message key="info.author" /></th>
            <th class='changenote'><fmt:message key="info.changenote" /></th>
          </tr>
      
          <wiki:HistoryIterator id="att"><%-- <wiki:AttachmentsIterator id="att"> --%>
          <%
            String name = att.getName(); //att.getFileName();
            int dot = name.lastIndexOf(".");
            String attachtype = ( dot != -1 ) ? name.substring(dot+1) : "&nbsp;";
      
            String sname = name;
            if( sname.length() > MAXATTACHNAMELENGTH ) sname = sname.substring(0,MAXATTACHNAMELENGTH) + "...";
          %>
          <tr>
            <td>
              <div id="attach-<%= attachtype %>" class="attachtype"><%= attachtype %></div>
            </td>
            <td>
              <a href="<wiki:Link version='<%=Integer.toString(att.getVersion())%>' format='url' />" title="<%= name %>" class="attachment"><wiki:PageVersion /></a>
            </td>
            <td style="white-space:nowrap;text-align:right;">
              <fmt:formatNumber value="<%=Double.toString(att.getSize()/1000.0) %>" groupingUsed="false" maxFractionDigits="1" minFractionDigits="1" />&nbsp;<fmt:message key="info.kilobytes" />
            </td>
      	    <td style="white-space:nowrap;">
      	      <fmt:formatDate value="<%= att.getLastModified() %>" pattern="${prefs.DateFormat}" timeZone="${prefs.TimeZone}" />
      	    </td>
            <td><wiki:Author/></td>
            <td class='changenote'>
            <%
                String changeNote = (String)att.getAttribute(WikiPage.CHANGENOTE);
                if( changeNote != null ) {
                    changeNote = TextUtil.replaceEntities(changeNote);
                %><%=changeNote%><%
                }
            %>
            </td>
          </tr>
          </wiki:HistoryIterator>
        </table>
      </div>
    </div>
  </wiki:Tab>

</wiki:TabbedSection>
