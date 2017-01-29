package trashday.ui.responses;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;

import trashday.model.Schedule;
import trashday.storage.SessionDao;

/**
 * Organizes all the voice responses for Alexa "exit" intents.
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
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @return "silence"
     */
    public static SpeechletResponse buildExitResponse(SessionDao sessionDao) {
    	log.info("buildExitResponse: configInProgress={}", sessionDao.getScheduleConfigInProgress());
    	
		Schedule schedule = sessionDao.getSchedule();
    	if (sessionDao.getScheduleConfigInProgress() && (schedule != null) && (! schedule.isEmpty()) ) {
    		return SpeechletResponse.newTellResponse(outputSpeech(Phrases.SCHEDULE_DONE_VERBAL));
    	} else {
    		return SpeechletResponse.newTellResponse(outputSpeech(Phrases.EXIT_VERBAL));
    	}
    }
}
