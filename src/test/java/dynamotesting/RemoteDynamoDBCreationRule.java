package dynamotesting;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import trashday.TrashDaySpeechletRequestStreamHandlerTest;
import trashday.storage.DynamoDbClient;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates a connection to the main Amazon DynamoDB cloud for testing.
 * 
 * @author  J. Todd Baldwin
 * @see 	<a href="http://stackoverflow.com/questions/26901613/easier-dynamodb-local-testing">Easier DynamoDB local testing</a>
 */
public class RemoteDynamoDBCreationRule extends ExternalResource {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechletRequestStreamHandlerTest.class);
    /** Connection to the Dynamo DB */
    private AmazonDynamoDBClient amazonDynamoDbClient;
    /** Dynamo DB access for saving and loading user Schedules. */
    private DynamoDbClient trashDayDbClient;

    /** Trash Day application will access Amazon Dynamo DB as a specific IAM user. 
     * The {@link AmazonDynamoDBClient} reads user access keys from this credential
     * file name.  */
    private static final String credentialFile = System.getProperty("user.home") + "/.aws/credentials";
    
    /** Trash Day application will access Amazon Dynamo DB as a specific IAM user.  The
     * credentials for this user are stored in the credentialFile under this
     * profile name. */
    private static final String credentialProfileName = "TrashDaySkill";
    
    /**
     * Rule initialization does not need to perform any configuration
     * for Remote Amazon DB access.  No activities performed in this
     * method.
     */
    public RemoteDynamoDBCreationRule() {
        // This one should be copied during test-compile time. If project's basedir does not contains a folder
        // named 'native-libs' please try '$ mvn clean install' from command line first
    	// log.info("RemoteDynamoCreationRule setting sqlite4java.library.path");
        // System.setProperty("sqlite4java.library.path", "native-libs");
    }

    /**
     * For classes using this rule, create client connections to the
     * Amazon Dynamo DB cloud.
     */
    @Override
    protected void before() throws Throwable {
    	log.info("RemoteDynamoCreationRule before");
    	
    	/*
    	 *  For running tests on a local development computer while accessing
    	 *  the remote Amazon Dynamo DB service, we need to tell it an Amazon
    	 *  IAM user for permissions.  This user must have permissions for
    	 *  Dynamo DB access to the appropriate database table(s).  The
    	 *  ProfileCredentialsProvider allows use to give the access key information
    	 *  for that IAM user.  For your own application, create the IAM user on
    	 *  Amazon.  Then create an access key for that IAM user and install it
    	 *  in a local credentials file under a profile name of your choice.
    	 */
    	ProfileCredentialsProvider pcp = new ProfileCredentialsProvider(credentialFile, credentialProfileName);
    	amazonDynamoDbClient = new AmazonDynamoDBClient(pcp);
    	trashDayDbClient = new DynamoDbClient(amazonDynamoDbClient);
    }

    /**
     * Getter for connection to the Dynamo DB.
     * 
     * @return Connection to the Dynamo DB
     */
    public AmazonDynamoDBClient getAmazonDynamoDBClient() {
    	log.info("RemoteDynamoCreationRule.getAmazonDynamoDBClient()");
        return amazonDynamoDbClient;
    }

    /**
     * Getter for Dynamo DB access for saving and loading user Schedules.
     * 
     * @return Dynamo DB access for saving and loading user Schedules.
     */
    public DynamoDbClient getTrashDayDbClient() {
    	log.info("RemoteDynamoCreationRule.getTrashDayDbClient()");
        return trashDayDbClient;
    }

    /**
     * Getter to determine if our Dynamo DB being used is a local
     * version suitable for test operations.
     * 
     * @return true for a test DB.  false if we're pointing to the
     * 			Amazon Dynamo DB cloud.
     */
    public boolean isTestDB() {
    	return false;
    }
}