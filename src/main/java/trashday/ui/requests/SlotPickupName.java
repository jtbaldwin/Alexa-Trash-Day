package trashday.ui.requests;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;

/**
 * Handle Intent Slot for Pickup Name information from user.
 * 
 * @author J. Todd Baldwin
 *
 */
public class SlotPickupName implements Slot {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotPickupName.class);
    /** {@link com.amazon.speech.slu.Slot} from the Alexa request. */
	private com.amazon.speech.slu.Slot slot;
	/** The slot field name corresponding to the VUI IntentSchema / Sample Utterances */
    public static final String name = "PickupName";
    /** A printable description of this slot's purpose */
    public static final String description = "Pickup Name";
	
	/**
	 * Handle parsing of a request slot intended to store Pickup Name information
	 * in user requests.
	 * 
	 * @param intent Intent from Alexa request
	 */
	public SlotPickupName(Intent intent) {
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
		return SlotPickupName.name;
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
		return SlotPickupName.description;
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
	 * java object.  For this particular slot, a pickup name is considered
	 * valid as long as it is not empty.
	 * 
	 * @return String corresponding to the text spoken by the user
	 */
	public String validate() {
    	log.trace("validate({})", slot);
    	
    	// Did we get a good Pickup Name value?
    	if (isEmpty()) {
        	log.debug("validate Slot is empty");
    		return null;
    	}
    	String slotString = slot.getValue().trim().toLowerCase();
    	log.debug("validate slotString={}",slotString);
    	return slotString;
	}

}
