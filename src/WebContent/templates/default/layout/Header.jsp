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
<div id="header">

  <div class="titlebox"><wiki:InsertPage page="TitleBox" /></div>

  <div class="applicationlogo" >
    <c:set var="frontPageTitle"><fmt:message key='actions.home.title' ><fmt:param><c:out value='${wikiEngine.frontPage}' /></fmt:param></fmt:message></c:set>
    <s:link beanclass="org.apache.wiki.action.ViewActionBean" title="${frontPageTitle}"><fmt:message key="actions.home" /></s:link>
  </div>

  <div class="companylogo"></div>

  <jsp:include page="${templates['layout/UserBox.jsp']}" />

  <div class="pagename"><wiki:PageName/></div>

  <div class="searchbox">
    <jsp:include page="${templates['layout/SearchBox.jsp']}" />
  </div>

  <div class="breadcrumbs"><fmt:message key="header.yourtrail" /><wiki:Breadcrumbs/></div>

</div>