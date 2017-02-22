package trashday.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.model.Schedule;

/**
 * JUnit tests for the {@link trashday.model.Schedule} class.
 * 
 * @author J. Todd Baldwin
 */
@RunWith(JUnit4.class)
@Deprecated
public class ScheduleTest {
    @Rule
    public ExpectedException thrown= ExpectedException.none();
    
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ScheduleTest.class);
    /** An example schedule for the tests to use */
	private static Schedule sched;
	
	/**
	 * Before starting tests in this class, create and initialize
	 * an example schedule.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		log.info("Load test schedule data.");
		sched = new Schedule();
	    sched.initExampleSchedule();
	}
	
	@Test
	public void testAddPickupSchedule() {
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		String firstSchedule = schedule.toStringPrintable();
		
		String expectedSchedule = firstSchedule+"Pickup mail on Monday at 4 PM.\n";
		String pickupName = "mail";
		TimeOfWeek tow = new TimeOfWeek(DayOfWeek.MONDAY, 16, 0);

		schedule.addPickupSchedule(pickupName, tow);
		String actualSchedule = schedule.toStringPrintable();
		assertEquals(expectedSchedule, actualSchedule);
		
		expectedSchedule = firstSchedule+"Pickup mail on Monday at 4 PM and Tuesday at 4 PM.\n";
		tow = new TimeOfWeek(DayOfWeek.TUESDAY, 16, 0);
		schedule.addPickupSchedule(pickupName, tow);
		actualSchedule = schedule.toStringPrintable();
		assertEquals(expectedSchedule, actualSchedule);
	}
	
	/**
	 * JUnit test to confirm {@link trashday.model.Schedule#deleteEntireSchedule()}
	 * works correctly.
	 */
	@Test
	public void testDeleteEntireSchedule() {
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		List<String> pickupNames = schedule.getPickupNames();
		assertNotNull("Problem creating example schedule: No pickups exist", pickupNames);
		assertTrue("Problem creating example schedule: No pickups listed", pickupNames.size()>0);
		assertFalse("Problem creating example schedule: Is Empty",schedule.isEmpty());
		
		schedule.deleteEntireSchedule();
		pickupNames = schedule.getPickupNames();
		assertNotNull("Problem deleting example schedule: No pickups exist", pickupNames);
		assertTrue("Problem deleting example schedule: Pickups still listed", pickupNames.size()<1);
		assertTrue("Problem deleting example schedule: Is Not Empty",schedule.isEmpty());
		
		schedule.deleteEntireSchedule();
		pickupNames = schedule.getPickupNames();
		assertNotNull("Problem deleting example schedule again: No pickups exist", pickupNames);
		assertTrue("Problem deleting example schedule again: Pickups still listed", pickupNames.size()<1);
		assertTrue("Problem deleting example schedule again: Is Not Empty",schedule.isEmpty());
	}
	
	@Test
	public void testDeleteEntirePickup() {
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		
		// Check delete existing pickup works
		String expectedPickups = "[trash, lawn waste]";
		schedule.deleteEntirePickup("recycling");
		List<String> pickupNames = schedule.getPickupNames();
		String actualPickups = pickupNames.toString();
		assertEquals(expectedPickups, actualPickups);
		
		// Check delete missing pickup works
		expectedPickups = "[trash, lawn waste]";
		schedule.deleteEntirePickup("garbage");
		pickupNames = schedule.getPickupNames();
		actualPickups = pickupNames.toString();
		assertEquals(expectedPickups, actualPickups);		
	}
	
	@Test
	public void testDeleteEntirePickupTimesMissing() {
		Schedule schedule = new Schedule();
		
		schedule.initExampleSchedule();
		schedule.getPickupSchedule().put("recycling", null);
		String expectedBrokenVerbal = "Pickup trash on Tuesday 6 30 AM and Friday 6 30 AM. Pickup lawn waste on Wednesday noon. ";
		String actualBrokenVerbal = schedule.toStringVerbal();
		assertEquals(expectedBrokenVerbal, actualBrokenVerbal);
		
		schedule.initExampleSchedule();
		schedule.getPickupSchedule().put("recycling", null);
		String expectedBrokenPrintable = "Pickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.\nPickup lawn waste on Wednesday at noon.\n";
		String actualBrokenPrintable = schedule.toStringPrintable();
		assertEquals(expectedBrokenPrintable, actualBrokenPrintable);
		
		// Check delete existing pickup with missing pickup times works
		schedule.initExampleSchedule();
		schedule.getPickupSchedule().put("recycling", null);
		String expectedPickups = "[trash, lawn waste]";
		schedule.deleteEntirePickup("recycling");
		List<String> pickupNames = schedule.getPickupNames();
		String actualPickups = pickupNames.toString();
		assertEquals(expectedPickups, actualPickups);
	}
	
	@Test
	public void testDeletePickupTime() {
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		
		// Delete an existing pickup
		String expectedSchedule = schedule.toStringPrintable().replaceFirst("Tuesday at 6:30 AM and ", "");
		DayOfWeek dow = DayOfWeek.TUESDAY;
		LocalTime tod = LocalTime.of(6, 30);
		schedule.deletePickupTime("trash", dow, tod);
		String actualSchedule = schedule.toStringPrintable();
		assertEquals(expectedSchedule, actualSchedule);
		
		// Delete a non-existant pickup
		expectedSchedule = schedule.toStringPrintable();
		schedule.deletePickupTime("garbage", dow, tod);
		actualSchedule = schedule.toStringPrintable();
		assertEquals(expectedSchedule, actualSchedule);
	}
	
	/**
	 * JUnit test to check that an example schedule gets spoken as expected.
	 */
	@Test
	public void testToStringVerbal() {
		String expectedScheduleString =
			"Pickup trash on Tuesday 6 30 AM and Friday 6 30 AM. "+
			"Pickup recycling on Friday 6 30 AM. "+
			"Pickup lawn waste on Wednesday noon. ";
		log.info("testToString Expected Test Schedule:\n"+expectedScheduleString);
		
		String actualScheduleString = sched.toStringVerbal();
		log.info("testToString Actual Test Schedule:\n"+actualScheduleString);
		
		assertEquals(expectedScheduleString, actualScheduleString);
	}
	
	/**
	 * JUnit test to check that an example schedule prints as expected.
	 */
	@Test
	public void testToStringPrintable() {
		String expectedScheduleString =
			"Pickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.\n"+
			"Pickup recycling on Friday at 6:30 AM.\n"+
			"Pickup lawn waste on Wednesday at noon.\n";				
		log.info("testToString Expected Test Schedule:\n"+expectedScheduleString);
		
		String actualScheduleString = sched.toStringPrintable();
		log.info("testToString Actual Test Schedule:\n"+actualScheduleString);
		
		assertEquals(expectedScheduleString, actualScheduleString);
	}
	
	@Test
	public void testScheduleJson() {
		String actualJson = sched.toJson();
		log.info("JSON for example Schedule: {}", actualJson);
		String expectedJson = "{\"pickupNames\":[\"trash\",\"recycling\",\"lawn waste\"],\"pickupSchedule\":{\"recycling\":[{\"dow\":\"FRIDAY\",\"tod\":[6,30],\"modelVersion\":\"1\"}],\"lawn waste\":[{\"dow\":\"WEDNESDAY\",\"tod\":[12,0],\"modelVersion\":\"1\"}],\"trash\":[{\"dow\":\"TUESDAY\",\"tod\":[6,30],\"modelVersion\":\"1\"},{\"dow\":\"FRIDAY\",\"tod\":[6,30],\"modelVersion\":\"1\"}]},\"modelVersion\":\"1\"}";
		assertEquals(expectedJson, actualJson);
		
		Schedule newSched = new Schedule(actualJson);
		String actualSchedule = newSched.toStringPrintable();
		log.info("Schedule loaded from JSON: {}", actualSchedule);
		String expectedSchedule = sched.toStringPrintable();
		assertEquals(expectedSchedule, actualSchedule);
	}
	
	@Test
	public void testScheduleBadJson() {
		thrown.expect(IllegalStateException.class);
		
		String actualJson = sched.toJson().replaceFirst("\\{", "{\"newattrib\":\"newValue\", ");
		log.info("Bad JSON for example Schedule: {}", actualJson);
		
		new Schedule(actualJson);
		fail("Schedule was supposed to throw exception on parsing bad JSON string.");
	}
	
	@Test
	public void testValidate() {
		Schedule brokenSchedule = new Schedule();
		brokenSchedule.initExampleSchedule();
		int actualErrorCount = brokenSchedule.validate();
		assertEquals("Example schedule should have no errors", 0, actualErrorCount);
		
		brokenSchedule.initExampleSchedule();
		brokenSchedule.getPickupNames().add("Extra Pickup Name");
		actualErrorCount = brokenSchedule.validate();
		assertEquals("Schedule with an extra pickup name but no schedule should have one error.", 1, actualErrorCount);
		
		brokenSchedule.initExampleSchedule();
		brokenSchedule.getPickupNames().remove("recycling");
		actualErrorCount = brokenSchedule.validate();
		assertEquals("Schedule missing one pickup name that is still in schedule should have one error.", 1, actualErrorCount);
		
		brokenSchedule.initExampleSchedule();
		brokenSchedule.getPickupNames().add("Extra Pickup Name");
		brokenSchedule.getPickupSchedule().put("Extra Pickup Name", null);
		actualErrorCount = brokenSchedule.validate();
		assertEquals("Schedule with an extra pickup schedule but missing pickup times should have one error.", 1, actualErrorCount);
		
		brokenSchedule.initExampleSchedule();
		brokenSchedule.getPickupSchedule().put("Extra Pickup Name", null);
		actualErrorCount = brokenSchedule.validate();
		assertEquals("Schedule with an extra pickup schedule but missing pickup times and name list entry should have one error.", 1, actualErrorCount);
		
		brokenSchedule.initExampleSchedule();
		brokenSchedule.getPickupNames().add("Extra Pickup Name");
		SortedSet<TimeOfWeek> emptySet = new TreeSet<TimeOfWeek>();
		brokenSchedule.getPickupSchedule().put("Extra Pickup Name", emptySet);
		actualErrorCount = brokenSchedule.validate();
		assertEquals("Schedule with an extra pickup schedule but empty pickup times should have one error.", 1, actualErrorCount);
		
	}
	
}
