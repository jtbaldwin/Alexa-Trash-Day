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

public class SlotPickupNameTest {
	/** Log object for this class */
    private static final Logger log = LoggerFactory.getLogger(SlotPickupNameTest.class);
    /** Test Data */
    private static Intent testIntent;
    /** Test Data */
    private static Slot testSlotNormal;
    /** Test Data */
    private static Map<String, Slot> testSlots;
    /** Test Data */
    private SlotPickupName testSlotPickupName;
    
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testSlots = new HashMap<String, Slot>();
		Slot.Builder builderSlot = Slot.builder();
		testSlotNormal = builderSlot
				.withName("PickupName")
				.withValue("trash")
				.build();
		testSlots.put("PickupName", testSlotNormal);
		Intent.Builder builder = Intent.builder();
		testIntent = builder
				.withName("MyIntentName")
				.withSlots(testSlots)
				.build();
		log.info("setUpBeforeClass: testIntent={}", testIntent);
	}

	@Before
	public void setUp() throws Exception {
		testSlotPickupName = new SlotPickupName(testIntent); 
		log.info("setUpBeforeClass: testSlotPickupName={}", testSlotPickupName);
	}

	@Test
	public void testGetSlot() {
		Slot expectedSlot = testSlotNormal;
		Slot actualSlot = testSlotPickupName.getSlot();
		assertEquals(expectedSlot, actualSlot);
	}

	@Test
	public void testGetName() {
		String expectedName = "PickupName";
		String actualName = testSlotPickupName.getName();
		assertEquals(expectedName, actualName);
	}

	@Test
	public void testGetDescription() {
		String expectedDescription = "Pickup Name";
		String actualDescription = testSlotPickupName.getDescription();
		assertEquals(expectedDescription, actualDescription);
	}

	@Test
	public void testIsEmpty() {
		boolean expectedIsEmpty = false;
		boolean actualIsEmpty = testSlotPickupName.isEmpty();
		assertEquals(expectedIsEmpty, actualIsEmpty);
	}

	@Test
	public void testValidate() {
		
		// Validate for a "good" Slot in SlotPickupName
		String expectedPickupName = "trash";
		testSlots.clear();
		testSlots.put("PickupName", Slot.builder().withName("PickupName").withValue("trash").build());
		Intent intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		SlotPickupName slotPickupName = new SlotPickupName(intent); 
		String actualPickupName = slotPickupName.validate();
		assertEquals(expectedPickupName, actualPickupName);
		
		// Validate for a null Slot in SlotPickupName
		expectedPickupName = null;
		testSlots.clear();
		testSlots.put("PickupName", Slot.builder().withName("PickupName").withValue(null).build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotPickupName = new SlotPickupName(intent);
		actualPickupName = slotPickupName.validate();
		assertEquals(expectedPickupName, actualPickupName);
		
		// Validate for a empty (whitespace only) Slot in SlotPickupName
		expectedPickupName = null;
		testSlots.clear();
		testSlots.put("PickupName", Slot.builder().withName("PickupName").withValue("  ").build());
		intent = Intent.builder().withName("MyIntentName").withSlots(testSlots).build();
		slotPickupName = new SlotPickupName(intent);
		actualPickupName = slotPickupName.validate();
		assertEquals(expectedPickupName, actualPickupName);
	}

}
