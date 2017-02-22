package trashday.ui.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;

import trashday.model.Calendar;
import trashday.storage.SessionDao;

/**
 * Organizes all the voice responses for Alexa "stop" and "exit" intents.
 * 
 * @author	J. Todd Baldwin
 */
public class ResponsesExit extends ResponseHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ResponsesExit.class);

    /**
     * Our standard application exit response is just a 
     * "Goodbye" from Alexa without any associated
     * {@link com.amazon.speech.ui.Card}.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @return "silence"
     */
    public static SpeechletResponse buildExitResponse(SessionDao sessionDao) {
    	log.info("buildExitResponse: configInProgress={}", sessionDao.getScheduleConfigInProgress());
    	
		Calendar calendar = sessionDao.getCalendar();
    	if (sessionDao.getScheduleConfigInProgress() && (calendar != null) && (! calendar.isEmpty()) ) {
    		return SpeechletResponse.newTellResponse(outputSpeech(Phrase.SCHEDULE_DONE_VERBAL));
    	} else {
    		return SpeechletResponse.newTellResponse(outputSpeech(Phrase.EXIT_VERBAL));
    	}
    }
}
