package alexatesting;

import java.util.TimeZone;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import trashday.ui.responses.Phrases;

// The default Response data:
//"{\n"+
//"  \"version\": \"1.0\",\n"+
//"  \"response\": {\n"+
//"    \"outputSpeech\": {\n"+
//"      \"type\": \"PlainText\",\n"+
//"      \"text\": \"Next trash pickup is tomorrow at 06:30.\\nNext recycling pickup is tomorrow at 06:30.\\nNext lawn waste pickup is WEDNESDAY at 12:00.\\n\"\n"+
//"    },\n"+
//"    \"card\": {\n"+
//"      \"type\": \"Simple\",\n"+
//"      \"title\": \"Trash Day\",\n"+
//"      \"content\": \"Next Pickup Times:\\nNext trash pickup is tomorrow at 06:30.\\nNext recycling pickup is tomorrow at 06:30.\\nNext lawn waste pickup is WEDNESDAY at 12:00.\\n\"\n"+
//"    },\n"+
//"    \"reprompt\": {\n"+
//"      \"outputSpeech\": {\n"+
//"        \"type\": \"PlainText\",\n"+
//"        \"text\": \"You can ask when is the next pickup, tell me the pickup schedule, add trash pickup on Wednesday at 7 am.\"\n"+
//"      }\n"+
//"    },\n"+
//"    \"shouldEndSession\": false\n"+
//"  },\n"+
//"  \"sessionAttributes\": {}\n"+
//"}";

/**
 * For JUnit testing, create JSON responses that we expect will match
 * to the actual JSON output from our application.
 * <p>
 * The Alexa service exchanges JSON data with our application
 * that conforms to the structure in "JSON Interface Reference
 * for Custom Skills."  For JUnit testing, we create conforming
 * JSON requests, send them through the application's stream
 * handler ({@link trashday.TrashDaySpeechletRequestStreamHandler}),
 * and read the JSON response output.  Tests are successful if
 * the actual application JSON output matches an expected JSON
 * response.
 * <p>
 * This class creates JSON responses that we expect will match
 * to the actual JSON output from our application.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#response-format">Alexa Skills Kit Docs: JSON Interface Reference for Custom Skills</a>
 *
 */
