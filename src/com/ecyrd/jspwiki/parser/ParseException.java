package com.ecyrd.jspwiki.parser;

import com.ecyrd.jspwiki.WikiException;

/**
 *  This is an exception which gets thrown whenever the parser cannot
 *  parse the parsing things.
 *  
 *  @author jalkanen
 */
public class ParseException extends WikiException
{
    public ParseException(String msg)
    {
        super(msg);
    }

}
