package trashday.model;

import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.NumberList;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Summary;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.util.HostInfo;
import net.fortuna.ical4j.util.UidGenerator;
import trashday.CoberturaIgnore;
import trashday.ui.FormatUtils;

/**
 * Data structure for the recurring iCal events in the regular pickup schedule.
 * <p>
 * NOTE: The calendar and its events do NOT keep associated time zone information.  This
 * keeps time zone management outside of the Calendar storage model.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://github.com/ical4j/ical4j">iCal4j</a>
 * @see		<a href="http://ical4j.github.io/docs/ical4j/api/2.0.0/">iCal4j Javadocs</a>
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 * @see		<a href="https://tools.ietf.org/html/rfc5545">RFC 5545: Internet Calendaring and Scheduling Core Object Specification (iCalendar)</a>
 */
public class CalendarEvent {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(CalendarEvent.class);

    /** The iCal calendar event that forms the basis of this Calendar object. */
    private VEvent event = null;
    
    /** Used to create unique event UIDs */
	private static UidGenerator ug = null;

    /** Used to create unique event UIDs because the stock version from ical4j causes five second pauses. */
	private static class MyHostInfo implements HostInfo {
		//TODO: Find something better than net.fortuna.ical4j.util.InetAddressHostInfo.  That takes 5 seconds-per-call on my laptop.  Need to determine what might be best if we eventually allow export of TrashDay Calendars.
		@Override
		public String getHostName() {
			return "AWS";
		}
	}
	
	static {
	    /** Used to create unique event UIDs */
		MyHostInfo hostInfo = new MyHostInfo();
		ug = new UidGenerator(hostInfo, "TrashDaySkill");
	}

    /**
     * Create a new {@link CalendarEvent} from an iCal {@link net.fortuna.ical4j.model.component.VEvent}.
     * 
     * @param event {@link net.fortuna.ical4j.model.component.VEvent} source iCal event
     */
    public CalendarEvent(VEvent event) {
		log.trace("CalendarEvent(event={})", event);
    	this.event = event;
    	
    	// Ensure event name is trimmed and lowercase.
		Summary summary = event.getSummary();
		String eventName = summary.getValue().trim().toLowerCase();
		summary.setValue(eventName);
    }
    
    /**
     * Create a new {@link CalendarEvent} from a given eventName and {@link java.time.LocalDateTime}.
     * 
     * @param eventName name for this event
     * @param ldt start date/time of this event
     */
    public CalendarEvent(String eventName, java.time.LocalDateTime ldt) {
		log.trace("CalendarEvent(eventName={}, ldt={})", eventName, ldt);
		java.time.ZonedDateTime zdt = java.time.ZonedDateTime
				.of(ldt, java.time.ZoneId.systemDefault())
				.truncatedTo(ChronoUnit.MINUTES);
		java.util.Date eventDate = java.util.Date.from(zdt.toInstant());
		DateTime start = new DateTime(eventDate);
		DateTime end = new DateTime(eventDate);
		
		this.event = new VEvent(start, end, eventName.trim().toLowerCase());

		// generate unique identifier..
		Uid uid = ug.generateUid();
		event.getProperties().add(uid);
    }
    
    /**
     * Add a weekly recurrence to this {@link CalendarEvent}.
     * <p>
     * RRULE:FREQ=WEEKLY;BYDAY=TU
     * RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=TU
     * RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=TU
     * 
	 * @param dow {@link java.time.DayOfWeek} this recurrence will happen.
     * @param interval This recurrence happens every N weeks.  For example, 1=weekly, 2=biweekly, etc.
     */
    public void addRecurrenceWeekly(DayOfWeek dow, Integer interval) {
		log.trace("addRecurrenceWeekly(dow={}, interval={})", dow, interval);
		if (interval<1) {
			throw new IllegalArgumentException("Interval must be a positive number.");
		}
		Recur recur = new Recur(Recur.WEEKLY, 0);
		if ( (interval!=0)&&(interval!=1) ) {
			recur.setInterval(interval);
		}
        recur.getDayList().add(DateTimeUtils.getWeekDay(dow, 0));

		RRule rrule = new RRule(recur);
		event.getProperties().add(rrule);
    }
    	
	/**
	 * Add a monthly recurrence to this {@link CalendarEvent}.
	 * <p>
	 * RRULE:FREQ=MONTHLY;BYMONTHDAY=1
	 * RRULE:FREQ=MONTHLY;BYMONTHDAY=-3
	 * 
	 * @param dom {@link java.lang.Integer} must be in [1,31] or [-31,-1]
	 * @param interval Number of months between recurrences.
	 */
	public void addRecurrenceDayOfMonth(Integer dom, Integer interval) {
		log.trace("addRecurrenceMonthly()");
		if (dom>31) {
			throw new IllegalArgumentException("Maximum day of month value (31) exceeded: "+dom);
		}
		if (dom<-31) {
			throw new IllegalArgumentException("Minimum day of month value (-31) exceeded: "+dom);
		}
		if (dom==0) {
			throw new IllegalArgumentException("No such day of month: "+dom);
		}
		if (interval<1) {
			throw new IllegalArgumentException("Interval must be a positive number.");
		}
		
		Recur recur = new Recur(Recur.MONTHLY, 0);
		if ( (interval!=0)&&(interval!=1) ) {
			recur.setInterval(interval);
		}
        recur.getMonthDayList().add(dom);

		RRule rrule = new RRule(recur);
		event.getProperties().add(rrule);
	}
	
