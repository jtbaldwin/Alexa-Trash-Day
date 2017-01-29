package trashday.storage;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
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

import trashday.model.Schedule;

public class SessionDaoTest {
    @Rule
    public ExpectedException thrown= ExpectedException.none();
    
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SessionDaoTest.class);
    /** Test Data */
    private static Session testSession;
    /** Test Data */
    private static SessionDao testSessionDao;
    /** {@link Session} attribute key to store user's {@link trashday.model.Schedule} */
	public static final String SESSION_ATTR_SCHEDULE = "trashDaySchedule";
    /** {@link Session} attribute key to store user's {@link java.util.TimeZone} */
	public static final String SESSION_ATTR_TIMEZONE = "trashDayTimeZone";
    ///** {@link com.amazon.speech.speechlet.Session Session}  attribute key to store intent names that require a Yes/No confirmation */
	//private static final String SESSION_ATTR_CONFIRM_INTENT = "trashDayConfirmIntent";
    ///** {@link com.amazon.speech.speechlet.Session Session} attribute key to store user text description of actions that require a Yes/No confirmation */
	//private static final String SESSION_ATTR_CONFIRM_DESC = "trashDayConfirmAction";

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
	public void testScheduleAccessors() {		
		log.trace("testScheduleAccessors");
		
		// Schedule does not yet exist in session.
		Schedule expectedSchedule = null;
		Schedule actualSchedule = testSessionDao.getSchedule();
		assertEquals(expectedSchedule, actualSchedule);
		
		// Set and Retrieve the schedule to the Session.
		expectedSchedule = new Schedule();
		expectedSchedule.initExampleSchedule();
		testSessionDao.setSchedule(expectedSchedule);
		actualSchedule = testSessionDao.getSchedule();
		assertNotNull(actualSchedule);
		assertEquals(expectedSchedule.toStringPrintable(), actualSchedule.toStringPrintable());
		
		// Clear the schedule from the Session.
		expectedSchedule = null;
		testSessionDao.clearSchedule();
		actualSchedule = testSessionDao.getSchedule();
		assertEquals(expectedSchedule, actualSchedule);
		
		// If Session is read from Alexa, it may have the schedule
		// data stored in some other object type instead of the
		// trashday.model.Schedule.  In this case, test how to handle if
		// it is a String.
		expectedSchedule = new Schedule();
		expectedSchedule.initExampleSchedule();
		testSession.setAttribute(SESSION_ATTR_SCHEDULE, expectedSchedule.toJson());
		actualSchedule = testSessionDao.getSchedule();
		assertNotNull(actualSchedule);
		assertEquals(expectedSchedule.toStringPrintable(), actualSchedule.toStringPrintable());

		// If Session is read from Alexa, it may have the session
		// data stored in some other object type instead of the
		// trashday.model.Schedule.  In this case, test how to handle if
		// an Object we don't know how to convert to Schedule.
		expectedSchedule = null;
		testSession.setAttribute(SESSION_ATTR_SCHEDULE, DayOfWeek.MONDAY);
		actualSchedule = testSessionDao.getSchedule();
		assertEquals(expectedSchedule, actualSchedule);
		
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
		testSession.setAttribute(SESSION_ATTR_TIMEZONE, "US/Central");
		actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);

		// If Session is read from Alexa, it may have the time zone
		// data stored in some other object type instead of the
		// java.util.TimeZone.  In this case, test how to handle if
		// an Object we don't know how to convert to TimeZone.
		expectedTimeZone = null;
		testSession.setAttribute(SESSION_ATTR_TIMEZONE, DayOfWeek.MONDAY);
		actualTimeZone = testSessionDao.getTimeZone();
		assertEquals(expectedTimeZone, actualTimeZone);		
	}

}
