package trashday.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.jonpeterson.jackson.module.versioning.JsonVersionedModel;

import trashday.ui.DateTimeOutputUtils;

import java.time.DayOfWeek;

/**
 * Data structure to indicate a Time Of Week.  Consists of
 * a {@link DayOfWeek} and a {@link LocalTime}.
 * <p>
 * The class implements Jackson Model Versioning using the 
 * "@JsonVersionedModel" and {@link ToCurrentSchedule} class.
 * <p>
 * Class also uses Jackson "@JsonIgnoreProperties" and 
 * "@JsonCreator" directives.  These avoid using standard
 * Jackson serialization and replace it with a specific method
 * {@link TimeOfWeek#TimeOfWeek(DayOfWeek, LocalTime)} that
 * truncates pickup times to the minute.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 *
 */
@JsonIgnoreProperties
@JsonVersionedModel(currentVersion = "1", toCurrentConverterClass = ToCurrentTimeOfWeek.class)
public class TimeOfWeek implements Comparable<TimeOfWeek> {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(TimeOfWeek.class);
    /** Day of week this pickup occurs. */
	private DayOfWeek dow;	// ISO-8601 standard, from 1 (Monday) to 7 (Sunday)
	/** Time of day this pickup occurs. */
	private LocalTime tod;
	
	/**
	 * Avoid using standard Jackson deserialization and replace 
	 * it with this method that truncates pickup times to the 
	 * minute.
	 * 
	 * @param dow DayOfWeek this pickup occurs.
	 * @param tod TimeOfDay this pickup occurs.
	 */
	@JsonCreator
    public TimeOfWeek(
    		@JsonProperty("dow") DayOfWeek dow,
    		@JsonProperty("tod") LocalTime tod
    		)
    {
		super();
		log.trace("TimeOfWeek(dow,tod)");
		this.dow = dow;
		this.tod = tod.truncatedTo(ChronoUnit.MINUTES);
    }
	
	/**
	 * Create object with defaults of today at this time of day.
	 */
	public TimeOfWeek() {
		super();
		log.trace("TimeOfWeek()");
		LocalDateTime dt = LocalDateTime.now();
		this.dow = dt.getDayOfWeek();
		this.tod = LocalTime.of(dt.getHour(), dt.getMinute());
	}
	
	/**
	 * Create object based on given Day of Week and Time Of Day information.
	 * 
	 * @param dow DayOfWeek this pickup occurs.
	 * @param hourOfDay int hour of day this pickup occurs.
	 * @param minuteOfHour int minute of hour this pickup occurs.
	 */
	public TimeOfWeek(DayOfWeek dow, int hourOfDay, int minuteOfHour) {
		super();
		log.trace("TimeOfWeek(dow,hourOfDay,minuteOfHour)");
		this.dow = dow;
		this.tod = LocalTime.of(hourOfDay, minuteOfHour);
	}
	
	/**
	 * Create object based on given Day of Week and Time Of Day information.
	 * 
	 * @param dow int day of week this pickup occurs.  ISO-8601 standard, from 1 (Monday) to 7 (Sunday)
	 * @param hourOfDay int hour of day this pickup occurs.
	 * @param minuteOfHour int minute of hour this pickup occurs.
	 */
	public TimeOfWeek(int dow, int hourOfDay, int minuteOfHour) {
		super();
		log.trace("TimeOfWeek(dow,hourOfDay,minuteOfHour)");
		this.dow = DayOfWeek.of(dow);
		this.tod = LocalTime.of(hourOfDay, minuteOfHour);
	}
	
	/**
	 * Getter for the Day Of Week component of this weekly time.
	 * <p>
	 * Uses Jackson "@JsonProperty" to specify "dow" field
	 * when serializing this object.
	 * 
	 * @return DayOfWeek for this weekly time.
	 */
	@JsonProperty("dow")
	public DayOfWeek getDayOfWeek() {
		log.trace("getDayOfWeek()");
		return dow;
	}
	/**
	 * Setter for the Day Of Week portion of this weekly time.
	 * 
	 * @param dow DayOfWeek for this weekly time.
	 */
	public void setDayOfWeek(DayOfWeek dow) {
		log.trace("setDayOfWeek({})", dow);
		this.dow = dow;
	}
	
	/**
	 * Getter for the Time Of Day component of this weekly time.
	 * <p>
	 * Uses Jackson "@JsonProperty" to specify "tod" field
	 * when serializing this object.
	 * 
	 * @return LocalTime for this weekly time.
	 */
	@JsonProperty("tod")
	public LocalTime getTimeOfDay() {
		log.trace("getTimeOfDay()");
		return tod;
	}
	/**
	 * Setter for the Time Of Day portion of this weekly time.
	 * 
	 * @param hour int for this weekly time hour of day.
	 * @param minute int for this weekly time minute of day.
	 */
	public void setTimeOfDay(int hour, int minute) {
		log.trace("setTimeOfDay({}, {})", hour, minute);
		tod = LocalTime.of(hour, minute);
	}
	
	/**
	 * Provide a comparator so we can use this class in
	 * SortedSets.
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(TimeOfWeek o) {
		return getMinuteOfWeek() - o.getMinuteOfWeek();
	}
	
	/**
	 * Getter for the Time Of Day in terms of minutes past 
	 * midnight.
	 * <p>
	 * Uses Jackson "@JsonIgnore" to ignore this getter
	 * when serializing this object.
	 * 
	 * @return Minutes past midnight
	 */
	@JsonIgnore
	public int getMinuteOfDay() {
		log.trace("getMinuteOfDay()");
		return (60 * tod.getHour()) + tod.getMinute();
	}
	
