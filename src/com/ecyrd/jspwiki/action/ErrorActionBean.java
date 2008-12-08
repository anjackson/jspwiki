package com.ecyrd.jspwiki.action;

import com.ecyrd.jspwiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.*;

@UrlBinding("/Error.jsp")
public class ErrorActionBean extends AbstractPageActionBean
{
    /**
     * Default event that forwards control to /Message.jsp.
     * @return the forward resolution
     */
    @DefaultHandler
    @HandlesEvent("error")
    @WikiRequestContext("error")
    public Resolution view() 
    {
        return new ForwardResolution( "/Error.jsp" );
    }
}
