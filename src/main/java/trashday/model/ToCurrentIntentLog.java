package trashday.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.jonpeterson.jackson.module.versioning.VersionedModelConverter;

/**
 * Handles converting from old versions of the {@link IntentLog}
 * class during deserialization.  Nothing useful in here for
 * version 1 of the application.  However, if later application
 * versions update the class, this class may need to
 * perform actions when reading older version(s) of the
 * {@link IntentLog} object from the database.
 * 
 * @author	J. Todd Baldwin
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module Model Versioning</a>
 *
 */
public class ToCurrentIntentLog implements VersionedModelConverter {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(ToCurrentIntentLog.class);
    
	@Override
	public ObjectNode convert(ObjectNode modelData, String modelVersion,
            String targetModelVersion, JsonNodeFactory nodeFactory) {
		
		Integer version = new Integer(modelVersion);		
		log.trace("Deserializing IntentLog (from version={}, to version={})", version, targetModelVersion);
		
		// No conversions necessary yet.  Still on version 1.
		
		// Example for up-converting:
//      // version 1 had a single 'model' field that combined 'make' and 'model' with a colon delimiter
//      if(version <= 1) {
//          def makeAndModel = modelData.get('model').asText().split(':')
//          modelData.put('make', makeAndModel[0])
//          modelData.put('model', makeAndModel[1])
//      }
//
//      // version 1-2 had a 'new' text field instead of a boolean 'used' field
//      if(version <= 2)
//          modelData.put('used', !Boolean.parseBoolean(modelData.remove('new').asText()))
		
		return null;
	}		
}
