package trashday.storage;

import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.model.IntentLog;
import trashday.model.Schedule;

/**
 * Trash Day data access object for DynamoDB.
 * 
 * @author	J. Todd Baldwin
 */
public class DynamoDao {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DynamoDao.class);
    /** Object that loads specific users' data to/from Dynamo DB */
    private final DynamoDbClient dynamoDbClient;

    /**
     * Create database access object
     * 
     * @param dynamoDbClient TrashDayDynamoDbClient to use for 
     * 			saving and loading user information.
     */
    public DynamoDao(DynamoDbClient dynamoDbClient) {
    	log.info("TrashDayDao()");
        this.dynamoDbClient = dynamoDbClient;
    }
    
    /**
     * Appends given user intent log data into this user's Dynamo DB entry log data.
     * 
     * @param sessionDao {@link SessionDao}
     * 			Access to the current Alexa session for this Trash Day user's data.
     * @param intentLog {@link trashday.model.IntentLog}
     * 			Log information to be appended into the user's database entry
     */
    public void appendIntentLogData(SessionDao sessionDao, IntentLog intentLog) {
    	log.trace("appendIntentLogData()");
    	if (intentLog == null) { return; }
    	
    	// Load this user's Dynamo DB item.
        DynamoItem item = new DynamoItem();
        String userId = sessionDao.getUserId();
        item.setCustomerId(userId);
        item = dynamoDbClient.loadItem(item);
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
        dynamoDbClient.saveOnlyIntentLog(item);
        
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
     * @param sessionDao {@link SessionDao}
     * 			Access to the current Alexa session for this Trash Day user's data.
     * @return true if a database item was found and read for this user
     */
    public boolean readUserData(SessionDao sessionDao) {
    	log.trace("readUserData()");
    	
        DynamoItem item = new DynamoItem();
        String userId = sessionDao.getUserId();
        item.setCustomerId(userId);
        item = dynamoDbClient.loadItem(item);
        if (item == null) {
        	log.info("No TrashDayDynamoItem available in DynamoDB for this user: {}", userId);
            return false;
        }
        log.info("Read user data from Dynamo DB: userId={}", userId);
        Schedule schedule = item.getSchedule();
		if (schedule != null) {
    		log.info("Loaded pickup schedule from DynamoDB.");
    		sessionDao.setSchedule(schedule);
		}
		TimeZone timeZone = item.getTimeZone();
		if (timeZone != null) {
    		log.info("Loaded time zone from DynamoDB.");
    		sessionDao.setTimeZone(timeZone);
		}
        return true;
    }

    /**
     * Saves user data from the {@link SessionDao} into the Dynamo DB.
     * 
     * @param sessionDao {@link SessionDao}
     * 			Access to the current Alexa session for this Trash Day user's data.
     */
    public void writeUserData(SessionDao sessionDao) {
    	log.trace("writeUserData()");
    	
    	Schedule schedule = sessionDao.getSchedule();
    	TimeZone timeZone = sessionDao.getTimeZone();
    	
        String userId = sessionDao.getUserId();
        DynamoItem item = new DynamoItem();
        item.setCustomerId(userId);
        item.setIntentLog(null);
        item.setSchedule(schedule);
        item.setTimeZone(timeZone);
        dynamoDbClient.saveItem(item);

        log.info("Wrote user data to Dynamo DB: userId={}", userId);
    }
    
    /**
     * Delete all user data when the clear their entire schedule.
     * 
     * @param sessionDao {@link SessionDao}
     * 			Access to the current Alexa session for this Trash Day user's data.
     */
    public void eraseUserData(SessionDao sessionDao) {
    	log.info("eraseUserData()");
    	
        String userId = sessionDao.getUserId();
        DynamoItem item = new DynamoItem();
        item.setCustomerId(userId);
        dynamoDbClient.eraseItem(item);
        log.info("Erased user data from Dynamo DB: userId={}", userId);
    }

}
