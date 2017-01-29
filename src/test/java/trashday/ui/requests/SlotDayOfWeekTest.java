package trashday.ui.requests;

import static org.junit.Assert.*;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;

public class SlotDayOfWeekTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotDayOfWeekTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotDayOfWeek testSlotDayOfWeek;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("DayOfWeek")
				.withValue("Monday")
				.build();
		testSlots.put("DayOfWeek", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotDayOfWeek = new SlotDayOfWeek(testIntent); 
		log.info("setUpBeforeClass: testSlotDayOfWeek={}", testSlotDayOfWeek);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotDayOfWeek.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "DayOfWeek";
		String actualName = testSlotDayOfWeek.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Day Of Week";
		String actualDescription = testSlotDayOfWeek.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotDayOfWeek.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
	}

	@Test
	public void testValidate() {
		LocalDateTime ldtRequest = LocalDateTime.now();
		
		// Validate for a "good" Slot in SlotDayOfWeek
		DayOfWeek expectedDayOfWeek = DayOfWeek.MONDAY;
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue("monday").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotDayOfWeek slotDayOfWeek = new SlotDayOfWeek(intent); 
		DayOfWeek actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
		
		// Validate for a "good" Slot in SlotDayOfWeek
		expectedDayOfWeek = ldtRequest.getDayOfWeek();
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue("today").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfWeek = new SlotDayOfWeek(intent); 
		actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
		
		// Validate for a "good" Slot in SlotDayOfWeek
		expectedDayOfWeek = ldtRequest.getDayOfWeek().plus(1);
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue("tomorrow").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfWeek = new SlotDayOfWeek(intent); 
		actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
		
		// Validate for a "good" Slot in SlotDayOfWeek
		expectedDayOfWeek = ldtRequest.getDayOfWeek().minus(1);
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue("yesterday").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfWeek = new SlotDayOfWeek(intent); 
		actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
		
		// Validate for a null Slot in SlotDayOfWeek
		expectedDayOfWeek = null;
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfWeek = new SlotDayOfWeek(intent);
		actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
		
		// Validate for a empty (whitespace only) Slot in SlotDayOfWeek
		expectedDayOfWeek = null;
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfWeek = new SlotDayOfWeek(intent);
		actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
		
		// Validate for an unknown time zone name Slot in SlotDayOfWeek
		expectedDayOfWeek = null;
		testSlots.clear();
		testSlots.put("DayOfWeek", Slot.builder().withName("DayOfWeek").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfWeek = new SlotDayOfWeek(intent);
		actualDayOfWeek = slotDayOfWeek.validate(ldtRequest);
		assertEquals(expectedDayOfWeek, actualDayOfWeek);
	}

}
