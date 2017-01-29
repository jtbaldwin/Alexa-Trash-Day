package trashday.ui.responses;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.SpeechletResponse;
import trashday.CoberturaIgnore;
import trashday.model.NextPickups;
import trashday.model.Schedule;
import trashday.storage.SessionDao;
import trashday.ui.DateTimeOutputUtils;

/**
 * Organizes all the voice responses when talking about
 * the Trash Day Schedule itself.
 * 
 * @author	J. Todd Baldwin
 */
public class ResponsesSchedule extends ResponseHelpers {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ResponsesSchedule.class);
	
	public static SpeechletResponse askWelcomeMenu() {
    	log.info("askWelcomeMenu()");
        return SpeechletResponse.newAskResponse(
        		outputSpeech(Phrases.OPEN_VERBAL), 
        		reprompt(Phrases.OPEN_REPROMPT));
	}

	/**
	 * Respond when user attempts first opens the application and there is
	 * no user configuration set, tell them "Welcome" and have them start 
	 * a configuration dialog.
	 * 
	 * @return
	 * 			Verbal "Welcome" and "Please set time zone"
	 */
	public static SpeechletResponse askInitialConfiguration() {
    	log.info("askInitialConfiguration()");
        return SpeechletResponse.newAskResponse(
        		outputSpeech("Welcome to Trash Day. Please " + Phrases.TIME_ZONE_SET_VERBAL), 
        		reprompt(Phrases.TIME_ZONE_SET_REPROMPT));
	}

	/**
	 * Respond when user attempts something that requires a
	 * time zone, but that time zone is missing.  Tell them and 
	 * give help on how to set the time zone.
	 * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * 
	 * @return
	 * 			Verbal "Time zone not configured" and
	 * 			how to configure with a corresponding {@link com.amazon.speech.ui.Card}
	 */
	public static SpeechletResponse askTimeZoneMissing(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("askTimeZoneMissing(withHelpCard={})", withHelpCard);
    	if (withHelpCard && (! sessionDao.getTimeZoneHelpCardSent())) {
    		sessionDao.setTimeZoneHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech("The time zone isn't configured yet. "+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX),
	        		reprompt(Phrases.TIME_ZONE_SET_REPROMPT), 
	        		card("Trash Day Set Time Zone", Phrases.TIME_ZONE_SET_HELP_CARD));
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech("The time zone isn't configured yet. "+Phrases.TIME_ZONE_SET_VERBAL),
	        		reprompt(Phrases.TIME_ZONE_SET_REPROMPT));
    	}
	}
	
	/**
	 * Respond when user attempts something that requires a
	 * schedule, but that schedule is empty, tell them and 
	 * give help on how to add pickups to
	 * make a schedule.
	 * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * 
	 * @return
	 * 			Verbal "Schedule empty" and
	 * 			how to add with a corresponding {@link com.amazon.speech.ui.Card}
	 */
    public static SpeechletResponse askScheduleEmpty(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("askScheduleEmpty(withHelpCard={})", withHelpCard);
    	
    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
			sessionDao.setScheduleHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX),
       				reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT),
       				card("Trash Day Change Schedule", Phrases.SCHEDULE_ALTER_CARD));
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech("The pickup schedule is empty. "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL),
	        		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT));
    	}
    }
    
	/**
	 * Respond when user gives "update schedule" command.
	 * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * 
	 * @return
	 * 			Verbal quick add instructions
	 */
    public static SpeechletResponse askScheduleChange(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("askScheduleChange(withHelpCard={})", withHelpCard);
    	
    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
			sessionDao.setScheduleHelpCardSent();
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrases.SCHEDULE_ALTER_VERBAL.toString()+Phrases.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX),
	        		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT),
	        		card("Trash Day Change Schedule", Phrases.SCHEDULE_ALTER_CARD)); 
    	} else {
	        return SpeechletResponse.newAskResponse(
	        		outputSpeech(Phrases.SCHEDULE_ALTER_VERBAL),
	        		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT)); 
    	}
    }
    
    /**
     * Respond when user just tried to set time zone, but they did not give
     * data required.
     * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param missingDataFields List of missing data field names
     * @return Verbal, and possible card-based, help
     */
    public static SpeechletResponse respondSetTimeZoneMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondSetTimeZoneMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		
		// Uhoh, missing some information on add schedule.
		String missingDataString = "I missed the ";
		if (missingDataFields.size() == 1) {
			missingDataString = missingDataString + missingDataFields.get(0);
		//} else if (missingDataFields.size() == 2) {
		//	missingDataString = missingDataString + missingDataFields.get(0) + " and " + missingDataFields.get(1);
		//} else if (missingDataFields.size() == 3) {
		//	missingDataString = missingDataString + missingDataFields.get(0) + ", " + missingDataFields.get(1) + ", and " + missingDataFields.get(2);    			
		}
		missingDataString = missingDataString + " information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getTimeZoneHelpCardSent())) {
				sessionDao.setTimeZoneHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.TIME_ZONE_SET_REPROMPT), 
	            		card(Phrases.TIME_ZONE_SET_HELP_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.TIME_ZONE_SET_VERBAL), 
	            		reprompt(Phrases.TIME_ZONE_SET_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getTimeZoneHelpCardSent())) {
				sessionDao.setTimeZoneHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX),
	            		card(Phrases.TIME_ZONE_SET_HELP_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(outputSpeech(missingDataString+Phrases.TIME_ZONE_SET_VERBAL));
			}
		}
    }
    
    /**
     * Respond when user just tried to set time zone, but the time zone spoken was
     * not understood.
     * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param invalidDataFields List of missing data field names
     * @return Verbal, and possible card-based, help
     */
    public static SpeechletResponse respondSetTimeZoneInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondSetTimeZoneInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		
		// Got some invalid values.
		String invalidDataString = "I didn't understand the ";
		if (invalidDataFields.size() == 1) {
			invalidDataString = invalidDataString + invalidDataFields.get(0);
		//} else if (invalidDataFields.size() == 2) {
		//	invalidDataString = invalidDataString + invalidDataFields.get(0) + " and " + invalidDataFields.get(1);    			
		//} else if (invalidDataFields.size() == 3) {
		//	invalidDataString = invalidDataString + invalidDataFields.get(0) + ", " + invalidDataFields.get(1) + ", and " + invalidDataFields.get(2);    			
		}
		invalidDataString = invalidDataString + " information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getTimeZoneHelpCardSent())) {
				sessionDao.setTimeZoneHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX),
	            		reprompt(Phrases.TIME_ZONE_SET_REPROMPT),
	            		card(Phrases.TIME_ZONE_SET_HELP_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.TIME_ZONE_SET_VERBAL),
	            		reprompt(Phrases.TIME_ZONE_SET_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getTimeZoneHelpCardSent())) {
				sessionDao.setTimeZoneHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(invalidDataString+Phrases.TIME_ZONE_SET_VERBAL+Phrases.TIME_ZONE_SET_VERBAL_CARD_SUFFIX),
	            		card(Phrases.TIME_ZONE_SET_HELP_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(invalidDataString+Phrases.TIME_ZONE_SET_VERBAL));
			}
		}
    }

    /**
     * Respond when user just successfully updated the time zone, but there is no
     * schedule configured.
     * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param timeZone {@link java.util.TimeZone} Configured time zone
     * @return Verbal "Time Zone set" and "Next, [add schedule instructions]".  Possible include a help card
     */
    public static SpeechletResponse respondTimeZoneUpdatedScheduleMissing(SessionDao sessionDao, boolean withHelpCard, TimeZone timeZone) {
    	log.info("respondTimeZoneUpdatedScheduleMissing: withHelpCard={}, timeZone={}", withHelpCard, timeZone.getID());
    	String timeZoneSet = "Time zone set to " + timeZone.getID() + ".";
    	
    	if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
		        return SpeechletResponse.newAskResponse(
		        		outputSpeech(timeZoneSet+" Next, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX),
		        		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT),
		        		card("Trash Day Change Schedule", Phrases.SCHEDULE_ALTER_CARD));
    		} else {
		        return SpeechletResponse.newAskResponse(
		        		outputSpeech(timeZoneSet+" Next, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL),
		        		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT));    			
    		}
    	} else {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
		        return SpeechletResponse.newTellResponse(
		        		outputSpeech(timeZoneSet+" Next, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX),
		        		card("Trash Day Change Schedule", Phrases.SCHEDULE_ALTER_CARD));
    		} else {
		        return SpeechletResponse.newTellResponse(
		        		outputSpeech(timeZoneSet+" Next, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL));
    		}
    	}
    }
	
    /**
     * Respond when user just successfully updated the time zone and a schedule is
     * already configured.
     * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param timeZone {@link java.util.TimeZone} Configured time zone
     * @return Verbal "Time Zone set" and, if already in a configuration conversation, "Next, [add schedule instructions]".  Possible include a help card
     */
    public static SpeechletResponse respondTimeZoneUpdatedScheduleExists(SessionDao sessionDao, boolean withHelpCard, TimeZone timeZone) {
    	log.info("respondTimeZoneUpdatedScheduleExists: withHelpCard={}, timeZone={}", withHelpCard, timeZone.getID());
    	String timeZoneSet = "Time zone set to " + timeZone.getID() + ".";
    	
    	if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(timeZoneSet+" Next, "+Phrases.SCHEDULE_ALTER_VERBAL+Phrases.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT),
	            		card("Trash Day Change Schedule", Phrases.SCHEDULE_ALTER_CARD));    		
    		} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(timeZoneSet+" Next, "+Phrases.SCHEDULE_ALTER_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT));    		
    		}
    	} else {
			return SpeechletResponse.newTellResponse(outputSpeech(timeZoneSet));
    	}
    }

    /**
	 * Respond when user asks to hear the schedule.
	 * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withDataCard boolean Send user an Alexa data card with this information.
     * @param request IntentRequest
	 * 			State the next pickup(s) after this request's date/time.
     * @param timeZone TimeZone
	 * 			Time zone for user requests.
     * @param schedule {@link trashday.model.Schedule} 
	 * 			User's weekly pickup schedule.
     * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} listing the entire weekly 
	 * 		pickup schedule.
	 */
    public static SpeechletResponse tellSchedule(SessionDao sessionDao, boolean withDataCard, IntentRequest request, TimeZone timeZone, Schedule schedule) {
    	log.info("respondScheduleTell(withDataCard={}) schedule={}", withDataCard, schedule.toStringPrintable());
    	
    	if (sessionDao.getScheduleConfigInProgress()) {
            return SpeechletResponse.newAskResponse(
            		outputSpeech(schedule.toStringVerbal()+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL),
            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT));
    	} else {
	    	if (withDataCard) {
		    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
		        String cardText;
		        if (ldtRequest == null) {
		        	cardText=schedule.toStringPrintable();
		        } else {
		        	cardText="As of " + DateTimeOutputUtils.printableDateAndTime(ldtRequest) + ":\n" + schedule.toStringPrintable();
		        }
		        return SpeechletResponse.newTellResponse(
		        		outputSpeech(schedule.toStringVerbal()), 
		        		card("Trash Day Schedule", cardText));
	    	} else {
	            return SpeechletResponse.newTellResponse(outputSpeech(schedule.toStringVerbal()));
	    	}
    	}
    }
    
	/**
	 * Respond when user asks to hear the next pickup(s) for a given pickup name.
	 * 
     * @param withDataCard boolean Send user an Alexa data card with this information.
	 * @param requestDate Date
	 * 			State the next pickup(s) after this date/time.
	 * @param timeZone TimeZone
	 * 			Time zone for user requests.
	 * @param nextPickups {@link trashday.model.NextPickups} mapping
	 * 			pickup names to the next pickup time (after
	 * 			ldtStartingPoint).
	 * @param pickupName String
	 * 			if non-null, state pickup time for only this
	 * 			pickup name.  if null, state for all pickups
	 * 			in the schedule.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} listing the next pickup 
	 * 		time for each schedule pickup.
	 */
    public static SpeechletResponse tellOneNextPickup(boolean withDataCard, Date requestDate, TimeZone timeZone, NextPickups nextPickups, String pickupName) {
    	log.info("tellOneNextPickup(withDataCard={}, pickupName={}", withDataCard, pickupName);
    	
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(requestDate, timeZone);
    	String speechText;
    	String cardText;
    	if (nextPickups.getPickupCount()==0) {
    		log.info("{} pickup is not scheduled.", pickupName);
    		speechText = "No "+pickupName+" pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"";
    		cardText = "As of " + DateTimeOutputUtils.printableDateAndTime(ldtRequest) + ", no "+pickupName+" pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"";
    	} else {
    		speechText = nextPickups.toStringVerbal();
    		cardText = "As of " + DateTimeOutputUtils.printableDateAndTime(ldtRequest) + ":\n" + nextPickups.toStringPrintable(ldtRequest);
    		log.info(cardText);
    	}
    	log.trace("tellOneNextPickup: Next Pickup after {} ({})", ldtRequest, ldtRequest.getDayOfWeek());

    	if (withDataCard) {
	        return SpeechletResponse.newTellResponse(
	        		outputSpeech(speechText), 
	        		card("Trash Day Pickups", cardText));
    	} else {
	        return SpeechletResponse.newTellResponse(outputSpeech(speechText));
    	}
    }
    
	/**
	 * Respond when user asks to hear all the next pickup(s).
	 * 
     * @param withDataCard boolean Send user an Alexa data card with this information.
	 * @param requestDate Date
	 * 			State the next pickup(s) after this date/time.
	 * @param timeZone TimeZone
	 * 			Time zone for user requests.
	 * @param nextPickups {@link trashday.model.NextPickups} mapping
	 * 			pickup names to the next pickup time (after
	 * 			ldtStartingPoint).
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} listing the next pickup 
	 * 		time for each schedule pickup.
	 */
    public static SpeechletResponse tellAllNextPickups(boolean withDataCard, Date requestDate, TimeZone timeZone, NextPickups nextPickups) {
    	log.info("tellAllNextPickups(withCard={})", withDataCard);
    	
    	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(requestDate, timeZone);
    	String speechText;
    	String cardText;
    	if (nextPickups.getPickupCount()==0) {
			speechText = "No pickups are scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"";
			cardText = "As of " + DateTimeOutputUtils.printableDateAndTime(ldtRequest) + ", no pickups are scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"\n";
    	} else {
    		speechText = nextPickups.toStringVerbal();
    		cardText = "As of " + DateTimeOutputUtils.printableDateAndTime(ldtRequest) + ":\n" + nextPickups.toStringPrintable(ldtRequest);
    	}
    	log.trace("tellAllNextPickups: Next Pickup after {} ({})", ldtRequest, ldtRequest.getDayOfWeek());

    	if (withDataCard) {
	        return SpeechletResponse.newTellResponse(
	        		outputSpeech(speechText), 
	        		card("Trash Day Pickups", cardText));
    	} else {
	        return SpeechletResponse.newTellResponse(outputSpeech(speechText));
    	}
    }
    
    /**
     * Respond when the user has confirmed to delete the entire schedule.
     * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param removed boolean True if the schedule had entries that were actually removed by this user action.
     * @return Verbal "Schedule cleared"
     */
    public static SpeechletResponse respondScheduleDeleted(SessionDao sessionDao, boolean withHelpCard, boolean removed) {
    	log.info("respondScheduleDeleted(withHelpCard={}, removed={})", withHelpCard, removed);
    	String speechText;
    	if (removed) {
    		speechText = "Cleared entire schedule.";
    	} else {
    		speechText = "Schedule was already empty.";
    	}
    	
    	if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
    			return SpeechletResponse.newAskResponse(
    					outputSpeech(speechText+" To make changes, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
    		} else {
    			return SpeechletResponse.newAskResponse(
    					outputSpeech(speechText+" To make changes, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT));
    		}
    	} else {
	        return SpeechletResponse.newTellResponse(outputSpeech(speechText));
    	}
    }    
    
	/**
	 * Respond with an "Are you sure?" when user requests to 
	 * delete the entire schedule.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param schedule Schedule
	 * 		A schedule, which might be empty, that the user wants to be deleted.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} asking "Are you sure?"
	 */
    public static SpeechletResponse respondScheduleDeleteAllRequested(SessionDao sessionDao, boolean withHelpCard, Schedule schedule) {
    	log.info("respondScheduleDeleteAllRequested(withHelpCard={})", withHelpCard);
    	
    	if (schedule==null) {
            String speechText = "The schedule is already cleared.";
            
	    	if (sessionDao.getScheduleConfigInProgress()) {
		    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
					sessionDao.setScheduleHelpCardSent();
	    			return SpeechletResponse.newAskResponse(
	    					outputSpeech(speechText+" To make changes, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX), 
		            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT),
		            		card(Phrases.SCHEDULE_ALTER_CARD));
	    		} else {
	    			return SpeechletResponse.newAskResponse(
	    					outputSpeech(speechText+" To make changes, "+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL), 
	    					reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT));
	    		}
	    	} else {
	            return SpeechletResponse.newTellResponse(outputSpeech(speechText));
	    	}
    	}
    	
    	String speechText = "Are you sure you want to delete the entire schedule?";    	
        return SpeechletResponse.newAskResponse(
        		outputSpeech(speechText), 
        		reprompt(speechText));
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		
		// Uhoh, missing some information on add schedule.
		String missingDataString = "I missed the ";
		if (missingDataFields.size() == 1) {
			missingDataString = missingDataString + missingDataFields.get(0);
		} else if (missingDataFields.size() == 2) {
			missingDataString = missingDataString + missingDataFields.get(0) + " and " + missingDataFields.get(1);
		} else if (missingDataFields.size() == 3) {
			missingDataString = missingDataString + missingDataFields.get(0) + ", " + missingDataFields.get(1) + ", and " + missingDataFields.get(2);    			
		}
		missingDataString = missingDataString + " information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL));
			}
		}
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		
		// Got some invalid values.
		String invalidDataString = "I didn't understand the ";
		if (invalidDataFields.size() == 1) {
			invalidDataString = invalidDataString + invalidDataFields.get(0);
		} else if (invalidDataFields.size() == 2) {
			invalidDataString = invalidDataString + invalidDataFields.get(0) + " and " + invalidDataFields.get(1);    			
		} else if (invalidDataFields.size() == 3) {
			invalidDataString = invalidDataString + invalidDataFields.get(0) + ", " + invalidDataFields.get(1) + ", and " + invalidDataFields.get(2);    			
		}
		invalidDataString = invalidDataString + " information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ADD_PICKUPS_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
		        return SpeechletResponse.newTellResponse(
		        		outputSpeech(invalidDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX), 
		        		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
		        return SpeechletResponse.newTellResponse(
		        		outputSpeech(invalidDataString+Phrases.SCHEDULE_ADD_PICKUPS_VERBAL));
			}
		}
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param dow DayOfWeek The pickup day of week.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddSingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, DayOfWeek dow, LocalTime tod) {
    	log.info("respondPickupAddSingle withHelpCard={} pickupName={} dow={} tod={}",withHelpCard,pickupName,dow,tod);
    	String addedPickupTimeString = "Added " + pickupName + " pickup on " + DateTimeOutputUtils.verbalDayOfWeekAndTime(dow, tod) + ".";
    	log.trace("respondPickupAddSingle {}", addedPickupTimeString);
    	
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(addedPickupTimeString+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL+Phrases.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(addedPickupTimeString+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT));        
			}
		} else {
            return SpeechletResponse.newTellResponse(outputSpeech(addedPickupTimeString));        
		}
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		
		// Uhoh, missing some information on add schedule.
		String missingDataString = "I missed the ";
		if (missingDataFields.size() == 1) {
			missingDataString = missingDataString + missingDataFields.get(0);
		} else if (missingDataFields.size() == 2) {
			missingDataString = missingDataString + missingDataFields.get(0) + " and " + missingDataFields.get(1);    			
		} else if (missingDataFields.size() == 3) {
			missingDataString = missingDataString + missingDataFields.get(0) + ", " + missingDataFields.get(1) + ", and " + missingDataFields.get(2);    			
		}
		missingDataString = missingDataString + " information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_DELETE_PICKUPS_REPROMPT), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_DELETE_PICKUPS_REPROMPT));
			}
		} else {
			if (withHelpCard) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL));
				
			}
		}
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		
		// Got some invalid values.
		String invalidDataString = "I didn't understand the ";
		if (invalidDataFields.size() == 1) {
			invalidDataString = invalidDataString + invalidDataFields.get(0);
		} else if (invalidDataFields.size() == 2) {
			invalidDataString = invalidDataString + invalidDataFields.get(0) + " and " + invalidDataFields.get(1);    			
		} else if (invalidDataFields.size() == 3) {
			invalidDataString = invalidDataString + invalidDataFields.get(0) + ", " + invalidDataFields.get(1) + ", and " + invalidDataFields.get(2);    			
		}
		invalidDataString = invalidDataString + " information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_DELETE_PICKUPS_REPROMPT), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_DELETE_PICKUPS_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_PICKUPS_VERBAL));
			}
		}
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param dow DayOfWeek The pickup day of week.
	 * @param tod TimeOfDay The pickup time of day.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup time.
	 */
    public static SpeechletResponse respondPickupDeleteSingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, DayOfWeek dow, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteSingle withHelpCard={} pickupName={} dow={} tod={}",withHelpCard,pickupName,dow,tod);
    	String speechText;
    	if (removed) {
    		speechText = "Removed " + pickupName + " pickup on " + DateTimeOutputUtils.verbalDayOfWeekAndTime(dow,tod) + " from the weekly schedule.";
    	} else {
    		speechText = "There was no " + pickupName + " pickup scheduled on " + DateTimeOutputUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	}
    	log.info("respondPickupDeleteSingle " + speechText);
    	
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(speechText+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL+Phrases.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX),
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(speechText+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT));
			}
		} else {
			return SpeechletResponse.newTellResponse(outputSpeech(speechText));
		}
    }
    
	/**
	 * Respond when user tries to delete entire pickup from the schedule but is
	 * missing essential data like pickup name.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondEntirePickupDeleteMissingName(SessionDao sessionDao, boolean withHelpCard) {
		log.info("respondEntirePickupDeleteMissingName(withHelpCard={})",withHelpCard);
		
		// Uhoh, missing some information on delete pickup.
		String missingDataString = "I missed the pickup name information.\n";
		
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(missingDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL));
			}
		}
    }
    
	/**
	 * Respond when user tries to delete an entire pickup from the schedule but gave
	 * invalid data for required fields like pickup name.
	 * <p>
	 * CoberturaIgnore this method since we don't have any
	 * rules that would make a pickup name invalid.  However,
	 * method is written in case we find something in the
	 * future.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    @CoberturaIgnore
    public static SpeechletResponse respondEntirePickupDeleteInvalidName(SessionDao sessionDao, boolean withHelpCard) {
		log.info("respondEntirePickupDeleteInvalidName(withHelpCard={})", withHelpCard);
		
		// Uhoh, missing some information on pickup delete.
		String invalidDataString = "I didn't understand the pickup name information.\n";
        
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT), 
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT));
			}
		} else {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newTellResponse(
	            		outputSpeech(invalidDataString+Phrases.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL));
			}
		}
    }
    
	/**
	 * Respond when user successfully deletes an entire pickup from the schedule.
     * @param sessionDao {@link trashday.storage.SessionDao} Alexa session used for storing state information for the current dialog with the user.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup.
	 */
    public static SpeechletResponse respondEntirePickupDelete(SessionDao sessionDao, boolean withHelpCard, String pickupName, boolean removed) {
    	log.info("respondEntirePickupDelete withHelpCard={} pickupName={} removed={}",withHelpCard,pickupName,removed);
    	String speechText;
    	if (removed) {
    		speechText = "Removed all " + pickupName + " pickups from the schedule.";
    	} else {
    		speechText = "There was no " + pickupName + " pickup in the schedule.";
    	}
    	log.trace("respondEntirePickupDelete {}", speechText);
    	
		if (sessionDao.getScheduleConfigInProgress()) {
	    	if (withHelpCard && (! sessionDao.getScheduleHelpCardSent())) {
				sessionDao.setScheduleHelpCardSent();
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(speechText+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL+Phrases.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT),
	            		card(Phrases.SCHEDULE_ALTER_CARD));
			} else {
	            return SpeechletResponse.newAskResponse(
	            		outputSpeech(speechText+" To make more changes, "+Phrases.SCHEDULE_ALTER_VERBAL), 
	            		reprompt(Phrases.SCHEDULE_ALTER_REPROMPT));
			}
		} else {
			return SpeechletResponse.newTellResponse(outputSpeech(speechText));
		}
    }
    
}
