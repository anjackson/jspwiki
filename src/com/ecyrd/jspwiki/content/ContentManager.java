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
package com.ecyrd.jspwiki.content;

import java.util.*;

import javax.jcr.*;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.jspwiki.api.WikiException;
import org.priha.RepositoryManager;
import org.priha.util.ConfigurationException;

import com.ecyrd.jspwiki.*;
import com.ecyrd.jspwiki.event.WikiEventManager;
import com.ecyrd.jspwiki.event.WikiPageEvent;
import com.ecyrd.jspwiki.log.Logger;
import com.ecyrd.jspwiki.log.LoggerFactory;
import com.ecyrd.jspwiki.providers.ProviderException;
import com.ecyrd.jspwiki.util.TextUtil;
import com.ecyrd.jspwiki.util.WikiBackgroundThread;

/**
 *  Provides access to the content repository.
 */
public class ContentManager
{
    protected static final String DEFAULT_SPACE = "Main";
    
    private static final String JCR_DEFAULT_SPACE = "pages/"+DEFAULT_SPACE;

    private static final String JCR_PAGES_NODE = "pages";

    private static final long serialVersionUID = 2L;
    
    /** The property value for setting the amount of time before the page locks expire. 
     *  Value is {@value}.
     */
    public static final String PROP_LOCKEXPIRY   = "jspwiki.lockExpiryTime";
    
    /** The message key for storing the text for the presave task.  Value is <tt>{@value}</tt>*/
    public static final String PRESAVE_TASK_MESSAGE_KEY = "task.preSaveWikiPage";
    
    /** The workflow attribute which stores the wikiContext. */
    public static final String PRESAVE_WIKI_CONTEXT = "wikiContext";
    
    /** The name of the key from jspwiki.properties which defines who shall approve
     *  the workflow of storing a wikipage.  Value is <tt>{@value}</tt>*/
    public static final String SAVE_APPROVER             = "workflow.saveWikiPage";
    
    /** The message key for storing the Decision text for saving a page.  Value is {@value}. */
    public static final String SAVE_DECISION_MESSAGE_KEY = "decision.saveWikiPage";
    
    /** The message key for rejecting the decision to save the page.  Value is {@value}. */
    public static final String SAVE_REJECT_MESSAGE_KEY   = "notification.saveWikiPage.reject";
    
    /** The message key of the text to finally approve a page save.  Value is {@value}. */
    public static final String SAVE_TASK_MESSAGE_KEY     = "task.saveWikiPage";
    
    /** Fact name for storing the page name.  Value is {@value}. */
    public static final String FACT_PAGE_NAME = "fact.pageName";
    
    /** Fact name for storing a diff text. Value is {@value}. */
    public static final String FACT_DIFF_TEXT = "fact.diffText";
    
    /** Fact name for storing the current text.  Value is {@value}. */
    public static final String FACT_CURRENT_TEXT = "fact.currentText";
    
    /** Fact name for storing the proposed (edited) text.  Value is {@value}. */
    public static final String FACT_PROPOSED_TEXT = "fact.proposedText";
    
    /** Fact name for storing whether the user is authenticated or not.  Value is {@value}. */
    public static final String FACT_IS_AUTHENTICATED = "fact.isAuthenticated";

    /** MIME type for JSPWiki markup content. */
    public static final String JSPWIKI_CONTENT_TYPE = "text/x-jspwiki";

    private static final String NS_JSPWIKI = "http://www.jspwiki.org/ns#";
    
    private static final String DEFAULT_WORKSPACE = "jspwiki";
    
    static Logger log = LoggerFactory.getLogger( ContentManager.class );

    protected HashMap<String,PageLock> m_pageLocks = new HashMap<String,PageLock>();

    private WikiEngine m_engine;

    private int m_expiryTime = 60;

    private LockReaper m_reaper = null;

    private Repository m_repository;
    
