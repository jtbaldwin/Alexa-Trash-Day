package trashday;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.lambda.SpeechletRequestStreamHandler;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;

/**
 * Ensure the Alexa request is intended for our service and pass it on
 * to our {@link TrashDaySpeechlet}.
 * <p>
 * This is mostly boilerplate code.  However the constructor from a
 * AmazonDynamoDBClient is added to allow the JUnit tests
 * to use a local Dynamo DB instance for testing purposes only.
 * 
 * @author      J. Todd Baldwin
 * @see			<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#verifying-that-the-request-is-intended-for-your-service">Alexa Skills Kit Doc: Handling Requests Sent By Alexa</a>
 * @since       1.0
 */
public class TrashDaySpeechletRequestStreamHandler extends SpeechletRequestStreamHandler {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechletRequestStreamHandler.class);
    /** Application id(s) for this Trash Day skill */
	private static final Set<String> supportedApplicationIds;

    static {
        supportedApplicationIds = new HashSet<String>();
        String applicationId = System.getenv("ApplicationId");
        if (applicationId != null) {
        	String[] appIds = applicationId.split(",");
        	for (String appId : appIds) {
        		supportedApplicationIds.add(appId);
        	}
        } else {
        	log.error("REQUIRE an environment definition for \"ApplicationId\".");
        }
    }
    
	/** 
     * Handle requests using the Amazon Dynamo DB cloud.  Used for
     * normal Alexa skill requests.
     * <p>
     * CoberturaIgnore directive used since we do not want to
     * build JUnit tests that connect to the actual Amazon
     * Dynamo DB cloud.
     * 
	 * @throws IllegalStateException if the environment does not 
	 * 			define "applicationId" then this Alexa skill will 
	 * 			not start.
     */
    @CoberturaIgnore
	public TrashDaySpeechletRequestStreamHandler() throws IllegalStateException {
		super(new TrashDaySpeechlet(), supportedApplicationIds);
		if (supportedApplicationIds.size() < 1) {
			throw new IllegalStateException("REQUIRE an environment variable defined for \"ApplicationId\".  Exiting application.");
		}
	}

	/** 
     * Handle requests using a specified Dynamo DB.  Used only for
     * JUnit tests at this time.
     *
     * @param db        specific Dynamo DB (instead of the default
     * 					Dynamo DB cloud)
	 * @throws IllegalStateException if the environment does not 
	 * 			define "applicationId" then this Alexa skill will 
	 * 			not start.
     */
	public TrashDaySpeechletRequestStreamHandler(AmazonDynamoDBClient db) throws IllegalStateException {
		super(new TrashDaySpeechlet(db), supportedApplicationIds);
		if (supportedApplicationIds.size() < 1) {
			throw new IllegalStateException("REQUIRE an environment variable defined for \"ApplicationId\".  Exiting application.");
		}
    }
}
