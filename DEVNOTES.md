# Trash Day Skill Development Notes

## Design

Originally intended as a simple application to allow me to learn how to code and Alexa skill. Designed to allow my Amazon Echo to answer "Is it Trash Day?"  Wanted to code in Java to allow exploring the depth of JavaDoc, JUnit, and Cobertura code documentation and testing tools.  Also wanted to increase my Amazon Web Service (AWS) experience by using their Lambda, Dynamo DB, IAM, S3, and Alexa services.

To perform the application function, I need to have a database with the pickup schedule for each user. Every schedule should allow add/delete of different pickups at different days and times.  For example, "weekly Trash pickup every Tuesday at 8 AM and every Friday at 8 AM" and "weekly Recycling pickup every Friday at 8 AM".

The skill dialog will include commands to query the pickup schedule (e.g. "Is it Trash day?", "what is the schedule?", "when is next recycling pickup?").  It will also include commands to alter the pickup schedule (e.g. "Add trash pickup on Tuesday at 8 AM." and "Delete recycling pickup on Friday at 8 AM.").

## Version 1.0 Notes - Initial Release

Initial code structure came from the [Alexa Skills Kit Java: ScoreKeepeer skill](https://github.com/amzn/alexa-skills-kit-java/tree/master/samples/src/main/java/scorekeeper).  This is a java-based skill that uses Lambda and Dynamo DB services and served as a good application base.

Constructed the data model in trashday.model package.  Based all the date and time handling on the new functions available in [Java SE 8 Date and Time](http://www.oracle.com/technetwork/articles/java/jf14-date-time-2125367.html).

Created model storage in the trashday.storage package.  Handles keeping user pickup schedule information in the Alexa Session objects used during user conversations and in Dynamo DB database used for long-term storage of user information.  Designed to use the session as a cache of user data whenever possible to minimize database access and improve user response times during conversations.

The trashday.ui.speechAssets package keeps information about the skill's verbal user interface (VUI) in a form required by Amazon Alexa.  User verbal requests are mapped by the Alexa speech recognition into user "Intents" (or commands) with accompanying data "Slots" (essentially variable parameters for each intent).  See [Alexa Skills Kit Custom Interaction Model Reference](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-interaction-model-reference).

Basics of user conversations take place in the trashday package.  The TrashDaySpeechlet handles the intents received from Alexa service and maps them to Trash-Day-specific functions in the TrashDayManager.  The TrashDayManager handle the core application logic of loading appropriate user data, performing queries and updates, and choosing appropriate verbal responses to each command.

As the project progressed, it became apparent that giving verbal responses to the user is a large component of the application.  As the VUI evolved through the coding, responses were constantly getting updated.  Found that Alexa spoken responses would have to be subtly different than printed responses (e.g. "6:30 AM" vs "6 30 AM.").  Found that conversation flow might change, requiring a "Tell" response vs. an "Ask" response and that some responses might need Card responses appended or removed.  Created the trashday.ui packages to provide all the responses and data handling required to make the VUI clear.

Added JUnit tests for as close to 100% of the code as possible.  Using the Cobertura test coverage reports, added JUnit tests to cover as much code as possible.  Needed to make different constructors in several parts of the code to ensure JUnit tests able to run on local development host and local Dynamo DB instance.  Therefore, can never really meet 100% code coverage as at least some code is different in the testing environment.  Added CoberturaIgnore annotation to help ignore as many of these areas as possible.

Added Javadoc documentation to nearly all of the code.  Encountered known bugs in maven-javadoc-plugin and found a variety of small issues with linking to external packages.  Created javadocs for the application itself as well as the JUnit tests.

In preparation for release, went through the [Certification Requirements for Custom Skills](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/alexa-skills-kit-submission-checklist).  Also added trashday.model.IntentLog with trashday.storage.* updates to keep log information on skill usage in the Dynamo DB.

Uploaded the application to GitHub.  Chose MIT License for source code distribution.  Included source code, javadocs, and Cobertura coverage reports.

### Time Zone Problems

A large work-around was required for handling time zone information.  The Alexa service does not share user time zone information with third-party skills created with Alexa Skill Kit (ASK).  This required adding new user conversation to acquire and store user time zone information.  Since Java supports roughly 600 different time zone names, added JUnit test code to handle translating acceptable Java time one names into the strings the Alexa service will give when the user speaks those names.  For example, "US/Eastern" Java time zone name is spoken as "US slash Eastern" and given to our skill as "US slash Eastern".  Added methods to trashday.ui.requests.SlotTimeZone to handle translating between these "Alexa-speak" forms and the strings java understands as time zone names.

This would be much easier if Alexa service provided this information (with appropriate user permissions) to 3rd party skills.  Note that other skills from large third-parties don't seem to have this problem.  Why does the ASK not provide this to the "little-guy" skill developers?

## Version 1.1 Notes - Bi-Weekly Pickups

Within three days after deployment, I received two requests for "every other week" pickups.

The original version used a custom pickup schedule model (trashday.model.Schedule).  This model was based on Java 8 Date and Time and stored pickups as simple java.time.DayOfWeek and java.time.LocalTime object for each pickup.  Design was chosen to minimize potential for bugs by keeping it a very simple format that did not rely on third-party libraries.  But with a request for more-advanced pickup schedules being an immediate need, chose to move to a full [RFC 5545 iCalendar](https://tools.ietf.org/html/rfc5545) implementation that would support arbitrarily complex recurring events and allow for, possibly, later sharing of pickup schedule information with other applications.  Chose the [Ben Fortuna's ical4j](https://github.com/ical4j/ical4j) library as it met my licensing requirements was mature enough that it should have a well-developed API and be bug-free for most functions.

Revised the application with new classes trashday.model.Calendar and trashday.model.CalendarEvent.  Deprecated the trashday.model.Schedule and trashday.model.TimeOfWeek classes and associated test classes.  Suppress deprecation warnings in trashday.model.Calendar and trashday.storage.* that will need to read old Schedule data for the intent of upgrading it to Calendar formats.  Removed ability to store Schedule data via SessionDao.

Updated the trashday.storage.Dynamo* functions to handle reading old (with Schedule) and new (with Calendar) user data.  Automatically update from Schedule to Calendar whenever a user item is loaded that has not yet been upgraded.  This ensures all application code now works only with new Calendar-based pickup schedules and isolates all pickup schedule updating to the Dynamo DB read functions and a Calendar constructor to create from Schedule.  Added test functions to test this upgrade-on-read.


## Further Work

### Conversation Handling

A glaring issue at this point is an absence of conversation handling support.  When the project required adding conversation for setting time zones or adding biweekly/monthly pickups, the size of the trashday.ui.responses.ScheduleResponses ballooned massively.  Most of the code is also not even exercised by the application or test functions.

A shortcoming of the existing application is that add/delete of pickup information requires several user data fields.  If any are missing, the application requires the userto repeat the entire command.  When the command is long (e.g. "add biweekly recycling pickup on Wednesday at 8 AM.") it becomes difficult to get this right for the whole command.  The skill should instead just asking for specific missing or invalid items required to finish the add/delete command.

Unfortunately, the ASK API does not provide much support for handling conversation state information.  The application must handle storing all conversation data in 
com.amazon.speech.speechlet.Session attributes.

Application screams for a hierarchical state machine or behavior tree to handle what user conversation is currently in progress and what data is still needed from the user to complete requested actions.  This state would need to be passed through Session attributes.  It would also need to be easy to update and handle add/remove or speech, reprompt, and card data in user responses.

If done properly, expect much decreased complexity in trashday.ui.responses and associated JUnit tests.  Should improve test coverage percentage and make tests easier to write for longer conversations with the user.  This should then yield quicker coding times with improved conversational patterns for the user.

Would like to have something that allows a graphical representation of the dialog structure as well.  Perhaps something that would even generate code from a UML (or other) model?

### State Machine Library Options
- https://github.com/libgdx/gdx-ai/wiki/State-Machine
- https://github.com/libgdx/gdx-ai/wiki/Behavior-Trees
- http://alistapart.com/article/all-talk-and-no-buttons-the-conversational-ui

### Conversation Library Options

Create a "conversation" and "data-gathering-conversation" object.  Store the conversation state in the session and use it in the Speechlet for routing certain intents to the correct (just one "open") conversation.  Make every conversation a state machine and a separate session variable to indicate a stack of currently open "conversations."  Each conversation may handle a (possibly overlapping) set of Intents.  A conversation may have a default handler if the user gives Intent not in open conversation's list.

- https://freebusy.io/blog/building-conversational-alexa-apps-for-amazon-echo

### Phrase Handling

The trashday.ui.responses.Phrase class keeps most of the hard-coded text for spoken phrases and cards.  Need to look at if expanding to something with parameters makes sense.  Should be better than having multiple phrases for "add weekly", "add biweekly", "add monthly", "delete weekly", "delete biweekly", "delete monthly".

