package trashday.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;

import trashday.CoberturaIgnore;

/**
 * Utility functions to help provide output in a format that sounds
 * good when verbally spoken by Alexa.
 * 
 * @author J. Todd Baldwin
 */
public final class DateTimeOutputUtils {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DateTimeOutputUtils.class);
    
	/** Format Time Of Day to something appropriate for Alexa to say */
    private static final DateTimeFormatter formatterVerbalTimeOfDay = DateTimeFormatter.ofPattern("h mm a");
	/** Format Time Of Day to something appropriate to be printed on Cards */
    private static final DateTimeFormatter formatterPrintableTimeOfDay = DateTimeFormatter.ofPattern("h:mm a");
	/** Format Time Of Day for on-the-hour in a format suitable for speaking or printing. */
    private static final DateTimeFormatter formatHourOnly = DateTimeFormatter.ofPattern("h a");
    
	/** Format Day to something appropriate for Alexa to say or to be printed on Cards */
	public static final DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("EEEE, MMMM d");
	/** Format Day to something appropriate for Alexa to say or to be printed on Cards */
	public static final DateTimeFormatter formatterDayOfWeek = DateTimeFormatter.ofPattern("EEEE");

	/**
	 * Private constructor given to this utility class.  Prevents instantiation since
	 * this class is only meant to provide public, static utility methods.
	 */
	@CoberturaIgnore
	private DateTimeOutputUtils () {
    }
	
	/**
	 * Find a {@link java.time.LocalDateTime} for the given user 
	 * {@link com.amazon.speech.speechlet.LaunchRequest} and {@link java.util.TimeZone}.
	 * 
	 * @param request {@link com.amazon.speech.speechlet.LaunchRequest} of this user's request
	 * @param timeZone {@link java.util.TimeZone} the user has configured
	 * @return LocalDateTime
	 */
	public static LocalDateTime getRequestLocalDateTime(LaunchRequest request, TimeZone timeZone) {
		if (timeZone==null) { return null; };
		return getRequestLocalDateTime(request.getTimestamp(), timeZone);
	}
	
	/**
	 * Find a {@link java.time.LocalDateTime} for the given user 
	 * {@link com.amazon.speech.speechlet.IntentRequest} and {@link java.util.TimeZone}.
	 * 
	 * @param request {@link com.amazon.speech.speechlet.IntentRequest} of this user's request
	 * @param timeZone {@link java.util.TimeZone} the user has configured
	 * @return LocalDateTime
	 */
	public static LocalDateTime getRequestLocalDateTime(IntentRequest request, TimeZone timeZone) {
		if (timeZone==null) { return null; };
		return getRequestLocalDateTime(request.getTimestamp(), timeZone);
	}
	
	/**
	 * Find a {@link java.time.LocalDateTime} for the given user 
	 * {@link java.util.Date} and {@link java.util.TimeZone}.
	 * 
	 * @param requestDate {@link java.util.Date} of this user's request
	 * @param timeZone {@link java.util.TimeZone} the user has configured
	 * @return LocalDateTime
	 */
	public static LocalDateTime getRequestLocalDateTime(Date requestDate, TimeZone timeZone) {
    	if (requestDate == null) {
    		return null;
    	}
		if (timeZone == null) {
			return null;
		}
    	ZoneId zoneId = timeZone.toZoneId();
		return LocalDateTime.ofInstant(requestDate.toInstant(), zoneId);
	}
	
	/**
	 * Provide a text version of a {@link LocalDate} that is relative to a
	 * second {@link LocalDate} and is suitable for Alexa to speak.
	 * 
	 * For example:
	 *   2015-11-24 relative to 2015-12-01 is "Thursday, November 24"
	 *   2015-11-24 relative to 2015-11-27 is "last Thursday"
	 *   2015-11-24 relative to 2015-11-26 is "last Thursday"
	 *   2015-11-24 relative to 2015-11-25 is "yesterday"
	 *   2015-11-24 relative to 2015-11-24 is "today"
	 *   2015-11-24 relative to 2015-11-23 is "tomorrow"
	 *   2015-11-24 relative to 2015-11-22 is "Thursday"
	 *   2015-11-24 relative to 2015-11-21 is "Thursday"
	 *   2015-11-24 relative to 2015-11-17 is "Thursday, November 24"
	 * 
	 * @param ldOccurrence LocalDate date of an occurrence
	 * @param ldBase LocalDate day that occurrence is being spoken
	 * @return String suitable for Alexa to speak.
	 */
	public static String dateRelative(LocalDate ldOccurrence, LocalDate ldBase) {
		log.trace("dateRelative({},{})",ldOccurrence, ldBase);
		
		long daysBetween = ChronoUnit.DAYS.between(ldBase, ldOccurrence);
		if (daysBetween==0) {
			// Occurrence on same date at the base
			return "today";
		}
		if (ldOccurrence.isAfter(ldBase)) {
			if (daysBetween == 1) {
				// Date is within 24 hours and on different Day Of Week
				return "tomorrow";				
			}
		    else if (daysBetween < 7) {
				// Date is within next week
		    	return ldOccurrence.format(formatterDayOfWeek);
			} 
		} else {
			if (daysBetween == -1) {
				return "yesterday";				
			}
		    else if (daysBetween > -7) {
				// Date is within last week
				return "last " + ldOccurrence.format(formatterDayOfWeek);
			}
		}
		
		// Just show the Date as "day-of-week, month day-of-month at time-of-day"
		return ldOccurrence.format(formatterDate);
	}
	
	/**
	 * Provide a text version of a {@link LocalDate} that is relative to a
	 * second {@link LocalDate} and is suitable for Alexa to speak.
	 * 
	 * For example:
	 *   2015-11-24 relative to 2015-12-01 is "Thursday, November 24"
	 *   2015-11-24 relative to 2015-11-27 is "last Thursday"
	 *   2015-11-24 relative to 2015-11-26 is "last Thursday"
	 *   2015-11-24 relative to 2015-11-25 is "yesterday"
	 *   2015-11-24 relative to 2015-11-24 is "today"
	 *   2015-11-24 relative to 2015-11-23 is "tomorrow"
	 *   2015-11-24 relative to 2015-11-22 is "Thursday"
	 *   2015-11-24 relative to 2015-11-21 is "Thursday"
	 *   2015-11-24 relative to 2015-11-17 is "Thursday, November 24"
	 * 
	 * @param ldtOccurrence LocalDateTime date of an occurrence
	 * @param ldtBase LocalDateTime day that occurrence is being spoken
	 * @return String suitable for Alexa to speak.
	 */
	public static String dateRelative(LocalDateTime ldtOccurrence, LocalDateTime ldtBase) {
		log.trace("dateRelative({},{})",ldtOccurrence,ldtBase);
		LocalDate ldOccurrence = LocalDate.from(ldtOccurrence);
		LocalDate ldBase = LocalDate.from(ldtBase);
		return dateRelative(ldOccurrence, ldBase);
	}

	/**
	 * Provide a printable version of the given {@link LocalTime}
	 * that is good for printing of time succinctly on Cards.
	 * 
	 * @param lt LocalTime to be printed
	 * @return text suitable for printing
	 */
	public static String printableTime(LocalTime lt) {		
		log.trace("printableTime({})", lt);
		
		// Special, friendly text for "on-the-hour" times.
		if (lt.getMinute() == 0) {
			if (lt.getHour() == 0) {
				return "midnight";
			}
			else if (lt.getHour() == 12) {
				return "noon";
			}
			// Return "Hour AM/PM"
			
			return lt.format(formatHourOnly);
		}
		
		// Return "Hour Minute AM/PM"
		return lt.format(formatterPrintableTimeOfDay);
	}
	
	/**
	 * Provide a printable version of the given {@link LocalDateTime}
	 * that is good for printing of time succinctly on Cards.
	 * 
	 * @param ldt LocalDateTime to be printed
	 * @return text suitable for printing
	 */
	public static String printableTime(LocalDateTime ldt) {	
		log.trace("printableTime({})", ldt);
		LocalTime lt = LocalTime.of(ldt.getHour(), ldt.getMinute());
		return printableTime(lt);
	}
	
	public static String printableDayOfWeek(DayOfWeek dow) {
		return dow.getDisplayName(TextStyle.FULL, Locale.getDefault());
	}
	
	@Deprecated
	@CoberturaIgnore
	public static String printableDayOfWeekAndTime(DayOfWeek dow, LocalTime tod) {
		return dow.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " at " + printableTime(tod);
	}
	
	/**
	 * Provide a text version of the given {@link LocalTime} that is 
	 * suitable for Alexa to speak.
	 * 
	 * @param lt LocalTime to be converted to be spoken verbally
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalTime(LocalTime lt) {
		log.trace("verbalTime({})", lt);
		
		// Special, friendly text for "on-the-hour" times.
		if (lt.getMinute() == 0) {
			if (lt.getHour() == 0) {
				return "midnight";
			}
			else if (lt.getHour() == 12) {
				return "noon";
			}
			// Return "Hour AM/PM"
			return lt.format(formatHourOnly);
		}
		
		// Return "Hour Minute AM/PM"
		return lt.format(formatterVerbalTimeOfDay);		
	}
	
	/**
	 * Provide a text version of the time given in {@link LocalDateTime} that is 
	 * suitable for Alexa to speak.
	 * 
	 * @param ldt LocalDateTime to be converted to be spoken verbally
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalTime(LocalDateTime ldt) {
		log.trace("verbalTime({})", ldt);
		LocalTime lt = LocalTime.of(ldt.getHour(), ldt.getMinute());
		return verbalTime(lt);
	}
	
	/**
	 * Provide a text version of the date and time that occurred, spoken relative to
	 * a given date and time.  For example, an occurrence that occurs on "Thursday,
	 * 2016-11-24 at 7 am" would be spoken as "Tomorrow at 7 am" if it was relative to
	 * a base date of "Wednesday, 2016-11-23".
	 * 
	 * @param ldtOccurrence LocalTime to be converted to be spoken verbally
	 * @param ldtBase LocalDateTime day of week is given as relative to the
	 * 		day the request is given.
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalDateAndTimeRelative(LocalDateTime ldtOccurrence, LocalDateTime ldtBase) {
		log.trace("verbalDateAndTime({},{})",ldtOccurrence,ldtBase);
		return dateRelative(ldtOccurrence,ldtBase) + " at " + verbalTime(ldtOccurrence);
	}
	
	/**
	 * Provide a text version of the day of week and time.  For example, an occurrence 
	 * that occurs on "Thursday, 2016-11-24 at 7 am" would be spoken as 
	 * "Thursday at 7 am".
	 * 
	 * @param dow DayOfWeek to be spoken verbally
	 * @param tod LocalTime time to be spoken verbally
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalDayOfWeekAndTime(DayOfWeek dow, LocalTime tod) {
		return dow.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " at " + verbalTime(tod);
	}
	
	/**
	 * Provide a text version of the date and time that occurred, printable relative to
	 * a given date and time.  For example, an occurrence that occurs on "Thursday,
	 * 2016-11-24 at 7 am" would be spoken as "Tomorrow at 7 am" if it was relative to
	 * a base date of "Wednesday, 2016-11-23".
	 * 
	 * @param ldtOccurrence LocalTime to be converted to be spoken verbally
	 * @param ldtBase LocalDateTime day of week is given as relative to the
	 * 		day the request is given.
	 * @return text suitable to be printed on an Alexa Card
	 */
	public static String printableDateAndTimeRelative(LocalDateTime ldtOccurrence, LocalDateTime ldtBase) {
		log.trace("printableDateAndTimeRelative({},{})",ldtOccurrence,ldtBase);
		return dateRelative(ldtOccurrence,ldtBase) + " at " + printableTime(ldtOccurrence);
	}
	
	/**
	 * Provide a text version of the date and time suitable to be
	 * printed on an Alexa Card response.
	 * 
	 * @param ldtRequest LocalTime to be converted to be spoken verbally
	 * @return text suitable for Alexa to speak
	 */
	public static String printableDateAndTime(LocalDateTime ldtRequest) {
		return ldtRequest.format(formatterDate) + " at " + printableTime(ldtRequest);
	}
	
}
