# Alexa Trash Day Skill

## Description
A small Alexa skill to help users remember if the trash needs to go out or not.

I can never seem to remember.  Is my trash pickup is Monday/Thursday or is it Tuesday/Friday?  And the recycling and lawn waste pickups happen on different days.  And some days I'm not sure if today is Tuesday or Wednesday anyway.  Alexa, help me!

## Skill Usage

WARNING: Ensure your Echo is setup with your correct timezone information.  In the Alexa app, go to Settings -> <Your Device> -> Device Time Zone.

Use this skill to teach Alexa your weekly pickup schedule.  Here is mine:

- "Alexa, ask Trash Day to add trash pickup on Tuesday at six a.m."

- "Alexa, ask Trash Day to add trash pickup on Friday at six a.m."

- "Alexa, ask Trash Day to add recycling pickup on Friday at six a.m."

- "Alexa, ask Trash Day to add lawn waste pickup on Wednesday at 1 p.m."


Then, whenever you aren't sure if the trash needs to go out to the curb:

- "Alexa, open Trash Day."

- "Alexa, ask Trash Day when is the next trash pickup?"

## Source Code Usage

This repository has the complete source code for the skill, along with complete javadocs and Cobertura code testing coverage report:

<dl>
  <dt>apidocs/</dt>
  <dd>Javadoc for this application</dd>

  <dt>cobertura/</dt>
  <dd>JUnit test coverage report</dd>

  <dt>pom.xml</dt>
  <dd>Maven configuration for this application</dd>

  <dt>src/</dt>
  <dd>Java source for this Alexa Custom Skill application</dd>

  <dt>test-apidocs/</dt>
  <dd>Javadoc for the JUnit tests of this application</dd>
</dl>


Project build was accomplished with the following Maven commands:

- mvn clean
- mvn test 
- mvn javadoc:javadoc 
- mvn javadoc:test-javadoc 
- mvn cobertura:cobertura 
- mvn assembly:assembly -DdescriptorId=jar-with-dependencies package


## Resources
Here are a few direct links to documentation associated with the project:

- [Using the Alexa Skills Kit Samples](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/using-the-alexa-skills-kit-samples)
- [Getting Started](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/getting-started-guide)
- [Developing an Alexa Skill as an AWS Lambda Function](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/docs/developing-an-alexa-skill-as-a-lambda-function)

