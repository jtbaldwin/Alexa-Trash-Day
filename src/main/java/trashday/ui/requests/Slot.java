package trashday.ui.requests;

/**
 * Interface that must be satisfied by all the Slot* classes in
 * {@link trashday.ui.requests}.
 * 
 * All Slot* classes provide helper functions for user data received
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
	public com.amazon.speech.slu.Slot getSlot();
	public String getName();
	public String getDescription();
	public boolean isEmpty();
}
