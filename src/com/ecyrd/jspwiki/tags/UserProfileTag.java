/* 
 JSPWiki - a JSP-based WikiWiki clone.

 Copyright (C) 2001-2002 Janne Jalkanen (Janne.Jalkanen@iki.fi)

 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU Lesser General Public License as published by
 the Free Software Foundation; either version 2.1 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.ecyrd.jspwiki.tags;

import java.io.IOException;

import com.ecyrd.jspwiki.auth.UserManager;
import com.ecyrd.jspwiki.auth.WikiSecurityException;
import com.ecyrd.jspwiki.auth.user.UserProfile;

/**
 * <p>
 * Returns user profile attributes, or empty strings if the user has not been
 * validated. This tag has a single attribute, "property." 
 * The <code>property</code> attribute may contain one of the following 
 * case-insensitive values:
 * </p>
 * <ul>
 * <code>created</code> - creation date</li>
 * <code>email</code> - user's e-mail address</li>
 * <code>fullname</code> - user's full name</li>
 * <code>loginname</code> - user's login name. If the current user does not have
 * a profile, the user's login principal (such as one provided by a container
 * login module, user cookie, or anonyous IP address), will supply the login 
 * name property</li>
 * <code>wikiname</code> - user's wiki name</li>
 * <code>modified</code> - last modification date</li>
 * <code>exists</code> - evaluates the body of the tag if user's profile exists
 * in the user database
 * <code>new</code> - evaluates the body of the tag if user's profile does not
 * exist in the user database
 * </ul>
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-08-03 04:02:57 $
 * @since 2.3
 */
public class UserProfileTag extends WikiTagBase
{
    public static final String BLANK = "(not set)";
    
    private static final String CREATED   = "created";

    private static final String EMAIL     = "email";

    private static final String EXISTS    = "exists";
    
    private static final String FULLNAME  = "fullname";
    
    private static final String LOGINNAME = "loginname";
        
    private static final String MODIFIED  = "modified";
    
    private static final String NEW       = "new";
    
    private static final String WIKINAME  = "wikiname";
    
    private String             m_prop;
    
    public final int doWikiStartTag() throws IOException, WikiSecurityException
    {
        UserManager manager = m_wikiContext.getEngine().getUserManager();
        UserProfile profile = manager.getUserProfile( m_wikiContext );
        String result = null;
        
        if ( EXISTS.equals( m_prop ) )
        {
            return profile.isNew() ? SKIP_BODY : EVAL_BODY_INCLUDE;
        }
        else if ( NEW.equals( m_prop ) )
        {
            return profile.isNew() ? EVAL_BODY_INCLUDE : SKIP_BODY;
        }

        else if ( CREATED.equals( m_prop ) && profile.getCreated() != null )
        {
            result = profile.getCreated().toString();
        }
        else if ( EMAIL.equals( m_prop ) )
        {
            result = profile.getEmail();
        }
        else if ( FULLNAME.equals( m_prop ) )
        {
            result = profile.getFullname();
        }
        else if ( LOGINNAME.equals( m_prop ) )
        {
            result = profile.getLoginName();
        }
        else if ( MODIFIED.equals( m_prop ) && profile.getLastModified() != null )
        {
            result = profile.getLastModified().toString();
        }
        else if ( WIKINAME.equals( m_prop ) )
        {
            result = profile.getWikiName();
        }
        if ( result != null )
        {
            pageContext.getOut().print( result );
        }
        return SKIP_BODY;
    }

    public void setProperty( String property )
    {
        m_prop = property.toLowerCase().trim();
    }
}