	/**
	 * Add a weekday-of-month recurrence to this {@link CalendarEvent}.
	 * <p>
	 * RRULE:FREQ=MONTHLY;BYDAY=1FR
	 * 
	 * @param dow {@link java.time.DayOfWeek} this recurrence will happen.
	 * @param weekNum Recur every Nth dow in the month.  Allowed values [1,5] and [-5,-1]
	 * @param interval Number of months between recurrences.
	 */
	public void addRecurrenceWeekdayOfMonth(DayOfWeek dow, Integer weekNum, Integer interval) {
		log.trace("addRecurrenceWeekdayOfMonth(dow={}, weekNum={}, interval={})", dow, weekNum, interval);
		if (weekNum>5) {
			throw new IllegalArgumentException("Maximum number of weeks-per-month value (5) exceeded: "+weekNum);
		}
		if (weekNum<-5) {
			throw new IllegalArgumentException("Minimum number of weeks-per-month value (-5) exceeded: "+weekNum);
		}
		if (weekNum==0) {
			throw new IllegalArgumentException("No recurrence meaning for weekNum=0");
		}
		if (interval<1) {
			throw new IllegalArgumentException("Interval must be a positive number.");
		}
		
		Recur recur = new Recur(Recur.MONTHLY, 0);
		if ( (interval!=0)&&(interval!=1) ) {
			recur.setInterval(interval);
		}
        recur.getDayList().add(DateTimeUtils.getWeekDay(dow, weekNum));
        
		RRule rrule = new RRule(recur);
		event.getProperties().add(rrule);
	}
	
	/**
	 * Remove any entries in an RRULE's BYDAY list that match the given {@link DayOfWeek}.
	 * 
	 * @param dayList {@link net.fortuna.ical4j.model.WeekDayList} for an RRULE BYDAY entry.
	 * @param dow {@link DayOfWeek} that must match
	 * @return Count of recurrence entries removed.
	 */
	protected int deleteMatchingDays(WeekDayList dayList, DayOfWeek dow) {
		log.debug("deleteMatchingDays(dayList={}, dow={})", dayList, dow);
        int countRemovedDays = 0;
    	for (int indexDayLists=dayList.size()-1; indexDayLists>=0; indexDayLists--) {
    		WeekDay wd = dayList.get(indexDayLists);
    		if (dow.equals(DateTimeUtils.getDayOfWeek(wd))) {
    			dayList.remove(indexDayLists);
    			countRemovedDays++;
    		}
    	}
    	return countRemovedDays;
	}

	/**
	 * Remove any entries in an RRULE's BYDAY list that match the given {@link DayOfWeek}
	 * and offset.  For example, "1FR", "2SA", "3SU", etc.
	 * 
	 * @param dayList {@link net.fortuna.ical4j.model.WeekDayList} for an RRULE BYDAY entry.
	 * @param dow {@link DayOfWeek} that must match
	 * @param offset {@link Integer} that must match the BYDAY offset.
	 * @return Count of recurrence entries removed.
	 */
	protected int deleteMatchingDays(WeekDayList dayList, DayOfWeek dow, Integer offset) {
		log.debug("deleteMatchingDays(dayList={}, dow={}, offset={})", dayList, dow, offset);
        int countRemovedDays = 0;
    	for (int indexDayLists=dayList.size()-1; indexDayLists>=0; indexDayLists--) {
    		WeekDay wd = dayList.get(indexDayLists);
    		if (dow.equals(DateTimeUtils.getDayOfWeek(wd))) {
    			if (offset == wd.getOffset()) {
	    			dayList.remove(indexDayLists);
	    			countRemovedDays++;
    			}
    		}
    	}
    	return countRemovedDays;
	}

	/**
	 * Remove any entries in an RRULE's BYMONTHDAY list that match the given day-of-month.
	 * 
	 * @param dayList {@link net.fortuna.ical4j.model.NumberList} for an RRULE BYMONTHDAY entry.
	 * @param dom {@link Integer} that must match
	 * @return Count of recurrence entries removed.
	 */ 
	protected int deleteMatchingDayOfMonth(NumberList dayList, Integer dom) {
		log.debug("deleteMatchingDayOfMonth(dayList={}, dom={})", dayList, dom);
        int countRemovedDays = 0;
    	for (int indexDayLists=dayList.size()-1; indexDayLists>=0; indexDayLists--) {
    		Integer dayNum = dayList.get(indexDayLists);
    		if (dom == dayNum) {
    			dayList.remove(indexDayLists);
    			countRemovedDays++;
    		}
    	}
    	return countRemovedDays;		
	}
	