    private String m_workspaceName = DEFAULT_WORKSPACE; // FIXME: Make settable
    
    /**
     *  Creates a new PageManager.
     *  
     *  @param engine WikiEngine instance
     *  @param props Properties to use for initialization
     *  @throws WikiException If anything goes wrong, you get this.
     */
    public ContentManager( WikiEngine engine )
        throws WikiException
    {
        String classname;

        m_engine = engine;

        m_expiryTime = TextUtil.parseIntParameter( engine.getWikiProperties().getProperty( PROP_LOCKEXPIRY ), 60 );

        InitialContext ctx;
        try
        {
            //
            //  Attempt to locate the repository object from the JNDI using
            //  "java:comp/env/jcr/repository" name
            //
            ctx = new InitialContext();
            
            Context environment = (Context) ctx.lookup("java:comp/env");
            m_repository = (Repository) environment.lookup("jcr/repository");
        }
        catch( NamingException e )
        {
            if( log.isDebugEnabled() )
                log.debug( "Unable to locate the repository from JNDI",e );
            else
                log.info( "Unable to locate the repository from JNDI, attempting to locate from jspwiki.properties" );
            
            String repositoryName = engine.getWikiProperties().getProperty( "jspwiki.repository", "priha" );
            
            log.info( "Trying repository "+repositoryName );
            
            if( "priha".equals(repositoryName) )
            {
                try
                {
                    m_repository = RepositoryManager.getRepository();
                }
                catch( ConfigurationException e1 )
                {
                    throw new WikiException( "Unable to initialize Priha as the main repository",e1);
                }
            }
            else
            {
                throw new WikiException("Unable to initialize repository for repositorytype "+repositoryName);
            }
        }
        
        try
        {
            initialize();
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Failed to initialize the repository content",e);
        }
    }

    /**
     *  Initializes the repository, making sure that everything is in place.
     * @throws RepositoryException 
     * @throws LoginException 
     */
    private void initialize() throws LoginException, RepositoryException 
    {
        Session session = m_repository.login(m_workspaceName);
        
        try
        {
            //
            //  Create the proper namespaces
            //
            
            session.getWorkspace().getNamespaceRegistry().registerNamespace( "wiki", NS_JSPWIKI );
            
            Node root = session.getRootNode();
        
            //
            // Create main page directory
            //
            if( !root.hasNode( JCR_PAGES_NODE ) )
            {
                root.addNode( JCR_PAGES_NODE );
            }
        
            //
            //  Make sure at least the default "Main" wikispace exists.
            //
            
            if( !root.hasNode( JCR_DEFAULT_SPACE ) )
            {
                root.addNode( JCR_DEFAULT_SPACE );
            }
            
            session.save();
        }
        finally
        {
            session.logout();
        }
    }
    
    /**
     *  Returns all pages in some random order.  If you need just the page names, 
     *  please see {@link ReferenceManager#findCreated()}, which is probably a lot
     *  faster.  This method may cause repository access.
     *  
     *  @param space Name of the Wiki space.  May be null, in which case gets all spaces
     *  @return A Collection of WikiPage objects.
     *  @throws ProviderException If the backend has problems.
     */
   
    public Collection<WikiPage> getAllPages( WikiContext ctx, String space )
        throws ProviderException
    {
        ArrayList<WikiPage> result = new ArrayList<WikiPage>();
        try
        {
            Session session = getJCRSession( ctx );
        
            QueryManager mgr = session.getWorkspace().getQueryManager();
            
            Query q = mgr.createQuery( "/"+JCR_PAGES_NODE+"/"+((space != null) ? space : "")+"/*", Query.XPATH );
            
            QueryResult qr = q.execute();
            
            for( NodeIterator ni = qr.getNodes(); ni.hasNext(); )
            {
                Node n = ni.nextNode();
                
                result.add( new WikiPage(ctx.getEngine(), n ) );
            }
        }
        catch( RepositoryException e )
        {
            throw new ProviderException("getAllPages()",e);
        }
        catch( WikiException e )
        {
            throw new ProviderException("getAllPages()",e);
        }
        
        return result;
    }

