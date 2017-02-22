package trashday.ui;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fortuna.ical4j.model.Recur;
import net.fortuna.ical4j.model.WeekDay;
import net.fortuna.ical4j.model.WeekDayList;
import trashday.CoberturaIgnore;
import trashday.model.Calendar;
import trashday.model.CalendarEvent;
import trashday.model.DateTimeUtils;
import trashday.model.NextPickups;

/**
 * Utility functions to help provide output in a format to be spoken by Alexa
 * or printed on Alexa cards.
 * 
 * @author J. Todd Baldwin
 */
public final class FormatUtils {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(FormatUtils.class);
    
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
	
	/** Map of day-of-month numbers to spoken form. */
	public static Map<Integer, String> domMap;
	/** Map of week-of-month numbers to spoken form. */
	public static Map<Integer, String> womMap;

	static {
		womMap = new HashMap<Integer, String>();
		womMap.put(1, "first");
		womMap.put(2, "second");
		womMap.put(3, "third");
		womMap.put(4, "fourth");
		womMap.put(5, "fifth");
		womMap.put(-1, "last");
		womMap.put(-2, "second-to-last");
		womMap.put(-3, "third-to-last");
		womMap.put(-4, "fourth-to-last");
		womMap.put(-5, "fifth-to-last");
		
		domMap = new HashMap<Integer, String>();
		domMap.put(1, "first");
		domMap.put(2, "second");
		domMap.put(3, "third");
		domMap.put(4, "fourth");
		domMap.put(5, "fifth");
		domMap.put(6, "sixth");
		domMap.put(7, "seventh");
		domMap.put(8, "eighth");
		domMap.put(9, "ninth");
		domMap.put(10, "tenth");
		domMap.put(11, "eleventh");
		domMap.put(12, "twelfth");
		domMap.put(13, "thirteenth");
		domMap.put(14, "fourteenth");
		domMap.put(15, "fifteenth");
		domMap.put(16, "sixteenth");
		domMap.put(17, "seventeenth");
		domMap.put(18, "eighteenth");
		domMap.put(19, "ninteenth");
		domMap.put(20, "twentieth");
		domMap.put(21, "twenty-first");
		domMap.put(22, "twenty-second");
		domMap.put(23, "twenty-third");
		domMap.put(24, "twenty-fourth");
		domMap.put(25, "twenty-fifth");
		domMap.put(26, "twenty-sixth");
		domMap.put(27, "twenty-seventh");
		domMap.put(28, "twenty-eighth");
		domMap.put(29, "twenty-ninth");
		domMap.put(30, "thirtieth");
		domMap.put(31, "thirty-first");
		// "Added monthly trash pickup on the " + DateTimeOutputUtils.verbalDayOfMonth(dom) + " at "+ DateTimeOutputUtils.verbalTime(tod) + ".";
		domMap.put(-1, "last day of the month");
		domMap.put(-2, "second day before the end of the month");
		domMap.put(-3, "third day before the end of the month");
		domMap.put(-4, "fourth day before the end of the month");
		domMap.put(-5, "fifth day before the end of the month");
		domMap.put(-6, "sixth day before the end of the month");
		domMap.put(-7, "seventh day before the end of the month");
		domMap.put(-8, "eighth day before the end of the month");
		domMap.put(-9, "ninth day before the end of the month");
		domMap.put(-10, "tenth day before the end of the month");
		domMap.put(-11, "eleventh day before the end of the month");
		domMap.put(-12, "twelfth day before the end of the month");
		domMap.put(-13, "thirteenth day before the end of the month");
		domMap.put(-14, "fourteenth day before the end of the month");
		domMap.put(-15, "fifteenth day before the end of the month");
		domMap.put(-16, "sixteenth day before the end of the month");
		domMap.put(-17, "seventeenth day before the end of the month");
		domMap.put(-18, "eighteenth day before the end of the month");
		domMap.put(-19, "ninteenth day before the end of the month");
		domMap.put(-20, "twentieth day before the end of the month");
		domMap.put(-21, "twenty-first day before the end of the month");
		domMap.put(-22, "twenty-second day before the end of the month");
		domMap.put(-23, "twenty-third day before the end of the month");
		domMap.put(-24, "twenty-fourth day before the end of the month");
		domMap.put(-25, "twenty-fifth day before the end of the month");
		domMap.put(-26, "twenty-sixth day before the end of the month");
		domMap.put(-27, "twenty-seventh day before the end of the month");
		domMap.put(-28, "twenty-eighth day before the end of the month");
		domMap.put(-29, "twenty-ninth day before the end of the month");
		domMap.put(-30, "thirtieth day before the end of the month");
		domMap.put(-31, "thirty-first day before the end of the month");
	};
	
