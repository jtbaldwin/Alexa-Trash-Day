package trashday;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.json.SpeechletRequestEnvelope;
import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.speechlet.SpeechletV2;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * Handles routing session requests to the appropriate
 * {@link TrashDayManager} methods.
 * <p>
 * This is mostly boilerplate code from Alexa samples modified to handle
 * the specific Intents for our application.  Additionally, the constructor from a
 * AmazonDynamoDBClient is added to allow the JUnit tests
 * to use a local Dynamo DB instance for testing purposes only.
 * 
 * @author      J. Todd Baldwin
 * @see			<a href="https://github.com/amzn/alexa-skills-kit-java">Java Alexa Skills Kit SDK &amp; Samples</a>
 */

public class TrashDaySpeechlet implements SpeechletV2 {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechlet.class);
    /** The connection used to store the Trash Day pickup schedule. */
    private AmazonDynamoDBClient db;
    /** The methods for appropriately handling our skill Intents. */
    private TrashDayManager tdm = null;
    
	/** 
     * Handle requests using a new connection to the Amazon Dynamo DB
     * cloud.  Used for normal Alexa skill requests.
     * <p>
     * CoberturaIgnore used because we don't want to use JUnit
     * tests that involve the Amazon Dynamo DB cloud that 
     * may contain user data.
     */
    @CoberturaIgnore
    public TrashDaySpeechlet() {
    	//ProfileCredentialsProvider pcp = new ProfileCredentialsProvider(credentialFile, credentialProfileName);
		db = new AmazonDynamoDBClient();
		tdm = new TrashDayManager(db);    	
    }
    
	/** 
     * Handle requests using a specified Dynamo DB.  Used only for
     * JUnit tests at this time.
     *
     * @param db        specific Dynamo DB (instead of the default
     * 					Dynamo DB cloud)
     */
    public TrashDaySpeechlet(AmazonDynamoDBClient db) {
    	this.db = db;
		tdm = new TrashDayManager(db);    	
    }
    
    /**
     * Handle the Alexa request {@code LaunchRequest}.
     * 
     * @param requestEnvelope	SpeechletRequestEnvelope Alexa request information
     * @return			Alexa speech and/or card response
     * @see				<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#types-of-requests-sent-by-alexa">Alexa Skills Kit Docs: Types of Requests Sent by Alexa</a>
     */
	@Override
	public SpeechletResponse onLaunch(SpeechletRequestEnvelope<LaunchRequest> requestEnvelope) {
		LaunchRequest request = requestEnvelope.getRequest();
		Session session = requestEnvelope.getSession();
		log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
		return tdm.handleLaunchRequest(request, session);
	}

    /**
     * Handle the Alexa request {@code IntentRequest}.  Accepts the following
     * intent names:
     * <p>
     * <b>Custom Intents</b>
     * <ul>
     * <li>AddPickupIntent
     * <li>TellScheduleIntent
     * <li>TellNextPickupIntent
     * <li>DeleteEntireScheduleIntent
     * <li>DeleteScheduleIntent
     * </ul>
     * <p>
     * <b>Built-in Intents</b>
     * <ul>
     * <li>AMAZON.YesIntent
     * <li>AMAZON.NoIntent
     * <li>AMAZON.HelpIntent
     * <li>AMAZON.CancelIntent
     * <li>AMAZON.StopIntent
     * </ul>
     * 
     * @param requestEnvelope	SpeechletRequestEnvelope Alexa request information
     * @return			Alexa speech and/or card response
     * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#types-of-requests-sent-by-alexa">Alexa Skills Kit Docs: Types of Requests Sent by Alexa</a>
     * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/built-in-intent-ref/standard-intents">Alexa Skills Kit Docs: Available Standard Built-in Intents</a>
     * 
     */
	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest();
		Session session = requestEnvelope.getSession();
        Intent intent = request.getIntent();
		log.info("onIntent {} requestId={}, sessionId={}", intent.getName(), request.getRequestId(), session.getSessionId());

		// User Queries for information
		if ("TellScheduleIntent".equals(intent.getName())) {
            return tdm.handleTellScheduleRequest(request, session);
        } else if ("TellNextPickupIntent".equals(intent.getName())) {
            return tdm.handleTellNextPickupRequest(request, session);
        } else if ("UpdateScheduleIntent".equals(intent.getName())) {
        	return tdm.handleUpdateScheduleRequest(request, session);
        }
        
        // User Answers to Questions from our skill.
	    else if ("AMAZON.YesIntent".equals(intent.getName())) {
	        return tdm.handleYesRequest(request, session);
	    } else if ("AMAZON.NoIntent".equals(intent.getName())) {
	        return tdm.handleNoRequest(session);
	    }
        
        // User commands to alter the Schedule of weekly pickups
        else if ("SetTimeZoneIntent".equals(intent.getName())) {
            return tdm.handleSetTimeZoneRequest(request, session);

        } else if ("AddPickupIntent".equals(intent.getName())) {
            return tdm.handleAddPickupRequest(request, session);

        } else if ("DeletePickupIntent".equals(intent.getName())) {
            return tdm.handleDeletePickupRequest(request, session);

        } else if ("DeleteEntirePickupIntent".equals(intent.getName())) {
            return tdm.handleDeleteEntirePickupRequest(request, session);

        } else if ("DeleteEntireScheduleIntent".equals(intent.getName())) {
            return tdm.handleDeleteEntireScheduleRequest(request, session);
            
        }
        
        // User gives standard, one-word commands
        else if ("AMAZON.HelpIntent".equals(intent.getName())) {
            return tdm.handleHelpRequest(request, session);

        } else if ("AMAZON.CancelIntent".equals(intent.getName())) {
            return tdm.handleExitRequest(session);

        } else if ("AMAZON.StopIntent".equals(intent.getName())) {
            return tdm.handleExitRequest(session);

        } else {
            throw new IllegalArgumentException("Unrecognized intent: " + intent.getName());
        }
	}

    /**
     * Ignore the Alexa request {@code SessionStartedRequest}.
     * 
     * @param requestEnvelope	SpeechletRequestEnvelope Alexa request information
     * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#types-of-requests-sent-by-alexa">Alexa Skills Kit Docs: Types of Requests Sent by Alexa</a>
     */
	@Override
	public void onSessionStarted(SpeechletRequestEnvelope<SessionStartedRequest> requestEnvelope) {
		SessionStartedRequest request = requestEnvelope.getRequest();
		Session session = requestEnvelope.getSession();
		log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(), session.getSessionId());
	}

    /**
     * Ignore the Alexa request {@code SessionEndedRequest}.
     * 
     * @param requestEnvelope	SpeechletRequestEnvelope Alexa request information
     * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#types-of-requests-sent-by-alexa">Alexa Skills Kit Docs: Types of Requests Sent by Alexa</a>
     */
	@Override
	public void onSessionEnded(SpeechletRequestEnvelope<SessionEndedRequest> requestEnvelope) {
		SessionEndedRequest request = requestEnvelope.getRequest();
		Session session = requestEnvelope.getSession();
		//SessionEndedRequest.Reason.EXCEEDED_MAX_REPROMPTS - User never responded to ask
		//SessionEndedRequest.Reason.USER_INITIATED - User said "exit"
		log.info("onSessionEnded reason={}, error={}, requestId={}, sessionId={}", request.getReason(), request.getError(), request.getRequestId(), session.getSessionId());
		
    	// Update the user's intent log before we exit.
		tdm.flushIntentLog(session);
	}

}
