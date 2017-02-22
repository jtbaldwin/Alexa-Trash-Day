package trashday.ui.requests;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.amazon.speech.slu.Intent;

import trashday.CoberturaIgnore;

// Experimental Class, probably NOT a good idea...
@Deprecated
@CoberturaIgnore
public class SlotGroup {
	
	public List<String> slotNamesRequired = null;
	public Map<String, Object> validatedSlotData = null;
	public Map<String, Object> requiredValidationData = null;
	
	// Current group state:
	public Map<String, Slot> emptySlots = null;
	public Map<String, Slot> invalidSlots = null;
	public Map<String, Slot> dataMissing = null;

	@CoberturaIgnore
	public SlotGroup(List<String> slotNamesRequired, Map<String, Object> requiredValidationData) {
		this.slotNamesRequired = slotNamesRequired;
		this.requiredValidationData = requiredValidationData;
		
		this.validatedSlotData = new LinkedHashMap<String,Object>();
		for (String slotName : slotNamesRequired) {
			validatedSlotData.put(slotName, null);
		}
	}
	
	@CoberturaIgnore
	public boolean addSlotData(Intent intent) {
		Map<String, Slot> emptySlots = new HashMap<String, Slot>();
		Map<String, Slot> invalidSlots = new HashMap<String, Slot>();
		Map<String, Slot> dataMissing = new HashMap<String, Slot>();
		
		// Find if any of the slots we need are in this Intent.
		for (String slotName: slotNamesRequired) {
			boolean existingValidSlotData = ( validatedSlotData.containsKey(slotName) && (validatedSlotData.get(slotName) != null) );			
			switch (slotName) {
			case "PickupName":
				SlotPickupName slotPickupName = new SlotPickupName(intent);
				if (slotPickupName.getSlot()==null) {
					if (! existingValidSlotData) {
						dataMissing.put(slotName, slotPickupName);
					}
					break;
				}
				if (slotPickupName.isEmpty()) {
					emptySlots.put(slotName, slotPickupName);
					break;
				}
				String pickupName = slotPickupName.validate();
				if (pickupName == null) {
					invalidSlots.put(slotName, slotPickupName);
					break;
				}
				validatedSlotData.put(slotName, pickupName);
				break;
				
			case "NextOrThis":
				SlotNextOrThis slotNextOrThis = new SlotNextOrThis(intent);
				if (slotNextOrThis.getSlot()==null) {
					if (! existingValidSlotData) {
						dataMissing.put(slotName, slotNextOrThis);
					}
					break;
				}
				if (slotNextOrThis.isEmpty()) {
					emptySlots.put(slotName, slotNextOrThis);
					break;
				}
				Boolean isNextWeek = slotNextOrThis.validate();
				if (isNextWeek == null) {
					invalidSlots.put(slotName, slotNextOrThis);
					break;
				}
				validatedSlotData.put(slotName, isNextWeek);
				break;
				
			case "DayOfWeek":
				SlotDayOfWeek slotDayOfWeek = new SlotDayOfWeek(intent);
				if (slotDayOfWeek.getSlot()==null) {
					if (! existingValidSlotData) {
						dataMissing.put(slotName, slotDayOfWeek);
					}
					break;
				}
				if (slotDayOfWeek.isEmpty()) {
					emptySlots.put(slotName, slotDayOfWeek);
					break;
				}
				DayOfWeek dow = slotDayOfWeek.validate((LocalDateTime) requiredValidationData.get("ldtRequest"));
				if (dow == null) {
					invalidSlots.put(slotName, slotDayOfWeek);
					break;
				}
				validatedSlotData.put(slotName, dow);
				break;
				
			case "TimeOfDay":
				SlotTimeOfDay slotTimeOfDay = new SlotTimeOfDay(intent);
				if (slotTimeOfDay.getSlot()==null) {
					if (! existingValidSlotData) {
						dataMissing.put(slotName, slotTimeOfDay);
					}
					break;
				}
				if (slotTimeOfDay.isEmpty()) {
					emptySlots.put(slotName, slotTimeOfDay);
					break;
				}
				LocalTime tod = slotTimeOfDay.validate();
				if (tod == null) {
					invalidSlots.put(slotName, slotTimeOfDay);
					break;
				}
				validatedSlotData.put(slotName, tod);
				break;
				
			default:
				throw new IllegalArgumentException("Program Error: Unknown Slot Name: "+slotName);
			}
		}
		
		this.emptySlots = emptySlots;
		this.invalidSlots = invalidSlots;
		this.dataMissing = dataMissing;
		
		return isAllDataAvailable();
	}

	@CoberturaIgnore
	public boolean isAllDataAvailable() {
		if (dataMissing==null) {
			return false;
		}
		if (dataMissing.size() > 0) {
			return false;
		}
		return true;
	}
	
	@CoberturaIgnore
	public Object get(String slotName) {
		return validatedSlotData.get(slotName);
	}
	
