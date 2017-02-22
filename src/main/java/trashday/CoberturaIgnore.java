package trashday;

/**
 * Create a method annotation to allow us to ignore some
 * methods from our Cobertura reporting.  This has no
 * impact on the actual application code.  Only changes
 * the Cobertura test coverage reporting.
 * 
 * @author J. Todd Baldwin
 * @see 	<a href="http://stackoverflow.com/questions/8225888/ignore-methods-in-class-cobertura-maven-plugin">Stack Overflow: Ignore methods in class. cobertura maven plugin</a>
 * @see		<a href="https://github.com/cobertura/cobertura/wiki/Ant-Task-Reference#ignore-method-annotation">Ant Task Reference: Ignore Method Annotation</a>
 *
 */
public @interface CoberturaIgnore{}
