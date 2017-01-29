
/**
 * Implements the {@link trashday.model.Schedule} object that stores the user's
 * weekly pickup schedule.  Also provides classes to enable
 * version-controlled deserialization to allow for update of the
 * Schedules stored in Dynamo DB in the event that a later
 * application update requires an update to {@link trashday.model.Schedule}
 * structure.
 * 
 * @author	J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 */
package trashday.model;
