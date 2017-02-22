package trashday.model;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTimeUtilsTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DateTimeUtilsTest.class);
    
    @Test
    public void testGetDayOfWeek() {
    	log.info("testGetDayOfWeek");
    	
    	assertEquals(DayOfWeek.SUNDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.SU));
    	assertEquals(DayOfWeek.MONDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.MO));
    	assertEquals(DayOfWeek.TUESDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.TU));
    	assertEquals(DayOfWeek.WEDNESDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.WE));
    	assertEquals(DayOfWeek.THURSDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.TH));
    	assertEquals(DayOfWeek.FRIDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.FR));
    	assertEquals(DayOfWeek.SATURDAY, DateTimeUtils.getDayOfWeek(net.fortuna.ical4j.model.WeekDay.SA));
    }
    
    @Test
    public void testGetWeekDay() {
    	
    	for (int weekNum=1; weekNum<4; weekNum++) {
    		
    		net.fortuna.ical4j.model.WeekDay wd = DateTimeUtils.getWeekDay(DayOfWeek.SUNDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.SU.toString(), wd.getDay().toString());
    		
    		wd = DateTimeUtils.getWeekDay(DayOfWeek.MONDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.MO.toString(), wd.getDay().toString());
    		
    		wd = DateTimeUtils.getWeekDay(DayOfWeek.TUESDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.TU.toString(), wd.getDay().toString());
    		
    		wd = DateTimeUtils.getWeekDay(DayOfWeek.WEDNESDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.WE.toString(), wd.getDay().toString());
    		
    		wd = DateTimeUtils.getWeekDay(DayOfWeek.THURSDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.TH.toString(), wd.getDay().toString());
    		
    		wd = DateTimeUtils.getWeekDay(DayOfWeek.FRIDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.FR.toString(), wd.getDay().toString());
    		
    		wd = DateTimeUtils.getWeekDay(DayOfWeek.SATURDAY, weekNum);
    		assertEquals(weekNum, wd.getOffset());
    		assertEquals(net.fortuna.ical4j.model.WeekDay.SA.toString(), wd.getDay().toString());
    	}
    }

	@Test
	public void testGetNextOrSameDayOfMonthExceptions() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 2, 14, 12, 38);
		LocalTime tod = LocalTime.of(12, 38);
		
		LocalDateTime ldtEvent=null;
		try {
			ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, 32, tod);
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum day of month value (31) exceeded: 32", ex.getMessage());
		}
		assertNull("Failed to throw expected exception",ldtEvent);
		try {
			ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, 0, tod);
		} catch (IllegalArgumentException ex) {
			assertEquals("No such day of month: 0", ex.getMessage());
		}
		assertNull("Failed to throw expected exception",ldtEvent);
		try {
			ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, -32, tod);
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum day of month value (-31) exceeded: -32", ex.getMessage());
		}
		assertNull("Failed to throw expected exception",ldtEvent);
	}
		
	@Test
	public void testGetNextOrSameDayOfMonthVariableMonthLengths() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 2, 14, 12, 38);
		LocalTime tod = LocalTime.of(12, 38);
		
		LocalDateTime ldtEvent;
		List<String> expectedAnswers = new ArrayList<String>();
		expectedAnswers.add("2017-01-27T12:38");
		expectedAnswers.add("2017-01-28T12:38");
		expectedAnswers.add("2017-01-29T12:38");
		expectedAnswers.add("2017-01-30T12:38");
		expectedAnswers.add("2017-01-31T12:38");
		expectedAnswers.add("2017-02-27T12:38");
		expectedAnswers.add("2017-02-28T12:38");
		expectedAnswers.add("2017-03-29T12:38");
		expectedAnswers.add("2017-03-30T12:38");
		expectedAnswers.add("2017-03-31T12:38");
		expectedAnswers.add("2017-03-27T12:38");
		expectedAnswers.add("2017-03-28T12:38");
		expectedAnswers.add("2017-03-29T12:38");
		expectedAnswers.add("2017-03-30T12:38");
		expectedAnswers.add("2017-03-31T12:38");
		expectedAnswers.add("2017-04-27T12:38");
		expectedAnswers.add("2017-04-28T12:38");
		expectedAnswers.add("2017-04-29T12:38");
		expectedAnswers.add("2017-04-30T12:38");
		expectedAnswers.add("2017-05-31T12:38");		
		for (int month=1; month<=4; month++) {
			for (Integer dom=27; dom<=31; dom++) {
				ldtBase = LocalDateTime.of(2017, month, 14, 12, 38);
				ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, dom, tod);
				//assertEquals("2017-01-27T12:38", ldtEvent.toString());
				log.info("month={}, dom={} => ldt={}", month, dom, ldtEvent);
				assertEquals(expectedAnswers.remove(0), ldtEvent.toString());
			}
		}
	}
	
	@Test
	public void testGetNextOrSameDayOfMonthPositiveDom() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 2, 14, 12, 38);
		LocalTime tod = LocalTime.of(12, 38);
		
		LocalDateTime ldtEvent;
		List<String> expectedAnswers = new ArrayList<String>();
		expectedAnswers.add("2017-02-01T12:38");
		expectedAnswers.add("2017-02-02T12:38");
		expectedAnswers.add("2017-02-03T12:38");
		expectedAnswers.add("2017-02-04T12:38");
		expectedAnswers.add("2017-02-05T12:38");
		expectedAnswers.add("2017-02-06T12:38");
		expectedAnswers.add("2017-02-07T12:38");
		expectedAnswers.add("2017-02-08T12:38");
		expectedAnswers.add("2017-02-09T12:38");
		expectedAnswers.add("2017-02-10T12:38");
		expectedAnswers.add("2017-02-11T12:38");
		expectedAnswers.add("2017-02-12T12:38");
		expectedAnswers.add("2017-02-13T12:38");
		expectedAnswers.add("2017-01-14T12:38");
		expectedAnswers.add("2017-01-15T12:38");
		expectedAnswers.add("2017-01-16T12:38");
		expectedAnswers.add("2017-01-17T12:38");
		expectedAnswers.add("2017-01-18T12:38");
		expectedAnswers.add("2017-01-19T12:38");
		expectedAnswers.add("2017-01-20T12:38");
		expectedAnswers.add("2017-01-21T12:38");
		expectedAnswers.add("2017-01-22T12:38");
		expectedAnswers.add("2017-01-23T12:38");
		expectedAnswers.add("2017-01-24T12:38");
		expectedAnswers.add("2017-01-25T12:38");
		expectedAnswers.add("2017-01-26T12:38");
		expectedAnswers.add("2017-01-27T12:38");
		expectedAnswers.add("2017-01-28T12:38");
		expectedAnswers.add("2017-01-29T12:38");
		expectedAnswers.add("2017-01-30T12:38");
		expectedAnswers.add("2017-01-31T12:38");
		for (int month=1; month<=1; month++) {
			for (Integer dom=1; dom<=31; dom++) {
				ldtBase = LocalDateTime.of(2017, month, 14, 12, 38);
				log.info("Call getNextOrSameDayOfMonth({},{},{})", ldtBase, dom, tod);
				ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, dom, tod);
				//assertEquals("2017-01-27T12:38", ldtEvent.toString());
				log.info(" => ldt={}", ldtEvent);
				assertEquals(expectedAnswers.remove(0), ldtEvent.toString());
			}
		}
	}
	
	@Test
	public void testGetNextOrSameDayOfMonthNegativeDom() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 2, 14, 12, 38);
		LocalTime tod = LocalTime.of(12, 38);
		
		LocalDateTime ldtEvent;

		List<String> expectedAnswers = new ArrayList<String>();
		expectedAnswers.add("2017-03-01T12:38");
		expectedAnswers.add("2017-03-02T12:38");
		expectedAnswers.add("2017-03-03T12:38");
		expectedAnswers.add("2017-02-01T12:38");
		expectedAnswers.add("2017-02-02T12:38");
		expectedAnswers.add("2017-02-03T12:38");
		expectedAnswers.add("2017-02-04T12:38");
		expectedAnswers.add("2017-02-05T12:38");
		expectedAnswers.add("2017-02-06T12:38");
		expectedAnswers.add("2017-02-07T12:38");
		expectedAnswers.add("2017-02-08T12:38");
		expectedAnswers.add("2017-02-09T12:38");
		expectedAnswers.add("2017-02-10T12:38");
		expectedAnswers.add("2017-01-14T12:38");
		expectedAnswers.add("2017-01-15T12:38");
		expectedAnswers.add("2017-01-16T12:38");
		expectedAnswers.add("2017-01-17T12:38");
		expectedAnswers.add("2017-01-18T12:38");
		expectedAnswers.add("2017-01-19T12:38");
		expectedAnswers.add("2017-01-20T12:38");
		expectedAnswers.add("2017-01-21T12:38");
		expectedAnswers.add("2017-01-22T12:38");
		expectedAnswers.add("2017-01-23T12:38");
		expectedAnswers.add("2017-01-24T12:38");
		expectedAnswers.add("2017-01-25T12:38");
		expectedAnswers.add("2017-01-26T12:38");
		expectedAnswers.add("2017-01-27T12:38");
		expectedAnswers.add("2017-01-28T12:38");
		expectedAnswers.add("2017-01-29T12:38");
		expectedAnswers.add("2017-01-30T12:38");
		expectedAnswers.add("2017-01-31T12:38");
		for (int month=1; month<=1;month++) {
			for (Integer dom=-31; dom<=-1; dom++) {
				ldtBase = LocalDateTime.of(2017, month, 14, 12, 38);
				log.info("Call getNextOrSameDayOfMonth({},{},{})", ldtBase, dom, tod);
				ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, dom, tod);
				//assertEquals("2017-01-27T12:38", ldtEvent.toString());
				log.info(" => ldt={}", ldtEvent);
				assertEquals(expectedAnswers.remove(0), ldtEvent.toString());
			}
		}
	}
	
	@Test
	public void testGetNextOrSameDayOfMonth() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 2, 14, 12, 38);
		LocalTime tod = LocalTime.of(12, 38);
		LocalDateTime ldtEvent;

		List<String> expectedAnswers = new ArrayList<String>();
		// January 14th base ( 31 days in January)
		expectedAnswers.add("2017-03-01T12:38"); // dom=-31
		expectedAnswers.add("2017-03-02T12:38"); // dom=-30
		expectedAnswers.add("2017-03-03T12:38"); // dom=-29
		expectedAnswers.add("2017-02-01T12:38"); // dom=-28
		expectedAnswers.add("2017-02-02T12:38"); // dom=-27
		// February 14th base (28 days in February)
		expectedAnswers.add("2017-03-01T12:38"); // dom=-31
		expectedAnswers.add("2017-03-02T12:38"); // dom=-30
		expectedAnswers.add("2017-03-03T12:38"); // dom=-29
		expectedAnswers.add("2017-03-04T12:38"); // dom=-28
		expectedAnswers.add("2017-03-05T12:38"); // dom=-27
		// March 14th base (31 days in March)
		expectedAnswers.add("2017-05-01T12:38"); // dom=-31
		expectedAnswers.add("2017-04-01T12:38"); // dom=-30
		expectedAnswers.add("2017-04-02T12:38"); // dom=-29
		expectedAnswers.add("2017-04-03T12:38"); // dom=-28
		expectedAnswers.add("2017-04-04T12:38"); // dom=-27
		// April 14th base (30 days in April)
		expectedAnswers.add("2017-05-01T12:38"); // dom=-31
		expectedAnswers.add("2017-05-02T12:38"); // dom=-30
		expectedAnswers.add("2017-05-03T12:38"); // dom=-29
		expectedAnswers.add("2017-05-04T12:38"); // dom=-28
		expectedAnswers.add("2017-05-05T12:38"); // dom=-27
		
		for (int month=1; month<=4; month++) {
			for (Integer dom=-31; dom<=-27; dom++) {
				ldtBase = LocalDateTime.of(2017, month, 14, 12, 38);
				log.info("Call getNextOrSameDayOfMonth({},{},{})", ldtBase, dom, tod);
				ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtBase, dom, tod);
				log.info(" => ldt={}", ldtEvent);
				assertEquals(expectedAnswers.remove(0), ldtEvent.toString());
			}
		}
	}

	@Test
	public void testGetNextOrSameWeekdayOfMonthException() {
		DayOfWeek dow = DayOfWeek.SATURDAY;
		LocalTime tod = LocalTime.of(12, 38);
		LocalDateTime ldtBase = LocalDateTime.of(2017, 1, 1, 12, 38);
		LocalDateTime ldtEvent = null;
		try {
			log.info("Call testGetNextOrSameWeekdayOfMonth({},{},{})", ldtBase, dow, 6, tod);
			ldtEvent = DateTimeUtils.getNextOrSameWeekdayOfMonth(ldtBase, dow, 6, tod);
			fail("Did not receive expected exception");
		} catch (IllegalArgumentException ex) {
			assertEquals("Cannot find the 6th SATURDAY within a year", ex.getMessage());
		}
		assertNull(ldtEvent);
		
		try {
			log.info("Call testGetNextOrSameWeekdayOfMonth({},{},{})", ldtBase, dow, 0, tod);
			ldtEvent = DateTimeUtils.getNextOrSameWeekdayOfMonth(ldtBase, dow, 0, tod);
			fail("Did not receive expected exception");
		} catch (IllegalArgumentException ex) {
			assertEquals("No such week number: 0", ex.getMessage());
		}
		assertNull(ldtEvent);
	}
	
	@Test
	public void testGetNextOrSameWeekdayOfMonthNegative() {
		LocalTime tod = LocalTime.of(12, 38);
		LocalDateTime ldtEvent;
		DayOfWeek dow = DayOfWeek.SATURDAY;
		
		List<String> expectedAnswers = new ArrayList<String>();
		expectedAnswers.add("2017-02-04T12:38");
		expectedAnswers.add("2017-02-11T12:38");
		expectedAnswers.add("2017-02-18T12:38");
		expectedAnswers.add("2017-02-25T12:38");

		for (int month=2; month<=2; month++) {
			for (Integer weekNum=-4; weekNum<=-1; weekNum++) {
				LocalDateTime ldtBase = LocalDateTime.of(2017, month, 1, 12, 38);
				log.info("Call testGetNextOrSameWeekdayOfMonth({},{},{})", ldtBase, dow, weekNum, tod);
				ldtEvent = DateTimeUtils.getNextOrSameWeekdayOfMonth(ldtBase, dow, weekNum, tod);
				log.info(" => ldt={}", ldtEvent);
				assertEquals(expectedAnswers.remove(0), ldtEvent.toString());
			}
		}		
	}
	
	@Test
	public void testGetNextOrSameWeekdayOfMonthPositive() {
		LocalTime tod = LocalTime.of(12, 38);
		LocalDateTime ldtEvent;
		DayOfWeek dow = DayOfWeek.SATURDAY;
		
		List<String> expectedAnswers = new ArrayList<String>();
		expectedAnswers.add("2017-01-07T12:38");
		expectedAnswers.add("2017-01-14T12:38");
		expectedAnswers.add("2017-01-21T12:38");
		expectedAnswers.add("2017-01-28T12:38");
		expectedAnswers.add("2017-04-29T12:38");

		for (int month=1; month<=1; month++) {
			for (Integer weekNum=1; weekNum<=5; weekNum++) {
				LocalDateTime ldtBase = LocalDateTime.of(2017, month, 1, 12, 38);
				log.info("Call testGetNextOrSameWeekdayOfMonth({},{},{})", ldtBase, dow, weekNum, tod);
				ldtEvent = DateTimeUtils.getNextOrSameWeekdayOfMonth(ldtBase, dow, weekNum, tod);
				log.info(" => ldt={}", ldtEvent);
				assertEquals(expectedAnswers.remove(0), ldtEvent.toString());
			}
		}		
		
		LocalDateTime ldtBase = LocalDateTime.of(2017, 2, 15, 12, 38);
		log.info("Call testGetNextOrSameWeekdayOfMonth({},{},{})", ldtBase, dow, 1, tod);
		ldtEvent = DateTimeUtils.getNextOrSameWeekdayOfMonth(ldtBase, dow, 1, tod);
		log.info(" => ldt={}", ldtEvent);
		assertEquals("2017-03-04T12:38", ldtEvent.toString());
	}
}
