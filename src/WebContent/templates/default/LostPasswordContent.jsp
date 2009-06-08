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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s" %>
<%@ page import="javax.servlet.jsp.jstl.fmt.*" %>
<wiki:TabbedSection defaultTab="lostpassword">

<%-- Login tab --%>
<wiki:UserCheck status="notauthenticated">
<wiki:Tab id="logincontent" titleKey="login.tab" url="Login.jsp" />

<%-- Lost password tab --%>
<wiki:Tab id="lostpassword" titleKey="login.lostpw.tab">

<div class="center">
<s:form beanclass="org.apache.wiki.action.LoginActionBean" id="lostpw" class="wikiform" acceptcharset="UTF-8">
  <s:param name="tab" value="lostpassword" />
  <h3><fmt:message key="login.lostpw.heading" /></h3>

  <c:choose>
  <c:when test="${passwordreset == 'done' }">
      <wiki:Messages div="information" topic="resetpw" prefix="" />
      <p>
        <fmt:message key="login.lostpw.reset.login">
          <fmt:param><a href="<wiki:Link jsp='Login.action' />"><fmt:message key="login.lostpw.reset.clickhere" /></a></fmt:param>
        </fmt:message>
      </p>
  </c:when>
  <c:otherwise>

  <div class="formhelp"><fmt:message key="login.lostpw.help"></fmt:message></div>

  <table>
    <c:if test="${param.tab eq 'lostpassword'}">
    <tr>
      <td colspan="2" class="formhelp">
        <wiki:Messages div="error" topic="resetpw" prefix='<%=LocaleSupport.getLocalizedMessage(pageContext,"login.errorprefix")%>' />
        <wiki:Messages div="information" topic="resetpwok" />
      </td>
    </tr>
    </c:if>
    <tr>
      <td><s:label for="email" /></td>
      <td><s:text size="24" name="email" id="email" /></td>
    </tr>
    <tr>
      <td>&nbsp;</td>
      <td>
        <s:submit name="resetPassword" />
      </td>
    </tr>
  </table>

  <div class="formhelp">
    <fmt:message key="login.invite" />
    <a href="#" onclick="$('menu-logincontent').fireEvent('click');" title="<fmt:message key='login.title' />">
      <fmt:message key="login.heading.login"><fmt:param><wiki:Variable var="applicationname" /></fmt:param></fmt:message>
    </a>
  </div>
  <div class="formhelp">
    <fmt:message key="login.nopassword" />
    <%--<a href="UserPreferences.jsp?tab=profile">--%>
    <a href="#" onclick="$('menu-profile').fireEvent('click');" title="<fmt:message key='login.registernow.title' />">
      <fmt:message key="login.registernow">
        <fmt:param><wiki:Variable var="applicationname" /></fmt:param>
      </fmt:message>
    </a>
  </div>

  </c:otherwise>
  </c:choose>

</s:form>
</div>

</wiki:Tab>
</wiki:UserCheck>

<%-- Register new user profile tab --%>
<wiki:Permission permission='editProfile'>
  <wiki:Tab id="profile" titleKey="login.register.tab" url="CreateProfile.jsp" />
</wiki:Permission>

<%-- Help tab --%>
<wiki:Tab id="loginhelp" titleKey="login.tab.help">
  <wiki:InsertPage page="LoginHelp" />
  <wiki:NoSuchPage page="LoginHelp">
  <div class="error">
    <fmt:message key="login.loginhelpmissing">
       <fmt:param><wiki:EditLink page="LoginHelp">LoginHelp</wiki:EditLink></fmt:param>
    </fmt:message>
  </div>
  </wiki:NoSuchPage>
</wiki:Tab>

</wiki:TabbedSection>
