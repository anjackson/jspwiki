package org.apache.wiki.content.inspect;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;

/**
 * Factory for creating spam-related {@link Inspection} and
 * {@link InspectionPlan} objects.
 */
public class SpamInspectionFactory
{
    private static final Logger log = LoggerFactory.getLogger( SpamInspectionFactory.class );

    protected static final String PROP_INSPECTOR_WEIGHT_PREFIX = "inspectorWeight.spam.";

    /**
     * The filter property name for specifying the threshold for scoring above
     * which a change should be considered spam.
     */
    public static final String PROP_SCORE_LIMIT = "spamScoreLimit";

    private static class SpamListener implements InspectionListener
    {
        private final float m_limit;

        public SpamListener( float limit )
        {
            super();
            m_limit = limit;
        }

        public void changedScore( Inspection inspection, Finding finding ) throws InspectionInterruptedException
        {
            float currentScore = inspection.getScore( Topic.SPAM );
            if( currentScore < m_limit )
            {
                throw new InspectionInterruptedException( "Limit reached." );
            }
        }
    }

    /**
     * Causes an existing Inspection to interrupt when spam scores fall below a
     * given limit. This is done by adding a private {@link InspectionListener}
     * to the Inspection, whose only function is to listen for findings for
     * {@link Topic#SPAM}. When the spam score goes below {@code limit}, the
     * private listener throws an {@link InspectionInterruptedException} which
     * terminates the inspection.
     * 
     * @param inspection the Inspection for which the limit is set
     * @param limit the spam score that causes the Inspection to stop. Usually
     *            this is a negative number.
     */
    public static void setSpamLimit( Inspection inspection, float limit )
    {
        InspectionListener listener = new SpamListener( limit );
        inspection.addListener( Topic.SPAM, listener );
    }

    /** Default limit at which we consider something to be spam. */
    protected static final float DEFAULT_SCORE_LIMIT = -0.01f;

    /**
     * Default weight for Inspectors.
     */
    protected static final float DEFAULT_WEIGHT = 0f;

    private static final Map<WikiEngine, InspectionPlan> c_plans = new HashMap<WikiEngine, InspectionPlan>();

    private static final Map<WikiEngine, Float> c_spamLimits = new HashMap<WikiEngine, Float>();

    /**
     * <p>
     * Looks up and returns the spam InspectionPlan for a given WikiEngine. If
     * the InspectionPlan does not exist, it will be created. The InpsectionPlan
     * will add the following Inspectors, in order:
     * </p>
     * <ul>
     * <li>{@link UserInspector}</li>
     * <li>{@link BanListInspector}</li>
     * <li>{@link ChangeRateInspector}</li>
     * <li>{@link LinkCountInspector}</li>
     * <li>{@link BotTrapInspector}</li>
     * <li>{@link AkismetInspector}</li>
     * <li>{@link PatternInspector}</li>
     * </ul>
     * <p>
     * The weights for each Inspector will be determined by examining {@code
     * props}. The property name that indicates the weight to be used is of the
     * form
     * <code>{@value #PROP_INSPECTOR_WEIGHT_PREFIX}<var>fullyQualifiedClassname</var></code>.
     * 
     * @param engine the wiki engine
     * @param props the properties used to initialize the InspectionPlan and
     *            Inspectors
     * @return the InspectionPlan
     */
    public static InspectionPlan getInspectionPlan( WikiEngine engine, Properties props )
    {
        InspectionPlan plan = c_plans.get( engine );
        if( plan != null )
        {
            return plan;
        }

        // Create new InspectionPlan for this WikiEngine
        plan = new InspectionPlan( props );
        plan.addInspector( new UserInspector(), getWeight( props, UserInspector.class ) );
        plan.addInspector( new BanListInspector(), getWeight( props, BanListInspector.class ) );
        plan.addInspector( new ChangeRateInspector(), getWeight( props, ChangeRateInspector.class ) );
        plan.addInspector( new LinkCountInspector(), getWeight( props, LinkCountInspector.class ) );
        plan.addInspector( new BotTrapInspector(), getWeight( props, BotTrapInspector.class ) );
        plan.addInspector( new AkismetInspector(), getWeight( props, AkismetInspector.class ) );
        plan.addInspector( new PatternInspector(), getWeight( props, PatternInspector.class ) );
        c_plans.put( engine, plan );

        // Get the default spam score limits
        float limit = DEFAULT_SCORE_LIMIT;
        String limitString = props.getProperty( PROP_SCORE_LIMIT, String.valueOf( DEFAULT_SCORE_LIMIT ) );
        try
        {
            limit = Float.parseFloat( limitString );
        }
        catch( NumberFormatException e )
        {
            log.error( "Property value " + PROP_SCORE_LIMIT + " did not parse to a float. Using " + DEFAULT_SCORE_LIMIT );
        }
        c_spamLimits.put( engine, Float.valueOf( limit ) );

        return plan;
    }

    /**
     * Determines the desired weight for an Inspector class, based on looking up
     * the value in the WikiEngine Properties
     * @param props the properties used to initialize the WikiEngine
     * @param inspectorClass the Inspector class
     * @return if found, the desired weight; if not, returns {@link #DEFAULT_WEIGHT}
     */
    protected static float getWeight( Properties props, Class<? extends Inspector> inspectorClass )
    {
        String key = PROP_INSPECTOR_WEIGHT_PREFIX + inspectorClass.getCanonicalName();
        float weight = DEFAULT_WEIGHT;
        String weightString = props.getProperty( key, String.valueOf( DEFAULT_WEIGHT ) );
        try
        {
            weight = Float.parseFloat( weightString );
        }
        catch( NumberFormatException e )
        {
        }
        return weight;
    }

    public static float defaultSpamLimit( WikiEngine engine )
    {
        Float limit = c_spamLimits.get( engine );
        return limit == null ? DEFAULT_SCORE_LIMIT : limit.floatValue();
    }

}
