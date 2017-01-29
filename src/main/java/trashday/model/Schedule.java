package trashday.model;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.jonpeterson.jackson.module.versioning.JsonVersionedModel;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;

/**
 * Data structure for the weekly pickup schedule.
 * <p>
 * The schedule is fundamentally a {@link java.util.Map Map} using
 * pickup names (String) as keys to refer to a {@link java.util.Set Set} of
 * pickup times ({@link TimeOfWeek}).  It keeps a {@link java.util.List List}
 * of pickup names as a convenience to keep ordering.
 * <p>
 * The class implements Jackson Model Versioning using the 
 * "@JsonVersionedModel" and {@link ToCurrentSchedule} class.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 *
 */
@JsonVersionedModel(currentVersion = "1", toCurrentConverterClass = ToCurrentSchedule.class)
public class Schedule {
	/** Serialized version number */
	//private static final long serialVersionUID = 1L;
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(Schedule.class);
    /** Convenience for ordered list of pickup names. */
	public List<String> 						pickupNames;
	/** The core schedule Map */
	public Map<String,SortedSet<TimeOfWeek>>	pickupSchedule;
	/** A Jackson object mapper configured to handle Java 8 LocalDateTime objects and Jon Peterson's object versioning module. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
 		   .registerModule(new ParameterNamesModule())
 		   .registerModule(new Jdk8Module())
 		   .registerModule(new JavaTimeModule())
 		   .registerModule(new VersioningModule())
 		;

	/**
	 * Create an empty schedule.
	 */
	public Schedule() {
		log.trace("Schedule(): Create empty schedule");
		pickupNames = new ArrayList<String>();
		pickupSchedule = new HashMap<String,SortedSet<TimeOfWeek>>();
	}
	
	/**
	 * Create a schedule from a given JSON.
	 * 
	 * @param json String JSON representation of a Schedule
	 */
	public Schedule(String json) {
		log.trace("Schedule({})", json);
		try {
			Schedule newSched = OBJECT_MAPPER.readValue(json, new TypeReference<Schedule>() { } );
			this.pickupNames = newSched.getPickupNames();
			this.pickupSchedule = newSched.getPickupSchedule();
			validate();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to unconvert schedule value.", e);
		}    		
	}
	
	/**
	 * Create an example schedule.  Used for JUnit testing.
	 */
	public void initExampleSchedule() {
		log.trace("initExampleSchedule");
		pickupNames.clear();
		pickupSchedule.clear();
		
		addPickupSchedule("trash", new TimeOfWeek(DayOfWeek.TUESDAY,6,30));
		addPickupSchedule("trash", new TimeOfWeek(DayOfWeek.FRIDAY,6,30));
		
		addPickupSchedule("recycling", new TimeOfWeek(DayOfWeek.FRIDAY,6,30));
		
		addPickupSchedule("lawn waste", new TimeOfWeek(DayOfWeek.WEDNESDAY,12,00));
	}
	
	/**
	 * Check for possible problems in the Schedule and clean them up.
	 * 
	 * @return Number of issues resolved
	 */
	public int validate() {
		int repairs = 0;
		
		// Any items in pickupNames not available in pickupSchedule?
		Set<String> setScheduleNames = pickupSchedule.keySet();
		Set<String> setPickupNames = new HashSet<String>(pickupNames);
		setPickupNames.removeAll(setScheduleNames);
		if (setPickupNames.size() > 0) {
			// pickupNames has items not available in the Schedule.
			for (String pickupName : setPickupNames) {
				log.warn("Schedule had a pickup without any times configured: {}", pickupName);
				pickupNames.remove(pickupName);
				repairs++;
			}
		}
		
		// Any items in pickupSchedule with missing or empty TimeOfWeek list?
		for( Entry<String,SortedSet<TimeOfWeek>> entry : pickupSchedule.entrySet()) {
			String pickupName = entry.getKey();
			SortedSet<TimeOfWeek> pickupTimes = entry.getValue();
			
			if (pickupTimes == null) {
				log.warn("Schedule had a pickup without any times configured: {}", pickupName);
				pickupNames.remove(pickupName);
				pickupSchedule.remove(pickupName);
				repairs++;
				break;
			}
			if (pickupTimes.size()==0) {
				log.warn("Schedule had a pickup with no times configured: {}", pickupName);
				pickupNames.remove(pickupName);
				pickupSchedule.remove(pickupName);
				repairs++;
				break;				
			}
		}
		
		// Any items in pickupSchedule not available in pickupNames?
		setScheduleNames = new HashSet<String>(pickupSchedule.keySet());
		setPickupNames = new HashSet<String>(pickupNames);
		setScheduleNames.removeAll(setPickupNames);
		if (setScheduleNames.size() > 0) {
			// pickupSchedule has items not available in the pickupNames.
			for (String pickupName : setScheduleNames) {
				log.warn("Pickup names was missing entry that had times configured: {}", pickupName);
				pickupNames.add(pickupName);
				repairs++;
			}
		}
		return repairs;
	}
	
