package dynamotesting;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;

import trashday.TrashDaySpeechletRequestStreamHandlerTest;
import trashday.storage.DynamoDbClient;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Creates a local DynamoDB instance for testing.
 * 
 * @author  J. Todd Baldwin
 * @see 	<a href="http://stackoverflow.com/questions/26901613/easier-dynamodb-local-testing">Easier DynamoDB local testing</a>
 */
public class LocalDynamoDBCreationRule extends ExternalResource {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechletRequestStreamHandlerTest.class);
    /** The local Dynamo DB server */
    private DynamoDBProxyServer server;
    /** Connection to the Dynamo DB */
    private AmazonDynamoDBClient amazonDynamoDbClient;
    /** Dynamo DB access for saving and loading user Schedules. */
    private DynamoDbClient trashDayDbClient;

    /**
     * Rule initialization needs sqlite4java binaries to run a local 
     * Dynamo DB instance. Maven handles downloading the library files,
     * but we need to configure the library path to them with this rule.
     */
    public LocalDynamoDBCreationRule() {
        // This one should be copied during test-compile time. If project's basedir does not contains a folder
        // named 'native-libs' please try '$ mvn clean install' from command line first
    	log.info("LocalDynamoDBCreationRule setting sqlite4java.library.path");
        System.setProperty("sqlite4java.library.path", "native-libs");
    }

    /**
     * For classes using this rule, start a new, local Dynamo DB
     * instance and create client connections to it.
     */
    @Override
    protected void before() throws Throwable {
    	log.info("LocalDynamoDBCreationRule before");
        try {
            final String port = getAvailablePort();
        	log.info("LocalDynamoDBCreationRule availablePort={}", port);
            this.server = ServerRunner.createServerFromCommandLineArgs(new String[]{"-inMemory", "-port", port});
        	log.info("LocalDynamoDBCreationRule server created");
            server.start();
        	log.info("LocalDynamoDBCreationRule server started");
            amazonDynamoDbClient = new AmazonDynamoDBClient(new BasicAWSCredentials("access", "secret"));
        	log.info("LocalDynamoDBCreationRule made new DynamoDB client");
            amazonDynamoDbClient.setEndpoint("http://localhost:" + port);
        	log.info("LocalDynamoDBCreationRule set endpoint");
        	trashDayDbClient = new DynamoDbClient(amazonDynamoDbClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * For classes using this rule, stop the local Dynamo DB
     * instance when the tests are complete.
     */
    @Override
    protected void after() {
    	log.info("LocalDynamoDBCreationRule after");

        if (server == null) {
            return;
        }

        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Getter for connection to the Dynamo DB.
     * 
     * @return Connection to the Dynamo DB
     */
    public AmazonDynamoDBClient getAmazonDynamoDBClient() {
    	log.info("LocalDynamoDBCreationRule.getAmazonDynamoDBClient()");
        return amazonDynamoDbClient;
    }
    
    /**
     * Getter for Dynamo DB access for saving and loading user Schedules.
     * 
     * @return Dynamo DB access for saving and loading user Schedules.
     */
    public DynamoDbClient getTrashDayDbClient() {
    	log.info("LocalDynamoDBCreationRule.getTrashDayDbClient()");
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
    	return true;
    }

    /**
     * Find an unused port to assign to the local Dynamo DB when we
     * start it.
     * 
     * @return Unused local network port
     */
    private String getAvailablePort() {
        try (final ServerSocket serverSocket = new ServerSocket(0)) {
        	String port = String.valueOf(serverSocket.getLocalPort());
        	log.info("LocalDynamoDBCreationRule.getAvailablePort(): port={}", port);
        	return port;
        } catch (IOException e) {
            throw new RuntimeException("Available port was not found", e);
        }
    }
}