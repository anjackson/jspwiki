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
<%@ taglib uri="http://jakarta.apache.org/jspwiki.tld" prefix="wiki"%>
<%@ page import="org.apache.wiki.*"%>
<%@ page import="org.apache.wiki.auth.AuthenticationManager"%>
<%@ page import="org.apache.wiki.log.Logger"%>
<%@ page import="org.apache.wiki.log.LoggerFactory"%>
<%@ page import="java.util.ResourceBundle"%>
<%@ page import="java.text.MessageFormat"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s"%>
<%@ page errorPage="/Error.jsp" %>
<s:useActionBean beanclass="org.apache.wiki.action.InstallActionBean"
    event="install" id="wikiActionBean" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" 
   "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
  <head>
    <title><fmt:message key="install.title" /></title>
    <link rel="stylesheet" media="screen, projection" type="text/css" href='<wiki:Link format="url" templatefile="jspwiki.css" />' />
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/mootools.js' />"></script>
    <script type="text/javascript" src="<wiki:Link format='url' jsp='scripts/stripes-support.js' />"></script>
  </head>
  <body class="view">
    <div id="wikibody">
      <div id="page">
        <div id="pagecontent">
        
          <h1><fmt:message key="install.jsp.intro.title" /></h1>
          <p><fmt:message key="install.jsp.intro.p1" /></p>
          <p><fmt:message key="install.jsp.intro.p2" /></p>
          <p><fmt:message key="install.jsp.intro.p3" /></p>
          
          <!-- Any messages or errors? -->
          <div class="instructions"><s:messages /></div>
          <div class="errors"><s:errors globalErrorsOnly="true" /></div>
          
          <div class="formcontainer">
            <s:form beanclass="org.apache.wiki.action.InstallActionBean">
            
              <!-- Admin password, application name, base URL and page directory -->
              <h3><fmt:message key="install.jsp.basics.title" /></h3>
              <div>
                <s:label for="adminPassword" />
                <s:text name="adminPassword" size="20" />
                <s:errors field="adminPassword" />
                <div class="description"><fmt:message key="adminPassword.description" /></div>
              </div>
              
              <div>
                <s:label for="properties.jspwiki.jspwiki_applicationName" />
                <s:text name="properties.jspwiki.jspwiki_applicationName" size="20" />
                <s:errors field="properties.jspwiki.jspwiki_applicationName" />
                <div class="description"><fmt:message key="applicationName.description" /></div>
              </div>
              <div>
                <s:label for="properties.jspwiki.jspwiki_baseURL" />
                <s:text name="properties.jspwiki.jspwiki_baseURL" size="40" />
                <s:errors field="properties.jspwiki.jspwiki_baseURL" />
                <div class="description"><fmt:message key="baseURL.description" /></div>
              </div>
              <div>
                <s:label for="properties.jspwiki.jspwiki_fileSystemProvider_pageDir" />
                <s:text name="properties.jspwiki.jspwiki_fileSystemProvider_pageDir" size="50" />
                <s:errors field="properties.jspwiki.jspwiki_fileSystemProvider_pageDir" />
                <div class="description"><fmt:message key="pageDir.description" /></div>
              </div>
              
              <!-- Advanced settings: logging/work directories -->
              <h3><fmt:message key="install.jsp.adv.settings.title" /></h3>
              <div>
                <s:label for="properties.log4j.log4j_appender_FileLog_File" />
                <s:text name="properties.log4j.log4j_appender_FileLog_File" size="50" />
                <s:errors field="properties.log4j.log4j_appender_FileLog_File" />
                <div class="description"><fmt:message key="logFile.description" /></div>
              </div>
              <div>
                <s:label for="properties.jspwiki.jspwiki_workDir" />
                <s:text name="properties.jspwiki.jspwiki_workDir" size="40" />
                <s:errors field="properties.jspwiki.jspwiki_workDir" />
                <div class="description"><fmt:message key="workDir.description" /></div>
              </div>
            
              <!-- LDAP -->
              <h3><fmt:message key="install.ldap.title" /></h3>
              <p><fmt:message key="install.ldap.description" /></p>
              <div id="security.ldap">
                <div>
                  <s:label for="properties.jspwiki.ldap_config" />
                  <s:select id="ldap.config" name="properties.jspwiki.ldap_config">
                    <s:options-enumeration enum="org.apache.wiki.auth.LdapConfig.Default" label="name" />
                  </s:select>
                  <s:errors field="properties.jspwiki.ldap_config" />
                  <div class="description"><fmt:message key="ldap.config.description" /></div>
                </div>
                <!-- LDAP connection settings and test button -->
                <div>
                  <s:label for="properties.jspwiki.ldap_connectionURL" />
                  <s:text name="properties.jspwiki.ldap_connectionURL" size="40" />
                  <s:errors field="properties.jspwiki.ldap_connectionURL" />
                  <div class="description"><fmt:message key="ldap.connectionURL.description" /></div>
                </div>
                <div>
                  <s:label for="properties.jspwiki.ldap_authentication" />
                  <s:select id="ldap.authentication" name="properties.jspwiki.ldap_authentication">
                    <s:option value="DIGEST-MD5">DIGEST-MD5</s:option>
                    <s:option value="simple">simple</s:option>
                  </s:select>
                  <s:errors field="properties.jspwiki.ldap_authentication" />
                  <div class="description"><fmt:message key="ldap.authentication.description" /></div>
                </div>
                <div>
                  <s:label for="properties.jspwiki.ldap_ssl" />
                  <s:checkbox name="properties.jspwiki.ldap_ssl" />
                  <div class="description"><fmt:message key="ldap.ssl.description" /></div>
                </div>
                <s:button name="testLdapConnection" onclick="Stripes.executeEvent(form, this.name, 'ldapConnResults');" />
                <div class="description" id="ldapConnResults"></div>
                <!-- LDAP authentication settings and test button -->
                <div>
                  <s:label for="properties.jspwiki.ldap_bindDN" />
                  <s:text name="properties.jspwiki.ldap_bindDN" size="40" />
                  <s:errors field="properties.jspwiki.ldap_bindDN" />
                  <div class="description"><fmt:message key="ldap.bindDN.description" /></div>
                </div>
                <div>
                  <s:label for="bindDNpassword" />
                  <s:text name="bindDNpassword" size="20" />
                  <s:errors field="bindDNpassword" />
                  <div class="description"><fmt:message key="ldap.bindDNpassword.description" /></div>
                </div>
                <s:button name="testLdapAuthentication" onclick="Stripes.executeEvent(form, this.name, 'ldapAuthResults');" />
                <div class="description" id="ldapAuthResults"></div>
                <!-- LDAP user database settings and test button -->
                <div>
                  <s:label for="properties.jspwiki.ldap_userBase" />
                  <s:text name="properties.jspwiki.ldap_userBase" size="40" />
                  <s:errors field="properties.jspwiki.ldap_userBase" />
                  <div class="description"><fmt:message key="ldap.userBase.description" /></div>
                </div>
                <s:button name="testLdapUsers" onclick="Stripes.executeEvent(form, this.name, 'ldapUserResults');" />
                <div class="description" id="ldapUserResults"></div>
                <!-- LDAP authorizer settings and test button -->
                <div>
                  <s:label for="properties.jspwiki.ldap_roleBase" />
                  <s:text name="properties.jspwiki.ldap_roleBase" size="40" />
                  <s:errors field="properties.jspwiki.ldap_roleBase" />
                  <div class="description"><fmt:message key="ldap.roleBase.description" /></div>
                </div>
                <s:button name="testLdapRoles" onclick="Stripes.executeEvent(form, this.name, 'ldapRoleResults');" />
                <div class="description" id="ldapRoleResults"></div>
              </div>
              
              <!-- Save the configuration -->
              <p>
                <fmt:message key="install.configure.description">
                  <fmt:param>${wikiActionBean.properties.jspwiki.path}</fmt:param>
                </fmt:message>
              <p>
              <s:submit name="save" />

            </s:form>
          </div>
          
        </div>
      </div>
    </div>
  </body>
</html>