	/**
	 * Getter to determine if the Schedule has any pickups configured.
	 * <p>
	 * Note: Uses the "@JsonIgnore" directive so that Jackson
	 * serializer will not try to write this information when
	 * save the Schedule to Dynamo DB.
	 * 
	 * @return True if the Schedule has no configured pickups.
	 */
	@JsonIgnore
	public Boolean isEmpty() {
		log.trace("isEmpty()");
		return pickupSchedule.isEmpty();
	}
	
	/**
	 * Getter for an order list of pickup names.
	 * 
	 * @return List of pickup names.
	 */
	public List<String> getPickupNames() {
		log.trace("getPickupNames()");
		return pickupNames;
	}
	
	/**
	 * Get entire pickupSchedule.
	 * 
	 * @return Map of the entire schedule
	 */
	public Map<String,SortedSet<TimeOfWeek>> getPickupSchedule() {
		log.trace("getPickupSchedule()");
		return pickupSchedule;
	}
	
	/**
	 * Gets a {@link java.util.SortedSet SortedSet} of the
	 * pickup times for a specific pickup name.
	 * 
	 * @param pickupName String 
	 * @return SortedSet of pickup times for given pickup name.
	 */
	public SortedSet<TimeOfWeek> getPickupSchedule(String pickupName) {
		log.trace("getPickupSchedule({})", pickupName);
		SortedSet<TimeOfWeek> ret = pickupSchedule.get(pickupName);
		return ret;
	}
	
	/**
	 * Add a new schedule entry for a given pickup name and time.
	 * 
	 * @param pickupName String Name for this pickup (eg. Trash, Recycling)
	 * @param dow DayOfWeek This pickup recurs on this day of the week.
	 * @param tod LocalTime This pickup occurs at this time of the day.
	 */
	public void addPickupSchedule(String pickupName, DayOfWeek dow, LocalTime tod) {
		log.trace("addPickupSchedule({}, {}, {})",pickupName,dow,tod);
		addPickupSchedule(pickupName, new TimeOfWeek(dow, tod));
	}
	
	/**
	 * Add a new schedule entry for a given pickup name and time.
	 * 
	 * @param pickupName String Name for this pickup (eg. Trash, Recycling)
	 * @param tow TimeOfWeek This pickup recurs on this day and time every week.
	 */
	public void addPickupSchedule(String pickupName, TimeOfWeek tow) {
		log.trace("addPickupSchedule({}, {})",pickupName,tow);
		if (pickupSchedule.containsKey(pickupName)) {
			SortedSet<TimeOfWeek> pickupTimes = pickupSchedule.get(pickupName);
			log.debug("addPickupSchedule existing pickup times: {}",pickupTimes);
			if (! pickupTimes.contains(tow)) {
				pickupTimes.add(tow);
			}
			log.debug("addPickupSchedule new pickup times: {}",pickupTimes);
		} else {
			log.debug("addPickupSchedule existing pickup times: <none>");
			SortedSet<TimeOfWeek> pickupTimes = new TreeSet<TimeOfWeek>();
			pickupTimes.add(tow);
			pickupSchedule.put(pickupName, pickupTimes);
			if (! pickupNames.contains(pickupName)) {
				pickupNames.add(pickupName);
			}
			log.debug("addPickupSchedule new pickup times: {}",pickupTimes);
		}
		log.debug("Updated schedule={}", toStringPrintable());
	}
	
	/**
	 * Delete a schedule entry for a given pickup name and time.
	 * 
	 * @param pickupName String Name for this pickup (eg. Trash, Recycling)
	 * @param dow DayOfWeek This pickup recurs on this day of the week.
	 * @param tod LocalTime This pickup occurs at this time of the day.
	 * @return True if this pickup time already existed and had to be removed.
	 */
	public Boolean deletePickupTime(String pickupName, DayOfWeek dow, LocalTime tod) {
		log.trace("deletePickupTime({}, {}, {})",pickupName,dow,tod);
		return deletePickupTimeOfWeek(pickupName, new TimeOfWeek(dow, tod));
	}
	
	/**
	 * Delete a schedule entry for a given pickup name and time.
	 * 
	 * @param pickupName String Name for this pickup (eg. Trash, Recycling)
	 * @param tow TimeOfWeek This pickup recurs on this day and time every week.
	 * @return True if this pickup time already existed and had to be removed.
	 */
	public Boolean deletePickupTimeOfWeek(String pickupName, TimeOfWeek tow) {
		log.trace("deletePickupTimeOfWeek({}, {})",pickupName,tow);
		if (! pickupSchedule.containsKey(pickupName)) {
			log.debug("deletePickupTimeOfWeek pickup name does not exist in schedule.");
			return false;
		}
		SortedSet<TimeOfWeek> pickupTimes = pickupSchedule.get(pickupName);
		log.debug("deletePickupTimeOfWeek existing pickup times: {}", pickupTimes);
		boolean ret = pickupTimes.remove(tow);
		log.debug("deletePickupTimeOfWeek Did an item actually get removed? {}", ret);
		if (pickupTimes.size() < 1) {
			log.debug("deletePickupTimeOfWeek No more pickup times for {} pickup.  Removing it from schedule.", pickupName);
			pickupSchedule.remove(pickupName);
			pickupNames.remove(pickupName);
		}
		log.debug("Updated schedule={}", toStringPrintable());
		return ret;
	}
	
