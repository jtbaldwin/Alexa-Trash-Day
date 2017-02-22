package trashday;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import trashday.model.Calendar;
import trashday.model.DateTimeUtils;
import trashday.model.IntentLog;
import trashday.model.NextPickups;
import trashday.storage.DynamoDao;
import trashday.storage.SessionDao;
import trashday.ui.requests.SlotDayOfMonth;
import trashday.ui.requests.SlotDayOfWeek;
import trashday.ui.requests.SlotNthOfMonth;
import trashday.ui.requests.SlotPickupName;
import trashday.ui.requests.SlotTimeOfDay;
import trashday.ui.requests.SlotTimeZone;
import trashday.ui.requests.SlotWeekOfMonth;
import trashday.ui.responses.ResponsesExit;
import trashday.ui.responses.ResponsesHelp;
import trashday.ui.responses.ResponsesSchedule;
import trashday.ui.responses.ResponsesYesNo;
import trashday.storage.DynamoItemPersistence;

/**
 * Handles the core application logic.  The {@link TrashDaySpeechlet}
 * received the Intents from the user and calls appropriate methods
 * in this class.  Methods in this class interact with the user's pickup
 * schedule and select an appropriate
 * Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.
 * 
 * @author J. Todd Baldwin
 *
 */
public class TrashDayManager {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDayManager.class);
    	
    /** Connection to the Dynamo DB used to store the user's pickup schedule data. */
    private final DynamoDao dynamoDao;
    /** Connection to the Alexa Session used to store user's information. */
    private SessionDao sessionDao = null;
    /** User time zone loaded from the Session or Dynamo DB when request handler requires. */
    private TimeZone timeZone = null;
    /** User schedule loaded from the Session or Dynamo DB when request handler requires. */
    //private Schedule schedule = null;
    /** User calendar loaded from the Session or Dynamo DB when request handler requires. */
    private Calendar calendar = null;

	/** 
     * Create manager object to handle mapping user intents to application
     * actions.  Methods will usually perform some action on the user's
     * pickup schedule and then will return verbal response from the
     * classes in the {@link trashday.ui.responses} package.
     * 
     * @param amazonDynamoDbClient
     *            {@link AmazonDynamoDBClient} connection for the Dynamo DB holding our schedule data.
     * @param tableNameOverride String table name used by JUnit tests to ensure they do not
     * 				write to the Production table name (which is hard-coded using {@literal @}DynamoDBTable in
     * 				{@link trashday.storage.DynamoItem}).  A null value indicates no override.
     * 				Any other value is the Dynamo table name to be used.
     */
    public TrashDayManager(AmazonDynamoDBClient amazonDynamoDbClient, String tableNameOverride) {
    	if (amazonDynamoDbClient==null) {
    		amazonDynamoDbClient = new AmazonDynamoDBClient();
    	}
    	DynamoItemPersistence dynamoItemPersistence = new DynamoItemPersistence(amazonDynamoDbClient, tableNameOverride);
    	dynamoDao = new DynamoDao(dynamoItemPersistence);
    }
    
	/**
	 * Find a {@link java.time.LocalDateTime} for the given user 
	 * {@link com.amazon.speech.speechlet.LaunchRequest} and {@link java.util.TimeZone}.
	 * 
	 * @param request {@link com.amazon.speech.speechlet.LaunchRequest} of this user's request
	 * @param timeZone {@link java.util.TimeZone} the user has configured
	 * @return LocalDateTime
	 */
	protected LocalDateTime getRequestLocalDateTime(LaunchRequest request, TimeZone timeZone) {
		if (timeZone==null) { return null; };
		return DateTimeUtils.getLocalDateTime(request.getTimestamp(), timeZone);
	}
	
	/**
	 * Find a {@link java.time.LocalDateTime} for the given user 
	 * {@link com.amazon.speech.speechlet.IntentRequest} and {@link java.util.TimeZone}.
	 * 
	 * @param request {@link com.amazon.speech.speechlet.IntentRequest} of this user's request
	 * @param timeZone {@link java.util.TimeZone} the user has configured
	 * @return LocalDateTime
	 */
	protected LocalDateTime getRequestLocalDateTime(IntentRequest request, TimeZone timeZone) {
		if (timeZone==null) { return null; };
		return DateTimeUtils.getLocalDateTime(request.getTimestamp(), timeZone);
	}
    
	/**
     * 
     * Get the {@link trashday.model.Calendar} from the {@link com.amazon.speech.speechlet.Session} if 
     * it is available.  Otherwise, try to load from Dynamo DB.
     * Finally, return null if it is not available in either
     * location.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
	 * @param dynamoDao {@link DynamoDao} data access object for storing user data between
	 * 			Alexa sessions.
	 * @return {@link trashday.model.Calendar} containing user's pickup schedule
	 */
    private Calendar loadCalendar(SessionDao sessionDao, DynamoDao dynamoDao) {
    	log.trace("loadCalendar()");
    	
    	// Try loading schedule from session.
    	Calendar calendar = sessionDao.getCalendar();
    	if (calendar == null) {
        	// If no schedule data available in the session, try load from DB.
    		dynamoDao.readUserData(sessionDao);
    		calendar = sessionDao.getCalendar();
    	}
    	return calendar;
    }
    
	/**
     * Get the {@link java.util.TimeZone} from the {@link Session} if 
     * it is available.  Otherwise, try to load from Dynamo DB.
     * Finally, return null if it is not available in either
     * location.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
	 * @param dynamoDao {@link DynamoDao} data access object for storing user data between
	 * 			Alexa sessions.
	 * @return {@link java.util.TimeZone} for this user.
	 */
    private TimeZone loadTimeZone(SessionDao sessionDao, DynamoDao dynamoDao) {
    	log.trace("loadTimeZone()");
    	
    	// Try loading time zone from session.
    	TimeZone timeZone = sessionDao.getTimeZone();
    	if (timeZone == null) {
        	// If no time zone data available in the session, try load from DB.
    		dynamoDao.readUserData(sessionDao);
    		timeZone = sessionDao.getTimeZone();
    	}
    	return timeZone;
    }

    /**
     * For handlers that require user time zone and schedule data to exist, use this
     * method to load the information from the {@link com.amazon.speech.speechlet.Session}
     * or from Dynomo DB.  If data is missing, respond to user with prompts to add
     * this information.
     * 
     * @return {@link com.amazon.speech.speechlet.SpeechletResponse} null if configuration
     * 		is complete.  Otherwise, a {@link com.amazon.speech.speechlet.SpeechletResponse}
     * 		prompting the user to add the missing information.
     */
    protected SpeechletResponse isConfigurationComplete() {
    	calendar = loadCalendar(sessionDao, dynamoDao);
    	timeZone = loadTimeZone(sessionDao, dynamoDao);

    	// No configuration available for this user => Welcome and start configuring.
    	if ( (calendar == null) && (timeZone==null)) {
    		// New user!  Yeah!
    		sessionDao.setScheduleConfigInProgress();
    		return ResponsesSchedule.askInitialConfiguration();
    	}
    	// There's a schedule, but time zone is missing.
    	if (timeZone==null) {
    		// Uhoh, need to have them configure a time zone first.
    		return ResponsesSchedule.askTimeZoneMissing(sessionDao, false);
    	}
    	// There's a time zone, but schedule is empty.
    	if ( (calendar==null) || (calendar.isEmpty()) ) {
    		// An empty schedule!
    		sessionDao.setScheduleConfigInProgress();
    		return ResponsesSchedule.askScheduleEmpty(sessionDao, false);
    	}
    	// No configuration dialog necessary.
    	return null;
    }
    
    /**
     * For handlers that require user time zone data to exist, use this
     * method to load the information from the {@link com.amazon.speech.speechlet.Session}
     * or from Dynomo DB.  If data is missing, respond to user with prompts to add
     * this information.
     * 
     * @return {@link com.amazon.speech.speechlet.SpeechletResponse} null if time zone configuration
     * 		is complete.  Otherwise, a {@link com.amazon.speech.speechlet.SpeechletResponse}
     * 		prompting the user to add the missing information.
     */
    protected SpeechletResponse isTimeZoneConfigurationComplete() {
    	calendar = loadCalendar(sessionDao, dynamoDao);
    	timeZone = loadTimeZone(sessionDao, dynamoDao);
    	
    	// No configuration available for this user => Welcome and start configuring.
    	if ( (calendar == null) && (timeZone==null)) {
    		// New user!  Yeah!
    		sessionDao.setScheduleConfigInProgress();
    		return ResponsesSchedule.askInitialConfiguration();
    	}
    	// There's a schedule, but time zone is missing.
    	if (timeZone==null) {
    		// Uhoh, need to have them configure a time zone first.
    		return ResponsesSchedule.askTimeZoneMissing(sessionDao, false);
    	}
    	// No configuration dialog necessary.
    	return null;
    }
    
    /**
     * Flush any intent log data that has accumulated in the current
     * user's session attributes to their correct Dynamo DB item.
     * 
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     */
    public void flushIntentLog(Session session) {
    	sessionDao = new SessionDao(session);
    	flushIntentLog();
    }
    
    /**
     * Flush any intent log data that has accumulated in the current
     * user's session attributes to their correct Dynamo DB item.  Use 
     * the current {@link #sessionDao}.
     */
    protected void flushIntentLog() {
    	// Update the user's intent log before we exit.
    	if (sessionDao.getIntentLogUpdated()) {
    		IntentLog intentLog = sessionDao.getIntentLog();
    		dynamoDao.appendIntentLogData(sessionDao, intentLog);
    		sessionDao.clearIntentLog();
    	}
    }    
    
    /**
     * Respond when the user says "Open Trash Day"
     * <p>
     * Use {@link #isConfigurationComplete()} to 
     * load the pickup schedule from the user's Session or,
     * if not available, from the Dynamo DB. If no
     * schedule available respond with instructions to add to
     * the schedule.  If there is an existing schedule, give a short
     * menu of possible user commands.
     * 
     * @param request LaunchRequest
     * 			Use the time this request was received from the user.
     * @param session
     *          {@link com.amazon.speech.speechlet.Session} for this request
     * @return Recite a short menu of possible actions the user can perform.
     */
    public SpeechletResponse handleLaunchRequest(LaunchRequest request, Session session) {
    	log.info("handleLaunchRequest(sessionId={})", session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete();
    	if (configurationNeeded != null) { return configurationNeeded; };
 
    	// Log one more use of this intent for this week.
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	sessionDao.incrementIntentLog(ldtRequest, "open");

    	// Respond with short Welcome + Menu.
    	return ResponsesSchedule.askWelcomeMenu();
    }
    
    /**
     * Respond when the user says "Change Schedule"
     * <p>
     * Set the "Schedule Change In Progress" flag on the user's {@link com.amazon.speech.speechlet.Session}
     * and respond with a prompt for a schedule change command.
     * 
     * @param request LaunchRequest
     * 			Use the time this request was received from the user.
     * @param session
     *          {@link com.amazon.speech.speechlet.Session} for this request
     * @return A prompt for schedule change commands.
     */
    public SpeechletResponse handleUpdateScheduleRequest(IntentRequest request, Session session) {
    	log.info("handleUpdateScheduleRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete();
    	if (configurationNeeded != null) { return configurationNeeded; };
    	
    	sessionDao.setScheduleConfigInProgress();
       	return ResponsesSchedule.askScheduleChange(sessionDao, false);
    }
    
    /**
     * Respond when the user asks for the next pickup time for one or
     * all of the scheduled pickups.
     * <p>
     * Use {@link #isConfigurationComplete()} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.  Respond with instructions to add to
     * the schedule if the schedule is missing or empty.
     * 
     * If there is no requested pickup name, calculate and recite the
     * next pickup time for all pickups in the schedule.  If given a
     * specific pickup name, just calculate and recite the next time 
     * for that pickup.
     * 
     * @param request IntentRequest
     * 			Use the time this request was received from the user.
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Recite the next pickup(s) time(s) or note the schedule is missing/empty
     */
    public SpeechletResponse handleTellNextPickupRequest(IntentRequest request, Session session) {
    	log.info("handleTellNextPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete();
    	if (configurationNeeded != null) { return configurationNeeded; };
    	
    	Intent intent = request.getIntent();
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	if (slotPickupName.isEmpty()) {
        	// Respond with next pickup for each item on the schedule.
        	NextPickups pickupsActual = new NextPickups(ldtRequest, calendar, null);
        	sessionDao.incrementIntentLog(ldtRequest, "tellAllNextPickups");
    		SpeechletResponse response = ResponsesSchedule.tellAllNextPickups(sessionDao, true, request.getTimestamp(), timeZone, pickupsActual);
    		flushIntentLog();
    		return response;
    	}
    	String pickupName = slotPickupName.validate();
    	if (pickupName == null) {
        	// Respond with next pickup for each item on the schedule.
    		NextPickups pickupsActual = new NextPickups(ldtRequest, calendar, null);
        	sessionDao.incrementIntentLog(ldtRequest, "tellAllNextPickups");
    		SpeechletResponse response = ResponsesSchedule.tellAllNextPickups(sessionDao, true, request.getTimestamp(), timeZone, pickupsActual);
    		flushIntentLog();
    		return response;
    	}
    	
    	// Respond with next pickup for one item on the schedule.
		NextPickups pickupsActual = new NextPickups(ldtRequest, calendar, pickupName);
    	sessionDao.incrementIntentLog(ldtRequest, "tellOneNextPickup");
		SpeechletResponse response = ResponsesSchedule.tellOneNextPickup(sessionDao, true, request.getTimestamp(), timeZone, pickupsActual, pickupName);
		flushIntentLog();
		return response;
    }

    /**
     * Respond when the user asks to hear the entire pickup schedule.
     * <p>
     * Use {@link #isConfigurationComplete()} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.  Respond with instructions to add to
     * the schedule if the schedule is missing or empty.  Otherwise,
     * recite the schedule to the user.
     * 
     * @param request IntentRequest
     * 			Use the time this request was received from the user.
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Recite the existing schedule or note the schedule is missing/empty
     */
    public SpeechletResponse handleTellScheduleRequest(IntentRequest request, Session session) {
    	log.info("handleTellScheduleRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete();
    	if (configurationNeeded != null) { return configurationNeeded; };
    	
    	// Log one more use of this intent for this week.
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	sessionDao.incrementIntentLog(ldtRequest, "tellSchedule");

    	SpeechletResponse response = ResponsesSchedule.tellSchedule(sessionDao, true, ldtRequest, calendar);
		flushIntentLog();
		return response;
    }
    
    /**
     * Respond when the user tries to set the time zone.
     * 
     * @param request IntentRequest 
     * 		Use the time zone slot provided by this user request to find a correct
     * 		{@link java.util.TimeZone} and store it for this suer.
     * @param session
     * 		{@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain and possibly send a Time Zone Help Card if the time zone
     * 		information can't be mapped to a good {@link java.util.TimeZone}.  If we get
     * 		good time zone information, set it in the Alexa session and this user's
     * 		Dynamo DB storage entry.
     */
    public SpeechletResponse handleSetTimeZoneRequest(IntentRequest request, Session session) {
    	log.info("handleSetTimeZoneRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	LocalDateTime ldtRequest = null;
    	TimeZone timeZone = loadTimeZone(sessionDao, dynamoDao);
    	if (timeZone != null) {
    		ldtRequest = getRequestLocalDateTime(request, timeZone);
    	}

    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
    	SlotTimeZone slotTimeZone = new SlotTimeZone(request.getIntent());
		if (slotTimeZone.isEmpty()) { missingDataFields.add(slotTimeZone.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondSetTimeZoneMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondSetTimeZoneMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// If user tried to set time zone to other, send useful help.
    	if (slotTimeZone.isOther()) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondSetTimeZoneOther");
    		SpeechletResponse response = ResponsesHelp.respondHelpOtherTimeZone(sessionDao, true);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
		timeZone = slotTimeZone.validate();
		if (timeZone==null) { invalidDataFields.add(slotTimeZone.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondSetTimeZoneInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondSetTimeZoneInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got validated time zone value.  Let's add it to the database.
    	sessionDao.setTimeZone(timeZone);
    	dynamoDao.writeUserData(sessionDao);
    	
		ldtRequest = getRequestLocalDateTime(request, timeZone);
    	Calendar calendar = loadCalendar(sessionDao, dynamoDao);
    	if ( (calendar==null) || (calendar.isEmpty()) ) {
    		sessionDao.setScheduleConfigInProgress();
        	sessionDao.incrementIntentLog(ldtRequest, "respondTimeZoneUpdatedScheduleMissing");
            SpeechletResponse response = ResponsesSchedule.respondTimeZoneUpdatedScheduleMissing(sessionDao, true, timeZone);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondTimeZoneUpdatedScheduleExists");
        SpeechletResponse response = ResponsesSchedule.respondTimeZoneUpdatedScheduleExists(sessionDao, false, timeZone);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a (weekly) pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddWeekly(LocalDateTime, String, DayOfWeek, LocalTime)} to add a named 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	Boolean added = calendar.pickupAddWeekly(ldtRequest, pickupName, dow, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddSingle(sessionDao, false, pickupName, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a weekly pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddWeekly(LocalDateTime, String, DayOfWeek, LocalTime)} to add a named 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddWeeklyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddWeeklyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddWeeklyMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddWeeklyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddWeeklyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddWeeklyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	Boolean added = calendar.pickupAddWeekly(ldtRequest, pickupName, dow, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddWeeklySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddWeeklySingle(sessionDao, false, pickupName, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a biweekly pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddBiWeekly(LocalDateTime, String, boolean, DayOfWeek, LocalTime)} to add a named 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddThisBiWeeklyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddThisBiWeeklyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddThisBiWeeklyMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddThisBiWeeklyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	Boolean added = calendar.pickupAddBiWeekly(ldtRequest, pickupName, false, dow, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddThisBiWeeklySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklySingle(sessionDao, false, pickupName, false, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a biweekly pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddBiWeekly(LocalDateTime, String, boolean, DayOfWeek, LocalTime)} to add a named 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddFollowingBiWeeklyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddFollowingBiWeeklyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddFollowingBiWeeklyMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddFollowingBiWeeklyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	Boolean added = calendar.pickupAddBiWeekly(ldtRequest, pickupName, true, dow, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}

    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddFollowingBiWeeklySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklySingle(sessionDao, false, pickupName, true, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a monthly pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddDayOfMonth(LocalDateTime, String, Integer, LocalTime)} to add a named 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddMonthlyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddMonthlyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotNthOfMonth.isEmpty()) { missingDataFields.add(slotNthOfMonth.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer dom = slotNthOfMonth.validate();
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dom==null) { invalidDataFields.add(slotNthOfMonth.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	Boolean added = calendar.pickupAddDayOfMonth(ldtRequest, pickupName, dom, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}

    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlySingle(sessionDao, false, pickupName, dom, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a last-day-of-month monthly pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddDayOfMonth(LocalDateTime, String, Integer, LocalTime)} to add a named 
     * pickup on last day of month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddMonthlyLastDayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddMonthlyLastDayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastDayMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastDayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastDayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastDayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	Boolean added = calendar.pickupAddDayOfMonth(ldtRequest, pickupName, -1, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}

    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastDaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastDaySingle(sessionDao, false, pickupName, -1, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a last-Nth-day-of-month monthly pickup time into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddDayOfMonth(LocalDateTime, String, Integer, LocalTime)} to add a named 
     * pickup on last Nth day of month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddMonthlyLastNDayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddMonthlyLastNDayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfMonth slotDayOfMonth = new SlotDayOfMonth(intent);
    	SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDayOfMonth.isEmpty() && slotNthOfMonth.isEmpty()) { missingDataFields.add(slotDayOfMonth.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastNDayMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastNDayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer dom = slotDayOfMonth.validate();
    	Integer nthDom = slotNthOfMonth.validate();
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if ((dom==null)&&(nthDom==null)) { invalidDataFields.add(slotDayOfMonth.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastNDayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastNDayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	if ((nthDom!=null)&&(dom==null)) {
    		dom = nthDom;
    	}
   		Boolean added = calendar.pickupAddDayOfMonth(ldtRequest, pickupName, -dom, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}

    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastNDaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastNDaySingle(sessionDao, false, pickupName, -dom, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a pickup on the Nth weekday of the month into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddWeekdayOfMonth(LocalDateTime, String, Integer, DayOfWeek, LocalTime)} to add a named 
     * pickup on the Nth weekday of the month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddMonthlyWeekdayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddMonthlyWeekdayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotWeekOfMonth.isEmpty()) { missingDataFields.add(slotWeekOfMonth.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyWeekdayMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyWeekdayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer weekOfMonth = slotWeekOfMonth.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (weekOfMonth==null) { invalidDataFields.add(slotWeekOfMonth.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyWeekdayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyWeekdayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
   		Boolean added = calendar.pickupAddWeekdayOfMonth(ldtRequest, pickupName, weekOfMonth, dow, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyWeekdaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyWeekdaySingle(sessionDao, false, pickupName, weekOfMonth, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a pickup on the Nth-to-last weekday of the month into the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupAddWeekdayOfMonth(LocalDateTime, String, Integer, DayOfWeek, LocalTime)} to add a named 
     * pickup on the Nth weekday of the month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddMonthlyLastNWeekdayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddMonthlyLastNWeekdayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotWeekOfMonth.isEmpty()) { missingDataFields.add(slotWeekOfMonth.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastNWeekdayMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastNWeekdayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer weekOfMonth = slotWeekOfMonth.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (weekOfMonth==null) { invalidDataFields.add(slotWeekOfMonth.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastNWeekdayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastNWeekdayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's add it to the calendar.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
   		Boolean added = calendar.pickupAddWeekdayOfMonth(ldtRequest, pickupName, -weekOfMonth, dow, tod);
    	if (! added) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondGeneralPickupNotAdded");
    		SpeechletResponse response = ResponsesSchedule.respondGeneralPickupNotAdded(sessionDao, false, pickupName);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}

    	sessionDao.setCalendar(calendar);
    	dynamoDao.writeUserData(sessionDao);
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddMonthlyLastNWeekdaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddMonthlyLastNWeekdaySingle(sessionDao, false, pickupName, weekOfMonth, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user deletes a (weekly) pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteWeekly(String, DayOfWeek, LocalTime)} to remove a 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeletePickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeletePickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteWeekly(pickupName, dow, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteSingle(sessionDao, false, pickupName, dow, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes a weekly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteWeekly(String, DayOfWeek, LocalTime)} to remove a 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteWeeklyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteWeeklyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteWeeklyMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteWeeklyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteWeeklyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteWeeklyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteWeekly(pickupName, dow, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteWeeklySingle(sessionDao, false, pickupName, dow, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user deletes a biweekly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteBiWeekly(String, DayOfWeek, LocalTime)} to remove a 
     * pickup at a given day-of-week and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * <p>
     * NOTE: A biweekly delete request may delete <b>two</b> entries from the schedule.
     * This makes it easier for the user to not have to know whether an event is due on
     * <b>this</b> day-of-week vs. <b>next</b> day-of-week when making the delete
     * request.  It's a user interface
     * assumption that overlapping biweekly pickups are probably not named the same.  For
     * example, "biweekly recycling pickup on Tuesday at noon (this coming Tuesday)" and "biweekly lawn waste
     * pickup on Tuesday at noon (the following Tuesday)".
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteBiWeeklyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteBiWeeklyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteBiWeeklyMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteBiWeeklyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteBiWeeklyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteBiWeeklyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteBiWeekly(pickupName, dow, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteBiWeeklySingle(sessionDao, false, pickupName, dow, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes a monthly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteDayOfMonth(String, Integer, LocalTime)} to remove a 
     * pickup at a given day-of-month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteMonthlyPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteMonthlyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotNthOfMonth.isEmpty()) { missingDataFields.add(slotNthOfMonth.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer dom = slotNthOfMonth.validate();
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (dom==null) { invalidDataFields.add(slotNthOfMonth.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteDayOfMonth(pickupName, dom, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlySingle(sessionDao, false, pickupName, dom, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes a monthly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteDayOfMonth(String, Integer, LocalTime)} to remove a 
     * pickup at a given day-of-month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteMonthlyLastDayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteMonthlyLastDayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastDayMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastDayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastDayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastDayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteDayOfMonth(pickupName, -1, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastDaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastDaySingle(sessionDao, false, pickupName, -1, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes a monthly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteDayOfMonth(String, Integer, LocalTime)} to remove a 
     * pickup at a given day-of-month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteMonthlyLastNDayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteMonthlyLastNDayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotDayOfMonth slotDayOfMonth = new SlotDayOfMonth(intent);
    	SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotDayOfMonth.isEmpty() && slotNthOfMonth.isEmpty()) { missingDataFields.add(slotDayOfMonth.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastNDayMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastNDayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer dom = slotDayOfMonth.validate();
    	Integer nthDom = slotNthOfMonth.validate();
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if ((dom==null)&&(nthDom==null)) { invalidDataFields.add(slotDayOfMonth.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastNDayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastNDayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	if ((nthDom!=null)&&(dom==null)) {
    		dom = nthDom;
    	}
    	int removedCount = calendar.pickupDeleteDayOfMonth(pickupName, -dom, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastNDaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastNDaySingle(sessionDao, false, pickupName, -dom, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes a weekday-of-monthly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteWeekdayOfMonth(String, Integer, DayOfWeek, LocalTime)} to remove a 
     * pickup at a given weekday-of-month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteMonthlyWeekdayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteMonthlyWeekdayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotWeekOfMonth.isEmpty()) { missingDataFields.add(slotWeekOfMonth.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyWeekdayMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyWeekdayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer weekOfMonth = slotWeekOfMonth.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (weekOfMonth==null) { invalidDataFields.add(slotWeekOfMonth.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyWeekdayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyWeekdayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteWeekdayOfMonth(pickupName, weekOfMonth, dow, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyWeekdaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyWeekdaySingle(sessionDao, false, pickupName, weekOfMonth, dow, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes a Nth to last weekday-of-monthly pickup time from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDeleteWeekdayOfMonth(String, Integer, DayOfWeek, LocalTime)} to remove a 
     * pickup at a given weekday-of-month and time-of-day (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteMonthlyLastNWeekdayPickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteMonthlyLastNWeekdayPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	
    	Intent intent = request.getIntent();
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent);
    	SlotDayOfWeek slotDow = new SlotDayOfWeek(intent);
    	SlotTimeOfDay slotTod = new SlotTimeOfDay(intent);
    	
    	// Respond if we're missing any data fields.
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		if (slotWeekOfMonth.isEmpty()) { missingDataFields.add(slotWeekOfMonth.getDescription()); };
		if (slotDow.isEmpty()) { missingDataFields.add(slotDow.getDescription()); };
		if (slotTod.isEmpty()) { missingDataFields.add(slotTod.getDescription()); };
    	if ( ! missingDataFields.isEmpty() ) {
			// Need more information
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastNWeekdayMissingData");
			SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastNWeekdayMissingData(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
    	Integer weekOfMonth = slotWeekOfMonth.validate();
    	DayOfWeek dow = slotDow.validate(ldtRequest);
    	LocalTime tod = slotTod.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		if (weekOfMonth==null) { invalidDataFields.add(slotWeekOfMonth.getDescription()); };
		if (dow==null) { invalidDataFields.add(slotDow.getDescription()); };
		if (tod==null) { invalidDataFields.add(slotTod.getDescription()); };
    	if ( invalidDataFields.size() > 0 ) {
        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastNWeekdayInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastNWeekdayInvalidData(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got all three validated values at once.  Let's delete it from the schedule.
    	if (calendar==null) {
    		calendar = new Calendar();
    	}
    	int removedCount = calendar.pickupDeleteWeekdayOfMonth(pickupName, -weekOfMonth, dow, tod);
		sessionDao.setCalendar(calendar);
    	if (removedCount > 0) {
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteMonthlyLastNWeekdaySingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteMonthlyLastNWeekdaySingle(sessionDao, false, pickupName, weekOfMonth, dow, tod, removedCount > 0);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes an entire pickup name from the schedule.
     * <p>
     * Use {@link #isTimeZoneConfigurationComplete()} to load the time zone from
     * the Session or Dynamo DB.  If not available, prompt the user to set the
     * time zone.  If time zone is available, check for any missing or invalid data 
     * in the request and respond appropriately so the user can correct and try again.
     * Once all required data is available, use 
     * {@link Calendar#pickupDelete(String)} to remove all 
     * pickups under a given name (e.g. "trash", "mail", etc.) (creating a new schedule if needed).
     * Save the updated schedule to Dynamo DB if needed and let the user
     * know if a removal was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule deletion and let the user know if an entry was
     * 			actually removed.
     */
    public SpeechletResponse handleDeleteEntirePickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteEntirePickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete();
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);

    	// Respond if we're missing any data fields.
    	SlotPickupName slotPickupName = new SlotPickupName(request.getIntent());
    	
		List<String> missingDataFields = new ArrayList<String>();
		if (slotPickupName.isEmpty()) { missingDataFields.add(slotPickupName.getDescription()); };
		
		if (! missingDataFields.isEmpty()) {
	    	sessionDao.incrementIntentLog(ldtRequest, "respondEntirePickupDeleteMissingData");
    		SpeechletResponse response = ResponsesSchedule.respondEntirePickupDeleteMissingName(sessionDao, true, missingDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
		List<String> invalidDataFields = new ArrayList<String>();
    	String pickupName = slotPickupName.validate();
		if (pickupName==null) { invalidDataFields.add(slotPickupName.getDescription()); };
		
		if (! invalidDataFields.isEmpty()) {
	    	sessionDao.incrementIntentLog(ldtRequest, "respondEntirePickupDeleteInvalidData");
    		SpeechletResponse response = ResponsesSchedule.respondEntirePickupDeleteInvalidName(sessionDao, true, invalidDataFields);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got needed validated value.  Let's delete it from the schedule.
    	Calendar calendar = loadCalendar(sessionDao, dynamoDao);
    	if (calendar==null) {
    		calendar = new Calendar();
    		sessionDao.setCalendar(calendar);
    	}
    	Boolean removed = calendar.pickupDelete(pickupName);
    	if (removed) {
    		sessionDao.setCalendar(calendar);
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondEntirePickupDelete");
    	SpeechletResponse response = ResponsesSchedule.respondEntirePickupDelete(sessionDao, false, pickupName, removed);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    

    /**
     * Respond when the user deletes the entire schedule.
     * <p>
     * Load only the pickup schedule from
     * the Session or Dynamo DB, if it exists.  If the schedule is already empty,
     * let the user know it is already cleared.  If schedule is available, we require a 
     * confirmation from the user.  So store confirmation data in the {@link SessionDao} 
     * to delete the entire schedule and then ask the user to confirm the schedule delete.
     * The user will respond with a Yes/No answer that will be handled by
     * {@link #handleYesRequest(IntentRequest, Session)}.  That handler will (or will not)
     * perform the delete entire schedule action.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Remove an entire pickup from the schedule and let the 
     * 			user know.  Otherwise, complain about any missing or 
     * 			invalid data from the user.
     * @see		#handleYesRequest(IntentRequest, Session)
     */
    public SpeechletResponse handleDeleteEntireScheduleRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteEntireScheduleRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	TimeZone timeZone = loadTimeZone(sessionDao, dynamoDao);
    	Calendar calendar = loadCalendar(sessionDao, dynamoDao);
    	
    	boolean alreadyClear = true;
    	if ( timeZone!=null || ( calendar!=null && (! calendar.isEmpty()) ) ) {    	
	    	sessionDao.setConfirmationIntent(request.getIntent().getName());
	    	sessionDao.setConfirmationDescription("delete entire schedule"); 
	    	alreadyClear = false;
    	}
    	SpeechletResponse response = ResponsesSchedule.respondScheduleDeleteAllRequested(sessionDao, false, alreadyClear);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user says "Help."  Give verbal and card-based help for how to
     * use the Trash Day application and commands the user can give to Trash Day.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this Yes/No request
     * @return Commands to Add, Delete, and Query the weekly pickup schedule.
     */
    public SpeechletResponse handleHelpRequest(IntentRequest request, Session session) {
    	log.trace("handleHelpRequest()");
    	sessionDao = new SessionDao(session);
    	calendar = loadCalendar(sessionDao, dynamoDao);
    	timeZone = loadTimeZone(sessionDao, dynamoDao);
    	
    	SpeechletResponse response;
    	if (timeZone == null) {
    		// User hasn't defined any time zone or schedule data yet.
    		response = ResponsesHelp.respondHelpInitial(sessionDao, true);
    	} else {
        	sessionDao = new SessionDao(session);
        	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
        	sessionDao.incrementIntentLog(ldtRequest, "help");
    		
        	if ( (calendar==null) || (calendar.isEmpty()) ) {
        		response = ResponsesHelp.respondHelpNoSchedule(sessionDao, true);
        		
        	} else {
        		response = ResponsesHelp.respondHelpWithSchedule(sessionDao, true);
        	}
    	}
    	
   	 	if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }

    /**
     * Respond when the user gives a Yes or No.
     * <p>
     * For some interactions, our application needs to ask the user for
     * a Yes/No answer.  Specifically, {@link #handleDeleteEntireScheduleRequest(IntentRequest, Session)}
     * needs to ask, "Are you sure you want to delete the entire schedule?"
     * When we ask the user a Yes/No question, we record in the session
     * an intent corresponding to the question we asked
     * ({@link SessionDao#SESSION_ATTR_CONFIRM_INTENT}) and a description of the action
     * we're contemplating ({@link SessionDao#SESSION_ATTR_CONFIRM_DESC}).
     * <p>
     * When the user gives a "Yes" response, we generate a specific response
     * based on the {@link SessionDao#SESSION_ATTR_CONFIRM_INTENT}.  We return helpful messages
     * when a question has not already been asked or the code does not understand
     * how to perform a given intent (code error).
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this Yes/No request
     * @return a generic "Cancelled" response or a perform a specific
     * 			action corresponding to a previously stored intent
     */
    public SpeechletResponse handleYesRequest(IntentRequest request, Session session) {
    	log.info("handleYesRequest(sessionId={})", session.getSessionId());
    	sessionDao = new SessionDao(session);
    	LocalDateTime ldtRequest = getRequestLocalDateTime(request, timeZone);
    	String intentToConfirm = sessionDao.getConfirmationIntent();
    	
    	if(intentToConfirm == null || intentToConfirm.trim().isEmpty()) {
			// Did not understand what we are saying yes/no to...
    		sessionDao.clearConfirmationData();
        	sessionDao.incrementIntentLog(ldtRequest, "tellYesNoMisunderstood");
	    	SpeechletResponse response = ResponsesYesNo.tellYesNoMisunderstood();
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got a positive confirmation and an outstanding question we asked.
		if (intentToConfirm.equals("DeleteEntireScheduleIntent")) {
    		sessionDao.clearConfirmationData();
        	TimeZone timeZone = loadTimeZone(sessionDao, dynamoDao);
        	Calendar calendar = loadCalendar(sessionDao, dynamoDao);
        	Boolean removed=false;
        	if (timeZone!= null || calendar!=null) {
        		if (calendar != null) {
        			removed = calendar.deleteEntireSchedule();
        		}
        		if (timeZone != null) {
        			removed = true;
        		}
    			sessionDao.clearCalendar();
    			sessionDao.clearTimeZone();
        		dynamoDao.eraseUserData(sessionDao);
        	}
        	sessionDao.incrementIntentLog(ldtRequest, "deleteEntireSchedule");
			SpeechletResponse response = ResponsesSchedule.respondScheduleDeleted(sessionDao, false, removed);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
		}
		
		// Did not understand the positive confirmation.
    	String confirmActionDesc = sessionDao.getConfirmationDescription();
		sessionDao.clearConfirmationData();
    	sessionDao.incrementIntentLog(ldtRequest, "tellYesNoProblem");
    	SpeechletResponse response = ResponsesYesNo.tellYesNoProblem(confirmActionDesc);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user gives a No.
     * <p>
     * For some interactions, our application needs to ask the user for
     * a Yes/No answer.  Specifically, {@link #handleDeleteEntireScheduleRequest(IntentRequest, Session)}
     * needs to ask, "Are you sure you want to delete the entire schedule?"
     * When we ask the user a Yes/No question, we record in the session
     * an intent corresponding to the question we asked
     * ({@link SessionDao#SESSION_ATTR_CONFIRM_INTENT}) and a description of the action
     * we're contemplating ({@link SessionDao#SESSION_ATTR_CONFIRM_DESC}).
     * <p>
     * When the user gives a "No" response, we return a generic "Cancelled"
     * response using the description from {@link SessionDao#SESSION_ATTR_CONFIRM_DESC}.
     * We return a helpful message when a question has not already been asked.
     * 
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this Yes/No request
     * 
     * @return a generic "Cancelled" response or a perform a specific
     * 			action corresponding to a previously stored intent
     */
    public SpeechletResponse handleNoRequest(Session session) {
    	log.info("handleNoRequest(sessionId={})", session.getSessionId());
    	sessionDao = new SessionDao(session);
    	String intentToConfirm = sessionDao.getConfirmationIntent();
    	
    	if(intentToConfirm == null || intentToConfirm.trim().isEmpty()) {
			// Did not understand what we are saying yes/no to...
    		sessionDao.clearConfirmationData();
	    	SpeechletResponse response = ResponsesYesNo.tellYesNoMisunderstood();
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got a negative confirmation
    	String confirmActionDesc = sessionDao.getConfirmationDescription();
		sessionDao.clearConfirmationData();
		SpeechletResponse response = ResponsesYesNo.tellCancellingAction(confirmActionDesc);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user says "Stop" or "Cancel"
     * <p>
     * If we had just asked the user a question, take a Stop/Cancel 
     * as a "No" answer.  Cleanup and respond appropriately. Otherwise, 
     * simply exit with a "Goodbye" message.
     * 
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     *
     * @return response for the Stop or Cancel intent
     */
    public SpeechletResponse handleExitRequest(Session session) {
    	log.info("handleExitRequest(sessionId={})", session.getSessionId());
    	sessionDao = new SessionDao(session);
    	
    	// If we had just asked the user a question, cancel it and tell user.
    	String intentToConfirm = sessionDao.getConfirmationIntent();
    	if(intentToConfirm != null && ! intentToConfirm.trim().isEmpty()) {
        	String confirmActionDesc = sessionDao.getConfirmationDescription();
    		sessionDao.clearConfirmationData();
    		SpeechletResponse response = ResponsesYesNo.tellCancellingAction(confirmActionDesc);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}

    	SpeechletResponse response = ResponsesExit.buildExitResponse(sessionDao);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
}
