package alexatesting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TestDataGeneralResponse {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TestDataGeneralResponse.class);
    /** Represent our test data as JsonObject */
	private JsonObject jo = null;
	/** A GSON-based JSON parser used to parse the JSON
	 * output from all the test functions. */
    private static JsonParser parser = new JsonParser();
    /** A GSON-based JSON formatter used to ensure JSON
     * test output is formatted consistently. */
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public TestDataGeneralResponse(String json) {
		log.debug("TestDataGeneralResponse(json={})", json);
		
	    JsonElement actualResponse = parser.parse(json);
	    if (actualResponse!=null) {
	    	jo = actualResponse.getAsJsonObject();
	    }

	}
	
	public boolean sessionAttributesExist() {
	    JsonElement jeSessionAttributes = jo.get("sessionAttributes");
	    if (jeSessionAttributes != null) {
	    	return true;
	    }
	    return false;
	}
	
	public boolean sessionAttributeExists(String attributeName) {
		JsonObject joSessionAttributes = getSessionAttributes();
		if (joSessionAttributes==null) { return false; }
    	JsonElement jeAttribute = joSessionAttributes.get(attributeName);
    	if (jeAttribute==null) { return false; }
    	return true;
	}
	
	public JsonObject getSessionAttributes() {
		if (jo==null) { return null; }
	    JsonElement jeSessionAttributes = jo.get("sessionAttributes");
	    if (jeSessionAttributes==null) { return null; }
	    JsonObject joSessionAttributes = jeSessionAttributes.getAsJsonObject();
	    return joSessionAttributes;
	}
	
	public Boolean getSessionAttributeBoolean(String attributeName) {
		JsonObject joSessionAttributes = getSessionAttributes();
		if (joSessionAttributes==null) { return null; }
    	JsonElement jeAttribute = joSessionAttributes.get(attributeName);
    	if (jeAttribute==null) { return null; }
    	boolean cip = jeAttribute.getAsBoolean();
		return new Boolean(cip);
	}

	public String getSessionAttributeString(String attributeName) {
		JsonObject joSessionAttributes = getSessionAttributes();
		if (joSessionAttributes==null) { return null; }
    	JsonElement jeAttribute = joSessionAttributes.get(attributeName);
    	if (jeAttribute==null) { return null; }
    	return jeAttribute.getAsString();
	}

	public String toString() {
		return gson.toJson(jo).toString();
	}
}
