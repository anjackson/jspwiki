<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.ViewActionBean" event="view" executeResolution="true" id="wikiActionBean" />
<stripes:layout-render name="/templates/default/ViewLayout.jsp">
  <stripes:layout-component name="content">
    <jsp:include page="/templates/default/PageContent.jsp" />
  </stripes:layout-component>
</stripes:layout-render>