	@CoberturaIgnore
	public String getMisunderstoodSlotSpeech() {
		if ((emptySlots==null) || (invalidSlots==null)) {
			throw new IllegalArgumentException("Must call addSlotData method before calling getMisunderstoodSlotSpeech");
		}
		if ( emptySlots.isEmpty() && invalidSlots.isEmpty() ) {
			return null;
		}
		
		List<String> invalidDataDescriptions = new ArrayList<String>();
		for (String slotName: slotNamesRequired) {
			if (emptySlots.containsKey(slotName)) {
				Slot slot = emptySlots.get(slotName);
				invalidDataDescriptions.add(slot.getDescription());
				continue;
			}
			if (invalidSlots.containsKey(slotName)) {
				Slot slot = invalidSlots.get(slotName);
				invalidDataDescriptions.add(slot.getDescription());
				continue;
			}
		}
		
		StringBuffer sb = new StringBuffer("I didn't understand the ");
		if (invalidDataDescriptions.size() == 1) {
			sb.append(invalidDataDescriptions.get(0));
		} else if (invalidDataDescriptions.size() == 2) {
			sb.append(invalidDataDescriptions.get(0));
			sb.append(" and ");
			sb.append(invalidDataDescriptions.get(1));
		} else {
			int limit = invalidDataDescriptions.size() - 1;
			for (int i=0; (i < limit); i++) {
				sb.append(invalidDataDescriptions.get(i));
				sb.append(", ");
			}
			sb.append("and ");
			sb.append(invalidDataDescriptions.get(limit));
		}
		sb.append(" information.");
		
		return sb.toString();
	}
	
	@CoberturaIgnore
	public String getMissingDataSpeech() {
		if (dataMissing==null) {
			throw new IllegalArgumentException("Must call addSlotData method before calling getMissingDataSpeech");
		}
		if (dataMissing.isEmpty()) {
			return null;
		}
		
		List<String> missingDataDescriptions = new ArrayList<String>();
		for (String slotName: slotNamesRequired) {
			if (dataMissing.containsKey(slotName)) {
				Slot slot = dataMissing.get(slotName);
				missingDataDescriptions.add(slot.getDescription());
				continue;
			}
		}
		
		StringBuffer sb = new StringBuffer("I still need the ");
		if (missingDataDescriptions.size() == 1) {
			sb.append(missingDataDescriptions.get(0));
		} else if (missingDataDescriptions.size() == 2) {
			sb.append(missingDataDescriptions.get(0));
			sb.append(" and ");
			sb.append(missingDataDescriptions.get(1));
		} else {
			int limit = missingDataDescriptions.size() - 1;
			for (int i=0; (i < limit); i++) {
				sb.append(missingDataDescriptions.get(i));
				sb.append(", ");
			}
			sb.append("and ");
			sb.append(missingDataDescriptions.get(limit));
		}
		sb.append(" information.");
		
		return sb.toString();
	}

}


//public SpeechletResponse handleAddBiWeeklyPickupRequest(IntentRequest request, Session session) {
//	log.info("handleAddNiWeeklyPickupRequest(intentName={}, sessionId={})", request.getIntent().getName(), session.getSessionId());
//	sessionDao = new SessionDao(session);
//	SpeechletResponse configNeeded = isTimeZoneConfigurationComplete(request.getTimestamp());
//	if (configNeeded != null) { return configNeeded; };
//	LocalDateTime ldtRequest = DateTimeOutputUtils.getRequestLocalDateTime(request, timeZone);
//	
//	Map<String, Object> requiredValidationData = new HashMap<String, Object>();
//	requiredValidationData.put("ldtRequest", ldtRequest);
//	SlotGroup slotGroup = new SlotGroup(
//					Arrays.asList("PickupName", "NextOrThis", "DayOfWeek", "TimeOfDay"),
//					requiredValidationData
//					);
//	boolean groupComplete = slotGroup.addSlotData(request.getIntent());
//	
//	if (! groupComplete) {
//		// Any misunderstood information in this latest intent from user?
//		String misunderstood = slotGroup.getMisunderstoodSlotSpeech();
//		if (misunderstood!=null) {
//        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddBiWeeklyInvalidData");
//    		SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklyInvalidData(sessionDao, true, misunderstood);
//    		if (response.getShouldEndSession()) { flushIntentLog(); }
//    		return response;
//		}
//		// Any data still required to complete this slot data group?
//		String missing = slotGroup.getMissingDataSpeech();
//		if (missing!=null) {
//        	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddBiWeeklyMissingData");
//    		SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklyMissingData(sessionDao, true, missing);
//    		if (response.getShouldEndSession()) { flushIntentLog(); }
//    		return response;
//		}
//	}
//	
//	// Got all three required data now.  Let's perform our intent.
//	if (calendar==null) {
//		calendar = new Calendar(timeZone);
//	}
//	calendar.addBiWeeklyEvent(
//			(String) slotGroup.get("pickupName"), 
//			(Boolean) slotGroup.get("nextWeekStart"), 
//			(DayOfWeek) slotGroup.get("dow"), 
//			(LocalTime) slotGroup.get("tod")
//			);
//	sessionDao.setCalendar(calendar);
//	dynamoDao.writeUserData(sessionDao);
//	
//	sessionDao.incrementIntentLog(ldtRequest, "respondPickupAddBiWeeklySingle");
//    SpeechletResponse response = ResponsesSchedule.respondPickupAddBiWeeklySingle(sessionDao, 
//    		false,
//    		(String) slotGroup.get("pickupName"), 
//    		(Boolean) slotGroup.get("nextWeekStart"), 
//    		(DayOfWeek) slotGroup.get("dow"), 
//    		(LocalTime) slotGroup.get("tod")
//    		);
//	if (response.getShouldEndSession()) { flushIntentLog(); }
//	return response;
//}


