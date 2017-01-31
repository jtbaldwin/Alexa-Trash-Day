package trashday.ui.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;
import trashday.storage.SessionDao;

/**
 * Organizes all the voice responses for Alexa "help" intent.
 * 
 * @author	J. Todd Baldwin
 */
public class ResponsesHelp extends ResponseHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ResponsesHelp.class);

    /**
     * Our standard help response is a verbal and {@link com.amazon.speech.ui.Card}
     * response text.
     * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param nonEmptyScheduleExists boolean True if the user already has a non-empty schedule defined.
     * 
     * @return Commands the User may give to our application
     */
    public static SpeechletResponse respondHelp(SessionDao sessionDao, boolean withHelpCard, boolean nonEmptyScheduleExists) {
    	log.info("respondHelp(withHelpCard={})", withHelpCard);
    	
    	if (nonEmptyScheduleExists) {
	    	if (withHelpCard && (! sessionDao.getOverallHelpCardSent())) {
				sessionDao.setOverallHelpCardSent();
		        return SpeechletResponse.newAskResponse(
		        		outputSpeech(Phrases.HELP_VERBAL_SCHEDULE_EXISTS.toString()+Phrases.HELP_VERBAL_CARD_SUFFIX.toString()),
		        		reprompt(Phrases.HELP_REPROMPT_SCHEDULE_EXISTS),
		        		card("Trash Day Help", Phrases.HELP_CARD)); 
	    	} else {
		        return SpeechletResponse.newAskResponse(
		        		outputSpeech(Phrases.HELP_VERBAL_SCHEDULE_EXISTS.toString()+Phrases.HELP_VERBAL_CARD_SUFFIX.toString()),
		        		reprompt(Phrases.HELP_REPROMPT_SCHEDULE_EXISTS)); 
	    	}
    	} else {
	    	if (withHelpCard && (! sessionDao.getOverallHelpCardSent())) {
				sessionDao.setOverallHelpCardSent();
		        return SpeechletResponse.newAskResponse(
		        		outputSpeech(Phrases.HELP_VERBAL_NO_SCHEDULE.toString()+Phrases.HELP_VERBAL_CARD_SUFFIX.toString()),
		        		reprompt(Phrases.HELP_REPROMPT_NO_SCHEDULE),
		        		card("Trash Day Help", Phrases.HELP_CARD)); 
	    	} else {
		        return SpeechletResponse.newAskResponse(
		        		outputSpeech(Phrases.HELP_VERBAL_NO_SCHEDULE.toString()+Phrases.HELP_VERBAL_CARD_SUFFIX.toString()),
		        		reprompt(Phrases.HELP_REPROMPT_NO_SCHEDULE)); 
	    	}
    	}
    }
}