    /**
     *  
     *  @param pageName The name of the page to fetch.
     *  @param version The version to find
     *  @return The page content as a raw string
     *  @throws ProviderException If the backend has issues.
     *  @deprecated
     */
    public String getPageText( String path, int version )
        throws WikiException
    {
        if( path == null || path.length() == 0 )
        {
            throw new WikiException("Illegal page name");
        }

        WikiPage p = getPage( null, path, version );
        
        return p.getContentAsString();
    }
    
    /**
     *  Returns the WikiEngine to which this PageManager belongs to.
     *  
     *  @return The WikiEngine object.
     */
    public WikiEngine getEngine()
    {
        return m_engine;
    }

    /**
     *  Locks page for editing.  Note, however, that the PageManager
     *  will in no way prevent you from actually editing this page;
     *  the lock is just for information.
     *
     *  @param page WikiPage to lock
     *  @param user Username to use for locking
     *  @return null, if page could not be locked.
     */
//    public PageLock lockPage( WikiPage page, String user )
//    {
//        PageLock lock = null;
//
//        if( m_reaper == null )
//        {
//            //
//            //  Start the lock reaper lazily.  We don't want to start it in
//            //  the constructor, because starting threads in constructors
//            //  is a bad idea when it comes to inheritance.  Besides,
//            //  laziness is a virtue.
//            //
//            m_reaper = new LockReaper( m_engine );
//            m_reaper.start();
//        }
//
//        synchronized( m_pageLocks )
//        {
//            fireEvent( WikiPageEvent.PAGE_LOCK, page.getName() ); // prior to or after actual lock?
//
//            lock = m_pageLocks.get( page.getName() );
//
//            if( lock == null )
//            {
//                //
//                //  Lock is available, so make a lock.
//                //
//                Date d = new Date();
//                lock = new PageLock( page, user, d,
//                                     new Date( d.getTime() + m_expiryTime*60*1000L ) );
//
//                m_pageLocks.put( page.getName(), lock );
//
//                log.debug( "Locked page "+page.getName()+" for "+user);
//            }
//            else
//            {
//                log.debug( "Page "+page.getName()+" already locked by "+lock.getLocker() );
//                lock = null; // Nothing to return
//            }
//        }
//
//        return lock;
//    }
    /**
     *  Marks a page free to be written again.  If there has not been a lock,
     *  will fail quietly.
     *
     *  @param lock A lock acquired in lockPage().  Safe to be null.
     */
    public void unlockPage( PageLock lock )
    {
        if( lock == null ) return;

        synchronized( m_pageLocks )
        {
            m_pageLocks.remove( lock.getPage() );

            log.debug( "Unlocked page "+lock.getPage() );
        }

        fireEvent( WikiPageEvent.PAGE_UNLOCK, lock.getPage() );
    }

    /**
     *  Returns the current lock owner of a page.  If the page is not
     *  locked, will return null.
     *
     *  @param page The page to check the lock for
     *  @return Current lock, or null, if there is no lock
     */
    public PageLock getCurrentLock( WikiPage page )
    {
        PageLock lock = null;

        synchronized( m_pageLocks )
        {
            lock = m_pageLocks.get( page.getName() );
        }

        return lock;
    }

    /**
     *  Returns a list of currently applicable locks.  Note that by the time you get the list,
     *  the locks may have already expired, so use this only for informational purposes.
     *
     *  @return List of PageLock objects, detailing the locks.  If no locks exist, returns
     *          an empty list.
     *  @since 2.0.22.
     */
    public List getActiveLocks()
    {
        ArrayList<PageLock> result = new ArrayList<PageLock>();

        synchronized( m_pageLocks )
        {
            for( PageLock lock : m_pageLocks.values() )
            {
                result.add( lock );
            }
        }

        return result;
    }

