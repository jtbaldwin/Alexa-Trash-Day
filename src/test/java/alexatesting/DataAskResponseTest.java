package alexatesting;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit tests for {@link TestDataAskResponse} class.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#response-format">Alexa Skills Kit Docs: JSON Interface Reference for Custom Skills</a>
 */
@RunWith(JUnit4.class)
public class DataAskResponseTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DataAskResponseTest.class);
    
    /** Example of an actual {@link com.amazon.speech.speechlet.SpeechletResponse#newAskResponse(com.amazon.speech.ui.OutputSpeech, com.amazon.speech.ui.Reprompt)} when converted to JSON and formatted. */
	private static final String testAskResponseBasicExample = 
			"{\n"+
			"  \"version\": \"1.0\",\n"+
			"  \"response\": {\n"+
			"    \"outputSpeech\": {\n"+
			"      \"type\": \"PlainText\",\n"+
			"      \"text\": \"Next trash pickup is tomorrow at 06:30.\\nNext recycling pickup is tomorrow at 06:30.\\nNext lawn waste pickup is WEDNESDAY at 12:00.\\n\"\n"+
			"    },\n"+
			"    \"card\": {\n"+
			"      \"type\": \"Simple\",\n"+
			"      \"title\": \"Trash Day\",\n"+
			"      \"content\": \"Next Pickup Times:\\nNext trash pickup is tomorrow at 06:30.\\nNext recycling pickup is tomorrow at 06:30.\\nNext lawn waste pickup is WEDNESDAY at 12:00.\\n\"\n"+
			"    },\n"+
		    "    \"reprompt\": {\n"+
			"      \"outputSpeech\": {\n"+
			"        \"type\": \"PlainText\",\n"+
			"        \"text\": \"You can ask when is the next pickup, tell me the pickup schedule, add trash pickup on Wednesday at 7 am.\"\n"+
			"      }\n"+
			"    },\n"+
			"    \"shouldEndSession\": false\n"+
			"  },\n"+
			"  \"sessionAttributes\": {}\n"+
			"}";
	
	/**
	 * JUnit test to confirm that a {@link TestDataAskResponse} object,
	 * when configured correctly and converted to JSON, will
	 * successfully match an actual JSON string generated from a
	 * {@link com.amazon.speech.speechlet.SpeechletResponse#newAskResponse(com.amazon.speech.ui.OutputSpeech, com.amazon.speech.ui.Reprompt)}
	 */
	@Test
	public void testTestDataAskResponse() {
		log.info("testTestDataAskResponse");
		String expectedJson = testAskResponseBasicExample;
		log.debug("testTestDataAskResponse expected={}", expectedJson);
		
		TestDataAskResponse testResponse = new TestDataAskResponse();
		String actualJson = testResponse.toString();
		log.debug("testTestDataAskResponse actual={}", actualJson);
		
		assertEquals(expectedJson, actualJson);
	}

}
