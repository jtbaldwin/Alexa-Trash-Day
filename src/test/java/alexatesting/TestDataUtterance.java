package alexatesting;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * For JUnit testing, a helper class to make it easier to
 * test dialogs between the user and our Alexa skill.
 * <p>
 * This allows us to easily create lists of JSON requests
 * that get sent to our skill during testing.  This enables
 * testing the multiple commands required to do things like
 * build a completely new schedule {@link trashday.TrashDaySpeechletRequestStreamHandlerTest#createDialogNewSchedule}
 * and delete from a schedule {@link trashday.TrashDaySpeechletRequestStreamHandlerTest#createDialogDeleteSchedule}.
 * 
 * @author J. Todd Baldwin
 *
 */
public class TestDataUtterance {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TestDataUtterance.class);
    /** The intent name sent to our application when the user speaks this uttterance (as defined in our application's SampleUtterances.txt file) */
	String	intentName;
	/** The spoken phrase from the user */
	String	utterance;
	/** List of the data Slot names that accompany this utterance from the user (may be empty) */
	Set<String>		slotNames;
	/** Map of the slot values for every slot name */
	Map<String,String>	slots;
	
	/**
	 * Create this object based on a single line from the SampleUtterances file.
	 * <p>
	 * Interprets the first word as the intent name.  Interprets the remaining text
	 * as the utterance (what is spoken by the user).  Interprets any word surrounded
	 * by braces (e.g. "{word}") as a slot name.
	 * 
	 * @param s A single line from the SampleUtterances file
	 */
	public TestDataUtterance(String s) {
		log.trace("TestDataUtterance({})",s);
		slotNames = new HashSet<String>();
		slots = new LinkedHashMap<String,String>();
		String[] words = s.split(" ", 2);
		intentName = words[0];
		utterance = words[1];
		words = utterance.split(" ");
		for (String word: words) {
			if (word.startsWith("{") && word.endsWith("}")) {
				int l = word.length();
				String slotName = word.substring(1, l-2);
				slotNames.add(slotName);
			}
		}
	}
	
	/**
	 * Getter for the intent name
	 * 
	 * @return intent name
	 */
	public String getIntentName() {
		return intentName;
	}

	/**
	 * Getter for the utterance
	 * 
	 * @return utterance
	 */
	public String getUtterance() {
		return utterance;
	}

	/**
	 * Getter for the slot names found in the utterance
	 * 
	 * @return Set of slot names
	 */
	public Set<String> getSlotNames() {
		return slotNames;
	}

	/**
	 * Getter for the slot name and values map.
	 * 
	 * @return Map of slot names to their values
	 */
	public Map<String, String> getSlots() {
		return slots;
	}

	/**
	 * Setter for the intent name
	 * 
	 * @param intentName String New value
	 */
	public void setIntentName(String intentName) {
		this.intentName = intentName;
	}

	/**
	 * Setter for the utterance
	 * 
	 * @param utterance String New value
	 */
	public void setUtterance(String utterance) {
		this.utterance = utterance;
	}

	/**
	 * Setter for the slot name list
	 * 
	 * @param slotNames Set New list of slot names
	 */
	public void setSlotNames(Set<String> slotNames) {
		this.slotNames = slotNames;
	}

	/**
	 * Setter for the slot name to value set
	 * 
	 * @param slots Map New set of slot names-to-values
	 */
	public void setSlots(Map<String, String> slots) {
		this.slots = slots;
	}

	/**
	 * Setter to add a new slot name and value pair
	 * 
	 * @param name String new slot name
	 * @param value String new slot value
	 */
	public void addSlotValue(String name, String value) {
		slots.put(name, value);
	}
}
