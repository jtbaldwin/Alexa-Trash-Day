package trashday.ui.requests;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;

/**
 * Handle Intent Slot for Nth Day Of Month information from user.
 * 
 * @author J. Todd Baldwin
 *
 */
public class SlotNthOfMonth implements Slot {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotNthOfMonth.class);
    
    /** {@link com.amazon.speech.slu.Slot} from the Alexa request. */
	private com.amazon.speech.slu.Slot slot;
	/** The slot field name corresponding to the VUI IntentSchema / Sample Utterances */
	public static final String name = "NthOfMonth";
    /** A printable description of this slot's purpose */
	public static final String description = "Day Of Month";

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
		
		nthMap.put("sixth", 6);
		nthMap.put("6th", 6);
		
		nthMap.put("seventh", 7);
		nthMap.put("7th", 7);
		
		nthMap.put("eighth", 8);
		nthMap.put("8th", 8);
		
		nthMap.put("ninth", 9);
		nthMap.put("9th", 9);
		
		nthMap.put("tenth", 10);
		nthMap.put("10th", 10);
		
		nthMap.put("eleventh", 11);
		nthMap.put("11th", 11);
		nthMap.put("11st", 11);
		
		nthMap.put("twelfth", 12);
		nthMap.put("12th", 12);
		
		nthMap.put("thirteenth", 13);
		nthMap.put("13rd", 13);
		
		nthMap.put("fourteenth", 14);
		nthMap.put("14th", 14);
		
		nthMap.put("fifteenth", 15);
		nthMap.put("15th", 15);
		
		nthMap.put("sixteenth", 16);
		nthMap.put("16th", 16);
		
		nthMap.put("seventeenth", 17);
		nthMap.put("17th", 17);
		
		nthMap.put("eighteenth", 18);
		nthMap.put("18th", 18);
		
		nthMap.put("ninteenth", 19);
		nthMap.put("19th", 19);

		nthMap.put("twentieth", 20);
		nthMap.put("20th", 20);
		
		nthMap.put("twenty-first", 21);
		nthMap.put("21st", 21);
		
		nthMap.put("twenty-second", 22);
		nthMap.put("20 second", 22);
		nthMap.put("22nd", 22);
		
		nthMap.put("twenty-third", 23);
		nthMap.put("23rd", 23);
		
		nthMap.put("twenty-fourth", 24);
		nthMap.put("24th", 24);
		
		nthMap.put("twenty-fifth", 25);
		nthMap.put("25th", 25);
		
		nthMap.put("twenty-sixth", 26);
		nthMap.put("26th", 26);
		
		nthMap.put("twenty-seventh", 27);
		nthMap.put("27th", 27);
		
		nthMap.put("twenty-eighth", 28);
		nthMap.put("28th", 28);
		
		nthMap.put("twenty-ninth", 29);
		nthMap.put("29th", 29);
		
		nthMap.put("thirtieth", 30);
		nthMap.put("30th", 30);
		
		nthMap.put("thirty-first", 31);
		nthMap.put("31st", 31);
		
		// ... of the month
		nthMap.put("last day", -1);
		nthMap.put("second day before the end", -2);
		nthMap.put("third day before the end", -3);
		nthMap.put("fourth day before the end", -4);
		nthMap.put("fifth day before the end", -5);
		nthMap.put("sixth day before the end", -6);
		nthMap.put("seventh day before the end", -7);
		nthMap.put("eighth day before the end", -8);
		nthMap.put("ninth day before the end", -9);
		nthMap.put("tenth day before the end", -10);
		nthMap.put("eleventh day before the end", -11);
		nthMap.put("twelfth day before the end", -12);
		nthMap.put("thirteenth day before the end", -13);
		nthMap.put("fourteenth day before the end", -14);
		nthMap.put("fifteenth day before the end", -15);
		nthMap.put("sixteenth day before the end", -16);
		nthMap.put("seventeenth day before the end", -17);
		nthMap.put("eighteenth day before the end", -18);
		nthMap.put("ninteenth day before the end", -19);
		nthMap.put("twentieth day before the end", -20);
		nthMap.put("twenty-first day before the end", -21);
		nthMap.put("twenty-second day before the end", -22);
		nthMap.put("twenty-third day before the end", -23);
		nthMap.put("twenty-fourth day before the end", -24);
		nthMap.put("twenty-fifth day before the end", -25);
		nthMap.put("twenty-sixth day before the end", -26);
		nthMap.put("twenty-seventh day before the end", -27);
		nthMap.put("twenty-eighth day before the end", -28);
		nthMap.put("twenty-ninth day before the end", -29);
		nthMap.put("thirtieth day before the end", -30);
		nthMap.put("thirty-first day before the end", -31);
	};
	
	
	/**
	 * Handle parsing of a request slot intended to store Time Zone information
	 * in user requests.
	 * 
	 * @param intent Intent from Alexa request
	 */
	public SlotNthOfMonth(Intent intent) {
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
		return SlotNthOfMonth.name;
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
		return SlotNthOfMonth.description;
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
    	log.debug("validate nth day of month slot value={}", slot.getValue());
    	String nthDayOfMonthString = slot.getValue();
    	log.debug("validate nth day of month text translated from Alexa={}", nthDayOfMonthString);
    	nthDayOfMonthString = nthDayOfMonthString.trim();
		
    	Integer dayOfMonth = nthMap.get(nthDayOfMonthString);
    	if (dayOfMonth==null) {
	    	log.error("validate nth day of month: invalid day = {}", nthDayOfMonthString);
	    	return null;
    	}
    	
    	log.debug("validate nthDayOfMonth={}", dayOfMonth);
    	return dayOfMonth;
	}

}
