<%@ taglib uri="/WEB-INF/jspwiki.tld" prefix="wiki" %>
<%@ taglib uri="http://java.sun.com/jstl/fmt" prefix="fmt" %>
<fmt:setBundle basename="templates.DefaultResources"/>

   <div class="error">
      <fmt:message key="conflict.oops"/>
   </div>

      <p><font color="#0000FF"><fmt:message key="conflict.modified"/></font></p>

      <hr />

      <tt>
        <%=pageContext.getAttribute("conflicttext",PageContext.REQUEST_SCOPE)%>
      </tt>      

      <hr />

      <p><font color="#0000FF"><fmt:message key="conflict.yourtext"/></font></p>

      <tt>
        <%=pageContext.getAttribute("usertext",PageContext.REQUEST_SCOPE)%>
      </tt>

      <hr />

      <p>
       <i><fmt:message key="conflict.goedit">
       <fmt:param><wiki:EditLink><wiki:PageName /></wiki:EditLink></fmt:param>
       </fmt:message></i>
      </p>
