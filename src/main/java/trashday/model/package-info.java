
/**
 * Implements the {@link trashday.model.Calendar} object that stores the user's
 * regular pickup schedule.  Also provides classes to enable
 * version-controlled de-serialization to allow for update of the
 * Schedules stored in Dynamo DB in the event that a later
 * application update requires an update to the model
 * structure.
 * <p>
 * It's important to note that the model is built to work with Java 8's new
 * Date and Time classes.  Specifically, the data storage of user pickup
 * schedule (both {@link Schedule} and {@link Calendar}) information is 
 * stored *without* embedded time zone information.  This is
 * useful in an Alexa Skills environment as the application does not
 * run the user's timezone.  All skills run in UTC timezone.
 * 
 * @author	J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 * @see     <a href="http://www.oracle.com/technetwork/articles/java/jf14-date-time-2125367.html">Java SE 8 Date and Time</a>
 */
package trashday.model;
