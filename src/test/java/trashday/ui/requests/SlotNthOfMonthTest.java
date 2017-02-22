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

public class SlotNthOfMonthTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotNthOfMonthTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotNthOfMonth testSlotNthOfMonth;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("NthOfMonth")
				.withValue("1")
				.build();
		testSlots.put("NthOfMonth", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotNthOfMonth = new SlotNthOfMonth(testIntent); 
		log.info("setUpBeforeClass: testSlotNthOfMonth={}", testSlotNthOfMonth);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotNthOfMonth.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "NthOfMonth";
		String actualName = testSlotNthOfMonth.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Day Of Month";
		String actualDescription = testSlotNthOfMonth.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotNthOfMonth.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
		
		// Validate for a null Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue(null).build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent);
		assertTrue(slotNthOfMonth.isEmpty());
		
		// Validate for a null Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("Garbage").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNthOfMonth = new SlotNthOfMonth(intent);
		assertTrue(slotNthOfMonth.isEmpty());
		
		// Validate for a empty (whitespace only) Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNthOfMonth = new SlotNthOfMonth(intent);
		assertTrue(slotNthOfMonth.isEmpty());

	}

	@Test
	public void testValidate() {		
		// Validate for a "good" Slot in SlotNthOfMonth
		for (Integer expectedNthOfMonth=1; expectedNthOfMonth<=31; expectedNthOfMonth++) {
			testSlots.clear();
			testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue(FormatUtils.domMap.get(expectedNthOfMonth)).build());
			Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
			SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent); 
			assertEquals(expectedNthOfMonth, slotNthOfMonth.validate());
		}
		
		// Validate for a "bad" Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue("0").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotNthOfMonth slotNthOfMonth = new SlotNthOfMonth(intent);
		assertNull(slotNthOfMonth.validate());
		
		// Validate for a "bad" Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue("thirty-two").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNthOfMonth = new SlotNthOfMonth(intent);
		assertNull(slotNthOfMonth.validate());
		
		// Validate for a "bad" Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNthOfMonth = new SlotNthOfMonth(intent);
		assertNull(slotNthOfMonth.validate());
		
		// Validate for a null Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNthOfMonth = new SlotNthOfMonth(intent);
		assertNull(slotNthOfMonth.validate());
		
		// Validate for a empty (whitespace only) Slot in SlotNthOfMonth
		testSlots.clear();
		testSlots.put("NthOfMonth", Slot.builder().withName("NthOfMonth").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNthOfMonth = new SlotNthOfMonth(intent);
		assertNull(slotNthOfMonth.validate());
	}

}
