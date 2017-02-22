package trashday.model;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.ComponentList;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.NumberList;
import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationException;
import trashday.CoberturaIgnore;
import trashday.ui.FormatUtils;

/**
 * Data structure for a regular pickup schedule.  Use an iCalendar-based calendar representation
 * to allow for future integration options and recurring event handling.
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
public class Calendar {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(Calendar.class);
    
    /** The iCal calendar that forms the basis of this Calendar object. */
    private net.fortuna.ical4j.model.Calendar cal = null;
	
	/**
	 * Create an empty pickups calendar.
	 */
	public Calendar() {
		log.trace("Calendar(): Create empty calendar");
		cal = new net.fortuna.ical4j.model.Calendar();
		cal.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 2.0.0//EN"));
		cal.getProperties().add(Version.VERSION_2_0);
		cal.getProperties().add(CalScale.GREGORIAN);
	}
	
	/**
	 * Create a calendar from a {@link trashday.model.Schedule}
	 * 
	 * @param schedule Schedule to be converted into a Calendar object.
	 */
	@SuppressWarnings("deprecation")
	public Calendar(Schedule schedule) {
		log.trace("Calendar(): Create from schedule={}", schedule);

		cal = new net.fortuna.ical4j.model.Calendar();
		cal.getProperties().add(new ProdId("-//Ben Fortuna//iCal4j 2.0.0//EN"));
		cal.getProperties().add(Version.VERSION_2_0);
		cal.getProperties().add(CalScale.GREGORIAN);
		
		loadFromSchedule(schedule);
	}
	
	/**
	 * Create a calendar from a {@link trashday.model.Schedule}
	 * @param icalString String with an iCalendar text.
	 * @throws ParserException Failed to understand the provided ical source.
	 * @throws IOException Could not read the ical source string correctly.
	 */
	public Calendar(String icalString) throws IOException, ParserException {
		log.trace("Calendar(): Create from icalString={}", icalString);
		
		StringReader sin = new StringReader(icalString);
		CalendarBuilder builder = new CalendarBuilder();
		cal = builder.build(sin);
	}
	
	/**
	 * Continue to support version 1 pickup schedules in {@link trashday.model.Schedule}
	 * form by converting all {@link trashday.model.Schedule} objects into the current
	 * ical-based {@link trashday.model.Calendar}.
	 * 
	 * @param schedule Schedule of weekly pickups in deprecated, version 1 {@link trashday.model.Schedule} object.
	 * @return List of pickup names found and converted from {@link trashday.model.Schedule}
	 */
	@SuppressWarnings("deprecation")
	public List<String> loadFromSchedule(Schedule schedule) {
		log.trace("loadFromSchedule({})", schedule.toJson());
		List<String> pickupNames = schedule.getPickupNames();
		
		for (String pickupName : pickupNames) {
			Set<TimeOfWeek> tows = schedule.getPickupSchedule(pickupName);
			for (TimeOfWeek tow : tows) {
				log.info("loadFromSchedule event: {} pickup on {} at {}", pickupName, tow.getDayOfWeek(), tow.getTimeOfDay());
		    	pickupAddWeekly(LocalDateTime.now(), pickupName, tow.getDayOfWeek(), tow.getTimeOfDay());
			}
		}
		
		return pickupNames;
	}

	/**
	 * Create an example schedule.  Used for JUnit testing.
	 */
	public void initBasicExampleCalendar() {
		log.trace("initBasicExampleCalendar");
		
		// Trash: Tuesday morning
		CalendarEvent event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		eventAdd(event);
		
		// Trash: Friday morning
		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 2, 3, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 1);
		eventAdd(event);
		
		// Recycling: Friday morning
		event = new CalendarEvent("Recycling", java.time.LocalDateTime.of(2017, 2, 3, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 2);
		eventAdd(event);
	}
	
	/**
	 * Create an example schedule.  Used for JUnit testing.
	 */
	public void initComplexExampleCalendar() {
		log.trace("initComplexExampleCalendar");
		
		// Trash: Tuesday morning
		CalendarEvent event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 1, 31, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.TUESDAY, 1);
		eventAdd(event);
		
		// Trash: Friday morning
		event = new CalendarEvent("Trash", java.time.LocalDateTime.of(2017, 2, 3, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 1);
		eventAdd(event);
		
		// Recycling: Friday morning
		event = new CalendarEvent("Recycling", java.time.LocalDateTime.of(2017, 2, 3, 7, 30));
		event.addRecurrenceWeekly(DayOfWeek.FRIDAY, 2);
		eventAdd(event);
		
		// Lawn Waste: First of month
		event = new CalendarEvent("Lawn Waste", java.time.LocalDateTime.of(2017, 2, 1, 12, 00));
		event.addRecurrenceDayOfMonth(1, 1);
		eventAdd(event);
		
		// Lawn Waste: Fifteenth of month
		event = new CalendarEvent("Lawn Waste", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceDayOfMonth(15, 1);
		eventAdd(event);
		
		// Scrap Metal: Last day of month
		event = new CalendarEvent("scrap metal", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceDayOfMonth(-1, 1);
		eventAdd(event);		
		
		// Mortgage: Five days before end of month day of month
		event = new CalendarEvent("mortgage", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceDayOfMonth(-5, 1);
		eventAdd(event);
		
		// Dry Cleaning: Second Saturday of every month
		event = new CalendarEvent("dry cleaning", java.time.LocalDateTime.of(2017, 2, 15, 12, 00));
		event.addRecurrenceWeekdayOfMonth(DayOfWeek.SATURDAY, 2, 1);
		eventAdd(event);
		
		// Hockey Team: Second-to-last Saturday of every month
		event = new CalendarEvent("hockey team", java.time.LocalDateTime.of(2017, 2, 15, 9, 00));
		event.addRecurrenceWeekdayOfMonth(DayOfWeek.SATURDAY, -2, 1);
		eventAdd(event);
	}
	
	/**
	 * Delete all entries from the calendar.
	 * 
	 * @return True if this schedule had any pickups scheduled that had to be removed.
	 */
	public Boolean deleteEntireSchedule() {
		log.trace("deleteEntireSchedule()");
		
		// Delete all event components...
		boolean eventsRemoved=false;
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (int indexComponents=components.size()-1; indexComponents>=0; indexComponents--) {
			CalendarComponent component = components.get(indexComponents);
        	if (Component.VEVENT.equals(component.getName())) {
        		components.remove(indexComponents);
        		eventsRemoved=true;
        	}
		}
		return eventsRemoved;
	}
	
	/**
	 * Add {@link trashday.model.CalendarEvent} into the {@link trashday.model.Calendar}.
	 * <p>
	 * NOTE: All events added to our calendar must have:
	 * (a) a pickup name in the Summary field.
	 * (b) at least one recurrence rule.
	 * 
	 * @param event {@link trashday.model.CalendarEvent} to add
	 * @return {@code true} if event was added.  {@code false} if event was not added due to a duplicate already existing.
	 */
	public Boolean eventAdd(CalendarEvent event) {
		log.trace("eventAdd({})", event);
		if (event.getName()==null) {
			throw new IllegalArgumentException("Will not accept events without names.");
		}
		List<RRule> rRules = event.getRRules();
		if ( (rRules==null) || (rRules.size()==0) ) {
			throw new IllegalArgumentException("Will not accept events without at least one RRULE.");
		}
		if (has(event)) {
			return false;
		}
		cal.getComponents().add(event.getVEvent());
		return true;
	}
	
	/**
	 * Get all the iCal events in the {@link trashday.model.Calendar}.
	 * 
	 * @return List of {@link CalendarEvent}
	 */
	public List<CalendarEvent> getEvents() {
		log.trace("getEvents()");
		List<CalendarEvent> events = new ArrayList<CalendarEvent>();
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (CalendarComponent component : components) {
			String name = component.getName();
			if (name.equals("VEVENT")) {
	        	CalendarEvent event = new CalendarEvent( (VEvent) component );
				events.add(event);				
			}
		}
		return events;
	}
	
	/**
	 * Get all the iCal events in the {@link trashday.model.Calendar} for the
	 * given pickup name.
	 * 
	 * @param pickupName find all events for this pickup name (e.g. "trash", "recycling", etc.)
	 * @return List of {@link CalendarEvent}
	 */
	public List<CalendarEvent> getEvents(String pickupName) {
		log.trace("getEvents(pickupName={})", pickupName);
		if (pickupName==null) { return null; }
		
		String pickupCheck = pickupName.trim().toLowerCase();
		List<CalendarEvent> events = new ArrayList<CalendarEvent>();
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (CalendarComponent component : components) {
			String name = component.getName();
			if (name.equals("VEVENT")) {
	        	CalendarEvent event = new CalendarEvent( (VEvent) component );
				if (event.getName().equals(pickupCheck)) {
					events.add(event);				
				}
			}
		}
		return events;
	}
	
	/**
	 * Test if the {@link trashday.model.Calendar} contains any pickup events.
	 * 
	 * @return {@code true} if pickup events exist in this calendar
	 */
	public boolean isEmpty() {
		log.trace("isEmpty()");
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (CalendarComponent component : components) {
			String name = component.getName();
			if (name.equals("VEVENT")) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Test if this event already exists in the calendar.  Checks to find any
	 * event that has same summary (pickup name) and matching RRULE.
	 * <p>
	 * A generalized algorithm would be better.  Since there is already an ical4j
	 * open issue, I'm just doing something sufficient for events created within
	 * this one application.  If/when the application starts integrating with external
	 * iCalendars, this needs to be upgraded to handle generalized cases.
	 * <p>
	 * Assertions for all existing calendar entries:
	 * Events are always active.  No RRULE COUNTs or DTENDs.
	 * 
	 * Assertions for all newEvents:
	 * Events are always active.  No RRULE COUNTs or DTENDs.
	 * One and only one RRULE per event
	 * One and only one entry in BYDAY or BYMONTHDAY in the RRULE
	 * RRULE:FREQ=WEEKLY;BYDAY=TU
     * RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=TU
     * RRULE:FREQ=WEEKLY;INTERVAL=2;BYDAY=TU
     * RRULE:FREQ=MONTHLY;BYMONTHDAY=1
	 * RRULE:FREQ=MONTHLY;BYMONTHDAY=-3
	 * RRULE:FREQ=MONTHLY;BYDAY=1FR
	 * 
	 * @param newEvent {@link trashday.model.CalendarEvent} the we are checking may
	 * 			already have a "duplicate" in the calendar.
	 * @return {@code true} if this newEvent already exists in the calendar.
	 * @see   <a href="https://github.com/ical4j/ical4j/issues/42">Can ical4j identify exceptions to recurring calendar events?</a>
	 */
	public Boolean has(CalendarEvent newEvent) {
		log.debug("has(newEvent={})", newEvent.getVEvent());
		
		// First, gather information from the newEvent and confirm the new event meets our assertions.
		String newEventName = newEvent.getName();
		if (newEvent.getName()==null) {
			throw new IllegalArgumentException("Will not accept events without names.");
		}
		List<RRule> newRRules = newEvent.getRRules();
		if ( (newRRules==null) || (newRRules.size()!=1) ) {
			throw new IllegalArgumentException("Will not accept events without exactly one RRULE. size="+newRRules.size());
		}
		RRule newRRule = newRRules.get(0);
		Recur newRecur = newRRule.getRecur();
		String newFrequency = newRecur.getFrequency();
		if ( (! newFrequency.equals("WEEKLY")) && (! newFrequency.equals("MONTHLY")) ) {
			throw new IllegalArgumentException("Will not accept RRULEs that are not WEEKLY or MONTHLY frequency.");
		}
		int newInterval = newRecur.getInterval();
		if (newInterval==0) { newInterval=1; }
		WeekDayList newDayList = newRecur.getDayList();
		NumberList newMonthDayList = newRecur.getMonthDayList();
		WeekDay newDay = null;
		Integer newMonthDay = null;		
		switch (newFrequency) {
		case "WEEKLY":
			// Confirm exactly one BYDAY entry.
			if ( (newDayList==null) || (newDayList.size()!=1) ) {
				throw new IllegalArgumentException("Will not accept WEEKLY RRULEs without exactly one BYDAY entry.");
			}
			newDay = newDayList.get(0);
			if ( newDay==null ) {
				throw new IllegalArgumentException("Will not accept WEEKLY RRULEs without exactly one non-null BYDAY entry.");
			}
			break;
		case "MONTHLY":
			// Confirm exactly one BYDAY or BYMONTHDAY entry.
			if ( (newDayList!=null) && (newDayList.size()==1) ) {
				newDay = newDayList.get(0);
			}
			if ( (newMonthDayList!=null) && (newMonthDayList.size()==1) ) {
				newMonthDay = newMonthDayList.get(0);
			}
			if ( ((newDay==null) && (newMonthDay==null)) ||
			     ((newDay!=null) && (newMonthDay!=null)) 
			   ) {
				throw new IllegalArgumentException("Will not accept MONTHLY RRULEs without exactly one non-null BYDAY or BYMONTHDAY entry.");
			}
			break;
		}
		net.fortuna.ical4j.model.Date dateNow = new net.fortuna.ical4j.model.Date(java.util.Date.from(Instant.now()));
		Date newStartDate = newEvent.getStartIcalDate();
        Date newNextDate = newRecur.getNextDate(newStartDate, dateNow);

		
		// Find any events that match...
		for (CalendarEvent existingEvent : getEvents()) {
			// Ignore events with a different pickup name.
			if (! newEventName.equals(existingEvent.getName())) {
				continue;
			}
			log.debug("name match for: {}", existingEvent);
			
			// For all the existing RRULEs
			Date existingStartDate = existingEvent.getStartIcalDate();
			for (RRule existingRRule : existingEvent.getRRules()) {
				log.debug("check RRULE: {}", existingRRule);
				Recur existingRecur = existingRRule.getRecur();
			
				// Check FREQUENCY: Ignore event where RRULE frequency doesn't match.
				if (! newFrequency.equals(existingRecur.getFrequency())) {
					log.debug("no match: frequency={}", existingRecur.getFrequency());
					continue;
				}
			
				// Check INTERVAL: Ignore event if RRULE intervals do not match.
				int existingInterval = existingRecur.getInterval();
				if (existingInterval==0) { existingInterval=1; }					
				if (newInterval != existingInterval) {
					log.debug("no match: interval={}", existingInterval);
					continue;
				}
			
				WeekDayList existingDayList = existingRecur.getDayList();
				NumberList existingMonthDayList = existingRecur.getMonthDayList();

				switch (newFrequency) {
				case "WEEKLY":
					// Check BYDAY: new BYDAY must be in existing BYDAY
					if (existingDayList.contains(newDay)) {
						// Check INTERVAL match: The existing and new events start date makes them overlap
						Recur testRecur = new Recur("WEEKLY", 0);
						testRecur.setInterval(existingInterval);
						testRecur.getDayList().add(newDay);
				        Date existingNextDate = testRecur.getNextDate(existingStartDate, dateNow);
						if (newNextDate.equals(existingNextDate)) {
							// MATCH found
							log.debug("Matched to existing RRULE: {}", existingRRule);
							return true;
						}
					} else {
						log.debug("no match: newDay={} not in existing BYDAY={}", newDay, existingDayList);
					}
					break;
				case "MONTHLY":
					// Check BYMONTHDAY: new BYMONTHDAY must be in existing BYMONTHDAY
					if (newMonthDay!=null) {
						if (existingMonthDayList.contains(newMonthDay)) {
							// Check INTERVAL match: The existing and new events start date makes them overlap
							Recur testRecur = new Recur("MONTHLY", 0);
							testRecur.setInterval(existingInterval);
							testRecur.getMonthDayList().add(newMonthDay);
					        Date existingNextDate = testRecur.getNextDate(existingStartDate, dateNow);
							if (newNextDate.equals(existingNextDate)) {
								// MATCH found
								log.debug("Matched to existing RRULE: {}", existingRRule);
								return true;
							}
						} else {
							log.debug("no match: newMonthDay={} not in existing BYMONTHDAY={}", newMonthDay, existingMonthDayList);
						}
					}
					// Check BYDAY: new BYDAY must be in existing BYDAY
					if (newDay!=null) {
						if (existingDayList.contains(newDay)) {
							// Check INTERVAL match: The existing and new events start date makes them overlap
							Recur testRecur = new Recur("MONTHLY", 0);
							testRecur.setInterval(existingInterval);
							testRecur.getDayList().add(newDay);
					        Date existingNextDate = testRecur.getNextDate(existingStartDate, dateNow);
							if (newNextDate.equals(existingNextDate)) {
								// MATCH found
								log.debug("Matched to existing RRULE: {}", existingRRule);
								return true;
							}
						} else {
							log.debug("no match: newDay={} not in existing BYDAY={}", newDay, existingDayList);
						}
					}
					break;
				}			
			}
			
			// So no match on this event, go to next one.
		}

		log.debug("No RRULE match");
		return false;
	}
	
	/**
	 * Add a pickup time that repeats weekly.
	 * 
	 * @param pickupName of this pickup
	 * @param ldtEvent {@link java.time.LocalDateTime} of this event start
	 * @param dow {@link java.time.DayOfWeek} when this pickup time recurs.
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup recurs
	 * @return {@code true} if added successfully, {@code false} if a duplicate of this event already exists in the calendar.
	 */
	public Boolean pickupAddWeekly(LocalDateTime ldtEvent, String pickupName, java.time.DayOfWeek dow, java.time.LocalTime tod) {
		log.trace("pickupAddWeekly(pickupName={}, dow={}, tod={})", pickupName, dow, tod);
		CalendarEvent event = new CalendarEvent(pickupName, ldtEvent.withHour(tod.getHour()).withMinute(tod.getMinute()));
		event.addRecurrenceWeekly(dow, 1);
		return eventAdd(event);
	}
	
	/**
	 * Add a pickup time that repeats every other week.
	 * 
	 * @param pickupName of this pickup
	 * @param ldtEvent {@link java.time.LocalDateTime} of this event start
	 * @param nextWeek {@code false} to start this pickup on this coming dow. {@code true} to start this pickup on the<b>following</b> day this dow occurs.  For example, if today is Monday and dow=Friday, a {@code false} next week means this coming Friday and a {@code true} means the Friday <b>after</b> this coming Friday.
	 * @param dow {@link java.time.DayOfWeek} when this pickup time recurs.
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup time recurs
	 * @return {@code true} if added successfully, {@code false} if a duplicate of this event already exists in the calendar.
	 */
	public Boolean pickupAddBiWeekly(LocalDateTime ldtEvent, String pickupName, boolean nextWeek, java.time.DayOfWeek dow, java.time.LocalTime tod) {
		log.trace("pickupAddBiWeekly(pickupName={}, nextWeek={}, dow={}, tod={})", pickupName, nextWeek, dow, tod);
		if (nextWeek) {
			ldtEvent = ldtEvent.plusWeeks(1);
		}
		CalendarEvent event = new CalendarEvent(pickupName, ldtEvent.withHour(tod.getHour()).withMinute(tod.getMinute()));
		event.addRecurrenceWeekly(dow, 2);
		return eventAdd(event);
	}
	
	/**
	 * Add a pickup time that repeats on a given day of month.
	 * 
	 * @param pickupName of this pickup
	 * @param ldtRequest {@link java.time.LocalDateTime} the starting date of this pickup.  The actual calendar event will be same or later than this given date/time.
	 * @param dom day-of-month that this monthly pickup will recur.  May be [1,31] or [-31,-1].
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup time recurs
	 * @return {@code true} if added successfully, {@code false} if a duplicate of this event already exists in the calendar.
	 */
	public Boolean pickupAddDayOfMonth(java.time.LocalDateTime ldtRequest, String pickupName, Integer dom, java.time.LocalTime tod) {
		log.trace("pickupAddDayOfMonth(ldtRequest={}, pickupName={}, dom={}, tod={})", ldtRequest, pickupName, dom, tod);
		java.time.LocalDateTime ldtEvent = DateTimeUtils.getNextOrSameDayOfMonth(ldtRequest, dom, tod);
		CalendarEvent event = new CalendarEvent(pickupName, ldtEvent.withHour(tod.getHour()).withMinute(tod.getMinute()));
		event.addRecurrenceDayOfMonth(dom, 1);
		return eventAdd(event);
	}
	
	/**
	 * Add a pickup time that repeats on a given weekday in the month.
	 * 
	 * @param pickupName of this pickup
	 * @param ldtRequest {@link java.time.LocalDateTime} the starting date of this pickup.  The actual calendar event will be same or later than this given date/time.
	 * @param dow day-of-week that this pickup will recur.
	 * @param weekNum Recur every Nth dow in the month.  Allowed values [1,5] and [-5,-1]
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup time recurs
	 * @return {@code true} if added successfully, {@code false} if a duplicate of this event already exists in the calendar.
	 */
	public Boolean pickupAddWeekdayOfMonth(java.time.LocalDateTime ldtRequest, String pickupName, Integer weekNum, DayOfWeek dow, java.time.LocalTime tod) {
		log.trace("pickupAddWeekdayOfMonth(ldtRequest={}, pickupName={}, dow={}, weekNum={}, tod={})", ldtRequest, pickupName, dow, weekNum, tod);
		java.time.LocalDateTime ldtEvent = DateTimeUtils.getNextOrSameWeekdayOfMonth(ldtRequest, dow, weekNum, tod);
		CalendarEvent event = new CalendarEvent(pickupName, ldtEvent.withHour(tod.getHour()).withMinute(tod.getMinute()));
		event.addRecurrenceWeekdayOfMonth(dow, weekNum, 1);
		return eventAdd(event);
	}
	
	/**
	 * Delete all entries from the schedule for the given pickupName.
	 * 
	 * @param pickupName String Name for this pickup (eg. Trash, Recycling)
	 * @return True if this pickup already existed and had to be removed.
	 */
	public Boolean pickupDelete(String pickupName) {
		log.trace("pickupDelete({})",pickupName);
		
		// Delete all event components...
		boolean eventsRemoved=false;
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (int indexComponents=components.size()-1; indexComponents>=0; indexComponents--) {
			CalendarComponent component = components.get(indexComponents);
        	if (Component.VEVENT.equals(component.getName())) {
        		// *If* the event name matches pickupName
        		VEvent event = (VEvent) component;
        		String eventName = event.getSummary().getValue().trim().toLowerCase();
        		if (pickupName.equals(eventName)) {
	        		components.remove(indexComponents);
	        		eventsRemoved=true;
        		}
        	}
		}
		return eventsRemoved;
	}
		
	/**
	 * Delete a pickup time that repeats weekly.
	 * 
	 * @param pickupName of this pickup
	 * @param dow {@link java.time.DayOfWeek} when this pickup time recurs.
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup recurs
	 * @param interval number of weeks between each recurrence.  1=every week, 2=every other week, etc.
	 * @return Number of recurrences removed
	 */
	public int pickupDeleteWeekly(String pickupName, java.time.DayOfWeek dow, java.time.LocalTime tod, Integer interval) {
		log.trace("pickupDeleteWeekly(pickupName={}, dow={}, tod={}, interval={}", pickupName, dow, tod, interval);
		// Count how many removals get performed.
		int removeCount = 0;
		
		// Check all calendar components...
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (int indexComponents=components.size()-1; indexComponents>=0; indexComponents--) {
			
    		// Ignore components that are not events.
        	CalendarComponent component = components.get(indexComponents);
        	if (! Component.VEVENT.equals(component.getName())) {
        		continue;
        	}
        	CalendarEvent event = new CalendarEvent( (VEvent) component );
        	
        	// Ignore any events that don't match the pickupName or TimeOfDay.
        	if (! event.matchesNameTod(pickupName, tod)) {
        		continue;
        	}
        	
        	// Delete any matching recurrence(s).
        	if (event.deleteRecurrenceWeekly(dow, interval) < 1) {
            	// Ignore any events that didn't have a weekly recurrence match.
        		continue;
        	}
        	
        	// OK, this event matches the caller's delete request and 
        	// we've already deleted some recurrence matches (BYDAY) and possibly RRULES.
        	removeCount++;
        	if (! event.hasRrules()) {
        		// There are no more RRULES on the event 
        		// => delete this entire event (component)
        		components.remove(indexComponents);
        	}
        }
        
		return removeCount;
	}
	

	/**
	 * Delete a pickup time that repeats every week.
	 * 
	 * @param pickupName of this pickup
	 * @param dow {@link java.time.DayOfWeek} when this pickup recurs.
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup recurs
	 * @return Number of pickup times removed
	 */
	public int pickupDeleteWeekly(String pickupName, java.time.DayOfWeek dow, java.time.LocalTime tod) {
		log.trace("pickupDeleteWeekly(pickupName={}, dow={}, tod={})", pickupName, dow, tod);
		return pickupDeleteWeekly(pickupName, dow, tod, 1);
	}
	
	/**
	 * Delete a pickup time that repeats every other week.
	 * 
	 * @param pickupName of this pickup
	 * @param dow {@link java.time.DayOfWeek} when this pickup recurs.
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup recurs
	 * @return Number of pickup times removed
	 */
	public int pickupDeleteBiWeekly(String pickupName, java.time.DayOfWeek dow, java.time.LocalTime tod) {
		log.trace("pickupDeleteBiWeekly(pickupName={}, dow={}, tod={})", pickupName, dow, tod);
		return pickupDeleteWeekly(pickupName, dow, tod, 2);
	}
	
	/**
	 * Delete a pickup time that repeats on the given day of the month.
	 * 
	 * @param pickupName of this pickup
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup time recurs
	 * @param dom {@link java.lang.Integer} day-of-month when this pickup time recurs. Must be in [1,31] or [-31,-1] ranges.
	 * @return Number of pickup recurrences removed
	 */
	public int pickupDeleteDayOfMonth(String pickupName, Integer dom, java.time.LocalTime tod) {
		log.debug("pickupDeleteDayOfMonth(pickupName={}, dom={}, tod={}", pickupName, dom, tod);
		// Count how many removals get performed.
		int removeCount = 0;
		
		// Check all calendar components...
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (int indexComponents=components.size()-1; indexComponents>=0; indexComponents--) {
			
    		// Ignore components that are not events.
        	CalendarComponent component = components.get(indexComponents);
        	if (! Component.VEVENT.equals(component.getName())) {
        		continue;
        	}
        	CalendarEvent event = new CalendarEvent( (VEvent) component );
        	
        	// Ignore any events that don't match the pickupName or TimeOfDay.
        	if (! event.matchesNameTod(pickupName, tod)) {
        		continue;
        	}
        	
        	// Delete any matching recurrence(s).
        	if (event.deleteRecurrenceDayOfMonth(dom, 1) < 1) {
            	// Ignore any events that didn't have a matching recurrence set.
        		continue;
        	}
        	
        	// OK, this event matches the caller's delete request and 
        	// we've already deleted some recurrence matches (BYMONTHDAY) and possibly RRULES.
        	removeCount++;
        	if (! event.hasRrules()) {
        		// There are no more RRULES on the event 
        		// => delete this entire event (component)
        		components.remove(indexComponents);
        	}
        }
        
		return removeCount;
	}
	
	/**
	 * Delete a pickup time that repeats on the given weekday of the month.
	 * 
	 * @param pickupName of this pickup
	 * @param weekNum Recur every Nth dow in the month.  Allowed values [1,5] and [-5,-1]
	 * @param dow day-of-week that this pickup recurs.
	 * @param tod {@link java.time.LocalTime} time-of-day when this pickup recurs
	 * @return Number of pickup times removed
	 */
	public int pickupDeleteWeekdayOfMonth(String pickupName, Integer weekNum, DayOfWeek dow, java.time.LocalTime tod) {
		log.trace("pickupDeleteWeekdayOfMonth(pickupName={}, weekNum={}, dow={}, tod={})", pickupName, weekNum, dow, tod);
		
		// Count how many removals get performed.
		int removeCount = 0;
		
		// Check all calendar components...
		ComponentList<CalendarComponent> components = cal.getComponents();
		for (int indexComponents=components.size()-1; indexComponents>=0; indexComponents--) {
			
    		// Ignore components that are not events.
        	CalendarComponent component = components.get(indexComponents);
        	if (! Component.VEVENT.equals(component.getName())) {
        		continue;
        	}
        	CalendarEvent event = new CalendarEvent( (VEvent) component );
        	
        	// Ignore any events that don't match the pickupName or TimeOfDay.
        	if (! event.matchesNameTod(pickupName, tod)) {
        		continue;
        	}
        	
        	// Delete any matching recurrence(s).
        	if (event.deleteRecurrenceWeekdayOfMonth(dow, weekNum, 1) < 1) {
            	// Ignore any events that didn't have a matching recurrence set.
        		continue;
        	}
        	
        	// OK, this event matches the caller's delete request and 
        	// we've already deleted some recurrence matches (BYDAY) and possibly RRULES.
        	removeCount++;
        	if (! event.hasRrules()) {
        		// There are no more RRULES on the event 
        		// => delete this entire event (component)
        		components.remove(indexComponents);
        	}
        }
        
		return removeCount;
	}
	
	/**
	 * Find the next time this pickup occurs after the given {@link java.time.LocalDateTime}.
	 * 
	 * @param ldtStartingPoint Find next pickup after this date/time.
	 * @param pickupName This pickup's name
	 * @return {@link java.time.LocalDateTime} of the next occurrence of this pickup.
	 */
	public java.time.LocalDateTime pickupGetNextOccurrence(java.time.LocalDateTime ldtStartingPoint, String pickupName) {
		log.trace("pickupGetNextOccurrence(pickupName={}, ldtStartingPoint={})", pickupName, ldtStartingPoint);

		// For all events in the pickup calendar...
		java.time.LocalDateTime ldtEarliestOccurrence = null;
		for (CalendarEvent event: getEvents()) {
			String eventName = event.getName();
			if (! pickupName.trim().toLowerCase().equals(eventName)) {
				continue;
			}
			java.time.LocalDateTime ldtOccurrence = event.getNextOccurrence(ldtStartingPoint);
			if (ldtOccurrence == null) { continue; }
			
        	if ((ldtEarliestOccurrence==null) || (ldtOccurrence.isBefore(ldtEarliestOccurrence))) {
        		ldtEarliestOccurrence=ldtOccurrence;	            		
        	}
		}
		return ldtEarliestOccurrence;
	}
	
	/**
	 * Find the next pickup occurrence for every pickup in the {@link trashday.model.Calendar}.
	 * 
	 * @param ldtStartingPoint Find next pickup after this date/time.
	 * @return {@link java.util.Map} with the {@link java.time.LocalDateTime} of the next occurrence of all pickups in the {@link trashday.model.Calendar}.
	 */
	public Map<String,java.time.LocalDateTime> pickupGetNextOccurrences(java.time.LocalDateTime ldtStartingPoint) {
		log.debug("pickupGetNextOccurrences(ldtStartingPoint={})", ldtStartingPoint);
		Map<String,java.time.LocalDateTime> nextPickupTimes = new HashMap<String,java.time.LocalDateTime>();

		// For all events in the pickup calendar...
		for (CalendarEvent event: getEvents()) {
			String eventName = event.getName();
			java.time.LocalDateTime ldtOccurrence = event.getNextOccurrence(ldtStartingPoint);
			if (ldtOccurrence == null) { continue; }
			
        	if (! nextPickupTimes.containsKey(eventName)) {
        		nextPickupTimes.put(eventName, ldtOccurrence);
        	}
        	else if (ldtOccurrence.isBefore(nextPickupTimes.get(eventName))) {
        		nextPickupTimes.put(eventName, ldtOccurrence);	            		
        	}
		}

		return nextPickupTimes;
	}
	
	/**
	 * Generate a {@link java.lang.String} form of the {@link trashday.model.Calendar}
	 * in a form suitable for printing.  Requires a {@link java.time.LocalDateTime}
	 * base so that biweekly pickups may be printed as "this" or "next" week.
	 * 
	 * @param ldtBase {@link java.time.LocalDateTime} used to print biweekly events in
	 * 			a relative form as "starting this week" or "starting next week"
	 * @return {@link java.lang.String} form of the {@link trashday.model.Calendar}
	 */
	@Deprecated
	@CoberturaIgnore
	public String toStringPrintable(java.time.LocalDateTime ldtBase) {
		List<String> pickupNames = new ArrayList<String>();
		Map<String, Map<String, String>> pickups = new HashMap<String, Map<String, String>>();
		for (CalendarEvent event : getEvents() ) {
			String pickupName = event.getName();
			
			Map<String, String> pickupMap;
			try {
				pickupMap = event.getRecurrencesPrintable(ldtBase);
			} catch (Exception e) {
				log.error("Calendar should NOT have any non-recurring events! ex={}", e);
				continue;
			}
			if (pickupMap==null) {
				log.error("Calendar should NOT have any non-recurring events! pickupMap={}", pickupMap);
				continue;
			}
			if (! pickupNames.contains(pickupName)) {
				pickupNames.add(pickupName);
			}
			if (pickups.containsKey(pickupName)) {
				Map<String, String> existingPickupMap = pickups.get(pickupName);
				for (String score: pickupMap.keySet()) {
					existingPickupMap.put(score, pickupMap.get(score));
				}
			} else {
				pickups.put(pickupName, pickupMap);
			}
		}
		
        StringBuilder sb = new StringBuilder();
        for (String pickupName: pickupNames) {
        	Map<String, String> recurrenceMap = pickups.get(pickupName);
            List<String> recurrenceStrings = new ArrayList<String>();
            for (Map.Entry<String, String> entry : recurrenceMap.entrySet()) {
            	recurrenceStrings.add(entry.getValue());
            }
    		
	        sb.append("Pickup ");
	        sb.append(pickupName);
	        sb.append(" ");
	        sb.append(FormatUtils.formattedJoin(recurrenceStrings, null, null));
	        sb.append(".\n");
        }
        return sb.toString();
	}
	
	/**
	 * Generate a {@link java.lang.String} form of the {@link trashday.model.Calendar}
	 * that is suitable for serialization.  We use the RFC 5545 format.
	 * 
	 * @return RFC 5545 form of the {@link trashday.model.Calendar}
	 * @see		<a href="https://tools.ietf.org/html/rfc5545">RFC 5545: Internet Calendaring and Scheduling Core Object Specification (iCalendar)</a>
	 */
	public String toStringRFC5545() {
		if (isEmpty()) {
			return null;
		}
		
		// Convert non-empty calendars to a String to store.
		StringWriter sw = new StringWriter();
		CalendarOutputter outputter = new CalendarOutputter();
		try {
			outputter.output(cal, sw);
		} catch (IOException ex) {
			throw new IllegalStateException("Unable to convert calendar value to storage string. "+ex.getMessage());
		} catch (ValidationException ex) {
			throw new IllegalStateException("Unable to convert calendar value to storage string. "+ex.getMessage());
		}
		return sw.toString();
	}

	/**
	 * Generate a {@link java.lang.String} form of the {@link trashday.model.Calendar}
	 * in a form suitable for Alexa to speak.  Requires a {@link java.time.LocalDateTime}
	 * base so that biweekly pickups may be spoken as "this" or "next" week.
	 * 
	 * @param ldtBase {@link java.time.LocalDateTime} used to speak biweekly events in
	 * 			a relative form as "starting this week" or "starting next week"
	 * @return {@link java.lang.String} form of the {@link trashday.model.Calendar}
	 */
	@Deprecated
	@CoberturaIgnore
	public String toStringVerbal(java.time.LocalDateTime ldtBase) {
		List<String> pickupNames = new ArrayList<String>();
		Map<String, Map<String, String>> pickups = new HashMap<String, Map<String, String>>();
		for (CalendarEvent event : getEvents() ) {
			String pickupName = event.getName();
			
			Map<String, String> pickupMap;
			try {
				pickupMap = event.getRecurrencesVerbal(ldtBase);
			} catch (Exception e) {
				log.error("Calendar should NOT have any non-recurring events! ex={}", e);
				continue;
			}
			if (pickupMap==null) {
				log.error("Calendar should NOT have any non-recurring events! pickupMap={}", pickupMap);
				continue;
			}
			if (! pickupNames.contains(pickupName)) {
				pickupNames.add(pickupName);
			}
			if (pickups.containsKey(pickupName)) {
				Map<String, String> existingPickupMap = pickups.get(pickupName);
				for (String score: pickupMap.keySet()) {
					existingPickupMap.put(score, pickupMap.get(score));
				}
			} else {
				pickups.put(pickupName, pickupMap);
			}
		}
		
        StringBuilder sb = new StringBuilder();
        for (String pickupName: pickupNames) {
        	Map<String, String> recurrenceMap = pickups.get(pickupName);
            List<String> recurrenceStrings = new ArrayList<String>();
            for (Map.Entry<String, String> entry : recurrenceMap.entrySet()) {
            	String recurrence = entry.getValue();
            	recurrenceStrings.add(recurrence);
            }
    		
	        sb.append("Pickup ");
	        sb.append(pickupName);
	        sb.append(" ");
	        sb.append(FormatUtils.formattedJoin(recurrenceStrings, null, null));
	        sb.append(". ");
        }
        return sb.toString();
	}

}
