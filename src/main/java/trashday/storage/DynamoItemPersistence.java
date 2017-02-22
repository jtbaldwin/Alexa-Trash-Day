package trashday.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.Builder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;

/**
 * Client for DynamoDB persistence layer for the Trash Day 
 * skill.  Handles save/load of specific users' {@link trashday.model.Schedule}
 * data in Amazon's Dynamo DB.
 * 
 * @author	J. Todd Baldwin
 */
public class DynamoItemPersistence {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DynamoItemPersistence.class);
    
    /** Dynamo DB connection */
    private final AmazonDynamoDBClient dynamoDbLowLevel;
    /** Override the Dynamo DB Table Name, if defined.  Used for JUnit testing. */
    private String tableNameOverride = null;

    /**
     * Create a new DB client to save/load user Schedules in
     * Dynamo DB.
     * 
     * @param dynamoDBClient AmazonDynamoDBClient
     * 			The Dynamo DB connection
     */
    public DynamoItemPersistence(final AmazonDynamoDBClient dynamoDBClient) {
    	log.trace("TrashDayDynamoDbClient()");
        this.dynamoDbLowLevel = dynamoDBClient;
        this.tableNameOverride = null;
    }

    /**
     * Create a new DB client to save/load user Schedules in
     * Dynamo DB.
     * 
     * @param dynamoDBClient AmazonDynamoDBClient
     * 			The Dynamo DB connection
     * @param tableNameOverride String table name used by JUnit tests to ensure they do not
     * 				write to the Production table name (which is hard-coded using {@literal @}DynamoDBTable in
     * 				{@link trashday.storage.DynamoItem}).  A null value indicates no override.
     * 				Any other value is the Dynamo table name to be used.
     */
    public DynamoItemPersistence(final AmazonDynamoDBClient dynamoDBClient, final String tableNameOverride) {
    	log.trace("TrashDayDynamoDbClient()");
        this.dynamoDbLowLevel = dynamoDBClient;
        this.tableNameOverride = tableNameOverride;
    }

    /**
     * Creates a {@link DynamoDBMapper} using the default
     * configurations and, optionally, overriding the table
     * used to store items.
     * 
     * @return Mapper to allow persisting
     * 			{@link DynamoItem} objects to a Dynamo DB
     * table.
     */
    private DynamoDBMapper createDynamoDBMapper() {
    	if (tableNameOverride != null) {
        	log.trace("createDynamoDBMapper() with tableNameOverride={}", tableNameOverride);
	    	TableNameOverride tno = DynamoDBMapperConfig.TableNameOverride.withTableNameReplacement(tableNameOverride);
	    	Builder builder = new DynamoDBMapperConfig.Builder();
	    	builder.setTableNameOverride(tno);
	    	DynamoDBMapperConfig config = builder.build();        
	        return new DynamoDBMapper(dynamoDbLowLevel, config);
    	} else {
        	log.trace("createDynamoDBMapper()");
	        return new DynamoDBMapper(dynamoDbLowLevel);
    	}
    }
    
    /**
     * Loads an item from DynamoDB by primary Hash Key. Callers of this method should pass in an
     * object which represents an item in the DynamoDB table item with the primary key populated.
     * All fields in the item are populated based on the DynamoDB table entry.
     * 
     * @param tableItem DynamoItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session} data.
     * @return
     * 			{@link DynamoItem} completely populated from the Dynamo DB entry for 
     * 			the given customer id.  Or null if no item exists for this customer id.
     */
    public DynamoItem loadCompleteItem(final DynamoItem tableItem) {
    	log.trace("loadCompleteItem()");
    	if (tableItem==null) {
    		log.error("loadCompleteItem failed due to null tableItem");
    		return null;
    	}
    	DynamoDBMapper mapper = createDynamoDBMapper();
    	DynamoItem item = mapper.load(tableItem);
    	if (item==null) {
    		log.info("loadCompleteItem failed for userId={}", tableItem.getCustomerId());
    	}
        return item;
    }

    /**
     * Stores an item (skipping null item attributes) to DynamoDB.  Used to update database item when
     * caller does not care about certain fields.  Always skips setting the IntentLog attribute.
     * 
     * @param tableItem DynamoItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session} data and {@link trashday.model.Schedule}
     * 			already set based on a previous load or user
     * 			updates.
     */
    @SuppressWarnings("deprecation")
	public void saveItem(final DynamoItem tableItem) {
    	log.trace("saveItem()");
    	DynamoDBMapper mapper = createDynamoDBMapper();
    	DynamoItem item = new DynamoItem(tableItem);
    	item.clearSchedule();  // Ensure we never write Schedule entries again.
    	item.clearIntentLog(); // Never save intent log information as part of normal save process.
		mapper.save(item, SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES.config() );
    }
    
    /**
     * Stores a complete (does not skip null attributes) item to DynamoDB.  Used to update database item when
     * caller wants to be able to clear some fields (item attribute is null) or overwrite all fields (all
     * item fields take precedence over database).
     * 
     * @param tableItem DynamoItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session} data 
     * 			and {@link trashday.model.Calendar}
     * 			already set based on a previous load or user
     * 			updates.
     */
    public void saveCompleteItem(final DynamoItem tableItem) {
    	log.trace("saveCompleteItem()");
    	DynamoDBMapper mapper = createDynamoDBMapper();
    	DynamoItem item = new DynamoItem(tableItem);
		mapper.save(item);
    }
    
    /**
     * Stores the *only* intent log component of an item to DynamoDB.
     * 
     * @param tableItem DynamoItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session} data and {@link trashday.model.Schedule}
     * 			already set based on a previous load or user
     * 			updates.
     */
	public void saveOnlyIntentLog(final DynamoItem tableItem) {
    	log.trace("saveItem()");
    	DynamoItem item = new DynamoItem();
    	item.setCustomerId(tableItem.getCustomerId());
    	item.setIntentLog(tableItem.getIntentLog());
    	DynamoDBMapper mapper = createDynamoDBMapper();
		mapper.save(item, SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES.config() );
    }
    
    /**
     * Erase a user's item in DynamoDB.
     * 
     * @param tableItem DynamoItem
     * 			Item with customer id already set based on
     * 			user's {@link com.amazon.speech.speechlet.Session} data and {@link trashday.model.Schedule}
     * 			already set based on a previous load or user
     * 			updates.
     */
    public void eraseItem(final DynamoItem tableItem) {
    	log.info("eraseItem(): userId={}", tableItem.getCustomerId());
    	DynamoDBMapper mapper = createDynamoDBMapper();
        mapper.delete(tableItem);
    }

}
