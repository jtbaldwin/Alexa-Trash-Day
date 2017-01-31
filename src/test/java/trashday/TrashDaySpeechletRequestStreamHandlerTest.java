package trashday;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import alexatesting.TestDataAskResponse;
import alexatesting.TestDataGeneralResponse;
import alexatesting.TestDataRequest;
import alexatesting.TestDataTellResponse;
import alexatesting.TestDataUtterance;
import dynamotesting.LocalDynamoDBCreationRule;
import trashday.model.IntentLog;
import trashday.model.Schedule;
import trashday.storage.DynamoItem;
import trashday.storage.SessionDao;
import trashday.ui.responses.Phrases;


/**
 * JUnit tests that work by providing input
 * that emulates the JSON-based requests produced by the
 * Alexa service when users talk to our skill.
 * <p>
 * Most tests work by creating test input in a {@link alexatesting.TestDataRequest}
 * object and expected test output in a {@link alexatesting.TestDataAskResponse}
 * object.  The test input is given to our skill's 
 * handler function.  The resulting JSON output from the
 * handler is then parsed, formatted, and compared to
 * the expected test output to determine if the test is
 * successful.
 * <p>
 * The remaining (non-Test) functions in the class focus
 * on producing test input data and maintaining a uniform 
 * state of the Dynamo DB database between each test.
 * 
 * @author J. Todd Baldwin
 * @see 	<a href="https://github.com/lukas-krecan/JsonUnit">JsonUnit</a>
 */
