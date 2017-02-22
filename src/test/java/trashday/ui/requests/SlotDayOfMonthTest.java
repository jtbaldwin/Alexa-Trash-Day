package trashday.ui.requests;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;

public class SlotDayOfMonthTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotDayOfMonthTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotDayOfMonth testSlotDayOfMonth;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("DayOfMonth")
				.withValue("1")
				.build();
		testSlots.put("DayOfMonth", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotDayOfMonth = new SlotDayOfMonth(testIntent); 
		log.info("setUpBeforeClass: testSlotDayOfMonth={}", testSlotDayOfMonth);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotDayOfMonth.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "DayOfMonth";
		String actualName = testSlotDayOfMonth.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Day Of Month";
		String actualDescription = testSlotDayOfMonth.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotDayOfMonth.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
		
		// Validate for a null Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue(null).build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotDayOfMonth slotDayOfMonth = new SlotDayOfMonth(intent);
		assertTrue(slotDayOfMonth.isEmpty());
		
		// Validate for a null Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("Garbage").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfMonth = new SlotDayOfMonth(intent);
		assertTrue(slotDayOfMonth.isEmpty());
		
		// Validate for a empty (whitespace only) Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfMonth = new SlotDayOfMonth(intent);
		assertTrue(slotDayOfMonth.isEmpty());

	}

	@Test
	public void testValidate() {		
		// Validate for a "good" Slot in SlotDayOfMonth
		for (Integer expectedDayOfMonth=1; expectedDayOfMonth<=31; expectedDayOfMonth++) {
			testSlots.clear();
			testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue(expectedDayOfMonth.toString()).build());
			Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
			SlotDayOfMonth slotDayOfMonth = new SlotDayOfMonth(intent); 
			assertEquals(expectedDayOfMonth, slotDayOfMonth.validate());
		}
		
		// Validate for a "bad" Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue("0").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotDayOfMonth slotDayOfMonth = new SlotDayOfMonth(intent);
		assertNull(slotDayOfMonth.validate());
		
		// Validate for a "bad" Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue("32").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfMonth = new SlotDayOfMonth(intent);
		assertNull(slotDayOfMonth.validate());
		
		// Validate for a "bad" Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfMonth = new SlotDayOfMonth(intent);
		assertNull(slotDayOfMonth.validate());
		
		// Validate for a null Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfMonth = new SlotDayOfMonth(intent);
		assertNull(slotDayOfMonth.validate());
		
		// Validate for a empty (whitespace only) Slot in SlotDayOfMonth
		testSlots.clear();
		testSlots.put("DayOfMonth", Slot.builder().withName("DayOfMonth").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotDayOfMonth = new SlotDayOfMonth(intent);
		assertNull(slotDayOfMonth.validate());
	}

}
