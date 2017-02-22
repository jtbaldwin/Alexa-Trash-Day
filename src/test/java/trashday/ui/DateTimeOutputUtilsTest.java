package trashday.ui;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import trashday.model.DateTimeUtils;

public class DateTimeOutputUtilsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testPrintableDayOfMonth() {
		String s = null;
		try {
			s = FormatUtils.printableDayOfMonth(-32);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum day of month value (-31) exceeded: -32", ex.getMessage());
		}
		assertNull(s);
		
		try {
			s = FormatUtils.printableDayOfMonth(0);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No such day of month: 0", ex.getMessage());
		}
		assertNull(s);

		try {
			s = FormatUtils.printableDayOfMonth(32);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum day of month value (31) exceeded: 32", ex.getMessage());
		}
		assertNull(s);
	}

	@Test
	public void testVerbalDayOfMonthException() {
		String s = null;
		try {
			s = FormatUtils.verbalDayOfMonth(-32);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Minimum day of month value (-31) exceeded: -32", ex.getMessage());
		}
		assertNull(s);
		
		try {
			s = FormatUtils.verbalDayOfMonth(0);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("No such day of month: 0", ex.getMessage());
		}
		assertNull(s);

		try {
			s = FormatUtils.verbalDayOfMonth(32);
			fail("Did not receive expected exception.");
		} catch (IllegalArgumentException ex) {
			assertEquals("Maximum day of month value (31) exceeded: 32", ex.getMessage());
		}
		assertNull(s);
	}

	@Test
	public void testGetRequestLocalDateTimeDateTimeZone() {
		Date requestDate = null;
		TimeZone timeZone = null;
		LocalDateTime ldtExpected = null;
		LocalDateTime ldtActual = DateTimeUtils.getLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
		
		requestDate = new Date();
		ldtActual = DateTimeUtils.getLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
		
		requestDate = null;
		timeZone = TimeZone.getTimeZone("US/Central");
		ldtActual = DateTimeUtils.getLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
		
		Calendar c = Calendar.getInstance();
		Instant instant = c.toInstant().truncatedTo(ChronoUnit.MINUTES);
		requestDate = Date.from(instant);
		ldtExpected = LocalDateTime.ofInstant(instant, timeZone.toZoneId());
		ldtActual = DateTimeUtils.getLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
	}

	@Test
	public void testPrintableDateAndTime() {
		java.util.TimeZone tz = TimeZone.getTimeZone("US/Eastern");
		ZonedDateTime zdtEvent = ZonedDateTime.of(2017, 2, 15, 20, 33, 00, 00, tz.toZoneId());
		assertEquals("Wednesday, February 15 at 8:33 PM", FormatUtils.printableDateAndTime(zdtEvent));
	}
}
