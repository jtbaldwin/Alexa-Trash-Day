package trashday.model;

import static org.junit.Assert.assertEquals;
import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntentLogTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(IntentLogTest.class);
    /** A basic IntentLog with test data */
    private static IntentLog testIntentLog;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testIntentLog = new IntentLog();
		LocalDateTime ldtEntry = LocalDateTime.of(2016, 1, 1, 8, 52);
		testIntentLog.incrementIntent(ldtEntry, "intentNameA");
		
		ldtEntry = ldtEntry.minusWeeks(2);
		testIntentLog.incrementIntent(ldtEntry, "intentNameB");
		testIntentLog.incrementIntent(ldtEntry, "intentNameC");
	}

	@Test
	public void testIncrementIntentStringStringInteger() {		
		IntentLog intentLog = new IntentLog();
		String yearWeekString = "2016-01";
		intentLog.incrementIntent(yearWeekString, "intentNameA", 1);
		intentLog.incrementIntent(yearWeekString, "intentNameA", 4);
		intentLog.incrementIntent(yearWeekString, "intentNameB", 5);

		yearWeekString = "2016-02";
		intentLog.incrementIntent(yearWeekString, "intentNameA", 2);
		intentLog.incrementIntent(yearWeekString, "intentNameB", 5);
		intentLog.incrementIntent(yearWeekString, "intentNameB", 5);
		
		String expectedPrinted = "2016-01:\n" + 
				"  intentNameA=5, intentNameB=5\n" + 
				"2016-02:\n" + 
				"  intentNameA=2, intentNameB=10\n";
		String actualPrinted = intentLog.toStringPrintable();
		log.info("actualPrinted:\n{}",actualPrinted);
		assertEquals(expectedPrinted,actualPrinted);
	}

	@Test
	public void testIncrementIntentLocalDateTimeStringInteger() {
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtEntry = LocalDateTime.of(2016, 1, 1, 8, 52);
		intentLog.incrementIntent(ldtEntry, "intentNameA");
		intentLog.incrementIntent(ldtEntry, "intentNameA", 4);
		intentLog.incrementIntent(ldtEntry, "intentNameB", 5);

		ldtEntry = ldtEntry.plusWeeks(1);
		intentLog.incrementIntent(ldtEntry, "intentNameA", 2);
		intentLog.incrementIntent(ldtEntry, "intentNameB", 5);
		intentLog.incrementIntent(ldtEntry, "intentNameB", 5);
		
		String expectedPrinted = "2016-01:\n" + 
				"  intentNameA=5, intentNameB=5\n" + 
				"2016-02:\n" + 
				"  intentNameA=2, intentNameB=10\n";
		String actualPrinted = intentLog.toStringPrintable();
		log.info("actualPrinted:\n{}",actualPrinted);
		assertEquals(expectedPrinted,actualPrinted);
	}

	@Test
	public void testIncrementIntentLocalDateTimeString() {
		IntentLog intentLog = new IntentLog();
		LocalDateTime ldtEntry = LocalDateTime.of(2016, 1, 1, 8, 52);
		intentLog.incrementIntent(ldtEntry, "intentNameA");
		intentLog.incrementIntent(ldtEntry, "intentNameA");
		intentLog.incrementIntent(ldtEntry, "intentNameB");

		ldtEntry = ldtEntry.plusWeeks(1);
		intentLog.incrementIntent(ldtEntry, "intentNameA");
		intentLog.incrementIntent(ldtEntry, "intentNameB");
		intentLog.incrementIntent(ldtEntry, "intentNameB");
		
		String expectedPrinted = "2016-01:\n" + 
				"  intentNameA=2, intentNameB=1\n" + 
				"2016-02:\n" + 
				"  intentNameA=1, intentNameB=2\n";
		String actualPrinted = intentLog.toStringPrintable();
		log.info("actualPrinted:\n{}",actualPrinted);
		assertEquals(expectedPrinted,actualPrinted);
	}

	@Test
	public void testGetLog() {
		Map<String, Map<String, Integer>> logMap = testIntentLog.getLog();
		
		String expectedPrinted = "{2015-51={intentNameB=1, intentNameC=1}, 2016-01={intentNameA=1}}";
		String actualPrinted = logMap.toString();
		log.info("actualMap: {}", actualPrinted);
		
		assertEquals(expectedPrinted,actualPrinted);
	}

	@Test
	public void testJoin() {
		IntentLog bigIntentLog = new IntentLog();
		LocalDateTime ldtEntry = LocalDateTime.of(2016, 1, 1, 8, 52).plusWeeks(4);
		bigIntentLog.incrementIntent(ldtEntry, "intentNameD");
		log.debug("bigIntentLog:\n{}", bigIntentLog.toStringPrintable());
		
		bigIntentLog.join(testIntentLog);
		log.debug("bigIntentLog (joined):\n{}", bigIntentLog.toStringPrintable());
		
		String expectedPrinted="2015-51:\n" + 
				"  intentNameB=1, intentNameC=1\n" + 
				"2016-01:\n" + 
				"  intentNameA=1\n" + 
				"2016-05:\n" + 
				"  intentNameD=1\n";
		String actualPrinted=bigIntentLog.toStringPrintable();
		assertEquals(expectedPrinted,actualPrinted);
	}

	@Test
	public void testPrune() {
		IntentLog bigIntentLog = new IntentLog();
		
		LocalDateTime ldtEntry = LocalDateTime.of(2016, 1, 1, 8, 52);
		for (int i=1; i<=15; i++) {
			bigIntentLog.incrementIntent(ldtEntry, "intentNameA");
			ldtEntry=ldtEntry.plusWeeks(2);
		}
		log.info("big intent log:\n{}", bigIntentLog.toStringPrintable());
		
		bigIntentLog.prune(12);
		
		String expectedPrinted = "2016-07:\n" + 
				"  intentNameA=1\n" + 
				"2016-09:\n" + 
				"  intentNameA=1\n" + 
				"2016-11:\n" + 
				"  intentNameA=1\n" + 
				"2016-13:\n" + 
				"  intentNameA=1\n" + 
				"2016-15:\n" + 
				"  intentNameA=1\n" + 
				"2016-17:\n" + 
				"  intentNameA=1\n" + 
				"2016-19:\n" + 
				"  intentNameA=1\n" + 
				"2016-21:\n" + 
				"  intentNameA=1\n" + 
				"2016-23:\n" + 
				"  intentNameA=1\n" + 
				"2016-25:\n" + 
				"  intentNameA=1\n" + 
				"2016-27:\n" + 
				"  intentNameA=1\n" + 
				"2016-29:\n" + 
				"  intentNameA=1\n";		
		String actualPrinted = bigIntentLog.toStringPrintable();
		
		assertEquals(expectedPrinted,actualPrinted);
	}

	@Test
	public void testToJson() {
		String expectedJson = "{\"log\":{\"2015-51\":{\"intentNameB\":1,\"intentNameC\":1},\"2016-01\":{\"intentNameA\":1}},\"modelVersion\":\"1\"}";
		String actualJson = testIntentLog.toJson();
		log.info("actual JSON: {}", actualJson);
		assertJsonEquals(expectedJson, actualJson);
	}

	@Test
	public void testToStringPrintable() {
		String expectedPrinted = "2015-51:\n" + 
				"  intentNameB=1, intentNameC=1\n" + 
				"2016-01:\n" + 
				"  intentNameA=1\n" + 
				"";
		
		String actualPrinted = testIntentLog.toStringPrintable();
		log.debug("actual printed:\n{}", actualPrinted);
				
		assertEquals(expectedPrinted,actualPrinted);
	}

}
