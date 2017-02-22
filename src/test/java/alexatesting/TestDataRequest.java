package alexatesting;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import trashday.TrashDaySpeechletRequestStreamHandlerTest;

//The default Request data:
//"{\n"+
//"  \"session\": {\n"+
//"    \"sessionId\": \"SessionId.d9e67a5b-9380-471d-bb17-01fcf6efe006\",\n"+
//"    \"application\": {\n"+
//"      \"applicationId\": \"TEST-AMAZON-APPLICATION-ID\"\n"+
//"    },\n"+
//"    \"attributes\": {},\n"+
//"    \"user\": {\n"+
//"      \"userId\": \"TEST-USER-ID\"\n"+
//"    },\n"+
//"    \"new\": true\n"+
//"  },\n"+
//"  \"request\": {\n"+
//"    \"type\": \"IntentRequest\",\n"+
//"    \"requestId\": \"EdwRequestId.e58f0b97-8cfc-4165-8377-1f3071a1431b\",\n"+
//"    \"locale\": \"en-US\",\n"+
//"    \"timestamp\": \"2016-11-24T15:27:15Z\",\n"+   // Thursday
//"    \"intent\": {\n"+
//"      \"name\": \"TellNextPickupIntent\",\n"+
//"      \"slots\": {}\n"+
//"    }\n"+
//"  },\n"+
//"  \"version\": \"1.0\"\n"+
//"}\n";

/**
 * For JUnit testing, create JSON requests (conforming to
 * what our application sees from the Alexa service) to use
 * as test input for our application.
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
 * This class creates JSON requests (conforming to
 * what our application sees from the Alexa service) to use
 * as test input for our application testing.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#response-format">Alexa Skills Kit Docs: JSON Interface Reference for Custom Skills</a>
 *
 */
