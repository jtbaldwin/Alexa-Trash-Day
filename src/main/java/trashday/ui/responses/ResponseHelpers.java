package trashday.ui.responses;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.Card;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

import trashday.storage.SessionDao;
import trashday.ui.FormatUtils;

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
     * set its text value based one of the hard-coded phrases in {@link Phrase}.
     * 
     * @param phrase Phrases for the {@link com.amazon.speech.ui.PlainTextOutputSpeech#setText(String)}
     * @return {@link com.amazon.speech.ui.PlainTextOutputSpeech}
     */
    protected static PlainTextOutputSpeech outputSpeech(Phrase phrase) {
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
     * set its text value based one of the hard-coded phrases in {@link Phrase}.
     * 
     * @param phrase Phrases for the {@link com.amazon.speech.ui.Reprompt}
     * @return {@link com.amazon.speech.ui.Reprompt}
     */
    protected static Reprompt reprompt(Phrase phrase) {
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
     * set its default title and content value based one of the hard-coded phrases in {@link Phrase}.
     * 
     * @param phrase Phrases for the new card
     * @return {@link com.amazon.speech.ui.SimpleCard}
     */
    protected static SimpleCard card(Phrase phrase) {
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
    protected static SimpleCard card(String title, Phrase phrase) {
    	return card(title, phrase.toString());
    }

    protected static SpeechletResponse generalAskOrTellResponse(SessionDao sessionDao, boolean askOrTell, boolean attachHelpCard, String helpCardSessionAttributeName, String verbalPrefix, Phrase verbalPhrase, Phrase verbalCardSuffix, Phrase verbalReprompt, String cardTitle, Phrase cardBody) {
		if (askOrTell) {
			return generalAskResponse(
					sessionDao,
					attachHelpCard,
					helpCardSessionAttributeName,
					verbalPrefix,
					verbalPhrase,
					verbalCardSuffix,
					verbalReprompt,
					cardTitle, cardBody
					);
		} else {
			return generalTellResponse(
					sessionDao,
					attachHelpCard,
					helpCardSessionAttributeName,
					verbalPrefix,
					verbalPhrase,
					verbalCardSuffix,
					cardTitle, cardBody				
					);
		}
    }
    
    protected static SpeechletResponse generalAskResponse(SessionDao sessionDao, boolean attachHelpCard, String helpCardSessionAttributeName, String verbalPrefix, Phrase verbalPhrase, Phrase verbalCardSuffix, Phrase verbalReprompt, String cardTitle, Phrase cardBody) {
		StringBuilder sb = new StringBuilder();
		if (verbalPrefix!=null) {
			sb.append(verbalPrefix);
		}
		if (verbalPhrase!=null) {
			sb.append(verbalPhrase);
		}
    	if (attachHelpCard && (! sessionDao.isSessionAttributeTrue(helpCardSessionAttributeName)) ) {
    		// With Card...
			sessionDao.setSessionAttributeTrue(helpCardSessionAttributeName);
			if (verbalCardSuffix!=null) {
				sb.append(verbalCardSuffix);
			}
			Card card;
			if (cardTitle!=null) {
				card = card(cardTitle, cardBody);
			} else {
				card = card(cardBody);
			}
            return SpeechletResponse.newAskResponse(
            		outputSpeech(sb.toString()),
            		reprompt(verbalReprompt), 
            		card);
		} else {
			// No card...
            return SpeechletResponse.newAskResponse(
            		outputSpeech(sb.toString()),
            		reprompt(verbalReprompt));
		}
    }
    
    protected static SpeechletResponse generalTellResponse(SessionDao sessionDao, boolean attachHelpCard, String helpCardSessionAttributeName, String verbalPrefix, Phrase verbalPhrase, Phrase verbalCardSuffix, String cardTitle, Phrase cardBody) {
    	String verbalPhraseString = null;
    	String verbalCardSuffixString = null;
    	String cardBodyString = null;
    	if (verbalPhrase!=null) { verbalPhraseString = verbalPhrase.toString(); }
    	if (verbalCardSuffix!=null) { verbalCardSuffixString = verbalCardSuffix.toString(); }
    	if (cardBody != null) { cardBodyString = cardBody.toString(); }
    	return generalTellResponse(sessionDao, attachHelpCard, helpCardSessionAttributeName, verbalPrefix, verbalPhraseString, verbalCardSuffixString, cardTitle, cardBodyString);
    }

    protected static SpeechletResponse generalTellResponse(SessionDao sessionDao, boolean attachHelpCard, String helpCardSessionAttributeName, String verbalPrefix, String verbalPhrase, String verbalCardSuffix, String cardTitle, String cardBody) {
		StringBuilder sb = new StringBuilder();
		if (verbalPrefix!=null) {
			sb.append(verbalPrefix);
		}
		if (verbalPhrase!=null) {
			sb.append(verbalPhrase);
		}
    	if (attachHelpCard && 
    		( (helpCardSessionAttributeName==null) || (! sessionDao.isSessionAttributeTrue(helpCardSessionAttributeName)) )
            ) {
    		// With Card...
			sessionDao.setSessionAttributeTrue(helpCardSessionAttributeName);
			if (verbalCardSuffix!=null) {
				sb.append(verbalCardSuffix);
			}
			Card card;
			if (cardTitle!=null) {
				card = card(cardTitle, cardBody);
			} else {
				card = card(cardBody);
			}
            return SpeechletResponse.newTellResponse(
            		outputSpeech(sb.toString()), 
            		card);
		} else {
			// No Card
            return SpeechletResponse.newTellResponse(
            		outputSpeech(sb.toString()));
		}

    }
    
    public static SpeechletResponse respondGeneralMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields, Phrase verbal, Phrase reprompt) {
		log.trace("respondGeneralMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		
		// Uhoh, missing some information on add schedule.
		String missingDataString = FormatUtils.formattedJoin(missingDataFields, "I missed the ", " information.\n");
		
		return generalAskOrTellResponse(
				sessionDao,
				sessionDao.getScheduleConfigInProgress(), // Ask if Config-in-Progress
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
				missingDataString, // A verbal prefix string
				verbal, // The main verbal phrase
				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				reprompt, // A reprompt phrase if an Ask response
				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
				);
    }

    public static SpeechletResponse respondGeneralInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields, Phrase verbal, Phrase reprompt) {
		log.trace("respondGeneralInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		
		// Got some invalid values.
		String invalidDataString = FormatUtils.formattedJoin(invalidDataFields, "I didn't understand the ", " information.\n");
		
		return generalAskOrTellResponse(
				sessionDao,
				sessionDao.getScheduleConfigInProgress(), // Ask if Config-in-Progress
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
				invalidDataString, // A verbal prefix string
				verbal, // The main verbal phrase
				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				reprompt, // A reprompt phrase if an Ask response
				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
				);
    }
    
    public static SpeechletResponse respondGeneralScheduleChange(SessionDao sessionDao, boolean withHelpCard, String changeDescription) {
    	log.trace("respondGeneralAddedEntry(withHelpCard={}, changeDescription={})", withHelpCard, changeDescription);
    	if (sessionDao.getScheduleConfigInProgress()) {
    		return generalAskResponse(
    				sessionDao,
    				withHelpCard, // Send help card if true and not already sent in this Session.
    				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
    				changeDescription+" To make more changes, ", // A verbal prefix string
    				Phrase.SCHEDULE_ALTER_VERBAL, // The main verbal phrase
    				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
    				Phrase.SCHEDULE_ALTER_REPROMPT, // A reprompt phrase if an Ask response
    				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
    				);
    	} else {
			return generalTellResponse(
					sessionDao,
					false, // No help card
					null, // Flag for which help card
					changeDescription, // A verbal prefix string
					(String) null,
					(String) null,
					null, (String) null
					);
		}
    }
    
    public static SpeechletResponse respondGeneralPickupNotAdded(SessionDao sessionDao, boolean withHelpCard, String pickupName) {
    	log.trace("respondGeneralPickupNotAdded(withHelpCard={}, pickupName={})", withHelpCard, pickupName);
    	if (sessionDao.getScheduleConfigInProgress()) {
    		return generalAskResponse(
    				sessionDao,
    				withHelpCard, // Send help card if true and not already sent in this Session.
    				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
    				"That "+pickupName+" pickup already exists. To make more changes, ", // A verbal prefix string
    				Phrase.SCHEDULE_ALTER_VERBAL, // The main verbal phrase
    				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
    				Phrase.SCHEDULE_ALTER_REPROMPT, // A reprompt phrase if an Ask response
    				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
    				);
    	} else {
			return generalTellResponse(
					sessionDao,
					false, // No help card
					null, // Flag for which help card
					"That "+pickupName+" pickup already exists.", // A verbal prefix string
					(String) null,
					(String) null,
					null, (String) null
					);
		}
    }
    

}
