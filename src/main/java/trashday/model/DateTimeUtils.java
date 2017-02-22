package trashday.model;

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import trashday.CoberturaIgnore;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Helper functions that address gaps and inconsistencies among the various date/time components in
 * early Java, ical4j's work-arounds, and Java 8.
 * 
 * @author J. Todd Baldwin
 * @see		<a href="https://github.com/ical4j/ical4j/blob/master/README.md">iCal4j Readme: ical4j and Timezones</a>
 * @see     <a href="http://www.oracle.com/technetwork/articles/java/jf14-date-time-2125367.html">Java SE 8 Date and Time</a>
 */
public class DateTimeUtils {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(DateTimeUtils.class);
    
	/**
	 * Private constructor given to this utility class.  Prevents instantiation since
	 * this class is only meant to provide public, static utility methods.
	 */
	@CoberturaIgnore
	private DateTimeUtils() {
    }
	

	/**
	 * Find a {@link java.time.LocalDateTime} for the given user 
	 * {@link java.util.Date} and {@link java.util.TimeZone}.
	 * 
	 * @param datetime {@link java.util.Date} source
	 * @param timeZone {@link java.util.TimeZone} source
	 * @return {@link java.time.LocalDateTime} corresponding to given datetime and timeZone
	 */
	public static LocalDateTime getLocalDateTime(Date datetime, TimeZone timeZone) {
    	if (datetime == null) {
    		return null;
    	}
		if (timeZone == null) {
			return null;
		}
    	ZoneId zoneId = timeZone.toZoneId();
		return LocalDateTime.ofInstant(datetime.toInstant(), zoneId);
	}
	
	/**
	 * Get the minute of the day for a given {@link java.time.LocalDateTime}.  For example,
	 * 1:05 AM = 60+5 = 65, 2:10 AM = 120 + 10 = 130, etc.  Provides an easy way to sort
	 * {@link java.time.LocalDateTime} objects based on time-of-day, but independent of which
	 * day they occur.
	 * 
	 * @param ldt {@link java.time.LocalDateTime} source
	 * @return minute-of-day for given {@link java.time.LocalDateTime}
	 */
	public static int getMinuteOfDay(LocalDateTime ldt) {
		return ( ( ldt.getHour() * 60 ) + ldt.getMinute() );
	}
	
    /**
     * Convert from an ical4j {@link net.fortuna.ical4j.model.WeekDay} to a Java 8 {@link java.time.DayOfWeek}.
     * 
     * @param wd ical4j weekday source
     * @return {@link java.time.DayOfWeek} value
     */
    public static java.time.DayOfWeek getDayOfWeek(net.fortuna.ical4j.model.WeekDay wd) {
    	switch (wd.getDay()) {
		case SU:
			return java.time.DayOfWeek.SUNDAY;
		case MO:
			return java.time.DayOfWeek.MONDAY;
		case TU:
			return java.time.DayOfWeek.TUESDAY;
		case WE:
			return java.time.DayOfWeek.WEDNESDAY;
		case TH:
			return java.time.DayOfWeek.THURSDAY;
		case FR:
			return java.time.DayOfWeek.FRIDAY;
		case SA:
			return java.time.DayOfWeek.SATURDAY;
		default:
			return null;
    	}
    }
    
