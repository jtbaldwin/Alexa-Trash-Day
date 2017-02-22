package trashday.ui.responses;

/**
 * Organizes the phrases used by the application.
 * <p>
 * Keep the static text that is spoken and printed by the application as centralized as possible.  Makes it
 * easier to (a) keep text updates separate from code updates, (b) keep text updates consistent with
 * each other, (c) build JUnit tests that don't need update when phrasing changes.
 * 
 * @author J. Todd Baldwin
 */
public enum Phrase {
	OPEN_VERBAL("Welcome to Trash Day.  You can say phrases like: \"What's next?\", \"Tell me the schedule\", \"Change Schedule.\", or \"Help\"."),
	OPEN_REPROMPT("You can say phrases like: \"What's next?\", \"Tell me the schedule\", \"Change Schedule.\", or \"Help\"."),
	
	EXIT_VERBAL("Goodbye"),
	
	HELP_VERBAL_INITIAL("The Trash Day skill remembers a regular pickup schedule for you.  You could then ask: \"Is it trash day?\" or \"what's the schedule?\".  But first, please set your time zone using a phrase like: \"Set time zone to Eastern, Central, Mountain, Pacific or Other.\"."),
	
	HELP_VERBAL_NO_SCHEDULE("The Trash Day skill remembers a regular pickup schedule for you.  You could then ask: \"Is it trash day?\" or \"what's the schedule?\".  But first, please create your regular pickup schedule by saying, \"Change Schedule\"."),
	HELP_REPROMPT_NO_SCHEDULE("Please create your pickup schedule by saying, \"Change Schedule\"."),
	
	HELP_VERBAL_SCHEDULE_EXISTS("The Trash Day skill remembers a regular pickup schedule for you.  You can ask: \"Is it trash day?\", \"what's next?\", or \"what's the schedule?\".  You can also update your pickup schedule by saying, \"Change Schedule\"."),
	HELP_REPROMPT_SCHEDULE_EXISTS("You can say phrases like: \\\"Is it trash day?\\\", \"What's next?\", \"Tell me the schedule\", \"Change Schedule.\", or \"Stop\"."),
	
    HELP_VERBAL_CARD_SUFFIX(" For further help, you can refer to the Trash Day Help Card just sent to your Alexa phone app."),
    
	TIME_ZONE_SET_VERBAL("Set your time zone using a phrase like: \"Set time zone to Eastern, Central, Mountain, Pacific or Other.\""),
	TIME_ZONE_SET_VERBAL_CARD_SUFFIX(" For a list of other available time zone names, see the Trash Day Help Card just sent to your Alexa app."),
	TIME_ZONE_SET_REPROMPT("Say a phrase like: \"Set time zone to Eastern, Central, Mountain, Pacific or Other.\""),
	
	TIME_ZONE_OTHER_VERBAL("There are nearly 600 other time zone names accepted by Trash Day.  You can try any common time zone name using the phrase, \"set time zone to something\"."),
	TIME_ZONE_OTHER_VERBAL_CARD_SUFFIX(" Or, see the complete time zone list using the URL at the bottom of the \"Trash Day\" help card just sent to your Alexa app."),
	TIME_ZONE_OTHER_REPROMPT("You can try any common time zone name using the phrase, \"set time zone to something\"."),

    SCHEDULE_ADD_PICKUPS_VERBAL("Add regular pickup times using phrases like: \"Add weekly trash pickup on Wednesday at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_PICKUPS_REPROMPT("Add regular pickup times using phrases like: \"Add weekly trash pickup on Wednesday at 6 A.M.\""),
	
    SCHEDULE_ADD_WEEKLY_PICKUPS_VERBAL("Add weekly pickup times using phrases like: \"Add weekly trash pickup on Wednesday at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_WEEKLY_PICKUPS_REPROMPT("Add weekly pickup times using phrases like: \"Add weekly trash pickup on Wednesday at 6 A.M.\""),
	