public class TestDataRequest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TrashDaySpeechletRequestStreamHandlerTest.class);
    /** Represent our test data as JsonObject */
	public JsonObject jo;
	/** Date used in this test request */
	Date requestDate;
	
	/**
	 * Create a Request object with basic data fields filled.
	 * 
	 * @param userId String fills the userId field in the Request
	 * @param tdu TestDataUtterance fills the intent name and slow information in the Request
	 */
	public TestDataRequest(String userId, TestDataUtterance tdu) {
		log.trace("TestDataRequest({}, {})", userId, tdu);
		makeRequest(userId, tdu);
	}
	
	/**
	 * Create a Request object with basic data fields filled.
	 * 
	 * @param userId String fills the userId field in the Request
	 */
	public TestDataRequest(String userId) {
		log.trace("TestDataRequest({})", userId);
		makeRequest(userId, null);		
	}
	
	/**
	 * Create a Request object with basic data fields filled.
	 * 
	 * @param userId String fills the userId field in the Request
	 * @param slots Slot information to be added to the Request
	 */
	public TestDataRequest(String userId, List<Map<String, String>> slots) {
		log.trace("TestDataRequest({})", userId);
		makeRequest(userId, null);
		for ( Map<String, String> slotInfo : slots) {
			addRequestIntentSlot(slotInfo.get("name"), slotInfo.get("value"));
		}
	}
	
	/**
	 * Create a Request object with basic data fields filled.
	 */
	public TestDataRequest() {
		log.trace("TestDataRequest()");
		makeRequest(null, null);
	}
	
	/**
	 * Helper function to make a basic Request, replacing the appropriate
	 * userId, intent name, and slot values if available.
	 * 
	 * @param userId String fills the userId field in the Request
	 * @param tdu TestDataUtterance fills the intent name and slow information in the Request
	 */
	private void makeRequest(String userId, TestDataUtterance tdu) {
		jo = new JsonObject();
		
		// Set requestDate to default: 2016-11-24T15:27:15Z
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.set(2016, 11-1, 24, 15, 27, 15);
		requestDate = c.getTime();
		
		JsonObject session = new JsonObject();		
		session.add("sessionId", new JsonPrimitive("SessionId.d9e67a5b-9380-471d-bb17-01fcf6efe006"));
		JsonObject application = new JsonObject();
		application.add("applicationId", new JsonPrimitive("TEST-AMAZON-APPLICATION-ID"));
		session.add("application", application);
		JsonObject attributes = new JsonObject();
		session.add("attributes", attributes);
		JsonObject user = new JsonObject();
		if (userId==null) {
			user.add("userId", new JsonPrimitive("TEST-USER-ID"));
		} else {
			user.add("userId", new JsonPrimitive(userId));			
		}
		session.add("user", user);
		session.add("new", new JsonPrimitive(true));
		jo.add("session", session);
		
		JsonObject request = new JsonObject();
		request.add("type", new JsonPrimitive("IntentRequest"));
		request.add("requestId", new JsonPrimitive("EdwRequestId.e58f0b97-8cfc-4165-8377-1f3071a1431b"));
		request.add("locale", new JsonPrimitive("en-US"));
		Instant requestInstant = requestDate.toInstant();
		request.add("timestamp", new JsonPrimitive(DateTimeFormatter.ISO_INSTANT.format(requestInstant.truncatedTo(ChronoUnit.SECONDS))));
		JsonObject intent = new JsonObject();
		if (tdu == null) {
			intent.add("name", new JsonPrimitive("TellNextPickupIntent"));
		} else {
			String intentName = tdu.getIntentName();
			intent.add("name", new JsonPrimitive(intentName));
		}
		JsonObject joSlots = new JsonObject();
		if (tdu != null) {
			Map<String,String> slots = tdu.getSlots();
			if (slots != null) {
				for (Map.Entry<String, String> entry: slots.entrySet()) {
					String name = entry.getKey();
					String value = entry.getValue();
					
					JsonObject slot = new JsonObject();
					slot.add("name", new JsonPrimitive(name));	
					if (value==null) {
						slot.add("value", (JsonPrimitive) null);
					} else {
						slot.add("value", new JsonPrimitive(value));	
					}
					joSlots.add(name, slot);
				}
			}
		}
		intent.add("slots", joSlots);
		request.add("intent", intent);
		jo.add("request", request);
		
		jo.add("version", new JsonPrimitive("1.0"));
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
	private void setAttribute(JsonObject jo, String key, JsonPrimitive value) {
		log.trace("setAttribute({}, {}, {})", jo, key, value);
		Boolean changed = false;
		for (Entry<String, JsonElement> member: jo.entrySet()) {
			if (member.getKey().equals(key)) {
				member.setValue(value);
				changed = true;
			}
		}
		if (! changed) {
			jo.add(key, value);
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
	 * Setter for the request-intent-name attribute
	 * 
	 * @param newName String new attribute value
	 */
	public void setRequestIntentName(String newName) {
		log.trace("setRequestIntentName({})", newName);
		JsonObject request = jo.get("request").getAsJsonObject();
		JsonObject intent = request.get("intent").getAsJsonObject();
		setAttribute(intent, "name", newName);
	}
	
	/**
	 * Setter for the request-reason attribute
	 * 
	 * @param newReason String new attribute value
	 */
	public void setRequestReason(String newReason) {
		log.trace("addRequestReason({})", newReason);
		JsonObject request = jo.get("request").getAsJsonObject();
		setAttribute(request, "reason", newReason);
	}
	
	/**
	 * Setter for the request-type attribute
	 * 
	 * @param newRequestType String new attribute value
	 */
	public void setRequestType(String newRequestType) {
		log.trace("setRequestType({})", newRequestType);
		JsonObject request = jo.get("request").getAsJsonObject();
		setAttribute(request, "type", newRequestType);
	}
	
	/**
	 * Setter for the request-timestamp attribute
	 * 
	 * @param newTimestamp String new attribute value
	 */
	public void setRequestTimestamp(String newTimestamp) {
		log.trace("setRequestTimestamp({})", newTimestamp);
		JsonObject request = jo.get("request").getAsJsonObject();
		setAttribute(request, "timestamp", newTimestamp);
	}
	
	public LocalDateTime getRequestTimestamp() {
		log.trace("getRequestTimestamp()");
		JsonObject request = jo.get("request").getAsJsonObject();
		JsonElement requestElement = request.get("timestamp");
		String timestampString = requestElement.getAsString();
		ZonedDateTime zdt = ZonedDateTime.parse(timestampString, DateTimeFormatter.ISO_DATE_TIME);
		return LocalDateTime.from(zdt);
	}
	
	/**
	 * Setter for the request-timestamp attribute
	 * 
	 * @param requestDate Date new attribute value
	 */
	public void setRequestTimestamp(Date requestDate) {
		log.trace("setRequestTimestamp({})", requestDate);
		this.requestDate = requestDate;
		
		Instant requestInstant = requestDate.toInstant();
		
		JsonObject request = jo.get("request").getAsJsonObject();
		setAttribute(request, "timestamp", DateTimeFormatter.ISO_INSTANT.format(requestInstant.truncatedTo(ChronoUnit.SECONDS)));
	}
		
	/**
	 * Setter for the request-requestId attribute
	 * 
	 * @param newRequestId String new attribute value
	 */
	public void setRequestRequestId(String newRequestId) {
		log.trace("setRequestRequestId({})", newRequestId);
		JsonObject request = jo.get("request").getAsJsonObject();
		setAttribute(request, "requestId", newRequestId);
	}
	
	/**
	 * Setter for the session-sessionId attribute
	 * 
	 * @param newSessionId String new attribute value
	 */
	public void setSessionSessionId(String newSessionId) {
		log.trace("setSessionSessionId({})", newSessionId);
		JsonObject session = jo.get("session").getAsJsonObject();
		setAttribute(session, "sessionId", newSessionId);
	}
	
	public void setSessionNew(boolean newSession) {
		log.trace("setSessionNew({})", newSession);
		JsonObject session = jo.get("session").getAsJsonObject();
		setAttribute(session, "new", newSession);
	}
	
	/**
	 * Setter for the session-user-userId attribute
	 * 
	 * @param newUserId String new attribute value
	 */
	public void setSessionUserId(String newUserId) {
		log.trace("setSessionUserId({})", newUserId);
		JsonObject session = jo.get("session").getAsJsonObject();
		JsonObject user = session.get("user").getAsJsonObject();
		setAttribute(user, "userId", newUserId);
	}
	
	/**
	 * Setter for the sessionAttributes attribute
	 * 
	 * @param attributeName String new attribute name
	 * @param joAttributeValue JsonObject new attribute value
	 */
	public void setSessionAttribute(String attributeName, JsonObject joAttributeValue) {
		log.trace("setSessionAttribute({}, {})", attributeName, joAttributeValue.toString());
		JsonObject session = jo.get("session").getAsJsonObject();
		JsonObject attributes = session.get("attributes").getAsJsonObject();
		setAttribute(attributes, attributeName, joAttributeValue);
		setSessionNew(false);
	}
	
	/**
	 * Setter for the sessionAttributes attribute
	 * 
	 * @param attributeName String new attribute name
	 * @param attributeValue String new attribute value
	 */
	public void setSessionAttribute(String attributeName, String attributeValue) {
		log.trace("setSessionAttribute({}, {})", attributeName, attributeValue);
		JsonObject session = jo.get("session").getAsJsonObject();
		JsonObject attributes = session.get("attributes").getAsJsonObject();
		setAttribute(attributes, attributeName, attributeValue);
		setSessionNew(false);
	}

	/**
	 * Setter for the sessionAttributes attribute
	 * 
	 * @param attributeName String new attribute name
	 * @param attributeValue boolean new attribute value
	 */
	public void setSessionAttribute(String attributeName, boolean attributeValue) {
		log.trace("setSessionAttribute({}, {})", attributeName, attributeValue);
		JsonObject session = jo.get("session").getAsJsonObject();
		JsonObject attributes = session.get("attributes").getAsJsonObject();
		setAttribute(attributes, attributeName, attributeValue);
		setSessionNew(false);
	}
	
	public void setSessionAttribute(String attributeName, JsonPrimitive attributeValue) {
		log.trace("setSessionAttribute({}, {})", attributeName, attributeValue);
		JsonObject session = jo.get("session").getAsJsonObject();
		JsonObject attributes = session.get("attributes").getAsJsonObject();
		setAttribute(attributes, attributeName, attributeValue);
		setSessionNew(false);
	}
	
	public void setSessionAttributes(TestDataGeneralResponse actualResponse) {
		if (actualResponse==null) { return; }
		JsonObject joSessionAttributes = actualResponse.getSessionAttributes();
		if (joSessionAttributes==null) { return; }
		Set<Entry<String, JsonElement>> members = joSessionAttributes.entrySet();
		for (Map.Entry<String, JsonElement> member : members) {
			String attributeName = member.getKey();
			JsonElement jeAttribute = member.getValue();
			
			if (jeAttribute!=null) {
				if (jeAttribute.isJsonObject()) {
					JsonObject joAttribute = jeAttribute.getAsJsonObject();
					setSessionAttribute(attributeName, joAttribute);
				}
				else if (jeAttribute.isJsonPrimitive()) {
					JsonPrimitive jpAttribute = jeAttribute.getAsJsonPrimitive();
					setSessionAttribute(attributeName, jpAttribute);
				}
			}
		}
	}

	/**
	 * Setter to add slots to request-intent-slots attribute
	 * 
	 * @param name String new slot name
	 * @param value String new slot value
	 */
	public void addRequestIntentSlot(String name, String value) {
		log.trace("addRequestIntentSlot({},{})", name, value);
		JsonObject request = jo.get("request").getAsJsonObject();
		JsonObject intent = request.get("intent").getAsJsonObject();
		JsonObject slots = intent.get("slots").getAsJsonObject();
		
		JsonObject slot = new JsonObject();
		slot.add("name", new JsonPrimitive(name));	
		if (value==null) {
			slot.add("value", (JsonPrimitive) null);
		} else {
			slot.add("value", new JsonPrimitive(value));	
		}
		slots.add(name, slot);
	}
	
	/**
	 * Setter to remove the request-intent
	 * 
	 * @return The removed intent
	 */
	public JsonElement removeRequestIntent() {
		log.trace("removeRequestIntent()");
		JsonObject request = jo.get("request").getAsJsonObject();
		return request.remove("intent");
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