    /**
     * Convert from Java 8 {@link java.time.DayOfWeek} with a given week-of-month offset into the ical4j
     * {@link net.fortuna.ical4j.model.WeekDay}.
     * 
     * @param dow {@link java.time.DayOfWeek} source
     * @param weekNum Integer for week-of-month turns into the {@link net.fortuna.ical4j.model.WeekDay} offset.
     * @return {@link net.fortuna.ical4j.model.WeekDay}
     */
    public static net.fortuna.ical4j.model.WeekDay getWeekDay(java.time.DayOfWeek dow, Integer weekNum) {
		switch (dow) {
		case SUNDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.SU, weekNum);
		case MONDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.MO, weekNum);
		case TUESDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.TU, weekNum);
		case WEDNESDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.WE, weekNum);
		case THURSDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.TH, weekNum);
		case FRIDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.FR, weekNum);
		case SATURDAY:
	        return new net.fortuna.ical4j.model.WeekDay(net.fortuna.ical4j.model.WeekDay.SA, weekNum);
		}
		return null;
    }
	    
	/**
	 * Find the time a given day-of-month occurs.
	 * <p>
	 * Would really like an easy Java 8 TemporalAdjuster.  (See link below.)  But this functions serves the purpose for now.
	 * 
	 * @param ldtBase Check for occurrences at this date/time or later.
	 * @param dom day-of-month to find.  May be [1,31] or [-31,-1].
	 * @param tod {@link java.time.LocalTime} time-of-day to set in result.
	 * @return {@link java.time.LocalDateTime} of the first time the given dom and tod occur on-or-after
	 * 				the given ldtBase.
	 * @see 	<a href="https://docs.oracle.com/javase/8/docs/api/java/time/temporal/TemporalAdjusters.html#nextOrSame-java.time.DayOfWeek-">Java 8 TemporalAdjusters.nextOrSame</a>
	 */
	public static LocalDateTime getNextOrSameDayOfMonth(LocalDateTime ldtBase, Integer dom, LocalTime tod) {
		log.trace("getNextOrSameDayOfMonth(ldtBase={}, dom={}, tod={}", ldtBase, dom, tod);
		if (dom>31) {
			throw new IllegalArgumentException("Maximum day of month value (31) exceeded: "+dom);
		}
		if (dom<-31) {
			throw new IllegalArgumentException("Minimum day of month value (-31) exceeded: "+dom);
		}
		if (dom==0) {
			throw new IllegalArgumentException("No such day of month: "+dom);
		}
		
		if (dom>0) {
			// Given a numeric day-of-month range: [1,31].
			log.debug("dom in [1,31] range.");
			
			// Use ldtBase as a starting point with the right day-of-month and time-of-day.
			// Advance month-by-month to ensure the day of month (31, 30, other) is possible.
			int incrementMonth = 0;
			LocalDateTime ldtNext = null;
			while (ldtNext==null) {
				// Does this month have enough days?
				try {
					ldtNext = ldtBase.truncatedTo(ChronoUnit.MINUTES)
						.plusMonths(incrementMonth)
						.withDayOfMonth(dom)
						.withHour(tod.getHour())
						.withMinute(tod.getMinute());
				} catch (DateTimeException ex) {
					// Not enough days, try next month.
					log.debug("Not enough days in {}", ldtBase.plusMonths(incrementMonth).getMonth());
					incrementMonth++;
					continue;
				}
				// Month has enough days.  If before ldtBase, move to next month.
				int daysInMonth = ldtNext.with(TemporalAdjusters.lastDayOfMonth()).getDayOfMonth();
				log.debug("There are {} days in {}.", daysInMonth, ldtNext.getMonth());
				if (ldtNext.isBefore(ldtBase)) {
					log.debug("{} is before base date.  Moving to next month.", ldtNext);
					incrementMonth++;
					ldtNext=null;
				}
			}
			return ldtNext;
			
		} else {
			log.debug("dom in [-31,-1] range.");
			// Find next day when day-of-month is dom days before end of month.
			// For example, dom=-1 => last day of month
			// For example, dom=-31 => first day of month in next 31-day month

			// Use ldtBase as a starting point with the right day-of-month and time-of-day.
			// Advance month-by-month to ensure the day of month (31, 30, other) is possible.
			int incrementMonth = 0;
			LocalDateTime ldtNext = null;
			while (ldtNext==null) {
				// Does this month have enough days?
				LocalDateTime ldtCheck = ldtBase
						.plusMonths(incrementMonth)
						.with(TemporalAdjusters.lastDayOfMonth());
				int daysInMonth = ldtCheck.getDayOfMonth();
				log.debug("There are {} days in {} (dom needs {}).", daysInMonth, ldtCheck.getMonth(), -dom);
				if (-dom > daysInMonth) {
					// Not enough days, try next month.
					log.debug("Not enough days in {}", ldtBase.plusMonths(incrementMonth).getMonth());
					incrementMonth++;
					continue;
				}
				
				// Month has enough days.  If before ldtBase, move to next month.
				ldtNext = ldtBase.truncatedTo(ChronoUnit.MINUTES)
						.plusMonths(incrementMonth)
						.withDayOfMonth( daysInMonth + dom + 1)
						.withHour(tod.getHour())
						.withMinute(tod.getMinute());					
				if (ldtNext.isBefore(ldtBase)) {
					log.debug("{} is before base date.  Moving to next month.", ldtNext);
					incrementMonth++;
					ldtNext=null;
				}
			}
			return ldtNext;
		}		
	}

	/**
	 * Find the time a given weekday-of-month occurs.  For example, 2nd Sunday, 1st Friday, etc.
	 * <p>
	 * Would really like an easy Java 8 TemporalAdjuster.  (See link below.)  But this functions serves the purpose for now.
	 * 
	 * @param ldtBase Check for occurrences at this date/time or later.
	 * @param dow day-of-week to find..
	 * @param weekNum Recur every Nth dow in the month.  Allowed values [1,5] and [-5,-1]
	 * @param tod {@link java.time.LocalTime} time-of-day to set in result.
	 * @return {@link java.time.LocalDateTime} of the first time the given dow+weekNum occur on-or-after
	 * 				the given ldtBase.
	 * @see 	<a href="https://docs.oracle.com/javase/8/docs/api/java/time/temporal/TemporalAdjusters.html#nextOrSame-java.time.DayOfWeek-">Java 8 TemporalAdjusters.nextOrSame</a>
	 */
	public static LocalDateTime getNextOrSameWeekdayOfMonth(LocalDateTime ldtBase, DayOfWeek dow, Integer weekNum, LocalTime tod) {
		log.trace("getNextOrSameWeekdayOfMonth(ldtBase={}, dow={}, weekNum={}, tod={}", ldtBase, dow, weekNum, tod);
		if (weekNum==0) {
			throw new IllegalArgumentException("No such week number: "+weekNum);
		}

		// Use ldtBase as a starting point with the right day-of-month and time-of-day.
		// Advance month-by-month to ensure the day of month (31, 30, other) is possible.
		int incrementMonth = 0;
		LocalDateTime ldtNext = null;
		while ((ldtNext==null)&&(incrementMonth<12)) {
			// Can we match this weekNum/dow in this month?
			LocalDateTime ldtMonth = ldtBase.truncatedTo(ChronoUnit.MINUTES)
					.plusMonths(incrementMonth);
			ldtNext = ldtMonth
				.with(TemporalAdjusters.dayOfWeekInMonth(weekNum, dow))
				.withHour(tod.getHour())
				.withMinute(tod.getMinute());
				
			if (ldtMonth.getMonthValue() != ldtNext.getMonthValue()) {
				// Can't get dow/weekNum in this month.  Try next one.
				log.debug("Can't find {}th {} in {}", weekNum, dow, ldtMonth.getMonth());
				ldtNext=null;
				incrementMonth++;
				continue;
			}
			
			// This month works.  Ensure we're after ldtBase though...
			log.debug("Found {}th {} is {}", weekNum, ldtNext.getDayOfWeek(), ldtNext);
			if (ldtNext.isBefore(ldtBase)) {
				log.debug("{} is before base date.  Moving to next month.", ldtNext);
				ldtNext=null;
				incrementMonth++;
				continue;
			}
		}
		if (ldtNext==null) {
			throw new IllegalArgumentException("Cannot find the "+weekNum+"th "+dow+" within a year");
		}
		return ldtNext;
	}
}
