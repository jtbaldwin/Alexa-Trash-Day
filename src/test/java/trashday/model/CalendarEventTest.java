package trashday.model;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;

public class CalendarEventTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(CalendarEventTest.class);

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCalendarEventVEvent() {
		log.info("testCalendarEventVEvent");
		net.fortuna.ical4j.model.Date dStart = new net.fortuna.ical4j.model.Date();
		VEvent vEvent = new VEvent(dStart, "trash");
		CalendarEvent event = new CalendarEvent(vEvent);
		assertNotNull(event);
		assertEquals("trash", event.getName());
	}

	@Test
	public void testCalendarEventStringLocalDateTime() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.now());
		assertNotNull(event);
		assertEquals("trash", event.getName());
	}

	@Test
	public void testAddRecurrenceWeeklyInteger() {
		LocalDateTime ldtNow = LocalDateTime.of(2017, 2, 18, 12, 17);
		CalendarEvent event = new CalendarEvent("Trash", ldtNow);
		Integer interval=3;
		event.addRecurrenceWeekly(ldtNow.getDayOfWeek(), interval);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=3;BYDAY=SA", rrule.toString().trim());
	}

	@Test
	public void testAddRecurrenceWeekly() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 18);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;BYDAY=SA", rrule.toString().trim());
	}

	@Test
	public void testAddRecurrenceBiWeekly() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 15);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=SA", rrule.toString().trim());
	}

	@Test
	public void testAddRecurrenceDayOfMonthExceptions() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 1, 8, 0);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		
		try {
			event.addRecurrenceDayOfMonth(0, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No such day of month: 0", ex.getMessage());
		}
		
		try {
			event.addRecurrenceDayOfMonth(-32, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum day of month value (-31) exceeded: -32", ex.getMessage());
		}
		
		try {
			event.addRecurrenceDayOfMonth(32, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum day of month value (31) exceeded: 32", ex.getMessage());
		}
		
		try {
			event.addRecurrenceDayOfMonth(1, 0);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Interval must be a positive number.", ex.getMessage());
		}
	}
	
	@Test
	public void testAddRecurrenceDayOfMonth() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		Integer dom = 5;
		event.addRecurrenceDayOfMonth(dom, 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYMONTHDAY=5", rrule.toString().trim());
		
		event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		dom = 31;
		event.addRecurrenceDayOfMonth(dom, 1);
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYMONTHDAY=31", rrule.toString().trim());
		
		event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		dom = -31;
		event.addRecurrenceDayOfMonth(dom, 1);
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYMONTHDAY=-31", rrule.toString().trim());
	}

	@Test
	public void testAddRecurrenceWeekdayOfMonthExceptions() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		
		try {
			event.addRecurrenceWeekdayOfMonth(DayOfWeek.MONDAY, 0, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No recurrence meaning for weekNum=0", ex.getMessage());
		}
		
		try {
			event.addRecurrenceWeekdayOfMonth(DayOfWeek.MONDAY, -6, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum number of weeks-per-month value (-5) exceeded: -6", ex.getMessage());
		}
		
		try {
			event.addRecurrenceWeekdayOfMonth(DayOfWeek.MONDAY, 6, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum number of weeks-per-month value (5) exceeded: 6", ex.getMessage());
		}
		
		try {
			event.addRecurrenceWeekdayOfMonth(DayOfWeek.MONDAY, 1, 0);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Interval must be a positive number.", ex.getMessage());
		}
	}
	
	@Test
	public void testAddRecurrenceWeekdayOfMonth() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		DayOfWeek dow = DayOfWeek.FRIDAY;
		Integer weekNum = 2;
		event.addRecurrenceWeekdayOfMonth(dow, weekNum, 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYDAY=2FR", rrule.toString().trim());
		
		event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		dow = DayOfWeek.FRIDAY;
		weekNum = 5;
		event.addRecurrenceWeekdayOfMonth(dow, weekNum, 1);
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYDAY=5FR", rrule.toString().trim());

		event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		dow = DayOfWeek.FRIDAY;
		weekNum = -1;
		event.addRecurrenceWeekdayOfMonth(dow, weekNum, 1);
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYDAY=-1FR", rrule.toString().trim());
	}

	@Test
	public void testDeleteRecurrencesAll() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 17);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(2, rRuleList.size());
		
		event.deleteRecurrencesAll();
		
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(0, rRuleList.size());
	}

	@Test
	public void testDeleteRecurrenceWeeklyInt() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 17);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		Integer interval=3;
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), interval);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=3;BYDAY=SA", rrule.toString().trim());
		
		event.deleteRecurrenceWeekly(ldtEvent.getDayOfWeek(), interval);
		
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(0, rRuleList.size());
	}

	@Test
	public void testDeleteRecurrenceWeeklyIntException() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 17);
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.now());
		Integer interval=3;
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), interval);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=3;BYDAY=SA", rrule.toString().trim());
		
		try {
			event.deleteRecurrenceWeekly(ldtEvent.getDayOfWeek(), 0);		
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Interval must be a positive number.", ex.getMessage());
		}
	}

	@Test
	public void testDeleteRecurrenceWeekly() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 15);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;BYDAY=SA", rrule.toString().trim());
		
		event.deleteRecurrenceWeekly(ldtEvent.getDayOfWeek(), 1);
		
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(0, rRuleList.size());
	}

	@Test
	public void testDeleteRecurrenceBiWeekly() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 18);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=SA", rrule.toString().trim());
		
		event.deleteRecurrenceDayOfMonth(5, 1);
		
		event.deleteRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);
		
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(0, rRuleList.size());
	}

	@Test
	public void testDeleteRecurrenceDayOfMonthExceptions() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 1, 8, 0);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		
		try {
			event.deleteRecurrenceDayOfMonth(0, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No such day of month: 0", ex.getMessage());
		}
		
		try {
			event.deleteRecurrenceDayOfMonth(-32, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum day of month value (-31) exceeded: -32", ex.getMessage());
		}
		
		try {
			event.deleteRecurrenceDayOfMonth(32, 1);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum day of month value (31) exceeded: 32", ex.getMessage());
		}
		
		try {
			event.deleteRecurrenceDayOfMonth(5, 0);
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Interval must be a positive number.", ex.getMessage());
		}
	}

	@Test
	public void testDeleteRecurrenceDayOfMonth() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		Integer dom = 5;
		event.addRecurrenceDayOfMonth(dom, 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYMONTHDAY=5", rrule.toString().trim());
		
		event.deleteRecurrenceDayOfMonth(dom, 1);
		
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(0, rRuleList.size());
	}

	@Test
	public void testDeleteRecurrenceWeekdayOfMonthException() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 18, 12, 17);
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.now());
		event.addRecurrenceWeekdayOfMonth(ldtEvent.getDayOfWeek(), 2, 2);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=2SA", rrule.toString().trim());
		
		try {
			event.deleteRecurrenceWeekdayOfMonth(ldtEvent.getDayOfWeek(), 0, 1);		
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No recurrence meaning for weekNum=0", ex.getMessage());
		}
		
		try {
			event.deleteRecurrenceWeekdayOfMonth(ldtEvent.getDayOfWeek(), -6, 1);		
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum number of weeks-per-month value (-5) exceeded: -6", ex.getMessage());
		}
		
		try {
			event.deleteRecurrenceWeekdayOfMonth(ldtEvent.getDayOfWeek(), 6, 1);		
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum number of weeks-per-month value (5) exceeded: 6", ex.getMessage());
		}
		
		try {
			event.deleteRecurrenceWeekdayOfMonth(ldtEvent.getDayOfWeek(), 2, 0);		
			fail("Failed to throw expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Interval must be a positive number.", ex.getMessage());
		}
	}

	@Test
	public void testDeleteRecurrenceWeekdayOfMonth() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 0));
		DayOfWeek dow = DayOfWeek.FRIDAY;
		Integer weekNum = 2;
		event.addRecurrenceWeekdayOfMonth(dow, weekNum, 1);
		PropertyList rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(1, rRuleList.size());
		Property rrule = rRuleList.get(0);
		assertNotNull(rrule);
		assertEquals("RRULE:FREQ=MONTHLY;BYDAY=2FR", rrule.toString().trim());
		
		event.deleteRecurrenceWeekdayOfMonth(DayOfWeek.SUNDAY, weekNum, 1);
		
		event.deleteRecurrenceWeekdayOfMonth(dow, 1, 1);
		
		event.deleteRecurrenceWeekdayOfMonth(dow, weekNum, 1);
		
		rRuleList = event.getProperties("RRULE");
		assertNotNull(rRuleList);
		assertEquals(0, rRuleList.size());
	}

	@Test
	public void testGetName() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.now());
		assertNotNull(event);
		assertEquals("trash", event.getName());
	}

	@Test
	public void testIcalDateConversions() {
		LocalDateTime ldtTest = LocalDateTime.of(2017, 2, 15, 15, 29);
		CalendarEvent event = new CalendarEvent("Trash", ldtTest);
		assertNotNull(event);
		net.fortuna.ical4j.model.DateTime dTest = event.getIcalDateTime(ldtTest);
		assertEquals("20170215T152900", dTest.toString());
		LocalDateTime ldtCheck = event.getLocalDateTime(dTest);
		assertEquals("2017-02-15T15:29", ldtCheck.toString());
	}

	@Test
	public void testGetNextOccurrence() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 15, 15, 33);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 1);
		
		LocalDateTime ldtStartingPoint = ldtEvent;
		LocalDateTime ldtActual = event.getNextOccurrence(ldtStartingPoint);
		assertEquals("2017-02-22T15:33", ldtActual.toString());
		
		ldtStartingPoint=ldtActual;
		ldtActual = event.getNextOccurrence(ldtStartingPoint);
		assertEquals("2017-03-01T15:33", ldtActual.toString());
		
		ldtStartingPoint=ldtActual;
		event.deleteRecurrenceWeekly(ldtEvent.getDayOfWeek(), 1);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);
		ldtActual = event.getNextOccurrence(ldtStartingPoint);
		assertEquals("2017-03-15T15:33", ldtActual.toString());

		ldtStartingPoint=ldtActual;
		event.deleteRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);
		event.addRecurrenceDayOfMonth(1, 1);
		ldtActual = event.getNextOccurrence(ldtStartingPoint);
		assertEquals("2017-04-01T15:33", ldtActual.toString());

		ldtStartingPoint=ldtActual;
		event.deleteRecurrenceDayOfMonth(1, 1);
		event.addRecurrenceWeekdayOfMonth(DayOfWeek.FRIDAY, 2, 1);
		ldtActual = event.getNextOccurrence(ldtStartingPoint);
		assertEquals("2017-04-14T15:33", ldtActual.toString());
	}
	
	protected String cleanTimeStamps(String src) {
		return src
				.replaceAll("DTSTAMP:.*Z", "DTSTAMP:<ignore>") 
				.replaceAll("UID:.*AWS", "UID:<ignore>")
				.replaceAll("\\r\\n", "\n")
				;
	}

	@Test
	public void testGetProperties() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 1, 8, 0);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);

		String expected = "DTSTAMP:20170215T204049Z\n" + 
				"DTSTART:20170201T080000\n" + 
				"DTEND:20170201T080000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170215T204049Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=WE\n";
		assertEquals(cleanTimeStamps(expected), 
				cleanTimeStamps(event.getProperties().toString()));
	}

	@Test
	public void testGetPropertiesString() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 1, 8, 0);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceWeekly(ldtEvent.getDayOfWeek(), 2);
		
		String expected = "RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=WE";
		String actual = event.getProperties("RRULE").toString().trim();
		assertEquals(expected, actual);
	}

	@Test
	@Deprecated
	public void testGetRecurrencesPrintable() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 1, 1, 8, 0);
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 1, 8, 30);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceDayOfMonth(23, 1);
		
		String expected = "{B-23=on the twenty-third at 8:30 AM}";
		String actual= event.getRecurrencesPrintable(ldtBase).toString();
		assertEquals(expected, actual);
	}

	@Test
	@Deprecated
	public void testGetRecurrencesVerbal() {
		LocalDateTime ldtBase = LocalDateTime.of(2017, 1, 1, 8, 0);
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 1, 8, 30);
		CalendarEvent event = new CalendarEvent("Trash", ldtEvent);
		event.addRecurrenceDayOfMonth(23, 1);
		
		String expected = "{B-23=on the twenty-third at 8 30 AM}";
		String actual= event.getRecurrencesVerbal(ldtBase).toString();
		assertEquals(expected, actual);
	}

	@Test
	public void testGetStartIcalDate() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 30));
		
		net.fortuna.ical4j.model.Date dStart = event.getStartIcalDate();
		assertEquals("20170201T083000", dStart.toString());
	}

	@Test
	public void testGetStartLocalDateTime() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 30));
		LocalDateTime ldtActual = event.getStartLocalDateTime();
		assertEquals("2017-02-01T08:30", ldtActual.toString());
	}

	@Test
	public void testGetVEvent() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 30));
		VEvent vEvent = event.getVEvent();
		
		assertEquals("BEGIN:VEVENT\n" + 
				"DTSTAMP:<ignore>\n" + 
				"DTSTART:20170201T083000\n" + 
				"DTEND:20170201T083000\n" + 
				"SUMMARY:trash\n" + 
				"UID:<ignore>\n" + 
				"END:VEVENT\n", 
				cleanTimeStamps(vEvent.toString()));
	}

	@Test
	public void testHasRrules() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 1, 8, 30));
		assertFalse(event.hasRrules());
		event.addRecurrenceWeekdayOfMonth(DayOfWeek.SUNDAY, 3, 1);
		assertTrue(event.hasRrules());
	}

	@Test
	public void testMatches() {
		CalendarEvent event = new CalendarEvent("Trash", LocalDateTime.of(2017, 2, 5, 8, 30));
		event.addRecurrenceWeekdayOfMonth(DayOfWeek.SUNDAY, 3, 1);
		
		boolean match = event.matchesNameTod("trash", LocalDateTime.of(2017, 2, 19, 8, 30).toLocalTime());		
		assertTrue(match);
	}

}
