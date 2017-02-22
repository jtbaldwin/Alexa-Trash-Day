package trashday.ui.requests;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;

/**
 * Handle Intent Slot for Nth Week Of Month information from user.
 * 
 * @author J. Todd Baldwin
 *
 */
public class SlotWeekOfMonth implements Slot {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotWeekOfMonth.class);
    
    /** {@link com.amazon.speech.slu.Slot} from the Alexa request. */
	private com.amazon.speech.slu.Slot slot;
	/** The slot field name corresponding to the VUI IntentSchema / Sample Utterances */
	public static final String name = "WeekOfMonth";
    /** A printable description of this slot's purpose */
	public static final String description = "Week Of Month";

	/** Map of month numbers to spoken form. */
	public static Map<String, Integer> nthMap;

	static {
		nthMap = new HashMap<String, Integer>();
		
		// ... of the month
		nthMap.put("first", 1);
		nthMap.put("1st", 1);
		
		nthMap.put("second", 2);
		nthMap.put("2nd", 2);
		
		nthMap.put("third", 3);
		nthMap.put("3rd", 3);
		
		nthMap.put("fourth", 4);
		nthMap.put("4th", 4);
		
		nthMap.put("fifth", 5);
		nthMap.put("5th", 5);
		
		nthMap.put("last", -1);
	};
	
	
	/**
	 * Handle parsing of a request slot intended to store Week of Month information
	 * in user requests.
	 * 
	 * @param intent Intent from Alexa request
	 */
	public SlotWeekOfMonth(Intent intent) {
		this.slot = intent.getSlot(name);
	}
	
	/**
	 * Getter for the actual {@link com.amazon.speech.slu.Slot} from the Alexa request.
	 * 
	 * @return Alexa request {@link com.amazon.speech.slu.Slot}
	 * @see trashday.ui.requests.Slot#getSlot()
	 */
	@Override
	public com.amazon.speech.slu.Slot getSlot() {
		return this.slot;
	}
	
	/**
	 * Getter for slot name that is used in VUI Intent Schema / Sample Utterances.
	 * 
	 * @return The name of this {@link com.amazon.speech.slu.Slot} that is
	 * 		used in the VUI IntentSchema / Sample Utterances
	 * @see trashday.ui.requests.Slot#getName()
	 */
	@Override
	public String getName() {
		return SlotWeekOfMonth.name;
	}

	/**
	 * Getter for slot description that tells the use of this slot in readable form.
	 * 
	 * @return The description of this {@link com.amazon.speech.slu.Slot} that is
	 * 		used in the VUI IntentSchema / Sample Utterances
	 * @see trashday.ui.requests.Slot#getDescription()
	 */
	@Override
	public String getDescription() {
		return SlotWeekOfMonth.description;
	}

	/**
	 * Check if the data in the {@link com.amazon.speech.slu.Slot} is null,
	 * empty, or just whitespace.
	 * 
	 * @return true if no usable data in the slot
	 * @see trashday.ui.requests.Slot#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
    	log.trace("isEmpty({})", slot);
    	if (slot==null) {
    		return true;
    	}
    	String s = slot.getValue();
    	if (s==null) {
    		return true;
    	}
    	if (s.trim().length() < 1) {
    		return true;
    	}
    	return false;
	}
	
	/**
	 * Validate that the user data given in the {@link com.amazon.speech.slu.Slot}
	 * is useful data that can be instantiated as expected to the correct
	 * java object.
	 * 
	 * @return Integer corresponding to the text spoken by the user
	 */
	public Integer validate() {
    	log.trace("validate({})", slot);

    	// Did we get a good time zone value?
    	if (isEmpty()) {
        	log.info("validate Slot is empty");
    		return null;
    	}
    	log.debug("validate nth week of month slot value={}", slot.getValue());
    	String nthWeekOfMonthString = slot.getValue();
    	log.debug("validate nth week of month text translated from Alexa={}", nthWeekOfMonthString);
    	nthWeekOfMonthString = nthWeekOfMonthString.trim();
		
    	Integer weekOfMonth = nthMap.get(nthWeekOfMonthString);
    	if (weekOfMonth==null) {
	    	log.error("validate nth week of month: invalid day = {}", nthWeekOfMonthString);
	    	return null;
    	}
    	
    	log.debug("validate nthWeekOfMonth={}", weekOfMonth);
    	return weekOfMonth;
	}

}
