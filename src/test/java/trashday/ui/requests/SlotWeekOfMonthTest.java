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

import trashday.ui.FormatUtils;

public class SlotWeekOfMonthTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotWeekOfMonthTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotWeekOfMonth testSlotWeekOfMonth;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("WeekOfMonth")
				.withValue("1")
				.build();
		testSlots.put("WeekOfMonth", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotWeekOfMonth = new SlotWeekOfMonth(testIntent); 
		log.info("setUpBeforeClass: testSlotWeekOfMonth={}", testSlotWeekOfMonth);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotWeekOfMonth.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "WeekOfMonth";
		String actualName = testSlotWeekOfMonth.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Week Of Month";
		String actualDescription = testSlotWeekOfMonth.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotWeekOfMonth.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
		
		// Validate for a null Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue(null).build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertTrue(slotWeekOfMonth.isEmpty());
		
		// Validate for a null Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("Garbage").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertTrue(slotWeekOfMonth.isEmpty());
		
		// Validate for a empty (whitespace only) Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertTrue(slotWeekOfMonth.isEmpty());

	}

	@Test
	public void testValidate() {		
		// Validate for a "good" Slot in SlotWeekOfMonth
		for (Integer expectedWeekOfMonth=1; expectedWeekOfMonth<=5; expectedWeekOfMonth++) {
			testSlots.clear();
			testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue(FormatUtils.domMap.get(expectedWeekOfMonth)).build());
			Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
			SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent); 
			assertEquals(expectedWeekOfMonth, slotWeekOfMonth.validate());
		}
		
		// Validate for a "bad" Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue("0").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotWeekOfMonth slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertNull(slotWeekOfMonth.validate());
		
		// Validate for a "bad" Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue("thirty-two").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertNull(slotWeekOfMonth.validate());
		
		// Validate for a "bad" Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertNull(slotWeekOfMonth.validate());
		
		// Validate for a null Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertNull(slotWeekOfMonth.validate());
		
		// Validate for a empty (whitespace only) Slot in SlotWeekOfMonth
		testSlots.clear();
		testSlots.put("WeekOfMonth", Slot.builder().withName("WeekOfMonth").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotWeekOfMonth = new SlotWeekOfMonth(intent);
		assertNull(slotWeekOfMonth.validate());
	}

}
