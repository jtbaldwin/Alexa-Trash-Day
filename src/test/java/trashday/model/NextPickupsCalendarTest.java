package trashday.model;

import static org.junit.Assert.assertEquals;

import java.time.LocalDateTime;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.model.NextPickups;
import trashday.ui.FormatUtils;

/**
 * JUnit tests for the {@link trashday.model.NextPickups} class when using the 
 * {@link trashday.model.Calendar}-based user pickup schedules..
 * 
 * @author J. Todd Baldwin
 */
@RunWith(JUnit4.class)
public class NextPickupsCalendarTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(NextPickupsCalendarTest.class);
    /** An example calendar for the tests to use */	
	private static Calendar calendar;

	/**
	 * Before starting tests in this class, create and initialize
	 * an example schedule.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
	    calendar = new Calendar();
	    calendar.initComplexExampleCalendar();
	    
	    LocalDateTime ldtRequest = LocalDateTime.now();
		log.info("test calendar={}", FormatUtils.printableCalendar(calendar, ldtRequest));
	}

	@Test
	public void testPrintableNextPickupsForCalendar() {
		/*
		 * Test Date: Compute next pickup after 2017-02-08 10:40am. (Wednesday)
		 * Test Calendar:
		 * Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.
		 * Pickup recycling every other Friday at 7:30 AM.
		 * Pickup lawn waste monthly on the first at 7:30 AM and monthly on the fifteenth at 7:30 AM.
		 * 
		 * Expected Result:
		 * Next trash pickup is Friday at 7:30 AM.
		 * Next lawn waste pickup is Wednesday, February 15 at 7:30 AM.
		 * Next recycling pickup is Friday, February 17 at 7:30 AM.
		 */
		String expected = "Next trash pickup is Friday at 7:30 AM.\n" + 
				"Next lawn waste pickup is Wednesday, February 15 at noon.\n" + 
				"Next recycling pickup is Friday, February 17 at 7:30 AM.\n" +
				"Next hockey team pickup is Saturday, February 18 at 9 AM.\n" +
				"Next mortgage pickup is Friday, February 24 at noon.\n" + 
				"Next scrap metal pickup is Tuesday, February 28 at noon.\n" +
				"Next dry cleaning pickup is Saturday, March 11 at noon.\n";
		
		log.info("testNextPickups: Compute next pickup after 2017-02-08 10:40am.");
		LocalDateTime ldtTestTime = LocalDateTime.of(2017, 2, 8, 10, 40);  
		NextPickups pickupsActual = new NextPickups(ldtTestTime, calendar, null);
		String actual = FormatUtils.printableNextPickups(pickupsActual);
		
		log.info("testNextPickups: expected={}",expected);
		log.info("testNextPickups: actual={}",actual);
		assertEquals(expected, actual);
	}
	
	@Test
	public void testVerbalNextPickupsForCalendar() {
		/*
		 * Test Date: Compute next pickup after 2017-02-08 10:40am. (Wednesday)
		 * Test Calendar:
		 * Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.
		 * Pickup recycling every other Friday at 7:30 AM.
		 * Pickup lawn waste monthly on the first at 7:30 AM and monthly on the fifteenth at 7:30 AM.
		 * 
		 * Expected Result:
		 * Next trash pickup is Friday at 7:30 AM.
		 * Next lawn waste pickup is Wednesday, February 15 at 7:30 AM.
		 * Next recycling pickup is Friday, February 17 at 7:30 AM.
		 */
		String expected = "Next trash pickup is Friday at 7 30 AM. Next lawn waste pickup is Wednesday, February 15 at noon. Next recycling pickup is Friday, February 17 at 7 30 AM. Next hockey team pickup is Saturday, February 18 at 9 AM. Next mortgage pickup is Friday, February 24 at noon. Next scrap metal pickup is Tuesday, February 28 at noon. Next dry cleaning pickup is Saturday, March 11 at noon. ";
		
		log.info("testNextPickups: Compute next pickup after 2017-02-08 10:40am.");
		LocalDateTime ldtTestTime = LocalDateTime.of(2017, 2, 8, 10, 40);  
		NextPickups pickupsActual = new NextPickups(ldtTestTime, calendar, null);
		String actual = FormatUtils.verbalNextPickups(pickupsActual);
		
		log.info("testNextPickups: expected={}",expected);
		log.info("testNextPickups: actual={}",actual);
		assertEquals(expected, actual);
	}
	
}