	/**
	 * Getter for the Time Of Day in terms of minutes past 
	 * midnight on the first day of the week.
	 * <p>
	 * Uses Jackson "@JsonIgnore" to ignore this getter
	 * when serializing this object.
	 * 
	 * @return Minutes past midnight on the first day 
	 * 			of the week.
	 */
	@JsonIgnore
	public int getMinuteOfWeek() {
		log.trace("getMinuteOfWeek()");
		return ((dow.getValue() - 1) * 1440) + getMinuteOfDay();
	}
	
	/**
	 * Getter for the Time Of Day in terms of hour of the day.
	 * <p>
	 * Uses Jackson "@JsonIgnore" to ignore this getter
	 * when serializing this object.
	 * 
	 * @return Hour of the day for this weekly time.
	 */
	@JsonIgnore
	public int getHour() {
		log.trace("getHour()");
		return tod.getHour();
	}
    
	/**
	 * Getter for the Time Of Day in terms of minute of the hour.
	 * <p>
	 * Uses Jackson "@JsonIgnore" to ignore this getter
	 * when serializing this object.
	 * 
	 * @return Minute of the hour for this weekly time.
	 */
	@JsonIgnore
	public int getMinute() {
		log.trace("getMinute()");
		return tod.getMinute();
	}
	
	/**
	 * Computes the number of minutes from a given datetime
	 * until this Time Of Week will occur again.
	 * <p>
	 * Uses Jackson "@JsonIgnore" to ignore this getter
	 * when serializing this object.
	 * 
	 * @param ldtNow LocalDateTime Count number of minutes 
	 * 			forward from this day and time.
	 * @return Minutes from this day and time until we reach
	 * 			this object's configured Time Of Week.
	 */
	@JsonIgnore
	public int getMinutesUntilNext(LocalDateTime ldtNow) {
		log.trace("getMinutesUntilNext({})", ldtNow);
		ldtNow = ldtNow.truncatedTo(ChronoUnit.MINUTES);
		
		int dayNow = ldtNow.getDayOfWeek().getValue();
		int modNow = (60 * ldtNow.getHour()) + ldtNow.getMinute();
		
		int dayScheduled = dow.getValue();
		int modScheduled = getMinuteOfDay();

		// Now:   Tuesday Midnight (mod=0)
		// Sched: Tuesday 1:15am (mod=75)
		int minutesToNextPickup = 0;
		if (modScheduled >= modNow) {
			// Scheduled time is same-or-later in the day than the "Now" time.
			minutesToNextPickup += modScheduled - modNow;
		} else {
			// Scheduled time is earlier in the day than the "Now" time.
			minutesToNextPickup += (1440 + modScheduled - modNow);
			// Adjust the scheduled day to account for the added minutes.
			dayScheduled--;
		}
		
		if (dayScheduled >= dayNow) {
			// Scheduled day is same-or-later in the week than the "Now" day.
			minutesToNextPickup += 1440 * (dayScheduled-dayNow);
		} else {
			// Scheduled day is earlier in the week than the "Now" day.
			minutesToNextPickup += 1440 * (7+dayScheduled-dayNow);
		}
		return minutesToNextPickup;
	}
	
	/**
	 * Computes the next date/time this Time Of Week will
	 * occur after the given date/time.
	 * <p>
	 * Uses Jackson "@JsonIgnore" to ignore this getter
	 * when serializing this object.
	 * 
	 * @param ldtNow LocalDateTime Count forward from this 
	 * 				day and time
	 * @return The next day and time that this
	 * 				Time Of Week will occur
	 */
	@JsonIgnore
	public LocalDateTime getNextPickupTime(LocalDateTime ldtNow) {
		log.trace("getNextPickupTime({})", ldtNow);
		int minutesToNextPickup = getMinutesUntilNext(ldtNow);
		LocalDateTime ldtNextPickup = ldtNow.truncatedTo(ChronoUnit.MINUTES).plusMinutes(minutesToNextPickup);
		return ldtNextPickup;
	}
	
	/**
	 * Create a printable version of the TimeOfWeek.
	 * <p>
	 * Used primarily to print the TimeOfWeek when it is returned
	 * as {@link com.amazon.speech.ui.Card} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * <p>
	 * Deprecated because caller should specifically choose a Printable
	 * or Alexa-statement form of the TimeOfWeek.
	 * 
	 * @return String representing this Time Of Week (day plus
	 * 			time of day).
	 */
	//@Deprecated
	//public String toString() {
	//	return toStringPrintable();
	//}
	
	/**
	 * Create a version of the TimeOfWeek suitable for Alexa to say.
	 * <p>
	 * Used primarily to say the TimeOfWeek when it is returned
	 * as {@link com.amazon.speech.ui.PlainTextOutputSpeech} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * 
	 * @return String representing this Time Of Week (day plus
	 * 			time of day).
	 */
	public String toStringVerbal() {
		return DateTimeOutputUtils.printableDayOfWeek(dow) +" "+ DateTimeOutputUtils.verbalTime(tod);
	}

	/**
	 * Create a printable version of the TimeOfWeek.
	 * <p>
	 * Used primarily to print the TimeOfWeek when it is returned
	 * as {@link com.amazon.speech.ui.Card} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * 
	 * @return String representing this Time Of Week (day plus
	 * 			time of day).
	 */
	public String toStringPrintable() {
		return DateTimeOutputUtils.printableDayOfWeek(dow) + " at " + DateTimeOutputUtils.printableTime(tod);
	}

}
