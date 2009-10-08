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
package org.apache.wiki.tags;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.content.inspect.BotTrapInspector;
import org.apache.wiki.filters.SpamFilter;
import org.apache.wiki.ui.stripes.SpamInterceptor;

/**
 * <p>
 * Tag that injects hidden {@link SpamFilter}-related parameters into the
 * current form, which will be parsed and verified by {@link SpamInterceptor}
 * whenever the input is processed by an ActionBean event handler method
 * annotated with the {@link org.apache.wiki.ui.stripes.SpamProtect} annotation.
 * </p>
 * <p>
 * This tag must be added as a child of an existing &lt;form&gt; or
 * &lt;stripes:form&gt; element. The SpamProtect tag will cause the
 * following parameters into the form:
 * </p>
 * <ol>
 * <li><b>An UTF-8 detector parameter called <code>encodingcheck</code>.</b>
 * This parameter contains a single non-Latin1 character. SpamInterceptor verifies
 * that the non-Latin1 character has not been mangled by a badly behaving robot or
 * user client. Many bots assume a form is Latin1. This also prevents the "hey,
 * my edit destroyed all UTF-8 characters" problem.</li>
 * <li><b>A token field </b>, which has a random name and fixed value</b>
 * This means that a bot will need to actually GET the form first and parse it
 * out before it can send syntactically correct POSTs. This is a LOT more effort
 * than just simply looking at the fields once and crafting your auto-poster to
 * conform. </li>
 * <li><b>An empty, input field hidden with CSS</b>. This parameter is meant
 * to catch bots which do a GET and then randomly fill all fields with garbage.
 * This field <em>must</em>be empty when SpamFilter examines the contents of
 * the POST. Since it's hidden with the use of CSS, the bot would need to
 * understand CSS to bypass this check. Because the parameter is also
 * randomized, it prevents bot authors from hard-coding the fact that it needs
 * to be empty.</li>
 * </li>
 * <li><b>An an encrypted parameter</b> called
 * {@link BotTrapInspector#REQ_SPAM_PARAM}, whose contents are the names of
 * parameters 2 and 3, separated by a carriage return character. These contents
 * are then encrypted.</li>
 * </ol>
 * <p>
 * The idea between 2 & 3 is that SpamInterceptor expects two fields with random
 * names, one of which needs to be empty, and one of which needs to be filled
 * with pre-determined data. This is quite hard for most bots to catch, unless
 * they are specifically crafted for JSPWiki and contain some amount of logic to
 * figure out the scheme.
 * </p>
 */
public class SpamProtectTag extends WikiTagBase
{

    @Override
    public int doWikiStartTag() throws Exception
    {
        return TagSupport.SKIP_BODY;
    }

    private void writeSpamParams() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();

        // Inject honey-trap param (should always be submitted with no value)
        String trapParam = BotTrapInspector.REQ_TRAP_PARAM;
        out.write( "<div style=\"display: none;\">" );
        out.write( "<input type=\"hidden\" name=\"" );
        out.write( trapParam );
        out.write( "\" value=\"\" /></div>\n" );
        
        // Inject token field
        String tokenParam = getUniqueID();
        String tokenValue = request.getSession().getId();
        out.write( "<input name=\"" + tokenParam + "\" type=\"hidden\" value=\"" + tokenValue + "\" />\n" );

        // Inject UTF-8 detector
        out.write( "<input name=\""+ BotTrapInspector.REQ_ENCODING_CHECK + "\" type=\"hidden\" value=\"\u3041\" />\n" );

        // Add encrypted parameter indicating the name of the token field
        String encryptedParam = CryptoUtil.encrypt( tokenParam );
        out.write( "<input name=\""+ BotTrapInspector.REQ_SPAM_PARAM+"\" type=\"hidden\" value=\""+encryptedParam+"\" />\n" );
    }

    private static String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();
        Random rand = new SecureRandom();

        for( int i = 0; i < 6; i++ )
        {
            char x = (char) ('A' + rand.nextInt( 26 ));

            sb.append( x );
        }

        return sb.toString();
    }

    @Override
    public int doEndTag() throws JspException
    {
        try
        {
            writeSpamParams();
        }
        catch( IOException e )
        {
            throw new JspException(e);
        }
        return super.doEndTag();
    }

}
