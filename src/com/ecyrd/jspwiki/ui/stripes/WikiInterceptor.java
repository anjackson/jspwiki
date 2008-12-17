package com.ecyrd.jspwiki.ui.stripes;

import java.lang.reflect.Method;
import java.security.Permission;
import java.util.Map;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.*;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.WikiSession;
import com.ecyrd.jspwiki.action.LoginActionBean;
import com.ecyrd.jspwiki.action.WikiActionBean;
import com.ecyrd.jspwiki.action.WikiContextFactory;
import com.ecyrd.jspwiki.auth.AuthorizationManager;
import com.ecyrd.jspwiki.auth.SessionMonitor;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;

/**
 * <p>
 * Stripes {@link net.sourceforge.stripes.controller.Interceptor} that
 * instantiates the correct WikiContext associated with JSPs, checks for access,
 * and redirects users if necessary. The interceptor executes twice: the first
 * time is after the first lifecycle state, <em>aka</em>
 * {@link  net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
 * but before the second stage.
 * {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution}.
 * The second time the interceptor executes is after the second lifecycle stage,
 * but before the third stage,
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}.
 * See the
 * </p>
 * <p>
 * WikiInterceptor assumes primary responsibility for making JSPWiki objects
 * available to JSPs as variables. In particular, when WikiInterceptor fires
 * during the binding and validation stage, sets the following PageContext
 * attributes, all in {@link PageContext#REQUEST_SCOPE}:
 * </p>
 * <ul>
 * <li><code>wikiEngine</code> - the {@link com.ecyrd.jspwiki.WikiEngine}</li>
 * <li><code>wikiSession</code> - the user's
 * {@link com.ecyrd.jspwiki.WikiSession}</li>
 * <li><code>wikiActionBean</code> - the
 * {@link com.ecyrd.jspwiki.action.WikiActionBean} injected by Stripes</li>
 * <li><code>wikiPage</code> - the {@link com.ecyrd.jspwiki.WikiPage}
 * associated with the WikiActionBean, or the "front page" if the WikiActionBean
 * is not a WikiContext</li>
 * </ul>
 * <p>
 * After the intercept method fires, calling classes can obtain the saved
 * WikiActionBean by calling
 * {@link WikiInterceptor#findActionBean(javax.servlet.ServletRequest)}. This
 * is the recommended method that JSP scriptlet code should use.
 * </p>
 * <p>
 * Because these objects are saved as attributes, they are available to JSPs as
 * the Expression Language variables <code>${wikiEngine}</code>,
 * <code>${wikiSession}</code>, <code>${wikiActionBean}</code> and
 * <code>${wikiPage}</code>.
 * </p>
 * 
 * @author Andrew Jaquith
 */
@Intercepts( { LifecycleStage.ActionBeanResolution, LifecycleStage.BindingAndValidation } )
public class WikiInterceptor implements Interceptor
{
    private static final Logger log = LoggerFactory.getLogger( WikiInterceptor.class );

    /**
     * The PageContext attribute name of the WikiActionBean stored by
     * WikiInterceptor.
     */
    public static final String ATTR_ACTIONBEAN = "wikiActionBean";

    /**
     * Intercepts the Stripes lifecycle stages and dispatches execution to
     * delegate methods {@link #interceptActionBeanResolution(ExecutionContext)}
     * and {@link #interceptBindingAndValidation(ExecutionContext)}, whichever
     * is appropriate.
     * 
     * @param context the current execution context
     * @return a Resolution if the
     *         {@link net.sourceforge.stripes.controller.LifecycleStage#HandlerResolution}
     *         lifecycle stage's normal execution returns one; <code>null</code>
     *         otherwise
     * @throws Exception if the underlying lifcycle stage's execution throws an
     *             Exception
     */
    public Resolution intercept( ExecutionContext context ) throws Exception
    {
        if( LifecycleStage.ActionBeanResolution.equals( context.getLifecycleStage() ) )
        {
            return interceptActionBeanResolution( context );
        }
        else if( LifecycleStage.BindingAndValidation.equals( context.getLifecycleStage() ) )
        {
            return interceptBindingAndValidation( context );
        }
        return null;
    }

    /**
     * After the Stripes
     * {@link net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
     * executes, this method injects the current WikiEngine, WikiSession and
     * WikiActionBean into request scope, and returns <code>null</code>.
     * After the objects are injected, downstream classes like WikiTagBase can
     * use them. The attribute can also be accessed as variables using the JSP
     * Expression Language (example: <code>${wikiPage}</code>).
     * 
     * @param context the execution context
     * @return a Resolution if the
     *         {@link net.sourceforge.stripes.controller.LifecycleStage#ActionBeanResolution}
     *         lifecycle stage's normal execution returns one; <code>null</code>
     *         otherwise
     * @throws Exception if the underlying lifcycle stage's execution throws an
     *             Exception
     */
    protected Resolution interceptActionBeanResolution( ExecutionContext context ) throws Exception
    {
        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // Retrieve the ActionBean, its ActionBeanContext, and HTTP request
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        WikiActionBeanContext actionBeanContext = actionBean.getContext();
        HttpServletRequest request = actionBeanContext.getRequest();

        // Set the WikiSession, if not set yet
        if( actionBeanContext.getWikiSession() == null )
        {
            WikiEngine engine = actionBeanContext.getEngine();
            WikiSession wikiSession = SessionMonitor.getInstance( engine ).find( request.getSession() );
            actionBeanContext.setWikiSession( wikiSession );
        }

        // Stash the ActionBean as request attribute, if not saved yet
        if( request.getAttribute( ATTR_ACTIONBEAN ) == null )
        {
            request.setAttribute( ATTR_ACTIONBEAN, actionBean );
        }

        // Stash the WikiContext, WikiEngine
        WikiContextFactory.saveContext( request, actionBean.getContext() );

        if( log.isDebugEnabled() )
        {
            log.debug( "WikiInterceptor resolved ActionBean: " + actionBean );
        }

        return null;
    }

