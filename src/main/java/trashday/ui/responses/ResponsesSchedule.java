package trashday.ui.responses;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.SpeechletResponse;
import trashday.CoberturaIgnore;
import trashday.model.Calendar;
import trashday.model.DateTimeUtils;
import trashday.model.NextPickups;
import trashday.storage.SessionDao;
import trashday.ui.FormatUtils;

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
        		outputSpeech(Phrase.OPEN_VERBAL), 
        		reprompt(Phrase.OPEN_REPROMPT));
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
        		outputSpeech("Welcome to Trash Day. Please " + Phrase.TIME_ZONE_SET_VERBAL), 
        		reprompt(Phrase.TIME_ZONE_SET_REPROMPT));
	}

	/**
	 * Respond when user attempts something that requires a
	 * time zone, but that time zone is missing.  Tell them and 
	 * give help on how to set the time zone.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * 
	 * @return
	 * 			Verbal "Time zone not configured" and
	 * 			how to configure with a corresponding {@link com.amazon.speech.ui.Card}
	 */
	public static SpeechletResponse askTimeZoneMissing(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("askTimeZoneMissing(withHelpCard={})", withHelpCard);
    	
		return generalAskResponse(
				sessionDao,
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, // Flag for which help card
				"The time zone isn't configured yet. ", // A verbal prefix string
				Phrase.TIME_ZONE_SET_VERBAL, // The main verbal phrase
				Phrase.TIME_ZONE_SET_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				Phrase.TIME_ZONE_SET_REPROMPT, // A reprompt phrase if an Ask response
				"Trash Day Set Time Zone", Phrase.TIME_ZONE_HELP_CARD // The card body, if it needs to be sent
				);
	}
	
	/**
	 * Respond when user attempts something that requires a
	 * schedule, but that schedule is empty, tell them and 
	 * give help on how to add pickups to
	 * make a schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * 
	 * @return
	 * 			Verbal "Schedule empty" and
	 * 			how to add with a corresponding {@link com.amazon.speech.ui.Card}
	 */
    public static SpeechletResponse askScheduleEmpty(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("askScheduleEmpty(withHelpCard={})", withHelpCard);
    	
		return generalAskResponse(
				sessionDao,
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
				"The pickup schedule is empty. ", // A verbal prefix string
				Phrase.SCHEDULE_ADD_PICKUPS_VERBAL, // The main verbal phrase
				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT, // A reprompt phrase if an Ask response
				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
				);
    }
    
	/**
	 * Respond when user gives "update schedule" command.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * 
	 * @return
	 * 			Verbal quick add instructions
	 */
    public static SpeechletResponse askScheduleChange(SessionDao sessionDao, boolean withHelpCard) {
    	log.info("askScheduleChange(withHelpCard={})", withHelpCard);
    	
		return generalAskResponse(
				sessionDao,
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
				null, // A verbal prefix string
				Phrase.SCHEDULE_ALTER_VERBAL, // The main verbal phrase
				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				Phrase.SCHEDULE_ALTER_REPROMPT, // A reprompt phrase if an Ask response
				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
				);
    }
    
    /**
     * Respond when user just tried to set time zone, but they did not give
     * data required.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param missingDataFields List of missing data field names
     * @return Verbal, and possible card-based, help
     */
    public static SpeechletResponse respondSetTimeZoneMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondSetTimeZoneMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		
		// Uhoh, missing some information on add schedule.
		String missingDataString = FormatUtils.formattedJoin(missingDataFields, "I missed the ", " information.\n");
		
		return generalAskOrTellResponse(
				sessionDao,
				sessionDao.getScheduleConfigInProgress(), // Ask if Config-in-Progress
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, // Flag for which help card
				missingDataString, // A verbal prefix string
				Phrase.TIME_ZONE_SET_VERBAL, // The main verbal phrase
				Phrase.TIME_ZONE_SET_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				Phrase.TIME_ZONE_SET_REPROMPT, // A reprompt phrase if an Ask response
				"Trash Day Set Time Zone", Phrase.TIME_ZONE_HELP_CARD // The card body, if it needs to be sent
				);
    }
    
    /**
     * Respond when user just tried to set time zone, but the time zone spoken was
     * not understood.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param invalidDataFields List of missing data field names
     * @return Verbal, and possible card-based, help
     */
    public static SpeechletResponse respondSetTimeZoneInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondSetTimeZoneInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		
		// Got some invalid values.
		String invalidDataString = FormatUtils.formattedJoin(invalidDataFields, "I didn't understand the ", " information.\n");
		
		return generalAskOrTellResponse(
				sessionDao,
				sessionDao.getScheduleConfigInProgress(), // Ask if Config-in-Progress
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_TIMEZONE_HELP_CARD_SENT, // Flag for which help card
				invalidDataString, // A verbal prefix string
				Phrase.TIME_ZONE_SET_VERBAL, // The main verbal phrase
				Phrase.TIME_ZONE_SET_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				Phrase.TIME_ZONE_SET_REPROMPT, // A reprompt phrase if an Ask response
				"Trash Day Set Time Zone", Phrase.TIME_ZONE_HELP_CARD // The card body, if it needs to be sent
				);
    }

    /**
     * Respond when user just successfully updated the time zone, but there is no
     * schedule configured.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param timeZone {@link java.util.TimeZone} Configured time zone
     * @return Verbal "Time Zone set" and "Next, [add schedule instructions]".  Possible include a help card
     */
    public static SpeechletResponse respondTimeZoneUpdatedScheduleMissing(SessionDao sessionDao, boolean withHelpCard, TimeZone timeZone) {
    	log.info("respondTimeZoneUpdatedScheduleMissing: withHelpCard={}, timeZone={}", withHelpCard, timeZone.getID());
    	String timeZoneSet = "Time zone set to " + timeZone.getID() + ".";
    	
		return generalAskOrTellResponse(
				sessionDao,
				sessionDao.getScheduleConfigInProgress(), // Ask if Config-in-Progress
				withHelpCard, // Send help card if true and not already sent in this Session.
				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
				timeZoneSet+" Next, ", // A verbal prefix string
				Phrase.SCHEDULE_ADD_PICKUPS_VERBAL, // The main verbal phrase
				Phrase.SCHEDULE_ALTER_VERBAL_CARD_SUFFIX, // An "added card" verbal suffix
				Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT, // A reprompt phrase if an Ask response
				"Trash Day Change Schedule", Phrase.SCHEDULE_ALTER_CARD // The card body, if it needs to be sent
				);
    }
	
    /**
     * Respond when user just successfully updated the time zone and a schedule is
     * already configured.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
     * @param timeZone {@link java.util.TimeZone} Configured time zone
     * @return Verbal "Time Zone set" and, if already in a configuration conversation, "Next, [add schedule instructions]".  Possible include a help card
     */
    public static SpeechletResponse respondTimeZoneUpdatedScheduleExists(SessionDao sessionDao, boolean withHelpCard, TimeZone timeZone) {
    	log.info("respondTimeZoneUpdatedScheduleExists: withHelpCard={}, timeZone={}", withHelpCard, timeZone.getID());
    	String timeZoneSet = "Time zone set to " + timeZone.getID() + ".";
    	
    	if (sessionDao.getScheduleConfigInProgress()) {
    		return generalAskResponse(
    				sessionDao,
    				withHelpCard, // Send help card if true and not already sent in this Session.
    				SessionDao.SESSION_ATTR_SCHEDULE_HELP_CARD_SENT, // Flag for which help card
    				timeZoneSet+" Next, ", // A verbal prefix string
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
					timeZoneSet, // A verbal prefix string
					(String) null,
					(String) null,
					null, (String) null
					);
    	}
    }

    /**
	 * Respond when user asks to hear the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withDataCard boolean Send user an Alexa data card with this information.
     * @param ldtRequest {@link LocalDateTime} when this request occurred.
     * @param calendar {@link trashday.model.Calendar} 
	 * 			User's weekly pickup schedule.
     * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} listing the entire weekly 
	 * 		pickup schedule.
	 */
    public static SpeechletResponse tellSchedule(SessionDao sessionDao, boolean withDataCard, LocalDateTime ldtRequest, Calendar calendar) {
    	log.info("tellSchedule(withDataCard={}, ldtRequest={}) calendar={}", withDataCard, ldtRequest, FormatUtils.printableCalendar(calendar,ldtRequest));
    	
    	if (sessionDao.getScheduleConfigInProgress()) {
            return SpeechletResponse.newAskResponse(
            		outputSpeech(FormatUtils.verbalCalendar(calendar, ldtRequest)+" To make more changes, "+Phrase.SCHEDULE_ALTER_VERBAL),
            		reprompt(Phrase.SCHEDULE_ALTER_REPROMPT));
    	} else {
	        String cardText="As of " + FormatUtils.printableDateAndTime(ldtRequest) + ":\n" + FormatUtils.printableCalendar(calendar,ldtRequest);
	    	return generalTellResponse(
	    			sessionDao, 
	    			withDataCard, 
	    			null, 
	    			FormatUtils.verbalCalendar(calendar, ldtRequest), 
	    			null,
	    			null, 
	    			"Trash Day Schedule", 
	    			cardText
	    			);
    	}
    }
    
	/**
	 * Respond when user asks to hear the next pickup(s) for a given pickup name.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
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
	 * 
     * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} listing the next pickup 
	 * 		time for each schedule pickup.
	 */
    public static SpeechletResponse tellOneNextPickup(SessionDao sessionDao, boolean withDataCard, Date requestDate, TimeZone timeZone, NextPickups nextPickups, String pickupName) {
    	log.info("tellOneNextPickup(withDataCard={}, pickupName={}", withDataCard, pickupName);
    	
    	LocalDateTime ldtRequest = DateTimeUtils.getLocalDateTime(requestDate, timeZone);
    	String speechText;
    	String cardText;
    	if (nextPickups.getPickupCount()==0) {
    		log.info("{} pickup is not scheduled.", pickupName);
    		speechText = "No "+pickupName+" pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"";
    		cardText = "As of " + FormatUtils.printableDateAndTime(ldtRequest) + ", no "+pickupName+" pickup is scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"";
    	} else {
    		speechText = FormatUtils.verbalNextPickups(nextPickups);
    		cardText = "As of " + FormatUtils.printableDateAndTime(ldtRequest) + ":\n" + FormatUtils.printableNextPickups(nextPickups);
    		log.info(cardText);
    	}
    	log.trace("tellOneNextPickup: Next Pickup after {} ({})", ldtRequest, ldtRequest.getDayOfWeek());

    	return generalTellResponse(
    			sessionDao, 
    			withDataCard, 
    			null, 
    			speechText, 
    			null,
    			null, 
    			"Trash Day Pickups", 
    			cardText
    			);
    }
    
	/**
	 * Respond when user asks to hear all the next pickup(s).
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
	 * @param withDataCard boolean Send user an Alexa data card with this information.
	 * @param requestDate Date
	 * 			State the next pickup(s) after this date/time.
	 * @param timeZone TimeZone
	 * 			Time zone for user requests.
	 * @param nextPickups {@link trashday.model.NextPickups} mapping
	 * 			pickup names to the next pickup time (after
	 * 			ldtStartingPoint).
	 * 
     * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} listing the next pickup 
	 * 		time for each schedule pickup.
	 */
    public static SpeechletResponse tellAllNextPickups(SessionDao sessionDao, boolean withDataCard, Date requestDate, TimeZone timeZone, NextPickups nextPickups) {
    	log.info("tellAllNextPickups(withCard={})", withDataCard);
    	
    	LocalDateTime ldtRequest = DateTimeUtils.getLocalDateTime(requestDate, timeZone);
    	String speechText;
    	String cardText;
    	if (nextPickups.getPickupCount()==0) {
			speechText = "No pickups are scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"";
			cardText = "As of " + FormatUtils.printableDateAndTime(ldtRequest) + ", no pickups are scheduled. Add schedule pickups by saying: \"Alexa, tell Trash Day to change schedule.\"\n";
    	} else {
    		speechText = FormatUtils.verbalNextPickups(nextPickups);
    		cardText = "As of " + FormatUtils.printableDateAndTime(ldtRequest) + ":\n" + FormatUtils.printableNextPickups(nextPickups);
    	}
    	log.trace("tellAllNextPickups: Next Pickup after {} ({})", ldtRequest, ldtRequest.getDayOfWeek());

    	return generalTellResponse(
    			sessionDao, 
    			withDataCard, 
    			null, 
    			speechText, 
    			null,
    			null, 
    			"Trash Day Pickups", 
    			cardText
    			);
    }
    
    /**
     * Respond when the user has confirmed to delete the entire schedule.
     * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
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
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, speechText);
    }    
    
	/**
	 * Respond with an "Are you sure?" when user requests to 
	 * delete the entire schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param alreadyClear boolean True if schedule is already cleared.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} asking "Are you sure?"
	 */
    public static SpeechletResponse respondScheduleDeleteAllRequested(SessionDao sessionDao, boolean withHelpCard, boolean alreadyClear) {
    	log.info("respondScheduleDeleteAllRequested(withHelpCard={})", withHelpCard);
    	
    	if (alreadyClear) {
            String speechText = "The schedule is already cleared.";
        	return respondGeneralScheduleChange(sessionDao, withHelpCard, speechText);
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
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param dow DayOfWeek The pickup day of week.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddSingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, DayOfWeek dow, LocalTime tod) {
    	log.info("respondPickupAddSingle withHelpCard={} pickupName={} dow={} tod={}",withHelpCard,pickupName,dow,tod);
    	String addedPickupTimeString = "Added weekly " + pickupName + " pickup on " + FormatUtils.verbalDayOfWeekAndTime(dow, tod) + ".";
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddWeeklyMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddWeeklyMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddWeeklyInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddWeeklyInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_WEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param dow DayOfWeek The pickup day of week.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddWeeklySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, DayOfWeek dow, LocalTime tod) {
    	log.info("respondPickupAddSingle withHelpCard={} pickupName={} dow={} tod={}",withHelpCard,pickupName,dow,tod);
    	String addedPickupTimeString = "Added weekly " + pickupName + " pickup on " + FormatUtils.verbalDayOfWeekAndTime(dow, tod) + ".";    	
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddBiWeeklyMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddBiWeeklyMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddBiWeeklyInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddBiWeeklyInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param nextWeek Boolean false setting the biweekly pickup on this day of week.  True if 
	 * 				setting on the next or following day of week.
	 * @param dow DayOfWeek The pickup day of week.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddBiWeeklySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Boolean nextWeek, DayOfWeek dow, LocalTime tod) {
    	log.info("respondPickupAddBiWeeklySingle withHelpCard={} pickupName={} nextWeek={} dow={} tod={}",withHelpCard,pickupName, nextWeek,dow,tod);
    	String addedPickupTimeString;
    	if (nextWeek) {
    		addedPickupTimeString = "Added biweekly " + pickupName + " pickup on " + FormatUtils.printableDayOfWeek(dow) + " after next at " + FormatUtils.verbalTime(tod) + ".";
    	} else {
    		addedPickupTimeString = "Added biweekly " + pickupName + " pickup on this " + FormatUtils.verbalDayOfWeekAndTime(dow, tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMonthlyMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_REPROMPT 
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddMonthlyInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param dom DayOfMonth The pickup day of the month.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddMonthlySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer dom, LocalTime tod) {
    	log.info("respondPickupAddMonthlySingle withHelpCard={} pickupName={} dom={} tod={}",withHelpCard,pickupName,dom,tod);
    	String addedPickupTimeString = "Added monthly " + pickupName + " pickup on the " + FormatUtils.verbalDayOfMonth(dom) + " at "+ FormatUtils.verbalTime(tod) + ".";
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyWeekdayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMonthlyWeekdayMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_REPROMPT 
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyWeekdayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddMonthlyWeekdayInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param weekOfMonth Integer week number
	 * @param dow DayOfWeek day of week
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddMonthlyWeekdaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer weekOfMonth, DayOfWeek dow, LocalTime tod) {
    	log.info("respondPickupAddMonthlyWeekdaySingle withHelpCard={} pickupName={} weekOfMonth={} dow={} tod={}",withHelpCard,pickupName,weekOfMonth,dow,tod);
    	String addedPickupTimeString = "Added monthly " + pickupName + " pickup on the "+FormatUtils.verbalWeekOfMonth(weekOfMonth)+" " +FormatUtils.printableDayOfWeek(dow)+" of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastNWeekdayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMonthlyLastNWeekdayMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT 
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastNWeekdayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddMonthlyLastNWeekdayInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param weekOfMonth Integer week number
	 * @param dow DayOfWeek day of week
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastNWeekdaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer weekOfMonth, DayOfWeek dow, LocalTime tod) {
    	log.info("respondPickupAddMonthlyLastNWeekdaySingle withHelpCard={} pickupName={} weekOfMonth={} dow={} tod={}",withHelpCard,pickupName,weekOfMonth,dow,tod);
    	String addedPickupTimeString = "Added monthly " + pickupName + " pickup on the "+FormatUtils.verbalWeekOfMonth(weekOfMonth)+" to last " +FormatUtils.printableDayOfWeek(dow)+" of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastNDayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMonthlyLastNDayMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_REPROMPT 
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastNDayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddMonthlyLastNDayInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param dom DayOfMonth The pickup day of the month.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastNDaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer dom, LocalTime tod) {
    	log.info("respondPickupAddMonthlyLastNDaySingle withHelpCard={} pickupName={} dom={} tod={}",withHelpCard,pickupName,dom,tod);
    	String addedPickupTimeString = "Added monthly " + pickupName + " pickup on the " + FormatUtils.verbalDayOfMonth(dom) + " at "+ FormatUtils.verbalTime(tod) + ".";
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastDayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupAddMonthlyLastDayMissingData(withHelpCard={}, missingDataFields={})", withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_REPROMPT 
				);
    }
    
	/**
	 * Respond when user tries to add a pickup time to the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule add commands.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastDayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupAddMonthlyLastDayInvalidData(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_VERBAL,
				Phrase.SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully adds a pickup time to the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup added.
	 * @param dom DayOfMonth The pickup day of the month.
	 * @param tod TimeOfDay The pickup time of day.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the added pickup.
	 */
    public static SpeechletResponse respondPickupAddMonthlyLastDaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer dom, LocalTime tod) {
    	log.info("respondPickupAddMonthlyLastDaySingle withHelpCard={} pickupName={} dom={} tod={}",withHelpCard,pickupName,dom,tod);
    	String addedPickupTimeString = "Added monthly " + pickupName + " pickup on the last day of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, addedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
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
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed " + pickupName + " pickup on " + FormatUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no " + pickupName + " pickup scheduled on " + FormatUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteWeeklyMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteWeeklyMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteWeeklyInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteWeeklyInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_WEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
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
    public static SpeechletResponse respondPickupDeleteWeeklySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, DayOfWeek dow, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteWeeklySingle withHelpCard={} pickupName={} dow={} tod={}",withHelpCard,pickupName,dow,tod);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed weekly " + pickupName + " pickup on " + FormatUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no weekly " + pickupName + " pickup scheduled on " + FormatUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteBiWeeklyMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteBiWeeklyMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteBiWeeklyInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteBiWeeklyInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
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
    public static SpeechletResponse respondPickupDeleteBiWeeklySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, DayOfWeek dow, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteBiWeeklySingle withHelpCard={} pickupName={} dow={} tod={}",withHelpCard,pickupName,dow,tod);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed biweekly " + pickupName + " pickup on " + FormatUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no biweekly " + pickupName + " pickup scheduled on " + FormatUtils.verbalDayOfWeekAndTime(dow,tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMonthlyMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteMonthlyInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param dom DayOfMonth The pickup day of the month.
	 * @param tod TimeOfDay The pickup time of day.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup time.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer dom, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteMonthlySingle withHelpCard={} pickupName={} dom={} tod={}",withHelpCard,pickupName,dom,tod);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed monthly " + pickupName + " pickup on the " + FormatUtils.verbalDayOfMonth(dom) + " at "+ FormatUtils.verbalTime(tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no monthly " + pickupName + " pickup scheduled on the " + FormatUtils.verbalDayOfMonth(dom) + " at "+ FormatUtils.verbalTime(tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastDayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMonthlyLastDayMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastDayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteMonthlyLastDayInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param dom DayOfMonth The pickup day of the month.
	 * @param tod TimeOfDay The pickup time of day.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup time.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastDaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer dom, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteMonthlyLastDaySingle withHelpCard={} pickupName={} dom={} tod={} removed={}",withHelpCard,pickupName,dom,tod,removed);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed monthly " + pickupName + " pickup on the last day of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no monthly " + pickupName + " pickup scheduled on the last day of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastNDayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMonthlyLastNDayMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastNDayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteMonthlyLastNDayInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param dom DayOfMonth The pickup day of the month.
	 * @param tod TimeOfDay The pickup time of day.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup time.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastNDaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer dom, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteMonthlyLastNDaySingle withHelpCard={} pickupName={} dom={} tod={}",withHelpCard,pickupName,dom,tod);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed monthly " + pickupName + " pickup on the " + FormatUtils.verbalDayOfMonth(dom) + " at "+ FormatUtils.verbalTime(tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no monthly " + pickupName + " pickup scheduled on the " + FormatUtils.verbalDayOfMonth(dom) + " at "+ FormatUtils.verbalTime(tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyWeekdayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMonthlyWeekdayMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyWeekdayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteMonthlyWeekdayInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param weekOfMonth Integer week number
	 * @param dow DayOfWeek day of week
	 * @param tod TimeOfDay The pickup time of day.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup time.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyWeekdaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer weekOfMonth, DayOfWeek dow, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteMonthlyWeekdaySingle withHelpCard={} pickupName={} weekOfMonth={} dow={} tod={}",withHelpCard,pickupName,weekOfMonth,dow,tod);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed monthly " + pickupName + " pickup on the "+FormatUtils.verbalWeekOfMonth(weekOfMonth)+" " +FormatUtils.printableDayOfWeek(dow)+" of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no monthly " + pickupName + " pickup scheduled on the "+FormatUtils.verbalWeekOfMonth(weekOfMonth)+" " +FormatUtils.printableDayOfWeek(dow)+" of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but is
	 * missing essential data like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastNWeekdayMissingData(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondPickupDeleteMonthlyLastNWeekdayMissingData (withHelpCard={}, missingDataFields={})",withHelpCard,missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete a pickup time from the schedule but gave
	 * invalid data for required fields like pickup name, week of month, day of week,
	 * or time of day.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastNWeekdayInvalidData(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondPickupDeleteMonthlyLastNWeekdayInvalidData (withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL,
				Phrase.SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes a pickup time from the schedule.
	 * 
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param pickupName String The pickup deleted.
	 * @param weekOfMonth Integer week number
	 * @param dow DayOfWeek day of week
	 * @param tod TimeOfDay The pickup time of day.
	 * @param removed boolean True if there was a pickup with this name 
	 * 			that got deleted.  False if the pickup did not 
	 * 			exist at all.
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with the deleted pickup time.
	 */
    public static SpeechletResponse respondPickupDeleteMonthlyLastNWeekdaySingle(SessionDao sessionDao, boolean withHelpCard, String pickupName, Integer weekOfMonth, DayOfWeek dow, LocalTime tod, boolean removed) {
    	log.info("respondPickupDeleteMonthlyLastNWeekdaySingle withHelpCard={} pickupName={} weekOfMonth={} dow={} tod={}",withHelpCard,pickupName,weekOfMonth,dow,tod);
    	String deletedPickupTimeString;
    	if (removed) {
    		deletedPickupTimeString = "Removed monthly " + pickupName + " pickup on the "+FormatUtils.verbalWeekOfMonth(weekOfMonth)+" to last " +FormatUtils.printableDayOfWeek(dow)+" of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	} else {
    		deletedPickupTimeString = "There was no monthly " + pickupName + " pickup scheduled on the "+FormatUtils.verbalWeekOfMonth(weekOfMonth)+" to last " +FormatUtils.printableDayOfWeek(dow)+" of the month at "+ FormatUtils.verbalTime(tod) + ".";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupTimeString);
    }
    
	/**
	 * Respond when user tries to delete entire pickup from the schedule but is
	 * missing essential data like pickup name.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param missingDataFields List which data fields are missing
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was missing
	 * 		and help for the schedule delete commands.
	 */
    public static SpeechletResponse respondEntirePickupDeleteMissingName(SessionDao sessionDao, boolean withHelpCard, List<String> missingDataFields) {
		log.info("respondEntirePickupDeleteMissingName(withHelpCard={}, missingDataFields={})",withHelpCard, missingDataFields);
		return respondGeneralMissingData(sessionDao, withHelpCard, missingDataFields,
				Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL,
				Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT
				);
    }
    
	/**
	 * Respond when user tries to delete an entire pickup from the schedule but gave
	 * invalid data for required fields like pickup name.
	 * <p>
	 * CoberturaIgnore this method since we don't have any
	 * rules that would make a pickup name invalid.  However,
	 * method is written in case we find something in the
	 * future.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
     * @param withHelpCard boolean Send user an Alexa help card if we haven't already done so in this session.
	 * @param invalidDataFields List which data fields are invalid
	 * @return
	 * 		Verbal and {@link com.amazon.speech.ui.Card} with what data was invalid
	 * 		and help for the schedule delete commands.
	 */
    @CoberturaIgnore
    public static SpeechletResponse respondEntirePickupDeleteInvalidName(SessionDao sessionDao, boolean withHelpCard, List<String> invalidDataFields) {
		log.info("respondEntirePickupDeleteInvalidName(withHelpCard={}, invalidDataFields={})", withHelpCard, invalidDataFields);
		return respondGeneralInvalidData(sessionDao, withHelpCard, invalidDataFields,
				Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL,
				Phrase.SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT
				);
    }
    
	/**
	 * Respond when user successfully deletes an entire pickup from the schedule.
	 * @param sessionDao {@link SessionDao} data access object for user data stored in 
	 * 			current {@link com.amazon.speech.speechlet.Session}.
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
    	String deletedPickupString;
    	if (removed) {
    		deletedPickupString = "Removed all " + pickupName + " pickups from the schedule.";
    	} else {
    		deletedPickupString = "There was no " + pickupName + " pickup in the schedule.";
    	}
    	return respondGeneralScheduleChange(sessionDao, withHelpCard, deletedPickupString);
    }
    
}
