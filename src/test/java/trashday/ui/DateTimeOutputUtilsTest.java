package trashday.ui;

import static org.junit.Assert.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DateTimeOutputUtilsTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testGetRequestLocalDateTimeDateTimeZone() {
		Date requestDate = null;
		TimeZone timeZone = null;
		LocalDateTime ldtExpected = null;
		LocalDateTime ldtActual = DateTimeOutputUtils.getRequestLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
		
		requestDate = new Date();
		ldtActual = DateTimeOutputUtils.getRequestLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
		
		requestDate = null;
		timeZone = TimeZone.getTimeZone("US/Central");
		ldtActual = DateTimeOutputUtils.getRequestLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
		
		Calendar c = Calendar.getInstance();
		Instant instant = c.toInstant().truncatedTo(ChronoUnit.MINUTES);
		requestDate = Date.from(instant);
		ldtExpected = LocalDateTime.ofInstant(instant, timeZone.toZoneId());
		ldtActual = DateTimeOutputUtils.getRequestLocalDateTime(requestDate, timeZone);
		assertEquals(ldtExpected, ldtActual);
	}

}
