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

@SuppressWarnings("deprecation")
public class SlotNextOrThisTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotNextOrThisTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotNextOrThis testSlotNextOrThis;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("NextOrThis")
				.withValue("next")
				.build();
		testSlots.put("NextOrThis", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotNextOrThis = new SlotNextOrThis(testIntent); 
		log.info("setUpBeforeClass: testSlotNextOrThis={}", testSlotNextOrThis);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotNextOrThis.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "NextOrThis";
		String actualName = testSlotNextOrThis.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "next or this week";
		String actualDescription = testSlotNextOrThis.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotNextOrThis.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
		
		// Validate for a empty (whitespace only) Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue("  ").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotNextOrThis slotNextOrThis = new SlotNextOrThis(intent); 
		assertTrue(slotNextOrThis.isEmpty());
		
		// Validate for a null Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("garbage").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNextOrThis = new SlotNextOrThis(intent); 
		assertTrue(slotNextOrThis.isEmpty());

		// Validate for a null Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNextOrThis = new SlotNextOrThis(intent); 
		assertTrue(slotNextOrThis.isEmpty());
	}

	@Test
	public void testValidate() {
		
		// Validate for a "good" Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue("next").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotNextOrThis slotNextOrThis = new SlotNextOrThis(intent); 
		assertTrue(slotNextOrThis.validate());
		
		// Validate for a "good" Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue("this").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNextOrThis = new SlotNextOrThis(intent); 
		assertFalse(slotNextOrThis.validate());
		
		// Validate for a garbage Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue("garbage").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNextOrThis = new SlotNextOrThis(intent); 
		assertNull(slotNextOrThis.validate());
		
		// Validate for a empty (whitespace only) Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNextOrThis = new SlotNextOrThis(intent); 
		assertNull(slotNextOrThis.validate());
		
		// Validate for a null Slot in slotNextOrThis
		testSlots.clear();
		testSlots.put("NextOrThis", Slot.builder().withName("NextOrThis").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotNextOrThis = new SlotNextOrThis(intent); 
		assertNull(slotNextOrThis.validate());
	}

}
