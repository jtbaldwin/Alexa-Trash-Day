package trashday.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.SocketException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.property.RRule;
import trashday.ui.FormatUtils;

/**
 * JUnit tests for the {@link trashday.model.Calendar} class.
 * 
 * @author J. Todd Baldwin
 */
@RunWith(JUnit4.class)
public class CalendarTest {
    @Rule
    public ExpectedException thrown= ExpectedException.none();
    
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(CalendarTest.class);
    /** An example schedule for the tests to use */
	private static Calendar testDataCalendar;
	
	/**
	 * Before starting tests in this class, create and initialize
	 * an example schedule.
	 */
	@BeforeClass
	public static void setUpBeforeClass() {
		log.info("Load test calendar data.");
		
		testDataCalendar = new Calendar();
		testDataCalendar.initBasicExampleCalendar();
		log.info("testDataCalendar: {}", testDataCalendar);
	}
	
//	public CalendarEvent createBasicEvent() throws SocketException {
//		return testDataCalendar.eventCreate("Trash", LocalDateTime.of(2017, 2, 5, 16, 15));
//	}
//	
	private String ignoreTestTimestamps(String testData) {
		return testData
				.replaceAll("DTSTAMP:\\d+T\\d+Z", "DTSTAMP:<ignore>")
				.replaceAll("UID:\\d+T\\d+Z", "UID:<ignore>")
				.replaceAll("\\r", "")
				;
	}
	
	@Test
	public void testNextPickupsForCalendar() {
		// The recurrence window to be checked
		LocalDateTime ldtStartingPoint = LocalDateTime.of(2017, 2, 7, 7, 15);
		NextPickups nextPickups = new NextPickups(ldtStartingPoint, testDataCalendar, null);
		
		String expectedNextPickups = "Next trash pickup is today at 7:30 AM.\n" + 
				"Next recycling pickup is Friday, February 17 at 7:30 AM.\n";
		String actualNextPickups = FormatUtils.printableNextPickups(nextPickups);
		assertEquals(expectedNextPickups, actualNextPickups);
		
		ldtStartingPoint = LocalDateTime.of(2017, 2, 7, 7, 45);
		nextPickups = new NextPickups(ldtStartingPoint, testDataCalendar, null);
		
		expectedNextPickups = "Next trash pickup is Friday at 7:30 AM.\n" + 
				"Next recycling pickup is Friday, February 17 at 7:30 AM.\n";
		actualNextPickups = FormatUtils.printableNextPickups(nextPickups);
		assertEquals(expectedNextPickups, actualNextPickups);
	}
	
	@Test
	public void testBasicSchedule() throws SocketException {
		// uidGen@fe80:0:0:0:1068:73ff:fe46:4136%awdl0
		String expectedCalendar = "BEGIN:VCALENDAR\n" + 
				"PRODID:-//Ben Fortuna//iCal4j 2.0.0//EN\n" + 
				"VERSION:2.0\n" + 
				"CALSCALE:GREGORIAN\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170206T030400Z\n" + 
				"DTSTART:20170131T073000\n" + 
				"DTEND:20170131T073000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170206T030400Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;BYDAY=TU\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170206T030405Z\n" + 
				"DTSTART:20170203T073000\n" + 
				"DTEND:20170203T073000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170206T030405Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;BYDAY=FR\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170206T030410Z\n" + 
				"DTSTART:20170203T073000\n" + 
				"DTEND:20170203T073000\n" + 
				"SUMMARY:recycling\n" + 
				"UID:20170206T030410Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=FR\n" + 
				"END:VEVENT\n" + 
				"END:VCALENDAR\n";
		String actualCalendar = testDataCalendar.toStringRFC5545();
		log.info("Calendar: {}", actualCalendar);
		
		assertEquals(
				ignoreTestTimestamps(expectedCalendar), 
				ignoreTestTimestamps(actualCalendar));
	}
	
