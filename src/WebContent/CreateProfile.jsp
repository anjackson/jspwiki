<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="stripes" %>
<stripes:useActionBean beanclass="org.apache.wiki.action.UserProfileActionBean" event="create" id="wikiActionBean" />
<stripes:layout-render name="/templates/default/DefaultLayout.jsp">
  <stripes:layout-component name="content">
    <jsp:include page="/templates/default/CreateProfileContent.jsp" />
  </stripes:layout-component>
</stripes:layout-render>
