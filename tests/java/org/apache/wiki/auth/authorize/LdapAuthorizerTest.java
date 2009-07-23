/*
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
 */
package org.apache.wiki.auth.authorize;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.wiki.TestEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.Authorizer;

/**
 * @author Andrew Jaquith
 */
public class LdapAuthorizerTest extends TestCase
{
    private Map<String, String> m_options;

    protected void setUp()
    {
        m_options = new HashMap<String, String>();
    }

    /**
     * @see junit.framework.TestCase#setUp()
     */
    protected TestEngine createEngine( Map<String, String> config ) throws Exception
    {
        Properties props = new Properties();
        props.load( TestEngine.findTestProperties() );
        props.putAll( config );
        props.put( AuthorizationManager.PROP_AUTHORIZER, LdapAuthorizer.class.getCanonicalName() );
        TestEngine engine = new TestEngine( props );
        return engine;
    }

    public void testGetRoles() throws Exception
    {
        m_options.put( LdapAuthorizer.PROPERTY_CONNECTION_URL, "ldap://127.0.0.1:4890" );
        m_options.put( LdapAuthorizer.PROPERTY_ROLE_BASE, "ou=roles,dc=jspwiki,dc=org" );
        m_options.put( LdapAuthorizer.PROPERTY_USER_PATTERN, "uid={0},ou=people,dc=jspwiki,dc=org" );
        Authorizer authorizer = createEngine( m_options ).getAuthorizationManager().getAuthorizer();

        // LDAP should return just 2 roles, Admin and Role1
        Principal[] roles = authorizer.getRoles();
        assertEquals( 2, roles.length );
        Role admin = new Role( "Admin" );
        Role role1 = new Role( "Role1" );
        assertTrue( roles[0].equals( admin ) || roles[1].equals( admin ) );
        assertTrue( roles[0].equals( role1 ) || roles[1].equals( role1 ) );
    }

    public void testFindRole() throws Exception
    {
        m_options.put( LdapAuthorizer.PROPERTY_CONNECTION_URL, "ldap://127.0.0.1:4890" );
        m_options.put( LdapAuthorizer.PROPERTY_ROLE_BASE, "ou=roles,dc=jspwiki,dc=org" );
        m_options.put( LdapAuthorizer.PROPERTY_USER_PATTERN, "uid={0},ou=people,dc=jspwiki,dc=org" );
        Authorizer authorizer = createEngine( m_options ).getAuthorizationManager().getAuthorizer();
        
        // We should be able to find roles Admin and Role1
        assertEquals( new Role("Admin"), authorizer.findRole( "Admin" ) );
        assertEquals( new Role("Role1"), authorizer.findRole( "Role1" ) );
        
        // We should not be able to find role Authenticated
        assertNull( null, authorizer.findRole( "Authenticated" ) );
    }

    public void testIsUserInRole() throws Exception
    {
        m_options.put( LdapAuthorizer.PROPERTY_CONNECTION_URL, "ldap://127.0.0.1:4890" );
        m_options.put( LdapAuthorizer.PROPERTY_ROLE_BASE, "ou=roles,dc=jspwiki,dc=org" );
        m_options.put( LdapAuthorizer.PROPERTY_USER_PATTERN, "uid={0},ou=people,dc=jspwiki,dc=org" );
        TestEngine engine = createEngine( m_options );
        Authorizer authorizer = engine.getAuthorizationManager().getAuthorizer();
        Role admin = new Role( "Admin" );
        Role role1 = new Role( "Role1" );
        
        // Janne does not belong to any roles
        WikiSession session = engine.janneSession();
        assertFalse( authorizer.isUserInRole( session, admin ) );
        assertFalse( authorizer.isUserInRole( session, role1 ) );
        
        // The Admin belongs to just the Admin role
        session = engine.adminSession();
        assertTrue( authorizer.isUserInRole( session, admin ) );
        assertFalse( authorizer.isUserInRole( session, role1 ) );
    }

}
