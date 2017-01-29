package trashday.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;

/**
 * Client for DynamoDB persistence layer for the Trash Day 
 * skill.  Handles save/load of specific users' {@link trashday.model.Schedule}
 * data in Amazon's Dynamo DB.
 * 
 * @author	J. Todd Baldwin
 */
public class DynamoDbClient {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DynamoDbClient.class);
    /** Dynamo DB connection */
    private final AmazonDynamoDBClient dynamoDBClient;


    /**
     * Create a new DB client to save/load user Schedules in
     * Dynamo DB.
     * 
     * @param dynamoDBClient AmazonDynamoDBClient
     * 			The Dynamo DB connection
     */
    public DynamoDbClient(final AmazonDynamoDBClient dynamoDBClient) {
    	log.trace("TrashDayDynamoDbClient()");
        this.dynamoDBClient = dynamoDBClient;
    }

    /**
     * Getter for the Dynamo DB connection itself.  Used only 
     * by JUnit tests so we can do database table manipulation
     * for test data.
     * 
     * @return The Dynamo DB connection
     */
    public AmazonDynamoDBClient getAmazonDynamoDBClient() {
    	log.trace("TrashDayDynamoDbClient.getAmazonDynamoDBClient()");
        return dynamoDBClient;
    }

    /**
     * Loads an item from DynamoDB by primary Hash Key. Callers of this method should pass in an
     * object which represents an item in the DynamoDB table item with the primary key populated.
     * 
     * @param tableItem TrashDayScheduleItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session Session} data.
     * @return
     * 			Database table item retrieved based on user's
     * 			customer id.
     */
    public DynamoItem loadItem(final DynamoItem tableItem) {
    	log.trace("loadItem()");
    	if (tableItem==null) {
    		log.error("loadItem failed due to null tableItem");
    		return null;
    	}
    	DynamoDBMapper mapper = createDynamoDBMapper();
    	DynamoItem item = mapper.load(tableItem);
    	if (item==null) {
    		log.info("loadItem failed for userId={}", tableItem.getCustomerId());
    	}
        return item;
    }

    /**
     * Stores an item to DynamoDB.
     * 
     * @param tableItem TrashDayScheduleItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session Session} data and {@link trashday.model.Schedule}
     * 			already set based on a previous load or user
     * 			updates.
     */
    public void saveItem(final DynamoItem tableItem) {
    	log.trace("saveItem()");
    	DynamoDBMapper mapper = createDynamoDBMapper();
    	tableItem.clearIntentLog();
		mapper.save(tableItem, SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES.config() );
    }
    
    /**
     * Stores the intent log component of an item to DynamoDB.
     * 
     * @param tableItem TrashDayScheduleItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session Session} data and {@link trashday.model.Schedule}
     * 			already set based on a previous load or user
     * 			updates.
     */
    public void saveOnlyIntentLog(final DynamoItem tableItem) {
    	log.trace("saveItem()");
    	tableItem.clearSchedule();
    	tableItem.clearTimeZone();
    	DynamoDBMapper mapper = createDynamoDBMapper();
		mapper.save(tableItem, SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES.config() );
    }
    
    /**
     * Erase a user's item in DynamoDB.
     * 
     * @param tableItem TrashDayScheduleItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session Session} data and {@link trashday.model.Schedule}
     * 			already set based on a previous load or user
     * 			updates.
     */
    public void eraseItem(final DynamoItem tableItem) {
    	log.info("eraseItem(): userId={}", tableItem.getCustomerId());
    	DynamoDBMapper mapper = createDynamoDBMapper();
        mapper.delete(tableItem);
    }

    /**
     * Creates a {@link DynamoDBMapper} using the default
     * configurations.
     * 
     * @return Mapper to allow persisting
     * 			{@link DynamoItem} objects to a Dynamo DB
     * table.
     */
    private DynamoDBMapper createDynamoDBMapper() {
    	log.trace("createDynamoDBMapper()");
        return new DynamoDBMapper(dynamoDBClient);
    }
}