    /**
     *  Finds a WikiPage object describing a particular page and version.
     *  
     *  @param pageName  The name of the page
     *  @param version   A version number
     *  @return          A WikiPage object, or null, if the page does not exist
     *  @throws ProviderException If there is something wrong with the page 
     *                            name or the repository
     */
    /*
    public WikiPage getPageInfo( String pageName, int version )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name '"+pageName+"'");
        }

        WikiPage page = null;

        try
        {
            page = m_provider.getPageInfo( pageName, version );
        }
        catch( RepositoryModifiedException e )
        {
            //
            //  This only occurs with the latest version.
            //
            log.info("Repository has been modified externally while fetching info for "+pageName );

            page = m_provider.getPageInfo( pageName, version );

            if( page != null )
            {
                m_engine.updateReferences( page );
            }
            else
            {
                m_engine.getReferenceManager().pageRemoved( new WikiPage(m_engine,pageName) );
            }
        }

        //
        //  Should update the metadata.
        //
        
        if( page != null && !page.hasMetadata() )
        {
            WikiContext ctx = new WikiContext(m_engine,page);
            m_engine.textToHTML( ctx, getPageText(pageName,version) );
        }
        
        return page;
    }
*/
    /**
     *  Gets a version history of page.  Each element in the returned
     *  List is a WikiPage.
     *  
     *  @param pageName The name of the page to fetch history for
     *  @return If the page does not exist, returns null, otherwise a List
     *          of WikiPages.
     *  @throws ProviderException If the repository fails.
     */
    /*
    public List getVersionHistory( String pageName )
        throws ProviderException
    {
        if( pageExists( pageName ) )
        {
            return m_provider.getVersionHistory( pageName );
        }

        return null;
    }
*/
    /**
     *  Returns a human-readable description of the current provider.
     *  
     *  @return A human-readable description.
     */
    /*
    public String getProviderDescription()
    {
        return m_provider.getProviderInfo();
    }
*/
    /**
     *  Returns the total count of all pages in the repository. This
     *  method is equivalent of calling getAllPages().size(), but
     *  it swallows the ProviderException and returns -1 instead of
     *  any problems.
     *  
     *  @return The number of pages, or -1, if there is an error.
     */
    /*
    public int getTotalPageCount()
    {
        try
        {
            return m_provider.getAllPages().size();
        }
        catch( ProviderException e )
        {
            log.error( "Unable to count pages: ",e );
            return -1;
        }
    }
*/
    /**
     *  Returns true, if the page exists (any version).
     *  
     *  @param pageName  Name of the page.
     *  @return A boolean value describing the existence of a page
     *  @throws ProviderException If the backend fails or the name is illegal.
     */
    /*
    public boolean pageExists( String pageName )
        throws ProviderException
    {
        if( pageName == null || pageName.length() == 0 )
        {
            throw new ProviderException("Illegal page name");
        }

        return m_provider.pageExists( pageName );
    }
*/
    /**
     *  Checks for existence of a specific page and version.
     *  
     *  @since 2.3.29
     *  @param pageName Name of the page
     *  @param version The version to check
     *  @return <code>true</code> if the page exists, <code>false</code> otherwise
     *  @throws ProviderException If backend fails or name is illegal
     */
    public boolean pageExists( WikiContext ctx, String path, int version )
        throws WikiException
    {
        if( path == null || path.length() == 0 )
        {
            throw new WikiException("Illegal page name");
        }

        Session session;
        try
        {
            session = getJCRSession( ctx );
            
            return session.itemExists( getJCRPath( (WikiContext) ctx, path ) );
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Unable to check for page existence",e);
        }
    }

