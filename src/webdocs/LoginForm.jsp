<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="com.ecyrd.jspwiki.action.LoginActionBean" event="view" id="wikiActionBean" />
<stripes:layout-render name="/templates/default/ViewLayout.jsp">
  <stripes:layout-component name="content">
    <jsp:include page="/templates/default/LoginContent.jsp" />
  </stripes:layout-component>
</stripes:layout-render>
