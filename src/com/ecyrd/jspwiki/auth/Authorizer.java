package com.ecyrd.jspwiki.auth;

import java.security.Principal;

import javax.security.auth.Subject;

import com.ecyrd.jspwiki.WikiContext;

/**
 * Interface for service providers of authorization information.
 * @author Andrew Jaquith
 * @version $Revision: 1.3 $ $Date: 2005-08-07 22:06:09 $
 * @since 2.3
 */
public interface Authorizer
{

    /**
     * Looks up and returns a role Principal matching a given String.
     * If a matching role cannot be found, this method returns <code>null</code>.
     * Note that it may not be feasible for an Authorizer implementation
     * to return a role Principal.
     * @param role the name of the role to retrieve
     * @return the role Principal
     */
    public Principal findRole( String role );

    /**
     * Determines whether the user represented by a supplied Subject is in a
     * particular role. This method takes three parameters. Context may be
     * <code>null</code>; however, if a Authorizer implementation requires it (<em>e.g.</em>,
     * {@link com.ecyrd.jspwiki.auth.authorize.WebContainerAuthorizer}), this
     * method must return <code>false</code>.
     * @param context the current WikiContext
     * @param subject the current Subject
     * @param role the role to check
     * @return <code>true</code> if the user is considered to be in the role,
     *         <code>false</code> otherwise
     */
    public boolean isUserInRole( WikiContext context, Subject subject, Principal role );

}