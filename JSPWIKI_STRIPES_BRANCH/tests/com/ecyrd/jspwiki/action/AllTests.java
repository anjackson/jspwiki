
package com.ecyrd.jspwiki.action;

import junit.framework.*;

public class AllTests extends TestCase
{
    public AllTests( String s )
    {
        super( s );
    }

    public static Test suite()
    {
        TestSuite suite = new TestSuite("ActionBean tests");

        suite.addTest( EventPermissionInfoTest.suite() );
        suite.addTest( RenameActionBeanTest.suite() );
        suite.addTest( UserPreferencesActionBeanTest.suite() );
        suite.addTest( UserProfileActionBeanTest.suite() );
        suite.addTest( ViewActionBeanTest.suite() );
        suite.addTest( WikiActionBeanFactoryTest.suite() );

        return suite;
    }
}
