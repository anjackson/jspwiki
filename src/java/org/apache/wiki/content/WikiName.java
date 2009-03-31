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
package org.apache.wiki.content;

import java.io.Serializable;

/**
 *  A WikiName represents a combination of a WikiSpace as well as a
 *  path within that space.  For example, in "MyWiki:MainPage/foo.jpg",
 *  "MyWiki" is the space, and "MainPage/foo.jpg" is the path within that space.
 *  <p>
 *  A WikiName is a static object which cannot be changed after it has been
 *  created.
 *  
 *  @since 3.0
 */
public class WikiName implements Serializable, Comparable<WikiName>
{
    private static final long serialVersionUID = 1L;
    private String m_space;
    private String m_path;
    private String m_stringRepresentation = null;
    
    private WikiName()
    {}
    
    /**
     *  Create a WikiName from a space and a path.
     *  
     *  @param space The space
     *  @param path The path
     */
    public WikiName(String space, String path)
    {
        m_space = space;
        m_path  = path;
    }
    
    /**
     *  Parses a fully-qualified name (FQN) and turns it into a WikiName.
     *  If the space name is missing, uses {@link ContentManager#DEFAULT_SPACE}
     *  for the space name.
     *  
     *  @param path Path to parse
     *  @return A WikiName
     */
    public static WikiName valueOf(String path)
    {
        WikiName name = new WikiName();
        int colon = path.indexOf(':');
        
        if( colon != -1 )
        {
            // This is a FQN
            name.m_space = path.substring( 0, colon );
            name.m_path  = path.substring( colon+1 );
            
            return name;
        }

        name.m_space = ContentManager.DEFAULT_SPACE;
        name.m_path = path;
        
        return name;
        
        // FIXME: Should probably use this
        //throw new IllegalArgumentException("The path does not represent a fully qualified WikiName (space:path/path/path)");
    }
    
    /**
     *  Return the space part of the WikiName
     *  
     *  @return Just the space name.
     */
    public String getSpace()
    {
        return m_space;
    }
    
    /**
     *  Return the path part of the WikiName.
     *  
     *  @return Just the path.
     */
    public String getPath()
    {
        return m_path;
    }
    
    /**
     *  Returns the WikiName of the parent.
     *  
     *  @return A Valid WikiName or null, if there is no parent.
     */
    // FIXME: Would it make more sense to throw an exception?
    public WikiName getParent()
    {
        int slash = m_path.lastIndexOf( '/' );
        
        if( slash == -1 ) return null;
        
        return new WikiName( m_space, m_path.substring( 0, slash ) );
    }
    
    /**
     *  Resolves a path with respect to this WikiName.  This is typically used
     *  when figuring out where a subpage should be pointing at.
     *  
     *  @param path Path to resolve
     *  @return A new WikiName
     */
    public WikiName resolve( String path )
    {
        int colon = path.indexOf( ':' );
        
        if( colon != -1 )
        {
            // It is a FQN, essentially an absolute path, so no resolution necessary
            return WikiName.valueOf( path );
        }
        
        return new WikiName( getSpace(), path );
    }
    
    /**
     *  Returns the FQN format (space:path) of the name.
     *  
     *  @return The name in FQN format.
     */
    public String toString()
    {
        //
        //  The String representation is cached for maximum speed
        //  and object creation overhead.
        //
        if( m_stringRepresentation == null )
            m_stringRepresentation = m_space+":"+m_path;
        
        return m_stringRepresentation;
    }

    /**
     *  The hashcode of the WikiName is exactly the same as the hashcode
     *  of its String representation.  This is to fulfil the general
     *  contract of equals().
     *  
     *  @return int 
     */
    public int hashCode()
    {
        return toString().hashCode();
    }
    
    /**
     *  A WikiName is compared using it's toString() method.
     *  
     *  @param o The Object to compare against.
     *  @return int
     */
    public int compareTo( WikiName o )
    {
        return toString().compareTo( o.toString() );
    }
    
    /**
     *  A WikiName is equal to another WikiName if the space and the path
     *  match.  A WikiName can also be compared to a String, in which case
     *  a WikiName is equal to the String if its String representation is
     *  the same.  This is to make it easier to compare.
     *  
     *  @param o The Object to compare against.
     *  @return True, if this WikiName is equal to another WikiName.
     */
    public boolean equals( Object o )
    {
        if( o instanceof WikiName )
        {
            WikiName n = (WikiName) o;
            
            return m_space.equals( n.m_space ) && m_path.equals( n.m_path );
        }
        else if( o instanceof String )
        {
            return toString().equals( o );
        }
        return false;
    }
}