	/**
	 * Remove all RRULE entries from this event.
	 * 
	 * @return Count of RRULES removed
	 */
	protected int deleteRecurrencesAll() {
		int countRemovedRrules = 0;
    	PropertyList properties = getProperties();
    	for (int indexProperties=properties.size()-1; indexProperties>=0; indexProperties--) {
    		Property property = properties.get(indexProperties);
    		
    		// Ignore non-RRULE properties.
    		if (! "RRULE".equals(property.getName())) {
    			continue;
    		}
    		
        	properties.remove(indexProperties);
        	countRemovedRrules++;
    	}
    	return countRemovedRrules;
	}
	
	/**
	 * Remove weekly recurrences that match a given interval (e.g. 1=every week,
	 * 2=every other week, etc.).
	 * 
	 * @param dow {@link java.time.DayOfWeek} this recurrence will happen.
	 * @param interval Number of weeks between recurrences.
	 * @return Number of recurrences removed.
	 */
	public int deleteRecurrenceWeekly(DayOfWeek dow, int interval) {
		log.debug("deleteRecurrenceWeekly(dow={}, interval={})", interval);
		if (interval<1) {
			throw new IllegalArgumentException("Interval must be a positive number.");
		}
		int countRemovedRecurrences = 0;
		
    	PropertyList properties = getProperties();
    	for (int indexProperties=properties.size()-1; indexProperties>=0; indexProperties--) {
    		Property property = properties.get(indexProperties);
    		
    		// Ignore non-RRULE properties.
    		if (! "RRULE".equals(property.getName())) {
    			continue;
    		}
    		
            // Ignore non-WEEKLY recurrences
            final RRule rrule = (RRule) property;
    		log.debug("Existing RRULE: {}", rrule);
            Recur recur = rrule.getRecur();
            String ruleFrequency = recur.getFrequency();            
            if (! "WEEKLY".equals(ruleFrequency)) {
            	continue;
            }
            
            // No match if RRULE interval does not match.
            int ruleInterval = recur.getInterval();
            if (ruleInterval<1) { ruleInterval=1; }
    		log.debug("Existing RRULE Interval: {}", ruleInterval);
            if (interval != ruleInterval) {
            	continue;
            }
            
            // Remove any matching BYDAY entries
            countRemovedRecurrences += deleteMatchingDays(recur.getDayList(), dow);
            
			// Remove entire RRULE if needed.
        	if (recur.getDayList().size()==0) {
       			properties.remove(indexProperties);
        	}
    	}
    	
    	return countRemovedRecurrences;
	}
	
	/**
	 * Remove recurrences that happen on given day-of-month from this {@link CalendarEvent}.
	 * <p>
	 * Example RRULES:
	 * RRULE:FREQ=MONTHLY;BYMONTHDAY=1
	 * RRULE:FREQ=MONTHLY;BYMONTHDAY=-3
	 * 
	 * @param dom {@link java.lang.Integer} must be in [1,31] or [-31,-1]
	 * @param interval Number of months between recurrences.
	 * @return Number of recurrences removed from the event.
	 */
	public int deleteRecurrenceDayOfMonth(Integer dom, Integer interval) {
		log.debug("deleteRecurrenceDayOfMonth(dom={}, interval={})", dom, interval);
		if (dom>31) {
			throw new IllegalArgumentException("Maximum day of month value (31) exceeded: "+dom);
		}
		if (dom<-31) {
			throw new IllegalArgumentException("Minimum day of month value (-31) exceeded: "+dom);
		}
		if (dom==0) {
			throw new IllegalArgumentException("No such day of month: "+dom);
		}
		if (interval<1) {
			throw new IllegalArgumentException("Interval must be a positive number.");
		}
		int countRemovedRecurrences = 0;
		
    	PropertyList properties = getProperties();
    	for (int indexProperties=properties.size()-1; indexProperties>=0; indexProperties--) {
    		Property property = properties.get(indexProperties);
    		
    		// Ignore non-RRULE properties.
    		if (! "RRULE".equals(property.getName())) {
    			continue;
    		}
    		
            // Ignore non-MONTHLY recurrences
            final RRule rrule = (RRule) property;
            Recur recur = rrule.getRecur();
            String ruleFrequency = recur.getFrequency();            
            if (! "MONTHLY".equals(ruleFrequency)) {
            	continue;
            }
            
            // No match if RRULE interval does not match.
            int ruleInterval = recur.getInterval();
            if (ruleInterval<1) { ruleInterval=1; }
            if (interval != ruleInterval) {
            	continue;
            }
            
            // Remove any matching BYMONTHDAY entries
            countRemovedRecurrences += deleteMatchingDayOfMonth(recur.getMonthDayList(), dom);
            
			// Remove entire RRULE if needed.
        	if ( (recur.getDayList().size()==0) && (recur.getMonthDayList().size()==0)) {
       			properties.remove(indexProperties);
        	}
    	}
    	return countRemovedRecurrences;
	}
	