    /**
     *  Deletes only a specific version of a WikiPage.
     *  
     *  @param page The page to delete.
     *  @throws ProviderException if the page fails
     */
    public void deleteVersion( WikiPage page )
        throws WikiException
    {
        fireEvent( WikiPageEvent.PAGE_DELETE_REQUEST, page.getName() );

        try
        {
            page.getJCRNode().remove();
            page.save();
            
            fireEvent( WikiPageEvent.PAGE_DELETED, page.getName() );
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Unable to delete a page",e);
        }
    }
    
    /**
     *  Deletes an entire page, all versions, all traces.
     *  
     *  @param page The WikiPage to delete
     *  @throws ProviderException If the repository operation fails
     */
    
    public void deletePage( WikiPage page )
        throws WikiException
    {
        fireEvent( WikiPageEvent.PAGE_DELETE_REQUEST, page.getName() );

        VersionHistory vh;
        try
        {
            Node nd = page.getJCRNode();
            
            // Remove version history
            if( nd.isNodeType( "mix:versionable" ) )
            {
                vh = nd.getVersionHistory();
            
                for( VersionIterator iter = vh.getAllVersions(); iter.hasNext(); )
                {
                    Version v = iter.nextVersion();
                
                    v.remove();
                    v.save();
                }
                vh.save();
            }
            
            // Remove the node itself.
            nd.remove();
            
            nd.getParent().save();
            
            fireEvent( WikiPageEvent.PAGE_DELETED, page.getName() );
        }
        catch( RepositoryException e )
        {
            throw new WikiException("Deletion of pages failed.",e);
        }
    }

    /**
     *  This is a simple reaper thread that runs roughly every minute
     *  or so (it's not really that important, as long as it runs),
     *  and removes all locks that have expired.
     */

    private class LockReaper extends WikiBackgroundThread
    {
        /**
         *  Create a LockReaper for a given engine.
         *  
         *  @param engine WikiEngine to own this thread.
         */
        public LockReaper( WikiEngine engine )
        {
            super( engine, 60 );
            setName("JSPWiki Lock Reaper");
        }

        public void backgroundTask() throws Exception
        {
            synchronized( m_pageLocks )
            {
                Collection entries = m_pageLocks.values();

                Date now = new Date();

                for( Iterator i = entries.iterator(); i.hasNext(); )
                {
                    PageLock p = (PageLock) i.next();

                    if( now.after( p.getExpiryTime() ) )
                    {
                        i.remove();

                        log.debug( "Reaped lock: "+p.getPage()+
                                   " by "+p.getLocker()+
                                   ", acquired "+p.getAcquisitionTime()+
                                   ", and expired "+p.getExpiryTime() );
                    }
                }
            }
        }
    }

    // workflow task inner classes....................................................

