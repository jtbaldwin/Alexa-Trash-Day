# Trash Day Skill Testing Notes

Testing a verbal user interface (VUI) is time-intensive.  This project makes extensive use of JUnit test to exercise as much code as possible.  However, it's still necessary to do verbal testing to:
- assess Alexa service spoken responses for cadence, length, and understandability
- ensure the speech model is well-defined enough to differentiate spoken commands easily

## Test Results - Version 1.1

### Initial Launch, General Helps, and Single Schedule Entry

* Alexa, tell Trash Day, to clear entire schedule.
	* "Are you sure?"
	* (open session)
* Yes
	* "Cleared entire Schedule."
	* (closed session)

* Alexa, tell Trash Day, to delete entire schedule.
	* "The schedule is already cleared."
	* (closed session)

* Alexa, open Trash Day.
	* Welcome to Trash Day.  Please set your time zone using a phrase like: "Set time zone to Eastern, Central, Mountain, Pacific or Other."
	* (open session)

* Help
	* "The Trash Day skill remembers a regular pickup schedule for you.  You can then ask "...". But first, please set your time zone using a phrase like "Set time zone to Eastern, Central, Mountain, Pacific or Other." For further help, you can refer to the Trash Day help card just sent to your Alexa phone app.
	* (open session)

* Set time zone to other.
	* "There are nearly 600 other tz names accepted by TD.  You can try any common tz name using the phrase, "Set time zone to something".  Or see the complete TZ list using the URL at the bottom of the Trash Day Set Time Zone help card just sent to your Alexa app.
	* You can try any common tz name using the phrase, "Set time zone to something".
	* (open session)

* Set time zone to Eastern.
	* "Time zone set to US/Eastern.  Next, add regular pickup times using phrases like "...".  When you are finished say "Stop".  For more commands to update your schedule see the Trash Day help card just sent to your Alexa app.
	* "Add regular pickup times using phrase like "..."."
	* (open session)

* Help (sched empty, tz set)
	* "The TD skill remembers a regular pickup schedule for you. You can then ask "..." But first, please create your regular pickup schedule by saying "change schedule"
	* (open session)

* Help
	* "The TD skill remembers a regular pickup schedule for you. You can then ask "..." But first, please create your regular pickup schedule by saying "change schedule"
	* (open session)

* Change Schedule
	* "The pickup schedule is empty.  Add regular pickup times using phrases like "..."  When finished say Stop.
	* "Add regular pickup times by saying phrases like "..."  When finished say Stop."
	* (open session)

* Add weekly mail pickup on Monday at 4 PM.
	* "Added weekly mail pickup on Monday at 4pm.  To make more changes update schedule using phrases like "..." When finished say "stop"
	* (open session)

* Help
	* The TD skill remembers a regular pickup schedule for you. You can ask "..."  You can also update your pickup schedule by saying "change schedule". For further help you can refer to the TD help card just sent to your phone app.  
	* You can say "all the phrases or Stop"
	* (open session)

* Stop
	* "Done configuring schedule.  Next, try saying "Alexa ask TD is it TD?" or "Alexa, tell TD to tell me the schedule"
	* (closed session)

### Full Schedule Creation

Ensure that the "new user" experience is seamless for arbitrary pickup schedule entries.

* Alexa, tell Trash Day, to clear entire schedule.
	* Are you sure?
	* (open session)
* Yes
	* Cleared entire schedule.
	* (closed session)

* Alexa, open Trash Day. 
	* Welcome to TD.  Please set your time zone using a phrase like... or Other.
	* (open session)

* Set time zone to Eastern.
	* Time zone set to US/Eastern.  Next, add regular pickup times using phrases like "add weekly..." When you are finished, say "Stop."  Or card.
	* (open session)

* Add weekly trash pickup on Tuesday at seven-thirty A.M.
	* Added weekly trash pickup on Tuesday at 7:30 am.  To make more changes, update the schedule using phrases like... When you are finished, say "Stop."
	* Update the schedule using phrase like
	* (open session)