	/**
	 * Remove a weekday-of-month recurrences.
	 * <p>
	 * Example RRULE:
	 * RRULE:FREQ=MONTHLY;BYDAY=1FR
	 * RRULE:FREQ=MONTHLY;INTERVAL=2;BYDAY=2SA,4SA
	 * 
	 * @param dow {@link java.time.DayOfWeek} this recurrence will happen.
	 * @param weekNum Recur every Nth dow in the month.  Allowed values [1,5] and [-5,-1]
	 * @param interval Number of months between recurrences.
	 * @return Number of recurrences removed from the event.
	 */
	public int deleteRecurrenceWeekdayOfMonth(DayOfWeek dow, Integer weekNum, Integer interval) {
		log.trace("deleteRecurrenceWeekdayOfMonth(dow={}, weekNum={}, interval={})", dow, weekNum, interval);
		if (weekNum>5) {
			throw new IllegalArgumentException("Maximum number of weeks-per-month value (5) exceeded: "+weekNum);
		}
		if (weekNum<-5) {
			throw new IllegalArgumentException("Minimum number of weeks-per-month value (-5) exceeded: "+weekNum);
		}
		if (weekNum==0) {
			throw new IllegalArgumentException("No recurrence meaning for weekNum=0");
		}
		if (interval<1) {
			throw new IllegalArgumentException("Interval must be a positive number.");
		}
		int countRemovedRecurrences = 0;
		
    	PropertyList properties = getProperties();
    	for (int indexProperties=properties.size()-1; indexProperties>=0; indexProperties--) {
    		Property property = properties.get(indexProperties);
    		
    		// Ignore non-RRULE properties.
    		if (! "RRULE".equals(property.getName())) {
    			continue;
    		}
    		
            // Ignore non-MONTHLY recurrences
            final RRule rrule = (RRule) property;
            Recur recur = rrule.getRecur();
            String ruleFrequency = recur.getFrequency();            
            if (! "MONTHLY".equals(ruleFrequency)) {
            	continue;
            }
            
            // No match if RRULE interval does not match.
            int ruleInterval = recur.getInterval();
            if (ruleInterval<1) { ruleInterval=1; }
            if (interval != ruleInterval) {
            	continue;
            }
            
            // Remove any matching BYDAY entries
            countRemovedRecurrences += deleteMatchingDays(recur.getDayList(), dow, weekNum);
            
			// Remove entire RRULE if needed.
        	if ( (recur.getDayList().size()==0) && (recur.getMonthDayList().size()==0)) {
       			properties.remove(indexProperties);
        	}
    	}
    	return countRemovedRecurrences;
	}
	
	/**
	 * Get the name of this {@link CalendarEvent}
	 * 
	 * @return String with this event name
	 */
	public String getName() {
		log.trace("getName()");
		Summary summary = event.getSummary();
		if (summary==null) { return null; }
		String value = summary.getValue();
		if (value==null) { return null; }
		return value.trim().toLowerCase();
	}
	
	/**
	 * Generate a {@link net.fortuna.ical4j.model.DateTime} from a
	 * {@link java.time.LocalDateTime}.
	 * <p>
	 * NOTE: Uses a ZonedDateTime as an interim step in the conversion.
	 * This is necessary because early Java date/time model was bad, then
	 * ical4j had to compensate, and then we're using the improved Java 8
	 * date/time model.  Grrr.  The use of time zone as an interim step actually
	 * "cancels-out" as the ical4j DateTime actually doesn't use the time
	 * zone later.
	 * 
	 * @param ldt {@link java.time.LocalDateTime} source
	 * @return {@link net.fortuna.ical4j.model.DateTime}
	 */
	protected net.fortuna.ical4j.model.DateTime getIcalDateTime(java.time.LocalDateTime  ldt) {
		log.trace("getIcalDateTime({})", ldt);
		java.time.ZonedDateTime zdt = java.time.ZonedDateTime
				.of(ldt, java.time.ZoneId.systemDefault())
				.truncatedTo(ChronoUnit.MINUTES);
		java.util.Date javaDate = java.util.Date.from(zdt.toInstant());
		net.fortuna.ical4j.model.DateTime icalDateTime = new net.fortuna.ical4j.model.DateTime(javaDate);
		return icalDateTime;
	}
	