    /**
     * Inner class that handles the page pre-save actions. If the proposed page
     * text is the same as the current version, the {@link #execute()} method
     * returns {@link com.ecyrd.jspwiki.workflow.Outcome#STEP_ABORT}. Any
     * WikiExceptions thrown by page filters will be re-thrown, and the workflow
     * will abort.
     *
     * @author Andrew Jaquith
     */
//    public static class PreSaveWikiPageTask extends Task
//    {
//        private static final long serialVersionUID = 6304715570092804615L;
//        private final WikiContext m_context;
//        private final String m_proposedText;
//
//        /**
//         *  Creates the task.
//         *  
//         *  @param context The WikiContext
//         *  @param proposedText The text that was just saved.
//         */
//        public PreSaveWikiPageTask( WikiContext context, String proposedText )
//        {
//            super( PRESAVE_TASK_MESSAGE_KEY );
//            m_context = context;
//            m_proposedText = proposedText;
//        }
//
//        /**
//         *  {@inheritDoc}
//         */
//        @Override
//        public Outcome execute() throws WikiException
//        {
//            // Retrieve attributes
//            WikiEngine engine = m_context.getEngine();
//            Workflow workflow = getWorkflow();
//
//            // Get the wiki page
//            WikiPage page = m_context.getPage();
//
//            // Figure out who the author was. Prefer the author
//            // set programmatically; otherwise get from the
//            // current logged in user
//            if ( page.getAuthor() == null )
//            {
//                Principal wup = m_context.getCurrentUser();
//
//                if ( wup != null )
//                    page.setAuthor( wup.getName() );
//            }
//
//            // Run the pre-save filters. If any exceptions, add error to list, abort, and redirect
//            String saveText;
//            try
//            {
//                saveText = engine.getFilterManager().doPreSaveFiltering( m_context, m_proposedText );
//            }
//            catch ( FilterException e )
//            {
//                throw e;
//            }
//
//            // Stash the wiki context, old and new text as workflow attributes
//            workflow.setAttribute( PRESAVE_WIKI_CONTEXT, m_context );
//            workflow.setAttribute( FACT_PROPOSED_TEXT, saveText );
//            return Outcome.STEP_COMPLETE;
//        }
//    }
//
//    /**
//     * Inner class that handles the actual page save and post-save actions. Instances
//     * of this class are assumed to have been added to an approval workflow via
//     * {@link com.ecyrd.jspwiki.workflow.WorkflowBuilder#buildApprovalWorkflow(Principal, String, Task, String, com.ecyrd.jspwiki.workflow.Fact[], Task, String)};
//     * they will not function correctly otherwise.
//     *
//     * @author Andrew Jaquith
//     */
//    public static class SaveWikiPageTask extends Task
//    {
//        private static final long serialVersionUID = 3190559953484411420L;
//
//        /**
//         *  Creates the Task.
//         */
//        public SaveWikiPageTask()
//        {
//            super( SAVE_TASK_MESSAGE_KEY );
//        }
//
//        /** {@inheritDoc} */
//        @Override
//        public Outcome execute() throws WikiException
//        {
//            // Retrieve attributes
//            WikiContext context = (WikiContext) getWorkflow().getAttribute( PRESAVE_WIKI_CONTEXT );
//            String proposedText = (String) getWorkflow().getAttribute( FACT_PROPOSED_TEXT );
//
//            WikiEngine engine = context.getEngine();
//            WikiPage page = context.getPage();
//
//            // Let the rest of the engine handle actual saving.
//            engine.getPageManager().putPageText( page, proposedText );
//
//            // Refresh the context for post save filtering.
//            engine.getPage( page.getName() );
//            engine.textToHTML( context, proposedText );
//            engine.getFilterManager().doPostSaveFiltering( context, proposedText );
//
//            return Outcome.STEP_COMPLETE;
//        }
//    }

    // events processing .......................................................

    /**
     *  Fires a WikiPageEvent of the provided type and page name
     *  to all registered listeners.
     *
     * @see com.ecyrd.jspwiki.event.WikiPageEvent
     * @param type       the event type to be fired
     * @param pagename   the wiki page name as a String
     */
    protected final void fireEvent( int type, String pagename )
    {
        if ( WikiEventManager.isListening(this) )
        {
            WikiEventManager.fireEvent(this,new WikiPageEvent(m_engine,type,pagename));
        }
    }

    private Session getJCRSession( WikiContext context ) throws RepositoryException
    {
        Session session = (Session)context.getVariable( "jcrsession" );
        
        if( session == null )
        {
            session = m_repository.login(m_workspaceName);
            
            context.setVariable( "jcrsession", session );
        }
        
        return session;
    }
    
