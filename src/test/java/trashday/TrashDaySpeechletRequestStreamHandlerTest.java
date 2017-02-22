package trashday;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;
import alexatesting.TestDataAskResponse;
import alexatesting.TestDataGeneralResponse;
import alexatesting.TestDataRequest;
import alexatesting.TestDataTellResponse;
import alexatesting.TestDataUtterance;
import dynamotesting.LocalDynamoDBCreationRule;
import trashday.model.Calendar;
import trashday.model.IntentLog;
import trashday.storage.DynamoAccessTestHelpers;
import trashday.storage.DynamoItem;
import trashday.storage.SessionDao;
import trashday.ui.FormatUtils;
import trashday.ui.requests.SlotDayOfMonth;
import trashday.ui.requests.SlotDayOfWeek;
import trashday.ui.requests.SlotNthOfMonth;
import trashday.ui.requests.SlotPickupName;
import trashday.ui.requests.SlotTimeOfDay;
import trashday.ui.requests.SlotTimeZone;
import trashday.ui.requests.SlotWeekOfMonth;
import trashday.ui.responses.Phrase;


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
public class TrashDaySpeechletRequestStreamHandlerTest extends DynamoAccessTestHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechletRequestStreamHandlerTest.class);
    
    /** Create a new, local Dynamo DB instance for JUnit testing */
    @ClassRule
    public static final LocalDynamoDBCreationRule localDynamoDB = new LocalDynamoDBCreationRule("TrashDayScheduleDataTest");
    
    /** Database table name for holding the users' saved Schedules. */
	private static final String tableNameOverride = "TrashDayScheduleDataTest";
	
	/** Format Time Of Day for Response Cards */
	public static final DateTimeFormatter formatterTimeOfDay = DateTimeFormatter.ofPattern("h:mm a");
	/** Format Day for Response Cards */
	public static final DateTimeFormatter formatterDay = DateTimeFormatter.ofPattern("EEEE, MMMM d");

	/** For read-only Test functions, a shared test with good data items. */
	private static DynamoItem testFullItem;
	/** For read-only Test functions, the customer ID from the shared testItem. */
	private static String testFullCustomerId;
	/** For read-only Test functions, the time zone from the shared testItem. */
	private static TimeZone testFullTimeZone;
	/** For read-only Test functions, the Calendar from the shared testItem. */
	private static Calendar testFullCalendar;
	
	/** For read-only Test functions, a shared test with an item containing only TimeZone information. */
	private static DynamoItem testTZItem;
	/** For read-only Test functions, the customer ID from the shared testItem. */
	private static String testTZCustomerId;
	
	/** For read-only Test functions, a shared test with no matching database item. */
	private static String testNoCustomerId;
	
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
		dynamoDbLowLevel = localDynamoDB.getAmazonDynamoDBClient();
		dynamoDbDocument = new DynamoDB(dynamoDbLowLevel);
		dynamoDbItemPersistence = localDynamoDB.getTrashDayDbClient();

		boolean isTestDynamoDB = localDynamoDB.isTestDB();
		CreateTableResult ctr = null;
		if (isTestDynamoDB) {
			log.info("(Re-)creating table in test DB.");
			tableDelete();
			ctr = tableCreate();			
		}
        TableDescription tableDescription = ctr.getTableDescription();
        log.info("Created Table Description: {}", tableDescription.toString());
        
        handler = new TrashDaySpeechletRequestStreamHandler(dynamoDbLowLevel, tableNameOverride);
        
		testFullItem = writeNewItemComplexCalendar();
		testFullCustomerId = testFullItem.getCustomerId();
		testFullTimeZone = testFullItem.getTimeZone();
		testFullCalendar = testFullItem.getCalendar();
		
		testTZItem = writeNewItemNoCalendar();
		testTZCustomerId = testTZItem.getCustomerId();
		
		testNoCustomerId = getNextCustomerId();
		
    }
    
	protected TestDataGeneralResponse executeAndCheckJson(TestDataRequest testRequest, TestDataAskResponse expectedResponse) {
		return executeAndCheckJson(testRequest, expectedResponse.toString(), false);
	}
	
	protected TestDataGeneralResponse executeAndCheckJson(TestDataRequest testRequest, TestDataTellResponse expectedResponse) {
		return executeAndCheckJson(testRequest, expectedResponse.toString(), false);
	}
	
	protected TestDataGeneralResponse executeAndCheckJsonIgnoreCalTimestamps(TestDataRequest testRequest, TestDataAskResponse expectedResponse) {
		return executeAndCheckJson(testRequest, expectedResponse.toString(), true);
	}
	
	protected TestDataGeneralResponse executeAndCheckJsonIgnoreCalTimestamps(TestDataRequest testRequest, TestDataTellResponse expectedResponse) {
		return executeAndCheckJson(testRequest, expectedResponse.toString(), true);
	}
	
	protected TestDataGeneralResponse executeAndCheckJson(TestDataRequest testRequest, String expectedResponseFormatted, boolean ignoreCalendarTimestamps) {
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testDataRequest={}", testDataRequestFormatted);
		log.debug("expectedResponse={}", expectedResponseFormatted);
		
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
		log.info("actualResponse={}", actualResponseFormatted);
		
		if (ignoreCalendarTimestamps) {
			assertJsonEquals(
					ignoreCalendarUidInfo(expectedResponseFormatted), 
					ignoreCalendarUidInfo(actualResponseFormatted) );
		} else {
			assertJsonEquals(expectedResponseFormatted, actualResponseFormatted);	
		}
		
		return actualResponse;
	}
	
    public static DynamoItem writeNewItemComplexCalendar() {
		String customerId = getNextCustomerId();
		
		Calendar calendar = new Calendar();
		calendar.initComplexExampleCalendar();
		
		DynamoItem item = new DynamoItem();
		item.setCustomerId(customerId);
		item.setCalendar(calendar);
		item.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
    	
    	dynamoDbItemPersistence.saveItem(item);
		
		return item;
    }
    
    public static DynamoItem writeNewItemEmptyCalendar() {
		String customerId = getNextCustomerId();
		
		Calendar calendar = new Calendar();
		
		DynamoItem item = new DynamoItem();
		item.setCustomerId(customerId);
		item.setCalendar(calendar);
		item.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
    	
    	dynamoDbItemPersistence.saveItem(item);
		
		return item;
    }
    
    public static DynamoItem writeNewItemNoCalendar() {
		String customerId = getNextCustomerId();
		
		DynamoItem item = new DynamoItem();
		item.setCustomerId(customerId);
		item.setTimeZone(TimeZone.getTimeZone("US/Eastern"));
    	
    	dynamoDbItemPersistence.saveItem(item);
		
		return item;
    }
    
    public static DynamoItem writeNewItemMissingTimeZone() {
		String customerId = getNextCustomerId();
		
		Calendar calendar = new Calendar();
		calendar.initComplexExampleCalendar();
		
		DynamoItem item = new DynamoItem();
		item.setCustomerId(customerId);
		item.setCalendar(calendar);
    	
    	dynamoDbItemPersistence.saveItem(item);
		
		return item;
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
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "lawn waste");
		tdu.addSlotValue("DayOfWeek", "wednesday");
		tdu.addSlotValue("TimeOfDay", "12:00");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddPickupIntent add {PickupName} pickup at {TimeOfDay} on {DayOfWeek}");
		tdu.addSlotValue("PickupName", "lawn waste");
		tdu.addSlotValue("DayOfWeek", "wednesday");
		tdu.addSlotValue("TimeOfDay", "12:00");
		dialog.add(tdu);
		
		tdu = new TestDataUtterance("AddMonthlyLastDayPickupIntent add monthly {PickupName} pickup on the last day of the month at {TimeOfDay}");
		tdu.addSlotValue("PickupName", "scrap metal");
		tdu.addSlotValue("TimeOfDay", "13:00");
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
			TestDataRequest testRequest = new TestDataRequest(testFullCustomerId, tdu);
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
	public void testHelpWithTimeZoneAndSchedule() {
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("AMAZON.HelpIntent");

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrase.HELP_VERBAL_SCHEDULE_EXISTS.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.HELP_REPROMPT_SCHEDULE_EXISTS);
		expectedResponse.setResponseCardTitle("Trash Day Help");
		expectedResponse.setResponseCardContent(Phrase.HELP_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "help");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);

		executeAndCheckJson(testRequest, expectedResponse);
		
		// Re-run without card request.
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);
		
		expectedResponse.deleteResponseCard();
		
		executeAndCheckJson(testRequest, expectedResponse);
	}

	@Test
	public void testHelpWithTimeZoneAndEmptySchedule() {
		TestDataRequest testRequest = new TestDataRequest(testTZCustomerId);
		testRequest.setRequestIntentName("AMAZON.HelpIntent");

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrase.HELP_VERBAL_NO_SCHEDULE.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.HELP_REPROMPT_NO_SCHEDULE);
		expectedResponse.setResponseCardTitle("Trash Day Help");
		expectedResponse.setResponseCardContent(Phrase.HELP_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "help");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);

		executeAndCheckJson(testRequest, expectedResponse);		
		
		// Re-run without card request.
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);
		
		expectedResponse.setResponseOutputSpeechText(Phrase.HELP_VERBAL_NO_SCHEDULE.toString());
		expectedResponse.deleteResponseCard();
		
		executeAndCheckJson(testRequest, expectedResponse);
	}

	@Test
	public void testHelpWithNoItem() {
		TestDataRequest testRequest = new TestDataRequest(testNoCustomerId);
		testRequest.setRequestIntentName("AMAZON.HelpIntent");

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrase.HELP_VERBAL_INITIAL.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_SET_REPROMPT);
		expectedResponse.setResponseCardTitle("Trash Day Help");
		expectedResponse.setResponseCardContent(Phrase.HELP_CARD);
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "help");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);

		executeAndCheckJson(testRequest, expectedResponse);		
		
		// Re-run without card request.
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_OVERALL_HELP_CARD_SENT,  true);
		
		expectedResponse.setResponseOutputSpeechText(Phrase.HELP_VERBAL_INITIAL.toString());
		expectedResponse.deleteResponseCard();
		
		executeAndCheckJson(testRequest, expectedResponse);		
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "Cancel."
	 * <p>
	 * Tested Intent: AMAZON.CancelIntent
	 */
	@Test
	public void testCancel() {
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("AMAZON.CancelIntent");

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Goodbye");
		expectedResponse.deleteResponseCard();

		executeAndCheckJson(testRequest, expectedResponse);		
	}

	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "Stop"
	 * <p>
	 * Tested Intent: AMAZON.StopIntent
	 */
	@Test
	public void testStop() {
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("AMAZON.StopIntent");

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Goodbye");
		expectedResponse.deleteResponseCard();

		executeAndCheckJson(testRequest, expectedResponse);		
	}

	@Test
	public void testChangeSchedule() {
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("UpdateScheduleIntent");

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrase.SCHEDULE_ALTER_VERBAL);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ALTER_REPROMPT);
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");

		executeAndCheckJson(testRequest, expectedResponse);		
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
		// Get a new, unique customer id.  Ensure there is no data left in the database for this id.
		String customerId = getNextCustomerId();
        itemDelete(customerId);
        
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestType("LaunchRequest");
		testRequest.removeRequestIntent();

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Welcome to Trash Day. Please " + Phrase.TIME_ZONE_SET_VERBAL);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_SET_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
	
		executeAndCheckJson(testRequest, expectedResponse);		
	}
	
	@Test
	public void testSetTimeZoneScheduleExists() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","pacific");
	
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("Time zone set to US/Pacific.");
			expectedResponse.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse);		
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	@Test
	public void testSetTimeZoneOther() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","other");
	
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText(Phrase.TIME_ZONE_OTHER_VERBAL.toString()+Phrase.TIME_ZONE_OTHER_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_OTHER_REPROMPT);
			expectedResponse.setResponseCardTitle("Trash Day Set Time Zone");
			expectedResponse.setResponseCardContent(Phrase.TIME_ZONE_HELP_CARD);
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondSetTimeZoneOther");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, true);
	
			executeAndCheckJson(testRequest, expectedResponse);		
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	@Test
	public void testSetTimeZoneScheduleExistsCIP() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","pacific");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
	
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Time zone set to US/Pacific. Next, "+Phrase.SCHEDULE_ALTER_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ALTER_REPROMPT);
			expectedResponse.deleteResponseCard();
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondTimeZoneUpdatedScheduleExists");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Pacific");
	
			executeAndCheckJsonIgnoreCalTimestamps(testRequest, expectedResponse);		
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	@Test
	public void testSetTimeZoneScheduleMissing() {
		// A new customer id should give a missing database item.
		String customerId = getNextCustomerId();
		itemDelete(customerId);
        
        try {		
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("SetTimeZoneIntent");
			testRequest.addRequestIntentSlot("TimeZone","pacific");
	
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Time zone set to US/Pacific. Next, "+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL+Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT);
			expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			expectedResponse.setResponseCardContent(Phrase.SCHEDULE_ALTER_CARD.toString());
			TimeZone expectedTimeZone = TimeZone.getTimeZone("US/Pacific");
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondTimeZoneUpdatedScheduleMissing");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, expectedTimeZone);
	
			executeAndCheckJson(testRequest, expectedResponse);		
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(customerId);
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
		TestDataRequest testRequest = new TestDataRequest(testTZCustomerId);
		testRequest.setRequestType("LaunchRequest");
		testRequest.removeRequestIntent();

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT);
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
	
		executeAndCheckJson(testRequest, expectedResponse);		
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestType("LaunchRequest");
		testRequest.removeRequestIntent();

		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText(Phrase.OPEN_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.OPEN_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "open");
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);

		executeAndCheckJson(testRequest, expectedResponse);		
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("AMAZON.YesIntent");

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Sorry.  I didn't understand what question you were answering.  Please say \"help\" for what things you can say.");
		expectedResponse.deleteResponseCard();

		executeAndCheckJson(testRequest, expectedResponse);		
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestType("LaunchRequest");
		testRequest.removeRequestIntent();

		TestDataAskResponse expectedResponse1 = new TestDataAskResponse();
		expectedResponse1.setResponseOutputSpeechText(Phrase.OPEN_VERBAL.toString());
		expectedResponse1.setResponseRepromptOutputSpeechText(Phrase.OPEN_REPROMPT.toString());
		expectedResponse1.deleteResponseCard();
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
		intentLog.incrementIntent(ldtRequest, "open");
		expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
		expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
		expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());
		expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);

		executeAndCheckJson(testRequest, expectedResponse1);		
		
		testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestType("SessionEndedRequest");
		testRequest.setRequestReason("USER_INITIATED");
		testRequest.removeRequestIntent();

		TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
		expectedResponse2.removeResponse();

		executeAndCheckJson(testRequest, expectedResponse2);
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("AMAZON.YesIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DoSomethingIntent");
		testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "perform new, unknown action");

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Uh-oh, I found a programming problem.  Cannot perform new, unknown action");
		expectedResponse.deleteResponseCard();

		executeAndCheckJson(testRequest, expectedResponse);
	}
	
	@Test
	public void testTellScheduleEmpty() {
		TestDataRequest testRequest = new TestDataRequest(testTZCustomerId);
		testRequest.setRequestIntentName("TellScheduleIntent");
	
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
	
		executeAndCheckJson(testRequest, expectedResponse);
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
		LocalDateTime ldtRequest = LocalDateTime.of(2017, 2, 12, 9, 12);
		ZonedDateTime zdtRequest = ZonedDateTime.of(ldtRequest, ZoneId.systemDefault());
		Date dRequest = Date.from(zdtRequest.toInstant());
		
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("TellScheduleIntent");
		testRequest.setRequestTimestamp(dRequest);

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Pickup trash every Tuesday at 7 30 AM and every Friday at 7 30 AM. Pickup recycling every other Friday at 7 30 AM (on this Friday). Pickup lawn waste on the first at noon and on the fifteenth at noon. Pickup scrap metal on the last day of the month at noon. Pickup mortgage on the fifth day before the end of the month at noon. Pickup dry cleaning on the second Saturday at noon. Pickup hockey team on the second-to-last Saturday at 9 AM. ");
		expectedResponse.setResponseCardTitle("Trash Day Schedule");
		expectedResponse.setResponseCardContent("As of Sunday, February 12 at 9:12 AM:\nPickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.\nPickup recycling every other Friday at 7:30 AM (on this Friday).\nPickup lawn waste on the first at noon and on the fifteenth at noon.\nPickup scrap metal on the last day of the month at noon.\nPickup mortgage on the fifth day before the end of the month at noon.\nPickup dry cleaning on the second Saturday at noon.\nPickup hockey team on the second-to-last Saturday at 9 AM.\n");

		executeAndCheckJson(testRequest, expectedResponse);
	}

	@Test
	public void testTellScheduleExistingNoTimeZone() {
		// Need an item that is mis-configured.  Make one for just this test.
		DynamoItem item = writeNewItemMissingTimeZone();

        try {		
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("TellScheduleIntent");
	
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The time zone isn't configured yet. "+Phrase.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_SET_REPROMPT);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());
	
			executeAndCheckJsonIgnoreCalTimestamps(testRequest, expectedResponse);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
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
	public void testTellScheduleDBItemMissing() {
		// A new customer id should give a missing database item.
		String customerId = getNextCustomerId();
		itemDelete(customerId);
        
		TestDataRequest testRequest = new TestDataRequest(customerId);
		testRequest.setRequestIntentName("TellScheduleIntent");
	
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("Welcome to Trash Day. Please " + Phrase.TIME_ZONE_SET_VERBAL);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_SET_REPROMPT);
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
	
		executeAndCheckJson(testRequest, expectedResponse);
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
		TestDataRequest testRequest = new TestDataRequest(testTZCustomerId);
		testRequest.setRequestIntentName("TellScheduleIntent");
	
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL);
		expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT);
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
	
		executeAndCheckJson(testRequest, expectedResponse);
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.setRequestTimestamp("2017-02-08T10:27:15Z"); // Wednesday morning

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next trash pickup is Friday at 7 30 AM. Next lawn waste pickup is Wednesday, February 15 at noon. Next recycling pickup is Friday, February 17 at 7 30 AM. Next hockey team pickup is Saturday, February 18 at 9 AM. Next mortgage pickup is Friday, February 24 at noon. Next scrap metal pickup is Tuesday, February 28 at noon. Next dry cleaning pickup is Saturday, March 11 at noon. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Wednesday, February 8 at 5:27 AM:\n" + 
				"Next trash pickup is Friday at 7:30 AM.\n" + 
				"Next lawn waste pickup is Wednesday, February 15 at noon.\n" + 
				"Next recycling pickup is Friday, February 17 at 7:30 AM.\n" +
				"Next hockey team pickup is Saturday, February 18 at 9 AM.\n" +
				"Next mortgage pickup is Friday, February 24 at noon.\n" +
				"Next scrap metal pickup is Tuesday, February 28 at noon.\n" +
				"Next dry cleaning pickup is Saturday, March 11 at noon.\n");

		executeAndCheckJson(testRequest, expectedResponse);
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.setRequestTimestamp("2017-03-08T10:27:15Z"); // Wednesday morning

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next trash pickup is Friday at 7 30 AM. Next dry cleaning pickup is Saturday at noon. Next lawn waste pickup is Wednesday, March 15 at noon. Next recycling pickup is Friday, March 17 at 7 30 AM. Next hockey team pickup is Saturday, March 18 at 9 AM. Next mortgage pickup is Monday, March 27 at noon. Next scrap metal pickup is Friday, March 31 at noon. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Wednesday, March 8 at 5:27 AM:\nNext trash pickup is Friday at 7:30 AM.\nNext dry cleaning pickup is Saturday at noon.\nNext lawn waste pickup is Wednesday, March 15 at noon.\nNext recycling pickup is Friday, March 17 at 7:30 AM.\nNext hockey team pickup is Saturday, March 18 at 9 AM.\nNext mortgage pickup is Monday, March 27 at noon.\nNext scrap metal pickup is Friday, March 31 at noon.\n");

		executeAndCheckJson(testRequest, expectedResponse);
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.addRequestIntentSlot("PickupName","trash");
		testRequest.setRequestTimestamp("2017-02-08T10:27:15Z"); // Wednesday morning

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next trash pickup is Friday at 7 30 AM. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Wednesday, February 8 at 5:27 AM:\nNext trash pickup is Friday at 7:30 AM.\n");

		executeAndCheckJson(testRequest, expectedResponse);
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
	public void testTellNextPickupBlank() {
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.addRequestIntentSlot("PickupName"," ");
		testRequest.setRequestTimestamp("2017-02-08T10:27:15Z"); // Wednesday morning

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("Next trash pickup is Friday at 7 30 AM. Next lawn waste pickup is Wednesday, February 15 at noon. Next recycling pickup is Friday, February 17 at 7 30 AM. Next hockey team pickup is Saturday, February 18 at 9 AM. Next mortgage pickup is Friday, February 24 at noon. Next scrap metal pickup is Tuesday, February 28 at noon. Next dry cleaning pickup is Saturday, March 11 at noon. ");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Wednesday, February 8 at 5:27 AM:\n" + 
				"Next trash pickup is Friday at 7:30 AM.\n" + 
				"Next lawn waste pickup is Wednesday, February 15 at noon.\n" + 
				"Next recycling pickup is Friday, February 17 at 7:30 AM.\n" +
				"Next hockey team pickup is Saturday, February 18 at 9 AM.\n" +
				"Next mortgage pickup is Friday, February 24 at noon.\n" +
				"Next scrap metal pickup is Tuesday, February 28 at noon.\n" +
				"Next dry cleaning pickup is Saturday, March 11 at noon.\n");

		executeAndCheckJson(testRequest, expectedResponse);
	}
	
	@Test
	public void testTellNextPickupsScheduleEmpty() {
		TestDataRequest testRequest = new TestDataRequest(testTZCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.setRequestTimestamp("2017-02-08T10:27:15Z"); // Wednesday morning
	
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL);
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT);
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
	
		executeAndCheckJson(testRequest, expectedResponse);
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
	public void testTellNextPickupNonExisting() {
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.setRequestTimestamp("2017-02-08T10:27:15Z"); // Wednesday morning
		testRequest.addRequestIntentSlot("PickupName","bad name");

		TestDataTellResponse expectedResponse = new TestDataTellResponse();
		expectedResponse.setResponseOutputSpeechText("No bad name pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"");
		expectedResponse.setResponseCardTitle("Trash Day Pickups");
		expectedResponse.setResponseCardContent("As of Wednesday, February 8 at 5:27 AM, no bad name pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"");

		executeAndCheckJson(testRequest, expectedResponse);
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
		TestDataRequest testRequest = new TestDataRequest(testTZCustomerId);
		testRequest.setRequestIntentName("TellNextPickupIntent");
		testRequest.setRequestTimestamp("2017-02-08T10:27:15Z"); // Wednesday morning
	
		TestDataAskResponse expectedResponse = new TestDataAskResponse();
		expectedResponse.setResponseOutputSpeechText("The pickup schedule is empty. "+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL.toString());
		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT.toString());
		expectedResponse.deleteResponseCard();
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Eastern");
	
		executeAndCheckJson(testRequest, expectedResponse);
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
	public void testAddPickup() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added weekly mail pickup on Monday at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mail pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
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
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","today");
			testRequest.addRequestIntentSlot("TimeOfDay","13:30");
			testRequest.addRequestIntentSlot("PickupName","lawn waste");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("Added weekly lawn waste pickup on Thursday at 1 30 PM.");
			expectedResponse.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","tomorrow");
			testRequest.addRequestIntentSlot("TimeOfDay","13:30");
			testRequest.addRequestIntentSlot("PickupName","lawn waste");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("Added weekly lawn waste pickup on Friday at 1 30 PM.");
			//expectedResponse.setResponseCardContent("Added lawn waste pickup on Friday at 1:30 PM.");
			expectedResponse.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddWeeklyPickupIntent
	 * <ul>
	 * <li>AddPickupIntent add weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * <li>AddPickupIntent add weekly {PickupName} pickup at {TimeOfDay} on {DayOfWeek}
	 * </ul>
	 */
	@Test
	public void testAddPickupWeekly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddWeeklyPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added weekly mail pickup on Monday at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mail pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add biweekly {PickupName} pickup on this {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddBiWeeklyPickupIntent
	 * <ul>
	 * <li>AddThisBiWeeklyPickupIntent add biweekly {PickupName} pickup on this {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupThisBiWeekly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddThisBiWeeklyPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added biweekly mail pickup on this Monday at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mail pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add biweekly {PickupName} pickup on this {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddBiWeeklyPickupIntent
	 * <ul>
	 * <li>AddThisBiWeeklyPickupIntent add biweekly {PickupName} pickup on this {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupFollowingBiWeekly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddFollowingBiWeeklyPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added biweekly mail pickup on Monday after next at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mail pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup on the {DayOfMonth} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyPickupIntent
	 * <ul>
	 * <li>AddMonthlyPickupIntent add monthly {PickupName} pickup on the {DayOfMonth} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyPickupIntent");
			testRequest.addRequestIntentSlot("NthOfMonth","fifth");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly mail pickup on the fifth at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mail pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup on the last day of the month at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyLastDayPickupIntent
	 * <ul>
	 * <li>AddMonthlyLastDayPickupIntent add monthly {PickupName} pickup on the last day of the month at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthlyLastDay() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyLastDayPickupIntent");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","scrap metal");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly scrap metal pickup on the last day of the month at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That scrap metal pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup {DayOfMonth} days before the end of the month at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyLastNDayPickupIntent
	 * <ul>
	 * <li>AddMonthlyLastNDayPickupIntent add monthly {PickupName} pickup {DayOfMonth} days before the end of the month at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthlyLastNDay() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyLastNDayPickupIntent");
			testRequest.addRequestIntentSlot("DayOfMonth","5");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mortgage");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly mortgage pickup on the fifth day before the end of the month at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mortgage pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup on the {NthOfMonth} day before the end of the month at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyLastNDayPickupIntent
	 * <ul>
	 * <li>AddMonthlyLastNDayPickupIntent add monthly {PickupName} pickup on the {NthOfMonth} day before the end of the month at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthlyLastNthDay() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyLastNDayPickupIntent");
			testRequest.addRequestIntentSlot("NthOfMonth","twenty-sixth");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mortgage");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly mortgage pickup on the twenty-sixth day before the end of the month at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That mortgage pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyWeekdayPickupIntent
	 * <ul>
	 * <li>AddMonthlyWeekdayPickupIntent add monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthlyWeekday() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyWeekdayPickupIntent");
			testRequest.addRequestIntentSlot("PickupName","baseball team");
			testRequest.addRequestIntentSlot("WeekOfMonth","first");
			testRequest.addRequestIntentSlot("DayOfWeek","saturday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly baseball team pickup on the first Saturday of the month at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That baseball team pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyWeekdayPickupIntent
	 * <ul>
	 * <li>AddMonthlyWeekdayPickupIntent add monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthlyLastWeekday() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyWeekdayPickupIntent");
			testRequest.addRequestIntentSlot("PickupName","football team");
			testRequest.addRequestIntentSlot("WeekOfMonth","last");
			testRequest.addRequestIntentSlot("DayOfWeek","saturday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly football team pickup on the last Saturday of the month at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That football team pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "add monthly {PickupName} pickup on the {WeekOfMonth} to last {DayOfWeek} at {TimeOfDay}" 
	 * <p>
	 * Tested Intent: AddMonthlyLastNWeekdayPickupIntent
	 * <ul>
	 * <li>AddMonthlyLastNWeekdayPickupIntent add monthly {PickupName} pickup on the {WeekOfMonth} to last {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testAddPickupMonthlyLastNWeekday() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddMonthlyLastNWeekdayPickupIntent");
			testRequest.addRequestIntentSlot("PickupName","hockey team");
			testRequest.addRequestIntentSlot("WeekOfMonth","second");
			testRequest.addRequestIntentSlot("DayOfWeek","saturday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Added monthly hockey team pickup on the second to last Saturday of the month at 4 PM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("That hockey team pickup already exists.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	@Test
	public void testAddPickupNothingConfigured() {
		// A new customer id should give a missing database item.
		String customerId = getNextCustomerId();
		itemDelete(customerId);
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse1 = new TestDataAskResponse();
			expectedResponse1.setResponseOutputSpeechText("Welcome to Trash Day. Please " + Phrase.TIME_ZONE_SET_VERBAL);
			expectedResponse1.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_SET_REPROMPT);
			expectedResponse1.deleteResponseCard();
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
	
			executeAndCheckJson(testRequest, expectedResponse1);
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(customerId);
        }
	}
	
	@Test
	public void testAddPickupTimeZoneMissing() {
		// Need an item that is mis-configured.  Make one for just this test.
		DynamoItem item = writeNewItemMissingTimeZone();

        try {		
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Monday");
			testRequest.addRequestIntentSlot("TimeOfDay","16:00");
			testRequest.addRequestIntentSlot("PickupName","mail");
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testAddPickupTime testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("The time zone isn't configured yet. " + Phrase.TIME_ZONE_SET_VERBAL);
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.TIME_ZONE_SET_REPROMPT);
			expectedResponse.deleteResponseCard();
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());			
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
			
			assertJsonEquals(
					ignoreCalendarUidInfo(expectedResponseFormatted), 
					ignoreCalendarUidInfo(actualResponseFormatted) );
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AddPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","");
			testRequest.addRequestIntentSlot("TimeOfDay"," ");
			testRequest.addRequestIntentSlot("PickupName"," ");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("I missed the Pickup Name, Day Of Week, and Time Of Day information.\n"+Phrase.SCHEDULE_ADD_PICKUPS_VERBAL+Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX);
			expectedResponse.setResponseCardTitle("Trash Day Change Schedule");
			expectedResponse.setResponseCardContent(Phrase.SCHEDULE_ALTER_CARD);
	
			executeAndCheckJson(testRequest, expectedResponse);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
	public void testDeletePickup() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeletePickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Friday");
			testRequest.addRequestIntentSlot("TimeOfDay","07:30");
			testRequest.addRequestIntentSlot("PickupName","trash");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed trash pickup on Friday at 7 30 AM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no trash pickup scheduled on Friday at 7 30 AM.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeletePickupIntent
	 * <ul>
	 * <li>DeleteWeeklyPickupIntent delete weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupWeekly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteWeeklyPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Friday");
			testRequest.addRequestIntentSlot("TimeOfDay","07:30");
			testRequest.addRequestIntentSlot("PickupName","trash");
	
			// Modify expectedResponse for this test.
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed weekly trash pickup on Friday at 7 30 AM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no weekly trash pickup scheduled on Friday at 7 30 AM.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete biweekly {PickupName} pickup on {NextOrThis} {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteBiWeeklyPickupIntent
	 * <ul>
	 * <li>DeleteBiWeeklyPickupIntent delete biweekly {PickupName} pickup on {NextOrThis} {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupBiWeekly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteBiWeeklyPickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Friday");
			testRequest.addRequestIntentSlot("TimeOfDay","07:30");
			testRequest.addRequestIntentSlot("PickupName","recycling");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed biweekly recycling pickup on Friday at 7 30 AM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no biweekly recycling pickup scheduled on Friday at 7 30 AM.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}" 
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteMonthlyPickupIntent
	 * <ul>
	 * <li>DeleteMonthlyPickupIntent delete monthly {PickupName} pickup on the {DayOfMonth} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupMonthly() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteMonthlyPickupIntent");
			testRequest.addRequestIntentSlot("NthOfMonth","fifteenth");
			testRequest.addRequestIntentSlot("TimeOfDay","12:00");
			testRequest.addRequestIntentSlot("PickupName","lawn waste");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed monthly lawn waste pickup on the fifteenth at noon.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no monthly lawn waste pickup scheduled on the fifteenth at noon.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete monthly {PickupName} pickup on the last day of the month at {TimeOfDay}" 
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteMonthlyLastDayPickupIntent
	 * <ul>
	 * <li>DeleteMonthlyLastDayPickupIntent delete monthly {PickupName} pickup on the last day of the month at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupMonthlyLastDay() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteMonthlyLastDayPickupIntent");
			testRequest.addRequestIntentSlot("TimeOfDay","12:00");
			testRequest.addRequestIntentSlot("PickupName","scrap metal");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed monthly scrap metal pickup on the last day of the month at noon.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no monthly scrap metal pickup scheduled on the last day of the month at noon.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete monthly {PickupName} pickup {DayOfMonth} days before the end of the month at {TimeOfDay}"
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteMonthlyLastNDayPickupIntent
	 * <ul>
	 * <li>DeleteMonthlyLastNDayPickupIntent delete monthly {PickupName} pickup {DayOfMonth} days before the end of the month at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupMonthlyLastNDay() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteMonthlyLastNDayPickupIntent");
			testRequest.addRequestIntentSlot("DayOfMonth","5");
			testRequest.addRequestIntentSlot("TimeOfDay","12:00");
			testRequest.addRequestIntentSlot("PickupName","mortgage");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed monthly mortgage pickup on the fifth day before the end of the month at noon.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no monthly mortgage pickup scheduled on the fifth day before the end of the month at noon.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete monthly {PickupName} pickup on the {NthOfMonth} day before the end of the month at {TimeOfDay}"
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteMonthlyLastNDayPickupIntent
	 * <ul>
	 * <li>DeleteMonthlyLastNDayPickupIntent delete monthly {PickupName} pickup on the {NthOfMonth} day before the end of the month at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupMonthlyLastNthDay() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteMonthlyLastNDayPickupIntent");
			testRequest.addRequestIntentSlot("NthOfMonth","fifth");
			testRequest.addRequestIntentSlot("TimeOfDay","12:00");
			testRequest.addRequestIntentSlot("PickupName","mortgage");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed monthly mortgage pickup on the fifth day before the end of the month at noon.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no monthly mortgage pickup scheduled on the fifth day before the end of the month at noon.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}"
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteMonthlyWeekdayPickupIntent
	 * <ul>
	 * <li>DeleteMonthlyWeekdayPickupIntent delete monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupMonthlyWeekday() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteMonthlyWeekdayPickupIntent");
			testRequest.addRequestIntentSlot("WeekOfMonth","second");
			testRequest.addRequestIntentSlot("DayOfWeek","saturday");
			testRequest.addRequestIntentSlot("TimeOfDay","12:00");
			testRequest.addRequestIntentSlot("PickupName","dry cleaning");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed monthly dry cleaning pickup on the second Saturday of the month at noon.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no monthly dry cleaning pickup scheduled on the second Saturday of the month at noon.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * JUnit test to ensure application responds correctly
	 * to the user asking to "delete monthly {PickupName} pickup on the {WeekOfMonth} to last {DayOfWeek} at {TimeOfDay}"
	 * when the pickup time already exists.
	 * <p>
	 * Tested Intent: DeleteMonthlyLastNWeekdayPickupIntent
	 * <ul>
	 * <li>DeleteMonthlyLastNWeekdayPickupIntent delete monthly {PickupName} pickup on the {WeekOfMonth} to last {DayOfWeek} at {TimeOfDay}
	 * </ul>
	 */
	@Test
	public void testDeletePickupMonthlyLastNWeekday() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteMonthlyLastNWeekdayPickupIntent");
			testRequest.addRequestIntentSlot("WeekOfMonth","second");
			testRequest.addRequestIntentSlot("DayOfWeek","saturday");
			testRequest.addRequestIntentSlot("TimeOfDay","09:00");
			testRequest.addRequestIntentSlot("PickupName","hockey team");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed monthly hockey team pickup on the second to last Saturday of the month at 9 AM.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no monthly hockey team pickup scheduled on the second to last Saturday of the month at 9 AM.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	@Test
	public void testDeletePickupTimeExistingCIP() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeletePickupIntent");
			testRequest.addRequestIntentSlot("DayOfWeek","Friday");
			testRequest.addRequestIntentSlot("TimeOfDay","07:30");
			testRequest.addRequestIntentSlot("PickupName","trash");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testDeletePickupTimeExisting testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Removed trash pickup on Friday at 7 30 AM. To make more changes, "+Phrase.SCHEDULE_ALTER_VERBAL.toString());
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ALTER_REPROMPT.toString());
			expectedResponse.deleteResponseCard();
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondPickupDeleteSingle");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
			String expectedResponseFormatted = expectedResponse.toString();
			log.debug("testDeletePickupTimeExisting expectedResponse={}", expectedResponseFormatted);
	
			executeAndCheckJson(testRequest, expectedResponse);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
	public void testDeleteEntirePickup() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteEntirePickupIntent");
			testRequest.addRequestIntentSlot("PickupName","recycling");
	
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText("Removed all recycling pickups from the schedule.");
			expectedResponse1.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("There was no recycling pickup in the schedule.");
			expectedResponse2.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse2);			
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}

	@Test
	public void testDeleteEntirePickupExistingCIP() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			// Modify testRequest for this test.
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteEntirePickupIntent");
			testRequest.addRequestIntentSlot("PickupName","recycling");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			String testDataRequestFormatted = testRequest.toString();
			log.debug("testDeletePickupExisting testDataRequest={}", testDataRequestFormatted);
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse = new TestDataAskResponse();
			expectedResponse.setResponseOutputSpeechText("Removed all recycling pickups from the schedule. To make more changes, "+Phrase.SCHEDULE_ALTER_VERBAL.toString());
			expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ALTER_REPROMPT.toString());
			expectedResponse.deleteResponseCard();
			Calendar smallerCalendar = new Calendar();
			smallerCalendar.initComplexExampleCalendar();
			smallerCalendar.pickupDelete("recycling");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, smallerCalendar.toStringRFC5545());			
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, "respondEntirePickupDelete");
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
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
			
			assertJsonEquals(
					ignoreCalendarUidInfo(expectedResponseFormatted), 
					ignoreCalendarUidInfo(actualResponseFormatted) );
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
	
			TestDataAskResponse expectedResponse1 = new TestDataAskResponse();
			expectedResponse1.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse1.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse1.deleteResponseCard();
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
	
			executeAndCheckJsonIgnoreCalTimestamps(testRequest, expectedResponse1);
			
			testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AMAZON.YesIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
	
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("Cleared entire schedule.");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);
		}
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
	public void testDeleteScheduleNoItem() {
		// A new customer id should give a missing database item.
		String customerId = getNextCustomerId();
		itemDelete(customerId);
        
        try {
			TestDataRequest testRequest = new TestDataRequest(customerId);
			testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
	
			TestDataTellResponse expectedResponse = new TestDataTellResponse();
			expectedResponse.setResponseOutputSpeechText("The schedule is already cleared.");
			expectedResponse.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse);
        } finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(customerId);
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
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
	
			TestDataAskResponse expectedResponse1 = new TestDataAskResponse();
			expectedResponse1.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse1.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse1.deleteResponseCard();
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
	
			executeAndCheckJsonIgnoreCalTimestamps(testRequest, expectedResponse1);
			
			testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AMAZON.NoIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
	
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("Cancelling the delete entire schedule");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);
		}
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
			TestDataRequest testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("DeleteEntireScheduleIntent");
	
			TestDataAskResponse expectedResponse1 = new TestDataAskResponse();
			expectedResponse1.setResponseOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse1.setResponseRepromptOutputSpeechText("Are you sure you want to delete the entire schedule?");
			expectedResponse1.deleteResponseCard();
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, testFullCalendar.toStringRFC5545());
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			expectedResponse1.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
	
			executeAndCheckJsonIgnoreCalTimestamps(testRequest, expectedResponse1);
			
			testRequest = new TestDataRequest(item.getCustomerId());
			testRequest.setRequestIntentName("AMAZON.CancelIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_INTENT, "DeleteEntireScheduleIntent");
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_CONFIRM_DESC, "delete entire schedule");
	
			TestDataTellResponse expectedResponse2 = new TestDataTellResponse();
			expectedResponse2.setResponseOutputSpeechText("Cancelling the delete entire schedule");
			expectedResponse2.deleteResponseCard();
	
			executeAndCheckJson(testRequest, expectedResponse2);
		}
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
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
		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
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
        itemDelete(testFullCustomerId);
        
        try {
        	TestDataGeneralResponse actualResponse = runDialog(dialog, null);

        	// Finally, check the resulting schedule.
    		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
    		testRequest.setRequestIntentName("TellScheduleIntent");
    		testRequest.setSessionAttributes(actualResponse);

    		TestDataAskResponse expectedResponse = new TestDataAskResponse();
    		expectedResponse.setResponseOutputSpeechText("Pickup trash every Tuesday at 6 30 AM and every Friday at 6 30 AM. Pickup recycling every Friday at 6 30 AM. Pickup lawn waste every Wednesday at noon. Pickup scrap metal on the last day of the month at 1 PM.  To make more changes, "+Phrase.SCHEDULE_ALTER_VERBAL);
    		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ALTER_REPROMPT);
    		expectedResponse.deleteResponseCard();
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);

    		actualResponse = executeAndCheckJson(testRequest, expectedResponse);
    		
    		// Run an exit to force database update of intentLog.
    		testRequest = new TestDataRequest(testFullCustomerId);
    		testRequest.setRequestType("SessionEndedRequest");
    		testRequest.setRequestReason("USER_INITIATED");
    		testRequest.setSessionAttributes(actualResponse);
    		testRequest.removeRequestIntent();

    		// Modify expectedResponse for this test.
    		TestDataTellResponse expectedTellResponse = new TestDataTellResponse();
    		expectedTellResponse.removeResponse();

			executeAndCheckJson(testRequest, expectedTellResponse);
    		
    		// Confirm the saved intent log from database.
    		log.info("Customer ID: {}", testFullItem.getCustomerId());
    		DynamoItem checkItem = localDynamoDB.getTrashDayDbClient().loadCompleteItem(testFullItem);
    		IntentLog intentLog = checkItem.getIntentLog();
    		
    		String expectedIntentLogJson = "{\"log\":{\"2016-48\":{\"respondTimeZoneUpdatedScheduleMissing\":1,\"respondPickupAddSingle\":4,\"respondGeneralPickupNotAdded\":1,\"respondPickupAddMonthlyLastDaySingle\":1,\"tellSchedule\":1}},\"modelVersion\":\"1\"}";
    		String actualIntentLogJson = intentLog.toJson();
    		assertJsonEquals(expectedIntentLogJson, actualIntentLogJson);

        } catch (IOException ex) {
        	fail("IOException: "+ex.getMessage());
        } finally {
        	// Re-save the item.
        	localDynamoDB.getTrashDayDbClient().saveItem(testFullItem);
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
        itemDelete(testFullCustomerId);
        
        try {
        	TestDataGeneralResponse actualResponse = runDialog(dialog, null);
    		
    		// Run a stop to force database update of intentLog.
        	TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
    		testRequest.setRequestIntentName("AMAZON.StopIntent");
    		testRequest.setSessionAttributes(actualResponse);

    		// Modify expectedResponse for this test.
    		TestDataTellResponse expectedResponse = new TestDataTellResponse();
    		expectedResponse.setResponseOutputSpeechText(Phrase.SCHEDULE_DONE_VERBAL);
    		expectedResponse.deleteResponseCard();

			executeAndCheckJson(testRequest, expectedResponse);
    		
    		// Confirm the saved intent log from database.
    		DynamoItem checkItem = localDynamoDB.getTrashDayDbClient().loadCompleteItem(testFullItem);
    		IntentLog intentLog = checkItem.getIntentLog();
    		
    		String expectedIntentLogJson = "{\"log\":{\"2016-48\":{\"respondTimeZoneUpdatedScheduleMissing\":1,\"respondPickupAddSingle\":4,\"respondGeneralPickupNotAdded\":1,\"respondPickupAddMonthlyLastDaySingle\":1}},\"modelVersion\":\"1\"}";
    		String actualIntentLogJson = intentLog.toJson();
    		assertJsonEquals(expectedIntentLogJson, actualIntentLogJson);

        } catch (IOException ex) {
        	fail("IOException: "+ex.getMessage());
        } finally {
        	// Re-save the item.
        	localDynamoDB.getTrashDayDbClient().saveItem(testFullItem);
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
        itemDelete(testFullCustomerId);
        
        try {
        	TestDataGeneralResponse actualResponse = runDialog(createDialogNewSchedule(), null);
        	actualResponse = runDialog(createDialogDeleteSchedule(), actualResponse);
        	
        	// Finally, check the resulting schedule.
    		TestDataRequest testRequest = new TestDataRequest(testFullCustomerId);
    		testRequest.setRequestIntentName("TellScheduleIntent");
    		testRequest.setSessionAttributes(actualResponse);

    		TestDataAskResponse expectedResponse = new TestDataAskResponse();
    		expectedResponse.setResponseOutputSpeechText("Pickup trash every Tuesday at 6 30 AM. Pickup scrap metal on the last day of the month at 1 PM.  To make more changes, "+Phrase.SCHEDULE_ALTER_VERBAL);
    		expectedResponse.setResponseRepromptOutputSpeechText(Phrase.SCHEDULE_ALTER_REPROMPT);
    		expectedResponse.deleteResponseCard();
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, true);
    		expectedResponse.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);

			executeAndCheckJson(testRequest, expectedResponse);
        } catch (IOException ex) {
        	fail("IOException: "+ex.getMessage());
        } finally {
        	// Re-save the item.
        	localDynamoDB.getTrashDayDbClient().saveItem(testFullItem);
        }
	}

    /**
     * Find all combinations of a list's items.  Credit to <a href="http://stackoverflow.com/users/1980909/adrian-leonhard">Adrian Leonhard</a>
     * <p>
     * Example Usage: slotCombos = getCombinationsStream(slots).collect(Collectors.toList());
     * 
     * @param list find all combinations of these items
     * @param <T> This is the type parameter
     * @return Stream containing all the combination lists.
     * @see 	<a href="http://stackoverflow.com/questions/28515516/enumeration-combinations-of-k-elements-using-java-8">StackOverflow: Enumeration Combinations of K elements using Java 8</a>
     */
    public static <T> Stream<List<T>> getCombinationsStream(List<T> list) {
        // there are 2 ^ list.size() possible combinations
        // stream through them and map the number of the combination to the combination
        return LongStream.range(1 , 1 << list.size())
                .mapToObj(l -> bitMapToList(l, list));
    }

    /**
     * Find all combinations of a list's items.  Credit to <a href="http://stackoverflow.com/users/1980909/adrian-leonhard">Adrian Leonhard</a>
     * 
     * @param bitmap used to pick the list items
     * @param list find all combinations of these items
     * @param <T> This is the type parameter
     * @return list of items indicated by the bitmap
     * @see 	<a href="http://stackoverflow.com/questions/28515516/enumeration-combinations-of-k-elements-using-java-8">StackOverflow: Enumeration Combinations of K elements using Java 8</a>
     */
    public static <T> List<T> bitMapToList(long bitmap, List<T> list) {
        // use the number of the combination (bitmap) as a bitmap to filter the input list
        return IntStream.range(0, list.size())
                .filter(i -> 0 != ((1 << i) & bitmap))
                .mapToObj(list::get)
                .collect(Collectors.toList());
    }
    
    protected List<Map<String,String>> slotGroupA() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotPickupName.name);
		slot.put("description", SlotPickupName.description);
		slot.put("value", "trash");
		slot.put("valid", "trash");
		slot.put("invalid", null);
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotDayOfWeek.name);
		slot.put("description", SlotDayOfWeek.description);
		slot.put("value", "saturday");
		slot.put("valid", "wednesday");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotTimeOfDay.name);
		slot.put("description", SlotTimeOfDay.description);
		slot.put("value", "16:00");
		slot.put("valid", "14:00");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);

		return slotsAll;
    }

    protected List<Map<String,String>> slotGroupB() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotPickupName.name);
		slot.put("description", SlotPickupName.description);
		slot.put("value", "trash");
		slot.put("valid", "trash");
		slot.put("invalid", null);
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotNthOfMonth.name);
		slot.put("description", SlotNthOfMonth.description);
		slot.put("value", "third");
		slot.put("valid", "ninth");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotTimeOfDay.name);
		slot.put("description", SlotTimeOfDay.description);
		slot.put("value", "16:00");
		slot.put("valid", "14:00");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);

		return slotsAll;
    }

    protected List<Map<String,String>> slotGroupC() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotPickupName.name);
		slot.put("description", SlotPickupName.description);
		slot.put("value", "trash");
		slot.put("valid", "trash");
		slot.put("invalid", null);
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotTimeOfDay.name);
		slot.put("description", SlotTimeOfDay.description);
		slot.put("value", "16:00");
		slot.put("valid", "14:00");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);

		return slotsAll;
    }

    protected List<Map<String,String>> slotGroupD() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotPickupName.name);
		slot.put("description", SlotPickupName.description);
		slot.put("value", "trash");
		slot.put("valid", "trash");
		slot.put("invalid", null);
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotDayOfMonth.name);
		slot.put("description", SlotDayOfMonth.description);
		slot.put("value", "14");
		slot.put("valid", "9");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotTimeOfDay.name);
		slot.put("description", SlotTimeOfDay.description);
		slot.put("value", "16:00");
		slot.put("valid", "14:00");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);

		return slotsAll;
    }

    protected List<Map<String,String>> slotGroupE() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotPickupName.name);
		slot.put("description", SlotPickupName.description);
		slot.put("value", "trash");
		slot.put("valid", "trash");
		slot.put("invalid", null);
		slotsAll.add(slot);
		
		slot = new HashMap<String,String>();
		slot.put("name", SlotWeekOfMonth.name);
		slot.put("description", SlotWeekOfMonth.description);
		slot.put("value", "second");
		slot.put("valid", "third");
		slot.put("invalid", "eighth");
		slotsAll.add(slot);

		slot = new HashMap<String,String>();
		slot.put("name", SlotDayOfWeek.name);
		slot.put("description", SlotDayOfWeek.description);
		slot.put("value", "saturday");
		slot.put("valid", "wednesday");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);

		slot = new HashMap<String,String>();
		slot.put("name", SlotTimeOfDay.name);
		slot.put("description", SlotTimeOfDay.description);
		slot.put("value", "16:00");
		slot.put("valid", "14:00");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);

		return slotsAll;
    }
    
    protected List<Map<String,String>> slotGroupF() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotPickupName.name);
		slot.put("description", SlotPickupName.description);
		slot.put("value", "trash");
		slot.put("valid", "trash");
		slot.put("invalid", null);
		slotsAll.add(slot);
		
		return slotsAll;
    }

    protected List<Map<String,String>> slotGroupG() {
		List<Map<String,String>> slotsAll = new ArrayList<Map<String,String>>();
		Map<String,String> slot = new HashMap<String,String>();
		slot.put("name", SlotTimeZone.name);
		slot.put("description", SlotTimeZone.description);
		slot.put("value", "US/Eastern");
		slot.put("valid", "eastern");
		slot.put("invalid", "garbage");
		slotsAll.add(slot);
		
		return slotsAll;
    }

	protected List<Map<String, String>> createValidSlots(List<Map<String, String>> slotsAll, List<String> slotsSkipNames) {
		List<Map<String, String>> slotsValid = new ArrayList<Map<String, String>>();
		List<String> slotsValidNames = new ArrayList<String>();
		for (Map<String,String> slotInfo : slotsAll) {
			if (slotsSkipNames.contains(slotInfo.get("name"))) { // Skip
				continue;
			}
			slotInfo.put("value", slotInfo.get("valid"));
			slotsValid.add(slotInfo);
			slotsValidNames.add(slotInfo.get("name"));
		}
		log.info("testMissingCombos slotsValid: {}", slotsValidNames);
		return slotsValid;
	}

	protected void testMissingCIPCombos(String customerId, String intentName, String intentLogEntry, List<Map<String,String>> slotsAll, Phrase addPhrase, Phrase repromptPhrase, Phrase cardSuffixPhrase, String cardTitle, String cardSessionAttribute, Phrase cardPhrase) {
		List<List<Map<String, String>>> slotCombos = getCombinationsStream(slotsAll)
				.collect(Collectors.toList());
		for ( List<Map<String, String>> slotsMissing : slotCombos) {
			List<String> slotsMissingNames = new ArrayList<String>();
			List<String> slotsMissingDescriptions = new ArrayList<String>();
			for (Map<String,String> slotInfo : slotsMissing) {
				slotsMissingNames.add(slotInfo.get("name"));
				slotsMissingDescriptions.add(slotInfo.get("description"));
			}
			log.info("testMissingCIPCombos slotsMissing: {}", slotsMissingNames);
			
			List<Map<String, String>> slotsValid = createValidSlots(slotsAll, slotsMissingNames);
			
			// Modify testRequest for this test (non-CIP).
			TestDataRequest testRequest = new TestDataRequest(customerId, slotsValid);
			testRequest.setRequestIntentName(intentName);

			// Modify expectedResponse for this test (non-CIP).
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();
			expectedResponse1.setResponseOutputSpeechText(FormatUtils.formattedJoin(slotsMissingDescriptions, "I missed the ", " information.\n")+addPhrase+cardSuffixPhrase);
			expectedResponse1.setResponseCardTitle(cardTitle);
			expectedResponse1.setResponseCardContent(cardPhrase);
			
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify testRequest for this test (CIP).
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
	
			// Modify expectedResponse for this test.
			TestDataAskResponse expectedResponse2 = new TestDataAskResponse();
			expectedResponse2.setResponseOutputSpeechText(FormatUtils.formattedJoin(slotsMissingDescriptions, "I missed the ", " information.\n")+addPhrase+cardSuffixPhrase);
			expectedResponse2.setResponseRepromptOutputSpeechText(repromptPhrase);
			expectedResponse2.setResponseCardTitle(cardTitle);
			expectedResponse2.setResponseCardContent(cardPhrase);
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, intentLogEntry);
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);
			expectedResponse2.setSessionAttribute(cardSessionAttribute,  true);
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
	
			executeAndCheckJson(testRequest, expectedResponse2);
		}
	}

	protected void testInvalidCIPCombos(String customerId, String intentName, String intentLogEntry, List<Map<String,String>> slotsAll, Phrase addPhrase, Phrase repromptPhrase, Phrase cardSuffixPhrase, String cardTitle, String cardSessionAttribute, Phrase cardPhrase) {
		List<List<Map<String, String>>> slotCombos = getCombinationsStream(slotsAll)
				//.filter(list -> list.size() <= slotsAll.size())
				.collect(Collectors.toList());
		for ( List<Map<String, String>> slotsInvalid : slotCombos) {
			List<String> slotsInvalidNames = new ArrayList<String>();
			List<String> slotsInvalidDescriptions = new ArrayList<String>();
			String badInvalidCombination = null;
			for (Map<String,String> slotInfo : slotsInvalid) {
				if (slotInfo.get("invalid")==null) {
					badInvalidCombination = slotInfo.get("name");
					break;
				}
				slotInfo.put("value", slotInfo.get("invalid"));
				slotsInvalidNames.add(slotInfo.get("name"));
				slotsInvalidDescriptions.add(slotInfo.get("description"));
			}
			if (badInvalidCombination!=null) {
				// Not a valid combination.  Skip it.
				continue;
			}
			log.info("testMissingCIPCombos slotsInvalid: {}", slotsInvalidNames);
			
			List<Map<String, String>> slotsValid = createValidSlots(slotsAll, slotsInvalidNames);
			
			List<Map<String, String>> slotsValidAndInvalid = new ArrayList<Map<String, String>>();
			slotsValidAndInvalid.addAll(slotsInvalid);
			slotsValidAndInvalid.addAll(slotsValid);
			
			// Modify testRequest for this test (non-CIP).
			TestDataRequest testRequest = new TestDataRequest(customerId, slotsValidAndInvalid);
			testRequest.setRequestIntentName(intentName);

			// Modify expectedResponse for this test (non-CIP).
			TestDataTellResponse expectedResponse1 = new TestDataTellResponse();			
			expectedResponse1.setResponseOutputSpeechText(FormatUtils.formattedJoin(slotsInvalidDescriptions, "I didn't understand the ", " information.\n")+addPhrase+cardSuffixPhrase);
			expectedResponse1.setResponseCardTitle(cardTitle);
			expectedResponse1.setResponseCardContent(cardPhrase);
	
			executeAndCheckJson(testRequest, expectedResponse1);
			
			// Modify testRequest for this test (CIP).
			testRequest.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);

			// Modify expectedResponse for this test (CIP).
			TestDataAskResponse expectedResponse2 = new TestDataAskResponse();
			expectedResponse2.setResponseOutputSpeechText(FormatUtils.formattedJoin(slotsInvalidDescriptions, "I didn't understand the ", " information.\n")+addPhrase+cardSuffixPhrase);
			expectedResponse2.setResponseRepromptOutputSpeechText(repromptPhrase);
			expectedResponse2.setResponseCardTitle(cardTitle);
			expectedResponse2.setResponseCardContent(cardPhrase);
			IntentLog intentLog = new IntentLog();
			LocalDateTime ldtRequest = testRequest.getRequestTimestamp();
			intentLog.incrementIntent(ldtRequest, intentLogEntry);
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_CALENDAR, "${json-unit.ignore}");			
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG, intentLog.toJson());
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_INTENT_LOG_UPDATED, true);
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_SCHEDULE_IN_PROGRESS, true);			
			expectedResponse2.setSessionAttribute(cardSessionAttribute,  true);
			expectedResponse2.setSessionAttribute(SessionDao.SESSION_ATTR_TIMEZONE, testFullTimeZone);
	
			executeAndCheckJson(testRequest, expectedResponse2);
		}
	}

	/**
	 * Test when missing/invalid slot data from: 
	 * AddPickupIntent add {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * DeletePickupIntent delete {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 */
	@Test
	public void testPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddPickupIntent", 
        			"respondPickupAddMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeletePickupIntent",
        			"respondPickupDeleteMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddPickupIntent", 
        			"respondPickupAddInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeletePickupIntent", 
        			"respondPickupDeleteInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddWeeklyPickupIntent add weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 * DeleteWeeklyPickupIntent delete weekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 */
	@Test
	public void testWeeklyPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddWeeklyPickupIntent",
        			"respondPickupAddWeeklyMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteWeeklyPickupIntent",
        			"respondPickupDeleteWeeklyMissingData",
        			slotGroupA(),
        			Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddWeeklyPickupIntent", 
        			"respondPickupAddWeeklyInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteWeeklyPickupIntent",
        			"respondPickupDeleteWeeklyInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddThisBiWeeklyPickupIntent add biweekly {PickupName} pickup on this {DayOfWeek} at {TimeOfDay}
	 * DeleteBiWeeklyPickupIntent delete biweekly {PickupName} pickup on {DayOfWeek} at {TimeOfDay}
	 */
	@Test
	public void testThisBiWeeklyPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddThisBiWeeklyPickupIntent", 
        			"respondPickupAddThisBiWeeklyMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteBiWeeklyPickupIntent",
        			"respondPickupDeleteBiWeeklyMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddThisBiWeeklyPickupIntent", 
        			"respondPickupAddThisBiWeeklyInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteBiWeeklyPickupIntent",
        			"respondPickupDeleteBiWeeklyInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddFollowingBiWeeklyPickupIntent add biweekly {PickupName} pickup on {DayOfWeek} after next at {TimeOfDay}
	 * DeleteBiWeeklyPickupIntent delete biweekly {PickupName} pickup on next {DayOfWeek} at {TimeOfDay}
	 */
	@Test
	public void testNextBiWeeklyPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddFollowingBiWeeklyPickupIntent", 
        			"respondPickupAddFollowingBiWeeklyMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteBiWeeklyPickupIntent", 
        			"respondPickupDeleteBiWeeklyMissingData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddFollowingBiWeeklyPickupIntent",
        			"respondPickupAddFollowingBiWeeklyInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteBiWeeklyPickupIntent",
        			"respondPickupDeleteBiWeeklyInvalidData",
        			slotGroupA(), 
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddMonthlyPickupIntent add monthly {PickupName} pickup on the {NthOfMonth} at {TimeOfDay}
	 * DeleteMonthlyPickupIntent delete monthly {PickupName} pickup on the {NthOfMonth} at {TimeOfDay}
	 */
	@Test
	public void testMonthlyPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyPickupIntent", 
        			"respondPickupAddMonthlyMissingData",
        			slotGroupB(),
        			Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyPickupIntent", 
        			"respondPickupDeleteMonthlyMissingData",
        			slotGroupB(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyPickupIntent", 
        			"respondPickupAddMonthlyInvalidData",
        			slotGroupB(),
        			Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyPickupIntent",
        			"respondPickupDeleteMonthlyInvalidData",
        			slotGroupB(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddMonthlyLastDayPickupIntent add monthly {PickupName} pickup on the last day of the month at {TimeOfDay}
	 * DeleteMonthlyLastDayPickupIntent delete monthly {PickupName} pickup on the last day of the month at {TimeOfDay}
	 */
	@Test
	public void testMonthlyLastDayPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyLastDayPickupIntent",
        			"respondPickupAddMonthlyLastDayMissingData",
        			slotGroupC(),
        			Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyLastDayPickupIntent",
        			"respondPickupDeleteMonthlyLastDayMissingData",
        			slotGroupC(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyLastDayPickupIntent", 
        			"respondPickupAddMonthlyLastDayInvalidData",
        			slotGroupC(),
        			Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyLastDayPickupIntent", 
        			"respondPickupDeleteMonthlyLastDayInvalidData",
        			slotGroupC(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddMonthlyLastNDayPickupIntent add monthly {PickupName} pickup {DayOfMonth} days before the end of the month at {TimeOfDay}
	 * DeleteMonthlyLastNDayPickupIntent delete monthly {PickupName} pickup {DayOfMonth} days before the end of the month at {TimeOfDay}
	 */
	@Test
	public void testMonthlyLastNDayPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyLastNDayPickupIntent", 
        			"respondPickupAddMonthlyLastNDayMissingData",
        			slotGroupD(),
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyLastNDayPickupIntent", 
        			"respondPickupDeleteMonthlyLastNDayMissingData",
        			slotGroupD(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyLastNDayPickupIntent", 
        			"respondPickupAddMonthlyLastNDayInvalidData",
        			slotGroupD(),
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyLastNDayPickupIntent", 
        			"respondPickupDeleteMonthlyLastNDayInvalidData",
        			slotGroupD(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddMonthlyWeekdayPickupIntent add monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}
	 * DeleteMonthlyWeekdayPickupIntent delete monthly {PickupName} pickup on the {WeekOfMonth} {DayOfWeek} at {TimeOfDay}
	 */
	@Test
	public void testMonthlyWeekdayPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyWeekdayPickupIntent", 
        			"respondPickupAddMonthlyWeekdayMissingData",
        			slotGroupE(),
        			Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyWeekdayPickupIntent", 
        			"respondPickupDeleteMonthlyWeekdayMissingData",
        			slotGroupE(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_VERBAL, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyWeekdayPickupIntent", 
        			"respondPickupAddMonthlyWeekdayInvalidData",
        			slotGroupE(),
        			Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyWeekdayPickupIntent", 
        			"respondPickupDeleteMonthlyWeekdayInvalidData",
        			slotGroupE(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing slot data from: 
	 * AddMonthlyLastNWeekdayPickupIntent add monthly {PickupName} pickup on the {WeekOfMonth} to last {DayOfWeek} at {TimeOfDay}
	 * DeleteMonthlyLastNWeekdayPickupIntent delete monthly {PickupName} pickup on the {WeekOfMonth} to last {DayOfWeek} at {TimeOfDay}
	 */
	@Test
	public void testMonthlyLastNWeekdayPickupBadDataCombinations() {
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyLastNWeekdayPickupIntent", 
        			"respondPickupAddMonthlyLastNWeekdayMissingData",
        			slotGroupE(),
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyLastNWeekdayPickupIntent", 
        			"respondPickupDeleteMonthlyLastNWeekdayMissingData",
        			slotGroupE(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"AddMonthlyLastNWeekdayPickupIntent", 
        			"respondPickupAddMonthlyLastNWeekdayInvalidData",
        			slotGroupE(),
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteMonthlyLastNWeekdayPickupIntent", 
        			"respondPickupDeleteMonthlyLastNWeekdayInvalidData",
        			slotGroupE(),
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
        			Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing/invalid slot data from: 
	 * DeleteEntirePickupIntent delete all {PickupName} pickups
	 */
	@Test
	public void testDeleteEntirePickupBadDataCombinations() {
		log.info("testDeleteEntirePickupBadDataCombinations");
		
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
		log.info("testDeleteEntirePickupBadDataCombinations test combos");
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"DeleteEntirePickupIntent", 
        			"respondEntirePickupDeleteMissingData",
        			slotGroupF(), 
        			Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL,
        			Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"DeleteEntirePickupIntent", 
        			"respondPickupAddInvalidData",
        			slotGroupF(), 
        			Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL,
        			Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT, Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, "Trash Day Change Schedule", SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, Phrase.SCHEDULE_ALTER_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
	
	/**
	 * Test when missing/invalid slot data from: 
	 * SetTimeZoneIntent set time zone as {TimeZone}
	 */
	@Test
	public void testSetTimeZoneBadDataCombinations() {
		log.info("testSetTimeZoneBadDataCombinations");
		
		// Since we're modifying an item, write a new one for this test.
        DynamoItem item = writeNewItemComplexCalendar();
        
		log.info("testSetTimeZoneBadDataCombinations test combos");
        try {
        	testMissingCIPCombos(
        			item.getCustomerId(), 
        			"SetTimeZoneIntent", 
        			"respondSetTimeZoneMissingData",
        			slotGroupG(), 
        			Phrase.TIME_ZONE_SET_VERBAL,
        			Phrase.TIME_ZONE_SET_REPROMPT, 
        			Phrase.TIME_ZONE_SET_VERBAL_CARD_SUFFIX, 
        			"Trash Day Set Time Zone", 
        			SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, 
        			Phrase.TIME_ZONE_HELP_CARD
        			);
        	testInvalidCIPCombos(
        			item.getCustomerId(), 
        			"SetTimeZoneIntent", 
        			"respondSetTimeZoneInvalidData",
        			slotGroupG(), 
        			Phrase.TIME_ZONE_SET_VERBAL,
        			Phrase.TIME_ZONE_SET_REPROMPT, 
        			Phrase.TIME_ZONE_SET_VERBAL_CARD_SUFFIX, 
        			"Trash Day Set Time Zone", 
        			SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, 
        			Phrase.TIME_ZONE_HELP_CARD
        			);
        }
        finally {
        	// Remove the no-longer-necessary item.
        	itemDelete(item.getCustomerId());
        }
	}
}
