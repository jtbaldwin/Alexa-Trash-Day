package trashday.ui.responses;

public enum Phrases {
	OPEN_VERBAL("Welcome to Trash Day.  You can say phrases like: \"What's next?\", \"Tell me the schedule\", \"Change Schedule.\", or \"Help\"."),
	OPEN_REPROMPT("You can say phrases like: \"What's next?\", \"Tell me the schedule\", \"Change Schedule.\", or \"Help\"."),
	
	EXIT_VERBAL("Goodbye"),
	
	HELP_VERBAL_HELP("Try asking \"what is the schedule?\"  Or ask for the next pickup time with \"what's next?\" or \"when is the next trash pickup?\". You can update the schedule by saying \"change schedule\"."),
    HELP_VERBAL_CARD_SUFFIX(" For further help, you can refer to the Trash Day Help Card just sent to your Alexa phone app."),
    HELP_CARD_HELP(
	"On initial setup, you will need to teach Trash Day your time zone and weekly pickup schedule.  Here's mine:\n" + 
	"  Alexa, open Trash Day.\n" + 
	"  Eastern\n" + 
	"  Add trash pickup on Tuesday at seven-thirty a.m.\n" + 
	"  Add trash pickup on Friday at seven-thirty a.m.\n" + 
	"  Add recycling pickup on Friday at seven-thirty a.m.\n" + 
	"  Add lawn waste pickup on Wednesday at one p.m.\n" + 
	"  Tell me the schedule.\n" + 
	"  Stop.\n"+
	"\n" + 
	"Once you setup your schedule, you can ask quick questions like...\n" + 
	"  Alexa, ask Trash Day, what's next?\n" + 
	"  Alexa, ask Trash Day, is it trash day?\n" + 
	"  Alexa, ask Trash Day, when is the next recycling pickup?\n" + 
	"  Alexa, ask Trash Day, what is the schedule?\n" + 
	"\n" + 
	"If you need to make schedule changes, you can use commands like...\n" + 
	"  Alexa, tell Trash Day to change schedule.\n"+
	"  Add recycling pickup on Friday at seven-thirty a.m.\n" + 
	"  Add recycling pickup on Wednesday at one p.m.\n" + 
	"  Delete all lawn waste pickups.\n" + 
	"  Tell me the schedule.\n" + 
	"  Stop.\n"+
	"\n" +
	"If you want, you can also delete the entire schedule and start over...\n" + 
	"  Alexa, tell Trash Day to delete the entire schedule.\n" + 
	"\n" + 
	"For more extensive help, refer to the documentation at: "+
	"https://s3.amazonaws.com/trash-day-docs/Help.html"),
	
	TIME_ZONE_SET_VERBAL("Set your time zone using a phrase like: \"Set time zone to Eastern, Central, Mountain, Pacific or Other.\""),
	TIME_ZONE_SET_VERBAL_CARD_SUFFIX(" For a list of other available time zone names, see the Trash Day Help Card just sent to your Alexa app."),
	TIME_ZONE_SET_REPROMPT("Say a phrase like: \"Set time zone to Eastern, Central, Mountain, Pacific or Other.\""),
	TIME_ZONE_SET_HELP_CARD(
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

    SCHEDULE_ADD_PICKUPS_VERBAL("Add weekly pickup times using phrases like: \"Add trash pickup on Wednesday at 6 A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ADD_PICKUPS_VERBAL_CARD_SUFFIX(" For help with updating the schedule, see the Trash Day Help Card just sent to your Alexa app."),
	SCHEDULE_ADD_PICKUPS_REPROMPT("Add weekly pickup times using phrases like: \"Add trash pickup on Wednesday at 6 A.M.\""),
	
	SCHEDULE_DELETE_PICKUPS_VERBAL("Delete weekly pickup times using phrases like: \"Delete trash pickup on Wednesday at 6 A.M.\""),
	SCHEDULE_DELETE_PICKUPS_VERBAL_CARD_SUFFIX(" For help with updating the schedule, see the Trash Day Help Card just sent to your Alexa app."),
	SCHEDULE_DELETE_PICKUPS_REPROMPT("Delete weekly pickup times using phrases like: \"Delete trash pickup on Wednesday at 6 A.M.\""),
	
	SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL("Delete entire weekly pickups using phrases like: \"Delete entire trash pickup.\""),
	SCHEDULE_DELETE_ENTIRE_PICKUP_VERBAL_CARD_SUFFIX(" For help with updating the schedule, see the Trash Day Help Card just sent to your Alexa app."),
	SCHEDULE_DELETE_ENTIRE_PICKUP_REPROMPT("Delete entire weekly pickups using phrases like: \"Delete entire trash pickup.\""),
	
	SCHEDULE_ALTER_VERBAL("Update the schedule using phrases like: \"Add or Delete trash pickup on Monday at seven A.M.\". When you are finished, say \"Stop\"."),
	SCHEDULE_ALTER_VERBAL_CARD_SUFFIX(" For more commands to update your schedule, see the Trash Day Help Card just sent to your Alexa app."),
	SCHEDULE_ALTER_REPROMPT("Update the schedule using phrases like: \"Add or Delete trash pickup on Monday at seven A.M.\""),
	
	SCHEDULE_DONE_VERBAL("Done configuring schedule. Next, try saying \"Alexa, ask Trash Day, is it trash day?\" or \"Alexa, tell Trash Day to tell me the schedule.\""),
	
	SCHEDULE_ALTER_CARD(
		"Use the following commands to alter your weekly pickup schedule.\n"+
		"\n"+
		"Add weekly pickup times using phrases like: \n"+
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Add trash pickup on Tuesday at 6 am.\"\n"+
		"  \"Add trash pickup on Friday at 6 am.\"\n"+
		"  \"Add recycling pickup on Friday at 6 am.\"\n"+
		"  \"Add lawn waste pickup on Wednesday at 4 pm.\"\n"+
		"  \"Tell me the schedule.\n"+
		"  \"Stop.\n"+
		"\n"+
		"Delete pickup times using phrases like:\n"+
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Delete trash pickup on Tuesday at 6 am.\"\n"+
		"  \"Delete lawn waste pickup on Wednesday at 4 pm.\"\n"+
		"  \"Stop.\n"+
		"\n"+
		"Delete entire pickups using a phrase like:\n"+
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Delete entire trash pickup.\"\n"+
		"  \"Stop.\n"+
		"\n"+
		"Update your time zone by saying:\n"+
		"  \"Alexa, tell Trash Day, to change schedule.\"\n"+
		"  \"Set time zone to Pacific.\"\n"+
		"  \"Stop.\n"+
		"\n"+
		"To start over, delete entire schedule by saying:\n"+
		"  \"Alexa, tell Trash Day to change schedule.\"\n"+
		"  \"Delete entire schedule.\"\n"+
		"  \"Yes.\"\n"+
		"  \"Stop.\n"
		),
	;
	
	private String value; 
	
	private Phrases(String value) { 
		this.value = value; 
	}
	
	@Override
	public String toString() {
		return value;
	}
	
}
