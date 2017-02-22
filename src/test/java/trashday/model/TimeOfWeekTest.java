package trashday.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.model.TimeOfWeek;

/**
 * JUnit tests for the {@link trashday.model.TimeOfWeek} class.
 * 
 * @author J. Todd Baldwin
 */
@SuppressWarnings("deprecation")
@RunWith(JUnit4.class)
public class TimeOfWeekTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TimeOfWeekTest.class);
    /** Run tests with this Day Of Week test data value. */
	DayOfWeek dowExpected = DayOfWeek.MONDAY;
    /** Run tests with this Time Of Day test data value. */
	LocalTime todExpected = LocalTime.of(7, 30);
    /** Run tests with this Hour Of Day test data value. */
	int		  hourExpected = 7;
    /** Run tests with this Minute Of Hour test data value. */
	int       minuteExpected = 30;
	
	/**
	 * JUnit test that creating a new TimeOfWeek with default
	 * constructor works.
	 */
	@Test
	public void testTrashDayTimeOfWeek() {
		TimeOfWeek tow = new TimeOfWeek();
		assertNotNull(tow);
	}

	/**
	 * JUnit test that creating a new TimeOfWeek with DayOfWeek
	 * and TimeOfWeek arguments works.
	 */
	@Test
	public void testTrashDayTimeOfWeekDOWLT() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,todExpected);
		assertNotNull(tow);
	}

	/**
	 * JUnit test that creating a new TimeOfWeek with DayOfWeek,
	 * HourOfDay, and MinuteOfDay arguments works.
	 */
	@Test
	public void testTrashDayTimeOfWeekDOWIntInt() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,hourExpected,minuteExpected);
		assertNotNull(tow);
	}
	
	/**
	 * JUnit test that creating a new TimeOfWeek with valid DayOfWeek
	 * and *invalid* HourOfDay and MinuteOfDay arguments throws
	 * appropriate DateTimeException.
	 */
	@Test
	public void testTrashDayTimeOfWeekDOWIntIntExceptions() {
		TimeOfWeek tow = null;
		try {
			tow = new TimeOfWeek(dowExpected,24,minuteExpected);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for HourOfDay (valid values 0 - 23): 24", ex.getMessage());
	    }
		assertNull(tow);
		
		try {
			tow = new TimeOfWeek(dowExpected,-1,minuteExpected);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for HourOfDay (valid values 0 - 23): -1", ex.getMessage());
	    }
		assertNull(tow);

		try {
			tow = new TimeOfWeek(dowExpected,hourExpected,60);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for MinuteOfHour (valid values 0 - 59): 60", ex.getMessage());
	    }
		assertNull(tow);

		try {
			tow = new TimeOfWeek(dowExpected,hourExpected,-1);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for MinuteOfHour (valid values 0 - 59): -1", ex.getMessage());
	    }
		assertNull(tow);

	}

	/**
	 * JUnit test that creating a new TimeOfWeek with DayOfWeek integer,
	 * HourOfDay, and MinuteOfDay arguments works.
	 */
	@Test
	public void testTrashDayTimeOfWeekIntIntInt() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected.getValue(),hourExpected,minuteExpected);
		assertNotNull(tow);
	}
	
	/**
	 * JUnit test that creating a new TimeOfWeek with invalid DayOfWeek,
	 * HourOfDay and MinuteOfDay arguments throws
	 * appropriate DateTimeException.
	 */
	@Test
	public void testTrashDayTimeOfWeekIntIntIntExceptions() {
		TimeOfWeek tow = null;
		try {
			tow = new TimeOfWeek(0,hourExpected,minuteExpected);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for DayOfWeek: 0", ex.getMessage());
	    }
		assertNull(tow);
		
		try {
			tow = new TimeOfWeek(8,hourExpected,minuteExpected);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for DayOfWeek: 8", ex.getMessage());
	    }
		assertNull(tow);
		
		try {
			tow = new TimeOfWeek(dowExpected.getValue(),24,minuteExpected);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for HourOfDay (valid values 0 - 23): 24", ex.getMessage());
	    }
		assertNull(tow);
		
		try {
			tow = new TimeOfWeek(dowExpected.getValue(),-1,minuteExpected);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for HourOfDay (valid values 0 - 23): -1", ex.getMessage());
	    }
		assertNull(tow);
		
		try {
			tow = new TimeOfWeek(dowExpected.getValue(),hourExpected,60);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for MinuteOfHour (valid values 0 - 59): 60", ex.getMessage());
	    }
		assertNull(tow);
		
		try {
			tow = new TimeOfWeek(dowExpected.getValue(),hourExpected,-1);
	        fail("Expected a DateTimeException to be thrown.");
	    } catch (DateTimeException ex) {
	    	assertEquals("Invalid value for MinuteOfHour (valid values 0 - 59): -1", ex.getMessage());
	    }
		assertNull(tow);
	}
	
	/**
	 * JUnit test that checks the Day Of Week getter and setter methods.
	 */
	@Test
	public void testSetDayOfWeek() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected.plus(1),hourExpected,minuteExpected);
		DayOfWeek dowActual = tow.getDayOfWeek();
		assertEquals(dowExpected.plus(1).getValue(), dowActual.getValue());
		
		tow.setDayOfWeek(dowExpected);
		dowActual = tow.getDayOfWeek();
		assertEquals(dowExpected.getValue(), dowActual.getValue());
	}
	
	/**
	 * JUnit test that checks the Time Of Day getter and setter methods.
	 */
	@Test
	public void testSetTimeOfDay() {
		int minuteAdjusted = minuteExpected + 1;
		if (minuteAdjusted > 59) {
			minuteAdjusted -= 60;
		}
		LocalTime todExpected = LocalTime.of(hourExpected, minuteAdjusted);
		TimeOfWeek tow = new TimeOfWeek(dowExpected,hourExpected,minuteAdjusted);
		LocalTime todActual = tow.getTimeOfDay();
		assertEquals(todExpected.toString(), todActual.toString());
		
		todExpected = LocalTime.of(hourExpected, minuteExpected);
		tow.setTimeOfDay(hourExpected, minuteExpected);
		todActual = tow.getTimeOfDay();
		assertEquals(todExpected.toString(), todActual.toString());		
	}
	
	/**
	 * JUnit test that checks the Minute Of Day getter method.
	 */
	@Test
	public void testGetMinuteOfDay() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,LocalTime.of(7, 30));
		int minuteOfDayExpected = (7*60)+30;
		int minuteOfDayActual = tow.getMinuteOfDay();
		assertEquals(minuteOfDayExpected, minuteOfDayActual);
		
		tow = new TimeOfWeek(dowExpected,LocalTime.of(23, 00));
		minuteOfDayExpected = (23*60)+00;
		minuteOfDayActual = tow.getMinuteOfDay();
		assertEquals(minuteOfDayExpected, minuteOfDayActual);
		
		tow = new TimeOfWeek(dowExpected,LocalTime.of(00, 00));
		minuteOfDayExpected = (00*60)+00;
		minuteOfDayActual = tow.getMinuteOfDay();
		assertEquals(minuteOfDayExpected, minuteOfDayActual);
	}

	/**
	 * JUnit test that checks the Minute Of Week getter method.
	 */
	@Test
	public void testGetMinuteOfWeek() {
		TimeOfWeek tow = new TimeOfWeek(DayOfWeek.TUESDAY,LocalTime.of(7, 30));
		int minuteOfWeekExpected = (1*1440)+(7*60)+30;
		int minuteOfWeekActual = tow.getMinuteOfWeek();
		assertEquals(minuteOfWeekExpected, minuteOfWeekActual);
		
		tow = new TimeOfWeek(DayOfWeek.TUESDAY,LocalTime.of(23, 00));
		minuteOfWeekExpected = (1*1440)+(23*60)+00;
		minuteOfWeekActual = tow.getMinuteOfWeek();
		assertEquals(minuteOfWeekExpected, minuteOfWeekActual);
		
		tow = new TimeOfWeek(DayOfWeek.TUESDAY,LocalTime.of(00, 00));
		minuteOfWeekExpected = (1*1440)+(00*60)+00;
		minuteOfWeekActual = tow.getMinuteOfWeek();
		assertEquals(minuteOfWeekExpected, minuteOfWeekActual);
	}

	/**
	 * JUnit test that checks the Day Of Week getter method.
	 */
	@Test
	public void testGetDayOfWeek() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,hourExpected,minuteExpected);
		DayOfWeek dowActual = tow.getDayOfWeek();
		assertEquals(dowExpected.getValue(),dowActual.getValue());
	}
	
	/**
	 * JUnit test that checks the Time Of Day getter method.
	 */
	@Test
	public void testGetTimeOfDay() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,hourExpected,minuteExpected);
		LocalTime  todExpected = LocalTime.of(hourExpected, minuteExpected);
		LocalTime  todActual = tow.getTimeOfDay();
		assertEquals(todExpected,todActual);
	}

	/**
	 * JUnit test that checks the Hour Of Day getter method.
	 */
	@Test
	public void testGetHour() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,hourExpected,minuteExpected);
		int hourActual = tow.getHour();
		assertEquals(hourExpected,hourActual);
	}

	/**
	 * JUnit test that checks the Minute Of Hour getter method.
	 */
	@Test
	public void testGetMinute() {
		TimeOfWeek tow = new TimeOfWeek(dowExpected,hourExpected,minuteExpected);
		int minuteActual = tow.getMinute();
		assertEquals(minuteExpected,minuteActual);
	}
	
	/**
	 * JUnit test to confirm TimeOfDay prints correctly.
	 */
	@Test
	public void testToStringVerbal() {
		TimeOfWeek tow = new TimeOfWeek(DayOfWeek.TUESDAY,LocalTime.of(00, 00));
		String towExpected = "Tuesday midnight";
		String towActual = tow.toStringVerbal();
		assertEquals(towExpected,towActual);
		
		tow = new TimeOfWeek(DayOfWeek.FRIDAY,LocalTime.of(1, 15));
		towExpected = "Friday 1 15 AM";
		towActual = tow.toStringVerbal();
		assertEquals(towExpected,towActual);
		
		tow = new TimeOfWeek(DayOfWeek.MONDAY,LocalTime.of(23, 15));
		towExpected = "Monday 11 15 PM";
		towActual = tow.toStringVerbal();
		assertEquals(towExpected,towActual);
	}
	/**
	 * JUnit test to confirm TimeOfDay prints correctly.
	 */
	@Test
	public void testToStringPrintable() {
		TimeOfWeek tow = new TimeOfWeek(DayOfWeek.TUESDAY,LocalTime.of(00, 00));
		String towExpected = "Tuesday at midnight";
		String towActual = tow.toStringPrintable();
		assertEquals(towExpected,towActual);
		
		tow = new TimeOfWeek(DayOfWeek.FRIDAY,LocalTime.of(1, 15));
		towExpected = "Friday at 1:15 AM";
		towActual = tow.toStringPrintable();
		assertEquals(towExpected,towActual);
		
		tow = new TimeOfWeek(DayOfWeek.MONDAY,LocalTime.of(23, 15));
		towExpected = "Monday at 11:15 PM";
		towActual = tow.toStringPrintable();
		assertEquals(towExpected,towActual);
	}
	
	/**
	 * Test support function that checks the given TimeOfWeek and
	 * start datetime match the expected next pickup datetime.
	 * 
	 * @param ldtCurrentTime LocalDateTime that will be used as the
	 * 			initial start datetime
	 * @param towTest TimeOfWeek to be tested
	 * @param ldtStringExpected String containing the expected next
	 * 			datetime this TimeOfWeek will occur based on the
	 * 			given initial start datetime.
	 */
	private void testOneDay(LocalDateTime ldtCurrentTime, TimeOfWeek towTest, String ldtStringExpected) {
		log.debug("Next Pickup after (" + ldtCurrentTime.getDayOfWeek() + ") " + ldtCurrentTime + " for scheduled pickup on " + towTest);
		int minutesActual   = towTest.getMinutesUntilNext(ldtCurrentTime);			
		log.debug("  minutes until next pickup: got " + minutesActual + " minutes");
		LocalDateTime ldtActual = towTest.getNextPickupTime(ldtCurrentTime);
		log.debug("  expected next pickup time: " + ldtStringExpected + " got: " + ldtActual);
		assertEquals(ldtStringExpected, ldtActual.toString());		
	}
	
	/**
	 * Test support function that loops through an entire week of 
	 * start datetimes and checks TimeOfWeek next occurrence
	 * calculations are correct.
	 * 
	 * @param ldtCurrentTime LocalDateTime that will be used as the
	 * 			initial start time and then be incremented six more
	 * 			times to cover an entire week.
	 * @param scheduledDow DayOfWeek used to create the TimeOfWeek to be tested.
	 * @param scheduledHour int used to create the Hour Of Day to be tested.
	 * @param scheduledMinute int used to create the Minute Of Hour to be tested.
	 * @param expectedLdts List of the expected LocalDateTime results
	 * 			for each tested start datetime.
	 */
	private void loopEntireWeek(LocalDateTime ldtCurrentTime, DayOfWeek scheduledDow, int scheduledHour, int scheduledMinute, List<String> expectedLdts) {
		int testDowOffset = scheduledDow.getValue() - ldtCurrentTime.getDayOfWeek().getValue();
		if (testDowOffset < 0) { testDowOffset += 7; };
		for (int d=0; (d<7); d++) {
			TimeOfWeek towTest = new TimeOfWeek(scheduledDow,LocalTime.of(scheduledHour, scheduledMinute));
			String ldtStringExpected = expectedLdts.get(d);
			testOneDay(ldtCurrentTime,towTest,ldtStringExpected);
			scheduledDow = scheduledDow.plus(1);
		}		
	}
	
	/**
	 * JUnit test that TimeOfWeek calculations of the "next occurrence
	 * after a given start datetime" are correct for a variety of
	 * normal and edge cases.
	 */
	@Test
	public void testGetNextPickupTime() {
		LocalDateTime ldtCurrentTime = LocalDateTime.of(2016, 11, 22, 00, 00);  // Tuesday	Midnight
		DayOfWeek scheduledDow = DayOfWeek.TUESDAY;
		int scheduledHour = 01;
		int scheduledMinute = 15;
		List<String> expectedResults = new ArrayList<String>();
		expectedResults.add("2016-11-22T01:15");
		expectedResults.add("2016-11-23T01:15");
		expectedResults.add("2016-11-24T01:15");
		expectedResults.add("2016-11-25T01:15");
		expectedResults.add("2016-11-26T01:15");
		expectedResults.add("2016-11-27T01:15");
		expectedResults.add("2016-11-28T01:15");
		loopEntireWeek(ldtCurrentTime,scheduledDow,scheduledHour,scheduledMinute,expectedResults);
		
		ldtCurrentTime = LocalDateTime.of(2016, 11, 29, 01, 00);  // Tuesday 01:00
		scheduledDow = DayOfWeek.WEDNESDAY;
		scheduledHour = 02;
		scheduledMinute = 00;
		expectedResults.clear();
		expectedResults.add("2016-11-30T02:00");
		expectedResults.add("2016-12-01T02:00");
		expectedResults.add("2016-12-02T02:00");
		expectedResults.add("2016-12-03T02:00");
		expectedResults.add("2016-12-04T02:00");
		expectedResults.add("2016-12-05T02:00");
		expectedResults.add("2016-11-29T02:00");
		loopEntireWeek(ldtCurrentTime,scheduledDow,scheduledHour,scheduledMinute,expectedResults);
		
		// Tuesday 23:00 - Wednesday 01:00
		ldtCurrentTime = LocalDateTime.of(2016, 11, 22, 23, 00);  
		scheduledDow = DayOfWeek.WEDNESDAY;
		scheduledHour = 01;
		scheduledMinute = 00;		
		TimeOfWeek towTest = new TimeOfWeek(scheduledDow,LocalTime.of(scheduledHour, scheduledMinute));
		String ldtStringExpected = "2016-11-23T01:00";
		testOneDay(ldtCurrentTime,towTest,ldtStringExpected);
		
		// Tuesday 01:00 - Wednesday 23:00
		ldtCurrentTime = LocalDateTime.of(2016, 11, 22, 01, 00);  
		scheduledDow = DayOfWeek.WEDNESDAY;
		scheduledHour = 23;
		scheduledMinute = 00;
		towTest = new TimeOfWeek(scheduledDow,LocalTime.of(scheduledHour, scheduledMinute));
		ldtStringExpected = "2016-11-23T23:00";
		testOneDay(ldtCurrentTime,towTest,ldtStringExpected);
		
		// Monday 01:00 - Monday 00:00
		ldtCurrentTime = LocalDateTime.of(2016, 11, 21, 01, 00);  
		scheduledDow = DayOfWeek.MONDAY;
		scheduledHour = 00;
		scheduledMinute = 00;
		towTest = new TimeOfWeek(scheduledDow,LocalTime.of(scheduledHour, scheduledMinute));
		ldtStringExpected = "2016-11-28T00:00";
		testOneDay(ldtCurrentTime,towTest,ldtStringExpected);
		
		// Monday 01:00 - Monday 01:00
		ldtCurrentTime = LocalDateTime.of(2016, 11, 21, 01, 00);  
		scheduledDow = DayOfWeek.MONDAY;
		scheduledHour = 01;
		scheduledMinute = 00;
		towTest = new TimeOfWeek(scheduledDow,LocalTime.of(scheduledHour, scheduledMinute));
		ldtStringExpected = "2016-11-21T01:00";
		testOneDay(ldtCurrentTime,towTest,ldtStringExpected);
	}
}
