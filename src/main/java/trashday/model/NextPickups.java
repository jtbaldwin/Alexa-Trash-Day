package trashday.model;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.CoberturaIgnore;
import trashday.ui.FormatUtils;

/**
 * Data structure to compute the next pickup times for every
 * pickup in a schedule that occurs after a given datetime.
 * <p>
 * This is fundamentally a {@link java.util.Map Map} using
 * pickup names (String) as keys to refer to a {@link LocalDateTime} for the
 * next pickup time.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 *
 */
public class NextPickups {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(NextPickups.class);
    
    /** Compute the next pickup after this datetime */
	public LocalDateTime ldtStartingPoint = null;
	/** The weekly pickup schedule */
	@Deprecated
	public Schedule sched = null;
	/** The general pickup schedule */
	public Calendar calendar = null;
	/** Map of pickup name to the computed next pickup time */
	public Map<String,LocalDateTime> pickups;
	
	/**
	 * Compute the next pickup time, after the given starting 
	 * date/time, for one or all of the scheduled pickups.
	 * 
	 * @param ldtStartingPoint LocalDateTime
	 * 			Compute the next pickup(s) after this date/time.
	 * @param sched {@link trashday.model.Schedule}
	 * 			The weekly pickup schedule
	 * @param pickupName String
	 * 			if non-null, compute pickup time for only this
	 * 			pickup name.  if null, compute for all pickups
	 * 			in the schedule.
	 */
	@Deprecated
	public NextPickups(LocalDateTime ldtStartingPoint, Schedule sched, String pickupName) {
		log.trace("new NextPickups({}, {})", ldtStartingPoint, sched);
		this.ldtStartingPoint = ldtStartingPoint;
		this.sched = sched;
		this.pickups = new LinkedHashMap<String,LocalDateTime>();
		
		/*
		 * Find the next times for each pickup, ordered by time
		 */
		
		// For each pickupName, find the next pickup time.
		// (Preserve order of pickupNames using LinkedHashMap).
		Map<String,LocalDateTime> nextPickupTimes = new LinkedHashMap<String,LocalDateTime>();
		List<String> pickupNames = sched.getPickupNames();
		if (pickupName == null) {
			for (String pickup : pickupNames) {
				LocalDateTime ldtNext = getNextPickupTime(pickup, ldtStartingPoint);
				log.debug("NextPickups pickupName={}, ldtNext={}", pickup, ldtNext);
				nextPickupTimes.put(pickup,ldtNext);
			}
		} else {
			if (pickupNames.contains(pickupName)) {
				LocalDateTime ldtNext = getNextPickupTime(pickupName, ldtStartingPoint);
				log.debug("NextPickups pickupName={}, ldtNext={}", pickupName, ldtNext);
				nextPickupTimes.put(pickupName,ldtNext);
			} else {
				log.debug("NextPickups pickupName={} does not exist in schedule.", pickupName);
			}
		}
			
		// Reorder the pickup Times based on (a) the next pickup times and 
		// (b) the order of the schedule's pickup names.
		nextPickupTimes.entrySet().stream()
			.sorted(Map.Entry.<String, LocalDateTime>comparingByValue())
			.forEachOrdered(x -> pickups.put(x.getKey(), x.getValue()));
	}
	
	public NextPickups(LocalDateTime ldtStartingPoint, Calendar calendar, String pickupName) {
		log.trace("new NextPickups({}, {})", ldtStartingPoint, calendar);
		this.ldtStartingPoint = ldtStartingPoint;
		this.calendar = calendar;
		this.pickups = new LinkedHashMap<String,LocalDateTime>();
		
		if (pickupName == null) {
			Map<String,LocalDateTime> nextPickupTimes = calendar.pickupGetNextOccurrences(ldtStartingPoint);
			nextPickupTimes.entrySet().stream()
				.sorted(Map.Entry.<String, LocalDateTime>comparingByValue())
				.forEachOrdered(x -> pickups.put(x.getKey(), x.getValue()));
		} else {
			LocalDateTime ldtEventRecurs = calendar.pickupGetNextOccurrence(ldtStartingPoint, pickupName);
			if (ldtEventRecurs!=null) {
				pickups.put(pickupName.trim().toLowerCase(), ldtEventRecurs);
			}
		}
	}

