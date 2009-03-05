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
package org.apache.wiki.attachment;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.apache.wiki.JCRWikiPage;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiProvider;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.WikiName;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.providers.ProviderException;

import com.opensymphony.oscache.base.Cache;
import com.opensymphony.oscache.base.NeedsRefreshException;

/**
 *  Provides facilities for handling attachments.  All attachment
 *  handling goes through this class.
 *  <p>
 *  The AttachmentManager provides a facade towards the current WikiAttachmentProvider
 *  that is in use.  It is created by the WikiEngine as a singleton object, and
 *  can be requested through the WikiEngine.
 *
 *  @since 1.9.28
 */
public class AttachmentManager
{
    /**
     *  The property name for defining the attachment provider class name.
     */
    public static final String  PROP_PROVIDER = "jspwiki.attachmentProvider";

    /**
     *  The maximum size of attachments that can be uploaded.
     */
    public static final String  PROP_MAXSIZE  = "jspwiki.attachment.maxsize";

    /**
     *  A space-separated list of attachment types which can be uploaded
     */
    public static final String PROP_ALLOWEDEXTENSIONS    = "jspwiki.attachment.allowed";

    /**
     *  A space-separated list of attachment types which cannot be uploaded
     */
    public static final String PROP_FORDBIDDENEXTENSIONS = "jspwiki.attachment.forbidden";

    static Logger log = LoggerFactory.getLogger( AttachmentManager.class );
    private WikiEngine             m_engine;

    /**
     *  Creates a new AttachmentManager.  Note that creation will never fail,
     *  but it's quite likely that attachments do not function.
     *  <p>
     *  <b>DO NOT CREATE</b> an AttachmentManager on your own, unless you really
     *  know what you're doing.  Just use WikiEngine.getAttachmentManager() if
     *  you're making a module for JSPWiki.
     *
     *  @param engine The wikiengine that owns this attachment manager.
     *  @param props  A list of properties from which the AttachmentManager will seek
     *  its configuration.  Typically this is the "jspwiki.properties".
     */

    // FIXME: Perhaps this should fail somehow.
    public AttachmentManager( WikiEngine engine, Properties props )
    {
        m_engine = engine;
    }

    /**
     *  Returns true, if attachments are enabled and running.
     *
     *  @return A boolean value indicating whether attachment functionality is enabled.
     */
    public boolean attachmentsEnabled()
    {
        return true; // ALways enabled in 3.0
    }

    /**
     *  Gets info on a particular attachment, latest version.
     *
     *  @param name A full attachment name.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */
    public Attachment getAttachmentInfo( String name )
        throws ProviderException
    {
        return m_engine.getContentManager().getPage( WikiName.valueOf( name ) );
    }

    /**
     *  Gets info on a particular attachment with the given version.
     *
     *  @param name A full attachment name.
     *  @param version A version number.
     *  @return Attachment, or null, if no such attachment or version exists.
     *  @throws ProviderException If something goes wrong.
     */

    public Attachment getAttachmentInfo( String name, int version )
        throws ProviderException
    {
        if( name == null )
        {
            return null;
        }

        return getAttachmentInfo( null, name, version );
    }

    /**
     *  Figures out the full attachment name from the context and
     *  attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @return Attachment, or null, if no such attachment exists.
     *  @throws ProviderException If something goes wrong.
     */

    public Attachment getAttachmentInfo( WikiContext context,
                                         String attachmentname )
        throws ProviderException
    {
        return getAttachmentInfo( context, attachmentname, WikiProvider.LATEST_VERSION );
    }

    /**
     *  Figures out the full attachment name from the context and
     *  attachment name.
     *
     *  @param context The current WikiContext
     *  @param attachmentname The file name of the attachment.
     *  @param version A particular version.
     *  @return Attachment, or null, if no such attachment or version exists.
     *  @throws ProviderException If something goes wrong.
     */

