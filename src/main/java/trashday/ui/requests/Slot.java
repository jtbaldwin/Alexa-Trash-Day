package trashday.ui.requests;

/**
 * Interface that must be satisfied by all the Slot classes in
 * {@link trashday.ui.requests}.
 * <p>
 * All Slot classes provide helper functions for user data received
 * in Intent Slots.  Primary use to is validate the data is
 * appropriate for each slot's intended purpose and to
 * convert it to appropriate java class.  For example,
 * the "DayOfWeek" slot needs to have something that will
 * correctly map to a specific day value and convert to
 * {@link java.time.DayOfWeek}.
 * 
 * @author	J. Todd Baldwin
 * @see		<a href="https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/defining-the-voice-interface#h2_intents">Alexa Skills Kit Docs: Defining the Voice Interface</a>
 *
 */
public interface Slot {
	/**
	 * Getter for the actual {@link com.amazon.speech.slu.Slot} from the Alexa request.
	 * 
	 * @return Alexa request {@link com.amazon.speech.slu.Slot}
	 */
	public com.amazon.speech.slu.Slot getSlot();
	
	/**
	 * Getter for slot name that is used in VUI Intent Schema / Sample Utterances.
	 * 
	 * @return The name of this {@link com.amazon.speech.slu.Slot} that is
	 * 		used in the VUI IntentSchema / Sample Utterances
	 */
	public String getName();
	
	/**
	 * Getter for slot description that tells the use of this slot in readable form.
	 * 
	 * @return The description of this {@link com.amazon.speech.slu.Slot} that is
	 * 		used in the VUI IntentSchema / Sample Utterances
	 */
	public String getDescription();
	
	/**
	 * Check if the data in the {@link com.amazon.speech.slu.Slot} is null,
	 * empty, or just whitespace.
	 * 
	 * @return true if no usable data in the slot
	 */
	public boolean isEmpty();
}
