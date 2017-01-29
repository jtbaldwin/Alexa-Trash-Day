package trashday.ui.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;

/**
 * Organizes all the voice responses for Alexa "Yes" and "No" 
 * intents.
 * 
 * @author	J. Todd Baldwin
 */
public class ResponsesYesNo {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ResponsesYesNo.class);

    /**
     * When we get a Yes/No from the user but don't have
     * any idea why, respond with a verbal "Sorry" from Alexa 
     * without any associated {@link com.amazon.speech.ui.Card}.
     * 
     * @return "Sorry.  I didn't understand."
     */
    public static SpeechletResponse tellYesNoMisunderstood() {
    	log.info("respondYesNoMisunderstood()");
        String speechText = "Sorry.  I didn't understand what question you were answering.  Please say \"help\" for what things you can say.";
        log.error(speechText);
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);
        return SpeechletResponse.newTellResponse(speech);		    		
    }
    
    /**
     * When we get an expected Yes/No from the user but we've
     * somehow lost track of how the application should respond
     * due to a code problem, then respond with a verbal "Oops" from Alexa 
     * without any associated {@link com.amazon.speech.ui.Card}.
     * 
     * @param confirmActionName String text description of the
     * 			action we don't know how to handle.
     * @return "Uh-oh, I found a programming problem."
     */
    public static SpeechletResponse tellYesNoProblem(String confirmActionName) {
    	log.info("respondYesNoProblem({})", confirmActionName);
        String speechText = "Uh-oh, I found a programming problem.  Cannot "+confirmActionName;
        log.error(speechText);
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);
        return SpeechletResponse.newTellResponse(speech);		    		
    }
    
    /**
     * When we get a No from the user that is canceling an
     * action, we use this standard, verbal "Cancelled" from Alexa 
     * without any associated {@link com.amazon.speech.ui.Card}.
     * 
     * @param confirmActionName String text description of the
     * 			action being cancelled.
     * @return "Cancelled"
     */
    public static SpeechletResponse tellCancellingAction(String confirmActionName) {
    	log.info("tellCancellingAction({})", confirmActionName);
        String speechText = "Cancelling the "+confirmActionName;
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);
        return SpeechletResponse.newTellResponse(speech);    		
    }
}