	/**
	 * Private constructor given to this utility class.  Prevents instantiation since
	 * this class is only meant to provide public, static utility methods.
	 */
	@CoberturaIgnore
	private FormatUtils () {
    }
	
	/**
	 * Provide a text version of a {@link LocalDateTime} that is relative to a
	 * second {@link LocalDateTime}.
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
	 * Provide a text version of a {@link LocalDate} that is relative to a
	 * second {@link LocalDate}.
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
	 * When speaking a list of items, we need to add comma(s) and the "and" word to make the
	 * spoken list sound correct.  For example, a three-item list is spoken like: "one, two, and three"
	 * 
	 * @param items to be spoken
	 * @param prefix optional prefix before listing the items
	 * @param suffix optional suffix for after the items.
	 * @return {@link java.lang.String} suitable to be spoken by Alexa.
	 */
	public static String formattedJoin(List<String> items, String prefix, String suffix) {
		log.trace("verbalJoin(items={}, prefix={}, suffix={}", items, prefix, suffix);
		StringBuffer sb = new StringBuffer();
		if (prefix!=null) {
			sb.append(prefix);
		}
		switch (items.size()) {
		case 0:
			break;
		case 1:
			sb.append(items.get(0));
			break;
		case 2:
			sb.append(items.get(0));
			sb.append(" and ");
			sb.append(items.get(1));
			break;
		default:
			int limit = items.size() - 1;
			for (int i=0; (i < limit); i++) {
				sb.append(items.get(i));
				sb.append(", ");
			}
			sb.append("and ");
			sb.append(items.get(limit));
		}
		if (suffix!=null) {
			sb.append(suffix);
		}
		return sb.toString();
	}
	
