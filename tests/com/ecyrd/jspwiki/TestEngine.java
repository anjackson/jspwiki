
package com.ecyrd.jspwiki;
import java.util.Properties;

public class TestEngine extends WikiEngine
{
    public TestEngine( Properties props )
        throws NoRequiredPropertyException
    {
        super( props );
    }

    public boolean pageExists( String page )
    {
        return true;
    }
}