	/**
	 * Generate a {@link java.time.LocalDateTime} from a
	 * {@link net.fortuna.ical4j.model.DateTime}.
	 * <p>
	 * NOTE: Uses a ZoneId.systemDefault() as an interim step in the conversion.
	 * This is necessary because early Java date/time model was bad, then
	 * ical4j had to compensate, and then we're using the improved Java 8
	 * date/time model.  Grrr.  The use of time zone as an interim step actually
	 * "cancels-out" as the ical4j DateTime was actually not used anyway.
	 * 
	 * @param icalDate {@link java.time.LocalDateTime} source
	 * @return {@link net.fortuna.ical4j.model.DateTime}
	 */
	protected java.time.LocalDateTime getLocalDateTime(net.fortuna.ical4j.model.Date icalDate) {
		log.trace("getLocalDateTime({})", icalDate);
		return java.time.LocalDateTime.ofInstant(
				icalDate.toInstant(), 
				ZoneId.systemDefault());
	}
	
	/**
	 * Find the first time this pickup occurs after the given {@link java.time.LocalDateTime}.
	 * 
	 * @param ldtStartingPoint Find next pickup after this date/time.
	 * @return {@link java.time.LocalDateTime} of the next pickup time.
	 */
	public java.time.LocalDateTime getNextOccurrence(java.time.LocalDateTime ldtStartingPoint) {
		log.trace("getNextOccurrence({})", ldtStartingPoint);
		// Get event start time
        net.fortuna.ical4j.model.Date eventStartDate = getStartIcalDate();
        
		// Start checking for recurrences this point
		net.fortuna.ical4j.model.DateTime recurrenceCheckStart = getIcalDateTime(ldtStartingPoint);
		
        // Get the event recurrence rule properties.
		net.fortuna.ical4j.model.Date earliestOccurrenceDate = null;
		for (Property property : event.getProperties(Property.RRULE)) {
            final RRule rrule = (RRule) property;
            Recur recur = rrule.getRecur();
            
            Date nextDate = recur.getNextDate(eventStartDate, recurrenceCheckStart);
            if ( (earliestOccurrenceDate==null) || (nextDate.before(earliestOccurrenceDate)) ) {
            	earliestOccurrenceDate = nextDate;
            }
		}
		
		if (earliestOccurrenceDate==null) {
			return null;
		}
		return getLocalDateTime(earliestOccurrenceDate);
	}

	/**
	 * Get the iCal properties for this {@link CalendarEvent}.
	 * 
	 * @return {@link net.fortuna.ical4j.model.PropertyList}
	 */
	public PropertyList getProperties() {
		log.trace("getProperties()");
		return event.getProperties();
	}
	
	/**
	 * Get the iCal properties for this {@link CalendarEvent} that match
	 * the given property name.  For example, getProperties("RRULE") gets
	 * all the recurrence rules for this {@link CalendarEvent}.
	 * 
	 * @param name property name (e.g. "RRULE")
	 * @return {@link net.fortuna.ical4j.model.PropertyList}
	 */
	public PropertyList getProperties(String name) {
		log.trace("getProperties({})", name);
		return event.getProperties(name);
	}
	
	/**
	 * Get all the {@link net.fortuna.ical4j.model.Recur} objects for
	 * this {@link CalendarEvent}.
	 * 
	 * @return List of {@link net.fortuna.ical4j.model.Recur} objects
	 */
	public List<Recur> getRecurrences() {
		List<Recur> recurrences = new ArrayList<Recur>();
		for (Property property : event.getProperties(Property.RRULE)) {
            final RRule rrule = (RRule) property;	
	        Recur recur = rrule.getRecur();
	        recurrences.add(recur);
		}
		return recurrences;
	}
	
	/**
	 * Get all the {@link net.fortuna.ical4j.model.property.RRule} objects for
	 * this {@link CalendarEvent}.
	 * 
	 * @return List of {@link net.fortuna.ical4j.model.property.RRule} objects
	 */
	public List<RRule> getRRules() {
		List<RRule> recurrences = new ArrayList<RRule>();
		for (Property property : event.getProperties(Property.RRULE)) {
            final RRule rrule = (RRule) property;	
	        recurrences.add(rrule);
		}
		return recurrences;
	}
	
