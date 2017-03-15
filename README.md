# Alexa Trash Day Skill

## Important Links
[Trash Day Skill](https://www.amazon.com/dp/B01MRAXTN8)
[Trash Day Help](https://s3.amazonaws.com/trash-day-docs/Help.html)

[Source Code](https://github.com/jtbaldwin/Alexa-Trash-Day)
[Javadoc](https://jtbaldwin.github.io/Alexa-Trash-Day/apidocs/)
[Cobertura Test Coverage Report](https://jtbaldwin.github.io/Alexa-Trash-Day/cobertura/)

## Skill Description
A small Alexa skill to help users remember if the trash needs to go out or not.

I can never seem to remember.  Is my trash pickup is Monday/Thursday or is it Tuesday/Friday?  And the recycling and lawn waste pickups happen on different days.  And some days I'm not sure if today is Tuesday or Wednesday anyway.  Alexa, help me!

## Skill Usage

To use the Trash Day skill, you need to enable the skill for your Alexa
device(s).  See instructions at the [Trash Day Skill](https://www.amazon.com/dp/B01MRAXTN8) page.

### Skill Usage: Setup

Once you have the skill enabled, you have to tell the application your time zone and make a pickup schedule.  This is the most difficult part of using the Trash Day application.  But you only have to do it for initial setup or when your pickup schedule changes.  As an example, here's how I setup to use the application in my house:

- Alexa, open Trash Day
- Set time zone to Eastern.
- Add trash pickup on Tuesday at seven-thirty am.
- Add trash pickup on Friday at seven-thirty am.
- Add recycling pickup on Friday at seven-thirty am.
- Add lawn waste pickup on Wednesday at one p.m.
- Tell me the schedule.
- Stop.

<B>NOTE:</B>The Trash Day application keeps listening (the blue ring is lit on the Echo) while you give commands.  If you mess up or get delayed, the blue ring will turn off. If that happens, just say "Alexa, open Trash Day" and continue where you left off with your setup.

### Skill Usage: Normal Usage 	

Once you have setup your time zone and pickup schedule, using Trash Day is easy.  Here are the common commands:

- Alexa, ask Trash Day, what's next?
- Alexa, ask Trash Day, is it trash day?
- Alexa, ask Trash Day, when is the next recycling pickup?
- Alexa, ask Trash Day, what is the schedule?

### Skill Usage: Schedule Updates

Any time you need to make changes to your pickup schedule, use commands like the following to set your time zone, add new weekly pickups, and delete old pickups.

- Alexa, open Trash Day.
- Set time zone to Pacific.
- Add recycling pickup on Friday at seven-thirty am.
- Add recycling pickup on Wednesday at one p.m.
- Delete all lawn waste pickups.
- Tell me the schedule.
- Stop.

If you need to make more extensive changes, you may want to delete the entire
schedule. To do this:

- Alexa, open Trash Day.
- Delete the entire schedule.
- Yes

You will need to answer "Yes" to confirm the schedule deletion.  Once the schedule
is deleted, rebuild a new schedule by using the commands given in the Setup section 
at the top of this page.

## Source Code Usage

This repository has the complete source code for the skill, along with complete Javadoc and Cobertura code testing coverage report:

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


## Further Resources
Here are a few direct links to documentation associated with the project:

- The [Trash Day Skill](https://www.amazon.com/dp/B01MRAXTN8)
- [Trash Day Docs: Help](https://s3.amazonaws.com/trash-day-docs/Help.html)
- [Trash Day Docs: Privacy Policy](https://s3.amazonaws.com/trash-day-docs/Privacy.html)
- [Trash Day Docs: Terms of Service](https://s3.amazonaws.com/trash-day-docs/TermsOfService.html)
- [Trash Day Docs: Time Zone Names](https://s3.amazonaws.com/trash-day-docs/TimeZoneNames.html)

- [Using the Alexa Skills Kit Samples](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/using-the-alexa-skills-kit-samples)
- [Getting Started](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/getting-started-guide)
- [Developing an Alexa Skill as an AWS Lambda Function](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/docs/developing-an-alexa-skill-as-a-lambda-function)

