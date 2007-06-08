<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.default"/>
<%
  WikiContext c = WikiContext.findContext( pageContext );
  int attCount = c.getEngine().getAttachmentManager().listAttachments(c.getPage()).size();
  String attTitle = LocaleSupport.getLocalizedMessage(pageContext, "attach.tab");
  if( attCount != 0 ) attTitle += " (" + attCount + ")";
%>
  
<wiki:TabbedSection>  
<wiki:Tab id="editcontent" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.edit")%>' >
  
  <wiki:CheckLock mode="locked" id="lock">
    <div class="error">
      <fmt:message key="edit.locked">
        <fmt:param><c:out value="${lock.locker}"/></fmt:param>
        <fmt:param><c:out value="${lock.timeLeft}"/></fmt:param>
      </fmt:message>
    </div>
  </wiki:CheckLock>
  
  <wiki:CheckVersion mode="notlatest">
    <div class="warning">
      <fmt:message key="edit.restoring">
        <fmt:param><wiki:PageVersion/></fmt:param>
      </fmt:message>
    </div>
  </wiki:CheckVersion>
    
  <div id="editorbar">  
    <fmt:message key="edit.chooseeditor"/>
    <select onchange="location.href=this.value">
      <wiki:EditorIterator id="editor">
        <option <%=editor.isSelected()%> value="<%=editor.getURL()%>"><%=editor.getName()%></option>
      </wiki:EditorIterator>
    </select>
  </div>

  <wiki:Editor />
    
</wiki:Tab>
  
<wiki:Tab id="attach" title="<%= attTitle %>" accesskey="a">
  <wiki:Include page="AttachmentTab.jsp"/>
</wiki:Tab>
  
<wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
         url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
         accesskey="i" >
</wiki:Tab>
    
<wiki:Tab id="edithelp" title='<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.help")%>' >
  <wiki:InsertPage page="EditPageHelp" />
  <wiki:NoSuchPage page="EditPageHelp">
    <div class="error">
      <fmt:message key="comment.edithelpmissing">
        <fmt:param><wiki:EditLink page="EditPageHelp">EditPageHelp</wiki:EditLink></fmt:param>
      </fmt:message>
    </div>
  </wiki:NoSuchPage>  
</wiki:Tab>

<%-- include all find & replace help in help tab  
  <wiki:Tab id="searchbarhelp" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"edit.tab.findreplacehelp")%>">
    <wiki:InsertPage page="EditFindAndReplaceHelp" />
  </wiki:Tab>
--%>
  
</wiki:TabbedSection>