    public Attachment getAttachmentInfo( WikiContext context,
                                         String attachmentname,
                                         int version )
        throws ProviderException
    {
        WikiPage currentPage = null;

        if( context != null )
        {
            currentPage = context.getPage();
        }

        WikiName name = currentPage.getQualifiedName().resolve( attachmentname );
        
        Attachment att;

        att = getDynamicAttachment( name );

        if( att == null )
        {
            att = m_engine.getContentManager().getPage( name, version );
        }

        return att;
    }

    /**
     *  Returns the list of attachments associated with a given wiki page.
     *  If there are no attachments, returns an empty Collection.
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return a valid collection of attachments.
     *  @throws ProviderException If there was something wrong in the backend.
     */

    // FIXME: This API should be changed to return a List.
    @SuppressWarnings("unchecked")
    public Collection listAttachments( WikiPage wikipage )
        throws ProviderException
    {
        List<WikiPage> children = wikipage.getChildren();
        ArrayList<Attachment> atts = new ArrayList<Attachment>(); 
        
        for( WikiPage p : children )
        {
            JCRWikiPage jwp = (JCRWikiPage)p;
            if( jwp.isAttachment() )
                atts.add( jwp );
        }
        
        return atts;
    }

    /**
     *  Returns true, if the page has any attachments at all.  This is
     *  a convinience method.
     *
     *
     *  @param wikipage The wiki page from which you are seeking attachments for.
     *  @return True, if the page has attachments, else false.
     */
    public boolean hasAttachments( WikiPage wikipage )
    {
        try
        {
            return listAttachments( wikipage ).size() > 0;
        }
        catch( Exception e ) {}

        return false;
    }

    /**
     *  Finds a (real) attachment from the repository as a stream.
     *
     *  @param att Attachment
     *  @return An InputStream to read from.  May return null, if
     *          attachments are disabled.
     *  @throws IOException If the stream cannot be opened
     *  @throws ProviderException If the backend fails due to some other reason.
     */
    public InputStream getAttachmentStream( Attachment att )
        throws IOException,
               ProviderException
    {
        return getAttachmentStream( null, att );
    }

    /**
     *  Returns an attachment stream using the particular WikiContext.  This method
     *  should be used instead of getAttachmentStream(Attachment), since it also allows
     *  the DynamicAttachments to function.
     *
     *  @param ctx The Wiki Context
     *  @param att The Attachment to find
     *  @return An InputStream.  May return null, if attachments are disabled.  You must
     *          take care of closing it.
     *  @throws ProviderException If the backend fails due to some reason
     *  @throws IOException If the stream cannot be opened
     */
    public InputStream getAttachmentStream( WikiContext ctx, Attachment att )
        throws ProviderException, IOException
    {
        if( att instanceof DynamicAttachment )
        {
            return ((DynamicAttachment)att).getProvider().getAttachmentData( ctx, att );
        }

        return att.getContentAsStream();
    }

    private Cache m_dynamicAttachments = new Cache( true, false, false );

    /**
     *  Stores a dynamic attachment.  Unlike storeAttachment(), this just stores
     *  the attachment in the memory.
     *
     *  @param ctx A WikiContext
     *  @param att An attachment to store
     */
    public void storeDynamicAttachment( WikiContext ctx, DynamicAttachment att )
    {
        m_dynamicAttachments.putInCache( att.getName(),  att );
    }

    /**
     *  Finds a DynamicAttachment.  Normally, you should just use getAttachmentInfo(),
     *  since that will find also DynamicAttachments.
     *
     *  @param name The name of the attachment to look for
     *  @return An Attachment, or null.
     *  @see #getAttachmentInfo(String)
     */

    public DynamicAttachment getDynamicAttachment( WikiName name )
    {
        try
        {
            return (DynamicAttachment) m_dynamicAttachments.getFromCache( name.toString() );
        }
        catch( NeedsRefreshException e )
        {
            //
            //  Remove from cache, it has expired.
            //
            m_dynamicAttachments.putInCache( name.toString(), null );

            return null;
        }
    }