	/**
	 * Create a {@link java.lang.String} representation of a Calendar's recurring events that
	 * is suitable for printing on an Alexa card.
	 * 
	 * @param calendar {@link trashday.model.Calendar} to show
	 * @param ldtBase Show the calendar with days relative to this {@link java.time.LocalDateTime}.  For example,
	 * 				if the ldtBase is Thursday, then a recurrence on Friday might be shown as "tomorrow".
	 * @return text suitable to be printed on an Alexa Card
	 */
	public static String printableCalendar(Calendar calendar, LocalDateTime ldtBase) {
		log.trace("printableCalendar(ldtBase={})",ldtBase);
		Map<String, Map<String, String>> pickups = new LinkedHashMap<String, Map<String, String>>();
		
		for (CalendarEvent event : calendar.getEvents() ) {
			String pickupName = event.getName();
    		java.time.LocalDateTime ldtEventStart = event.getStartLocalDateTime();
			
			Map<String, String> eventOrderedRecurrences = new TreeMap<String, String>();
			for (Recur recur: event.getRecurrences()) {
				StringBuilder recurrenceString = new StringBuilder();
				String recurrenceOrder;
	        	WeekDayList dayList = recur.getDayList();
				
		        switch (recur.getFrequency()) {
		        case "WEEKLY":
	        		
		        	switch (recur.getInterval()) {
		        	case -1:
		        	case 0: // Weekly recurrence
		        	case 1:
		        		for (WeekDay wd : dayList) {
			        		recurrenceString.append("every ");
			        		recurrenceString.append(FormatUtils.printableDayOfWeek(wd));
			        		recurrenceString.append(" at ");
			        		recurrenceString.append(FormatUtils.printableTime(ldtEventStart));
				        	recurrenceOrder = String.format("A-%03d%d%05d", 1, DateTimeUtils.getDayOfWeek(wd).getValue(), DateTimeUtils.getMinuteOfDay(ldtEventStart));
					        log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
					        eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        		}
			        	break;
	        		
		        	case 2: // Bi-weekly recurrence		        		
		        		for (WeekDay wd : dayList) {
			        		recurrenceString.append("every other ");
			        		recurrenceString.append(FormatUtils.printableDayOfWeek(wd));
			        		recurrenceString.append(" at ");
			        		recurrenceString.append(FormatUtils.printableTime(ldtEventStart));
			        		java.time.LocalDateTime ldtBiWeeklyOccurrence = event.getNextOccurrence(ldtBase);
			        		if (ldtBiWeeklyOccurrence!=null) {
			        			long days = ChronoUnit.DAYS.between(ldtBase, ldtBiWeeklyOccurrence);	        			
			        			if (days < 7) {
			        				recurrenceString.append(" (on this ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtBiWeeklyOccurrence, ldtBase));
			        				recurrenceString.append(")");
			        			}
			        			else {
			        				recurrenceString.append(" (on next ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtBiWeeklyOccurrence, ldtBase));
			        				recurrenceString.append(")");	        				
			        			}
			        		}
			        		
			        		recurrenceOrder = String.format("A-%03d%d%05d", 2, ldtEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtEventStart));
					        log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
					        eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        		}
		        		break;
	        		
		        	default:// Multi-week recurrence
		        		for (WeekDay wd : dayList) {
			        		recurrenceString.append("every ");
			        		recurrenceString.append(recur.getInterval());
			        		recurrenceString.append(" weeks on ");
			        		recurrenceString.append(FormatUtils.printableDayOfWeek(wd));
			        		recurrenceString.append(" at ");
			        		recurrenceString.append(FormatUtils.printableTime(ldtEventStart));
			        		
			        		java.time.LocalDateTime ldtMultiWeekOccurrence = event.getNextOccurrence(ldtBase);
			        		if (ldtMultiWeekOccurrence!=null) {
			        			long days = ChronoUnit.DAYS.between(ldtBase, ldtMultiWeekOccurrence);
			        			if (days < 7) {
			        				recurrenceString.append(" (on this ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtMultiWeekOccurrence, ldtBase));
			        				recurrenceString.append(")");
			        			}
			        			else {
			        				recurrenceString.append(" (on next ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtMultiWeekOccurrence, ldtBase));
			        				recurrenceString.append(")");	        				
			        			}
			        		}
		
			        		recurrenceOrder = String.format("A-%03d%d%05d", recur.getInterval(), ldtEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtEventStart));
					        log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
					        eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        		}
		        		break;
		        	} // End of interval switch
		        	break;  // End of Weekly case
	        	
		        case "MONTHLY":
		        	for(Number dayNum : recur.getMonthDayList()) {
		        		int n = dayNum.intValue();  // n = [-31,-1] and [1,31]
		        		
		        		recurrenceString.append("on the ");
		        		recurrenceString.append(FormatUtils.printableDayOfMonth(n));
		        		recurrenceString.append(" at ");
		        		recurrenceString.append(FormatUtils.printableTime(ldtEventStart));
		        		
		        		if (n>0) {
		        			recurrenceOrder = String.format("B-1%02d", n);
		        		} else {
		        			recurrenceOrder = String.format("B-3%02d", 70+n);
		        		}
			        	log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
			        	eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        	}
	        		for (WeekDay wd : dayList) {
	        			int offset = wd.getOffset();
		        		if (offset!=0) {
			        		recurrenceString.append("on the ");
			        		recurrenceString.append(printableWeekOfMonth(wd.getOffset()));
			        		recurrenceString.append(" ");
		        		} else {
			        		recurrenceString.append("on ");
		        		}
		        		recurrenceString.append(FormatUtils.printableDayOfWeek(wd));
		        		recurrenceString.append(" at ");
		        		recurrenceString.append(FormatUtils.printableTime(ldtEventStart));
		        		
		        		if (offset>0) {
		        			recurrenceOrder = String.format("B-2%02d", offset);
		        		} else {
		        			recurrenceOrder = String.format("B-2%02d", 70+offset);
		        		}
			        	log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
			        	eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
	        		}
	        		break; // End of Monthly case
	        		
		    	default:
		        	throw new IllegalArgumentException("Unknown Recurrence rule frequency: "+recur.getFrequency());
		        	
		        } // End of frequency switch
			} // End of recurrences
			
			// Append this event's ordered recurrences to the ones in pickups map.
			if (pickups.containsKey(pickupName)) {
				Map<String, String> existingPickupMap = pickups.get(pickupName);
				for (String score: eventOrderedRecurrences.keySet()) {
					existingPickupMap.put(score, eventOrderedRecurrences.get(score));
				}
			} else {
				pickups.put(pickupName, eventOrderedRecurrences);
			}
		}
		
		// Print the pickups.
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> pickupEntry: pickups.entrySet()) {
        	String pickupName = pickupEntry.getKey();
        	Map<String, String> recurrenceMap = pickupEntry.getValue();
            List<String> recurrenceStrings = new ArrayList<String>();
            for (Map.Entry<String, String> recurrenceEntry : recurrenceMap.entrySet()) {
            	recurrenceStrings.add(recurrenceEntry.getValue());
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
	 * Provide a text version of the date and time suitable to be
	 * printed on an Alexa Card response.
	 * 
	 * @param ldt {@link java.time.LocalDateTime} to be printed
	 * @return text suitable to be printed on an Alexa Card
	 */
	public static String printableDateAndTime(LocalDateTime ldt) {
		log.trace("printableDateAndTime(ldt={})",ldt);
		return ldt.format(formatterDate) + " at " + printableTime(ldt);
	}
	
	/**
	 * Provide a text version of the date and time suitable to be
	 * printed on an Alexa Card response.
	 * 
	 * @param zdt {@link java.time.ZonedDateTime} to be printed
	 * @return text suitable to be printed on an Alexa Card
	 */
	public static String printableDateAndTime(ZonedDateTime zdt) {
		log.trace("printableDateAndTime(zdt={})",zdt);
		return zdt.format(formatterDate) + " at " + printableTime(zdt);
	}
		
	/**
	 * Provide a text version of the date and time that occurred, printable relative to
	 * a given date and time.  For example, an occurrence that occurs on "Thursday,
	 * 2016-11-24 at 7 am" would be spoken as "Tomorrow at 7 am" if it was relative to
	 * a base date of "Wednesday, 2016-11-23".
	 * 
	 * @param ldtOccurrence {@link java.time.LocalDateTime} to be printed
	 * @param ldtBase {@link java.time.LocalDateTime} print day of week relative to the
	 * 		day the in ldtBase.
	 * @return text suitable to be printed on an Alexa Card
	 */
	public static String printableDateAndTimeRelative(LocalDateTime ldtOccurrence, LocalDateTime ldtBase) {
		log.trace("printableDateAndTimeRelative({},{})",ldtOccurrence,ldtBase);
		return dateRelative(ldtOccurrence,ldtBase) + " at " + printableTime(ldtOccurrence);
	}
	
	/**
	 * Printable day-of-month.  For example, "third", "last day of the month", "second day before the end of the month".
	 * 
	 * @param dom day-of-month to print.  May be [1,31] or [-31,-1].
	 * @return {@link java.lang.String} printable representation
	 */
	public static String printableDayOfMonth(Integer dom) {
		log.trace("printableDayOfMonth(dom={})", dom);
		if (dom>31) {
			throw new IllegalArgumentException("Maximum day of month value (31) exceeded: "+dom);
		}
		if (dom<-31) {
			throw new IllegalArgumentException("Minimum day of month value (-31) exceeded: "+dom);
		}
		if (dom==0) {
			throw new IllegalArgumentException("No such day of month: "+dom);
		}
		return domMap.get(dom);
	}
	
	/**
	 * Printable day-of-week, properly capitalized.
	 * 
	 * @param dow {@link java.time.DayOfWeek} source
	 * @return {@link java.lang.String} printable representation
	 */
	public static String printableDayOfWeek(DayOfWeek dow) {
		log.trace("printableDayOfWeek(dow={})", dow);
		return dow.getDisplayName(TextStyle.FULL, Locale.getDefault());
	}
	
	public static String printableDayOfWeek(WeekDay wd) {
		log.trace("printableDayOfWeek(wd={})", wd);
		switch (wd.getDay()) {
		case SU:
			return "Sunday";
		case MO:
			return "Monday";
		case TU:
			return "Tuesday";
		case WE:
			return "Wednesday";
		case TH:
			return "Thursday";
		case FR:
			return "Friday";
		case SA:
			return "Saturday";
		default:
			return null;
		}
	}

	/**
	 * Provide a text version of the day of week and time.  For example, an occurrence 
	 * that occurs on "Thursday, 2016-11-24 at 7 am" would be spoken as 
	 * "Thursday at 7 am".
	 * 
	 * @param dow {@link java.time.DayOfWeek} to be printed
	 * @param tod {@link java.time.LocalTime} to be printed
	 * @return {@link java.lang.String} printable representation
	 */
	public static String printableDayOfWeekAndTime(DayOfWeek dow, LocalTime tod) {
		log.trace("printableDayOfWeekAndTime(dow={}, tod={})", dow, tod);
		return dow.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " at " + printableTime(tod);
	}
	
	/**
	 * Provide a text version of the day of week and time.  For example, an occurrence 
	 * that occurs on "Thursday, 2016-11-24 at 7 am" would be spoken as 
	 * "Thursday at 7 am".
	 * 
	 * @param ldt {@link java.time.LocalDateTime} to be printed
	 * @return {@link java.lang.String} printable representation
	 */
	public static String printableDayOfWeekAndTime(LocalDateTime ldt) {
		log.trace("printableDayOfWeekAndTime(ldt={})", ldt);
		DayOfWeek dow = ldt.getDayOfWeek();
		LocalTime tod = ldt.toLocalTime();
		return printableDayOfWeekAndTime(dow, tod);
	}
	
	/**
	 * Create a {@link java.lang.String} representation of a {@link trashday.model.NextPickups} that
	 * is suitable for printing on an Alexa card.
	 * 
	 * @param nextPickups {@link trashday.model.NextPickups} to be spoken verbally
	 * @return text suitable to be printed on an Alexa Card
	 */
	public static String printableNextPickups(NextPickups nextPickups) {
		log.trace("printableNextPickups()");
		LocalDateTime ldtStartingPoint = nextPickups.getStartingPoint();
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<String,LocalDateTime> entry : nextPickups.getPickups().entrySet()) {
			String pickupName = entry.getKey();
			LocalDateTime ldtNextPickup = entry.getValue();
			sb.append("Next " + pickupName + " pickup is " + FormatUtils.printableDateAndTimeRelative(ldtNextPickup, ldtStartingPoint) + ".\n");
		}
		return sb.toString();
	}
	
	/**
	 * Provide a printable version of the given {@link java.time.LocalTime}
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
	
	/**
	 * Provide a printable version of the given {@link ZonedDateTime}
	 * that is good for printing of time succinctly on Cards.
	 * 
	 * @param zdt {@link ZonedDateTime} to be printed
	 * @return text suitable for printing
	 */
	public static String printableTime(ZonedDateTime zdt) {	
		log.trace("printableTime({})", zdt);
		LocalTime lt = LocalTime.of(zdt.getHour(), zdt.getMinute());
		return printableTime(lt);
	}
	
	/**
	 * Printable week-of-month.  For example, "third", "last", "second to last".
	 * 
	 * @param wom week-of-month to print.  May be [1,5] or [-5,-1].
	 * @return {@link java.lang.String} printable representation
	 */
	public static String printableWeekOfMonth(Integer wom) {
		log.trace("printableWeekOfMonth(dom={})", wom);
		if (wom>5) {
			throw new IllegalArgumentException("Maximum week of month value (5) exceeded: "+wom);
		}
		if (wom<-5) {
			throw new IllegalArgumentException("Minimum week of month value (-5) exceeded: "+wom);
		}
		if (wom==0) {
			throw new IllegalArgumentException("No such week of month: "+wom);
		}
		return womMap.get(wom);
	}
	
	/**
	 * Create a {@link java.lang.String} representation of a Calendar's recurring events that
	 * is suitable to be spoken by Alexa.
	 * 
	 * @param calendar {@link trashday.model.Calendar} to be spoken verbally
	 * @param ldtBase Show the calendar with days relative to this {@link java.time.LocalDateTime}.  For example,
	 * 				if the ldtBase is Thursday, then a recurrence on Friday might be shown as "tomorrow".
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalCalendar(Calendar calendar, LocalDateTime ldtBase) {
		log.trace("verbalCalendar(ldtBase={})",ldtBase);
		Map<String, Map<String, String>> pickups = new LinkedHashMap<String, Map<String, String>>();
		
		for (CalendarEvent event : calendar.getEvents() ) {
			String pickupName = event.getName();
    		java.time.LocalDateTime ldtEventStart = event.getStartLocalDateTime();
			
			Map<String, String> eventOrderedRecurrences = new TreeMap<String, String>();
			for (Recur recur: event.getRecurrences()) {
				StringBuilder recurrenceString = new StringBuilder();
				String recurrenceOrder;
	        	WeekDayList dayList = recur.getDayList();
				
		        switch (recur.getFrequency()) {
		        case "WEEKLY":
	        		
		        	switch (recur.getInterval()) {
		        	case -1:
		        	case 0: // Weekly recurrence
		        	case 1:
		        		for (WeekDay wd : dayList) {
			        		recurrenceString.append("every ");
			        		recurrenceString.append(FormatUtils.verbalDayOfWeek(wd));
			        		recurrenceString.append(" at ");
			        		recurrenceString.append(FormatUtils.verbalTime(ldtEventStart));
				        	recurrenceOrder = String.format("A-%03d%d%05d", 1, DateTimeUtils.getDayOfWeek(wd).getValue(), DateTimeUtils.getMinuteOfDay(ldtEventStart));
					        log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
					        eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        		}
			        	break;
	        		
		        	case 2: // Bi-weekly recurrence		        		
		        		for (WeekDay wd : dayList) {
			        		recurrenceString.append("every other ");
			        		recurrenceString.append(FormatUtils.verbalDayOfWeek(wd));
			        		recurrenceString.append(" at ");
			        		recurrenceString.append(FormatUtils.verbalTime(ldtEventStart));
			        		java.time.LocalDateTime ldtBiWeeklyOccurrence = event.getNextOccurrence(ldtBase);
			        		if (ldtBiWeeklyOccurrence!=null) {
			        			long days = ChronoUnit.DAYS.between(ldtBase, ldtBiWeeklyOccurrence);	        			
			        			if (days < 7) {
			        				recurrenceString.append(" (on this ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtBiWeeklyOccurrence, ldtBase));
			        				recurrenceString.append(")");
			        			}
			        			else {
			        				recurrenceString.append(" (on next ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtBiWeeklyOccurrence, ldtBase));
			        				recurrenceString.append(")");	        				
			        			}
			        		}
			        		
			        		recurrenceOrder = String.format("A-%03d%d%05d", 2, ldtEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtEventStart));
					        log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
					        eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        		}
		        		break;
	        		
		        	default:// Multi-week recurrence
		        		for (WeekDay wd : dayList) {
			        		recurrenceString.append("every ");
			        		recurrenceString.append(recur.getInterval());
			        		recurrenceString.append(" weeks on ");
			        		recurrenceString.append(FormatUtils.verbalDayOfWeek(wd));
			        		recurrenceString.append(" at ");
			        		recurrenceString.append(FormatUtils.verbalTime(ldtEventStart));
			        		
			        		java.time.LocalDateTime ldtMultiWeekOccurrence = event.getNextOccurrence(ldtBase);
			        		if (ldtMultiWeekOccurrence!=null) {
			        			long days = ChronoUnit.DAYS.between(ldtBase, ldtMultiWeekOccurrence);
			        			if (days < 7) {
			        				recurrenceString.append(" (on this ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtMultiWeekOccurrence, ldtBase));
			        				recurrenceString.append(")");
			        			}
			        			else {
			        				recurrenceString.append(" (on next ");
			        				recurrenceString.append(FormatUtils.dateRelative(ldtMultiWeekOccurrence, ldtBase));
			        				recurrenceString.append(")");	        				
			        			}
			        		}
		
			        		recurrenceOrder = String.format("A-%03d%d%05d", recur.getInterval(), ldtEventStart.getDayOfWeek().getValue(), DateTimeUtils.getMinuteOfDay(ldtEventStart));
					        log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
					        eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        		}
		        		break;
		        	} // End of interval switch
		        	break;  // End of Weekly case
	        	
		        case "MONTHLY":
		        	for(Number dayNum : recur.getMonthDayList()) {
		        		int n = dayNum.intValue();  // n = [-31,-1] and [1,31]
		        		
		        		recurrenceString.append("on the ");
		        		recurrenceString.append(FormatUtils.verbalDayOfMonth(n));
		        		recurrenceString.append(" at ");
		        		recurrenceString.append(FormatUtils.verbalTime(ldtEventStart));
		        		
		        		if (n>0) {
		        			recurrenceOrder = String.format("B-1%02d", n);
		        		} else {
		        			recurrenceOrder = String.format("B-3%02d", 70+n);
		        		}
			        	log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
			        	eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
		        	}
	        		for (WeekDay wd : dayList) {
	        			int offset = wd.getOffset();
		        		if (offset!=0) {
			        		recurrenceString.append("on the ");
			        		recurrenceString.append(verbalWeekOfMonth(wd.getOffset()));
			        		recurrenceString.append(" ");
		        		} else {
			        		recurrenceString.append("on ");
		        		}
		        		recurrenceString.append(FormatUtils.verbalDayOfWeek(wd));
		        		recurrenceString.append(" at ");
		        		recurrenceString.append(FormatUtils.verbalTime(ldtEventStart));
		        		
		        		if (offset>0) {
		        			recurrenceOrder = String.format("B-2%02d", offset);
		        		} else {
		        			recurrenceOrder = String.format("B-2%02d", 70+offset);
		        		}
			        	log.debug("Add {} Recurrence: {}={}", pickupName, recurrenceOrder, recurrenceString.toString());
			        	eventOrderedRecurrences.put(recurrenceOrder, recurrenceString.toString());
	        		}
	        		break; // End of Monthly case
	        		
		    	default:
		        	throw new IllegalArgumentException("Unknown Recurrence rule frequency: "+recur.getFrequency());
		        	
		        } // End of frequency switch
			} // End of recurrences
			
			// Append this event's ordered recurrences to the ones in pickups map.
			if (pickups.containsKey(pickupName)) {
				Map<String, String> existingPickupMap = pickups.get(pickupName);
				for (String score: eventOrderedRecurrences.keySet()) {
					existingPickupMap.put(score, eventOrderedRecurrences.get(score));
				}
			} else {
				pickups.put(pickupName, eventOrderedRecurrences);
			}
		}
		
		// Print the pickups.
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Map<String, String>> pickupEntry: pickups.entrySet()) {
        	String pickupName = pickupEntry.getKey();
        	Map<String, String> recurrenceMap = pickupEntry.getValue();
            List<String> recurrenceStrings = new ArrayList<String>();
            for (Map.Entry<String, String> recurrenceEntry : recurrenceMap.entrySet()) {
            	recurrenceStrings.add(recurrenceEntry.getValue());
            }
    		
	        sb.append("Pickup ");
	        sb.append(pickupName);
	        sb.append(" ");
	        sb.append(FormatUtils.formattedJoin(recurrenceStrings, null, null));
	        sb.append(". ");
        }
        return sb.toString();
	}
	
	/**
	 * Provide a text version of the date and time that occurred, spoken relative to
	 * a given date and time.  For example, an occurrence that occurs on "Thursday,
	 * 2016-11-24 at 7 am" would be spoken as "Tomorrow at 7 am" if it was relative to
	 * a base date of "Wednesday, 2016-11-23".
	 * 
	 * @param ldtOccurrence {@link LocalTime} to be converted to be spoken verbally
	 * @param ldtBase {@link LocalDateTime} day of week is given as relative to the
	 * 		day the request is given.
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalDateAndTimeRelative(LocalDateTime ldtOccurrence, LocalDateTime ldtBase) {
		log.trace("verbalDateAndTimeRelative({},{})",ldtOccurrence,ldtBase);
		return dateRelative(ldtOccurrence,ldtBase) + " at " + verbalTime(ldtOccurrence);
	}
	
	/**
	 * Verbal day-of-month.  For example, "third", "last day of the month", "second day before the end of the month".
	 * 
	 * @param dom day-of-month to speak.  May be [1,31] or [-31,-1].
	 * @return {@link java.lang.String} verbal representation
	 */
	public static String verbalDayOfMonth(Integer dom) {
		log.trace("verbalDayOfMonth(dom={})", dom);
		if (dom>31) {
			throw new IllegalArgumentException("Maximum day of month value (31) exceeded: "+dom);
		}
		if (dom<-31) {
			throw new IllegalArgumentException("Minimum day of month value (-31) exceeded: "+dom);
		}
		if (dom==0) {
			throw new IllegalArgumentException("No such day of month: "+dom);
		}
		return domMap.get(dom);
	}
	
	public static String verbalDayOfWeek(WeekDay wd) {
		log.trace("verbalDayOfWeek(wd={})", wd);
		return printableDayOfWeek(wd);
	}
	
	/**
	 * Verbal day-of-week and time-of-day.  For example, "Saturday at 9 44 AM".
	 * 
	 * @param dow {@link java.time.DayOfWeek} to be spoken verbally
	 * @param tod {@link java.time.LocalTime} to be spoken verbally
	 * @return {@link java.lang.String} verbal representation
	 */
	public static String verbalDayOfWeekAndTime(DayOfWeek dow, LocalTime tod) {
		log.trace("verbalDayOfWeekAndTime(dow={}, tod={})", dow, tod);
		return dow.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " at " + verbalTime(tod);
	}
	
	/**
	 * Verbal day-of-week and time-of-day.  For example, "Saturday at 9 44 AM".
	 * 
	 * @param ldt {@link java.time.LocalDateTime} to be spoken verbally
	 * @return {@link java.lang.String} verbal representation
	 */
	public static String verbalDayOfWeekAndTime(LocalDateTime ldt) {
		log.trace("verbalDayOfWeekAndTime(ldt={})", ldt);
		DayOfWeek dow = ldt.getDayOfWeek();
		LocalTime tod = ldt.toLocalTime();
		return verbalDayOfWeekAndTime(dow, tod);
	}
	
	/**
	 * Create a {@link java.lang.String} representation of a {@link trashday.model.NextPickups} that
	 * is suitable to be spoken by Alexa.
	 * 
	 * @param nextPickups {@link trashday.model.NextPickups} to be spoken verbally
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalNextPickups(NextPickups nextPickups) {
		log.trace("verbalNextPickups()");
		LocalDateTime ldtStartingPoint = nextPickups.getStartingPoint();
		StringBuilder sb = new StringBuilder();
		for(Map.Entry<String,LocalDateTime> entry : nextPickups.getPickups().entrySet()) {
			String pickupName = entry.getKey();
			LocalDateTime ldtNextPickup = entry.getValue();
			sb.append("Next " + pickupName + " pickup is " + FormatUtils.verbalDateAndTimeRelative(ldtNextPickup, ldtStartingPoint) + ". ");
		}
		return sb.toString();
	}
	
	/**
	 * Provide a text version of the given {@link LocalTime} that is 
	 * suitable for Alexa to speak.
	 * 
	 * @param lt {@link LocalTime} to be spoken verbally
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
	 * @param ldt {@link LocalDateTime} to be spoken verbally
	 * @return text suitable for Alexa to speak
	 */
	public static String verbalTime(LocalDateTime ldt) {
		log.trace("verbalTime({})", ldt);
		LocalTime lt = LocalTime.of(ldt.getHour(), ldt.getMinute());
		return verbalTime(lt);
	}

	/**
	 * Verbal week-of-month.  For example, "third", "last", "second before last".
	 * 
	 * @param wom week-of-month to speak.  May be [1,5] or [-5,-1].
	 * @return {@link java.lang.String} verbal representation
	 */
	public static String verbalWeekOfMonth(Integer wom) {
		log.trace("verbalDWeekOfMonth(wom={})", wom);
		if (wom>5) {
			throw new IllegalArgumentException("Maximum week of month value (5) exceeded: "+wom);
		}
		if (wom<-5) {
			throw new IllegalArgumentException("Minimum week of month value (-5) exceeded: "+wom);
		}
		if (wom==0) {
			throw new IllegalArgumentException("No such week of month: "+wom);
		}
		return womMap.get(wom);
	}
	
}
