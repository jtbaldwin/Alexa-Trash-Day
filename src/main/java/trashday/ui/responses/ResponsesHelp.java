package trashday.ui.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.SimpleCard;

/**
 * Organizes all the voice responses for Alexa "help" intent.
 * 
 * @author	J. Todd Baldwin
 */
public class ResponsesHelp {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ResponsesHelp.class);

    /**
     * Our standard help response is a verbal and {@link com.amazon.speech.ui.Card}
     * response text.
     * 
     * @return Commands the User may give to our application
     */
    public static SpeechletResponse tellHelp() {
    	log.info("tellHelp");
    	
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(Phrases.HELP_VERBAL_HELP.toString()+Phrases.HELP_VERBAL_CARD_SUFFIX.toString());
        
        SimpleCard card = new SimpleCard();
        card.setTitle("Trash Day Help");
        card.setContent(Phrases.HELP_CARD_HELP.toString());
        
        return SpeechletResponse.newTellResponse(speech, card);
    }

}