	/**
	 * Delete all entries from the schedule for the given pickupName.
	 * 
	 * @param pickupName String Name for this pickup (eg. Trash, Recycling)
	 * @return True if this pickup already existed and had to be removed.
	 */
	public Boolean deleteEntirePickup(String pickupName) {
		log.trace("deleteEntirePickup({})",pickupName);
		if (! pickupSchedule.containsKey(pickupName)) {
			log.debug("deleteEntirePickup pickup name does not exist.");
			return false;
		}
		pickupNames.remove(pickupName);
		SortedSet<TimeOfWeek> pickupTimes = pickupSchedule.remove(pickupName);
		if (pickupTimes == null) {
			log.debug("deleteEntirePickup pickup {} had no scheduled pickups.");
			log.debug("Updated schedule={}", toStringPrintable());
			return false;
		} else {
			log.debug("deleteEntirePickup existing pickup times: {}", pickupTimes);
			log.debug("Updated schedule={}", toStringPrintable());
			return true;
		}
	}
	
	/**
	 * Delete all entries from the schedule.
	 * 
	 * @return True if this schedule had any pickups scheduled that had to be removed.
	 */
	public Boolean deleteEntireSchedule() {
		log.trace("deleteEntireSchedule()");
		if (! pickupSchedule.isEmpty()) {
			pickupNames.clear();
			pickupSchedule.clear();
			return true;
		}
		return false;
	}
	
	/**
	 * Create a JSON version of this object.
	 * 
	 * @return String with JSON version of this Schedule.
	 */
	public String toJson() {
		try {
			log.info("Schedule convert: {}", this);
			return OBJECT_MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to convert schedule.", e);
		}
	}
	
	/**
	 * Create a printable version of the Schedule.
	 * <p>
	 * Used primarily to print the schedule when it is returned
	 * as {@link com.amazon.speech.ui.Card} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * <p>
	 * Deprecated because caller should specifically choose a Printable
	 * or Alexa-statement form of the Schedule.
	 * 
	 * @return String representing the pickups and their times
	 * 			during the week.
	 */
	//@Deprecated
	//public String toString() {
	//	return toStringPrintable();
	//}
	
	/**
	 * Create a version of the Schedule suitable for Alexa to say.
	 * <p>
	 * Used primarily to say the schedule when it is returned
	 * as {@link com.amazon.speech.ui.PlainTextOutputSpeech} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * 
	 * @return String representing the pickups and their times
	 * 			during the week.
	 */
	public String toStringVerbal() {
		//log.trace("Schedule.toStringVerbal() for schedule={}", toStringPrintable());
		
		StringBuilder sb = new StringBuilder();
		//sb.append("<speak>\n");
		//int pickupNum = 1;
		for (String pickupName : new ArrayList<String>(pickupNames)) {
			SortedSet<TimeOfWeek> pickupTimes = pickupSchedule.get(pickupName);
			if (pickupTimes == null) {
				log.error("Schedule had a pickup name without any schedule configured: {}", pickupName);
				pickupNames.remove(pickupName);
				continue;
			}
			sb.append("Pickup " + pickupName + " on");
			int i=0;
			for (TimeOfWeek tow : pickupTimes) {
				i++;
				if (i<=1) {
					sb.append(" " + tow.toStringVerbal());
				} else {
					sb.append(" and " + tow.toStringVerbal());
				}
			}
			sb.append(". ");
			//if (pickupNum != pickupNames.size()) {
			//	// Not the last item
			//	sb.append("<break strength=\"strong\"/>\n");
			//}
			//pickupNum++;
		}
		//sb.append("</speak>");
		String ret = sb.toString();
		return ret;
	}
	
	/**
	 * Create a printable version of the Schedule.
	 * <p>
	 * Used primarily to print the schedule when it is returned
	 * as {@link com.amazon.speech.ui.Card} on Alexa {@link com.amazon.speech.speechlet.SpeechletResponse}.  Also
	 * used by JUnit for testing.
	 * 
	 * @return String representing the pickups and their times
	 * 			during the week.
	 */
	public String toStringPrintable() {
		StringBuilder sb = new StringBuilder();
		for (String pickupName : new ArrayList<String>(pickupNames)) {
			SortedSet<TimeOfWeek> pickupTimes = pickupSchedule.get(pickupName);
			if (pickupTimes == null) {
				log.error("Schedule had a pickup name without any schedule configured: {}", pickupName);
				pickupNames.remove(pickupName);
				continue;
			}
			sb.append("Pickup " + pickupName + " on");
			int i=0;
			for (TimeOfWeek tow : pickupTimes) {
				i++;
				if (i<=1) {
					sb.append(" " + tow.toStringPrintable());
				} else {
					sb.append(" and " + tow.toStringPrintable());
				}
			}
			sb.append(".\n");
		}
		String ret = sb.toString();
		return ret;
	}
}
