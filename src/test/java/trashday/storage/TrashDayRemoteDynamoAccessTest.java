package trashday.storage;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
import dynamotesting.RemoteDynamoDBCreationRule;
import trashday.model.Schedule;

/**
 * JUnit tests for reading and writing to the Amazon Dynamo DB cloud.
 * 
 * @author J. Todd Baldwin
 */
@RunWith(JUnit4.class)
public class TrashDayRemoteDynamoAccessTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDayRemoteDynamoAccessTest.class);
    /** Table to store all user schedules */
	private static final String tableName = "TrashDayScheduleData";
	/** Customer Id to be used for these tests */
	private static final String customerId = "testing-only";

	/**
	 * Use {@link dynamotesting.RemoteDynamoDBCreationRule} to create
	 * a connection to the Amazon Dynamo DB cloud for this test class.
	 */
    @ClassRule
    public static final RemoteDynamoDBCreationRule dynamoDB = new RemoteDynamoDBCreationRule();

    /**
     * Before running tests in this class, log some information about
     * the Amazon Dynamo DB server.
     */
	@BeforeClass
	public static void setUpBeforeClass() {
		String serviceName = dynamoDB.getTrashDayDbClient().getAmazonDynamoDBClient().getServiceName();
		log.info("setUpBeforeClass: serviceName={}", serviceName);
		boolean isTestDynamoDB = dynamoDB.isTestDB();
		log.info("setUpBeforeClass: isTestDB={}", isTestDynamoDB);
	}

	/**
	 * Table management helper function that creates a Dynamo DB table
	 * suitable for storing user Schedules.
	 */
    public static void tableCreate() {
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
		} catch (AmazonDynamoDBException ex) {
			log.info("table create exception: {}", ex.getErrorMessage());			
		}
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
			dtr = dynamoDB.getTrashDayDbClient().getAmazonDynamoDBClient().deleteTable(tableName);
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
		
        assertEquals((Integer) 0, itemCount(customerId));		
	}

	/**
	 * Table item helper function that counts how many items in
	 * the Dynamo DB table exist for the given customer id.
	 * 
	 * @param customerId String customer identifier
	 * @return Number of table items existing for this customer
	 */
	public Integer itemCount(String customerId) {
        // Scan items with this Customer Id
        HashMap<String, Condition> scanFilter = new HashMap<String, Condition>();
        Condition condition = new Condition()
            .withComparisonOperator(ComparisonOperator.EQ.toString())
            .withAttributeValueList(new AttributeValue().withS(customerId));
        scanFilter.put("CustomerId", condition);
        ScanRequest scanRequest = new ScanRequest(tableName).withScanFilter(scanFilter);
        ScanResult scanResult;
        try {
        	scanResult = dynamoDB.getTrashDayDbClient().getAmazonDynamoDBClient().scan(scanRequest);
		} catch (AmazonDynamoDBException ex) {
			log.info("table scan exception: {}", ex.getErrorMessage());			
			return -1;
		}
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
	public void itemDelete(String customerId) {
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
	 * Schedule helper function that creates a new, basic {@link trashday.model.Schedule}
	 * 
	 * @return a {@link trashday.model.Schedule} with just one configured pickup
	 */
	public Schedule newBasicSchedule() {
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
	public Schedule newComplexSchedule() {
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		return schedule;
	}
    
	/**
	 * JUnit test that confirms we can Create, Read, Update, and
	 * Delete (CRUD) user {@link trashday.model.Schedule} items from the
	 * Dynamo DB table.
	 */
	@Test
	public void testCRUD() {
		String expectedBasicSchedule = "Pickup trash on Monday at 6:30 AM.\n";
		String expectedComplexSchedule = "Pickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.\nPickup recycling on Friday at 6:30 AM.\nPickup lawn waste on Wednesday at noon.\n";
		
		// Setup a clear table for testing.
		boolean isTestDynamoDB = dynamoDB.isTestDB();
		if (isTestDynamoDB) {
			// Table deleted and re-created for local Dynamo testing.
			log.info("(Re-)creating table from test DB.");
			tableDelete();
			tableCreate();			
	        assertEquals((Integer) 0, itemCount(customerId));		
		} else {
			// Table should already exist for remote Dynamo testing.
			log.info("Deleting item for {}", customerId);
			itemDelete(customerId);
		}
        
        // Create a basic item
		Schedule schedule = newBasicSchedule();		
        DynamoItem item = new DynamoItem();
        item.setCustomerId(customerId);
        item.setSchedule(schedule);
        dynamoDB.getTrashDayDbClient().saveItem(item);
        log.info("Saved Schedule: {}", item.getSchedule().toStringPrintable());
        
        assertEquals(expectedBasicSchedule, item.getSchedule().toStringPrintable());
		if (isTestDynamoDB) {
			// Remote Dynamo does not allow item count permission.
			assertEquals((Integer) 1, itemCount(customerId));
		}
        
        // Read the item back
        DynamoItem searchItem = new DynamoItem();
        searchItem.setCustomerId(customerId);
        item = dynamoDB.getTrashDayDbClient().loadItem(searchItem);
        log.info("Loaded Schedule: {}", item.getSchedule().toStringPrintable());
        
        assertEquals(expectedBasicSchedule, item.getSchedule().toStringPrintable());
        
        // Update the item
        schedule = newComplexSchedule();
        item.setSchedule(schedule);
        dynamoDB.getTrashDayDbClient().saveItem(item);
        log.info("Updated Schedule: {}", item.getSchedule().toStringPrintable());
        
        assertEquals(expectedComplexSchedule, item.getSchedule().toStringPrintable());
		if (isTestDynamoDB) {
			// Remote Dynamo does not allow item count permission.
			assertEquals((Integer) 1, itemCount(customerId));
		}
        
        // Read the item back
        item = dynamoDB.getTrashDayDbClient().loadItem(searchItem);
        log.info("Loaded Schedule: {}", item.getSchedule().toStringPrintable());
        
        assertEquals(expectedComplexSchedule, item.getSchedule().toStringPrintable());
		if (isTestDynamoDB) {
			// Remote Dynamo does not allow item count permission.
			assertEquals((Integer) 1, itemCount(customerId));
		}
        
        // Delete the item
		log.info("Deleting item for {}", customerId);
		itemDelete(customerId); 
		if (isTestDynamoDB) {
			// Remote Dynamo does not allow item count permission.
			assertEquals((Integer) 0, itemCount(customerId));
		}
	}

}
