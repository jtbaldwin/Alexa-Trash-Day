package trashday.storage;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.Session;

import trashday.model.Calendar;
import trashday.ui.FormatUtils;

public class SessionDaoTest {
    @Rule
    public ExpectedException thrown= ExpectedException.none();
    
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SessionDaoTest.class);
    /** Test Data */
    private static Session testSession;
    /** Test Data */
    private static SessionDao testSessionDao;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		Map<String, Object> attributes = new HashMap<String, Object>();
		testSession = Session.builder()
				.withAttributes(attributes)
				.withSessionId("TEST-SESSION-ID")
				.build();
		testSessionDao = new SessionDao(testSession);
	}

	@Before
	public void setUp() throws Exception {
	}
	
	@Test
	public void testCalendarAccessors() {
		log.trace("testCalendarAccessors");
		
		// Schedule does not yet exist in session.
		Calendar expectedCalendar = null;
		Calendar actualCalendar = testSessionDao.getCalendar();
		assertEquals(expectedCalendar, actualCalendar);
		
		// Set and Retrieve the schedule to the Session.
		LocalDateTime ldtRequest = LocalDateTime.now();
		expectedCalendar = new Calendar();
		expectedCalendar.initComplexExampleCalendar();
		testSessionDao.setCalendar(expectedCalendar);
		actualCalendar = testSessionDao.getCalendar();
		assertNotNull(actualCalendar);
		assertEquals(FormatUtils.printableCalendar(expectedCalendar, ldtRequest), FormatUtils.printableCalendar(actualCalendar, ldtRequest));
		
		// Clear the schedule from the Session.
		expectedCalendar = null;
		testSessionDao.clearCalendar();
		actualCalendar = testSessionDao.getCalendar();
		assertEquals(expectedCalendar, actualCalendar);
		
		// If Session is read from Alexa, it may have the schedule
		// data stored in some other object type instead of the
		// trashday.model.Schedule.  In this case, test how to handle if
		// it is a String.
		expectedCalendar = new Calendar();
		expectedCalendar.initComplexExampleCalendar();
		testSession.setAttribute(SessionDao.SESSION_ATTR_CALENDAR, expectedCalendar.toStringRFC5545());
		actualCalendar = testSessionDao.getCalendar();
		assertNotNull(actualCalendar);
		assertEquals(FormatUtils.printableCalendar(expectedCalendar, ldtRequest), FormatUtils.printableCalendar(actualCalendar, ldtRequest));

		// If Session is read from Alexa, it may have the session
		// data stored in some other object type instead of the
		// trashday.model.Calendar.  In this case, test how to handle if
		// an Object we don't know how to convert to Calendar.
		expectedCalendar = null;
		testSession.setAttribute(SessionDao.SESSION_ATTR_CALENDAR, DayOfWeek.MONDAY);
		actualCalendar = testSessionDao.getCalendar();
		assertEquals(expectedCalendar, actualCalendar);
	}

	@Test
	public void testTimeZoneAccessors() {		
		log.trace("testTimeZoneAccessors");
		
		// TimeZone does not yet exist in session.
		TimeZone expectedTimeZone = null;
		TimeZone actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// Set and Retrieve the time zone to the Session.
		expectedTimeZone = TimeZone.getTimeZone("US/Central");
		testSessionDao.setTimeZone(expectedTimeZone);
		actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// Clear the time zone from the Session.
		expectedTimeZone = null;
		testSessionDao.clearTimeZone();
		actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// If Session is read from Alexa, it may have the time zone
		// data stored in some other object type instead of the
		// java.util.TimeZone.  In this case, test how to handle if
		// it is a String.
		expectedTimeZone = TimeZone.getTimeZone("US/Central");
		testSession.setAttribute(SessionDao.SESSION_ATTR_TIMEZONE, "US/Central");
		actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);

		// If Session is read from Alexa, it may have the time zone
		// data stored in some other object type instead of the
		// java.util.TimeZone.  In this case, test how to handle if
		// an Object we don't know how to convert to TimeZone.
		expectedTimeZone = null;
		testSession.setAttribute(SessionDao.SESSION_ATTR_TIMEZONE, DayOfWeek.MONDAY);
		actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);		
	}

}
