package trashday.ui.responses;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.ui.DateTimeOutputUtils;

/**
 * JUnit tests for the {@link trashday.ui.DateTimeOutputUtils} class.
 * 
 * @author J.Todd Baldwin
 *
 */
@RunWith(JUnit4.class)
public class VerbalUtilsTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(VerbalUtilsTest.class);
    
	/**
	 * Test method for {@link trashday.ui.DateTimeOutputUtils#verbalTime(java.time.LocalTime)}.
	 */
	@Test
	public void testVerbalTimeLocalTime() {
		log.info("testVerbalTimeLocalTime()");
		
		LocalTime ltTestTime = LocalTime.of(7, 00);
		String expectedResult = "7 AM";
		String actualResult = DateTimeOutputUtils.verbalTime(ltTestTime);
		assertEquals(expectedResult, actualResult);
		
		ltTestTime = LocalTime.of(12, 00);
		expectedResult = "noon";
		actualResult = DateTimeOutputUtils.verbalTime(ltTestTime);
		assertEquals(expectedResult, actualResult);
		
		ltTestTime = LocalTime.of(0, 00);
		expectedResult = "midnight";
		actualResult = DateTimeOutputUtils.verbalTime(ltTestTime);
		assertEquals(expectedResult, actualResult);
		
		ltTestTime = LocalTime.of(7, 30);
		expectedResult = "7 30 AM";
		actualResult = DateTimeOutputUtils.verbalTime(ltTestTime);
		assertEquals(expectedResult, actualResult);
		
		ltTestTime = LocalTime.of(15, 30);
		expectedResult = "3 30 PM";
		actualResult = DateTimeOutputUtils.verbalTime(ltTestTime);
		assertEquals(expectedResult, actualResult);
	}

	/**
	 * Test method for {@link trashday.ui.DateTimeOutputUtils#verbalTime(java.time.LocalDateTime)}.
	 */
	@Test
	public void testVerbalTimeLocalDateTime() {
		log.info("testVerbalTimeLocalDateTime()");
		
		LocalDateTime ldtTestTime = LocalDateTime.of(2016, 11, 24, 7, 0);
		String expectedResult = "7 AM";
		String actualResult = DateTimeOutputUtils.verbalTime(ldtTestTime);
		assertEquals(expectedResult, actualResult);
		
		ldtTestTime = LocalDateTime.of(2016, 11, 24, 12, 0);
		expectedResult = "noon";
		actualResult = DateTimeOutputUtils.verbalTime(ldtTestTime);
		assertEquals(expectedResult, actualResult);
		
		ldtTestTime = LocalDateTime.of(2016, 11, 24, 0, 0);
		expectedResult = "midnight";
		actualResult = DateTimeOutputUtils.verbalTime(ldtTestTime);
		assertEquals(expectedResult, actualResult);
		
		ldtTestTime = LocalDateTime.of(2016, 11, 24, 7, 30);
		expectedResult = "7 30 AM";
		actualResult = DateTimeOutputUtils.verbalTime(ldtTestTime);
		assertEquals(expectedResult, actualResult);
		
		ldtTestTime = LocalDateTime.of(2016, 11, 24, 15, 30);
		expectedResult = "3 30 PM";
		actualResult = DateTimeOutputUtils.verbalTime(ldtTestTime);
		assertEquals(expectedResult, actualResult);
	}

	/**
	 * Test method for {@link trashday.ui.DateTimeOutputUtils#verbalDateAndTimeRelative(java.time.LocalDateTime, java.time.LocalDateTime)}.
	 */
	@Test
	public void testVerbalDateAndTime() {
		// Single pickup time: Friday 7am
		LocalDateTime ldtTestPickupTime = LocalDateTime.of(2016, 11, 25, 7, 0); // Friday
		
		// Range of request times: Thursday 1am to Saturday 8am
		LocalDateTime ldtTestRequestStart = LocalDateTime.of(2016, 11, 17, 1, 0); // Thursday
		LocalDateTime ldtTestRequestEnd = LocalDateTime.of(2016, 12, 03, 8, 0); // Saturday
		LocalDateTime ldtTestRequestTime = LocalDateTime.from(ldtTestRequestStart);
		
		Map<LocalDate, String> expectedResultsByDay = new TreeMap<LocalDate, String>();
		expectedResultsByDay.put( LocalDate.of(2016, 11, 17), "Friday, November 25 at 7 AM");		// Thursday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 18), "Friday, November 25 at 7 AM");		// Friday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 19), "Friday at 7 AM");		// Saturday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 20), "Friday at 7 AM");		// Sunday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 21), "Friday at 7 AM");		// Monday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 22), "Friday at 7 AM");		// Tuesday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 23), "Friday at 7 AM");		// Wednesday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 24), "tomorrow at 7 AM");	// Thursday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 25), "today at 7 AM");		// Friday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 26), "yesterday at 7 AM");	// Saturday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 27), "last Friday at 7 AM");	// Sunday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 28), "last Friday at 7 AM");	// Monday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 29), "last Friday at 7 AM");	// Tuesday
		expectedResultsByDay.put( LocalDate.of(2016, 11, 30), "last Friday at 7 AM");	// Wednesday
		expectedResultsByDay.put( LocalDate.of(2016, 12, 01), "last Friday at 7 AM");	// Thursday
		expectedResultsByDay.put( LocalDate.of(2016, 12, 02), "Friday, November 25 at 7 AM");	// Friday
		expectedResultsByDay.put( LocalDate.of(2016, 12, 03), "Friday, November 25 at 7 AM");	// Saturday

		String priorExpectedResult = "Friday at 7 AM";
		
		for (;ldtTestRequestTime.isBefore(ldtTestRequestEnd);ldtTestRequestTime=ldtTestRequestTime.plusMinutes(60)) {
			// Range of expectations for: Thursday 1am to Saturday 8am
			String expectedResult = expectedResultsByDay.get(LocalDate.from(ldtTestRequestTime));
			if (! expectedResult.equals(priorExpectedResult)) {
				log.info("NEW Expected Results: {}", expectedResult);
				priorExpectedResult = expectedResult;
			}
			
			String actualResult = DateTimeOutputUtils.verbalDateAndTimeRelative(ldtTestPickupTime, ldtTestRequestTime);
			log.info("Base: \"{}\",  Occurrence: \"{}\", Actual \"{}\"",ldtTestRequestTime,ldtTestPickupTime,actualResult);
			assertEquals(expectedResult, actualResult);
		}
		
		
	}

}
