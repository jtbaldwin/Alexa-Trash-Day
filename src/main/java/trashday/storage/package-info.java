
/**
 * Provides the classes necessary to store user information in the Alexa
 * session and Amazon Dynamo DB.  Uses Alexa session to manage
 * user dialog and cache user data from Dynamo DB.  Relies 
 * on Amazon's Dynamo DB for storing user data and on FasterXML's
 * Jackson Project for JSON-based serialization/deserialization
 * of the {@link trashday.model.Schedule Schedule} to/from the database.
 * 
 * @author	J. Todd Baldwin
 * @see		<a href="https://aws.amazon.com/documentation/dynamodb/">Amazon DynamoDB Documentation</a>
 * @see		<a href="http://docs.aws.amazon.com/amazondynamodb/latest/developerguide/DynamoDBMapper.html">Java: DynamoDBMapper</a>
 * @see		<a href="https://github.com/FasterXML/jackson">FasterXML Jackson Project Home</a>
 * @see		<a href="https://github.com/jonpeterson/jackson-module-model-versioning">Jackson Module: Model Versioning</a>
 */
package trashday.storage;