    SCHEDULE_ADD_BIWEEKLY_PICKUPS_VERBAL("Add biweekly pickup times using phrases like: \"Add biweekly trash pickup on Wednesday at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_BIWEEKLY_PICKUPS_REPROMPT("Add biweekly pickup times using phrases like: \"Add biweekly trash pickup on Wednesday at 6 A.M.\""),
	
    SCHEDULE_ADD_MONTHLY_PICKUPS_VERBAL("Add monthly pickup times using phrases like: \"Add monthly trash pickup on the fifth at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_MONTHLY_PICKUPS_REPROMPT("Add monthly pickup times using phrases like: \"Add monthly trash pickup on the fifth at 6 A.M.\""),
	
    SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_VERBAL("Add last day of month pickup times using phrases like: \"Add monthly trash pickup on the last day of the month at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_MONTHLY_LAST_PICKUPS_REPROMPT("Add last day of month pickup times using phrases like: \"Add monthly trash pickup on the last day of the month at 6 A.M.\""),
	
    SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_VERBAL("Add monthly pickup times from the end of the month using phrases like: \"Add monthly trash pickup seven days before the end of the month at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_MONTHLY_LASTN_PICKUPS_REPROMPT("Add monthly pickup times from the end of the month using phrases like: \"Add monthly trash pickup seven days before the end of the month at 6 A.M.\""),
	
    SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_VERBAL("Add monthly pickup times for a given weekday of the month using phrases like: \"Add monthly trash pickup on the third Saturday of the month at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_MONTHLY_WEEKDAY_PICKUPS_REPROMPT("Add monthly pickup times for a given weekday of the month using phrases like: \"Add monthly trash pickup on the third Saturday of the month at 6 A.M.\""),
	
    SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL("Add monthly pickup times for a given weekday before the end of the month using phrases like: \"Add monthly trash pickup on the third to last Saturday of the month at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT("Add monthly pickup times for a given weekday before the end of the month using phrases like: \"Add monthly trash pickup on the third to last Saturday of the month at 6 A.M.\""),
	
	SCHEDULE_DELETE_PICKUPS_VERBAL("Delete pickup times using phrases like: \"Delete weekly trash pickup on Wednesday at 6 A.M.\""),
	SCHEDULE_DELETE_PICKUPS_REPROMPT("Delete pickup times using phrases like: \"Delete weekly trash pickup on Wednesday at 6 A.M.\""),
	
	SCHEDULE_DELETE_WEEKLY_PICKUPS_VERBAL("Delete weekly pickup times using phrases like: \"Delete weekly trash pickup on Wednesday at 6 A.M.\""),
	SCHEDULE_DELETE_WEEKLY_PICKUPS_REPROMPT("Delete weekly pickup times using phrases like: \"Delete weekly trash pickup on Wednesday at 6 A.M.\""),
	
	SCHEDULE_DELETE_BIWEEKLY_PICKUPS_VERBAL("Delete biweekly pickup times using phrases like: \"Delete biweekly trash pickup on Wednesday at 6 A.M.\""),
	SCHEDULE_DELETE_BIWEEKLY_PICKUPS_REPROMPT("Delete biweekly pickup times using phrases like: \"Delete biweekly trash pickup on Wednesday at 6 A.M.\""),
	
	SCHEDULE_DELETE_MONTHLY_PICKUPS_VERBAL("Delete monthly pickup times using phrases like: \"Delete monthly trash pickup on the fifteenth at 6 A.M.\""),
	SCHEDULE_DELETE_MONTHLY_PICKUPS_REPROMPT("Delete monthly pickup times using phrases like: \"Delete monthly trash pickup on the fifteenth at 6 A.M.\""),
	
	SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_VERBAL("Delete last day of month pickup times using phrases like: \"Delete monthly trash pickup on the last day of the month at 6 A.M.\""),
	SCHEDULE_DELETE_MONTHLY_LAST_PICKUPS_REPROMPT("Delete last day of month pickup times using phrases like: \"Delete monthly trash pickup on the last day of the month at 6 A.M.\""),
	
	SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_VERBAL("Delete monthly pickup times from the end of the month using phrases like: \"Delete monthly trash pickup seven days before the end of the month at 6 A.M.\""),
	SCHEDULE_DELETE_MONTHLY_LASTN_PICKUPS_REPROMPT("Delete monthly pickup times from the end of the month using phrases like: \"Delete monthly trash pickup seven days before the end of the month at 6 A.M.\""),
	
	SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_VERBAL("Delete monthly pickup times for a given weekday of the month using phrases like: \"Delete monthly trash pickup on the second Monday of the month at 6 A.M.\""),
	SCHEDULE_DELETE_MONTHLY_WEEKDAY_PICKUPS_REPROMPT("Delete monthly pickup times for a given weekday of the month using phrases like: \"Delete monthly trash pickup on the second Monday of the month at 6 A.M.\""),
	
	SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_VERBAL("Delete monthly pickup times for a given weekday before the end of the month using phrases like: \"Delete monthly trash pickup on the second to last Monday of the month at 6 A.M.\""),
	SCHEDULE_DELETE_MONTHLY_LASTN_WEEKDAY_PICKUPS_REPROMPT("Delete monthly pickup times for a given weekday before the end of the month using phrases like: \"Delete monthly trash pickup on the second to last Monday of the month at 6 A.M.\""),
	
	SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL("Delete entire pickups using phrases like: \"Delete entire trash pickup.\""),
	SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT("Delete entire pickups using phrases like: \"Delete entire trash pickup.\""),
	