* Add weekly trash pickup on Tuesday at seven-thirty A.M.
	* That trash pickup already exists.  To make more changes,... When you are finished, say "Stop."
	* (open session)

* Add weekly trash pickup on Friday at seven-thirty A.M.
	* Added weekly trash pickup on Friday at 7:30 am.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Add biweekly recycling pickup on this Wednesday at four P.M.
	* Added biweekly recycling pickup on this Wednesday at 4pm.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Add biweekly lawn waste pickup on Wednesday after next at four P.M.
	* Added biweekly lawn waste pickup on Wednesday after next at 4pm.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Add monthly scrap metal pickup on the fifteenth at four P.M.
	* Added monthly scrap metal pickup on 15th at 4pm.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Add monthly scrap metal pickup on the last day of the month at four P.M.
	* Added monthly scrap metal pickup on the last day of the month at 4pm.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Add monthly rent pickup on the fifth day before the end of the month at four P.M.
	* Added monthly rent pickup on fifth day before the end of the month at four P.M.  To make more changes... When you are finished, say "Stop."
	* (open session)
	* Tested OK = first (=last day), second, third, fourth, fifth, sixth, seventh, eighth, ninth, tenth, eleventh, twelfth,
	* Tested OK = thirteenth, fourteenth, fifteenth, sixteenth, seventeenth, eighteenth, nineteenth, twentieth
	* Tested OK = twenty-first, twenty-second, twenty-third, twenty-fourth, twenty-fifth, twenty-sixth
	* Tested OK = twenty-seventh, twenty-eighth, twenty-ninth, thirtieth, thirty-first

* Add monthly rent pickup on the ninth day before the end of the month at four P.M.
	* Added monthly rent pickup on ninth day before the end of the month at four P.M.  Tom make more changes... When you are finished, say "Stop."
	* (open session)

* Add monthly rent pickup five days before the end of the month at 4 PM.
	* That rent pickup already exists.  To make more changes,... When you are finished, say "Stop."
	* (open session)

* Tell me the schedule
	* Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.
	* Pickup recycling every other Wednesday at 4 PM (on this Wednesday).
	* Pickup lawn waste every other Wednesday at 4 PM (on next Wednesday, March 1st).
	* Pickup scrap metal on the fifteenth at 4 PM and on the last day of the month at 4 PM.
	* Pickup rent on the ninth day before the end of the month at 4 PM and on the fifth day before the end of the month at 4 PM.
	* To make more changes... Stop
	* (open session)

* Stop
	* Done configuring the schedule.  Next, try saying "Alexa, ask TD, is it trash day" or "Alexa, tell Trash Day, to tell me the schedule"
	* (closed session)
 

### Normal Queries

Provide and test the common commands listed when opening Trash Day after user pickup schedule is defined.

* Alexa, ask Trash Day, is it Trash Day?
	* Next trash pickup is Friday at 7:30am.
	* (closed session)
	
* Alexa, ask Trash Day, when is the next recycling pickup?
	* Next recycling pickup is today at 4pm.
	* (closed session)

* Alexa, ask Trash Day, what's next?
	* Next recycling pickup is today at 4pm.
	* Next trash pickup is Friday at 7:30am.
	* Next rent pickup is Friday at 4pm.
	* Next scrap metal pickup is Tuesday at 4pm.
	* Next lawn waste pickup is Wednesday, March 1st at 4pm.
	* (closed session)
	
* Alexa, tell Trash Day to tell me the schedule.
	* Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.
	* Pickup recycling every other Wednesday at 4 PM (on this Wednesday).
	* Pickup lawn waste every other Wednesday at 4 PM (on next Wednesday, March 1st).
	* Pickup scrap metal on the fifteenth at 4 PM and on the last day of the month at 4 PM.
	* Pickup rent on the ninth day before the end of the month at 4 PM and on the fifth day before the end of the month at 4 PM.
	* (closed session)

