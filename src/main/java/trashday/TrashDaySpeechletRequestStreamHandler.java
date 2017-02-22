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
 * All Alexa skills are required to validate that requests are sent by 
 * the correct, registered Amazon skill (see links below).  Since our application
 * code is posted publicly, this application id must be kept secret (i.e. <b>not</b>
 * hard-coded).  Therefore, the Trash Day application <b>requires</b> an environment
 * variable {@code ApplicationId} to be set to the correct value for the 
 * registered Traash Day skill.  If not available, the application will exit
 * before allowing any information to be processed.
 * 
 * @author      J. Todd Baldwin
 * @see			<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#verifying-that-the-request-is-intended-for-your-service">Alexa Skills Kit Doc: Handling Requests Sent By Alexa</a>
 * @see			<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-submission-checklist">Certification Requirements for Custom Skills</a>
 * @see      	<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-security-testing#skills-hosted-as-lambda-functions">Skills Hosted as Lambda Functions</a>
 * @see			<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/handling-requests-sent-by-alexa#verifying-that-the-request-is-intended-for-your-service">Verifying that the Request is Intended for Your Service</a>
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
     * @param tableNameOverride String table name used by JUnit tests to ensure they do not
     * 				write to the Production table name (which is hard-coded using {@literal @}DynamoDBTable in
     * 				{@link trashday.storage.DynamoItem}).  A null value indicates no override.
     * 				Any other value is the Dynamo table name to be used.
	 * @throws IllegalStateException if the environment does not 
	 * 			define "applicationId" then this Alexa skill will 
	 * 			not start.
     */
	public TrashDaySpeechletRequestStreamHandler(AmazonDynamoDBClient db, String tableNameOverride) throws IllegalStateException {
		super(new TrashDaySpeechlet(db, tableNameOverride), supportedApplicationIds);
		if (supportedApplicationIds.size() < 1) {
			throw new IllegalStateException("REQUIRE an environment variable defined for \"ApplicationId\".  Exiting application.");
		}
    }
}
