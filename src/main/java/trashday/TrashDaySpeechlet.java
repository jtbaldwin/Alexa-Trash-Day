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
 * the specific Intents for our application.  Additionally, the {@link #TrashDaySpeechlet(AmazonDynamoDBClient, String)}
 * constructor is added to allow the JUnit tests
 * to use a local Dynamo DB instance for testing purposes only.
 * 
 * @author      J. Todd Baldwin
 * @see			<a href="https://github.com/amzn/alexa-skills-kit-java">Java Alexa Skills Kit SDK &amp; Samples</a>
 */

public class TrashDaySpeechlet implements SpeechletV2 {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechlet.class);
    /** The methods for appropriately handling our skill Intents. */
    private TrashDayManager tdm = null;
    
	/** 
     * Handle requests using a new connection to the Amazon Dynamo DB
     * cloud.  Used for normal Alexa skill requests.
     * <p>
     * {@literal @}{@link trashday.CoberturaIgnore} used because we don't want to use JUnit
     * tests that involve the Amazon Dynamo DB cloud that 
     * may contain user data.
     */
    @CoberturaIgnore
    public TrashDaySpeechlet() {
    	//ProfileCredentialsProvider pcp = new ProfileCredentialsProvider(credentialFile, credentialProfileName);
		tdm = new TrashDayManager(null, null);    	
    }
    
	/** 
     * Handle requests using a specified Dynamo DB and, optionally, a given
     * table for storing user data.  {@code tableNameOverride} is only for
     * JUnit tests at this time.
     *
     * @param db        specific Dynamo DB (instead of the default
     * 					Dynamo DB cloud)
     * @param tableNameOverride String table name used by JUnit tests to ensure they do not
     * 				write to the Production table name (which is hard-coded using 
     * 				{@literal @}{@link com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable} in
     * 				{@link trashday.storage.DynamoItem}).  A null value indicates no override.
     * 				Any other value is the Dynamo table name to be used.
     */
    public TrashDaySpeechlet(AmazonDynamoDBClient db, String tableNameOverride) {
		tdm = new TrashDayManager(db, tableNameOverride);    	
    }
    
    /**
     * Handle the Alexa request when the user says "Alexa, open Trash Day."
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
     * Handle the Alexa request for all the regular user conversation.
     * 
     * @param requestEnvelope	Alexa request data
     * @return Alexa speech and/or card response
     * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#types-of-requests-sent-by-alexa">Alexa Skills Kit Docs: Types of Requests Sent by Alexa</a>
     * @see <a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/built-in-intent-ref/standard-intents">Alexa Skills Kit Docs: Available Standard Built-in Intents</a>
     */
	@Override
	public SpeechletResponse onIntent(SpeechletRequestEnvelope<IntentRequest> requestEnvelope) {
		IntentRequest request = requestEnvelope.getRequest();
		Session session = requestEnvelope.getSession();
        Intent intent = request.getIntent();
		log.info("onIntent {} requestId={}, sessionId={}", intent.getName(), request.getRequestId(), session.getSessionId());

		SpeechletResponse response = null;
		switch (intent.getName()) {
		
		// User Queries for information
		case "TellScheduleIntent":
            response = tdm.handleTellScheduleRequest(request, session);
			break;
		case "TellNextPickupIntent":
			response = tdm.handleTellNextPickupRequest(request, session);
			break;
		case "UpdateScheduleIntent":
			response = tdm.handleUpdateScheduleRequest(request, session);
			break;
        
        // User Answers to Questions from our skill.
		case "AMAZON.YesIntent":
			response = tdm.handleYesRequest(request, session);
			break;
		case "AMAZON.NoIntent":
			response = tdm.handleNoRequest(session);
			break;
        
        // User commands to alter the Schedule of weekly pickups
		case "SetTimeZoneIntent":
			response = tdm.handleSetTimeZoneRequest(request, session);
			break;
		case "AddPickupIntent":
			response = tdm.handleAddPickupRequest(request, session);
			break;
		case "AddWeeklyPickupIntent":
			response = tdm.handleAddWeeklyPickupRequest(request, session);
			break;
		case "AddThisBiWeeklyPickupIntent":
			response = tdm.handleAddThisBiWeeklyPickupRequest(request, session);
			break;
		case "AddFollowingBiWeeklyPickupIntent":
			response = tdm.handleAddFollowingBiWeeklyPickupRequest(request, session);
			break;
		case "AddMonthlyPickupIntent":
			response = tdm.handleAddMonthlyPickupRequest(request, session);
			break;
		case "AddMonthlyLastDayPickupIntent":
			response = tdm.handleAddMonthlyLastDayPickupRequest(request, session);
			break;
		case "AddMonthlyLastNDayPickupIntent":
			response = tdm.handleAddMonthlyLastNDayPickupRequest(request, session);
			break;
		case "AddMonthlyWeekdayPickupIntent":
			response = tdm.handleAddMonthlyWeekdayPickupRequest(request, session);
			break;
		case "AddMonthlyLastNWeekdayPickupIntent":
			response = tdm.handleAddMonthlyLastNWeekdayPickupRequest(request, session);
			break;
		case "DeletePickupIntent":
			response = tdm.handleDeletePickupRequest(request, session);
			break;
		case "DeleteWeeklyPickupIntent":
			response = tdm.handleDeleteWeeklyPickupRequest(request, session);
			break;
		case "DeleteBiWeeklyPickupIntent":
			response = tdm.handleDeleteBiWeeklyPickupRequest(request, session);
			break;
		case "DeleteMonthlyPickupIntent":
			response = tdm.handleDeleteMonthlyPickupRequest(request, session);
			break;
		case "DeleteMonthlyLastDayPickupIntent":
			response = tdm.handleDeleteMonthlyLastDayPickupRequest(request, session);
			break;
		case "DeleteMonthlyLastNDayPickupIntent":
			response = tdm.handleDeleteMonthlyLastNDayPickupRequest(request, session);
			break;
		case "DeleteMonthlyWeekdayPickupIntent":
			response = tdm.handleDeleteMonthlyWeekdayPickupRequest(request, session);
			break;
		case "DeleteMonthlyLastNWeekdayPickupIntent":
			response = tdm.handleDeleteMonthlyLastNWeekdayPickupRequest(request, session);
			break;
		case "DeleteEntirePickupIntent":
			response = tdm.handleDeleteEntirePickupRequest(request, session);
			break;
		case "DeleteEntireScheduleIntent":
			response = tdm.handleDeleteEntireScheduleRequest(request, session);
            break;
        
        // User gives standard, one-word commands
		case "AMAZON.HelpIntent":
			response = tdm.handleHelpRequest(request, session);
			break;
		case "AMAZON.CancelIntent":
			response = tdm.handleExitRequest(session);
			break;
		case "AMAZON.StopIntent":
			response = tdm.handleExitRequest(session);
			break;
			
		// Error if program gets an unexpected Intent Name
        default:
            throw new IllegalArgumentException("Unrecognized intent: " + intent.getName());
		}
		return response;
	}

    /**
     * Ignore this kind of Alexa request.
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
     * Handle the Alexa request when a user session ends.
     * <p>
     * At session end, we flush {@link trashday.model.IntentLog} information.  This
     * records the actions a user performed in order to provide later analysis of
     * how often users use which functions from our skill.
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
