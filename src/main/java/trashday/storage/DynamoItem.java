package trashday.storage;

import java.io.IOException;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverted;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.github.jonpeterson.jackson.module.versioning.VersioningModule;

import net.fortuna.ical4j.data.ParserException;
import trashday.model.Calendar;
import trashday.model.IntentLog;
import trashday.model.Schedule;

/**
 * Class for storing a table item for each application user
 * in a Dynamo DB table.  Each item contains the user {@link trashday.model.Schedule}
 * and {@link java.util.TimeZone} data.
 * 
 * @author	J. Todd Baldwin
 */
@SuppressWarnings("deprecation")
@DynamoDBTable(tableName = "TrashDayScheduleData")
public class DynamoItem {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DynamoItem.class);
    
    /** The user's customer id from {@link com.amazon.speech.speechlet.Session#getUser} */
    private String customerId;
    /** This user's Trash Day calendar. */
    private Calendar calendar;
    /** The user's intent log. */
    private IntentLog intentLog;
    /** This user's Trash Day schedule. */
    private Schedule schedule;
    /** This user's TimeZone information. */
    private TimeZone timeZone;
    
	/** A Jackson object mapper configured to handle Java 8 LocalDateTime objects and Jon Peterson's object versioning module. */
    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
 		   .registerModule(new ParameterNamesModule())
 		   .registerModule(new Jdk8Module())
 		   .registerModule(new JavaTimeModule())
 		   .registerModule(new VersioningModule())
 		;

	/**
	 * Make a new, empty {@link DynamoItem}.
	 */
	public DynamoItem() {
		this.customerId = null;
		this.calendar = null;
		this.intentLog = null;
		this.schedule = null;
		this.timeZone = null;
	}

	/**
	 * Make a shallow copy of the given {@link DynamoItem}.
	 * 
	 * @param source DynamoItem to be copied from.
	 */
	public DynamoItem(DynamoItem source) {
		this.customerId = source.customerId;
		this.calendar = source.calendar;
		this.intentLog = source.intentLog;
		this.schedule = source.schedule;
		this.timeZone = source.timeZone;
	}

    /**
     * Get the customerId attribute.
     * <p>
     * The DynamoDB directive specifies the user's id from 
     * {@link com.amazon.speech.speechlet.Session#getUser} as the primary hash key on 
     * the Dynamo DB table when we save this object.
     * 
     * @return User's id
     */
    @DynamoDBHashKey(attributeName = "CustomerId")
    public String getCustomerId() {
    	log.trace("getCustomerId()={}", customerId);
        return customerId;
    }

    /**
     * Get the schedule attribute.  After {@link DynamoDao#readUserData}
     * has loaded the correct table item for this user into this
     * object, use this getter to retrieve the {@link trashday.model.Schedule}
     * object for this user.
     * <p>
     * The DynamoDB directives specify how to serialize the
     * {@link trashday.model.Schedule} object into the table's "Data" field.
     * 
     * @return User's {@link trashday.model.Schedule}
     */
    @DynamoDBAttribute(attributeName = "Calendar")
    @DynamoDBTypeConverted(converter = CalendarConverter.class)
    public Calendar getCalendar() {
    	log.trace("getCalendar()={}", calendar);
        return calendar;
    }
    
    /**
     * Get the schedule attribute.  After {@link DynamoDao#readUserData}
     * has loaded the correct table item for this user into this
     * object, use this getter to retrieve the {@link trashday.model.IntentLog}
     * object for this user.
     * <p>
     * The DynamoDB directives specify how to serialize the
     * {@link trashday.model.IntentLog} object into the table's "Data" field.
     * 
     * @return User's {@link trashday.model.IntentLog}
     */
    @DynamoDBAttribute(attributeName = "IntentLog")
    @DynamoDBTypeConverted(converter = IntentLogConverter.class)
    public IntentLog getIntentLog() {
    	log.trace("getIntentLog()={}", intentLog);
        return intentLog;
    }
    
    /**
     * Get the schedule attribute.  After {@link DynamoDao#readUserData}
     * has loaded the correct table item for this user into this
     * object, use this getter to retrieve the {@link trashday.model.Schedule}
     * object for this user.
     * <p>
     * The DynamoDB directives specify how to serialize the
     * {@link trashday.model.Schedule} object into the table's "Data" field.
     * 
     * @return User's {@link trashday.model.Schedule}
     */
    @DynamoDBAttribute(attributeName = "Schedule")
    @DynamoDBTypeConverted(converter = ScheduleConverter.class)
    @Deprecated
    public Schedule getSchedule() {
    	log.trace("getSchedule()={}", schedule);
        return schedule;
    }
    
    /**
     * Get the timeZone attribute.  After {@link DynamoDao#readUserData}
     * has loaded the correct table item for this user into this
     * object, use this getter to retrieve the {@link java.util.TimeZone}
     * object for this user.
     * 
     * @return User's {@link java.util.TimeZone}
     */
    @DynamoDBAttribute(attributeName = "TimeZone")
    public TimeZone getTimeZone() {
    	log.trace("getTimeZone()={}", timeZone);
    	return timeZone;
    }
    
    /**
     * Clear the intentLog attribute.
     */
    public void clearIntentLog() {
    	log.trace("clearIntentLog()");
    	this.intentLog = null;
    }
    
    /**
     * Clear the schedule attribute.
     */
    @Deprecated
    public void clearSchedule() {
    	log.trace("clearSchedule()");
    	this.schedule = null;
    }
    
    /**
     * Set the customerId attribute.  Used to set the correct
     * id prior to a database save or load request.
     * 
     * @param customerId String User's id
     */
    public void setCustomerId(String customerId) {
    	log.trace("setCustomerId({})", customerId);
        this.customerId = customerId;
    }

    /**
     * Set the calendar attribute.  Used to set the correct
     * information before a database save request ({@link DynamoDao#writeUserData}).
     * 
     * @param calendar Calendar data to be stored
     */
    public void setCalendar(Calendar calendar) {
    	log.trace("setCalendar({})", calendar);
        this.calendar = calendar;
    }
    
    /**
     * Set the intentLog attribute.  Used to set the correct
     * information before a database save request ({@link DynamoDao#writeUserData}).
     * 
     * @param intentLog IntentLog data to be stored
     */
    public void setIntentLog(IntentLog intentLog) {
    	log.trace("setIntentLog({})", intentLog);
        this.intentLog = intentLog;
    }
    
    /**
     * Set the schedule attribute.  Used to set the correct
     * information before a database save request ({@link DynamoDao#writeUserData}).
     * 
     * @param schedule Schedule data to be stored
     */
    @Deprecated
    public void setSchedule(Schedule schedule) {
    	log.trace("setSchedule({})", schedule);
        this.schedule = schedule;
    }
    
    /**
     * Set the timeZone attribute.  Used to set the correct
     * information before a database save request ({@link DynamoDao#writeUserData}).
     * 
     * @param timeZone TimeZone data to be stored
     */
    public void setTimeZone(TimeZone timeZone) {
    	log.trace("setTimeZone({})", timeZone);
    	this.timeZone = timeZone;
    }
    
    /**
     * Class to handle serialization for {@link trashday.model.Calendar} objects.
     * 
     * @author J. Todd Baldwin
     */
    public static class CalendarConverter implements DynamoDBTypeConverter<String, Calendar> {
    	/**
    	 * Actually serialize a {@link trashday.model.Calendar} object.
    	 */
    	@Override
    	public String convert(Calendar calendar) {
			log.trace("convert: {}", calendar);
			if (calendar.isEmpty()) {
				return "(empty)";
			}
			return calendar.toStringRFC5545();
    	}

    	/**
    	 * Actually de-serialize a {@link trashday.model.Calendar} object.
    	 */
    	@Override
    	public Calendar unconvert(String value) {
			log.trace("unconvert: {}", value);
			try {
				if ("(empty)".equals(value.trim())) {
					return new Calendar();
				}
				return new Calendar(value);
			} catch (IOException ex) {
    			throw new IllegalStateException("Unable to convert calendar value from storage string."+ex.getMessage());
			} catch (ParserException ex) {
    			throw new IllegalStateException("Unable to convert calendar value from storage string."+ex.getMessage());
			}
    	}
   	}    

    /**
     * Class to handle serialization for {@link trashday.model.Schedule} objects.
     * Relies on FasterXML's Jackson Project for JSON-based 
     * serialization/deserialization.
     * 
     * @author J. Todd Baldwin
     * @see		<a href="https://github.com/FasterXML/jackson">FasterXML Jackson Project Home</a>
     * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
     */
    public static class ScheduleConverter implements DynamoDBTypeConverter<String, Schedule> {

    	/**
    	 * Actually serialize a {@link trashday.model.Schedule} object using the
    	 * Jackson object mapper.
    	 */
    	@Override
    	public String convert(Schedule schedule) {
    		try {
    			log.trace("convert: {}", schedule);
    			return OBJECT_MAPPER.writeValueAsString(schedule);
    		} catch (JsonProcessingException e) {
    			throw new IllegalStateException("Unable to convert schedule.", e);
    		}
    	}

    	/**
    	 * Actually de-serialize a {@link trashday.model.Schedule} object using the
    	 * Jackson object mapper.
    	 */
    	@Override
    	public Schedule unconvert(String value) {
    		try {
    			log.trace("unconvert: {}", value);
    			return OBJECT_MAPPER.readValue(value, new TypeReference<Schedule>() { } );
    		} catch (Exception e) {
    			throw new IllegalStateException("Unable to unconvert schedule value.", e);
    		}    		
    	}
   	}    
    
    /**
     * Class to handle serialization for {@link trashday.model.IntentLog} objects.
     * Relies on FasterXML's Jackson Project for JSON-based 
     * serialization/deserialization.
     * 
     * @author J. Todd Baldwin
     * @see		<a href="https://github.com/FasterXML/jackson">FasterXML Jackson Project Home</a>
     * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
     */
    public static class IntentLogConverter implements DynamoDBTypeConverter<String, IntentLog> {
    	/**
    	 * Actually serialize a {@link trashday.model.IntentLog} object using the
    	 * Jackson object mapper.
    	 */
    	@Override
    	public String convert(IntentLog intentLog) {
    		try {
    			log.trace("convert: {}", intentLog);
    			return OBJECT_MAPPER.writeValueAsString(intentLog);
    		} catch (JsonProcessingException e) {
    			throw new IllegalStateException("Unable to convert intent log.", e);
    		}
    	}

    	/**
    	 * Actually de-serialize a {@link trashday.model.IntentLog} object using the
    	 * Jackson object mapper.
    	 */
    	@Override
    	public IntentLog unconvert(String value) {
    		try {
    			log.trace("unconvert: {}", value);
    			return OBJECT_MAPPER.readValue(value, new TypeReference<IntentLog>() { } );
    		} catch (Exception e) {
    			throw new IllegalStateException("Unable to unconvert intent log value.", e);
    		}    		
    	}
   	}    

}
