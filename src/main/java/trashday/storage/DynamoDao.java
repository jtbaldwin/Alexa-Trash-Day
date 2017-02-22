package trashday.storage;

import java.time.LocalDateTime;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.model.Calendar;
import trashday.model.IntentLog;
import trashday.model.Schedule;
import trashday.ui.FormatUtils;

/**
 * Trash Day data access object for DynamoDB.
 * 
 * @author	J. Todd Baldwin
 */
@SuppressWarnings("deprecation")
public class DynamoDao {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DynamoDao.class);
    
    /** Object that loads specific users' data to/from Dynamo DB */
    private final DynamoItemPersistence dynamoDbItemPersistence;

    /**
     * Create database access object
     * 
     * @param dynamoItemPersistence TrashDayDynamoDbClient to use for 
     * 			saving and loading user information.
     */
    public DynamoDao(DynamoItemPersistence dynamoItemPersistence) {
    	log.trace("DynamoDao({})", dynamoItemPersistence);
        this.dynamoDbItemPersistence = dynamoItemPersistence;
    }
    
    /**
     * Appends given user intent log data into this user's Dynamo DB entry log data.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param intentLog {@link trashday.model.IntentLog}
     * 			Log information to be appended into the user's database entry
     */
    public void appendIntentLogData(SessionDao sessionDao, IntentLog intentLog) {
    	log.trace("appendIntentLogData(intentLog={})", intentLog);
    	if (intentLog == null) { return; }
    	
    	// Load this user's Dynamo DB item.
        DynamoItem item = new DynamoItem();
        String userId = sessionDao.getUserId();
        item.setCustomerId(userId);
        item = dynamoDbItemPersistence.loadCompleteItem(item);
        if (item == null) {
        	log.info("No TrashDayDynamoItem available in DynamoDB.  No intent log information saved for this user: {}", userId);
            return;
        }
        log.info("Read user data from Dynamo DB: userId={}", userId);
        
		IntentLog storedIntentLog = item.getIntentLog();
		if (storedIntentLog == null) {
			storedIntentLog = intentLog;
			log.info("Empty intent log from DynamoDB. Storing current intent log: {}", storedIntentLog.toStringPrintable());
		} else {
			log.info("Appending intent log items: {}",intentLog.toStringPrintable());
	    	storedIntentLog.join(intentLog);
		}
		storedIntentLog.prune(12);
		item.setIntentLog(storedIntentLog);
        dynamoDbItemPersistence.saveOnlyIntentLog(item);
        
        log.info("Wrote intent log data to Dynamo DB: userId={} intentLog={}", userId, intentLog.toStringPrintable());
    }
        
    /**
     * Reads user data from Dynamo DB and stores in the {@link SessionDao}.
     * Uses {@link SessionDao#getUserId()} to read the correct database table
     * item for this particular user.  Loaded data is stored in the user's
     * session data via the {@link SessionDao}.
     * <p>
     * Returns null if the user's item could not be found in the database.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @return true if a database item was found and read for this user
     */
	public boolean readUserData(SessionDao sessionDao) {
    	log.trace("readUserData()");
    	
    	// Load a user's item from the DynamoDB table.
        DynamoItem item = new DynamoItem();
        String userId = sessionDao.getUserId();
        item.setCustomerId(userId);
        item = dynamoDbItemPersistence.loadCompleteItem(item);
        if (item == null) {
        	log.info("No TrashDayDynamoItem available in DynamoDB for this user: {}", userId);
            return false;
        }
        log.info("Read user data from Dynamo DB: userId={}", userId);
        
        // Load Calendar data from item into Session.
		Schedule schedule = item.getSchedule();
        Calendar calendar = item.getCalendar();
        if ( (schedule!=null) && (calendar==null) ) {
            // Convert from old Schedule to new Calendar.
    		log.info("Creating pickup calendar from Schedule={}", schedule.toStringPrintable());
    		calendar = new Calendar(schedule);
    		schedule = null;
    		log.info("New calendar from Schedule={}", FormatUtils.printableCalendar(calendar, LocalDateTime.now()));
    		item.clearSchedule();
    		item.setCalendar(calendar);
    		dynamoDbItemPersistence.saveCompleteItem(item);
        }
		if (calendar != null) {
    		log.info("Loaded calendar.");
    		sessionDao.setCalendar(calendar);
		}
        
        // Load TimeZone data from item into Session.
		TimeZone timeZone = item.getTimeZone();
		if (timeZone != null) {
    		log.info("Loaded time zone from DynamoDB.");
    		sessionDao.setTimeZone(timeZone);
		}
		
		// NOTE: Do NOT load IntentLog into the Session.  Intent log entries are appended to Session
		// and later *appended* when written to Dynamo DB.
		
        return true;
    }

    /**
     * Saves user data from the {@link SessionDao} into the Dynamo DB.  Does NOT write
     * intent log information.  Does NOT write Schedule information as that is deprecated and
     * Calendar is used instead.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     */
    public void writeUserData(SessionDao sessionDao) {
    	log.trace("writeUserData()");
    	
    	Calendar calendar = sessionDao.getCalendar();
    	TimeZone timeZone = sessionDao.getTimeZone();
    	
        String userId = sessionDao.getUserId();
        DynamoItem item = new DynamoItem();
        item.setCustomerId(userId);
        item.setCalendar(calendar);
        item.setIntentLog(null);
        item.setTimeZone(timeZone);
        dynamoDbItemPersistence.saveItem(item);

        log.info("Wrote user data to Dynamo DB: userId={}", userId);
    }
    
    /**
     * Delete all user data when the clear their entire schedule.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     */
    public void eraseUserData(SessionDao sessionDao) {
    	log.info("eraseUserData()");
    	
        String userId = sessionDao.getUserId();
        DynamoItem item = new DynamoItem();
        item.setCustomerId(userId);
        dynamoDbItemPersistence.eraseItem(item);
        log.info("Erased user data from Dynamo DB: userId={}", userId);
    }

}