	/**
	 * Get a printable form of all the ways this event recurs.  For example, 
	 * "every Tuesday at 7:30 AM", "every other Friday at 7:30 AM (starting this Friday at 7:30 AM)",
	 * and "on the fifteenth at noon."
	 * <p>
	 * Requires {@link java.time.LocalDateTime} base to allow printing of
	 * "every other week" recurrences with starting information that states
	 * "starting this Friday" or "starting Friday after next".
	 * <p>
	 * Returns recurrences as a sorted map.  This allows recurrences to be
	 * sorted for display (like "every Tuesday" comes before "every Friday",
	 * "on the first" before "on the fifteenth", and weekly before biweekly
	 * recurrences).  It also allows the caller to combine the recurrences from
	 * multiple CalendarEvents of the same name to be combined in a sorted order
	 * as well.
	 * 
	 * @param ldtBase {@link java.time.LocalDateTime} as the base for determining if
	 * 			every other week starts "this" week or "next" week.
	 * @return {@link java.util.TreeMap} sorted map of the recurrences on this {@link CalendarEvent}
	 */
	@Deprecated
	@CoberturaIgnore
	public Map<String, String> getRecurrencesPrintable(java.time.LocalDateTime ldtBase) {
		String pickupName = getName();
        log.trace("getRecurrencesPrintable(eventName={}, ldtBase={})", pickupName, ldtBase);
                
        Map<String, String> recurrenceMap = new TreeMap<String, String>();
		for (Property property : event.getProperties(Property.RRULE)) {
            final RRule rrule = (RRule) property;	
	        Recur recur = rrule.getRecur();

	        StringBuilder sb = new StringBuilder();
	        String orderingString = "";
	        switch (recur.getFrequency()) {
	        case "WEEKLY":
	        	switch (recur.getInterval()) {
	        	case -1:
	        	case 0: // Weekly recurrence
	        	case 1:
	        		sb.append("every ");
	        		java.time.LocalDateTime ldtWeeklyEventStart = getStartLocalDateTime();
	        		sb.append(FormatUtils.printableDayOfWeekAndTime(ldtWeeklyEventStart));
	        		
	        		StringBuilder sbOrderString = new StringBuilder();
	        		Formatter formatter = new Formatter(sbOrderString);
		        	formatter.format("A-%03d%d%05d", 1, ldtWeeklyEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtWeeklyEventStart));
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}={}", pickupName, orderingString, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        		break;
	        		
	        	case 2: // Bi-weekly recurrence
	        		
	        		sb.append("every other ");
	        		java.time.LocalDateTime ldtBiWeeklyEventStart = getStartLocalDateTime();
	        		sb.append(FormatUtils.printableDayOfWeekAndTime(ldtBiWeeklyEventStart));
	        		
	        		java.time.LocalDateTime ldtBiWeeklyOccurrence = getNextOccurrence(ldtBase);
	        		if (ldtBiWeeklyOccurrence!=null) {
	        			long days = ChronoUnit.DAYS.between(ldtBase, ldtBiWeeklyOccurrence);	        			
	        			if (days < 7) {
	        				sb.append(" (starting this ");
	        				sb.append(FormatUtils.printableDateAndTimeRelative(ldtBiWeeklyOccurrence, ldtBase));
	        				sb.append(")");
	        			}
	        			else {
	        				sb.append(" (starting ");
	        				sb.append(FormatUtils.printableDateAndTimeRelative(ldtBiWeeklyOccurrence, ldtBase));
	        				sb.append(")");	        				
	        			}
	        		}
	        		
	        		sbOrderString = new StringBuilder();
	        		formatter = new Formatter(sbOrderString);
		        	formatter.format("A-%03d%d%05d", 2, ldtBiWeeklyEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtBiWeeklyEventStart));
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}={}", pickupName, orderingString, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        		break;
	        		
	        	default:// Multi-week recurrence
	        		sb.append("every ");
	        		sb.append(recur.getInterval());
	        		sb.append(" weeks ");
	        		java.time.LocalDateTime ldtMultiWeeklyEventStart = getStartLocalDateTime();
	        		sb.append(FormatUtils.printableDayOfWeekAndTime(ldtMultiWeeklyEventStart));
	        		
	        		java.time.LocalDateTime ldtMultiWeekOccurrence = getNextOccurrence(ldtBase);
	        		if (ldtMultiWeekOccurrence!=null) {
	        			long days = ChronoUnit.DAYS.between(ldtBase, ldtMultiWeekOccurrence);	        			
	        			if (days < 7) {
	        				sb.append(" (starting this ");
	        				sb.append(FormatUtils.printableDateAndTimeRelative(ldtMultiWeekOccurrence, ldtBase));
	        				sb.append(")");
	        			}
	        			else {
	        				sb.append(" (starting ");
	        				sb.append(FormatUtils.printableDateAndTimeRelative(ldtMultiWeekOccurrence, ldtBase));
	        				sb.append(")");	        				
	        			}
	        		}

