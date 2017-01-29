package trashday.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.jonpeterson.jackson.module.versioning.JsonVersionedModel;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;

/**
 * Data structure for storing user activity log.
 * <p>
 * The log keeps a weekly total of the type and number of user interactions
 * Trash Day sees per-user.  Application stores up to 12 weeks of data to
 * help analyze user usage patterns.
 * <p>
 * The class implements Jackson Model Versioning using the 
 * "@JsonVersionedModel" and {@link ToCurrentIntentLog} class.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 *
 */
@JsonIgnoreProperties
@JsonVersionedModel(currentVersion = "1", toCurrentConverterClass = ToCurrentIntentLog.class)
public class IntentLog {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(IntentLog.class);
    
	/** A Jackson object mapper configured to handle Java 8 LocalDateTime objects and Jon Peterson's object versioning module. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
 		   .registerModule(new ParameterNamesModule())
 		   .registerModule(new Jdk8Module())
 		   .registerModule(new JavaTimeModule())
 		   .registerModule(new VersioningModule())
 		;
    
    /** Format to obtain LocalDateTime in terms of year--week-of-year. */
    private static DateTimeFormatter formatterYearWeek = DateTimeFormatter.ofPattern("YYYY-ww");

    Map<String, Map<String, Integer>> intentLog;
    
    /**
     * Create an empty IntentLog.
     */
    public IntentLog() {
    	intentLog = new TreeMap<String, Map<String,Integer>>();
    }
	
    /**
     * Increment the IntentLog count for the given intentName that occurred
     * in the given yearWeek (eg. 2014-38).  IntentLog stores counts each intentName occurred
     * on a per-week basis.
     * 
     * @param yearWeek String in form YYYY-ww when the intentName occurred
     * @param intentName String of the intent invoked by the user of the application.
     * @param count Integer of how many times this intentName occurred within 
     * 		the week corresponding to the ldtEntry
     */
    public void incrementIntent(String yearWeek, String intentName, Integer count) {
    	log.trace("incrementIntent(yearWeek={}, intentName={}, count={}", yearWeek, intentName, count);
    	
    	Map<String,Integer> intentCounts;
    	if (intentLog.containsKey(yearWeek)) {
    		intentCounts = intentLog.get(yearWeek);
    	} else {
    		intentCounts = new TreeMap<String,Integer>();
    		intentLog.put(yearWeek, intentCounts);
    	}
    	if (intentCounts.containsKey(intentName)) {
    		Integer i = intentCounts.get(intentName);
    		intentCounts.put(intentName, i+count);
    	} else {
    		intentCounts.put(intentName, count);
    	}
    }
    
    /**
     * Increment the IntentLog count for the given intentName that occurred
     * at ldtEntry time.  IntentLog stores counts each intentName occurred
     * on a per-week basis.
     * 
     * @param ldtEntry LocalDateTime when the intentName occurred
     * @param intentName String of the intent invoked by the user of the application.
     * @param count Integer of how many times this intentName occurred within 
     * 		the week corresponding to the ldtEntry
     */
    public void incrementIntent(LocalDateTime ldtEntry, String intentName, Integer count) {
    	log.trace("incrementIntent(ldtEntry={}, intentName={}, count={}", ldtEntry, intentName, count);
    	String yearWeek = ldtEntry.format(formatterYearWeek);
    	incrementIntent(yearWeek, intentName, count);
    }
    
    /**
     * Increment the IntentLog count by one for the given intentName that occurred
     * at ldtEntry time.  IntentLog stores counts each intentName occurred
     * on a per-week basis.
     * 
     * @param ldtEntry LocalDateTime when the intentName occurred
     * @param intentName String of the intent invoked by the user of the application.
     */
    public void incrementIntent(LocalDateTime ldtEntry, String intentName) {
    	log.trace("incrementIntent(ldtEntry={}, intentName={}", ldtEntry, intentName);
    	incrementIntent(ldtEntry, intentName, 1);
    }
    
    /**
     * Basic getter for the underlying log data.
     * 
     * @return Map of each week's user interaction counts.
     */
    public Map<String, Map<String,Integer>> getLog() {
    	return intentLog;
    }
    
    /**
     * Joins the log data from the given IntentLog into this IntentLog.  Used 
     * when merging new log entries from a current session into the stored
     * entries in the Dynamo DB.
     * 
     * @param otherLog IntentLog to be merged in to this one
     */
    public void join(IntentLog otherLog) {
    	Map<String, Map<String,Integer>> other = otherLog.getLog();
    	for (Map.Entry<String, Map<String,Integer>> entry : other.entrySet()) {
    		String yearWeek = entry.getKey();
    		Map<String,Integer> intentCounts = entry.getValue();
    		for (Map.Entry<String, Integer> countEntry : intentCounts.entrySet()) {
    			String intentName = countEntry.getKey();
    			Integer count = countEntry.getValue();
    			incrementIntent(yearWeek, intentName, count);
    		}
    	}
    }
    
    /**
     * Prune to a maximum number of weeks of log data kept.  Currently keep up to 12
     * weeks of user interaction log.  Called before saving IntentLog objects
     * to Dynamo DB.
     * 
     * @param weeksEntriesToKeep int Number of weeks to keep in this IntentLog.
     */
    public void prune(int weeksEntriesToKeep) {
    	Set<String> weeks = intentLog.keySet();
    	int pruneCount = weeks.size() - weeksEntriesToKeep;
    	if (pruneCount>0) {
    		Set<String> weeksToRemove = new HashSet<String>();
    		for (String week : weeks) {
    			pruneCount--;
    			if (pruneCount>=0) {
    				weeksToRemove.add(week);
    			}
    		}
    		weeks.removeAll(weeksToRemove);
    	}
    }
    
	/**
	 * Create a JSON version of this object.
	 * 
	 * @return String with JSON version of this Schedule.
	 */
	public String toJson() {
		try {
			log.info("IntentLog convert: {}", this);
			return OBJECT_MAPPER.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("Unable to convert schedule.", e);
		}
	}
	
	/**
	 * Prefer callers to specify what kind of printed output they need, rather
	 * than just any String.
	 */
	@Deprecated
	public String toString() {
		return toStringPrintable();
	}

	/**
	 * Generate a printed representation of the Intent Log.
	 * 
	 * @return String showing the entire log
	 */
	public String toStringPrintable() {
		StringBuilder sb = new StringBuilder();
		
    	for (Map.Entry<String, Map<String,Integer>> entry : intentLog.entrySet()) {
    		String yearWeek = entry.getKey();
    		sb.append(yearWeek+":\n");
    		Map<String,Integer> intentCounts = entry.getValue();
    		int i = 0;
    		Set<Map.Entry<String, Integer>> es = intentCounts.entrySet();
    		int setSize = es.size();
    		if (setSize > 0) {
    			sb.append("  ");
	    		for (Map.Entry<String, Integer> countEntry : es) {
	    			i++;
	    			String intentName = countEntry.getKey();
	    			Integer count = countEntry.getValue();
	    			if (i < setSize) {
	    				// Not last entry
	    				sb.append(intentName+"="+count+", ");
	    			} else {
	    				// Last in set
	    				sb.append(intentName+"="+count);
	    			}
	    		}
	    		sb.append("\n");
    		}
    	}
    	
    	return sb.toString();
	}
}