    /**
     * <p>
     * Intercepts the
     * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
     * lifecycle stage and checks for proper access to the current ActionBean
     * and target event. The access-checking logic runs after after the rest of
     * the BindingAndValidation processing logic does, after which point Stripes
     * has already discovered the correct ActionBean, and bound and validated
     * its request parameters.
     * </p>
     * <p>
     * To determine if the user is allowed to access the target event method,
     * the method is examined to see if contains a
     * {@link com.ecyrd.jspwiki.ui.stripes.HandlerPermission}) annotation that
     * specifies the required {@link java.security.Permission}. If the user
     * does not possess the Permission -- that is,
     * {@link com.ecyrd.jspwiki.auth.AuthorizationManager#checkPermission(WikiSession, Permission)}
     * returns <code>false</code> -- this method returns a RedirectResolution
     * to the login page, with all current parameters appended.
     * </p>
     * 
     * @param context the execution context
     * @return a Resolution if the
     *         {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
     *         lifecycle stage's normal execution returns one; <code>null</code>
     *         otherwise
     * @throws Exception if the underlying lifcycle stage's execution throws an
     *             Exception
     */
    protected Resolution interceptBindingAndValidation( ExecutionContext context ) throws Exception
    {
        // Stash the WikiActionBean as a PageContext attribute
        WikiActionBean actionBean = (WikiActionBean) context.getActionBean();
        PageContext pageContext = DispatcherHelper.getPageContext();
        if( pageContext != null )
        {
            pageContext.setAttribute( ATTR_ACTIONBEAN, actionBean );
        }

        // Did the handler resolution stage return a Resolution? If so, bail.
        Resolution r = context.proceed();
        if( r != null )
        {
            return r;
        }

        // Get the event handler method
        Method handler = context.getHandler();

        // Make sure we set the WikiContext request context, while we're at it
        Map<Method, HandlerInfo> eventinfos = HandlerInfo.getHandlerInfoCollection( actionBean.getClass() );
        HandlerInfo eventInfo = eventinfos.get( handler );
        if( eventInfo != null )
        {
            String requestContext = eventInfo.getRequestContext();
            actionBean.getContext().setRequestContext( requestContext );
        }

        // Does the event handler have a required permission?
        boolean allowed = true;
        if( eventInfo != null )
        {
            Permission requiredPermission = eventInfo.getPermission( actionBean );
            if( requiredPermission != null )
            {
                WikiEngine engine = actionBean.getContext().getEngine();
                AuthorizationManager mgr = engine.getAuthorizationManager();
                WikiSession wikiSession = actionBean.getContext().getWikiSession();
                allowed = mgr.checkPermission( wikiSession, requiredPermission );
            }
        }

        // If not allowed, redirect to login page with all parameters intact;
        // otherwise proceed
        if( !allowed )
        {
            r = new RedirectResolution( LoginActionBean.class );
            ((RedirectResolution) r).includeRequestParameters( true );
            if( log.isDebugEnabled() )
            {
                log.debug( "WikiInterceptor rejected access to ActionBean=" + actionBean.getClass().getCanonicalName()
                           + ", method=" + handler.getName() );
            }
            return r;
        }

        return null;
    }

    /**
     * Returns the WikiActionBean associated with the current
     * {@link javax.servlet.jsp.PageContext}, which may have been previously stashed by
     * {@link #interceptBindingAndValidation(ExecutionContext)}. Note that each
     * PageContext can contain its own ActionBean. The ActionBean will be
     * retrieved from page-scope attribute {@link WikiInterceptor#ATTR_ACTIONBEAN}.
     * If the WikiActionBean cannot be obtained as a page-scope attribute, the
     * request scope will be tried also.
     * 
     * @param pageContext the page context
     * @return the WikiActionBean
     * @throws IllegalStateException if the WikiActionBean was not found in the
     *             page context or 
     */
    public static WikiActionBean findActionBean( PageContext pageContext )
    {
        WikiActionBean bean = (WikiActionBean) pageContext.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        if( bean == null )
        {
            bean = findActionBean( pageContext.getRequest() );
            if ( bean == null )
            {
                log.debug( "WikiActionBean not found under page context attribute '" + WikiInterceptor.ATTR_ACTIONBEAN
                           + "'! Something failed to stash it..." );
            }
        }
        return bean;
    }

    /**
     * Returns the WikiActionBean associated with the current
     * {@link javax.servlet.http.HttpServletRequest}, which was previously
     * stashed by {@link #interceptActionBeanResolution(ExecutionContext)}.
     * Only the first ActionBean on a JSP will be stashed as a request-level
     * attribute. The ActionBean will be retrieved from attribute
     * {@link WikiInterceptor#ATTR_ACTIONBEAN}.
     * 
     * @param request the HTTP request
     * @return the WikiActionBean
     * @throws IllegalStateException if the WikiActionBean was not found in the
     *             request scope
     */
    public static WikiActionBean findActionBean( ServletRequest request )
    {
        WikiActionBean bean = (WikiActionBean) request.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        if( bean == null )
        {
            log.debug( "WikiActionBean not found under request attribute '" + WikiInterceptor.ATTR_ACTIONBEAN
                       + "'! Something failed to stash it..." );
            bean = (WikiActionBean) request.getAttribute( WikiInterceptor.ATTR_ACTIONBEAN );
        }
        return bean;
    }

}
