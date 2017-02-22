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
     * Give help information before a user has set time zone or any schedule information.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @return {@link SpeechletResponse} with appropriate spoken output and, optionally, a printed
     * 			help card sent to the Alexa app.
     */
    public static SpeechletResponse respondHelpInitial(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("respondHelpInitial(withHelpCard={})", withHelpCard);
    	
    	if (withHelpCard && (! sessionDao.getOverallHelpCardSent())) {
			sessionDao.setOverallHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.HELP_VERBAL_INITIAL.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX.toString()),
	        		reprompt(Phrase.TIME_ZONE_SET_REPROMPT),
	        		card("Trash Day Help", Phrase.HELP_CARD)); 
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.HELP_VERBAL_INITIAL.toString()),
	        		reprompt(Phrase.TIME_ZONE_SET_REPROMPT)); 
    	}
    }
    
    /**
     * Give help information when a user has requested "set time zone other"
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @return {@link SpeechletResponse} with appropriate spoken output and, optionally, a printed
     * 			help card sent to the Alexa app.
     */
    public static SpeechletResponse respondHelpOtherTimeZone(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("respondHelpOtherTimeZone(withHelpCard={})", withHelpCard);
    	
    	if (withHelpCard && (! sessionDao.getOverallHelpCardSent())) {
			sessionDao.setTimeZoneHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.TIME_ZONE_OTHER_VERBAL.toString()+Phrase.TIME_ZONE_OTHER_VERBAL_CARD_SUFFIX.toString()),
	        		reprompt(Phrase.TIME_ZONE_OTHER_REPROMPT),
	        		card("Trash Day Set Time Zone", Phrase.TIME_ZONE_HELP_CARD)); 
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.TIME_ZONE_OTHER_VERBAL.toString()),
	        		reprompt(Phrase.TIME_ZONE_OTHER_REPROMPT)); 
    	}
    }

    /**
     * Give help information after a user has set time zone but before the user has
     * set any schedule information.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @return {@link SpeechletResponse} with appropriate spoken output and, optionally, a printed
     * 			help card sent to the Alexa app.
     */
    public static SpeechletResponse respondHelpNoSchedule(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("respondHelpNoSchedule(withHelpCard={})", withHelpCard);
    	
    	if (withHelpCard && (! sessionDao.getOverallHelpCardSent())) {
			sessionDao.setOverallHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.HELP_VERBAL_NO_SCHEDULE.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX.toString()),
	        		reprompt(Phrase.HELP_REPROMPT_NO_SCHEDULE),
	        		card("Trash Day Help", Phrase.HELP_CARD)); 
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.HELP_VERBAL_NO_SCHEDULE.toString()),
	        		reprompt(Phrase.HELP_REPROMPT_NO_SCHEDULE)); 
    	}
    }

    /**
     * Our standard help response is a verbal and {@link com.amazon.speech.ui.Card}
     * response text.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @return Commands the User may give to our application
     */
    public static SpeechletResponse respondHelpWithSchedule(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("respondHelpWithSchedule(withHelpCard={})", withHelpCard);
    	
    	if (withHelpCard && (! sessionDao.getOverallHelpCardSent())) {
			sessionDao.setOverallHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.HELP_VERBAL_SCHEDULE_EXISTS.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX.toString()),
	        		reprompt(Phrase.HELP_REPROMPT_SCHEDULE_EXISTS),
	        		card("Trash Day Help", Phrase.HELP_CARD)); 
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrase.HELP_VERBAL_SCHEDULE_EXISTS.toString()+Phrase.HELP_VERBAL_CARD_SUFFIX.toString()),
	        		reprompt(Phrase.HELP_REPROMPT_SCHEDULE_EXISTS)); 
    	}
    }
    
}