	@Test
	public void testCalendarFromVCALENDAR() throws IOException, ParserException {
		String expectedCalendar = "BEGIN:VCALENDAR\n" + 
				"PRODID:-//Ben Fortuna//iCal4j 2.0.0//EN\n" + 
				"VERSION:2.0\n" + 
				"CALSCALE:GREGORIAN\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170131T073000\n" + 
				"DTEND:20170131T073000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170210T192058Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170203T073000\n" + 
				"DTEND:20170203T073000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170210T192059Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170203T073000\n" + 
				"DTEND:20170203T073000\n" + 
				"SUMMARY:recycling\n" + 
				"UID:20170210T192100Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;INTERVAL=2\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170201T120000\n" + 
				"DTEND:20170201T120000\n" + 
				"SUMMARY:lawn waste\n" + 
				"UID:20170210T192101Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170215T120000\n" + 
				"DTEND:20170215T120000\n" + 
				"SUMMARY:lawn waste\n" + 
				"UID:20170210T192102Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY\n" + 
				"END:VEVENT\n" + 
				"END:VCALENDAR\n";
		Calendar calendar = new Calendar(expectedCalendar);
		String actualCalendar = calendar.toStringRFC5545();
		assertEquals(ignoreTestTimestamps(expectedCalendar), ignoreTestTimestamps(actualCalendar));
	}
	
	@Test
	public void testWeeklyRecurringEvent() throws SocketException {
		// Create a weekly recurring event
		LocalDateTime ldtEventStart = LocalDateTime.of(2017, 2, 7, 7, 30);
		CalendarEvent event = new CalendarEvent("Trash", ldtEventStart);
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		log.info("Event Start: {}", ldtEventStart);
		
		// Query for next time the event recurs...
		for (int i=7; i<14; i++) {
			LocalDateTime ldtSearchStart = LocalDateTime.of(2017, 2, i, 16, 00);
			log.info("Find Recurrence {} after: {}", i, ldtSearchStart);
			LocalDateTime ldtEventRecurs = event.getNextOccurrence(ldtSearchStart);
			assertNotNull(ldtEventRecurs);
			log.info("Event Recurs on {}", ldtEventRecurs);
			
			assertEquals("Tuesday, February 14 at 7:30 AM", FormatUtils.printableDateAndTime(ldtEventRecurs));		
		}
	}
	
