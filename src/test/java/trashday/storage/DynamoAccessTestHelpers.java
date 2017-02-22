package trashday.storage;

import static org.junit.Assert.assertEquals;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.User;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DeleteItemOutcome;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.DeleteItemSpec;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import com.amazonaws.services.dynamodbv2.model.CreateTableResult;
import com.amazonaws.services.dynamodbv2.model.DeleteItemResult;
import com.amazonaws.services.dynamodbv2.model.DeleteTableResult;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ResourceInUseException;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.amazonaws.services.dynamodbv2.model.ScanRequest;
import com.amazonaws.services.dynamodbv2.model.ScanResult;
import com.amazonaws.services.dynamodbv2.model.TableDescription;

import trashday.model.Calendar;
import trashday.model.IntentLog;
import trashday.model.Schedule;

@SuppressWarnings("deprecation")
public class DynamoAccessTestHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DynamoAccessTestHelpers.class);
    
    /** Table to store all user schedules */
	protected static final String tableName = "TrashDayScheduleDataTest";
	/** Each test grabs unique customer ID(s) via {@link #getNextCustomerId()} to avoid problems if/when we run the tests multi-threaded. */
	protected static int customerIdNumber = 0;

	/** DynamoDB Client for the Low-Level Interfaces: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.SDKs.Interfaces.LowLevel.html */
	protected static AmazonDynamoDBClient dynamoDbLowLevel = null;
	/** DynamoDB Client for the Document Interfaces: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.SDKs.Interfaces.Document.html */
	protected static DynamoDB dynamoDbDocument = null;
	/** DynamoDB Client for the Object Persistence Interface: http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Programming.SDKs.Interfaces.Mapper.html */
	protected static DynamoItemPersistence dynamoDbItemPersistence = null;

	/**
	 * Table management helper function that creates a Dynamo DB table
	 * suitable for storing user Schedules.
	 * 
	 * @return null on failure.  Otherwise a CreateTableResult that includes 
	 * 		information about the newly-created table. 
	 */
    public static CreateTableResult tableCreate() {
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
			ctr = dynamoDbLowLevel.createTable(attributeDefinitions, tableName, keySchema, provisionedThroughput);
			log.info("Created Table: {}", ctr.toString());
		} catch (ResourceInUseException ex) {
			log.info("table create exception: {}", ex.getErrorMessage());
		} catch (AmazonDynamoDBException ex) {
			log.info("table create exception: {}", ex.getErrorMessage());			
		}
		return ctr;
    }
    
	/**
	 * Table management helper function that deletes a Dynamo DB table.
	 * 
	 * @return true if table was successfully deleted
	 */
    public static boolean tableDelete() {
		log.info("Delete {} table", tableName);
		DeleteTableResult dtr;
		try {
			dtr = dynamoDbLowLevel.deleteTable(tableName);
		} catch (ResourceNotFoundException ex) {
			log.info("Table {} not found.", tableName);
			return false;
		}
		log.info("Deleted Table: {}", dtr.toString());
		return true;
    }
    
	/**
	 * Table management helper function that (re-)creates a Dynamo DB table
	 * suitable for storing user Schedules.
	 */
	public void tableClear() {
		tableDelete();
		
		tableCreate();
		
        assertEquals((Long) 0L, tableItemCount());		
	}
	
	public static Long tableItemCount() {
		Table table = dynamoDbDocument.getTable(tableName);
		TableDescription td = table.describe();
		return td.getItemCount();
	}
	
	public static synchronized String getNextCustomerId() {
		customerIdNumber++;
		return "test-customer-"+customerIdNumber;
	}
	
	public static synchronized void resetCustomerId() {
		customerIdNumber=0;
	}
	
	public String ignoreCalendarUidInfo(String icalText) {
		return icalText
				.replaceAll("UID:(\\d+T\\d+Z)", "UID:XXXXXXXXXXXX")
				.replaceAll("DTSTAMP:(\\d+T\\d+Z)", "DTSTAMP:XXXXXXXXXXXX");
	}

	/**
	 * Create a table item using "document" db access in version 1 format.  Contains fields: CustomerId,
	 * TimeZone, Schedule, and IntentLog.
	 * 
	 * @param customerId String of this user's id
	 * @return PutItemOutcome of the item write
	 */
	public PutItemOutcome itemCreateVersion1(String customerId) {
        // Create a manual v1 item
        Table table = dynamoDbDocument.getTable(tableName);
        Item itemModelVersion1 = new Item()
        	    .withPrimaryKey("CustomerId", customerId)
        	    .withString("IntentLog", "{\"log\":{\"2017-05\":{\"open\":2,\"respondPickupAddSingle\":1,\"respondTimeZoneUpdatedScheduleMissing\":1,\"tellSchedule\":2}},\"modelVersion\":\"1\"}")
        	    .withString("TimeZone", "US/Eastern")
        	    .withString("Schedule", "{\"pickupNames\":[\"trash\"],\"pickupSchedule\":{\"trash\":[{\"dow\":\"MONDAY\",\"tod\":[6,30],\"modelVersion\":\"1\"}]},\"modelVersion\":\"1\"}")
        	    ;
        return table.putItem(itemModelVersion1);
	}
	
	/**
	 * Create a table item using "item persistence" db access in current format.  Contains fields: CustomerId,
	 * TimeZone, Calendar, and IntentLog.
	 * 
	 * @param customerId String of this user's id
	 */
	public void itemCreateCurrentVersion(String customerId) {
        // Create a basic item with the current Model version
		Calendar calendar = newCalendarVeryBasic();
		TimeZone timeZone = TimeZone.getTimeZone("US/Eastern");
        DynamoItem itemWriteCurrentModel = new DynamoItem();
        itemWriteCurrentModel.setCustomerId(customerId);
        itemWriteCurrentModel.setCalendar(calendar);
        itemWriteCurrentModel.setTimeZone(timeZone);
        dynamoDbItemPersistence.saveItem(itemWriteCurrentModel);
        
        IntentLog intentLog = new IntentLog();
        intentLog.incrementIntent("2017-05", "respondTimeZoneUpdatedScheduleMissing", 1);
        intentLog.incrementIntent("2017-05", "respondPickupAddSingle", 1);
        intentLog.incrementIntent("2017-05", "open", 2);
        intentLog.incrementIntent("2017-05", "tellSchedule", 2);
        itemWriteCurrentModel.setIntentLog(intentLog);
        dynamoDbItemPersistence.saveOnlyIntentLog(itemWriteCurrentModel);
	}

	/**
	 * Table item helper function that counts how many items in
	 * the Dynamo DB table exist for the given customer id.
	 * 
	 * @param customerId String customer identifier
	 * @return Number of table items existing for this customer
	 */
	public static Integer itemCount(String customerId) {
        // Scan items with this Customer Id
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        Condition condition = new Condition()
            .withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue().withS(customerId));
        scanFilter.put("CustomerId", condition);
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult = dynamoDbLowLevel.scan(scanRequest);
        log.info("item scan: {}", scanResult.toString());
        
        Integer ret = scanResult.getCount();
        log.info("item count: {}", ret);
        return ret;
	}
	
	/**
	 * Table item helper function to remove item(s) that match the 
	 * given customer id.
	 * 
	 * @param customerId String customer identifier
	 */
	public static void itemDelete(String customerId) {
        DeleteItemSpec deleteItemSpec = new DeleteItemSpec()
        		.withPrimaryKey("CustomerId", customerId);
        
		try {
	        Table table = dynamoDbDocument.getTable(tableName);
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
	 * Schedule helper function that creates a new, basic {@link trashday.model.Schedule}
	 * 
	 * @return a {@link trashday.model.Schedule} with just one configured pickup
	 */
	@Deprecated
	public Schedule newScheduleVeryBasic() {
        Schedule schedule = new Schedule();
        LocalTime tod = LocalTime.of(06, 30);        
        schedule.addPickupSchedule("trash", DayOfWeek.MONDAY, tod);
        return schedule;
	}
	
	/**
	 * Schedule helper function that creates a new, complex {@link trashday.model.Schedule}
	 * 
	 * @return a {@link trashday.model.Schedule} with several configured pickups.
	 */
	@Deprecated
	public Schedule newComplexSchedule() {
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		return schedule;
	}
	
	public Calendar newCalendarVeryBasic() {
		Calendar calendar = new Calendar();
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 1, 30, 6, 30);
		calendar.pickupAddWeekly(ldtEvent, "trash", ldtEvent.getDayOfWeek(), ldtEvent.toLocalTime());
		return calendar;
	}
	
	public Calendar newComplexCalendar() {
		Calendar calendar = new Calendar();
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 1, 31, 7, 30);
		calendar.pickupAddWeekly(ldtEvent, "Trash", ldtEvent.getDayOfWeek(), ldtEvent.toLocalTime());
		ldtEvent = LocalDateTime.of(2017, 2, 3, 7, 30);
		calendar.pickupAddWeekly(ldtEvent, "Trash", ldtEvent.getDayOfWeek(), ldtEvent.toLocalTime());
		calendar.pickupAddBiWeekly(ldtEvent, "Recycling", false, ldtEvent.getDayOfWeek(), ldtEvent.toLocalTime());
		ldtEvent = LocalDateTime.of(2017, 2, 1, 12, 30);
		calendar.pickupAddDayOfMonth(ldtEvent, "Lawn Waste", 1, ldtEvent.toLocalTime());
		ldtEvent = LocalDateTime.of(2017, 2, 15, 12, 30);
		calendar.pickupAddDayOfMonth(ldtEvent, "Lawn Waste", 15, ldtEvent.toLocalTime());
		return calendar;
	}
	
	public SessionDao newSessionDao(String userId) {
		Map<String, Object> attributes = new HashMap<String, Object>();
		User user = User.builder().withUserId(userId).build();
        Session testSession = Session.builder()
				.withAttributes(attributes)
				.withSessionId("TEST-SESSION-ID")
				.withUser(user)
				.build();
		return new SessionDao(testSession);
	}
}
