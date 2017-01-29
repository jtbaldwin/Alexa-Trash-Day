package trashday.ui.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

/**
 * Provides some "helper" functions that make the code more readable for
 * all child classes.
 * 
 * @author	J. Todd Baldwin
 */
public class ResponseHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ResponseHelpers.class);
    
    /**
     * Create a new {@link com.amazon.speech.ui.PlainTextOutputSpeech} object and
     * set its text value.
     * 
     * @param text String for the {@link com.amazon.speech.ui.PlainTextOutputSpeech#setText(String)}
     * @return {@link com.amazon.speech.ui.PlainTextOutputSpeech}
     */
    protected static PlainTextOutputSpeech outputSpeech(String text) {
    	log.trace("outputSpeech(text={})", text);
    	PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
    	outputSpeech.setText(text);
    	return outputSpeech;
    }
    
    /**
     * Create a new {@link com.amazon.speech.ui.PlainTextOutputSpeech} object and
     * set its text value based one of the hard-coded phrases in {@link Phrases}.
     * 
     * @param phrase Phrases for the {@link com.amazon.speech.ui.PlainTextOutputSpeech#setText(String)}
     * @return {@link com.amazon.speech.ui.PlainTextOutputSpeech}
     */
    protected static PlainTextOutputSpeech outputSpeech(Phrases phrase) {
    	return outputSpeech(phrase.toString());
    }
    
    /**
     * Create a new {@link com.amazon.speech.ui.Reprompt} object and
     * set its text value.
     * 
     * @param text String for the {@link com.amazon.speech.ui.Reprompt}
     * @return {@link com.amazon.speech.ui.Reprompt}
     */
    protected static Reprompt reprompt(String text) {
    	log.trace("reprompt(text={})", text);
        PlainTextOutputSpeech repromptText = new PlainTextOutputSpeech();
        repromptText.setText(text);
        
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptText);
        return reprompt;
    }
    
    /**
     * Create a new {@link com.amazon.speech.ui.Reprompt} object and
     * set its text value based one of the hard-coded phrases in {@link Phrases}.
     * 
     * @param phrase Phrases for the {@link com.amazon.speech.ui.Reprompt}
     * @return {@link com.amazon.speech.ui.Reprompt}
     */
    protected static Reprompt reprompt(Phrases phrase) {
    	return reprompt(phrase.toString());
    }
    
    /**
     * Create a new {@link com.amazon.speech.ui.SimpleCard} object and
     * set its default title and content values.
     * 
     * @param text String for the {@link com.amazon.speech.ui.SimpleCard#setContent(String)}
     * @return {@link com.amazon.speech.ui.SimpleCard}
     */
    protected static SimpleCard card(String text) {
    	log.trace("card(text={})", text);
        SimpleCard card = new SimpleCard();
        card.setTitle("Trash Day");
       	card.setContent(text);
		return card;
    }
    
    /**
     * Create a new {@link com.amazon.speech.ui.SimpleCard} object and
     * set its default title and content value based one of the hard-coded phrases in {@link Phrases}.
     * 
     * @param phrase Phrases for the new card
     * @return {@link com.amazon.speech.ui.SimpleCard}
     */
    protected static SimpleCard card(Phrases phrase) {
    	return card(phrase.toString());
    }

    /**
     * Create a new {@link com.amazon.speech.ui.SimpleCard} object and
     * set its title and content values.
     * 
     * @param title String for this new card
     * @param text String for this new card
     * @return {@link com.amazon.speech.ui.SimpleCard}
     */
    protected static SimpleCard card(String title, String text) {
    	log.trace("card(title={}, text={})", title, text);
        SimpleCard card = new SimpleCard();
        card.setTitle(title);
       	card.setContent(text);
		return card;
    }

    /**
     * Create a new {@link com.amazon.speech.ui.SimpleCard} object and
     * set its title and content values.
     * 
     * @param title String for this new card
     * @param phrase Phrases for this new card
     * @return {@link com.amazon.speech.ui.SimpleCard}
     */
    protected static SimpleCard card(String title, Phrases phrase) {
    	return card(title, phrase.toString());
    }
}