	/**
	 * Calculate next pickup time, after the given starting 
	 * date/time, for a specific pickup in the
	 * schedule.
	 * <p>
	 * Find the pickup Time-of-Week(s) in the schedule.  Use
	 * the {@link TimeOfWeek#getNextPickupTime(LocalDateTime)} method to
	 * find the actual next pickup that would occur.  Keep the
	 * earliest next pickup time for this pickup and return it.
	 * 
	 * @param pickupName String which schedule pickup to look at
	 * @param ldtStartingPoint LocalDateTime
	 * 			Compute the next pickup(s) after this date/time.
	 * @return The next pickup time for this pickup name
	 * 			that would occur after the {@code ldtStartingPoint}
	 */
	@Deprecated
	private LocalDateTime getNextPickupTime(String pickupName, LocalDateTime ldtStartingPoint) {
		log.trace("getNextPickupTime({}, {})", pickupName, ldtStartingPoint);
		LocalDateTime ldwNextPickup = null;
		
		SortedSet<TimeOfWeek> pickupTimes = sched.getPickupSchedule(pickupName);
		for (TimeOfWeek scheduledTow : pickupTimes) {
			LocalDateTime ldwForThisTow = scheduledTow.getNextPickupTime(ldtStartingPoint);
			log.debug("getNextPickupTime: ldwForThisTow={}", ldwForThisTow);
			if ((ldwNextPickup == null)||(ldwForThisTow.isBefore(ldwNextPickup))) {
				ldwNextPickup = ldwForThisTow;
			}
		}
		return ldwNextPickup;
	}
	
	/**
	 * Get the starting point for when these next pickups were calculated.
	 * 
	 * @return Starting point given when the next pickups were calculated.
	 */
	public LocalDateTime getStartingPoint() {
		log.trace("getStartingPoint");
		return ldtStartingPoint;
	}
	
	/**
	 * Get the number of pickups that have been calculated.
	 * 
	 * @return Number of pickups we've computed.
	 */
	public int getPickupCount() {
		log.trace("getPickupCount");
		return pickups.size();
	}
	
	/**
	 * Get the actual new pickup times.
	 * 
	 * @return
	 * 		Map of pickup names to the next pickup time after
	 * 		the {@link ldtStartingPoint}
	 */
	public Map<String,LocalDateTime> getPickups() {
		log.trace("getPickups={}", pickups);
		return pickups;
	}
	
	/**
	 * Create a printable version of the next pickup times, showing
	 * those time relative to right now.
	 * <p>
	 * Friendlier for voice output.  Instead of a date, use words
	 * like "today", "tomorrow",  or day of week for next pickups
	 * within the next seven days.
	 * <p>
	 * Used primarily to print the schedule when it is returned
	 * as {@link com.amazon.speech.ui.Card} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * <p>
	 * Deprecated because caller should specifically choose a Printable
	 * or Alexa-statement form of the Schedule.
	 * 
	 * @return String representing the next pickup times
	 */
	@Deprecated
	public String toString() {
		return toStringPrintable();
	}
	
	/**
	 * Create a version of the next pickup times, that is suitable for
	 * Alexa to say, showing those times relative to right now.
	 * <p>
	 * Friendlier for voice output.  Instead of a date, use words
	 * like "today", "tomorrow",  or day of week for next pickups
	 * within the next seven days.
	 * <p>
	 * Used primarily to say the schedule when it is returned
	 * as {@link com.amazon.speech.ui.PlainTextOutputSpeech} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * 
	 * @return String representing the next pickup times
	 */
	@Deprecated
	@CoberturaIgnore
	public String toStringVerbal() {
		log.trace("toStringVerbal()");
		StringBuilder sb = new StringBuilder();
		//sb.append("<speak>\n");
		//int pickupNum = 1;
		for(Map.Entry<String,LocalDateTime> entry : pickups.entrySet()) {
			String pickupName = entry.getKey();
			LocalDateTime ldtNextPickup = entry.getValue();
			sb.append("Next " + pickupName + " pickup is " + FormatUtils.verbalDateAndTimeRelative(ldtNextPickup, ldtStartingPoint) + ". ");
			//if (pickupNum != pickups.size()) {
			//	// Not the last item
			//	//sb.append("<break strength=\"strong\"/>\n");
			//}
			//pickupNum++;
		}
		//sb.append("</speak>");
		return sb.toString();
	}
	
	/**
	 * Create a printable version of the next pickup times, showing
	 * those times as absolute date/time values.
	 * <p>
	 * Gives specific dates and times, not relative to the current
	 * date/time.
	 * <p>
	 * Used primarily for JUnit for testing.
	 * 
	 * @return String representing the next pickup times
	 */
	@Deprecated
	@CoberturaIgnore
	public String toStringPrintable() {
		log.trace("toStringPrintable()");
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<String,LocalDateTime> entry : pickups.entrySet()) {
			String pickupName = entry.getKey();
			LocalDateTime ldtNextPickup = entry.getValue();
			sb.append("Next " + pickupName + " pickup is " + FormatUtils.printableDateAndTimeRelative(ldtNextPickup, ldtStartingPoint) + ".\n");
		}
		return sb.toString();
	}
}
