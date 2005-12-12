package com.ecyrd.jspwiki.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ecyrd.jspwiki.WikiSession;

/**
 * Provides basic validation services for HTTP parameters. Two standard
 * validators are provided: email address and standard input. Standard input
 * validator will reject any HTML-like input, and any of a number of special
 * characters.
 * @author Andrew Jaquith
 * @version $Revision: 1.1 $ $Date: 2005-12-12 07:04:43 $
 * @since 2.3.54
 */
public final class InputValidator
{
    public static final int        STANDARD       = 0;

    public static final int        EMAIL          = 1;

    protected static final Pattern EMAIL_PATTERN  = Pattern.compile( "^[0-9a-zA-Z-_]+@([0-9a-zA-Z-_]+\\.)+[a-zA-Z]+$" );

    protected static final Pattern UNSAFE_PATTERN = Pattern.compile( "[\\x00\\r\\n\\x0f\"':<>;&@\\xff{}\\$%\\\\]" );

    private final String           m_form;

    private final WikiSession      m_session;

    /**
     * Constructs a new input validator for a specific form and wiki session.
     * When validation errors are detected, they will be added to the wiki
     * session's messages.
     * @param form the ID or name of the form this validator should be
     * associated with
     * @param session the wiki session
     */
    public InputValidator( String form, WikiSession session )
    {
        m_form = form;
        m_session = session;
    }

    /**
     * Validates a string against the {@link #STANDARD} validator and
     * additionally checks that the value is not <code>null</code> or blank.
     * @param input the string to validate
     * @param label the label for the string or field ("E-mail address")
     * @return returns <code>true</code> if valid, <code>false</code>
     * otherwise
     */
    public final boolean validateNotNull( String input, String label )
    {
        return validateNotNull( input, label, STANDARD );
    }

    /**
     * Validates a string against a particular pattern type and additionally
     * checks that the value is not <code>null</code> or blank. Delegates to
     * {@link #validate(String, int)}.
     * @param input the string to validate
     * @param label the label for the string or field ("E-mail address")
     * @param type
     * @return returns <code>true</code> if valid, <code>false</code>
     * otherwise
     */
    public final boolean validateNotNull( String input, String label, int type )
    {
        if ( isBlank( input ) )
        {
            m_session.addMessage( m_form, label + " cannot be null" );
            return false;
        }
        return validate( input, label, type ) && !isBlank( input );
    }

    /**
     * Validates a string against a particular pattern type: e-mail address,
     * standard HTML input, etc.
     * @param input the string to validate
     * @param label the label for the string or field ("E-mail address")
     * @param type the target pattern to validate against ({@link #STANDARD},
     * {@link #EMAIL})
     * @return returns <code>true</code> if valid, <code>false</code>
     * otherwise
     */
    public final boolean validate( String input, String label, int type )
    {
        Matcher matcher;
        boolean valid;
        switch( type )
        {
        case STANDARD:
            matcher = UNSAFE_PATTERN.matcher( input );
            valid = !matcher.find();
            if ( !valid )
            {
                m_session.addMessage( m_form, label + " cannot contain these characters: \"'<>;&@{}%$\\" );
            }
            return valid;
        case EMAIL:
            matcher = EMAIL_PATTERN.matcher( input );
            valid = matcher.matches();
            if ( !valid )
            {
                m_session.addMessage( m_form, label + " is not valid" );
            }
            return valid;
        }
        throw new IllegalArgumentException( "Invalid input type." );
    }

    /**
     * Returns <code>true</code> if a supplied string is null or blank
     * @param parameter
     */
    public static final boolean isBlank( String input )
    {
        return ( input == null || input.trim().length() < 1 );
    }
}