    /**
     *  Evaluates a WikiName in the context of the current page request.
     *  
     *  @param ctx The current WikiContext.  May be null, in which case the wikiName must be a FQN.
     *  @param wikiName The WikiName.
     *  @return A full JCR path
     *  @throws WikiException If the conversion could not be done.
     */
    protected static String getJCRPath( WikiContext ctx, String wikiName ) throws WikiException
    {
        String spaceName;
        String spacePath;
        
        int colon = wikiName.indexOf(':');
        
        if( colon != -1 )
        {
            // This is a FQN
            spaceName = wikiName.substring( 0, colon );
            spacePath = wikiName.substring( colon+1 );
        }
        else if( ctx != null )
        {
            WikiPage contextPage = ctx.getPage();
            
            // Not an FQN, the wiki name is missing, so we'll use the context to figure it out
            spaceName = contextPage.getWiki();
            
            // If the wikipath starts with "/", we assume it is an absolute path within this
            // wiki space.  Otherwise, it must be relative to the current path.
            if( wikiName.startsWith( "/" ) )
            {
                spacePath = wikiName;
            }
            else
            {
                spacePath = contextPage.getName()+"/"+wikiName;
            }
        }
        else
        {
            spaceName = DEFAULT_SPACE;
            spacePath = wikiName;
        }
        
        return "/pages/"+spaceName+"/"+spacePath;
    }

    // FIXME: Should be protected - fix once WikiPage moves to content-package
    public static WikiName getWikiPath( String jcrpath ) throws WikiException
    {
        if( jcrpath.startsWith("/"+JCR_PAGES_NODE+"/") )
        {
            String wikiPath = jcrpath.substring( ("/"+JCR_PAGES_NODE+"/").length() );

            int firstSlash = wikiPath.indexOf( '/' );
            
            return new WikiName(wikiPath.substring( 0, firstSlash ), 
                                wikiPath.substring( firstSlash+1 ) );
        }
        
        throw new WikiException("This is not a valid JCR path: "+jcrpath);
    }
    
    /**
     *  Adds new content to the repository.  To update, get a page, modify
     *  it, then store it back using save().
     *  
     *  @param path
     *  @param contentType
     *  @return
     */
    public WikiPage addPage( WikiContext context, String path, String contentType ) throws WikiException
    {
        try
        {
            Session session = getJCRSession( context );
        
            Node nd = session.getRootNode().addNode( getJCRPath(null, path) );
            
            WikiPage page = new WikiPage(m_engine, path);
            page.setJCRNode( nd );
            
            return page;
        }
        catch( RepositoryException e )
        {
            throw new WikiException( "Unable to add a page", e );
        }
    }

    /**
     *  Get content from the repository.
     *  
     *  @param path
     *  @return
     */
    public WikiPage getPage( WikiContext context, String path ) throws WikiException
    {
        try
        {
            Session session = getJCRSession( context );
        
            Node nd = session.getRootNode().getNode( getJCRPath(context, path) );
            WikiPage page = new WikiPage(m_engine, path);
            page.setJCRNode( nd );
            
            return page;
        }
        catch( PathNotFoundException e )
        {
            return null;
        }
        catch( RepositoryException e )
        {
            throw new WikiException( "Unable to get a page", e );
        }
    }

    public WikiPage getPage( WikiContext context, String path, int version ) throws WikiException
    {
        try
        {
            Session session = getJCRSession( context );
        
            Node nd = session.getRootNode().getNode( getJCRPath(null, path) );

            VersionHistory vh = nd.getVersionHistory();
            
            Version v = vh.getVersion( Integer.toString( version ) );
            
            WikiPage page = new WikiPage(m_engine, path);
            page.setJCRNode( v );
            
            return page;
        }
        catch( RepositoryException e )
        {
            throw new WikiException( "Unable to get a page", e );
        }
    }
    