* Alexa, open Trash Day.
	* "Welcome to Trash Day.  You can say phrases like: "What's next?", "Tell me the schedule", "Change Schedule.", or "Help"."
	* (open session)

* Change schedule.
	* Update the schedule using phrase like... Stop
	* (open session)

* Add weekly mail pickup on Monday at four P.M.
	* Added weekly mail pickup on Monday at 4pm.  To make more changes... Stop
	* (open session)
	
* Tell me the schedule
	* Pickup trash every Tuesday at 7:30 AM and every Friday at 7:30 AM.
	* Pickup recycling every other Wednesday at 4 PM (on this Wednesday).
	* Pickup lawn waste every other Wednesday at 4 PM (on next Wednesday, March 1st).
	* Pickup scrap metal on the fifteenth at 4 PM and on the last day of the month at 4 PM.
	* Pickup rent on the ninth day before the end of the month at 4 PM and on the fifth day before the end of the month at 4 PM.
	* Pickup mail every Monday at 4pm.
	* To make more changes... Stop
	* (open session)

* Stop
	* "Done configuring schedule.  Next, try saying "Alexa ask TD is it TD?" or "Alexa, tell TD to tell me the schedule"
	* (closed session)

* Alexa, ask Trash Day for Help
	* The TD skill remembers a regular pickup schedule for you.  You can ask ...  For further help refer to help card just sent.
	* (open session)
	
* Stop
	* Goodbye

### Full Schedule Deletions

Ensure all the delete pickup commands work.

* Alexa, tell Trash Day, to change schedule.
	* Update the schedule using phrases like... When you are finished, say "Stop."
	* (open session)
	
* Delete weekly trash pickup on Tuesday at seven-thirty A.M.
	* Removed weekly trash pickup on Tuesday at seven-thirty AM.  To make more changes... When you are finished, say "Stop."
	* (open session)
	
* Delete weekly trash pickup on Friday at seven-thirty P.M.
	* There was no weekly trash pickup scheduled on Friday at seven-thirty P.M.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Delete weekly trash pickup on Friday at seven-thirty A.M.
	* Removed weekly trash pickup on Friday at seven-thirty AM.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Delete biweekly recycling pickup on this Wednesday at four P.M.
	* Removed biweekly recycling pickup on Wednesday at four P.M. To make more changes... When you are finished, say "Stop."
	* (open session)

* Delete biweekly lawn waste pickup on Wednesday after next at four P.M.
	* Removed biweekly lawn waste pickup on Wednesday at 4 pm.  To make more changes... When you are finished, say "Stop."
	* (open session)
	
* Delete monthly scrap metal pickup on the fifteenth at four P.M.
	* Removed monthly scrap metal pickup on the 15th at 4pm.  To make more changes... When you are finished, say "Stop."
	* (open session)

* Delete monthly scrap metal pickup on the last day of the month at four P.M.
	* Removed monthly scrap metal pickup on the last day of the month at 4pm. To make more changes... When you are finished, say "Stop."
	* (open session)
	
* Delete monthly rent pickup on the fifth day before the end of the month at four P.M.
	* Removed monthly rent pickup on the fifth day before the end of the month at 4 pm. To make more changes... When you are finished, say "Stop."
	* (open session)
	
* Delete all rent pickups.
	* Removed all rent pickups from the schedule. To make more changes... When you are finished, say "Stop."  
	* (open session)

* Delete all mail pickups.
	* Removed all mail pickups from the schedule. To make more changes... When you are finished, say "Stop."  
	* (open session)

* Tell me the schedule.
	* The pickup schedule is empty.  Add regular pickup times using phrases like...  When you are finished, say "Stop."
	* (open session)
	
* Stop
	* Goodbye

* Alexa, open Trash Day.
	* The pickup schedule is empty.  Add regular pickup times using phrases like...  When you are finished, say "Stop."
	* (open session)
	
