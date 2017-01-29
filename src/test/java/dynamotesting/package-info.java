
/**
 * JUnit support to allow running tests to a local instance
 * of Dynamo DB or the Amazon Dynamo DB cloud.  See
 * {@link trashday.TrashDaySpeechletRequestStreamHandlerTest} usage of
 * {@link dynamotesting.LocalDynamoDBCreationRule} in the ClassRule.
 * 
 * @author	J. Todd Baldwin
 * @see		trashday.TrashDaySpeechletRequestStreamHandlerTest
 * @see 	<a href="http://stackoverflow.com/questions/26901613/easier-dynamodb-local-testing">Easier DynamoDB local testing</a>
 */
package dynamotesting;