public class TestDataAskResponse {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TestDataAskResponse.class);
    /** Represent our test data as JsonObject */
	public JsonObject jo;
	
	/**
	 * Create a Response object with basic data fields filled.
	 */
	public TestDataAskResponse() {
		log.trace("TestDataAskResponse()");
		jo = new JsonObject();
		
		jo.add("version", new JsonPrimitive("1.0"));
		
		JsonObject response = new JsonObject();
		
		JsonObject outputSpeech = new JsonObject();
		outputSpeech.add("type", new JsonPrimitive("PlainText"));
		outputSpeech.add("text", new JsonPrimitive("Next trash pickup is tomorrow at 06:30.\nNext recycling pickup is tomorrow at 06:30.\nNext lawn waste pickup is WEDNESDAY at 12:00.\n"));	
		response.add("outputSpeech", outputSpeech);
		
		JsonObject card = new JsonObject();
		card.add("type", new JsonPrimitive("Simple"));
		card.add("title", new JsonPrimitive("Trash Day"));
		card.add("content", new JsonPrimitive("Next Pickup Times:\nNext trash pickup is tomorrow at 06:30.\nNext recycling pickup is tomorrow at 06:30.\nNext lawn waste pickup is WEDNESDAY at 12:00.\n"));
		response.add("card", card);
		
		JsonObject reprompt = new JsonObject();
		JsonObject repromptOutputSpeech = new JsonObject();
		repromptOutputSpeech.add("type", new JsonPrimitive("PlainText"));
		repromptOutputSpeech.add("text", new JsonPrimitive("You can ask when is the next pickup, tell me the pickup schedule, add trash pickup on Wednesday at 7 am."));
		reprompt.add("outputSpeech", repromptOutputSpeech);
		response.add("reprompt", reprompt);
		
		response.add("shouldEndSession", new JsonPrimitive(false));
		
		jo.add("response", response);
		
		JsonObject sessionAttributes = new JsonObject();
		jo.add("sessionAttributes", sessionAttributes);
	}
	
	/**
	 * Getter for the Response JsonObject
	 * 
	 * @return The Response object
	 */
	public JsonObject getJo() {
		log.trace("getJo()");
		return jo;
	}

	/**
	 * Helper function to replace member attribute values (without perturbing
	 * attribute order) or add the attribute if it doesn't already exist.
	 * 
	 * @param jo JsonObject that is getting modified
	 * @param key String the attribute name
	 * @param joValue JsonObject that is the new attribute value
	 */
	private void setAttribute(JsonObject jo, String key, JsonObject joValue) {
		log.trace("setAttribute({}, {}, {})", jo, key, joValue);
		Boolean changed = false;
		for (Entry<String, JsonElement> member: jo.entrySet()) {
			if (member.getKey().equals(key)) {
				member.setValue(joValue);
				changed = true;
			}
		}
		if (! changed) {
			jo.add(key, joValue);
		}
	}
	
	/**
	 * Helper function to replace member attribute values (without perturbing
	 * attribute order) or add the attribute if it doesn't already exist.
	 * 
	 * @param jo JsonObject that is getting modified
	 * @param key String the attribute name
	 * @param value boolean that is the new attribute value
	 */
	private void setAttribute(JsonObject jo, String key, boolean value) {
		log.trace("setAttribute({}, {}, {})", jo, key, value);
		Boolean changed = false;
		for (Entry<String, JsonElement> member: jo.entrySet()) {
			if (member.getKey().equals(key)) {
				member.setValue(new JsonPrimitive(value));
				changed = true;
			}
		}
		if (! changed) {
			jo.add(key, new JsonPrimitive(value));
		}
	}
	
	/**
	 * Helper function to replace member attribute values (without perturbing
	 * attribute order) or add the attribute if it doesn't already exist.
	 * 
	 * @param jo JsonObject that is getting modified
	 * @param key String the attribute name
	 * @param joValue String that is the new attribute value
	 */
	private void setAttribute(JsonObject jo, String key, String value) {
		log.trace("setAttribute({}, {}, {})", jo, key, value);
		Boolean changed = false;
		for (Entry<String, JsonElement> member: jo.entrySet()) {
			if (member.getKey().equals(key)) {
				member.setValue(new JsonPrimitive(value));
				changed = true;
			}
		}
		if (! changed) {
			jo.add(key, new JsonPrimitive(value));
		}
	}
	
	/**
	 * Setter for the response-outputSpeech-type attribute
	 * 
	 * @param newType String new attribute value
	 */
	public void setResponseOutputSpeechType(String newType) {
		log.trace("setResponseOutputSpeechText({})", newType);
		JsonObject response = jo.get("response").getAsJsonObject();
		JsonObject outputSpeech = response.get("outputSpeech").getAsJsonObject();
		setAttribute(outputSpeech, "type", newType);
	}
	
	/**
	 * Setter for the response-outputSpeech-text attribute
	 * 
	 * @param newText String new attribute value
	 */
	public void setResponseOutputSpeechText(String newText) {
		log.trace("setResponseOutputSpeechText({})", newText);
		JsonObject response = jo.get("response").getAsJsonObject();
		JsonObject outputSpeech = response.get("outputSpeech").getAsJsonObject();
		setAttribute(outputSpeech, "text", newText);
		outputSpeech.remove("ssml");
		setResponseOutputSpeechType("PlainText");
	}
	
	public void setResponseOutputSpeechText(Phrases phrase) {
		setResponseOutputSpeechText(phrase.toString());
	}
	
	/**
	 * Setter for the response-outputSpeech-ssml attribute
	 * 
	 * @param newSSML String new attribute value
	 */
	public void setResponseOutputSpeechSSML(String newSSML) {
		log.trace("setResponseOutputSpeechSSML({})", newSSML);
		JsonObject response = jo.get("response").getAsJsonObject();
		JsonObject outputSpeech = response.get("outputSpeech").getAsJsonObject();
		setAttribute(outputSpeech, "ssml", newSSML);
		outputSpeech.remove("text");
		setResponseOutputSpeechType("SSML");
	}
	
	/**
	 * Setter for the response-reprompt-outputSpeech-text attribute
	 * 
	 * @param newText String new attribute value
	 */
	public void setResponseRepromptOutputSpeechText(String newText) {
		log.trace("setResponseRepromptOutputSpeechText({})", newText);
		JsonObject response = jo.get("response").getAsJsonObject();
		JsonObject reprompt = response.get("reprompt").getAsJsonObject();
		JsonObject outputSpeech = reprompt.get("outputSpeech").getAsJsonObject();
		setAttribute(outputSpeech, "text", newText);
	}
	
	public void setResponseRepromptOutputSpeechText(Phrases phrase) {
		setResponseRepromptOutputSpeechText(phrase.toString());
	}
	
	/**
	 * Setter for the response-card-title attribute
	 * 
	 * @param newText String new attribute value
	 */
	public void setResponseCardTitle(String newText) {
		log.trace("setResponseCardTitle({})", newText);
		JsonObject response = jo.get("response").getAsJsonObject();
		JsonObject card = response.get("card").getAsJsonObject();
		setAttribute(card, "title", newText);
	}
	
	/**
	 * Setter for the response-card-content attribute
	 * 
	 * @param newText String new attribute value
	 */
	public void setResponseCardContent(String newText) {
		log.trace("setResponseCardContent({})", newText);
		JsonObject response = jo.get("response").getAsJsonObject();
		JsonObject card = response.get("card").getAsJsonObject();
		setAttribute(card, "content", newText);
	}
	
	public void setResponseCardContent(Phrases phrase) {
		setResponseCardContent(phrase.toString());
	}
	
	/**
	 * Setter for the sessionAttributes attribute
	 * 
	 * @param attributeName String new attribute name
	 * @param joAttributeValue JsonObject new attribute value
	 */ 
	public void setSessionAttribute(String attributeName, JsonObject joAttributeValue) {
		log.trace("setSessionAttribute({}, {})", attributeName, joAttributeValue.toString());
		JsonObject response = jo.get("sessionAttributes").getAsJsonObject();
		setAttribute(response, attributeName, joAttributeValue);		
	}
	
	/**
	 * Setter for the sessionAttributes attribute
	 * 
	 * @param attributeName String new attribute name
	 * @param value boolean new attribute value
	 */ 
	public void setSessionAttribute(String attributeName, boolean value) {
		log.trace("setSessionAttribute({}, {})", attributeName, value);
		JsonObject response = jo.get("sessionAttributes").getAsJsonObject();
		setAttribute(response, attributeName, value);		
	}
	
	/**
	 * Setter for the sessionAttributes attribute
	 * 
	 * @param attributeName String new attribute name
	 * @param attributeValue String new attribute value
	 */
	public void setSessionAttribute(String attributeName, String attributeValue) {
		log.trace("setSessionAttribute({}, {})", attributeName, attributeValue);
		JsonObject response = jo.get("sessionAttributes").getAsJsonObject();
		setAttribute(response, attributeName, attributeValue);
	}
	
	public void setSessionAttribute(String attributeName, TimeZone timeZone) {
		log.trace("setSessionAttribute({},{})", attributeName, timeZone.getID());
		JsonObject sessionAttributes;
		
		if (jo.has("sessionAttributes")) {
			sessionAttributes = jo.get("sessionAttributes").getAsJsonObject();
			setAttribute(sessionAttributes, attributeName, timeZone.getID());
		} else {
			sessionAttributes = new JsonObject();
			setAttribute(sessionAttributes, attributeName, timeZone.getID());
			jo.add("sessionAttributes", sessionAttributes);
		}
	}
	
	/**
	 * Setter to remove the response-card
	 * 
	 * @return The removed card
	 */
	public JsonElement deleteResponseCard() {
		log.trace("deleteResponseCard()");
		JsonObject response = jo.get("response").getAsJsonObject();
		if (response==null) {
			return null;
		}
		return response.remove("card");
	}
	
	/**
	 * Getter for a pretty-formatted String representation
	 * 
	 * @return Formatted JSON representation
	 */
	public String toString() {
		log.trace("toString()");
	    Gson gson = new GsonBuilder().setPrettyPrinting().create();	    
		String actualJson = gson.toJson(jo);
		return actualJson;
	}
}
