package trashday.model;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.model.NextPickups;
import trashday.model.Schedule;

/**
 * JUnit tests for the {@link trashday.model.NextPickups} class.
 * 
 * @author J. Todd Baldwin
 */
@RunWith(JUnit4.class)
public class NextPickupsTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(NextPickupsTest.class);
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

	/**
	 * JUnit test to confirm that calculation of next pickup times
	 * for the example schedule are correct.
	 */
	@Test
	public void testNextPickups() {
		//  Test Now = Tuesday 2016-11-22T07:30
		//	Test Schedule:
		//	trash:
		//	  TUESDAY 06:30 AM
		//	  FRIDAY 06:30 AM
		//	recycling:
		//	  FRIDAY 06:30 AM
		//	lawn waste:
		//	  WEDNESDAY 12:00 PM
		
		// Expected Test Result:
		// Next lawn waste pickup is 2016-11-23T12:00 (Wed)
		// Next trash pickup is 2016-11-25T06:30 (Fri)
		// Next recycling pickup is 2016-11-25T06:30 (Fri)
		
		log.info("testNextPickups: Compute next pickup after 2016-11-22 07:30am.");
		LocalDateTime ldtTestTime = LocalDateTime.of(2016, 11, 22, 07, 30);  
		NextPickups pickupsActual = new NextPickups(ldtTestTime, sched, null);
		
		List<String> pickupNameListExpected = new ArrayList<String>();
		pickupNameListExpected.add("lawn waste");
		pickupNameListExpected.add("trash");
		pickupNameListExpected.add("recycling");
		List<LocalDateTime> pickupLdtListExpected = new ArrayList<LocalDateTime>();
		pickupLdtListExpected.add(LocalDateTime.of(2016, 11, 23, 12, 00));
		pickupLdtListExpected.add(LocalDateTime.of(2016, 11, 25, 06, 30));
		pickupLdtListExpected.add(LocalDateTime.of(2016, 11, 25, 06, 30));
		
		int i = -1;
		for(Map.Entry<String,LocalDateTime> entry : pickupsActual.getPickups().entrySet()) {
			i++;
			String pickupNameExpected = pickupNameListExpected.get(i);
			LocalDateTime ldtNextPickupExpected = pickupLdtListExpected.get(i);
			log.info("testNextPickups: Expected: "+pickupNameExpected+" pickup at " + ldtNextPickupExpected);
			String pickupNameActual = entry.getKey();
			LocalDateTime ldtNextPickupActual = entry.getValue();
			log.info("testNextPickups: Actual: "+pickupNameActual+" pickup at " + ldtNextPickupActual);
			
			assertEquals(pickupNameExpected,pickupNameActual);
			assertEquals(ldtNextPickupExpected.toString(), ldtNextPickupActual.toString());
		}
	}

	/**
	 * JUnit test to confirm that calculation of next pickup times
	 * for the example schedule are correct and that the verbal
	 * representation of those pickups is also correct.
	 */
	@Test
	public void testToStringVerbal() {
		String pickupsExpectedVerbalString = 
			"Next lawn waste pickup is tomorrow at noon. "+
			"Next trash pickup is Friday at 6 30 AM. "+
			"Next recycling pickup is Friday at 6 30 AM. ";
		
		log.info("testToStringVerbal: Compute next pickup after 2016-11-22 07:30am.");
		LocalDateTime ldtTestTime = LocalDateTime.of(2016, 11, 22, 07, 30);  
		NextPickups pickupsActual = new NextPickups(ldtTestTime, sched, null);
		String pickupsActualString = pickupsActual.toStringVerbal();
		
		log.info("testToStringVerbal: Expected: {}",pickupsExpectedVerbalString);
		log.info("testToStringVerbal: Actual:   {}",pickupsActualString);
		assertEquals(pickupsExpectedVerbalString, pickupsActualString);
	}

	/**
	 * JUnit test to confirm that calculation of next pickup times
	 * for the example schedule are correct and that the printed
	 * representation of those pickups is also correct.
	 */
	@Test
	public void testToStringPrintable() {
		String pickupsExpectedPrintableString = 
			"Next trash pickup is tomorrow at 6:30 AM.\n"+
			"Next recycling pickup is tomorrow at 6:30 AM.\n"+
			"Next lawn waste pickup is Wednesday at noon.\n";
			
		log.info("testToStringAbsolute: Compute next pickup after Thursday, 2016-11-24 3:27 pm.");
		LocalDateTime ldtTestTime = LocalDateTime.of(2016, 11, 24, 15, 27);
		NextPickups pickupsActual = new NextPickups(ldtTestTime, sched, null);
		String pickupsActualString = pickupsActual.toStringPrintable(ldtTestTime);
		
		log.info("testToStringAbsolute: Expected: {}",pickupsExpectedPrintableString);
		log.info("testToStringAbsolute: Actual:   {}",pickupsActualString);
		assertEquals(pickupsExpectedPrintableString, pickupsActualString);
	}

}
