package trashday.ui.requests;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;

/**
 * Handle Intent Slot for Day Of Week information from user.
 * 
 * @author J. Todd Baldwin
 *
 */
public class SlotDayOfWeek implements Slot {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotDayOfWeek.class);
    /** {@link com.amazon.speech.slu.Slot} from the Alexa request. */
	private com.amazon.speech.slu.Slot slot;
	/** The slot field name corresponding to the VUI IntentSchema / Sample Utterances */
    private static final String name = "DayOfWeek";
    /** A printable description of this slot's purpose */
    private static final String description = "Day Of Week";
	
	/**
	 * Handle parsing of a request slot intended to store Day Of Week information
	 * in user requests.
	 * 
	 * @param intent Intent from Alexa request
	 */
	public SlotDayOfWeek(Intent intent) {
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
		return SlotDayOfWeek.name;
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
		return SlotDayOfWeek.description;
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
	 * java object.  For this particular slot, we need to know the 
	 * appropriate time zone to handle day of week given as today, tomorrow
	 * or yesterday.
	 * 
	 * @param ldtRequest LocalDateTime when the user request was given
	 * @return DayOfWeek corresponding to the text spoken by the user
	 */
	public DayOfWeek validate(LocalDateTime ldtRequest) {
    	log.debug("validate() ldtRequest={}", ldtRequest);
    	log.trace("validate({})", slot);
    	
    	// Did we get a good Day-of-Week value?
    	if (isEmpty()) {
        	log.debug("validate Slot is empty");
    		return null;
    	}
    	String slotString = slot.getValue().trim();
    	log.debug("validate slotString={}",slotString);
    	
    	DayOfWeek dow = null;
    	try {
    		if (slotString.equalsIgnoreCase("today")) {
    			dow = ldtRequest.getDayOfWeek();
    	    	log.debug("validate today=>dow={}",dow);
    	    	
    		} else if (slotString.equalsIgnoreCase("tomorrow")) {
    			dow = ldtRequest.getDayOfWeek().plus(1);
    	    	log.debug("validate tomorrow=>dow={}",dow);
    	    	
    		} else if (slotString.equalsIgnoreCase("yesterday")) {
    			dow = ldtRequest.getDayOfWeek().minus(1);
    	    	log.debug("validate yesterday=>dow={}",dow);
    	    	
    		} else {
    			dow=DayOfWeek.valueOf(slotString.toUpperCase());
    	    	log.debug("validate parse=>dow={}",dow);
    		}
    	} catch (IllegalArgumentException e) {
	    	log.error("validate DayOfWeek parse error={}",e);
    	}
    	log.debug("validate dow={}", dow);
    	return dow;
	}

}
