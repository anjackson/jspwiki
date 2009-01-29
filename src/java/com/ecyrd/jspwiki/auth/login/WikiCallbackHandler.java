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
package com.ecyrd.jspwiki.auth.login;

import java.io.IOException;
import java.util.Locale;

import javax.security.auth.callback.*;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 * Handles logins made from inside the wiki application, rather than via the web
 * container. This handler is instantiated in
 * {@link com.ecyrd.jspwiki.auth.AuthenticationManager#login(WikiSession, String, String)}.
 * If container-managed authentication is used, the
 * {@link WebContainerCallbackHandler} is used instead. This callback handler is
 * designed to be used with {@link UserDatabaseLoginModule}.
 * @author Andrew Jaquith
 * @since 2.3
 */
public class WikiCallbackHandler implements CallbackHandler
{
    private static final Logger log = LoggerFactory.getLogger(WikiCallbackHandler.class);

    private final WikiEngine m_engine;

    private final String       m_password;

    private final String       m_username;
    
    private final Locale      m_locale;

    /**
     *  Create a new callback handler.
     *  
     *  @param engine the WikiEngine for this wiki
     *  @param locale the Locale to use, for localizing messages
     *  @param username the username
     *  @param password the password
     */
    public WikiCallbackHandler( WikiEngine engine, Locale locale, String username, String password )
    {
        m_engine = engine;
        m_locale = locale;
        m_username = username;
        m_password = password;
    }

    /**
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     * 
     * {@inheritDoc}
     */
    public void handle( Callback[] callbacks ) throws IOException, UnsupportedCallbackException
    {
        for( int i = 0; i < callbacks.length; i++ )
        {
            Callback callback = callbacks[i];
            if ( callback instanceof WikiEngineCallback )
            {
                ( (WikiEngineCallback) callback ).setEngine( m_engine );
            }
            else if ( callback instanceof NameCallback )
            {
                ( (NameCallback) callback ).setName( m_username );
            }
            else if ( callback instanceof PasswordCallback )
            {
                ( (PasswordCallback) callback ).setPassword( m_password.toCharArray() );
            }
            else if ( callback instanceof LocaleCallback )
            {
                ( (LocaleCallback) callback ).setLocale( m_locale );
            }
            else if( callbacks[i] instanceof TextOutputCallback )
            {
                TextOutputCallback textOutputCb = (TextOutputCallback) callbacks[i];
                String loginResult = textOutputCb.getMessage();
                if(  textOutputCb.getMessageType() == TextOutputCallback.ERROR )
                {
                    log.error( loginResult );
                    throw new IOException( loginResult );
                }

                log.info( loginResult );
            }
            else
            {
                throw new UnsupportedCallbackException( callback );
            }
        }
    }
}
