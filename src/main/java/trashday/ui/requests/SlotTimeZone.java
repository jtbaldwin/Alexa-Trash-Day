package trashday.ui.requests;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;

/**
 * Handle Intent Slot for Time Zone information from user.
 * 
 * @author J. Todd Baldwin
 *
 */
public class SlotTimeZone implements Slot {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotTimeZone.class);
    /** {@link com.amazon.speech.slu.Slot} from the Alexa request. */
	private com.amazon.speech.slu.Slot slot;
	/** The slot field name corresponding to the VUI IntentSchema / Sample Utterances */
	public static final String name = "TimeZone";
    /** A printable description of this slot's purpose */
    public static final String description = "Time Zone";
    /** Regex pattern used for removing dots from abbreviations like: U.S. and E.D.T. */
    private static final Pattern p = Pattern.compile("([A-Z].)+");

	
	/**
	 * Handle parsing of a request slot intended to store Time Zone information
	 * in user requests.
	 * 
	 * @param intent Intent from Alexa request
	 */
	public SlotTimeZone(Intent intent) {
		this.slot = intent.getSlot(name);
	}
	
	/**
	 * Getter for the actual {@link com.amazon.speech.slu.Slot} from the Alexa request.
	 * 
	 * @return Alexa request {@link com.amazon.speech.slu.Slot}
	 * @see trashday.ui.requests.Slot#getSlot()
	 */
	@Override
	public com.amazon.speech.slu.Slot getSlot() {
		return this.slot;
	}
	
	/**
	 * Getter for slot name that is used in VUI Intent Schema / Sample Utterances.
	 * 
	 * @return The name of this {@link com.amazon.speech.slu.Slot} that is
	 * 		used in the VUI IntentSchema / Sample Utterances
	 * @see trashday.ui.requests.Slot#getName()
	 */
	@Override
	public String getName() {
		return SlotTimeZone.name;
	}

	/**
	 * Getter for slot description that tells the use of this slot in readable form.
	 * 
	 * @return The description of this {@link com.amazon.speech.slu.Slot} that is
	 * 		used in the VUI IntentSchema / Sample Utterances
	 * @see trashday.ui.requests.Slot#getDescription()
	 */
	@Override
	public String getDescription() {
		return SlotTimeZone.description;
	}

	/**
	 * Check if the data in the {@link com.amazon.speech.slu.Slot} is null,
	 * empty, or just whitespace.
	 * 
	 * @return true if no usable data in the slot
	 * @see trashday.ui.requests.Slot#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
    	log.trace("isEmpty({})", slot);
    	if (slot==null) {
    		return true;
    	}
    	String s = slot.getValue();
    	if (s==null) {
    		return true;
    	}
    	if (s.trim().length() < 1) {
    		return true;
    	}
    	return false;
	}
	
	/**
	 * From a given time zone name, generates a textual representation suitable
	 * for someone to say to Alexa.  Called only from {@link #translateToAlexaSpeak(String)}.
	 * Does not create multiple options for a given time zone ("US/Eastern" can be
	 * understood as "Eastern" and "U.S. slash Eastern").  Instead only translates
	 * a given time zone into something suitable to be spoken (eg. "US/Eastern" to
	 * "U.S. slash eastern").
	 * 
	 * @param zoneName String to be translated
	 * @return String that represents the speech a user could give to Alexa for the
	 * 		given zoneName.
	 */
	public static String translateToAlexaSpeakSingleForm(String zoneName) {
		log.trace("translateSingleToAlexaSpeak(zoneName={})", zoneName);
		
    	// Split some zone words into multiples so we can translate them easier
    	String zoneInAlexa = zoneName
    			.replaceAll("\\b([A-Z]+)(\\d+)\\b", "$1 $2 ")  // If zone name has "GMT0", split it up into separate words
    			.replaceAll("\\b([A-Z]+)(\\d+)(?=\\S+)", "$1 $2 ")  // If zone name has "CST6CDT", split it up into separate words
    			.replaceAll("_", " ") // If zone name has "_", split it into separate words.
    			.replaceAll("([^\\s/]+)/(?=[^\\s/]+)", "$1 slash ") // If "America/Argentina/Buenos Aires" => "America slash Argentina slash Buenos Aires"
    			.replaceAll("\\+", " plus ")
    			.replaceAll("-\\s*(\\d+)", " minus $1")
    			.replaceAll("-", " dash ")
    			;

    	// Do some per-word translations...
		String[] words = zoneInAlexa.split("\\s+");
		List<String> wordList = new ArrayList<String>();
		for (int w=0; w < words.length ; w++) {
			String word = words[w];
			
			// If word is all-caps, tell it to Alexa as A.L.T.R.
			if (word.matches("\\b[A-Z]+\\b")) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0;i < word.length(); i++){
				    sb.append(word.charAt(i)+".");
				}
				word=sb.toString();
			}
			
			if (word.equals("SystemV")) {	word = "System Five"; }
    		if (word.equals("Etc")) {		word = "Etcetera"; }
    		if (word.equals("St")) {		word = "Saint"; }
    		if (word.equals("14")) {		word = "fourteen"; }
    		if (word.equals("13")) {		word = "thirteen"; }
    		if (word.equals("12")) {		word = "twelve"; }
    		if (word.equals("11")) {		word = "eleven"; }
    		if (word.equals("10")) {		word = "ten"; }
    		if (word.equals("9")) {			word = "nine"; }
    		if (word.equals("8")) {			word = "eight"; }
    		if (word.equals("7")) {			word = "seven"; }
    		if (word.equals("6")) {			word = "six"; }
    		if (word.equals("5")) {			word = "five"; }
    		if (word.equals("4")) {			word = "four"; }
    		if (word.equals("3")) {			word = "three"; }
    		if (word.equals("2")) {			word = "two"; }
    		if (word.equals("1")) {			word = "one"; }
    		if (word.equals("0")) {			word = "zero"; }
			
			wordList.add(word);
		}
    	zoneInAlexa = String.join(" ", wordList);
    	
		return zoneInAlexa;
	}
	
	/**
	 * Translate a single java time zone ID to a textual representation suitable
	 * for someone to say to Alexa.  First use is to generate valid values for the
	 * Trash Day skill's custom TIME_ZONE slot.  Second use is to generate help
	 * documentation.  This function is called from JUnit tests in trashday.ui.requests.SlotTimeZoneTest.
	 * 
	 * @param javaTimeZoneName String java time zone id returned from {@link java.util.TimeZone#getAvailableIDs}
	 * @return List of Strings that show how to say this time zone name to Alexa.  For example
	 * 		java time zone name "US/Eastern" can be said to Alexa 
	 * 		as "eastern" or "U.S. slash Eastern" and the Trash Day Skill will be
	 * 		able to set the correct java time zone.
	 */
	public static List<String> translateToAlexaSpeak(String javaTimeZoneName) {
		log.trace("translateToAlexaSpeak(javaTimeZoneName={})", javaTimeZoneName);
		List<String> alexaTexts = new ArrayList<String>();
		List<String> zoneNames = new ArrayList<String>();
		
		// For some zone names, we accept it in multiple forms.  So, we make
		// multiple translations for some java time zone names.
		if (javaTimeZoneName.startsWith("Etc/GMT")) {
			zoneNames.add(javaTimeZoneName.substring(4, javaTimeZoneName.length()));
			zoneNames.add(javaTimeZoneName);
		}
		else if (javaTimeZoneName.equals("US/Eastern")) {
			zoneNames.add("Eastern");
			zoneNames.add(javaTimeZoneName);
		}
		else if (javaTimeZoneName.equals("US/Central")) {
			zoneNames.add("Central");
			zoneNames.add(javaTimeZoneName);
		}
		else if (javaTimeZoneName.equals("US/Mountain")) {
			zoneNames.add("Mountain");
			zoneNames.add(javaTimeZoneName);
		}
		else if (javaTimeZoneName.equals("US/Pacific")) {
			zoneNames.add("Pacific");
			zoneNames.add(javaTimeZoneName);
		}
		else {
			zoneNames.add(javaTimeZoneName);
		}
		
		for (String zoneName: zoneNames) {
			// For each java zone name we understand, translate it to remove
			// things that are hard to say verbally to Alexa.
			alexaTexts.add(translateToAlexaSpeakSingleForm(zoneName));
		}
    	
    	return alexaTexts;
	}
	
	/**
	 * Since time zone information contains abbreviations, special characters,
	 * numbers and computer-specific terms, we use this function to accept speech
	 * from Alexa and try to convert it to a good java time zone name.  For example,
	 * a user may say "set time zone to US/Eastern."  Alexa then hears that as
	 * "set time zone to U.S. slash eastern".  This function attempts to translate
	 * all time zone data from the user into the names expected by java time zone
	 * IDs.  So this translateFromAlexaSpeak("U.S. slash eastern") should give
	 * the result "US/Eastern"
	 * 
	 * @param timeZoneText String of text heard by Alexa as the time zone.  For example, "U.S. slash eastern"
	 * @return String that we hope will match to a good java Time Zone ID.
	 */
	public static String translateFromAlexaSpeak(String timeZoneText) {
		log.info("translateFromAlexaSpeak(timeZoneText={})", timeZoneText);
		
		String javaTimeZoneName = timeZoneText.trim();
		if (javaTimeZoneName.equalsIgnoreCase("eastern")) {
			return "US/Eastern";
		}
		if (javaTimeZoneName.equalsIgnoreCase("central")) {
			return "US/Central";
		}
		if (javaTimeZoneName.equalsIgnoreCase("mountain")) {
			return "US/Mountain";
		}
		if (javaTimeZoneName.equalsIgnoreCase("pacific")) {
    		return "US/Pacific";
		}
		if (javaTimeZoneName.matches(".*([A-Z].)+.*")) {
			// Matches an abbreviation like U.S.
			Matcher matcher = p.matcher(javaTimeZoneName);
			StringBuffer stringBuffer = new StringBuffer();
	        while(matcher.find()){
	            matcher.appendReplacement(stringBuffer, matcher.group().replaceAll("\\.", ""));
	        }
	        matcher.appendTail(stringBuffer);
	        javaTimeZoneName = stringBuffer.toString();
		}
		if (javaTimeZoneName.matches("^[Gg][Mm][Tt].*")) {
			javaTimeZoneName="Etc/"+javaTimeZoneName;
		}
		javaTimeZoneName = javaTimeZoneName
				.replaceFirst("^[Ee]tcetera slash ", "Etc/")
				.replaceFirst("^et cetera slash ", "Etc/")
				.replaceFirst("^etc. slash ", "Etc/")
				.replaceFirst("^[Ss]ystem [fF]ive slash ", "SystemV/")
				.replaceFirst("^system 5 slash ", "SystemV/")
    			.replaceFirst("\\.\\$", "")
    			.replaceAll(" slash ", "/")
    			.replaceAll(" dash ", "-")
    			.replaceAll(" minus ", "-")
    			.replaceAll(" plus ", "+")
    			.replaceAll("\\bfourteen\\b", "14")
    			.replaceAll("\\bthirteen\\b", "13")
    			.replaceAll("\\btwelve\\b", "12")
    			.replaceAll("\\beleven\\b", "11")
    			.replaceAll("\\bten\\b", "10")
    			.replaceAll("\\bnine\\b", "9")
    			.replaceAll("\\beight\\b", "8")
    			.replaceAll("\\bseven\\b", "7")
    			.replaceAll("\\bsix\\b", "6")
    			.replaceAll("\\bfive\\b", "5")
    			.replaceAll("\\bfour\\b", "4")
    			.replaceAll("\\bthree\\b", "3")
    			.replaceAll("\\btwo\\b", "2")
    			.replaceAll("\\bone\\b", "1")
    			.replaceAll("\\bzero\\b", "0")
    			.replaceAll("\\s+(\\d+)\\s*", "$1")
    			.replaceAll("\\bsaint ", "St ")
    			.replaceAll("\\bSaint ", "St ")
    			.replaceFirst(" lash ", "/")
    			.replaceAll("[Gg][Mm][Tt]\\s+", "GMT")
    			.replaceAll(" ", "_")
				;
		return javaTimeZoneName;
	}
	
	public boolean isOther() {
    	log.trace("isOther()", slot);

    	// Did we get a good time zone value?
    	if (isEmpty()) {
        	return false;
    	}
    	String slotValue = slot.getValue().trim().toLowerCase();
    	log.debug("isOther({})", slotValue);
    	return "other".equals(slotValue);
	}

	/**
	 * Validate that the user data given in the {@link com.amazon.speech.slu.Slot}
	 * is useful data that can be instantiated as expected to the correct
	 * java object.  User data must be in form acceptable to 
	 * {@link java.util.TimeZone#getAvailableIDs}.
	 * 
	 * @return TimeZone corresponding to the text spoken by the user
	 */
	public TimeZone validate() {
    	log.trace("validate({})", slot);

    	// Did we get a good time zone value?
    	if (isEmpty()) {
        	log.debug("validate Slot is empty");
    		return null;
    	}
    	log.debug("validate time zone slot value={}", slot.getValue());
    	String timeZoneString = translateFromAlexaSpeak(slot.getValue());
    	log.debug("validate time zone text translated from Alexa={}", timeZoneString);
    	TimeZone timeZone = null;
		
    	boolean found = false;
    	String[] availableIDs = TimeZone.getAvailableIDs();
    	for (String timeZoneName : availableIDs) {
    		if (timeZoneName.equalsIgnoreCase(timeZoneString)) {
    			timeZone = TimeZone.getTimeZone(timeZoneName);
    			found = true;
    			break;
    		}
    	}
    	if (! found) {
	    	log.error("validate Time Zone name not matched {}",timeZoneString);
    	}
    	
    	log.debug("validate timeZone={}", timeZone);
    	return timeZone;
	}

}