@RunWith(JUnit4.class)
public class TrashDaySpeechletRequestStreamHandlerTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechletRequestStreamHandlerTest.class);
    
    /** Create a new, local Dynamo DB instance for JUnit testing */
    @ClassRule
    public static final LocalDynamoDBCreationRule dynamoDB = new LocalDynamoDBCreationRule();
    
    /** Database table name for holding the users' saved Schedules. */
	private static final String tableName = "TrashDayScheduleData";
	/** For testing purposes, store data in Dynamo DB using this customerId. */
	private static final String customerId = "testing-only";
	
	/** Format Time Of Day for Response Cards */
	public static final DateTimeFormatter formatterTimeOfDay = DateTimeFormatter.ofPattern("h:mm a");
	/** Format Day for Response Cards */
	public static final DateTimeFormatter formatterDay = DateTimeFormatter.ofPattern("EEEE, MMMM d");

	private static final TimeZone testTimeZone = TimeZone.getTimeZone("US/Eastern");
	/** A Schedule used to ensure database table
	 * stays consistent between tests that alter the
	 * stored schedule. */
	private static Schedule testSchedule;
	/** A Dynamo DB table item used to ensure database table
	 * stays consistent between tests that alter the
	 * stored schedule. */
	private static DynamoItem testScheduleItem;
    /** Our skill's stream handler that accepts JSON-formatted
     * Alexa service requests, hands to our {@link trashday.TrashDaySpeechlet},
     * and produces JSON-formatted Alexa service responses. */
    private static TrashDaySpeechletRequestStreamHandler handler;
    
    /**
     * Before running the JUnit tests in this class, create
     * a new handler for Alexa service requests and create
     * an example Schedule for tests to use.
     */
    @BeforeClass
    public static void setupBeforeClass() {
    	log.info("setupBeforeClass");
        handler = new TrashDaySpeechletRequestStreamHandler(dynamoDB.getAmazonDynamoDBClient());
        
		tableCreate();
		testSchedule = new Schedule();
		testSchedule.initExampleSchedule();	
		
		testScheduleItem = new DynamoItem();
		testScheduleItem.setCustomerId(customerId);
		testScheduleItem.setSchedule(testSchedule);
		testScheduleItem.setTimeZone(testTimeZone);
    }
    
    /**
     * Before running each JUnit test in this class, save the
     * Schedule to the Dynamo DB database.  Ensures all test
     * functions can expect a schedule to exist in the database.
     */
    @Before
    public void setup() {
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        log.trace("Setup Schedule: {}", testScheduleItem.getSchedule().toStringPrintable());
    }

    /**
     * To setup the test environment, this function creates
     * a new Dynamo DB table suitable for storing user
     * Schedules.
     */
    private static void tableCreate() {
		log.info("Create {} table", tableName);
		AttributeDefinition ad = new AttributeDefinition();
		ad.setAttributeName("CustomerId");
		ad.setAttributeType(ScalarAttributeType.S);
		List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
		attributeDefinitions.add(ad);
		ProvisionedThroughput provisionedThroughput = new ProvisionedThroughput();
		provisionedThroughput.setReadCapacityUnits((long) 5);
		provisionedThroughput.setWriteCapacityUnits((long) 5);
		KeySchemaElement kse = new KeySchemaElement();
		kse.setAttributeName("CustomerId");
		kse.setKeyType(KeyType.HASH);
		List<KeySchemaElement> keySchema = new ArrayList<KeySchemaElement>();
		keySchema.add(kse);
		CreateTableResult ctr = null;
		try {
			ctr = dynamoDB.getTrashDayDbClient().getAmazonDynamoDBClient().createTable(attributeDefinitions, tableName, keySchema, provisionedThroughput);
			log.info("Created Table: {}", ctr.toString());
		} catch (ResourceInUseException ex) {
			log.info("table create exception: {}", ex.getErrorMessage());
		}
    }
    
    /**
     * To setup the test environment, this function deletes
     * a Schedule from the Dynamo DB table for the given
     * customer Id.
     * 
     * @param customerId String Which item should be deleted from the table.
     */
	private void itemDelete(String customerId) {
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
        		.withPrimaryKey("CustomerId", customerId);
        
		try {
			AmazonDynamoDBClient client = dynamoDB.getTrashDayDbClient().getAmazonDynamoDBClient();
			log.info("itemDelete client={}", client);
	        DynamoDB dynamoDB = new DynamoDB(client);
			log.info("itemDelete dynamoDB={}", dynamoDB);
	        Table table = dynamoDB.getTable(tableName);
			log.info("itemDelete table={}", table);
            DeleteItemOutcome outcome = table.deleteItem(deleteItemSpec);
            if (outcome==null) {
            	log.info("itemDelete outcome=null");            	
            } else {
            	log.info("itemDelete outcome={}", outcome.toString());
            }
            DeleteItemResult dir = outcome.getDeleteItemResult();
            if (dir==null) {
            	log.info("itemDelete dir=null");            	
            } else {
            	log.info("itemDelete dir={}", dir.toString());
            }
            Item item = outcome.getItem();
            if (item==null) {
            	log.info("itemDelete item=null");            	
            } else {
            	log.info("itemDelete item={}", item.toString());
            }

        } catch (Exception e) {
            log.info("Error deleting item in {}: {},", tableName, e.getMessage());
            //fail("Cannot perform item delete");
        }
	}
	
	/**
	 * Create a list of the user commands needed to create
	 * a new Schedule.  JUnit test functions can feed this
	 * list to the {@link runDialog} method as part of a 
	 * test.
	 * <p>
	 * <b>Test Utterances:</b>
	 * <ul>
	 * <li>SetTimeZoneIntent set time zone to {TimeZone}
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 * @return List of TestDataUtterance objects suitable for the {@link runDialog} method.
	 */
	public List<TestDataUtterance> createDialogNewSchedule() {
		List<TestDataUtterance> dialog = new ArrayList<TestDataUtterance>();
		
		TestDataUtterance tdu = new TestDataUtterance("UpdateScheduleIntent change schedule");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("SetTimeZoneIntent set time zone to {TimeZone}");
		tdu.addSlotValue("TimeZone", "eastern");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}");
		tdu.addSlotValue("PickupName", "trash");
		tdu.addSlotValue("DayOfWeek", "tuesday");
		tdu.addSlotValue("TimeOfDay", "06:30");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}");
		tdu.addSlotValue("PickupName", "trash");
		tdu.addSlotValue("DayOfWeek", "friday");
		tdu.addSlotValue("TimeOfDay", "06:30");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "recycling");
		tdu.addSlotValue("DayOfWeek", "friday");
		tdu.addSlotValue("TimeOfDay", "06:30");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup at {TimeOfDay}on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "lawn waste");
		tdu.addSlotValue("DayOfWeek", "wednesday");
		tdu.addSlotValue("TimeOfDay", "12:00");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup at {TimeOfDay}on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "lawn waste");
		tdu.addSlotValue("DayOfWeek", "wednesday");
		tdu.addSlotValue("TimeOfDay", "12:00");
		dialog.add(tdu);
		
		return dialog;
	}
	
	/**
	 * Create a list of the user commands needed to delete
	 * a items from a Schedule.  JUnit test functions can feed this
	 * list to the {@link runDialog} method as part of a 
	 * test.
	 * <p>
	 * <b>Test Utterances:</b>
	 * <ul>
	 * <li>DeleteEntirePickupIntent delete all {PickupName} pickups
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 * @return List of TestDataUtterance objects suitable for the {@link runDialog} method.
	 */
	public List<TestDataUtterance> createDialogDeleteSchedule() {
		List<TestDataUtterance> dialog = new ArrayList<TestDataUtterance>();
		
		//TestDataUtterance tdu = new TestDataUtterance("DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}");
		//tdu.addSlotValue("PickupName", "trash");
		//tdu.addSlotValue("DayOfWeek", "tuesday");
		//tdu.addSlotValue("TimeOfDay", "06:30");
		//dialog.add(tdu);
		
		TestDataUtterance tdu = new TestDataUtterance("DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "trash");
		tdu.addSlotValue("DayOfWeek", "friday");
		tdu.addSlotValue("TimeOfDay", "06:30");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("DeleteEntirePickupIntent delete all {PickupName} pickups");
		tdu.addSlotValue("PickupName", "recycling");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("DeleteEntirePickupIntent delete all {PickupName} pickups");
		tdu.addSlotValue("PickupName", "whatever");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}");
		tdu.addSlotValue("PickupName", "lawn waste");
		tdu.addSlotValue("DayOfWeek", "wednesday");
		tdu.addSlotValue("TimeOfDay", "12:00");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "trash");
		tdu.addSlotValue("DayOfWeek", "friday");
		tdu.addSlotValue("TimeOfDay", "06:30");
		dialog.add(tdu);
		
		return dialog;
	}
	
	/**
	 * Run a list of user commands ({@link alexatesting.TestDataUtterance} objects)
	 * through the skill handler.  Carries sessionAttribute date beteween the tests.
	 * 
	 * @param dialog List of TestDataUtterances
	 * @param startingResponse TestDataGeneralResponse from any prior test performed.  Used to seed sessionAttributes, if they exist.
	 * @return TestDataGeneralResponse representing the last response received from running all the commands in the dialog.
	 * @throws IOException if the handler has IO problems.
	 */
	public TestDataGeneralResponse runDialog(List<TestDataUtterance> dialog, TestDataGeneralResponse startingResponse) throws IOException {
		TestDataGeneralResponse actualResponse = null;
		if (startingResponse!=null) {
			actualResponse = startingResponse;
		}
    	for (TestDataUtterance tdu: dialog) {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId, tdu);
			testRequest.setSessionAttributes(actualResponse);
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testDialogScript testDataRequest={}", testDataRequestFormatted);
			
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    handler.handleRequest(input, output, null);      
			
		    // Read the actualResponse for this test data.
		    actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellScheduleRequest actualResponse={}", actualResponseFormatted);

			//TODO: Ensure an "OK" response each time.  But for the current tests, just testing the final schedule is adequate.
			//assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
    	}
    	return actualResponse;
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking for "Help."
	 * <p>
	 * Tested Intent: AMAZON.HelpIntent
	 */
	@Test
	public void testHelp() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.HelpIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testHelp testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrases.HELP_VERBAL_SCHEDULE_EXISTS.toString()+Phrases.HELP_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.HELP_REPROMPT_SCHEDULE_EXISTS);
		expectedResponse.setResponseCardTitle("Trash Day Help");
		expectedResponse.setResponseCardContent(Phrases.HELP_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "help");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, "${json-unit.ignore}");			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testHelp expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testHelp actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "Cancel."
	 * <p>
	 * Tested Intent: AMAZON.CancelIntent
	 */
	@Test
	public void testCancel() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.CancelIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testCancel testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Goodbye");
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testCancel expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testCancel actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "Stop"
	 * <p>
	 * Tested Intent: AMAZON.StopIntent
	 */
	@Test
	public void testStop() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.StopIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testStop testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Goodbye");
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testStop expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testStop actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	@Test
	public void testChangeSchedule() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("UpdateScheduleIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testChangeSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrases.SCHEDULE_ALTER_VERBAL);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT);
		//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
		//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD.toString());
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testChangeSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testChangeSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "open Trash Day" for a missing
	 * schedule.
	 * <p>
	 * Tested Request Type: LaunchRequest
	 * <ul>
	 * <li>Alexa, open Trash Day
	 * </ul>
	 */
	@Test
	public void testLaunchWelcome() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestType("LaunchRequest");
				testRequest.removeRequestIntent();
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testLaunchScheduleMissing testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Welcome to Trash Day. Please " + Phrases.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT.toString());
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testLaunchScheduleMissing expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testLaunchScheduleMissing actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneScheduleExists() {
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","pacific");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("Time zone set to US/Pacific.");
			expectedResponse.deleteResponseCard();
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	testScheduleItem.setTimeZone(testTimeZone);
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneScheduleExistsCIP() {
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","pacific");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Time zone set to US/Pacific. Next, "+Phrases.SCHEDULE_ALTER_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT);
			//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
			expectedResponse.deleteResponseCard();
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondTimeZoneUpdatedScheduleExists");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Pacific");
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	testScheduleItem.setTimeZone(testTimeZone);
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneScheduleMissing() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","pacific");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Time zone set to US/Pacific. Next, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT);
			//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD.toString());
			expectedResponse.deleteResponseCard();
			TimeZone expectedTimeZone = TimeZone.getTimeZone("US/Pacific");
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondTimeZoneUpdatedScheduleMissing");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, expectedTimeZone);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneMissing() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("I missed the Time Zone information.\n"+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseCardTitle("Trash Day");
			expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneMissingCIP() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("I missed the Time Zone information.\n"+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT);
			expectedResponse.setResponseCardTitle("Trash Day");
			expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, true);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneInvalid() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","garbage_zone_name");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("I didn't understand the Time Zone information.\n"+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	@Test
	public void testSetTimeZoneInvalidCIP() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","garbage_zone_name");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testSetTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("I didn't understand the Time Zone information.\n"+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT);
			expectedResponse.setResponseCardTitle("Trash Day");
			expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, true);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testSetTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testSetTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "open Trash Day" for an empty
	 * schedule, but timezone defined.
	 * <p>
	 * Tested Request Type: LaunchRequest
	 * <ul>
	 * <li>Alexa, open Trash Day
	 * </ul>
	 */
	@Test
	public void testLaunchScheduleEmpty() {
		// Remove the existing schedule pickups, timezone data will remain.
        testSchedule.deleteEntireSchedule();
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestType("LaunchRequest");
			testRequest.removeRequestIntent();
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testLaunchScheduleEmpty testDataRequest={}", testDataRequestFormatted);

			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT);
			//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testLaunchScheduleEmpty expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testLaunchScheduleEmpty actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        }
        finally {
	       	// Restore the schedule
	       	testSchedule.initExampleSchedule();
	        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
	    }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "open Trash Day" for an
	 * example schedule.
	 * <p>
	 * Tested Request Type: LaunchRequest
	 * <ul>
	 * <li>Alexa, open Trash Day
	 * </ul>
	 */
	@Test
	public void testLaunchScheduleConfigured() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestType("LaunchRequest");
		testRequest.removeRequestIntent();
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testLaunch testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrases.OPEN_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.OPEN_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "open");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testLaunch expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testLaunch actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "Yes" *without* a previous
	 * question they are answering.
	 * <p>
	 * Tested Intent: AMAZON.YesIntent
	 */
	@Test
	public void testYesWithoutPriorQuestion() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.YesIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testYesWithoutPriorQuestion testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Sorry.  I didn't understand what question you were answering.  Please say \"help\" for what things you can say.");
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testYesWithoutPriorQuestion expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testYesWithoutPriorQuestion actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "open Trash Day" and "exit"
	 * <p>
	 * Tested Request Type: LaunchRequest
	 * <ul>
	 * <li>Alexa, open Trash Day
	 * </ul>
	 */
	@Test
	public void testLaunchAndExit() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestType("LaunchRequest");
		testRequest.removeRequestIntent();
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testLaunchAndExit testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrases.OPEN_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.OPEN_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "open");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testLaunchAndExit expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testLaunchAndExit actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
		
		// Modify testRequest for this test.
		testRequest = new TestDataRequest(customerId);
		testRequest.setRequestType("SessionEndedRequest");
		testRequest.setRequestReason("USER_INITIATED");
		testRequest.removeRequestIntent();
		testDataRequestFormatted = testRequest.toString();
		log.debug("testLaunchAndExit testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedTellResponse = new TestDataTellResponse();
		expectedTellResponse.removeResponse();
		expectedResponseFormatted = expectedTellResponse.toString();
		log.debug("testLaunchAndExit expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    actualResponse = new TestDataGeneralResponse(output.toString());
		actualResponseFormatted = actualResponse.toString();
		log.info("testLaunchAndExit actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user answering "Yes" to a question the code
	 * does not know how to answer.
	 * <p>
	 * Tested Intent: AMAZON.YesIntent
	 */
	@Test
	public void testYesToUnknownQuestion() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.YesIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DoSomethingIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "perform new, unknown action");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testYesToUnknownQuestion testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Uh-oh, I found a programming problem.  Cannot perform new, unknown action");
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testYesToUnknownQuestion expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testYesToUnknownQuestion actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	@Test
	public void testTellScheduleEmpty() {
		// Remove the existing schedule pickups, timezone data will remain.
        testSchedule.deleteEntireSchedule();
        testScheduleItem.setSchedule(testSchedule);
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("TellScheduleIntent");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testTellScheduleEmpty testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL.toString());
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT.toString());
			//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testTellScheduleEmpty expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellScheduleEmpty actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        }
        finally {
	       	// Restore the schedule
	       	testSchedule.initExampleSchedule();
	        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
	    }

	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "tell me the schedule" for an
	 * existing schedule.
	 * <p>
	 * Tested Intent: TellScheduleIntent
	 * <ul>
	 * <li>TellScheduleIntent tell me the schedule
	 * <li>TellScheduleIntent tell me the pickup schedule
	 * <li>TellScheduleIntent what is the schedule
	 * <li>TellScheduleIntent what is the pickup schedule
	 * </ul>
	 */
	@Test
	public void testTellScheduleExisting() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("TellScheduleIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testTellScheduleExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Pickup trash on Tuesday 6 30 AM and Friday 6 30 AM. Pickup recycling on Friday 6 30 AM. Pickup lawn waste on Wednesday noon. ");
		expectedResponse.setResponseCardTitle("Trash Day Schedule");
		expectedResponse.setResponseCardContent("As of Thursday, November 24 at 10:27 AM:\nPickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.\nPickup recycling on Friday at 6:30 AM.\nPickup lawn waste on Wednesday at noon.\n");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testTellScheduleExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testTellScheduleExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	@Test
	public void testTellScheduleExistingNoTimeZone() {
		// Remove the existing schedule pickups, timezone data will remain.
        itemDelete(customerId);
		testScheduleItem.clearTimeZone();
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);

        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("TellScheduleIntent");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testTellScheduleExistingNoTimeZone testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The time zone isn't configured yet. "+Phrases.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT);
			//expectedResponse.setResponseCardTitle("Trash Day Set Time Zone");
			//expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testTellScheduleExistingNoTimeZone expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellScheduleExistingNoTimeZone actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        }
        finally {
	       	// Restore the schedule
			testScheduleItem.setTimeZone(testTimeZone);
	        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
	    }

	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "tell me the schedule" for a
	 * missing schedule.
	 * <p>
	 * Tested Intent: TellScheduleIntent
	 * <ul>
	 * <li>TellScheduleIntent tell me the schedule
	 * <li>TellScheduleIntent tell me the pickup schedule
	 * <li>TellScheduleIntent what is the schedule
	 * <li>TellScheduleIntent what is the pickup schedule
	 * </ul>
	 */
	@Test
	public void testTellDBItemMissing() {
		// Remove the existing schedule in database.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("TellScheduleIntent");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testTellDBItemMissing testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Welcome to Trash Day. Please " + Phrases.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT);
			//expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testTellDBItemMissing expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellDBItemMissing actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
		
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "tell me the schedule" for a
	 * missing schedule, but tiemzone already set.
	 * <p>
	 * Tested Intent: TellScheduleIntent
	 * <ul>
	 * <li>TellScheduleIntent tell me the schedule
	 * <li>TellScheduleIntent tell me the pickup schedule
	 * <li>TellScheduleIntent what is the schedule
	 * <li>TellScheduleIntent what is the pickup schedule
	 * </ul>
	 */
	@Test
	public void testTellScheduleMissingTimeZoneExisting() {
		// Remove the existing schedule in database.
        itemDelete(customerId);
        
        // Add an entry with just one item.
		TimeZone timeZone = TimeZone.getTimeZone("US/Eastern");
        DynamoItem item = new DynamoItem();
        item.setCustomerId(customerId);
        item.setTimeZone(timeZone);
        dynamoDB.getTrashDayDbClient().saveItem(item);
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("TellScheduleIntent");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testTellScheduleMissingTimeZoneExisting testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL);
			expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT);
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testTellScheduleMissingTimeZoneExisting expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellScheduleMissingTimeZoneExisting actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
		
        } finally {
        	// Ensure the modified item is removed.
            itemDelete(customerId);

        	// Save the correct item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "when is the next pickup" for an
	 * example schedule.
	 * <p>
	 * Tested Intent: TellNextPickupIntent
	 * <ul>
	 * <li>TellNextPickupIntent when is next pickup
	 * </ul>
	 */
	@Test
	public void testTellNextPickups() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testTellNextPickups testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next trash pickup is tomorrow at 6 30 AM. Next recycling pickup is tomorrow at 6 30 AM. Next lawn waste pickup is Wednesday at noon. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Thursday, November 24 at 10:27 AM:\nNext trash pickup is tomorrow at 6:30 AM.\nNext recycling pickup is tomorrow at 6:30 AM.\nNext lawn waste pickup is Wednesday at noon.\n");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testTellNextPickups expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testTellNextPickups actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "when is the next pickup" for an
	 * example schedule and that the resulting pickups are
	 * spoken in the order of "next pickup times", not in
	 * pickup name order.
	 * <p>
	 * Tested Intent: TellNextPickupIntent
	 * <ul>
	 * <li>TellNextPickupIntent when is next pickup
	 * </ul>
	 */
	@Test
	public void testTellNextPickupsInOrder() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.setRequestTimestamp("2017-01-11T10:27:15Z"); // Wednesday morning
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testTellNextPickups testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next lawn waste pickup is today at noon. Next trash pickup is Friday at 6 30 AM. Next recycling pickup is Friday at 6 30 AM. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Wednesday, January 11 at 5:27 AM:\nNext lawn waste pickup is today at noon.\nNext trash pickup is Friday at 6:30 AM.\nNext recycling pickup is Friday at 6:30 AM.\n");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testTellNextPickups expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testTellNextPickups actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "when is the next {pickupName} pickup" for an
	 * example schedule when the pickup exists.
	 * <p>
	 * Tested Intent: TellNextPickupIntent
	 * <ul>
	 * <li>TellNextPickupIntent when is next {PickupName} pickup
	 * </ul>
	 */
	@Test
	public void testTellNextPickupSingle() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testTellNextPickupSingle testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next trash pickup is tomorrow at 6 30 AM. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Thursday, November 24 at 10:27 AM:\nNext trash pickup is tomorrow at 6:30 AM.\n");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testTellNextPickupSingle expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testTellNextPickupSingle actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	@Test
	public void testTellNextPickupsScheduleEmpty() {
		// Remove the existing schedule pickups, timezone data will remain.
        testSchedule.deleteEntireSchedule();
        testScheduleItem.setSchedule(testSchedule);
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("TellNextPickupIntent");
			//testRequest.addRequestIntentSlot("PickupName","trash");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testTellNextPickupSingle testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT);
			//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testTellNextPickupSingle expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellNextPickupSingle actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        }
        finally {
	       	// Restore the schedule
	       	testSchedule.initExampleSchedule();
	        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
	    }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "when is the next {pickupName} pickup" for an
	 * example schedule when the pickup does not exist.
	 * <p>
	 * Tested Intent: TellNextPickupIntent
	 * <ul>
	 * <li>TellNextPickupIntent when is next {PickupName} pickup
	 * </ul>
	 */
	@Test
	public void testTellNextPickupMissing() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.addRequestIntentSlot("PickupName","bad name");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testTellNextPickupMissing testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("No bad name pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Thursday, November 24 at 10:27 AM, no bad name pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testTellNextPickupMissing expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testTellNextPickupMissing actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "when is the next {pickupName} pickup" for an
	 * example schedule when the pickup does not exist.
	 * <p>
	 * Tested Intent: TellNextPickupIntent
	 * <ul>
	 * <li>TellNextPickupIntent when is next {PickupName} pickup
	 * </ul>
	 */
	@Test
	public void testTellNextPickupScheduleEmpty() {
		// Remove the existing schedule pickups.
        testSchedule.deleteEntireSchedule();
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("TellNextPickupIntent");
				String testDataRequestFormatted = testRequest.toString();
			log.debug("testTellNextPickupMissing testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL.toString());
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT.toString());
			//expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			//expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD.toString());
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testTellNextPickupMissing expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testTellNextPickupMissing actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        }
        finally {
        	// Restore the schedule
        	testSchedule.initExampleSchedule();
            dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup at {TimeOfDay} on today"
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupToday() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","today");
		testRequest.addRequestIntentSlot("TimeOfDay","13:30");
		testRequest.addRequestIntentSlot("PickupName","lawn waste");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupToday testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Added lawn waste pickup on Thursday at 1 30 PM.");
		expectedResponse.deleteResponseCard();
		Schedule expandedSchedule = new Schedule();
		expandedSchedule.initExampleSchedule();
		expandedSchedule.addPickupSchedule("lawn waste", DayOfWeek.THURSDAY, LocalTime.of(13, 30));
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupToday expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupToday actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup tomorrow at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupTomorrow() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","tomorrow");
		testRequest.addRequestIntentSlot("TimeOfDay","13:30");
		testRequest.addRequestIntentSlot("PickupName","lawn waste");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupTomorrow testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Added lawn waste pickup on Friday at 1 30 PM.");
		//expectedResponse.setResponseCardContent("Added lawn waste pickup on Friday at 1:30 PM.");
		expectedResponse.deleteResponseCard();
		Schedule expandedSchedule = new Schedule();
		expandedSchedule.initExampleSchedule();
		expandedSchedule.addPickupSchedule("lawn waste", DayOfWeek.FRIDAY, LocalTime.of(13, 30));
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupTomorrow expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupTomorrow actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupTime() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Monday");
		testRequest.addRequestIntentSlot("TimeOfDay","16:00");
		testRequest.addRequestIntentSlot("PickupName","mail");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupTime testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Added mail pickup on Monday at 4 PM.");
		expectedResponse.deleteResponseCard();
		Schedule expandedSchedule = new Schedule();
		expandedSchedule.initExampleSchedule();
		expandedSchedule.addPickupSchedule("mail", DayOfWeek.MONDAY, LocalTime.of(16, 00));
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupTime expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupTime actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	@Test
	public void testAddPickupTimeNothingConfigured() {
		// Remove the existing schedule in database.
		// No entries implies new user.
        itemDelete(customerId);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testAddPickupTime testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Welcome to Trash Day. Please " + Phrases.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT);
			//expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testAddPickupTime expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testAddPickupTime actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }

	}
	
	@Test
	public void testAddPickupTimeTimeZoneMissing() {
		// Remove the existing time zone, schedule data will remain.
        itemDelete(customerId);
        testScheduleItem.clearTimeZone();
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testAddPickupTime testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The time zone isn't configured yet. " + Phrases.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrases.TIME_ZONE_SET_REPROMPT);
			//expectedResponse.setResponseCardTitle("Trash Day Set Time Zone");
			//expectedResponse.setResponseCardContent(Phrases.TIME_ZONE_SET_HELP_CARD);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testAddPickupTime expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testAddPickupTime actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } finally {
	       	// Restore the time zone
	       	testScheduleItem.setTimeZone(testTimeZone);
	        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }

	}
	
	@Test
	public void testAddPickupTimeCIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Monday");
		testRequest.addRequestIntentSlot("TimeOfDay","16:00");
		testRequest.addRequestIntentSlot("PickupName","mail");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupTime testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Added mail pickup on Monday at 4 PM. To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT.toString());
		Schedule expandedSchedule = new Schedule();
		expandedSchedule.initExampleSchedule();
		expandedSchedule.addPickupSchedule("mail", DayOfWeek.MONDAY, LocalTime.of(16, 00));
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondPickupAddSingle");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, "${json-unit.ignore}");			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupTime expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupTime actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is missing.
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupMissingData1() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("PickupName","mail");
		testRequest.addRequestIntentSlot("DayOfWeek","Monday");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	@Test
	public void testAddPickupMissingData1CIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("PickupName","mail");
		testRequest.addRequestIntentSlot("DayOfWeek","Monday");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondPickupAddMissingData");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is missing.
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupMissingData2() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("PickupName","mail");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Day Of Week and Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is missing.
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupMissingData3() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Pickup Name, Day Of Week, and Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup name is blank.
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupBlankData() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","");
		testRequest.addRequestIntentSlot("TimeOfDay"," ");
		testRequest.addRequestIntentSlot("PickupName"," ");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupBlankName testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Pickup Name, Day Of Week, and Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupBlankName expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupBlankName actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is invalid.
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupInvalidData1() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","yesterday");
		testRequest.addRequestIntentSlot("TimeOfDay","seven");
		testRequest.addRequestIntentSlot("PickupName","mail");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupInvalidData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I didn\u0027t understand the Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupInvalidData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupInvalidData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is invalid.
	 * <p>
	 * Tested Intent: AddPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupInvalidData2() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","bad");
		testRequest.addRequestIntentSlot("TimeOfDay","garbage");
		testRequest.addRequestIntentSlot("PickupName","mail");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupInvalidData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I didn\u0027t understand the Day Of Week and Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupInvalidData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupInvalidData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	@Test
	public void testAddPickupInvalidData2CIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AddPickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","bad");
		testRequest.addRequestIntentSlot("TimeOfDay","garbage");
		testRequest.addRequestIntentSlot("PickupName","mail");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testAddPickupInvalidData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("I didn\u0027t understand the Day Of Week and Time Of Day information.\n"+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondPickupAddInvalidData");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testAddPickupInvalidData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testAddPickupInvalidData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeExisting() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("TimeOfDay","06:30");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Removed trash pickup on Friday at 6 30 AM from the weekly schedule.");
		expectedResponse.deleteResponseCard();
		Schedule smallerSchedule = new Schedule();
		smallerSchedule.initExampleSchedule();
		smallerSchedule.deletePickupTime("trash", DayOfWeek.FRIDAY, LocalTime.of(6, 30));
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	@Test
	public void testDeletePickupTimeExistingCIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("TimeOfDay","06:30");
		testRequest.addRequestIntentSlot("PickupName","trash");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Removed trash pickup on Friday at 6 30 AM from the weekly schedule. To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondPickupDeleteSingle");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, "${json-unit.ignore}");			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		Schedule smallerSchedule = new Schedule();
		smallerSchedule.initExampleSchedule();
		smallerSchedule.deletePickupTime("trash", DayOfWeek.FRIDAY, LocalTime.of(6, 30));
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeAlsoExisting() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("TimeOfDay","06:30");
		testRequest.addRequestIntentSlot("PickupName","recycling");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeAlsoExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Removed recycling pickup on Friday at 6 30 AM from the weekly schedule.");
		expectedResponse.deleteResponseCard();
		Schedule smallerSchedule = new Schedule();
		smallerSchedule.initExampleSchedule();
		smallerSchedule.deletePickupTime("recycling", DayOfWeek.FRIDAY, LocalTime.of(6, 30));
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeAlsoExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeAlsoExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup time does not exist.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeNonExisting() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("TimeOfDay","12:30");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeNonExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("There was no trash pickup scheduled on Friday at 12 30 PM.");
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeNonExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeNonExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is missing.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeMissingData1() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD.toString());
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	@Test
	public void testDeletePickupTimeMissingData1CIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("PickupName","trash");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeMissingData1CIP testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_DELETE_PICKUPS_REPROMPT);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD.toString());
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondPickupDeleteMissingData");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeMissingData1CIP expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is missing.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeMissingData2() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Day Of Week and Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is missing.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeMissingData3() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the Pickup Name, Day Of Week, and Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is invalid.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeInvalidData1() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("TimeOfDay","25:30");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeInvalidData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I didn't understand the Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeInvalidData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeInvalidData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	@Test
	public void testDeletePickupTimeInvalidData1CIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Friday");
		testRequest.addRequestIntentSlot("TimeOfDay","25:30");
		testRequest.addRequestIntentSlot("PickupName","trash");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeInvalidData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("I didn't understand the Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_DELETE_PICKUPS_REPROMPT);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondPickupDeleteInvalidData");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeInvalidData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeInvalidData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when one or more of the data fields is invalid.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testDeletePickupTimeInvalidData2() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeletePickupIntent");
		testRequest.addRequestIntentSlot("DayOfWeek","Someday");
		testRequest.addRequestIntentSlot("TimeOfDay","25:30");
		testRequest.addRequestIntentSlot("PickupName","trash");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupTimeInvalidData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I didn't understand the Day Of Week and Time Of Day information.\n"+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupTimeInvalidData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupTimeInvalidData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete all {PickupName} pickups" 
	 * when the pickup already exists.
	 * <p>
	 * Tested Intent: DeleteEntirePickupIntent
	 * <ul>
	 * <li>DeleteEntirePickupIntent delete all {PickupName} pickups
	 * </ul>
	 */
	@Test
	public void testDeletePickupExisting() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntirePickupIntent");
		testRequest.addRequestIntentSlot("PickupName","recycling");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Removed all recycling pickups from the schedule.");
		expectedResponse.deleteResponseCard();
		Schedule smallerSchedule = new Schedule();
		smallerSchedule.initExampleSchedule();
		smallerSchedule.deleteEntirePickup("recycling");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	@Test
	public void testDeletePickupExistingCIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntirePickupIntent");
		testRequest.addRequestIntentSlot("PickupName","recycling");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Removed all recycling pickups from the schedule. To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		Schedule smallerSchedule = new Schedule();
		smallerSchedule.initExampleSchedule();
		smallerSchedule.deleteEntirePickup("recycling");
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondEntirePickupDelete");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, smallerSchedule.toJson());			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete all {PickupName} pickups" 
	 * when the pickup does not exist.
	 * <p>
	 * Tested Intent: DeleteEntirePickupIntent
	 * <ul>
	 * <li>DeleteEntirePickupIntent delete all {PickupName} pickups
	 * </ul>
	 */
	@Test
	public void testDeletePickupNonExisting() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntirePickupIntent");
		testRequest.addRequestIntentSlot("PickupName","whatever");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupNonExisting testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("There was no whatever pickup in the schedule.");
		expectedResponse.deleteResponseCard();
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupNonExisting expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupNonExisting actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete all {PickupName} pickups" 
	 * when the pickup name data is missing.
	 * <p>
	 * Tested Intent: DeleteEntirePickupIntent
	 * <ul>
	 * <li>DeleteEntirePickupIntent delete all {PickupName} pickups
	 * </ul>
	 */
	@Test
	public void testDeletePickupMissingData() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntirePickupIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the pickup name information.\n"+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	@Test
	public void testDeletePickupMissingDataCIP() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntirePickupIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeletePickupMissingData testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("I missed the pickup name information.\n"+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT);
		expectedResponse.setResponseCardContent(Phrases.SCHEDULE_ALTER_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "respondEntirePickupDeleteMissingName");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeletePickupMissingData expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeletePickupMissingData actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user has confirmed with "Yes" after asking to 
	 * "delete entire schedule" 
	 * <p>
	 * Tested Intent: DeleteEntireScheduleIntent and AMAZON.YesIntent
	 * <ul>
	 * <li>DeleteEntireScheduleIntent delete entire schedule
	 * <li>Yes
	 * </ul>
	 */
	@Test
	public void testDeleteScheduleYes() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeleteSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
		expectedResponse.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeleteSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeleteSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
		
		// Modify testRequest for this test.
		testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.YesIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
		testDataRequestFormatted = testRequest.toString();
		log.debug("testDeleteSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedTellResponse = new TestDataTellResponse();
		expectedTellResponse.setResponseOutputSpeechText("Cleared entire schedule.");
		expectedTellResponse.deleteResponseCard();
		expectedResponseFormatted = expectedTellResponse.toString();
		log.debug("testDeleteSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    actualResponse = new TestDataGeneralResponse(output.toString());
		actualResponseFormatted = actualResponse.toString();
		log.info("testDeleteSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user has confirmed with "Yes" after asking to 
	 * "delete entire schedule" 
	 * <p>
	 * Tested Intent: DeleteEntireScheduleIntent and AMAZON.YesIntent
	 * <ul>
	 * <li>DeleteEntireScheduleIntent delete entire schedule
	 * <li>Yes
	 * </ul>
	 */
	@Test
	public void testDeleteScheduleEmpty() {
		// Remove the existing schedule pickups.
        testSchedule.deleteEntireSchedule();
        dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        
        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testDeleteScheduleEmpty testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testDeleteScheduleEmpty expectedResponse={}", expectedResponseFormatted);
	
			// Run the testRequest through the handler.
		    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
		    OutputStream output = new ByteArrayOutputStream();
		    try {
		    	handler.handleRequest(input, output, null);      
		    } catch (IOException ex) {
		    	fail("IOException: "+ex.getMessage());
		    }
			
		    // Read the actualResponse for this test data.
		    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
			String actualResponseFormatted = actualResponse.toString();
			log.info("testDeleteScheduleEmpty actualResponse={}", actualResponseFormatted);
			
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        }
        finally {
        	// Restore the schedule
        	testSchedule.initExampleSchedule();
            dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user has cancelled with "No" after asking to 
	 * "delete entire schedule" 
	 * <p>
	 * Tested Intent: DeleteEntireScheduleIntent and AMAZON.NoIntent
	 * <ul>
	 * <li>DeleteEntireScheduleIntent delete entire schedule
	 * <li>No
	 * </ul>
	 */
	@Test
	public void testDeleteScheduleNo() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeleteSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
		expectedResponse.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeleteSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeleteSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
		
		// Modify testRequest for this test.
		testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.NoIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
		testDataRequestFormatted = testRequest.toString();
		log.debug("testDeleteSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedTellResponse = new TestDataTellResponse();
		expectedTellResponse.setResponseOutputSpeechText("Cancelling the delete entire schedule");
		expectedTellResponse.deleteResponseCard();
		expectedResponseFormatted = expectedTellResponse.toString();
		log.debug("testDeleteSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    actualResponse = new TestDataGeneralResponse(output.toString());
		actualResponseFormatted = actualResponse.toString();
		log.info("testDeleteSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user has cancelled with "Cancel" after asking to 
	 * "delete entire schedule" 
	 * <p>
	 * Tested Intent: DeleteEntireScheduleIntent and AMAZON.CancelIntent
	 * <ul>
	 * <li>DeleteEntireScheduleIntent delete entire schedule
	 * <li>Cancel
	 * </ul>
	 */
	@Test
	public void testDeleteScheduleCancel() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDeleteSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
		expectedResponse.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
		expectedResponse.deleteResponseCard();		
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, testSchedule.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
		String expectedResponseFormatted = expectedResponse.toString();
		log.debug("testDeleteSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    TestDataGeneralResponse actualResponse = new TestDataGeneralResponse(output.toString());
		String actualResponseFormatted = actualResponse.toString();
		log.info("testDeleteSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
		
		// Modify testRequest for this test.
		testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("AMAZON.CancelIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
		testDataRequestFormatted = testRequest.toString();
		log.debug("testDeleteSchedule testDataRequest={}", testDataRequestFormatted);

		// Modify expectedResponse for this test.
		TestDataTellResponse expectedTellResponse = new TestDataTellResponse();
		expectedTellResponse.setResponseOutputSpeechText("Cancelling the delete entire schedule");
		expectedTellResponse.deleteResponseCard();
		expectedResponseFormatted = expectedTellResponse.toString();
		log.debug("testDeleteSchedule expectedResponse={}", expectedResponseFormatted);

		// Run the testRequest through the handler.
	    input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    }
		
	    // Read the actualResponse for this test data.
	    actualResponse = new TestDataGeneralResponse(output.toString());
		actualResponseFormatted = actualResponse.toString();
		log.info("testDeleteSchedule actualResponse={}", actualResponseFormatted);
		
		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user when it does not recognize the Intent
	 * received. 
	 * <p>
	 * Tested Intent: NewUnknownIntent
	 * <ul>
	 * <li>NewUnknownIntent Some unknown, new phrase we did not handle
	 * </ul>
	 */
	@Test
	public void testIntentUnrecognized() {
		// Modify testRequest for this test.
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("NewUnknownIntent");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testIntentUnrecognized testDataRequest={}", testDataRequestFormatted);
		
		// Run the testRequest through the handler.
	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
	    OutputStream output = new ByteArrayOutputStream();
	    try {
	    	handler.handleRequest(input, output, null);      
	    } catch (IOException ex) {
	    	fail("IOException: "+ex.getMessage());
	    } catch (IllegalArgumentException e) {
	    	String expectedException = "Unrecognized intent: NewUnknownIntent";
	    	String actualException = e.getLocalizedMessage();
	    	log.info("testIntentUnrecognized: actualException={}", actualException);
	    	assertEquals(expectedException, actualException);
	    	return;
	    }
	    fail("Did not get exception on unrecognized intent.");
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user when they build a new schedule with a 
	 * series of "AddPickup" commands.
	 * <p>
	 * Tested Intent: AddPickupIntent and TellScheduleIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * <li>TellScheduleIntent what is the schedule
	 * <li>exit
	 * </ul>
	 */
	@Test
	public void testDialogCreateScheduleAndExit() {
		List<TestDataUtterance> dialog = createDialogNewSchedule();
		
		// Remove the existing schedule in database.
        itemDelete(customerId);
        
        try {
        	TestDataGeneralResponse actualResponse = runDialog(dialog, null);

        	// Finally, check the resulting schedule.
    		// Modify testRequest for this test.
    		TestDataRequest testRequest = new TestDataRequest(customerId);
    		testRequest.setRequestIntentName("TellScheduleIntent");
    		testRequest.setSessionAttributes(actualResponse);
    		String testDataRequestFormatted = testRequest.toString();
    		log.debug("testDialogCreateScheduleAndExit testDataRequest={}", testDataRequestFormatted);

    		// Modify expectedResponse for this test.
    		TestDataAskResponse expectedResponse = new TestDataAskResponse();
    		expectedResponse.setResponseOutputSpeechText("Pickup trash on Tuesday 6 30 AM and Friday 6 30 AM. Pickup recycling on Friday 6 30 AM. Pickup lawn waste on Wednesday noon.  To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL);
    		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT);
    		//expectedResponse.setResponseCardTitle("Trash Day Schedule");
    		//expectedResponse.setResponseCardContent("As of Thursday, November 24 at 10:27 AM:\nPickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.\nPickup recycling on Friday at 6:30 AM.\nPickup lawn waste on Wednesday at noon.\n");
    		expectedResponse.deleteResponseCard();
//    		IntentLog intentLog = new IntentLog();
//    		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
//    		intentLog.incrementIntent(ldtRequest, "respondPickupAddSingle", 5);
//    		intentLog.incrementIntent(ldtRequest, "respondTimeZoneUpdatedScheduleMissing");
//    		intentLog.incrementIntent(ldtRequest, "tellSchedule");
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, "${json-unit.ignore}");
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, "${json-unit.ignore}");			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
    		String expectedResponseFormatted = expectedResponse.toString();
    		log.debug("testDialogCreateScheduleAndExit expectedResponse={}", expectedResponseFormatted);

    		// Run the testRequest through the handler.
    	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
    	    OutputStream output = new ByteArrayOutputStream();
   	    	handler.handleRequest(input, output, null);      
    		
    	    // Read the actualResponse for this test data.
   		    actualResponse = new TestDataGeneralResponse(output.toString());
   			String actualResponseFormatted = actualResponse.toString();
    		log.info("testDialogCreateScheduleAndExit actualResponse={}", actualResponseFormatted);
    		
    		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
    		
    		// Run an exit to force database update of intentLog.
    		testRequest = new TestDataRequest(customerId);
    		testRequest.setRequestType("SessionEndedRequest");
    		testRequest.setRequestReason("USER_INITIATED");
    		testRequest.setSessionAttributes(actualResponse);
    		testRequest.removeRequestIntent();
    		testDataRequestFormatted = testRequest.toString();
    		log.debug("testDialogCreateScheduleAndExit testDataRequest={}", testDataRequestFormatted);

    		// Modify expectedResponse for this test.
    		TestDataTellResponse expectedTellResponse = new TestDataTellResponse();
    		expectedTellResponse.removeResponse();
    		expectedResponseFormatted = expectedTellResponse.toString();
    		log.debug("testDialogCreateScheduleAndExit expectedResponse={}", expectedResponseFormatted);

    		// Run the testRequest through the handler.
    	    input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
    	    output = new ByteArrayOutputStream();
    	    try {
    	    	handler.handleRequest(input, output, null);      
    	    } catch (IOException ex) {
    	    	fail("IOException: "+ex.getMessage());
    	    }
    		
    	    // Read the actualResponse for this test data.
    	    actualResponse = new TestDataGeneralResponse(output.toString());
    		actualResponseFormatted = actualResponse.toString();
    		log.info("testDialogCreateScheduleAndExit actualResponse={}", actualResponseFormatted);
    		
    		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
    		
    		// Confirm the saved intent log from database.
    		log.info("Customer ID: {}", testScheduleItem.getCustomerId());
    		DynamoItem item = dynamoDB.getTrashDayDbClient().loadItem(testScheduleItem);
    		IntentLog intentLog = item.getIntentLog();
    		
    		String expectedIntentLogJson = "{\"log\":{\"2016-48\":{\"respondTimeZoneUpdatedScheduleMissing\":1,\"respondPickupAddSingle\":5,\"tellSchedule\":1}},\"modelVersion\":\"1\"}";
    		String actualIntentLogJson = intentLog.toJson();
    		assertJsonEquals(expectedIntentLogJson, actualIntentLogJson);

        } catch (IOException ex) {
        	fail("IOException: "+ex.getMessage());
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user when they build a new schedule with a 
	 * series of "AddPickup" commands.
	 * <p>
	 * Tested Intent: AddPickupIntent and TellScheduleIntent
	 * <ul>
	 * <li>AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * <li>TellScheduleIntent what is the schedule
	 * <li>exit
	 * </ul>
	 */
	@Test
	public void testDialogCreateScheduleAndStop() {
		List<TestDataUtterance> dialog = createDialogNewSchedule();
		
		// Remove the existing schedule in database.
        itemDelete(customerId);
        
        try {
        	TestDataGeneralResponse actualResponse = runDialog(dialog, null);
    		
    		// Run a stop to force database update of intentLog.
        	TestDataRequest testRequest = new TestDataRequest(customerId);
    		testRequest.setRequestIntentName("AMAZON.StopIntent");
    		testRequest.setSessionAttributes(actualResponse);
    		String testDataRequestFormatted = testRequest.toString();
    		log.debug("testDialogCreateScheduleAndStop testDataRequest={}", testDataRequestFormatted);

    		// Modify expectedResponse for this test.
    		TestDataTellResponse expectedResponse = new TestDataTellResponse();
    		expectedResponse.setResponseOutputSpeechText(Phrases.SCHEDULE_DONE_VERBAL);
    		expectedResponse.deleteResponseCard();
    		String expectedResponseFormatted = expectedResponse.toString();
    		log.debug("testDialogCreateScheduleAndStop expectedResponse={}", expectedResponseFormatted);

    		// Run the testRequest through the handler.
    		InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
    	    OutputStream output = new ByteArrayOutputStream();
    	    try {
    	    	handler.handleRequest(input, output, null);      
    	    } catch (IOException ex) {
    	    	fail("IOException: "+ex.getMessage());
    	    }
    		
    	    // Read the actualResponse for this test data.
    	    actualResponse = new TestDataGeneralResponse(output.toString());
    		String actualResponseFormatted = actualResponse.toString();
    		log.info("testDialogCreateScheduleAndStop actualResponse={}", actualResponseFormatted);
    		
    		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
    		
    		// Confirm the saved intent log from database.
    		DynamoItem item = dynamoDB.getTrashDayDbClient().loadItem(testScheduleItem);
    		IntentLog intentLog = item.getIntentLog();
    		
    		String expectedIntentLogJson = "{\"log\":{\"2016-48\":{\"respondTimeZoneUpdatedScheduleMissing\":1,\"respondPickupAddSingle\":5}},\"modelVersion\":\"1\"}";
    		String actualIntentLogJson = intentLog.toJson();
    		assertJsonEquals(expectedIntentLogJson, actualIntentLogJson);

        } catch (IOException ex) {
        	fail("IOException: "+ex.getMessage());
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user when they delete items from a schedule 
	 * with a series of "DeletePickup" commands.
	 * <p>
	 * Tested Intent: DeletePickupIntent, DeleteEntirePickupIntent and TellScheduleIntent
	 * <ul>
	 * <li>DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>DeletePickupIntent delete {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * <li>DeleteEntirePickupIntent delete all {PickupName} pickups
	 * <li>TellScheduleIntent what is the schedule
	 * </ul>
	 */
	@Test
	public void testDialogDeleteSchedule() {
		// Remove the existing schedule in database.
        itemDelete(customerId);
        
        try {
        	TestDataGeneralResponse actualResponse = runDialog(createDialogNewSchedule(), null);
        	actualResponse = runDialog(createDialogDeleteSchedule(), actualResponse);
        	
        	// Finally, check the resulting schedule.
    		// Modify testRequest for this test.
    		TestDataRequest testRequest = new TestDataRequest(customerId);
    		testRequest.setRequestIntentName("TellScheduleIntent");
    		testRequest.setSessionAttributes(actualResponse);
    		String testDataRequestFormatted = testRequest.toString();
    		log.debug("testDialogDeleteSchedule testDataRequest={}", testDataRequestFormatted);

    		// Modify expectedResponse for this test.
    		TestDataAskResponse expectedResponse = new TestDataAskResponse();
    		expectedResponse.setResponseOutputSpeechText("Pickup trash on Tuesday 6 30 AM.  To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL);
    		expectedResponse.setResponseRepromptOutputSpeechText(Phrases.SCHEDULE_ALTER_REPROMPT);
    		//expectedResponse.setResponseCardTitle("Trash Day Schedule");
    		//expectedResponse.setResponseCardContent("As of Thursday, November 24 at 10:27 AM:\nPickup trash on Tuesday at 6:30 AM.\n");
    		expectedResponse.deleteResponseCard();
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, "${json-unit.ignore}");
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE, "${json-unit.ignore}");			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testTimeZone);
    		Schedule smallerSchedule = new Schedule();
    		smallerSchedule.initExampleSchedule();
    		smallerSchedule.deleteEntirePickup("recycling");
    		smallerSchedule.deletePickupTime("trash", DayOfWeek.FRIDAY, LocalTime.of(6, 30));
    		smallerSchedule.deletePickupTime("lawn waste", DayOfWeek.WEDNESDAY, LocalTime.of(12, 00));
    		String expectedResponseFormatted = expectedResponse.toString();
    		log.debug("testDialogDeleteSchedule expectedResponse={}", expectedResponseFormatted);

    		// Run the testRequest through the handler.
    	    InputStream input = new ByteArrayInputStream(testDataRequestFormatted.getBytes());;
    	    OutputStream output = new ByteArrayOutputStream();
   	    	handler.handleRequest(input, output, null);      
    		
    	    // Read the actualResponse for this test data.
   		    actualResponse = new TestDataGeneralResponse(output.toString());
   			String actualResponseFormatted = actualResponse.toString();
    		log.info("testDialogDeleteSchedule actualResponse={}", actualResponseFormatted);
    		
    		assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);
        } catch (IOException ex) {
        	fail("IOException: "+ex.getMessage());
        } finally {
        	// Re-save the item.
        	dynamoDB.getTrashDayDbClient().saveItem(testScheduleItem);
        }
	}

}
