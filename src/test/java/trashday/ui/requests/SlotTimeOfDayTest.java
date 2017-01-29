package trashday.ui.requests;

import static org.junit.Assert.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;

public class SlotTimeOfDayTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotTimeOfDayTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotTimeOfDay testSlotTimeOfDay;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("TimeOfDay")
				.withValue("14:50")
				.build();
		testSlots.put("TimeOfDay", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotTimeOfDay = new SlotTimeOfDay(testIntent); 
		log.info("setUpBeforeClass: testSlotTimeOfDay={}", testSlotTimeOfDay);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotTimeOfDay.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "TimeOfDay";
		String actualName = testSlotTimeOfDay.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Time Of Day";
		String actualDescription = testSlotTimeOfDay.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotTimeOfDay.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
	}

	@Test
	public void testValidate() {
		
		// Validate for a "good" Slot in SlotTimeOfDay
		LocalTime expectedTimeOfDay = LocalTime.of(14, 50);
		testSlots.clear();
		testSlots.put("TimeOfDay", Slot.builder().withName("TimeOfDay").withValue("14:50").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotTimeOfDay slotTimeOfDay = new SlotTimeOfDay(intent); 
		LocalTime actualTimeOfDay = slotTimeOfDay.validate();
		assertEquals(expectedTimeOfDay, actualTimeOfDay);
		
		// Validate for a "good" Slot in SlotTimeOfDay
		expectedTimeOfDay = LocalTime.of(7, 30);
		testSlots.clear();
		testSlots.put("TimeOfDay", Slot.builder().withName("TimeOfDay").withValue("07:30").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeOfDay = new SlotTimeOfDay(intent); 
		actualTimeOfDay = slotTimeOfDay.validate();
		assertEquals(expectedTimeOfDay, actualTimeOfDay);
		
		// Validate for a null Slot in SlotTimeOfDay
		expectedTimeOfDay = null;
		testSlots.clear();
		testSlots.put("TimeOfDay", Slot.builder().withName("TimeOfDay").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeOfDay = new SlotTimeOfDay(intent);
		actualTimeOfDay = slotTimeOfDay.validate();
		assertEquals(expectedTimeOfDay, actualTimeOfDay);
		
		// Validate for a empty (whitespace only) Slot in SlotTimeOfDay
		expectedTimeOfDay = null;
		testSlots.clear();
		testSlots.put("TimeOfDay", Slot.builder().withName("TimeOfDay").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeOfDay = new SlotTimeOfDay(intent);
		actualTimeOfDay = slotTimeOfDay.validate();
		assertEquals(expectedTimeOfDay, actualTimeOfDay);
		
		// Validate for an unknown time zone name Slot in SlotTimeOfDay
		expectedTimeOfDay = null;
		testSlots.clear();
		testSlots.put("TimeOfDay", Slot.builder().withName("TimeOfDay").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotTimeOfDay = new SlotTimeOfDay(intent);
		actualTimeOfDay = slotTimeOfDay.validate();
		assertEquals(expectedTimeOfDay, actualTimeOfDay);
	}

}