	@Test
	public void testBiWeeklyBasicRecurringEvent() throws SocketException {
		String pickupName = "Trash";
		
		// Create a biweekly recurring event
		Calendar cal = new Calendar();
		LocalDateTime ldtEventStart = LocalDateTime.of(2017, 2, 7, 7, 30);
		cal.pickupAddBiWeekly(ldtEventStart, pickupName, false, DayOfWeek.TUESDAY, ldtEventStart.toLocalTime());
		
		// Query for next time the event recurs...
		for (int i=7; i<21; i++) {
			LocalDateTime ldtSearchStart = LocalDateTime.of(2017, 2, i, 16, 00);
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart, pickupName);
			assertNotNull(ldtEventRecurs);
			assertEquals("Tuesday, February 21 at 7:30 AM", FormatUtils.printableDateAndTime(ldtEventRecurs));		
		}
	}
	
	@Test
	public void testBiWeeklyAlternatingEvent() {		
		Calendar cal = new Calendar();
		LocalDateTime ldtEventStart = LocalDateTime.of(2017, 2, 18, 15, 30);
		LocalDateTime ldtPrintRequest = LocalDateTime.of(2017, 2, 23, 15, 30);
		
		cal.pickupAddBiWeekly(ldtEventStart, "Recycling", true, ldtEventStart.getDayOfWeek(), ldtEventStart.toLocalTime());
		cal.pickupAddBiWeekly(ldtEventStart, "Lawn Waste", false, ldtEventStart.getDayOfWeek(), ldtEventStart.toLocalTime());
		
		String expectedCalendar = 
				"Pickup recycling every other Saturday at 3:30 PM (on this Saturday).\n" + 
				"Pickup lawn waste every other Saturday at 3:30 PM (on next Saturday, March 4).\n";
		String actualCalendar = FormatUtils.printableCalendar(cal, ldtPrintRequest);
		assertEquals(expectedCalendar, actualCalendar);
	}
		
	@Test
	public void testBiWeeklyNextRecurringEvent() throws SocketException {
		String pickupName = "Trash";
		
		Calendar cal = new Calendar();
		LocalDateTime ldtEventStart = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 45));
		log.info("Add biweekly event starting one week from: {}", FormatUtils.printableDateAndTime(ldtEventStart));
		
		cal.pickupAddBiWeekly(ldtEventStart, pickupName, true, ldtEventStart.getDayOfWeek(), ldtEventStart.toLocalTime());
		List<CalendarEvent> events = cal.getEvents(pickupName);
		assertEquals(1, events.size());
		CalendarEvent event = events.get(0);
		log.info("Add biweekly event starting: {}", FormatUtils.printableDateAndTime(event.getStartLocalDateTime()));
		
		assertEquals(ldtEventStart.getDayOfWeek(), event.getStartLocalDateTime().getDayOfWeek());
		
		// Query for next time the event recurs...
		LocalDateTime ldtSearchStart = ldtEventStart.minusMinutes(15);
		LocalDateTime ldtExpected = ldtEventStart.plusDays(7);
		for (int i=1; i<8; i++) {
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart.plusDays(i), pickupName);
			assertNotNull(ldtEventRecurs);
			log.info("Find next bi-weekly recurrence after {} gives: {}", ldtSearchStart.plusDays(i), FormatUtils.printableDateAndTime(ldtEventRecurs));
			String expectedRecurrence = FormatUtils.printableDateAndTime(ldtExpected);
			assertEquals(expectedRecurrence, FormatUtils.printableDateAndTime(ldtEventRecurs));		
		}
		ldtExpected = ldtEventStart.plusDays(21);
		for (int i=8; i<21; i++) {
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart.plusDays(i), pickupName);
			assertNotNull(ldtEventRecurs);
			log.info("Find next bi-weekly recurrence after {} gives: {}", ldtSearchStart.plusDays(i), FormatUtils.printableDateAndTime(ldtEventRecurs));
			String expectedRecurrence = FormatUtils.printableDateAndTime(ldtExpected);
			assertEquals(expectedRecurrence, FormatUtils.printableDateAndTime(ldtEventRecurs));		
		}
	}
	
	@Test
	public void testBiWeeklyThisRecurringEvent() throws SocketException {
		String pickupName = "Trash";
		
		Calendar cal = new Calendar();
		LocalDateTime ldtEventStart = LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 45));
		log.info("Add biweekly event starting: {}", FormatUtils.printableDateAndTime(ldtEventStart));
		
		cal.pickupAddBiWeekly(ldtEventStart, pickupName, false, ldtEventStart.getDayOfWeek(), ldtEventStart.toLocalTime());
		List<CalendarEvent> events = cal.getEvents(pickupName);
		assertEquals(1, events.size());
		CalendarEvent event = events.get(0);
		log.info("Add biweekly event starting: {}", FormatUtils.printableDateAndTime(event.getStartLocalDateTime()));
		
		assertEquals(ldtEventStart.getDayOfWeek(), event.getStartLocalDateTime().getDayOfWeek());
		
		// Query for next time the event recurs...
		LocalDateTime ldtSearchStart = ldtEventStart.minusMinutes(15);
		LocalDateTime ldtExpected = ldtEventStart.plusDays(14);
		for (int i=1; i<15; i++) {
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart.plusDays(i), pickupName);
			assertNotNull(ldtEventRecurs);
			log.info("Find next bi-weekly recurrence after {} gives: {}", ldtSearchStart.plusDays(i), FormatUtils.printableDateAndTime(ldtEventRecurs));
			String expectedRecurrence = FormatUtils.printableDateAndTime(ldtExpected);
			assertEquals(expectedRecurrence, FormatUtils.printableDateAndTime(ldtEventRecurs));		
		}
		ldtExpected = ldtEventStart.plusDays(28);
		for (int i=15; i<29; i++) {
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart.plusDays(i), pickupName);
			assertNotNull(ldtEventRecurs);
			log.info("Find next bi-weekly recurrence after {} gives: {}", ldtSearchStart.plusDays(i), FormatUtils.printableDateAndTime(ldtEventRecurs));
			String expectedRecurrence = FormatUtils.printableDateAndTime(ldtExpected);
			assertEquals(expectedRecurrence, FormatUtils.printableDateAndTime(ldtEventRecurs));		
		}
		
	}
	
	@Test
	public void testMonthlyLDTRecurringEvent() throws SocketException {
		// Create a monthly recurring event
		Calendar cal = new Calendar();
		LocalDateTime ldtEventStart = LocalDateTime.of(2017, 1, 1, 7, 30);
		cal.pickupAddDayOfMonth(ldtEventStart, "Trash", 1, ldtEventStart.toLocalTime());
		
		// Query for next time the event recurs...
		List<String> expectedRecurrences = new ArrayList<String>();
		expectedRecurrences.add("Wednesday, February 1 at 7:30 AM");
		expectedRecurrences.add("Wednesday, March 1 at 7:30 AM");
		expectedRecurrences.add("Saturday, April 1 at 7:30 AM");
		expectedRecurrences.add("Monday, May 1 at 7:30 AM");
		expectedRecurrences.add("Thursday, June 1 at 7:30 AM");
		expectedRecurrences.add("Saturday, July 1 at 7:30 AM");
		expectedRecurrences.add("Tuesday, August 1 at 7:30 AM");
		expectedRecurrences.add("Friday, September 1 at 7:30 AM");
		expectedRecurrences.add("Sunday, October 1 at 7:30 AM");
		expectedRecurrences.add("Wednesday, November 1 at 7:30 AM");
		expectedRecurrences.add("Friday, December 1 at 7:30 AM");
		expectedRecurrences.add("Monday, January 1 at 7:30 AM");
		expectedRecurrences.add("Thursday, February 1 at 7:30 AM");
		LocalDateTime ldtSearchStart = LocalDateTime.of(2016, 12, 31, 16, 00);
		int currentMonth = 1;
		for (int i=1; i<370; i++) {
			ldtSearchStart = ldtSearchStart.plusDays(1);
			if (ldtSearchStart.getMonth().getValue() != currentMonth) {
				expectedRecurrences.remove(0);
				currentMonth = ldtSearchStart.getMonth().getValue();
			}
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart, "Trash");
			assertNotNull(ldtEventRecurs);
			String actualRecurrence = FormatUtils.printableDateAndTime(ldtEventRecurs);
			log.trace("Next recurrence after {} is: {}", ldtSearchStart, actualRecurrence);
			assertEquals(expectedRecurrences.get(0), actualRecurrence);
		}
	}
	
	@Test
	public void testAddRecurrenceDayOfMonthExceptions() {
		// Create a monthly recurring event
		Calendar cal = new Calendar();
		LocalDateTime ldtRequest = LocalDateTime.of(2017, 2, 9, 13, 35);
		LocalDateTime ldtEventStart = LocalDateTime.of(2017, 2, 9, 7, 30);
		String pickupName = "Trash";
		
		try {
			cal.pickupAddDayOfMonth(ldtRequest, pickupName, -32, ldtEventStart.toLocalTime());
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum day of month value (-31) exceeded: -32", ex.getMessage());
		}
		
		try {
			cal.pickupAddDayOfMonth(ldtRequest, pickupName, 0, ldtEventStart.toLocalTime());
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No such day of month: 0", ex.getMessage());
		}
		
		try {
			cal.pickupAddDayOfMonth(ldtRequest, pickupName, 32, ldtEventStart.toLocalTime());
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum day of month value (31) exceeded: 32", ex.getMessage());
		}
		
	}
	
	@Test
	public void testPickupAddWeekdayOfMonth() {
		LocalDateTime ldtEvent = LocalDateTime.of(2017,  2, 15, 21, 00);
		Calendar cal = new Calendar();

		cal.pickupAddWeekdayOfMonth(ldtEvent, "trash", 2, DayOfWeek.THURSDAY, java.time.LocalTime.of(21, 00));
		cal.pickupAddWeekdayOfMonth(ldtEvent, "trash", 3, DayOfWeek.THURSDAY, java.time.LocalTime.of(21, 00));
		
		cal.pickupDeleteWeekdayOfMonth("trash", 3, DayOfWeek.THURSDAY, java.time.LocalTime.of(21, 00));
		cal.pickupDeleteWeekdayOfMonth("trash", 2, DayOfWeek.THURSDAY, java.time.LocalTime.of(21, 00));
		
		assertTrue(cal.isEmpty());
	}
	
	@Test
	public void testMonthlyDOMRecurringEvent() throws SocketException {
		LocalDateTime ldtRequest = LocalDateTime.of(2017, 2, 9, 13, 35);
		String pickupName = "Trash";
		
		// Create a monthly recurring event
		Calendar cal = new Calendar();
		LocalDateTime ldtEventStart = LocalDateTime.of(2017, 2, 9, 7, 30);
		cal.pickupAddDayOfMonth(ldtRequest, pickupName, 1, ldtEventStart.toLocalTime());
		List<CalendarEvent> events = cal.getEvents(pickupName);
		assertEquals(1, events.size());
		CalendarEvent event = events.get(0);
		log.info("Add monthly event starting: {}", FormatUtils.printableDateAndTime(event.getStartLocalDateTime()));
		
		// Query for next time the event recurs...
		List<String> expectedRecurrences = new ArrayList<String>();
		LocalDateTime ldtMonthStart = ldtEventStart.minusDays(ldtEventStart.getDayOfMonth()-1);
		for (int i=1; i<=24; i++) {
			expectedRecurrences.add(FormatUtils.printableDateAndTime(ldtMonthStart.plusMonths(i)));
		}		
		
		LocalDateTime ldtSearchStart = LocalDateTime.of(LocalDate.now(), LocalTime.of(16, 0));
		int currentMonth = ldtSearchStart.getMonthValue();
		for (int i=1; i<370; i++) {
			ldtSearchStart = ldtSearchStart.plusDays(1);
			if (ldtSearchStart.getMonth().getValue() != currentMonth) {
				expectedRecurrences.remove(0);
				currentMonth = ldtSearchStart.getMonth().getValue();
			}
			LocalDateTime ldtEventRecurs = cal.pickupGetNextOccurrence(ldtSearchStart, "Trash");
			assertNotNull(ldtEventRecurs);
			String actualRecurrence = FormatUtils.printableDateAndTime(ldtEventRecurs);
			log.debug("Next recurrence after {} is: {}", ldtSearchStart, actualRecurrence);
			assertEquals(expectedRecurrences.get(0), actualRecurrence);
		}
	}
	
	@Test
	public void testHasExceptions() {
		Calendar cal = new Calendar();
		cal.initComplexExampleCalendar();
		
		CalendarEvent event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		try {
			event.getVEvent().getSummary().setValue(null);
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept events without names.", ex.getMessage());
		}
		
		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept events without exactly one RRULE. size=0", ex.getMessage());
		}
		
		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 2);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept events without exactly one RRULE. size=2", ex.getMessage());
		}
		
		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		Recur recur = new Recur(Recur.WEEKLY, 0);
		RRule rrule = new RRule(recur);
		event.getProperties().add(rrule);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept WEEKLY RRULEs without exactly one BYDAY entry.", ex.getMessage());
		}

		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		recur = new Recur(Recur.WEEKLY, 0);
        recur.getDayList().add(DateTimeUtils.getWeekDay(DayOfWeek.SATURDAY, 0));
        recur.getDayList().add(DateTimeUtils.getWeekDay(DayOfWeek.SUNDAY, 0));
		rrule = new RRule(recur);
		event.getProperties().add(rrule);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept WEEKLY RRULEs without exactly one BYDAY entry.", ex.getMessage());
		}

		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		recur = new Recur(Recur.MONTHLY, 0);
		rrule = new RRule(recur);
		event.getProperties().add(rrule);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept MONTHLY RRULEs without exactly one non-null BYDAY or BYMONTHDAY entry.", ex.getMessage());
		}

		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		recur = new Recur(Recur.MONTHLY, 0);
        recur.getDayList().add(DateTimeUtils.getWeekDay(DayOfWeek.SATURDAY, 2));
        recur.getMonthDayList().add(15);
		rrule = new RRule(recur);
		event.getProperties().add(rrule);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept MONTHLY RRULEs without exactly one non-null BYDAY or BYMONTHDAY entry.", ex.getMessage());
		}

		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		recur = new Recur(Recur.YEARLY, 0);
		rrule = new RRule(recur);
		event.getProperties().add(rrule);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept RRULEs that are not WEEKLY or MONTHLY frequency.", ex.getMessage());
		}

		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		recur = new Recur(Recur.WEEKLY, 0);
		recur.setInterval(0);
        recur.getDayList().add(null);
		rrule = new RRule(recur);
		event.getProperties().add(rrule);
		try {
			cal.has(event);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Will not accept WEEKLY RRULEs without exactly one non-null BYDAY entry.", ex.getMessage());
		}

	}
	
	@Test
	public void testHas() {
		Calendar cal = new Calendar();
		cal.initComplexExampleCalendar();

		// Trash: Tuesday morning
		CalendarEvent event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		Boolean added = cal.eventAdd(event);
		assertFalse(added);
		
		// Trash: Friday morning
		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 2, 3, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 1);
		added = cal.eventAdd(event);
		assertFalse(added);
		
		// Recycling: Friday morning
		event = new CalendarEvent("Recycling", java.time.LocalDateTime.of(2017, 2, 3, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 2);
		added = cal.eventAdd(event);
		assertFalse(added);
		
		// Lawn Waste: First of month
		event = new CalendarEvent("Lawn Waste", java.time.LocalDateTime.of(2017, 2, 1, 12, 00));
		event.addRecurrenceDayOfMonth(1, 1);
		added = cal.eventAdd(event);
		assertFalse(added);
		
		// Lawn Waste: Fifteenth of month
		event = new CalendarEvent("Lawn Waste", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceDayOfMonth(15, 1);
		added = cal.eventAdd(event);
		assertFalse(added);
		
		// Scrap Metal: Last day of month
		event = new CalendarEvent("scrap metal", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceDayOfMonth(-1, 1);
		added = cal.eventAdd(event);
		assertFalse(added);
		
		// Mortgage: Five days before end of month day of month
		event = new CalendarEvent("mortgage", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceDayOfMonth(-5, 1);
		added = cal.eventAdd(event);
		assertFalse(added);
		
		// Dry Cleaning: Second Saturday of every month
		event = new CalendarEvent("dry cleaning", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceWeekdayOfMonth(DayOfWeek.SATURDAY, 2, 1);
		added = cal.eventAdd(event);
		assertFalse(added);
	}

	@Test
	public void testEventDeletes() throws SocketException {
		String pickupName = "Trash";
		LocalDateTime ldtRequest = LocalDateTime.of(2017, 2, 9, 13, 35); // Thursday
		LocalDateTime ldtEvent = LocalDateTime.of(2017, 2, 10, 11, 30);  // Friday
		
		// Create a monthly recurring event
		Calendar cal = new Calendar();
		Boolean added = cal.pickupAddWeekly(ldtEvent, pickupName, DayOfWeek.FRIDAY, ldtEvent.toLocalTime());
		assertTrue(added);
		added = cal.pickupAddBiWeekly(ldtEvent.plusDays(8), pickupName, false, DayOfWeek.SATURDAY, ldtEvent.toLocalTime());
		assertTrue(added);
		added = cal.pickupAddBiWeekly(ldtEvent.plusDays(2), pickupName, true, ldtEvent.plusDays(2).getDayOfWeek(), LocalTime.of(11, 30));
		assertTrue(added);
		added = cal.pickupAddBiWeekly(ldtEvent.plusDays(2), pickupName, false, ldtEvent.plusDays(2).getDayOfWeek(), LocalTime.of(11, 30));
		assertTrue(added);
		added = cal.pickupAddDayOfMonth(ldtEvent.plusDays(35), pickupName, ldtEvent.getDayOfMonth(), ldtEvent.toLocalTime());
		assertTrue(added);
		added = cal.pickupAddDayOfMonth(ldtRequest, pickupName, ldtEvent.getDayOfMonth(), LocalTime.of(11, 30));
		assertTrue(added);
		
		List<CalendarEvent> events = cal.getEvents();
		assertEquals(6, events.size());
		
		cal.pickupDeleteWeekly(pickupName, ldtEvent.getDayOfWeek(), ldtEvent.toLocalTime());
		cal.pickupDeleteBiWeekly(pickupName, ldtEvent.plusDays(8).getDayOfWeek(), ldtEvent.toLocalTime());
		cal.pickupDeleteBiWeekly(pickupName, ldtEvent.plusDays(2).getDayOfWeek(), LocalTime.of(11, 30));
		cal.pickupDeleteDayOfMonth(pickupName, ldtEvent.plusDays(35).getDayOfMonth(), ldtEvent.toLocalTime());
		cal.pickupDeleteDayOfMonth(pickupName, ldtEvent.getDayOfMonth(), LocalTime.of(11, 30));
		
		events = cal.getEvents();
		assertEquals(0, events.size());
	}
	
	@Test
	public void testCalendarIsEmpty() {
		Calendar calendar = new Calendar();
		assertTrue(calendar.isEmpty());
		
		calendar.initComplexExampleCalendar();
		assertFalse(calendar.isEmpty());
	}
		
	@Test
	public void testCalendarPrintable() {
		LocalDateTime ldtRequest = LocalDateTime.of(2017, 2, 12, 9, 5);
		Calendar calendar = new Calendar();
		calendar.initComplexExampleCalendar();

		String expectedCalendar = 
				"Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.\n" + 
				"Pickup recycling every other Friday at 7:30 AM (on this Friday).\n" + 
				"Pickup lawn waste on the first at noon and on the fifteenth at noon.\n" +
				"Pickup scrap metal on the last day of the month at noon.\n" +
				"Pickup mortgage on the fifth day before the end of the month at noon.\n" +
				"Pickup dry cleaning on the second Saturday at noon.\n" +
				"Pickup hockey team on the second-to-last Saturday at 9 AM.\n";
		String actualCalendar = FormatUtils.printableCalendar(calendar, ldtRequest);
		assertEquals(expectedCalendar, actualCalendar);
		
		ldtRequest = LocalDateTime.of(2017, 2, 9, 9, 5);
		expectedCalendar = 
				"Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.\n" + 
				"Pickup recycling every other Friday at 7:30 AM (on next Friday, February 17).\n" + 
				"Pickup lawn waste on the first at noon and on the fifteenth at noon.\n" +
				"Pickup scrap metal on the last day of the month at noon.\n" +
				"Pickup mortgage on the fifth day before the end of the month at noon.\n" +
				"Pickup dry cleaning on the second Saturday at noon.\n" +
				"Pickup hockey team on the second-to-last Saturday at 9 AM.\n";
		actualCalendar = FormatUtils.printableCalendar(calendar, ldtRequest);
		assertEquals(expectedCalendar, actualCalendar);
	}
	
	@Test
	public void testCalendarStorable() {
		Calendar calendar = new Calendar();
		calendar.initComplexExampleCalendar();

		String expectedCalendar = "BEGIN:VCALENDAR\n" + 
				"PRODID:-//Ben Fortuna//iCal4j 2.0.0//EN\n" + 
				"VERSION:2.0\n" + 
				"CALSCALE:GREGORIAN\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170131T073000\n" + 
				"DTEND:20170131T073000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170210T192058Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;BYDAY=TU\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170203T073000\n" + 
				"DTEND:20170203T073000\n" + 
				"SUMMARY:trash\n" + 
				"UID:20170210T192059Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;BYDAY=FR\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170203T073000\n" + 
				"DTEND:20170203T073000\n" + 
				"SUMMARY:recycling\n" + 
				"UID:20170210T192100Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=FR\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170201T120000\n" + 
				"DTEND:20170201T120000\n" + 
				"SUMMARY:lawn waste\n" + 
				"UID:20170210T192101Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY;BYMONTHDAY=1\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:20170210T192055Z\n" + 
				"DTSTART:20170215T120000\n" + 
				"DTEND:20170215T120000\n" + 
				"SUMMARY:lawn waste\n" + 
				"UID:20170210T192102Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY;BYMONTHDAY=15\n" + 
				"END:VEVENT\n" +
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:<ignore>\n" + 
				"DTSTART:20170215T120000\n" + 
				"DTEND:20170215T120000\n" + 
				"SUMMARY:scrap metal\n" + 
				"UID:20170210T192102Z-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY;BYMONTHDAY=-1\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:<ignore>\n" + 
				"DTSTART:20170215T120000\n" + 
				"DTEND:20170215T120000\n" + 
				"SUMMARY:mortgage\n" + 
				"UID:<ignore>-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY;BYMONTHDAY=-5\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:<ignore>\n" + 
				"DTSTART:20170215T120000\n" + 
				"DTEND:20170215T120000\n" + 
				"SUMMARY:dry cleaning\n" + 
				"UID:<ignore>-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY;BYDAY=2SA\n" + 
				"END:VEVENT\n" + 
				"BEGIN:VEVENT\n" + 
				"DTSTAMP:<ignore>\n" + 
				"DTSTART:20170215T090000\n" + 
				"DTEND:20170215T090000\n" + 
				"SUMMARY:hockey team\n" + 
				"UID:<ignore>-TrashDaySkill@AWS\n" + 
				"RRULE:FREQ=MONTHLY;BYDAY=-2SA\n" + 
				"END:VEVENT\n" + 
				"END:VCALENDAR\n";
		String actualCalendar = calendar.toStringRFC5545();
		assertEquals(ignoreTestTimestamps(expectedCalendar), ignoreTestTimestamps(actualCalendar));
	}
	
	@Test
	public void testCalendarVerbal() {
		LocalDateTime ldtRequest = LocalDateTime.of(2017,  2, 12, 9, 36);
		Calendar calendar = new Calendar();
		calendar.initComplexExampleCalendar();

		String expectedCalendar = "Pickup trash every Tuesday at 7 30 AM and every Friday at 7 30 AM. Pickup recycling every other Friday at 7 30 AM (on this Friday). Pickup lawn waste on the first at noon and on the fifteenth at noon. Pickup scrap metal on the last day of the month at noon. Pickup mortgage on the fifth day before the end of the month at noon. Pickup dry cleaning on the second Saturday at noon. Pickup hockey team on the second-to-last Saturday at 9 AM. ";
		String actualCalendar = FormatUtils.verbalCalendar(calendar, ldtRequest);
		assertEquals(expectedCalendar, actualCalendar);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testConversionFromSchedulePrintable() {
		LocalDateTime ldtRequest = LocalDateTime.now();
		/*
		 * Expected (as Schedule printed):
		 * Pickup trash on Tuesday at 6:30 AM and Friday at 6:30 AM.
		 * Pickup recycling on Friday at 6:30 AM.
		 * Pickup lawn waste on Wednesday at noon.
		 */
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		log.info("Schedule: {}", schedule.toStringPrintable());
		String actualScheduleString = schedule.toStringPrintable();
		
		/*
		 * Actual (as Calendar printed):
		 * Pickup trash every Tuesday at 6:30 AM and every Friday at 6:30 AM.
		 * Pickup recycling every Friday at 6:30 AM.
		 * Pickup lawn waste every Wednesday at noon.
		 */
		Calendar calendar = new Calendar(schedule);
		String actualCalendarString = FormatUtils.printableCalendar(calendar, ldtRequest);
		log.info("Calendar: {}", actualCalendarString);
		
		assertEquals(
				actualScheduleString
					.replaceAll(" on ", " ")
					.replaceAll("([A-Z][a-z]+day)", "every $1")
					, 
				actualCalendarString);
	}
	
	@SuppressWarnings("deprecation")
	@Test
	public void testConversionFromScheduleVerbal() {
		LocalDateTime ldtRequest = LocalDateTime.of(2017,  2, 12, 9, 35);
		/*
		 * Expected (as Schedule spoken):
		 * Pickup trash on Tuesday 6 30 AM and Friday 6 30 AM. Pickup recycling on Friday 6 30 AM. Pickup lawn waste on Wednesday noon. 
		 */
		Schedule schedule = new Schedule();
		schedule.initExampleSchedule();
		log.info("Schedule: {}", schedule.toStringVerbal());
		String actualScheduleString = schedule.toStringVerbal();
		
		/*
		 * Actual (as Calendar spoken):
		 * Pickup trash every Tuesday at 6 30 AM and every Friday at 6 30 AM. Pickup recycling every Friday at 6 30 AM. Pickup lawn waste every Wednesday at noon. 
		 */
		Calendar calendar = new Calendar(schedule);
		String actualCalendarString = FormatUtils.verbalCalendar(calendar, ldtRequest);
		log.info("Calendar: {}", actualCalendarString);
		
		assertEquals(
				actualScheduleString
					.replaceAll(" on ", " ")
					.replaceAll("([A-Z][a-z]+day)", "every $1 at")
					, 
				actualCalendarString);
	}

}
