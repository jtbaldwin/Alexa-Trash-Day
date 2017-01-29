package alexatesting;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit tests for {@link TestDataRequest} class.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#response-format">Alexa Skills Kit Docs: JSON Interface Reference for Custom Skills</a>
 */
@RunWith(JUnit4.class)
public class DataRequestTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DataRequestTest.class);
    
    /** Example of an actual {@link com.amazon.speech.speechlet.SpeechletRequest.IntentRequest} when converted to JSON and formatted. */
	private static final String testRequestBasicExample = 
			"{\n"+
			"  \"session\": {\n"+
			"    \"sessionId\": \"SessionId.d9e67a5b-9380-471d-bb17-01fcf6efe006\",\n"+
			"    \"application\": {\n"+
			"      \"applicationId\": \"TEST-AMAZON-APPLICATION-ID\"\n"+
			"    },\n"+
			"    \"attributes\": {},\n"+
			"    \"user\": {\n"+
			"      \"userId\": \"TEST-USER-ID\"\n"+
			"    },\n"+
			"    \"new\": true\n"+
			"  },\n"+
			"  \"request\": {\n"+
			"    \"type\": \"IntentRequest\",\n"+
			"    \"requestId\": \"EdwRequestId.e58f0b97-8cfc-4165-8377-1f3071a1431b\",\n"+
			"    \"locale\": \"en-US\",\n"+
			"    \"timestamp\": \"2016-11-24T15:27:15Z\",\n"+   // Thursday
			"    \"intent\": {\n"+
			"      \"name\": \"TellNextPickupIntent\",\n"+
			"      \"slots\": {}\n"+
			"    }\n"+
			"  },\n"+
			"  \"version\": \"1.0\"\n"+
			"}";

    /** Example of an actual {@link com.amazon.speech.speechlet.SpeechletRequest.IntentRequest} when converted to JSON and formatted. */
	private static final String testRequestComplexExample =
			"{\n"+
			"  \"session\": {\n"+
			"    \"sessionId\": \"SessionId.ceaf16e6-3566-4a1a-b2d4-620422f3fda5\",\n"+
			"    \"application\": {\n"+
			"      \"applicationId\": \"TEST-AMAZON-APPLICATION-ID\"\n"+
			"    },\n"+
			"    \"attributes\": {},\n"+
			"    \"user\": {\n"+
			"      \"userId\": \"TEST-USER-ID\"\n"+
			"    },\n"+
			"    \"new\": true\n"+
			"  },\n"+
			"  \"request\": {\n"+
			"    \"type\": \"IntentRequest\",\n"+
			"    \"requestId\": \"EdwRequestId.60c03ee3-82b9-404e-afed-7887bb1f7ff2\",\n"+
			"    \"locale\": \"en-US\",\n"+
			"    \"timestamp\": \"2016-11-29T01:46:34Z\",\n"+
			"    \"intent\": {\n"+
			"      \"name\": \"AddScheduleIntent\",\n"+
			"      \"slots\": {\n"+
			"        \"DayOfWeek\": {\n"+
			"          \"name\": \"DayOfWeek\",\n"+
			"          \"value\": \"Monday\"\n"+
			"        },\n"+
			"        \"TimeOfDay\": {\n"+
			"          \"name\": \"TimeOfDay\",\n"+
			"          \"value\": \"16:00\"\n"+
			"        },\n"+
			"        \"PickupName\": {\n"+
			"          \"name\": \"PickupName\",\n"+
			"          \"value\": \"mail\"\n"+
			"        }\n"+
			"      }\n"+
			"    }\n"+
			"  },\n"+
			"  \"version\": \"1.0\"\n"+
			"}";
	
	/**
	 * JUnit test to confirm that a {@link TestDataRequest} object,
	 * when configured correctly and converted to JSON, will
	 * successfully match an actual JSON string generated from a
	 * {@link com.amazon.speech.speechlet.SpeechletRequest}
	 */
	@Test
	public void testTestDataBasicRequest() {
		log.info("testTestDataBasicRequest");
		String expectedJson = testRequestBasicExample;
		log.debug("testTestDataBasicRequest expected={}", expectedJson);
		
		TestDataRequest testRequest = new TestDataRequest();
		String actualJson = testRequest.toString();
		log.debug("testTestDataBasicRequest actual={}", actualJson);
		
		assertEquals(expectedJson, actualJson);
	}

	/**
	 * JUnit test to confirm that a {@link TestDataRequest} object,
	 * when configured correctly and converted to JSON, will
	 * successfully match an actual JSON string generated from a
	 * {@link com.amazon.speech.speechlet.SpeechletRequest}
	 */
	@Test
	public void testTestDataComplexRequest() {
		log.info("testTestDataComplexRequest");
		String expectedJson = testRequestComplexExample;
		log.info("testTestDataComplexRequest expected={}", expectedJson);
		
		TestDataRequest testRequest = new TestDataRequest();
		testRequest.setSessionSessionId("SessionId.ceaf16e6-3566-4a1a-b2d4-620422f3fda5");
		testRequest.setSessionUserId("TEST-USER-ID");
		testRequest.setRequestIntentName("AddScheduleIntent");
		testRequest.setRequestRequestId("EdwRequestId.60c03ee3-82b9-404e-afed-7887bb1f7ff2");
		testRequest.setRequestTimestamp("2016-11-29T01:46:34Z");
		testRequest.addRequestIntentSlot("DayOfWeek","Monday");
		testRequest.addRequestIntentSlot("TimeOfDay","16:00");
		testRequest.addRequestIntentSlot("PickupName","mail");
		String testDataRequestFormatted = testRequest.toString();
		log.debug("testTestDataComplexRequest testDataRequest={}", testDataRequestFormatted);
		
		String actualJson = testRequest.toString();
		log.info("testTestDataComplexRequest actual={}", actualJson);
		
		assertEquals(expectedJson, actualJson);
	}

}
