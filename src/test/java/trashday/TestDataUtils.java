package trashday;

import java.util.List;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import trashday.model.Schedule;
import trashday.model.TimeOfWeek;

/**
 * For JUnit testing, we need to provide the ability to translate
 * information into JSON format.  This lets us create expected responses that include
 * storing the expected result in sessionAttributes.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interface-reference#response-format">Alexa Skills Kit Docs: JSON Interface Reference for Custom Skills</a>
 *
 */
public final class TestDataUtils {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TestDataUtils.class);
	
	/**
	 * Getter a schedule in a JSON format.
	 * 
	 * @param schedule Schedule
	 * 		Get the JSON version of this Schedule
	 * @return JsonObject representation of the Schedule
	 */
	public static JsonObject getJson(Schedule schedule) {
		log.trace("getJson()");
		JsonObject jo = new JsonObject();
		
		List<String> pickupNames = schedule.getPickupNames();
		JsonArray joPickupNames = new JsonArray();
		for (String pickupName : pickupNames) {
			joPickupNames.add(pickupName);
		}
		jo.add("pickupNames", joPickupNames);
		
		JsonObject joPickupSchedule = new JsonObject();
		for (String pickupName : pickupNames) {
			JsonArray joPickupTimes = new JsonArray();
			SortedSet<TimeOfWeek> pickupTowSet = schedule.getPickupSchedule(pickupName);
			for (TimeOfWeek tow : pickupTowSet) {
				JsonObject joPickupTime = new JsonObject();
				joPickupTime.add("dow", new JsonPrimitive(tow.getDayOfWeek().toString()));
				JsonObject joTod = new JsonObject();
				joTod.add("hour", new JsonPrimitive( tow.getHour() ));
				joTod.add("minute", new JsonPrimitive( tow.getMinute() ));
				joTod.add("second", new JsonPrimitive( 0 ));
				joTod.add("nano", new JsonPrimitive( 0 ));
				joPickupTime.add("tod",joTod);
				joPickupTimes.add(joPickupTime);
			}
			joPickupSchedule.add(pickupName, joPickupTimes);
		}
		jo.add("pickupSchedule", joPickupSchedule);
		return jo;
	}
}
