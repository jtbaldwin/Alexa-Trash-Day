package trashday.storage;

import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.util.TimeZone;

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

import dynamotesting.LocalDynamoDBCreationRule;
import trashday.model.Calendar;
import trashday.model.Schedule;
import trashday.ui.FormatUtils;

/**
 * JUnit tests for reading and writing to a local Dynamo DB instance.
 * 
 * @author J. Todd Baldwin
 */
@SuppressWarnings("deprecation")
@RunWith(JUnit4.class)
public class LocalDynamoAccessTest extends DynamoAccessTestHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(LocalDynamoAccessTest.class);
	
	/**
	 * Use {@link dynamotesting.LocalDynamoDBCreationRule} to create
	 * a local Dynamo DB instance for this test class.
	 */
    @ClassRule
    public static final LocalDynamoDBCreationRule localDynamoDB = new LocalDynamoDBCreationRule(tableName);

    /**
     * Before running tests in this class, log some information about
     * the running local Dynamo DB server.
     */
	@BeforeClass
	public static void setUpBeforeClass() {
		dynamoDbLowLevel = localDynamoDB.getAmazonDynamoDBClient();
		dynamoDbDocument = new DynamoDB(dynamoDbLowLevel);
		dynamoDbItemPersistence = localDynamoDB.getTrashDayDbClient();
		
		String serviceName = dynamoDbLowLevel.getServiceName();
		log.info("setUpBeforeClass: serviceName={}", serviceName);
		boolean isTestDynamoDB = localDynamoDB.isTestDB();
		log.info("setUpBeforeClass: isTestDB={}", isTestDynamoDB);
		
		// Setup a clear table for testing.
		CreateTableResult ctr = null;
		if (isTestDynamoDB) {
			log.info("(Re-)creating table in test DB.");
			tableDelete();
			ctr = tableCreate();
		} else {
			ctr = tableCreate();
			resetCustomerId();
			for (int i=1; i<=10; i++) {
				String customerId = getNextCustomerId();
				log.info("Deleting item for {}", customerId);
				itemDelete(customerId);
			}
			resetCustomerId();
		}
        TableDescription tableDescription = ctr.getTableDescription();
        log.info("Created Table Description: {}", tableDescription.toString());
	}
	
	@Test
	public void checkReadV1ItemViaDAO() {
		LocalDateTime ldtRequest = LocalDateTime.now();
		
        // Create a manual v1 item
		String customerVersion1 = getNextCustomerId();
        itemCreateVersion1(customerVersion1);
        assertEquals((Integer) 1, itemCount(customerVersion1));		

        // Create a DynamoDAO & SessionDAO for testing.
        DynamoDao dynamoDao = new DynamoDao(dynamoDbItemPersistence);
        SessionDao sessionDao = newSessionDao(customerVersion1);

        // Read back the v1 item via DynamoDAO.
        boolean itemRead = dynamoDao.readUserData(sessionDao);
        assertTrue(itemRead);
        
        // TimeZone should read normally.
        assertNotNull(sessionDao.getTimeZone());
        assertEquals("US/Eastern", sessionDao.getTimeZone().getID());
        
        // Normal readUserData() must always skip IntentLog data.
        assertNull(sessionDao.getIntentLog());  
        
        // Should have a (new from Schedule) Calendar.
        assertNotNull(sessionDao.getCalendar());
        assertEquals("Pickup trash every Monday at 6:30 AM.\n", FormatUtils.printableCalendar(sessionDao.getCalendar(), ldtRequest));
	}

	/**
	 * Confirm we can handle reading a "version 1" of a user's database table
	 * item and that it matches (as closely as possible) what a current item
	 * looks like.  Uses Dynamo "Item Persistence" DB access to get access
	 * to the exact {@link DynamoItem} object read/written.
	 */
	@Test
	public void checkReadV1ItemMatchesCurrentItemViaDBItemPersistence() {
		LocalDateTime ldtRequest = LocalDateTime.now();
        
        // Create a basic item with the current Model version
		String customerCurrent = getNextCustomerId();
        itemCreateCurrentVersion(customerCurrent);
		
        // Create a manual v1 item
		String customerVersion1 = getNextCustomerId();
        itemCreateVersion1(customerVersion1);
        
        assertEquals((Integer) 1, itemCount(customerCurrent));		
        assertEquals((Integer) 1, itemCount(customerVersion1));		
        
        // Read the item(s) back
        DynamoItem searchItem = new DynamoItem();
        searchItem.setCustomerId(customerCurrent);
        DynamoItem itemReadCurrentModel = dynamoDbItemPersistence.loadCompleteItem(searchItem);
        assertNotNull(itemReadCurrentModel);
        assertNull(itemReadCurrentModel.getSchedule());
        String currentPickupSchedule = FormatUtils.printableCalendar(itemReadCurrentModel.getCalendar(), ldtRequest);
        log.info("Loaded Current Item TimeZone: {}", itemReadCurrentModel.getTimeZone());
        log.info("Loaded Current Item Calendar: {}", currentPickupSchedule);

        // Read the item(s) back
        searchItem.setCustomerId(customerVersion1);
        DynamoItem itemReadModelVersion1 = dynamoDbItemPersistence.loadCompleteItem(searchItem);
        assertNotNull(itemReadModelVersion1);
        log.info("Loaded Version 1 Item TimeZone: {}", itemReadModelVersion1.getTimeZone());
        log.info("Loaded Version 1 Item Schedule: {}", itemReadModelVersion1.getSchedule().toStringPrintable());

        assertEquals(itemReadCurrentModel.getTimeZone(), itemReadModelVersion1.getTimeZone());
        assertEquals(itemReadCurrentModel.getIntentLog().toStringPrintable(), itemReadModelVersion1.getIntentLog().toStringPrintable());
        
        String version1PickupSchedule = itemReadModelVersion1.getSchedule().toStringPrintable()
        		.replaceAll(" on ", " every ");
        assertEquals(currentPickupSchedule, version1PickupSchedule);
	}
    
	/**
	 * JUnit test that confirms we can Create, Read, Update, and
	 * Delete (CRUD) user {@link trashday.model.Schedule} items from the
	 * Dynamo DB table.
	 */
	@Test
	public void testCRUDCalendar() {
		LocalDateTime ldtRequest = LocalDateTime.of(2017, 2, 12, 9, 10);
		
		String expectedBasicCalendar = "Pickup trash every Monday at 6:30 AM.\n";
		String expectedComplexCalendar = "Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.\n" + 
				"Pickup recycling every other Friday at 7:30 AM (on this Friday).\n" + 
				"Pickup lawn waste on the first at 12:30 PM and on the fifteenth at 12:30 PM.\n";
		
        // Create a basic item
		String customerId1 = getNextCustomerId();
		Calendar calendar = newCalendarVeryBasic();		
		TimeZone timeZone = TimeZone.getTimeZone("US/Eastern");
        DynamoItem item = new DynamoItem();
        item.setCustomerId(customerId1);
        item.setCalendar(calendar);
        item.setTimeZone(timeZone);
        dynamoDbItemPersistence.saveItem(item);
        log.info("Saved Calendar: {}", FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        log.info("Saved Calendar (Storable): {}", item.getCalendar().toStringRFC5545());
        log.info("Saved TimeZone: {}", item.getTimeZone());
        
        assertEquals(expectedBasicCalendar, FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        assertEquals((Integer) 1, itemCount(customerId1));
        
        // Read the item back
        DynamoItem searchItem = new DynamoItem();
        searchItem.setCustomerId(customerId1);
        item = dynamoDbItemPersistence.loadCompleteItem(searchItem);
        log.info("Loaded Calendar: {}", FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        log.info("Loaded TimeZone: {}", item.getTimeZone());
        
        assertEquals(expectedBasicCalendar, FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        
        // Update the item
        calendar = newComplexCalendar();
        item.setCalendar(calendar);
        timeZone = TimeZone.getTimeZone("US/Pacific");
        item.setTimeZone(timeZone);
        dynamoDbItemPersistence.saveItem(item);
        log.info("Updated Calendar: {}", FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        log.info("Updated TimeZone: {}", item.getTimeZone());

        assertEquals(expectedComplexCalendar, FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        assertEquals((Integer) 1, itemCount(customerId1));
        
        // Read the item back
        item = dynamoDbItemPersistence.loadCompleteItem(searchItem);
        log.info("Loaded Calendar: {}", FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        log.info("Loaded TimeZone: {}", item.getTimeZone());
        
        assertEquals(expectedComplexCalendar, FormatUtils.printableCalendar(item.getCalendar(), ldtRequest));
        assertEquals((Integer) 1, itemCount(customerId1));
        
        // Delete the item
		log.info("Deleting item for {}", customerId1);
		itemDelete(customerId1); 
        assertEquals((Integer) 0, itemCount(customerId1));
	}

	/**
	 * JUnit test that confirms we can Create, Read, Update, and
	 * Delete (CRUD) user {@link trashday.model.Schedule} items from the
	 * Dynamo DB table.
	 */
	@Test
	public void testCRUDSchedule() {
		String expectedBasicSchedule = "Pickup trash on Monday at 6:30 AM.\n";
		String expectedComplexSchedule = "Pickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.\nPickup recycling on Friday at 6:30 AM.\nPickup lawn waste on Wednesday at noon.\n";

        // Create a basic item
		String customerId1 = getNextCustomerId();
		Schedule schedule = newScheduleVeryBasic();		
		TimeZone timeZone = TimeZone.getTimeZone("US/Eastern");
        DynamoItem item = new DynamoItem();
        item.setCustomerId(customerId1);
        item.setSchedule(schedule);
        item.setTimeZone(timeZone);
        dynamoDbItemPersistence.saveCompleteItem(item);
        log.info("Saved Schedule: {}", item.getSchedule().toStringPrintable());
        log.info("Saved Schedule (JSON): {}", item.getSchedule().toJson());
        log.info("Saved TimeZone: {}", item.getTimeZone());        
        assertEquals(expectedBasicSchedule, item.getSchedule().toStringPrintable());
        assertEquals((Integer) 1, itemCount(customerId1));
        
        // Read the item back.
        DynamoItem searchItem = new DynamoItem();
        searchItem.setCustomerId(customerId1);
        item = dynamoDbItemPersistence.loadCompleteItem(searchItem);
        
        assertNotNull(item.getTimeZone());
        assertNotNull(item.getSchedule());
        log.info("Loaded TimeZone: {}", item.getTimeZone());
        log.info("Loaded Schedule: {}", item.getSchedule().toStringPrintable());
        
        assertEquals(expectedBasicSchedule, item.getSchedule().toStringPrintable());
        
        // Update the item
        schedule = newComplexSchedule();
        item.setSchedule(schedule);
        timeZone = TimeZone.getTimeZone("US/Pacific");
        item.setTimeZone(timeZone);
        dynamoDbItemPersistence.saveCompleteItem(item);
        log.info("Updated Schedule: {}", item.getSchedule().toStringPrintable());
        log.info("Updated TimeZone: {}", item.getTimeZone());

        assertEquals(expectedComplexSchedule, item.getSchedule().toStringPrintable());
        assertEquals((Integer) 1, itemCount(customerId1));
        
        // Read the item back
        item = dynamoDbItemPersistence.loadCompleteItem(searchItem);
        assertNotNull(item.getTimeZone());
        assertNotNull(item.getSchedule());
        log.info("Loaded Schedule: {}", item.getSchedule().toStringPrintable());
        log.info("Loaded TimeZone: {}", item.getTimeZone());
        
        assertEquals(expectedComplexSchedule, item.getSchedule().toStringPrintable());
        assertEquals((Integer) 1, itemCount(customerId1));
        
        // Delete the item
		log.info("Deleting item for {}", customerId1);
		itemDelete(customerId1); 
        assertEquals((Integer) 0, itemCount(customerId1));
	}
}