    /**
     *  Listens for {@link com.ecyrd.jspwiki.event.WikiSecurityEvent#PROFILE_NAME_CHANGED}
     *  events. If a user profile's name changes, each page ACL is inspected. If an entry contains
     *  a name that has changed, it is replaced with the new one. No events are emitted
     *  as a consequence of this method, because the page contents are still the same; it is
     *  only the representations of the names within the ACL that are changing.
     * 
     *  @param event The event
     */
//    public void actionPerformed(WikiEvent event)
//    {
//        if (! ( event instanceof WikiSecurityEvent ) )
//        {
//            return;
//        }
//
//        WikiSecurityEvent se = (WikiSecurityEvent)event;
//        if ( se.getType() == WikiSecurityEvent.PROFILE_NAME_CHANGED )
//        {
//            UserProfile[] profiles = (UserProfile[])se.getTarget();
//            Principal[] oldPrincipals = new Principal[]
//                { new WikiPrincipal( profiles[0].getLoginName() ),
//                  new WikiPrincipal( profiles[0].getFullname() ),
//                  new WikiPrincipal( profiles[0].getWikiName() ) };
//            Principal newPrincipal = new WikiPrincipal( profiles[1].getFullname() );
//
//            // Examine each page ACL
//            try
//            {
//                int pagesChanged = 0;
//                Collection pages = getAllPages();
//                for ( Iterator it = pages.iterator(); it.hasNext(); )
//                {
//                    WikiPage page = (WikiPage)it.next();
//                    boolean aclChanged = changeAcl( page, oldPrincipals, newPrincipal );
//                    if ( aclChanged )
//                    {
//                        // If the Acl needed changing, change it now
//                        try
//                        {
//                            m_engine.getAclManager().setPermissions( page, page.getAcl() );
//                        }
//                        catch ( WikiSecurityException e )
//                        {
//                            log.error( "Could not change page ACL for page " + page.getName() + ": " + e.getMessage() );
//                        }
//                        pagesChanged++;
//                    }
//                }
//                log.info( "Profile name change for '" + newPrincipal.toString() +
//                          "' caused " + pagesChanged + " page ACLs to change also." );
//            }
//            catch ( ProviderException e )
//            {
//                // Oooo! This is really bad...
//                log.error( "Could not change user name in Page ACLs because of Provider error:" + e.getMessage() );
//            }
//        }
//    }

    /**
     *  For a single wiki page, replaces all Acl entries matching a supplied array of Principals 
     *  with a new Principal.
     * 
     *  @param page the wiki page whose Acl is to be modified
     *  @param oldPrincipals an array of Principals to replace; all AclEntry objects whose
     *   {@link AclEntry#getPrincipal()} method returns one of these Principals will be replaced
     *  @param newPrincipal the Principal that should receive the old Principals' permissions
     *  @return <code>true</code> if the Acl was actually changed; <code>false</code> otherwise
     */
//    protected boolean changeAcl( WikiPage page, Principal[] oldPrincipals, Principal newPrincipal )
//    {
//        Acl acl = page.getAcl();
//        boolean pageChanged = false;
//        if ( acl != null )
//        {
//            Enumeration entries = acl.entries();
//            Collection<AclEntry> entriesToAdd    = new ArrayList<AclEntry>();
//            Collection<AclEntry> entriesToRemove = new ArrayList<AclEntry>();
//            while ( entries.hasMoreElements() )
//            {
//                AclEntry entry = (AclEntry)entries.nextElement();
//                if ( ArrayUtils.contains( oldPrincipals, entry.getPrincipal() ) )
//                {
//                    // Create new entry
//                    AclEntry newEntry = new AclEntryImpl();
//                    newEntry.setPrincipal( newPrincipal );
//                    Enumeration permissions = entry.permissions();
//                    while ( permissions.hasMoreElements() )
//                    {
//                        Permission permission = (Permission)permissions.nextElement();
//                        newEntry.addPermission(permission);
//                    }
//                    pageChanged = true;
//                    entriesToRemove.add( entry );
//                    entriesToAdd.add( newEntry );
//                }
//            }
//            for ( Iterator ix = entriesToRemove.iterator(); ix.hasNext(); )
//            {
//                AclEntry entry = (AclEntry)ix.next();
//                acl.removeEntry( entry );
//            }
//            for ( Iterator ix = entriesToAdd.iterator(); ix.hasNext(); )
//            {
//                AclEntry entry = (AclEntry)ix.next();
//                acl.addEntry( entry );
//            }
//        }
//        return pageChanged;
//    }

}
