package trashday.storage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.Session;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;

import trashday.model.IntentLog;
import trashday.model.Schedule;

/**
 * Trash Day data access object for Alexa Session.
 * 
 * @author	J. Todd Baldwin
 */
public class SessionDao {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SessionDao.class);
    
    /** Object that loads specific users' Schedules in Dynamo DB */
    private Session session;
    
	/** A Jackson object mapper configured to handle Java 8 LocalDateTime objects and Jon Peterson's object versioning module. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
 		   .registerModule(new ParameterNamesModule())
 		   .registerModule(new Jdk8Module())
 		   .registerModule(new JavaTimeModule())
 		   .registerModule(new VersioningModule());

    /** {@link Session} attribute key to store user is currently configuring their {@link trashday.model.Schedule}. */
    public static final String SESSION_ATTR_SCHEDULE_IN_PROGRESS = "trashDayScheduleConfigInProgress";
    /** {@link Session} attribute key to store user's {@link trashday.model.Schedule} */
	public static final String SESSION_ATTR_SCHEDULE = "trashDaySchedule";
    /** {@link Session} attribute key to store user's {@link java.util.TimeZone} */
	public static final String SESSION_ATTR_TIMEZONE = "trashDayTimeZone";
    /** {@link com.amazon.speech.speechlet.Session Session} attribute key to store intent names that require a Yes/No confirmation */
	public static final String SESSION_ATTR_CONFIRM_INTENT = "trashDayConfirmIntent";
    /** {@link com.amazon.speech.speechlet.Session Session} attribute key to store user text description of actions that require a Yes/No confirmation */
	public static final String SESSION_ATTR_CONFIRM_DESC = "trashDayConfirmAction";
	
    /** {@link com.amazon.speech.speechlet.Session Session} attribute key to store if overall help card is already sent in this session. */
	public static final String SESSION_ATTR_OVERALL_HELP_CARD_SENT = "trashDayOverallHelpCardSent";
    /** {@link com.amazon.speech.speechlet.Session Session} attribute key to store if Time Zone help card is already sent in this session. */
	public static final String SESSION_ATTR_TIMEZONE_HELP_CARD_SENT = "trashDayTimeZoneHelpCardSent";
    /** {@link com.amazon.speech.speechlet.Session Session} attribute key to store if Schedule help card is already sent in this session. */
	public static final String SESSION_ATTR_SCHEDULE_HELP_CARD_SENT = "trashDayScheduleHelpCardSent";
	/** {@link com.amazon.speech.speechlet.Session Session} attribute key to store a log of intents performed */
	public static final String SESSION_ATTR_INTENT_LOG = "trashDayIntentLog";
	/** {@link com.amazon.speech.speechlet.Session Session} attribute key to store a flag when intent log is updated so we know to store it later to Dynamo DB */
	public static final String SESSION_ATTR_INTENT_LOG_UPDATED = "trashDayIntentLogUpdated";
	
    /**
     * Create data access object for a given {@link Session}
     * 
     * @param session Session
     * 			The Alexa {@link Session} that we will be using to store Trash Day information.
     */
    public SessionDao(Session session) {
    	log.trace("SessionDao()");
        this.session = session;
    }
    
	/**
	 * Parse a TimeZone from an arbitrary Object.  The session attributes may
	 * end up with Strings, Json, or actual {@link java.util.TimeZone} objects
	 * when we read the attributes.  This method takes an arbitrary object that
	 * we expect to hold a {@link java.util.TimeZone} and tries to parse it
	 * correctly.
	 * 
	 * @param o Object that should be parsable as a {@link java.util.TimeZone}
	 * @return TimeZone object
	 */
	private TimeZone parseTimeZoneFromObject(Object o) {
		log.trace("parseTimeZoneFromObject({})", o);
		TimeZone timeZone = null;
		
		String cName = o.getClass().getName();
		if (cName.equals("sun.util.calendar.ZoneInfo") || cName.equals("java.util.TimeZone")) {
			log.debug("Parse TimeZone from {} object.", cName);
			timeZone = (TimeZone) o;
    		return timeZone;
		}
		else if (cName.equals("java.lang.String")) {
			log.debug("Parse TimeZone from {} object.", cName);
			return TimeZone.getTimeZone((String) o);
		}
		log.error("Cannot parse time zone from current session (class={}, string={}) ", cName, o.toString());
		return timeZone;
	}
	
    /**
     * Get session attribute that contains a description of the confirmation action
     * currently in progress.
     * 
     * @return String description
     */
    public String getConfirmationDescription() {
		return (String) session.getAttribute(SESSION_ATTR_CONFIRM_DESC);
    }
    
    /**
     * Get session attribute that contains the intent name of the confirmation action
     * currently in progress.  This is the string the application uses to know
     * what action to call on a user's "Yes" response.
     * 
     * @return String intent name
     */
    public String getConfirmationIntent() {
    	return (String) session.getAttribute(SESSION_ATTR_CONFIRM_INTENT);
    }
    
    /**
     * Load a given {@link trashday.model.IntentLog} from the {@link Session}.
     * 
     * @return if existing, the {@link trashday.model.IntentLog}
     */
    public IntentLog getIntentLog() {
    	log.trace("getIntentLog");
    	
    	// Load schedule data from the session, if exists.
    	Object o = session.getAttribute(SESSION_ATTR_INTENT_LOG);
    	IntentLog intentLog = null;
    	if (o != null) {
    		if (o.getClass().equals(IntentLog.class)) {
    			intentLog = (IntentLog) o;
    			log.info("Using Intent Log from current session. intentLog={}", intentLog.toStringPrintable());
    			return intentLog;
    		}
    		try {
    			String s = o.toString();
    			log.trace("Deserialize this: {}",s);
    			intentLog = OBJECT_MAPPER.readValue(s, new TypeReference<IntentLog>() { } );
    		} catch (JsonParseException e) {
				log.error("JsonParsingException: {}",e.getMessage());
			} catch (JsonMappingException e) {
				log.error("JsonMappingException: {}",e.getMessage());
			} catch (IOException e) {
				log.error("IOException: {}",e.getMessage());
			}
    		if (intentLog != null) {
    			log.info("Using Intent Log from current session. intentLog={}", intentLog.toStringPrintable());
    			return intentLog;
    		}
    	}
    	
    	// No schedule data in the DB, run configuration conversation
    	log.info("No Intent Log available from Session.");
    	return null;
    }
    
    /**
     * Load the session attribute that tracks if the intent log has been
     * updated and return true/false.
     * 
     * @return true if flag exists and is true
     */
    public boolean getIntentLogUpdated() {
    	Object o = session.getAttribute(SESSION_ATTR_INTENT_LOG_UPDATED);
    	if ( o == null) { return false; };
    	if (o.getClass().getName().equals("java.lang.Boolean")) {
    		Boolean updated = (Boolean) o;
    		return updated.booleanValue();
    	}
    	return false;
    }
    
    /**
     * Load the session attribute that tracks if the overall help card has
     * been sent to the user during this session.
     * 
     * @return true if flag exists and is true
     */
    public boolean getOverallHelpCardSent() {
    	Object o = session.getAttribute(SESSION_ATTR_OVERALL_HELP_CARD_SENT);
    	if ( o == null) { return false; };
    	if (o.getClass().getName().equals("java.lang.Boolean")) {
    		Boolean sent = (Boolean) o;
    		return sent.booleanValue();
    	}
    	return false;
    }
    
   /**
     * Load a given {@link trashday.model.Schedule} from the {@link Session}.
     * 
     * @return if existing, the {@link trashday.model.Schedule}
     */
    public Schedule getSchedule() {
    	log.trace("getSchedule");
    	
    	// Load schedule data from the session, if exists.
    	Object o = session.getAttribute(SESSION_ATTR_SCHEDULE);
    	Schedule schedule = null;
    	if (o != null) {
    		if (o.getClass().equals(Schedule.class)) {
    			schedule = (Schedule) o;
    			log.info("Using pickup Schedule from current session. schedule={}", schedule.toStringPrintable());
    			return schedule;
    		}
    		try {
    			String s = o.toString();
    			log.trace("Deserialize this: {}",s);
    			schedule = OBJECT_MAPPER.readValue(s, new TypeReference<Schedule>() { } );
    		} catch (JsonParseException e) {
				log.error("JsonParsingException: {}",e.getMessage());
			} catch (JsonMappingException e) {
				log.error("JsonMappingException: {}",e.getMessage());
			} catch (IOException e) {
				log.error("IOException: {}",e.getMessage());
			}
    		if (schedule != null) {
    			log.info("Using pickup schedule from current session. schedule={}", schedule.toStringPrintable());
    			return schedule;
    		}
    	}
    	
    	// No schedule data in the DB, run configuration conversation
    	log.info("No pickup schedule available from Session.");
    	return null;
    }
    
    /**
     * Load the session attribute that tracks if the schedule help card has
     * been sent to the user during this session.
     * 
     * @return true if flag exists and is true
     */
    public boolean getScheduleHelpCardSent() {
    	Object o = session.getAttribute(SESSION_ATTR_SCHEDULE_HELP_CARD_SENT);
    	if ( o == null) { return false; };
    	if (o.getClass().getName().equals("java.lang.Boolean")) {
    		Boolean sent = (Boolean) o;
    		return sent.booleanValue();
    	}
    	return false;
    }
    
    /**
     * Load the session attribute that tracks if we are currently in a conversation
     * with the user about configuring the schedule.  Used to differentiate from
     * one-shot "Alexa, tell Trash Day, to add mail pickup on Monday at 4pm." or
     * user is answering our prompt with "Add mail pickup on Monday at 4pm."
     * 
     * @return true if flag exists and is true
     */
    public boolean getScheduleConfigInProgress() {
    	Object o = session.getAttribute(SESSION_ATTR_SCHEDULE_IN_PROGRESS);
    	if ( o == null) { return false; };
    	if (o.getClass().getName().equals("java.lang.Boolean")) {
    		Boolean inProgress = (Boolean) o;
    		return inProgress.booleanValue();
    	}
    	return false;
    }
    
    /**
     * Load a given {@link java.util.TimeZone} from the {@link Session}.
     * 
     * @return if existing, the {@link java.util.TimeZone}
     */
    public TimeZone getTimeZone() {
    	log.trace("getTimeZone");
    	
    	// Load time zone data from the session, if exists.
    	Object o = session.getAttribute(SESSION_ATTR_TIMEZONE);
    	TimeZone timeZone = null;
    	if (o != null) {
    		timeZone=parseTimeZoneFromObject(o);
    		if (timeZone != null) {
    			log.info("Using time zone from current session. timeZone={}", timeZone.getID());
    			return timeZone;
    		}
			log.warn("Cannot create TimeZone from given Object (class="+o.getClass().getName()+", string="+o.toString()+")");
    	}
    	log.info("No time zone data available from Session");
    	return null;
    }
    
    /**
     * Load the session attribute that tracks if the time zone help card has
     * been sent to the user during this session.
     * 
     * @return true if flag exists and is true
     */
    public boolean getTimeZoneHelpCardSent() {
    	Object o = session.getAttribute(SESSION_ATTR_TIMEZONE_HELP_CARD_SENT);
    	if ( o == null) { return false; };
    	if (o.getClass().getName().equals("java.lang.Boolean")) {
    		Boolean sent = (Boolean) o;
    		return sent.booleanValue();
    	}
    	return false;
    }
    
    /**
     * Get the user id from the current {@link com.amazon.speech.speechlet.Session Session}.
     * Use this information to read/write Dynamo DB database object per-user.
     * 
     * @return String user ID
     */
    public String getUserId() {
    	return session.getUser().getUserId();
    }
    
    /**
     * When user completes a Trash Day action, we increment an entry in the
     * intent log stored in the current session attributes.
     * 
     * @param ldtRequest LocalDateTime this request occurred, so we know what
     * 			year-week to log this activity.
     * @param intentName String of the activity user performed.
     */
    public void incrementIntentLog(LocalDateTime ldtRequest, String intentName) {
    	if (ldtRequest != null) {
	    	IntentLog intentLog = getIntentLog();
	    	if (intentLog == null) {
	    		intentLog = new IntentLog();
	    	}
	    	intentLog.incrementIntent(ldtRequest, intentName);
	    	setIntentLog(intentLog);
			session.setAttribute(SESSION_ATTR_INTENT_LOG_UPDATED, true);
    	}
    }
    
    /**
     * Set session attribute that contains a description of the confirmation action
     * currently in progress.
     * 
     * @param description String that is a name for what function to perform if/when the
     * 			user answers "Yes" to performing this action.
     */
	public void setConfirmationDescription(String description) {
    	session.setAttribute(SESSION_ATTR_CONFIRM_DESC, description);
	}
    
    /**
     * Set session attribute that contains the intent name of the confirmation action
     * currently in progress.  This is the string the application uses to know
     * what action to call on a user's "Yes" response.
     * 
     * @param intentName String that represents what function to call if/when the
     * 			user answers "Yes" to performing this action.
     */
    public void setConfirmationIntent(String intentName) {
    	session.setAttribute(SESSION_ATTR_CONFIRM_INTENT, intentName);
    }
    
    /**
     * Save a given {@link trashday.model.IntentLog} to the {@link Session}.
     * 
     * @param intentLog
     * 	          {@link trashday.model.IntentLog} to be saved
     */
    public void setIntentLog(IntentLog intentLog) {
    	log.trace("setIntentLog");
        try {
			String json = OBJECT_MAPPER.writeValueAsString(intentLog);
			session.setAttribute(SESSION_ATTR_INTENT_LOG, json);
			log.info("Saved intent log to current session. intentLog={}", json);
		} catch (JsonProcessingException e) {
			log.error("Failed to save intent log to current session. JsonProcessingException={}", e.getMessage());
		}
    }
    
    /**
     * Set the session attribute that tracks if the overall help card has
     * been sent to the user during this session.
     */
    public void setOverallHelpCardSent() {
    	Boolean sent = new Boolean(true);
    	session.setAttribute(SESSION_ATTR_OVERALL_HELP_CARD_SENT, sent);
    }
    
    /**
     * Save a given {@link trashday.model.Schedule} to the {@link Session}.
     * 
     * @param schedule
     * 	          {@link trashday.model.Schedule} to be saved
     */
    public void setSchedule(Schedule schedule) {
    	log.trace("setSchedule");
        schedule.validate();
        try {
			String json = OBJECT_MAPPER.writeValueAsString(schedule);
			session.setAttribute(SESSION_ATTR_SCHEDULE, json);
			log.info("Saved schedule to current session. schedule={}", json);
		} catch (JsonProcessingException e) {
			log.error("Failed to save schedule to current session. JsonProcessingException={}", e.getMessage());
		}
    }
    
    /**
     * Set the session attribute that tracks if the schedule help card has
     * been sent to the user during this session.
     */
    public void setScheduleHelpCardSent() {
    	Boolean sent = new Boolean(true);
    	session.setAttribute(SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, sent);
    }
    
    /**
     * Set the session attribute that tracks if we are currently in a conversation
     * with the user about configuring the schedule.  Used to differentiate from
     * one-shot "Alexa, tell Trash Day, to add mail pickup on Monday at 4pm." or
     * user is answering our prompt with "Add mail pickup on Monday at 4pm."
     */
    public void setScheduleConfigInProgress() {
    	Boolean inProgress = new Boolean(true);
    	session.setAttribute(SESSION_ATTR_SCHEDULE_IN_PROGRESS, inProgress);
    }
    
    /**
     * Save a given {@link java.util.TimeZone} to the {@link Session}.
     * 
     * @param timeZone
     * 	          {@link java.util.TimeZone} to be saved
     */
    public void setTimeZone(TimeZone timeZone) {
    	log.trace("setTimeZone");
		session.setAttribute(SESSION_ATTR_TIMEZONE, timeZone);
		log.info("Saved time zone to current session. timeZone={}", timeZone.getID());
    }
    
    /**
     * Set the session attribute that tracks if the time zone help card has
     * been sent to the user during this session.
     */
    public void setTimeZoneHelpCardSent() {
    	Boolean sent = new Boolean(true);
    	session.setAttribute(SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, sent);
    }
    
    /**
     * Remove session attribute that contains the intent name of the confirmation action
     * currently in progress.  This is the string the application uses to know
     * what action to call on a user's "Yes" response.
     */
    public void clearConfirmationIntent() {
    	session.removeAttribute(SESSION_ATTR_CONFIRM_INTENT);
    }

    /**
     * Remove session attribute that contains a description of the confirmation action
     * currently in progress.
     */
    public void clearConfirmationDescription() {
    	session.removeAttribute(SESSION_ATTR_CONFIRM_DESC);
    }
    
    /**
     * Remove both session attributes that contain a description and intent name of 
     * the confirmation action currently in progress.
     */
    public void clearConfirmationData() {
    	clearConfirmationIntent();
    	clearConfirmationDescription();
    }

    /**
     * Clear {@link trashday.model.IntentLog} from the {@link Session}.
     */
    public void clearIntentLog() {
    	session.removeAttribute(SESSION_ATTR_INTENT_LOG);
    	session.removeAttribute(SESSION_ATTR_INTENT_LOG_UPDATED);
    }
    
    /**
     * Clear {@link trashday.model.Schedule} from the {@link Session}.
     */
    public void clearSchedule() {
    	session.removeAttribute(SESSION_ATTR_SCHEDULE);
    }
    
    /**
     * Clear the session attribute that tracks if the schedule help card has
     * been sent to the user during this session.
     */
    public void clearScheduleHelpCardSent() {
    	session.removeAttribute(SESSION_ATTR_SCHEDULE_HELP_CARD_SENT);
    }
    
    /**
     * Clear the session attribute that tracks if we are currently in a conversation
     * with the user about configuring the schedule.  Used to differentiate from
     * one-shot "Alexa, tell Trash Day, to add mail pickup on Monday at 4pm." or
     * user is answering our prompt with "Add mail pickup on Monday at 4pm."
     */
    public void clearScheduleConfigInProgress() {
    	session.removeAttribute(SESSION_ATTR_SCHEDULE_IN_PROGRESS);
    }
    
    /**
     * Clear {@link java.util.TimeZone} from the {@link Session}.
     */
    public void clearTimeZone() {
		session.removeAttribute(SESSION_ATTR_TIMEZONE);
    }
    
    /**
     * Clear the session attribute that tracks if the time zone help card has
     * been sent to the user during this session.
     */
    public void clearTimeZoneHelpCardSent() {
    	session.removeAttribute(SESSION_ATTR_TIMEZONE_HELP_CARD_SENT);
    }
    
}