    /**
     *  Stores an attachment that lives in the given file.
     *  If the attachment did not exist previously, this method
     *  will create it.  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param source A file to read from.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    public void storeAttachment( Attachment att, File source )
        throws IOException,
               ProviderException
    {
        FileInputStream in = null;

        try
        {
            in = new FileInputStream( source );
            storeAttachment( att, in );
        }
        finally
        {
            if( in != null ) in.close();
        }
    }

    /**
     *  Stores an attachment directly from a stream.
     *  If the attachment did not exist previously, this method
     *  will create it.  If it did exist, it stores a new version.
     *
     *  @param att Attachment to store this under.
     *  @param in  InputStream from which the attachment contents will be read.
     *
     *  @throws IOException If writing the attachment failed.
     *  @throws ProviderException If something else went wrong.
     */
    public void storeAttachment( Attachment att, InputStream in )
        throws IOException,
               ProviderException
    {
        att.setContent( in );
        att.save();
    }

    /**
     *  Returns a list of versions of the attachment.
     *
     *  @param attachmentName A fully qualified name of the attachment.
     *
     *  @return A list of Attachments.  May return null, if attachments are
     *          disabled.
     *  @throws ProviderException If the provider fails for some reason.
     */
    public List getVersionHistory( String attachmentName )
        throws ProviderException
    {
        return m_engine.getContentManager().getVersionHistory( WikiName.valueOf(attachmentName) );
    }

    /**
     *  Deletes the given attachment version.
     *
     *  @param att The attachment to delete
     *  @throws ProviderException If something goes wrong with the backend.
     */
    public void deleteVersion( WikiPage att )
        throws ProviderException
    {
        m_engine.getContentManager().deleteVersion( att );
    }

    /**
     *  Deletes all versions of the given attachment.
     *  @param att The Attachment to delete.
     *  @throws ProviderException if something goes wrong with the backend.
     */
    // FIXME: Should also use events!
    public void deleteAttachment( Attachment att )
        throws ProviderException
    {
        m_engine.getContentManager().deletePage( att );
    }

    /**
     *  Validates the filename and makes sure it is legal.  It trims and splits
     *  and replaces bad characters.
     *  
     *  @param filename
     *  @return A validated name with annoying characters replaced.
     *  @throws WikiException If the filename is not legal (e.g. empty)
     */
    static String validateFileName( String filename )
        throws WikiException
    {
        if( filename == null || filename.trim().length() == 0 )
        {
            log.error("Empty file name given.");
    
            // the caller should catch the exception and use the exception text as an i18n key
            throw new WikiException(  "attach.empty.file" );
        }
    
        //
        //  Should help with IE 5.22 on OSX
        //
        filename = filename.trim();

        // If file name ends with .jsp or .jspf, the user is being naughty!
        if( filename.toLowerCase().endsWith( ".jsp" ) || filename.toLowerCase().endsWith(".jspf") )
        {
            log.info( "Attempt to upload a file with a .jsp/.jspf extension.  In certain cases this" +
                      " can trigger unwanted security side effects, so we're preventing it." );
            //
            // the caller should catch the exception and use the exception text as an i18n key
            throw new WikiException(  "attach.unwanted.file"  );
        }
    
        //
        //  Some browser send the full path info with the filename, so we need
        //  to remove it here by simply splitting along slashes and then taking the path.
        //
        
        String[] splitpath = filename.split( "[/\\\\]" );
        filename = splitpath[splitpath.length-1];
        
        //
        //  Remove any characters that might be a problem. Most
        //  importantly - characters that might stop processing
        //  of the URL.
        //
        filename = StringUtils.replaceChars( filename, "#?\"'", "____" );
    
        return filename;
    }
}