	SCHEDULE_ALTER_VERBAL("Update the schedule using phrases like: \"Add or Delete weekly trash pickup on Monday at seven A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ALTER_VERBAL_CARD_SUFFIX(" For more commands to update your schedule, see the Trash Day Help Card just sent to your Alexa app."),
	SCHEDULE_ALTER_REPROMPT("Update the schedule using phrases like: \"Add or Delete weekly trash pickup on Monday at seven A.M.\""),
	
	SCHEDULE_DONE_VERBAL("Done configuring schedule. Next, try saying \"Alexa, ask Trash Day, is it trash day?\" or \"Alexa, tell Trash Day to tell me the schedule.\""),
	
    HELP_CARD(
		"On initial setup, you will need to teach Trash Day your time zone and regular pickup schedule.  Here's an simple example:\n" + 
		"  \"Alexa, open Trash Day.\"\n" + 
		"  \"Set time zone to Eastern\"\n" + 
		"  \"Add weekly trash pickup on Tuesday at seven-thirty A.M.\"\n" + 
		"  \"Add weekly trash pickup on Friday at seven-thirty A.M.\"\n" + 
		"  \"Add biweekly recycling pickup on this Wednesday at one P.M.\"\n" + 
		"  \"Add biweekly lawn waste pickup on next Wednesday at one P.M.\"\n" + 
		"  \"Tell me the schedule.\"\n" + 
		"  \"Stop.\"\n"+
		"\n" + 
		"Once you setup your schedule, you can ask quick questions like...\n" + 
		"  \"Alexa, ask Trash Day, what's next?\"\n" + 
		"  \"Alexa, ask Trash Day, is it trash day?\"\n" + 
		"  \"Alexa, ask Trash Day, when is the next recycling pickup?\"\n" + 
		"  \"Alexa, ask Trash Day, what is the schedule?\"\n" + 
		"\n" + 
		"If you need to make schedule changes later, you can use any commands like the following examples.  (NOTE: Any of the \"add\" commands also have \"delete\" counterparts.)\n" + 
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Delete all recycling pickups.\"\n" + 
		"  \"Add weekly recycling pickup on Friday at seven-thirty A.M.\"\n" + 
		"  \"Add monthly scrap metal pickup on the fifteenth at four P.M.\"\n" + 
		"  \"Add monthly scrap metal pickup on the last day of the month at four P.M.\"\n" + 
		"  \"Delete monthly rent pickup five days before the end of the month at five P.M.\"\n" + 
		"  \"Add monthly rent pickup seven days before the end of the month at five P.M.\"\n" + 
		"  \"Delete all hockey team pickups.\"\n" +
		"  \"Add monthly hockey team pickup on the first Saturday of the month at nine A.M.\"\n" +
		"  \"Add monthly hockey team pickup on the third Saturday of the month at nine A.M.\"\n" +
		"  \"Add monthly little league pickup on the last Saturday of the month at ten A.M.\"\n" +
		"  \"Tell me the schedule.\"\n" +
		"  \"Stop.\"\n"+
		"\n" +
		"If you want, you can also delete the entire schedule and start over...\n" + 
		"  \"Alexa, tell Trash Day to delete the entire schedule.\"\n" + 
		"\n" + 
		"For more extensive help, refer to the documentation at: "+
		"https://s3.amazonaws.com/trash-day-docs/Help.html"),

	TIME_ZONE_HELP_CARD(
		"Set your time zone by saying:\n"+
		"  \"Alexa, tell Trash Day, set time zone to <Your_Time_Zone>\"\n"+
		"\n"+
		"The United States time zone names are:\n"+
		"  Eastern, Central, Mountain, and Pacific\n"+
		"\n"+
		"There are nearly 600 other time zone names accepted by "+
		"Trash Day (too many to list here).  Try the time zone "+
		"you use on a computer in your house.  Or see the "+
		"complete list at: https://s3.amazonaws.com/trash-day-docs/TimeZoneNames.html"),

	SCHEDULE_ALTER_CARD(
		"Use the following commands to alter your regular pickup schedule.\n"+
		"\n"+
		"Add weekly, biweekly, or monthly pickup times using phrases like: \n"+
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Add weekly trash pickup on Tuesday at seven-thirty A.M.\"\n" + 
		"  \"Add weekly trash pickup on Friday at seven-thirty A.M.\"\n" + 
		"  \"Add biweekly recycling pickup on this Wednesday at one P.M.\"\n" + 
		"  \"Add biweekly lawn waste pickup on next Wednesday at one P.M.\"\n" + 
		"  \"Add monthly scrap metal pickup on the fifteenth at four P.M.\"\n" + 
		"  \"Add monthly scrap metal pickup on the last day of the month at four P.M.\"\n" + 
		"  \"Add monthly rent pickup seven days before the end of the month at five P.M.\"\n" + 
		"  \"Add monthly hockey team pickup on the first Saturday of the month at nine A.M.\"\n" +
		"  \"Add monthly hockey team pickup on the third Saturday of the month at nine A.M.\"\n" +
		"  \"Add monthly little league pickup on the last Saturday of the month at ten A.M.\"\n" +
		"  \"Tell me the schedule.\"\n"+
		"  \"Stop.\"\n"+
		"\n"+
		"Delete pickup times using the \"delete all XYZ pickups\" command or any of the \"add\" phrases by changing \"add\" to \"delete\".  For example:\n"+
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Delete all trash pickups.\"\n" +
		"  \"Delete weekly trash pickup on Tuesday at seven-thirty A.M.\"\n" + 
		"  \"Delete biweekly recycling pickup on this Wednesday at one P.M.\"\n" + 
		"  \"Delete monthly scrap metal pickup on the last day of the month at four P.M.\"\n" + 
		"  \"Delete monthly rent pickup seven days before the end of the month at five P.M.\"\n" + 
		"  \"Delete monthly hockey team pickup on the third Saturday of the month at nine A.M.\"\n" +
		"  \"Tell me the schedule.\"\n"+
		"  \"Stop.\"\n"+
		"\n"+
		"Update your time zone by saying:\n"+
		"  \"Alexa, tell Trash Day, set time zone to Pacific.\"\n"+
		"\n"+
		"To start over, delete entire schedule by saying:\n"+
		"  \"Alexa, tell Trash Day to change schedule.\"\n"+
		"  \"Delete entire schedule.\"\n"+
		"  \"Yes.\"\n"+
		"  \"Stop.\"\n"+
		"\n" + 
		"For more extensive help, refer to the documentation at: "+
		"https://s3.amazonaws.com/trash-day-docs/Help.html"),
	;
	
	private String value; 
	
	private Phrase(String value) { 
		this.value = value; 
	}
	
	@Override
	public String toString() {
		return value;
	}
	
}
