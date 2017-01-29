package trashday;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
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

import trashday.model.IntentLog;
import trashday.model.NextPickups;
import trashday.model.Schedule;
import trashday.storage.DynamoDao;
import trashday.storage.SessionDao;
import trashday.ui.DateTimeOutputUtils;
import trashday.ui.requests.SlotDayOfWeek;
import trashday.ui.requests.SlotPickupName;
import trashday.ui.requests.SlotTimeOfDay;
import trashday.ui.requests.SlotTimeZone;
import trashday.ui.responses.ResponsesExit;
import trashday.ui.responses.ResponsesHelp;
import trashday.ui.responses.ResponsesSchedule;
import trashday.ui.responses.ResponsesYesNo;
import trashday.storage.DynamoDbClient;

/**
 * Handles the core application logic.  The {@link TrashDaySpeechlet}
 * interprets the Intents from the user and calls appropriate methods
 * in this class.  Methods in this class interact with the user's pickup
 * {@link trashday.model.Schedule Schedule} and select an appropriate
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
    private Schedule schedule = null;

	/** 
     * Create manager object to handle mapping user intents to application
     * actions.  Methods will usually perform some action on the user's
     * pickup schedule and then will return verbal response from the
     * classes in the {@link trashday.ui.responses} package.
     * 
     * @param amazonDynamoDbClient
     *            {@link AmazonDynamoDBClient} connection for the Dynamo DB holding our schedule data.
     */
    public TrashDayManager(final AmazonDynamoDBClient amazonDynamoDbClient) {
    	DynamoDbClient dynamoDbClient = new DynamoDbClient(amazonDynamoDbClient);
    	dynamoDao = new DynamoDao(dynamoDbClient);
    }
    
	/**
     * 
     * Get the {@link trashday.model.Schedule} from the {@link Session} if 
     * it is available.  Otherwise, try to load from Dynamo DB.
     * Finally, return null if it is not available in either
     * location.
	 * 
	 * @param sessionDao
	 * @param dynamoDao
	 * @return
	 */
    private Schedule loadSchedule(SessionDao sessionDao, DynamoDao dynamoDao) {
    	log.trace("loadSchedule()");
    	
    	// Try loading schedule from session.
    	Schedule schedule = sessionDao.getSchedule();
    	if (schedule == null) {
        	// If no schedule data available in the session, try load from DB.
    		dynamoDao.readUserData(sessionDao);
    		schedule = sessionDao.getSchedule();
    	}
    	return schedule;
    }
    
	/**
     * Get the {@link java.util.TimeZone} from the {@link Session} if 
     * it is available.  Otherwise, try to load from Dynamo DB.
     * Finally, return null if it is not available in either
     * location.
	 * 
	 * @param sessionDao
	 * @param dynamoDao
	 * @return
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

    private SpeechletResponse isConfigurationComplete(Date requestDate) {
    	schedule = loadSchedule(sessionDao, dynamoDao);
    	timeZone = loadTimeZone(sessionDao, dynamoDao);

    	// No configuration available for this user => Welcome and start configuring.
    	if ( (schedule == null) && (timeZone==null)) {
    		// New user!  Yeah!
    		sessionDao.setScheduleConfigInProgress();
    		SpeechletResponse response = ResponsesSchedule.askInitialConfiguration();
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	// There's a schedule, but time zone is missing.
    	if (timeZone==null) {
    		// Uhoh, need to have them configure a time zone first.
    		SpeechletResponse response = ResponsesSchedule.askTimeZoneMissing(sessionDao, false);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	// There's a time zone, but schedule is empty.
    	if ( (schedule==null) || (schedule.getPickupNames().size() < 1) ) {
    		// An empty schedule!
    		sessionDao.setScheduleConfigInProgress();
    		SpeechletResponse response = ResponsesSchedule.askScheduleEmpty(sessionDao, false);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	// No configuration dialog necessary.
    	return null;
    }
    
    private SpeechletResponse isTimeZoneConfigurationComplete(Date requestDate) {
    	schedule = loadSchedule(sessionDao, dynamoDao);
    	timeZone = loadTimeZone(sessionDao, dynamoDao);
    	
    	// No configuration available for this user => Welcome and start configuring.
    	if ( (schedule == null) && (timeZone==null)) {
    		// New user!  Yeah!
    		sessionDao.setScheduleConfigInProgress();
    		SpeechletResponse response = ResponsesSchedule.askInitialConfiguration();
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	// There's a schedule, but time zone is missing.
    	if (timeZone==null) {
    		// Uhoh, need to have them configure a time zone first.
    		SpeechletResponse response = ResponsesSchedule.askTimeZoneMissing(sessionDao, false);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	// No configuration dialog necessary.
    	return null;
    }
    
    /**
     * Flush any intent log data that has accumulated in the current
     * user's session attributes to their correct Dynamo DB item.  Create
     * a new {@link #sessionDao}.
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
    public void flushIntentLog() {
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
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to 
     * load the pickup schedule from the user's Session or,
     * if not available, from the Dyanamo DB.
     * <p>
     * If no
     * schedule available respond with instructions to add to
     * the schedule.
     * <p>
     * If there is an existing schedule, calculate and recite the
     * next pickup time for all pickups in the schedule.
     * 
     * @param request LaunchRequest
     * 			Use the time this request was received from the user.
     * @param session
     *          {@link com.amazon.speech.speechlet.Session Session} for this request
     * @return Recite the next pickup times or note the schedule is missing/empty
     */
    public SpeechletResponse handleLaunchRequest(LaunchRequest request, Session session) {
    	log.info("handleLaunchRequest(sessionId={})", session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete(request.getTimestamp());
    	if (configurationNeeded != null) { return configurationNeeded; };
 
    	// Log one more use of this intent for this week.
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
    	sessionDao.incrementIntentLog(ldtRequest, "open");

    	// Respond with short Welcome + Menu.
    	SpeechletResponse response = ResponsesSchedule.askWelcomeMenu();
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    public SpeechletResponse handleUpdateScheduleRequest(IntentRequest request, Session session) {
    	log.info("handleUpdateScheduleRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete(request.getTimestamp());
    	if (configurationNeeded != null) { return configurationNeeded; };
    	
    	sessionDao.setScheduleConfigInProgress();
       	SpeechletResponse response = ResponsesSchedule.askScheduleChange(sessionDao, false);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user asks for the next pickup time for one or
     * all of the scheduled pickups.
     * <p>
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.  Respond with instructions to add to
     * the schedule if the schedule is missing or empty.
     * <p>
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
    	SpeechletResponse configurationNeeded = isConfigurationComplete(request.getTimestamp());
    	if (configurationNeeded != null) { return configurationNeeded; };
    	
    	Intent intent = request.getIntent();
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
    	SlotPickupName slotPickupName = new SlotPickupName(intent);
    	if (slotPickupName.isEmpty()) {
        	// Respond with next pickup for each item on the schedule.
        	NextPickups pickupsActual = new NextPickups(ldtRequest, schedule, null);
        	sessionDao.incrementIntentLog(ldtRequest, "tellAllNextPickups");
    		SpeechletResponse response = ResponsesSchedule.tellAllNextPickups(true, request.getTimestamp(), timeZone, pickupsActual);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	String pickupName = slotPickupName.validate();
    	if (pickupName == null) {
        	// Respond with next pickup for each item on the schedule.
    		NextPickups pickupsActual = new NextPickups(ldtRequest, schedule, null);
        	sessionDao.incrementIntentLog(ldtRequest, "tellAllNextPickups");
    		SpeechletResponse response = ResponsesSchedule.tellAllNextPickups(true, request.getTimestamp(), timeZone, pickupsActual);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond with next pickup for one item on the schedule.
		NextPickups pickupsActual = new NextPickups(ldtRequest, schedule, pickupName);
    	sessionDao.incrementIntentLog(ldtRequest, "tellOneNextPickup");
		SpeechletResponse response = ResponsesSchedule.tellOneNextPickup(true, request.getTimestamp(), timeZone, pickupsActual, pickupName);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }

    /**
     * Respond when the user asks to hear the entire pickup schedule.
     * <p>
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.  Respond with instructions to add to
     * the schedule if the schedule is missing or empty.  Otherwise,
     * recite the schedule to the user.
     * 
     * @param request IntentRequest
     * 			Use the time this request was received from the user.
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this request
     * @return Recite the existing schedule or note the schedule is missing/empty
     */
    public SpeechletResponse handleTellScheduleRequest(IntentRequest request, Session session) {
    	log.info("handleTellScheduleRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configurationNeeded = isConfigurationComplete(request.getTimestamp());
    	if (configurationNeeded != null) { return configurationNeeded; };
    	
    	// Log one more use of this intent for this week.
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
    	sessionDao.incrementIntentLog(ldtRequest, "tellSchedule");

    	SpeechletResponse response = ResponsesSchedule.tellSchedule(sessionDao, true, request, timeZone, schedule);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user tries to set the time zone.
     * 
     * @param request IntentRequest 
     * 		Use the time zone slot provided by this user request to find a correct
     * 		{@link java.util.TimeZone} and store it for this suer.
     * @param session
     * 		{@link com.amazon.speech.speechlet.Session Session} for this request
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
    		ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
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
    	
		ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
    	Schedule schedule = loadSchedule(sessionDao, dynamoDao);
    	if ( (schedule==null) || (schedule.isEmpty()) ) {
    		sessionDao.setScheduleConfigInProgress();
        	sessionDao.incrementIntentLog(ldtRequest, "respondTimeZoneUpdatedScheduleMissing");
            SpeechletResponse response = ResponsesSchedule.respondTimeZoneUpdatedScheduleMissing(sessionDao, false, timeZone);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondTimeZoneUpdatedScheduleExists");
        SpeechletResponse response = ResponsesSchedule.respondTimeZoneUpdatedScheduleExists(sessionDao, false, timeZone);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user adds a pickup time into the schedule.
     * <p>
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.
     * <p>
     * If we find any missing or invalid data in the request, respond
     * appropriately so the user can correct and try again.
     * <p>
     * Use {@link Schedule#addPickupSchedule(String, DayOfWeek, LocalTime)} to add a named pickup at a given 
     * day-of-week and time-of-day (creating a new schedule if needed).
     * Then save the updated schedule to Dynamo DB and let the user
     * know the addition was made.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this request
     * @return Complain if any required data is missing or invalid.  Otherwise,
     * 			make the schedule addition and let the user know.
     */
    public SpeechletResponse handleAddPickupRequest(IntentRequest request, Session session) {
    	log.info("handleAddPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete(request.getTimestamp());
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
    	
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
    	
		// Got all three validated values at once.  Let's add it to the schedule.
    	if (schedule==null) {
    		schedule = new Schedule();
    	}
    	schedule.addPickupSchedule(pickupName, dow, tod);
    	sessionDao.setSchedule(schedule);
    	dynamoDao.writeUserData(sessionDao);
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupAddSingle(sessionDao, false, pickupName, dow, tod);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user deletes a specific pickup time.
     * <p>
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.
     * <p>
     * If we get a specific pickup (name, day-of-week, and time-of-day),
     * delete the one pickup time {@link Schedule#deletePickupTime(String, DayOfWeek, LocalTime)}, save the schedule 
     * to Dynamo DB, and let the user know.
     * <p>
     * If we find any missing or invalid data in the request, respond
     * appropriately so the user can correct and try again.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this request
     * @return Remove an entire pickup or a specific pickup time from
     * 			the schedule and let the user know.  Otherwise, complain
     * 			about any missing or invalid data from the user.
     */
    public SpeechletResponse handleDeletePickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeletePickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete(request.getTimestamp());
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
    	
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
    	if (schedule==null) {
    		schedule = new Schedule();
    		sessionDao.setSchedule(schedule);
    	}
    	Boolean removed = schedule.deletePickupTime(pickupName, dow, tod);
    	if (removed) {
    		sessionDao.setSchedule(schedule);
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondPickupDeleteSingle");
        SpeechletResponse response = ResponsesSchedule.respondPickupDeleteSingle(sessionDao, false, pickupName, dow, tod, removed);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    
    
    /**
     * Respond when the user deletes an entire pickup.
     * <p>
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.
     * <p>
     * If we find any missing or invalid data in the request, respond
     * appropriately so the user can correct and try again.
     * <p>
     * If we get only a pickup name from the user, {@link Schedule#deleteEntirePickup(String)},
     * save the schedule to Dynamo DB, and let the user know.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session} for this request
     * @return Remove an entire pickup from the schedule and let the 
     * 			user know.  Otherwise, complain about any missing or 
     * 			invalid data from the user.
     */
    public SpeechletResponse handleDeleteEntirePickupRequest(IntentRequest request, Session session) {
    	log.info("handleDeleteEntirePickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
    	sessionDao = new SessionDao(session);
    	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete(request.getTimestamp());
    	if (configNeeded != null) { return configNeeded; };
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);

    	// Respond if we're missing any data fields.
    	SlotPickupName slotPickupName = new SlotPickupName(request.getIntent());
		if (slotPickupName.isEmpty()) {
	    	sessionDao.incrementIntentLog(ldtRequest, "respondEntirePickupDeleteMissingName");
    		SpeechletResponse response = ResponsesSchedule.respondEntirePickupDeleteMissingName(sessionDao, true);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
    	// Respond if cannot validate any data fields.
    	String pickupName = slotPickupName.validate();
		if (pickupName==null) {
	    	sessionDao.incrementIntentLog(ldtRequest, "respondEntirePickupDeleteInvalidName");
    		SpeechletResponse response = ResponsesSchedule.respondEntirePickupDeleteInvalidName(sessionDao, true);
    		if (response.getShouldEndSession()) { flushIntentLog(); }
    		return response;
    	}
    	
		// Got needed validated value.  Let's delete it from the schedule.
    	Schedule schedule = loadSchedule(sessionDao, dynamoDao);
    	if (schedule==null) {
    		schedule = new Schedule();
    		sessionDao.setSchedule(schedule);
    	}
    	Boolean removed = schedule.deleteEntirePickup(pickupName);
    	if (removed) {
    		sessionDao.setSchedule(schedule);
        	dynamoDao.writeUserData(sessionDao);
    	}
    	
    	sessionDao.incrementIntentLog(ldtRequest, "respondEntirePickupDelete");
    	SpeechletResponse response = ResponsesSchedule.respondEntirePickupDelete(sessionDao, false, pickupName, removed);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }    

    /**
     * Respond when the user wants to delete the entire schedule.
     * <p>
     * Use {@link #loadSchedule(SessionDao, DynamoDao)} to load the pickup schedule from
     * the Session or Dynamo DB, if it exists.  If the schedule is already empty,
     * let the user know it is already cleared.
     * <p>
     * We require a confirmation from the user.  So store confirmation data
     * in the {@link SessionDao} to delete the entire schedule.
     * Then ask the user to confirm the schedule delete.
     * <p>
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
    	Schedule schedule = loadSchedule(sessionDao, dynamoDao);
    	
    	if ( schedule!=null ) {    	
	    	sessionDao.setConfirmationIntent(request.getIntent().getName());
	    	sessionDao.setConfirmationDescription("delete entire schedule"); 
    	}
    	SpeechletResponse response = ResponsesSchedule.respondScheduleDeleteAllRequested(sessionDao, false, schedule);
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }
    
    /**
     * Respond when the user says "Help"
     * <p>
     * Respond with help for using the Trash Day application.
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this Yes/No request
     * @return Commands to Add, Delete, and Query the weekly pickup schedule.
     */
    public SpeechletResponse handleHelpRequest(IntentRequest request, Session session) {
    	log.trace("handleHelpRequest()");
    	timeZone = loadTimeZone(sessionDao, dynamoDao);
    	if (timeZone!=null) {
        	sessionDao = new SessionDao(session);
        	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
        	sessionDao.incrementIntentLog(ldtRequest, "help");
    	}
    	SpeechletResponse response = ResponsesHelp.tellHelp();
		if (response.getShouldEndSession()) { flushIntentLog(); }
		return response;
    }

    /**
     * Respond when the user gives a Yes or No.
     * <p>
     * For some interactions, our application needs to ask the user for
     * a Yes/No answer.  Specifically, {@link handleDeleteEntireScheduleRequest})
     * needs to ask, "Are you sure you want to delete the entire schedule?"
     * <p>
     * When we ask the user a Yes/No question, we record in the session
     * an intent corresponding to the question we asked
     * ({@code SESSION_ATTR_CONFIRM_INTENT}) and a description of the action
     * we're contemplating ({@code SESSION_ATTR_CONFIRM_DESC}).
     * <p>
     * When the user gives a "Yes" response, we generate a specific response
     * based on the {@code SESSION_ATTR_CONFIRM_INTENT}.  We return helpful messages
     * when a question has not already been asked or the code does not understand
     * how to perform a given intent (code error).
     * 
     * @param request
     *            {@link IntentRequest} for this request
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this Yes/No request
     * @return a generic "Cancelled" response or a perform a specific
     * 			action corresponding to a previously stored intent
     */
    public SpeechletResponse handleYesRequest(IntentRequest request, Session session) {
    	log.info("handleYesRequest(sessionId={})", session.getSessionId());
    	sessionDao = new SessionDao(session);
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
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
        	Schedule schedule = loadSchedule(sessionDao, dynamoDao);
        	Boolean removed=false;
        	if (schedule!=null) {
            	removed = schedule.deleteEntireSchedule();
        		sessionDao.clearSchedule();
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
     * a Yes/No answer.  Specifically, {@link handleDeleteEntireScheduleRequest})
     * needs to ask, "Are you sure you want to delete the entire schedule?"
     * <p>
     * When we ask the user a Yes/No question, we record in the session
     * an intent corresponding to the question we asked
     * ({@code SESSION_ATTR_CONFIRM_INTENT}) and a description of the action
     * we're contemplating ({@code SESSION_ATTR_CONFIRM_DESC}).
     * <p>
     * When the user gives a "No" response, we return a generic "Cancelled"
     * response using the description from {@code SESSION_ATTR_CONFIRM_DESC}.
     * We return a helpful message when a question has not already been asked.
     * 
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this Yes/No request
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
     * as a "No" answer.  Cleanup and respond appropriately.
     * <p>
     * Otherwise, simply exit with a "Goodbye" message.
     * @param session
     *            {@link com.amazon.speech.speechlet.Session Session} for this request
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