	        		sbOrderString = new StringBuilder();
	        		formatter = new Formatter(sbOrderString);
		        	formatter.format("A-%03d%d%05d", recur.getInterval(), ldtMultiWeeklyEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtMultiWeeklyEventStart));
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}={}", pickupName, orderingString, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        		break;
	        	}
	        	break;
	        	
	        case "MONTHLY":
        		java.time.LocalDateTime ldtMonthlyEventStart = getStartLocalDateTime();
	        	NumberList dayList = recur.getMonthDayList();
	        	for(Number dayNum : dayList) {
	        		int n = dayNum.intValue();  // n = [-31,-1] and [1,31]
	        		
	        		sb.append("on the ");
	        		sb.append(FormatUtils.printableDayOfMonth(n));
	        		sb.append(" at ");
	        		sb.append(FormatUtils.printableTime(ldtMonthlyEventStart));
	        		
	        		StringBuilder sbOrderString = new StringBuilder();
	        		Formatter formatter = new Formatter(sbOrderString);
	        		if (n>0) {
	        			formatter.format("B-%02d", n);
	        		} else {
			        	formatter.format("B-%02d", 70+n);
	        		}
	        		formatter.close();
		        	orderingString = sbOrderString.toString();		        	
		        	
		        	log.debug("Add {} Recurrence: {}={}", pickupName, orderingString, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        	}
        		break;
	    	default:
	        	throw new IllegalArgumentException("Unknown Recurrence rule frequency: "+recur.getFrequency());
	        }
		}
		return recurrenceMap;
	}
	
	/**
	 * Get a verbal form of all the ways this event recurs.  For example, 
	 * "every Tuesday at 7 30 AM", "every other Friday at 7 30 AM (starting this Friday at 7 30 AM)",
	 * and "on the fifteenth at noon."
	 * <p>
	 * Requires {@link java.time.LocalDateTime} base to allow printing of
	 * "every other week" recurrences with starting information that states
	 * "starting this Friday" or "starting Friday after next".
	 * <p>
	 * Returns recurrences as a sorted map.  This allows recurrences to be
	 * sorted for display (like "every Tuesday" comes before "every Friday",
	 * "on the first" before "on the fifteenth", and weekly before biweekly
	 * recurrences).  It also allows the caller to combine the recurrences from
	 * multiple CalendarEvents of the same name to be combined in a sorted order
	 * as well.
	 * 
	 * @param ldtBase {@link java.time.LocalDateTime} as the base for determining if
	 * 			every other week starts "this" week or "next" week.
	 * @return {@link java.util.TreeMap} sorted map of the recurrences on this {@link CalendarEvent}
	 */
	@Deprecated
	@CoberturaIgnore
	public Map<String, String> getRecurrencesVerbal(java.time.LocalDateTime ldtBase) {
		String pickupName = getName();
        log.trace("getRecurrencesVerbal(eventName={}, ldtBase={})", pickupName, ldtBase);
                
        Map<String, String> recurrenceMap = new TreeMap<String, String>();
		for (Property property : event.getProperties(Property.RRULE)) {
            final RRule rrule = (RRule) property;	
	        Recur recur = rrule.getRecur();

	        StringBuilder sb = new StringBuilder();
	        String orderingString = "";
	        switch (recur.getFrequency()) {
	        case "WEEKLY":
	        	switch (recur.getInterval()) {
	        	case -1:
	        	case 0: // Weekly recurrence
	        	case 1:
	        		sb.append("every ");
	        		java.time.LocalDateTime ldtWeeklyEventStart = getStartLocalDateTime();
	        		sb.append(FormatUtils.verbalDayOfWeekAndTime(ldtWeeklyEventStart));
	        		StringBuilder sbOrderString = new StringBuilder();
	        		Formatter formatter = new Formatter(sbOrderString);
		        	formatter.format("A-%03d%d%05d", 1, ldtWeeklyEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtWeeklyEventStart));
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}", pickupName, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        		break;
	        		
	        	case 2: // Bi-weekly recurrence
	        		sb.append("every other ");
	        		java.time.LocalDateTime ldtBiWeeklyEventStart = getStartLocalDateTime();
	        		sb.append(FormatUtils.verbalDayOfWeekAndTime(ldtBiWeeklyEventStart));
	        		
	        		java.time.LocalDateTime ldtBiWeeklyOccurrence = getNextOccurrence(ldtBase);
	        		if (ldtBiWeeklyOccurrence!=null) {
	        			long days = ChronoUnit.DAYS.between(ldtBase, ldtBiWeeklyOccurrence);	        			
	        			if (days < 7) {
	        				sb.append(" (starting this ");
	        				sb.append(FormatUtils.verbalDateAndTimeRelative(ldtBiWeeklyOccurrence, ldtBase));
	        				sb.append(")");
	        			}
	        			else {
	        				sb.append(" (starting ");
	        				sb.append(FormatUtils.verbalDateAndTimeRelative(ldtBiWeeklyOccurrence, ldtBase));
	        				sb.append(")");	        				
	        			}
	        		}
	        		
	        		sbOrderString = new StringBuilder();
	        		formatter = new Formatter(sbOrderString);
		        	formatter.format("A-%03d%d%05d", 2, ldtBiWeeklyEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtBiWeeklyEventStart));
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}", pickupName, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        		break;
	        		
	        	default:// Multi-week recurrence
	        		sb.append("every ");
	        		sb.append(recur.getInterval());
	        		sb.append(" weeks ");
	        		java.time.LocalDateTime ldtMultiWeeklyEventStart = getStartLocalDateTime();
	        		sb.append(FormatUtils.verbalDayOfWeekAndTime(ldtMultiWeeklyEventStart));
	        		
	        		java.time.LocalDateTime ldtMultiWeekOccurrence = getNextOccurrence(ldtBase);
	        		if (ldtMultiWeekOccurrence!=null) {
	        			long days = ChronoUnit.DAYS.between(ldtBase, ldtMultiWeekOccurrence);	        			
	        			if (days < 7) {
	        				sb.append(" (starting this ");
	        				sb.append(FormatUtils.verbalDateAndTimeRelative(ldtMultiWeekOccurrence, ldtBase));
	        				sb.append(")");
	        			}
	        			else {
	        				sb.append(" (starting ");
	        				sb.append(FormatUtils.verbalDateAndTimeRelative(ldtMultiWeekOccurrence, ldtBase));
	        				sb.append(")");	        				
	        			}
	        		}

	        		sbOrderString = new StringBuilder();
	        		formatter = new Formatter(sbOrderString);
		        	formatter.format("A-%03d%d%05d", recur.getInterval(), ldtMultiWeeklyEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtMultiWeeklyEventStart));
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}", pickupName, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        		break;
	        	}
	        	break;
	        	
	        case "MONTHLY":
        		java.time.LocalDateTime ldtMonthlyEventStart = getStartLocalDateTime();
	        	NumberList dayList = recur.getMonthDayList();
	        	for(Number dayNum : dayList) {
	        		int n = dayNum.intValue();  // n = [-31,-1] and [1,31]
	        		
	        		sb.append("on the ");
	        		sb.append(FormatUtils.verbalDayOfMonth(n));
	        		sb.append(" at ");
	        		sb.append(FormatUtils.verbalTime(ldtMonthlyEventStart));
	        		
	        		StringBuilder sbOrderString = new StringBuilder();
	        		Formatter formatter = new Formatter(sbOrderString);
	        		if (n>0) {
	        			formatter.format("B-%02d", n);
	        		} else {
			        	formatter.format("B-%02d", 70+n);
	        		}
	        		formatter.close();
		        	orderingString = sbOrderString.toString();
		        	
		        	log.debug("Add {} Recurrence: {}", pickupName, sb.toString());
		        	recurrenceMap.put(orderingString, sb.toString());
	        	}
        		break;
	    	default:
	        	throw new IllegalArgumentException("Unknown Recurrence rule frequency: "+recur.getFrequency());
	        }
		}
		return recurrenceMap;
	}
	
	/**
	 * Get event's start date/time.
	 * 
	 * @return {@link net.fortuna.ical4j.model.Date}
	 */
	public net.fortuna.ical4j.model.Date getStartIcalDate() {
		log.trace("getStartIcalDate()");
		// Get event start time
		final DtStart eventStartProperty = (DtStart) event.getProperty(Property.DTSTART);
        return eventStartProperty.getDate();
	}
		
	/**
	 * Get event's start date/time.
	 * 
	 * @return {@link java.time.LocalDateTime}
	 */
	public java.time.LocalDateTime getStartLocalDateTime() {
		log.trace("getStartLocalDateTime()");
		// Get event start time
		final DtStart eventStartProperty = (DtStart) event.getProperty(Property.DTSTART);
		net.fortuna.ical4j.model.Date dateStart = eventStartProperty.getDate();
		java.time.Instant instant = dateStart.toInstant();
		return java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
	}
		
	/**
	 * Get the iCal {@link net.fortuna.ical4j.model.component.VEvent} for this
	 * {@link CalendarEvent}.
	 * 
	 * @return {@link net.fortuna.ical4j.model.component.VEvent}
	 */
	public VEvent getVEvent() {
		log.trace("getVEvent()");
		return event;
	}
	
	/**
	 * Check if this event has existing recurrence rules.
	 * 
	 * @return {@code true} if this event has one or more recurrences configured.
	 */
	public boolean hasRrules() {
		log.trace("hasRrules()");
		PropertyList rrulesList = event.getProperties("RRULE");
		if (rrulesList == null) { return false; }
		if (rrulesList.size() < 1) { return false; }
		return true;
	}
	
	/**
	 * Check if this {@link CalendarEvent} matches pickup name and
	 * time of day.  Serves as a quick check on basic attributes before looking
	 * at event recurrences.
	 * 
	 * @param pickupName event name match
	 * @param ltEvent time-of-day match information
	 * @return {@code true} if all given arguments match for this {@link CalendarEvent}
	 */
	public boolean matchesNameTod(String pickupName, java.time.LocalTime ltEvent) {
		log.trace("matchesNameDowTod(pickupName={}, ltEvent={})", pickupName, ltEvent);
		String matchEventName = pickupName.trim().toLowerCase();
		java.time.LocalTime matchTOD = ltEvent.truncatedTo(ChronoUnit.MINUTES);

    	if (! matchEventName.equals(getName())) {
    		return false;
    	}
    	log.debug("matches: name={}", pickupName);
    	
    	java.time.LocalDateTime eventStart = getStartLocalDateTime();
    	if (! matchTOD.equals(eventStart.truncatedTo(ChronoUnit.MINUTES).toLocalTime())) {
        	log.debug("matches: fail event tod={}", eventStart.truncatedTo(ChronoUnit.MINUTES).toLocalTime());
    		return false;
    	}
    	log.debug("matches: tod={}", matchTOD);
    	
    	return true;
	}
}
