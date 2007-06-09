<%@ page import="com.ecyrd.jspwiki.tags.InsertDiffTag" %>
<%@ page import="com.ecyrd.jspwiki.*" %>
<%@ page import="java.util.*" %>
<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<fmt:setBundle basename="templates.default"/>

<% 
  WikiContext c = WikiContext.findContext( pageContext );  
  List history = c.getEngine().getVersionHistory(c.getPage().getName());
  pageContext.setAttribute( "history", history );
  pageContext.setAttribute( "diffprovider", c.getEngine().getVariable(c,"jspwiki.diffProvider"));
 %>

<wiki:TabbedSection defaultTab="diffcontent">
  <wiki:Tab id="pagecontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext,"view.tab")%>" accesskey="v" 
	       url="<%=c.getURL(WikiContext.VIEW, c.getPage().getName())%>">
  </wiki:Tab>

  <wiki:Tab id="info" title='<%=LocaleSupport.getLocalizedMessage(pageContext, "info.tab")%>'
           url="<%=c.getURL(WikiContext.INFO, c.getPage().getName())%>"
           accesskey="i" >
  </wiki:Tab>
    

<wiki:Tab id="diffcontent" title="<%=LocaleSupport.getLocalizedMessage(pageContext, "diff.tab")%>">

<wiki:PageExists>

  <div class="diffnote">
    <form action="<wiki:Link jsp='Diff.jsp'format='url' />" 
          method="get" accept-charset="UTF-8">

       <input type="hidden" name="page" value="<wiki:Variable var='pagename' />" />
       <fmt:message key="diff.difference">
         <fmt:param>
           <select id="r1" name="r1" onchange="this.form.submit();" >
           <c:forEach items="${history}" var="i">
             <option value="<c:out value='${i.version}'/>" <c:if test="${i.version == param.r1}">selected="selected"</c:if> ><c:out value="${i.version}"/></option>
           </c:forEach>
           </select>
         </fmt:param>
         <fmt:param>
           <select id="r2" name="r2" onchange="this.form.submit();" >
           <c:forEach items="${history}" var="i">
             <option value="<c:out value='${i.version}'/>" <c:if test="${i.version == param.r2}">selected="selected"</c:if> ><c:out value="${i.version}"/></option>
           </c:forEach>
           </select>
         </fmt:param>
       </fmt:message>

       <c:if test='${diffprovider eq "ContextualDiffProvider"}' >
         &nbsp;&nbsp;&nbsp;&nbsp;
         <a href="#change-1" title="<fmt:message key='diff.gotofirst.title'/>" class="diff-nextprev" >
           <fmt:message key="diff.gotofirst"/>
         </a>&raquo;&raquo;
       </c:if>

    </form>
  </div>

  <div class="diffbody">
    <wiki:InsertDiff><i><fmt:message key="diff.nodiff"/></i></wiki:InsertDiff> 
  </div>
  
</wiki:PageExists>
    
<wiki:NoSuchPage>
    <p>
    <fmt:message key="common.nopage">
       <fmt:param>
          <wiki:EditLink><fmt:message key="common.createit"/></wiki:EditLink>
       </fmt:param>
    </fmt:message>
    </p>
</wiki:NoSuchPage>

</wiki:Tab>
</wiki:TabbedSection>