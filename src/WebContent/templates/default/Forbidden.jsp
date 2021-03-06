<!-- 
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
 -->
<%@ taglib uri="http://stripes.sourceforge.net/stripes.tld" prefix="s"%>
<%@ page errorPage="/Error.jsp" %>
<s:layout-render name="${templates['layout/StaticLayout.jsp']}">

  <s:layout-component name="headTitle">
    Forbidden
  </s:layout-component>

  <s:layout-component name="pageTitle">
    Forbidden
  </s:layout-component>

  <s:layout-component name="content">
    <p>
      <strong>Sorry, but you are not allowed to do that.</strong>
    </p>
    <p> 
      Usually we block access to
      something because you do not have the correct privileges (<em>e.g.</em>,
      read, edit, comment) for the page you are looking for. In this particular case,
      it is likely that you are not listed in the page&rsquo;s access control list
      or that your privileges aren&rsquo;t high enough (you want
      to edit, but ACL only allows &lsquo;read&rsquo;).
    </p>
    <p>
      It is also possible that JSPWiki cannot find its security policy, or that
      the policy is not configured correctly. Either of these cases would cause
      JSPWiki to block access, too.
    </p>
    <p><a href=".">Better luck next time.</a></p>
  </s:layout-component>
  
</s:layout-render>
