<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ page import="com.ecyrd.jspwiki.ui.EditorManager" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<fmt:setBundle basename="templates.DefaultResources"/>

<%-- Inserts page content for preview. --%>

<div class="information">
   <fmt:message key="preview.info"/>
</div>

<div class="previewcontent">
   <wiki:Translate><%=EditorManager.getEditedText(pageContext)%></wiki:Translate>
</div>

<div class="information">
   <fmt:message key="preview.info"/>
</div>

<wiki:Editor/>