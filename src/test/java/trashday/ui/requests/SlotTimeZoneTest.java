package trashday.ui.requests;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;

public class SlotTimeZoneTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotTimeZoneTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotTimeZone testSlotTimeZone;
    /** A list of all time zone names understood by Java. */
	private static TreeSet<String> sortedJavaTimeZoneIDs;
	/** A map translating Java time zone names to zone names in "Alexa-speak." */
	private static Map<String, List<String>> alexaSpeakTimeZoneIDs;
	
	private static String htmlDocHeader = "<!DOCTYPE html>\n" + 
			"<html>\n" + 
			"<head>\n" + 
			"<meta charset=\"UTF-8\">\n" + 
			"<title>Time Zone Names</title>\n" + 
			"</head>\n" + 
			"<body>\n" + 
			"\n"+
			"<h3>How to set Time Zone for Trash Day Skill</h3>\n" + 
			"\n" + 
			"<p>Use the following list of time zone names to tell Trash Day your time zone.  For example:\n" + 
			"<p>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Alexa, tell Trash Day, to set time zone to &lt;Time_Zone_Name&gt;\n" + 
			"<p>For the continental United States, there are four easily-spoken time zone names (Eastern, \n" +
			"Central, Mountain, and Pacific).  However, the Trash Day skill understands approximately six hundred \n"+
			"other time zone names.  This complete list is given in the table at the end of this page.\n"+
			"\n" +
			"<h3>\"Alexa-Speak\"</h3>\n"+
			"<p>Most of the time zone names include numbers, special characters (\"/\", \"+\", \"-\", \"_\"), "+
			"common abbreviations (\"Etc\", \"St\", \"US\"), and computing-specific terms (\"SystemV\").  To "+
			"say these time zone names to Alexa, you will need to state them so that Alexa "+
			"can understand them.  I call this \"Alexa-Speak\" and the translation for every time zone "+
			"is given in the table as well.\n"+
			"<p>NOTE: I apologize if your time zone is one of the \"unpronouncable\" city names.  Alexa "+
			"does not do well with these as regular translations and I have not yet managed to integrate the "+
			"Trash Day skill with Amazon's city name database for Alexa input.  If you have a case where your "+
			"time zone name is just not understood clearly by Alexa, please use one of the \"Etc/GMT\" time "+
			"zone names in the list.\n"+
			"\n" + 
			"<h3>Why does Trash Day need my time zone?</h3>\n" + 
			"\n" + 
			"<p>Amazon treats a user's time zone information as \"Personally Identifiable Information.\" " + 
			"Therefore, they do not make that information available to 3rd-party custom skills " + 
			"like Trash Day.  So when Alexa sends your request to Trash Day, the request time stamp " + 
			"is in GMT time zone.\n" + 
			"\n" + 
			"<p>This is a problem because, for example,<ul>\n" + 
			"<li>you live in the US/Pacific time zone (eight hours earlier than GMT)\n" + 
			"<li>you have trash pickups on Monday &amp; Thursday at 8 AM\n" + 
			"<li>and, on Sunday 7pm, you say \"Alexa, ask Trash Day, when is my next trash pickup\"\n" + 
			"</li>\n" + 
			"</ul>\n" + 
			"<p>In this case, you would expect Trash Day to tell you \"Next trash pickup is tomorrow at 8am\" " + 
			"but the request Trash Day gets is in GMT.  So Sunday 7pm Pacific time comes to Trash Day " + 
			"as <b>Monday</b> 3am GMT.  If Trash Day relied on GMT, it would think it is " + 
			"already Monday and would tell you \"Next trash pickup is <b>today</b> at 8am.\"  Telling " + 
			"you at 7pm that your trash pickup was this morning at 8am when you're asking " + 
			"for the <b>next</b> trash pickup is very wrong.\n" + 
			"\n" + 
			"<p>I hope Amazon will eventually find a better way to provide time zone " + 
			"information to 3rd-party skills.  Even just something that required a user to allow access to "+
			"the time zone set in the Alexa app or at least a common time zone database like they provide "+
			"for city names would help.  However, until they change their policy, " + 
			"Trash Day gathers this information from every user and stores it with their " + 
			"schedule.\n" + 
			"\n" + 
			"<h3>Full Time Zone List</h3>\n"+
			"<p>NOTE: Abbreviations like \"ACT\" need to be stated as the individual letters.  So, \"A.C.T.\" "+
			"is stated as \"ay cee tee\" to Alexa, but I've just shown \"A.C.T.\" in the table for clarity.\n"+
			"\n";

	private static Map<String, List<String>> createAlexaSpeakTimeZoneIDs(Set<String> javaZoneIDs) {		
		// For each zone, let's find an Alexa translation
		Map<String, List<String>> alexaSpeakTimeZones = new TreeMap<String, List<String>>();
        for (String javaZoneID : javaZoneIDs) {
        	List<String> zonesInAlexaSpeak = SlotTimeZone.translateToAlexaSpeak(javaZoneID);
            alexaSpeakTimeZones.put(javaZoneID, zonesInAlexaSpeak);
        }
		return alexaSpeakTimeZones;
	}
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		// What are the time zones Java knows?
        String[] zoneIDs = TimeZone.getAvailableIDs();
        sortedJavaTimeZoneIDs = new TreeSet<String>();
        for (String javaZoneID : zoneIDs) {
        	sortedJavaTimeZoneIDs.add(javaZoneID);
        }
        // What are the translations of those in terms of what Alexa "hears"?
        alexaSpeakTimeZoneIDs=createAlexaSpeakTimeZoneIDs(sortedJavaTimeZoneIDs);
        
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("TimeZone")
				.withValue("mountain")
				.build();
		testSlots.put("TimeZone", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotTimeZone = new SlotTimeZone(testIntent); 
		log.info("setUpBeforeClass: testSlotTimeZone={}", testSlotTimeZone);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotTimeZone.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "TimeZone";
		String actualName = testSlotTimeZone.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Time Zone";
		String actualDescription = testSlotTimeZone.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotTimeZone.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
	}

	@Test
	public void testValidate() {
		
		// Validate for a "good" Slot in SlotTimeZone
		TimeZone expectedTimeZone = TimeZone.getTimeZone("US/Central");
		testSlots.clear();
		testSlots.put("TimeZone", Slot.builder().withName("TimeZone").withValue("central").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotTimeZone slotTimeZone = new SlotTimeZone(intent); 
		TimeZone actualTimeZone = slotTimeZone.validate();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// Validate for a "good" Slot in SlotTimeZone
		expectedTimeZone = TimeZone.getTimeZone("US/Mountain");
		testSlots.clear();
		testSlots.put("TimeZone", Slot.builder().withName("TimeZone").withValue("mountain").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeZone = new SlotTimeZone(intent); 
		actualTimeZone = slotTimeZone.validate();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// Validate for a null Slot in SlotTimeZone
		expectedTimeZone = null;
		testSlots.clear();
		testSlots.put("TimeZone", Slot.builder().withName("TimeZone").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeZone = new SlotTimeZone(intent);
		actualTimeZone = slotTimeZone.validate();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// Validate for a empty (whitespace only) Slot in SlotTimeZone
		expectedTimeZone = null;
		testSlots.clear();
		testSlots.put("TimeZone", Slot.builder().withName("TimeZone").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeZone = new SlotTimeZone(intent);
		actualTimeZone = slotTimeZone.validate();
		assertEquals(expectedTimeZone, actualTimeZone);
		
		// Validate for an unknown time zone name Slot in SlotTimeZone
		expectedTimeZone = null;
		testSlots.clear();
		testSlots.put("TimeZone", Slot.builder().withName("TimeZone").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeZone = new SlotTimeZone(intent);
		actualTimeZone = slotTimeZone.validate();
		assertEquals(expectedTimeZone, actualTimeZone);
	}
	
	@Test
	public void translateFromAlexaSpeakTest() {
		for (Map.Entry<String, List<String>> entry : alexaSpeakTimeZoneIDs.entrySet()) {
			String javaZoneName = entry.getKey();
			List<String> translations = entry.getValue();
			for (String translatedToAlexaSpeak : translations) {
				log.info("Translated java time zone \"{}\" to \"{}\"", javaZoneName, translatedToAlexaSpeak);
				
				String translatedFromAlexaSpeak = SlotTimeZone.translateFromAlexaSpeak(translatedToAlexaSpeak);
				log.info("Translated from Alexa-speak time zone \"{}\" to java time zone \"{}\"", translatedToAlexaSpeak, translatedFromAlexaSpeak);
				
				if (javaZoneName.equals("GMT") || javaZoneName.equals("GMT0")) {
					assertEquals(javaZoneName, translatedFromAlexaSpeak.replaceFirst("Etc/", ""));
				} else {
					assertEquals(javaZoneName, translatedFromAlexaSpeak);
				}
			}
		}
	}

	@Test
	public void createTimeZoneSlotData() {
		try {
			File fileTimeZone = File.createTempFile("TIME_ZONE", ".txt");
			FileWriter fileWriter = new FileWriter(fileTimeZone, true);
		    BufferedWriter bw = new BufferedWriter(fileWriter);
		    bw.write("other\n");
		    
		    Set<String> translations = new TreeSet<String>();
			for (Map.Entry<String, List<String>> entry : alexaSpeakTimeZoneIDs.entrySet()) {
				List<String> alexaTranslations = entry.getValue();
				for (String alexaTranslation : alexaTranslations) {
					translations.add(alexaTranslation);
				}
			}
			for (String translation: translations) {
				bw.write(translation+"\n");				
			}
	        bw.close();
	        
	        log.info("Created TIME_ZONE slot data file: {}", fileTimeZone.getPath());
	        System.out.println("Created TIME_ZONE slot data file: "+fileTimeZone.getPath());
		} catch (IOException e) {
			fail("Cannot create temp file: "+e.getMessage());
		}
	}

	@Test
	public void createTimeZoneHelpDoc() {
		try {
			File fileTimeZone = File.createTempFile("TIME_ZONE", ".html");
			FileWriter fileWriter = new FileWriter(fileTimeZone, true);
		    BufferedWriter bw = new BufferedWriter(fileWriter);
		    
		    bw.write(htmlDocHeader);
		    bw.write("<table border=\"1\"><tr bgcolor=\"lightblue\"><th>Time Zone Name</th><th>&quot;Alexa-Speak&quot;</th></tr>\n");
			for (Map.Entry<String, List<String>> entry : alexaSpeakTimeZoneIDs.entrySet()) {
				String javaZoneName = entry.getKey();
				List<String> alexaTranslations = entry.getValue();
				bw.write("<tr><td>"+javaZoneName+"</td><td>");
				bw.write(String.join(" <i>or</i> ", alexaTranslations));
//				for (String alexaTranslation: alexaTranslations) {
//					bw.write("<tr><td>"+javaZoneName+"</td><td>"+alexaTranslation+"</td></tr>\n");
//				}
				bw.write("</td></tr>\n");
			}
	        bw.write("</table>\n");
	        bw.write("</body></html>\n");
	        bw.close();
	        
	        log.info("Created Time Zone Html file: {}", fileTimeZone.getPath());
	        System.out.println("Created Time Zone Html file: "+fileTimeZone.getPath());
		} catch (IOException e) {
			fail("Cannot create temp file: "+e.getMessage());
		}
	}
}
