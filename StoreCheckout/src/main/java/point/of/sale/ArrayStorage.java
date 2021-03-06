package point.of.sale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ArrayStorage extends HashStorage {

	int readInconsistencies = 0;
	
	private static Logger logger = LogManager.getLogger();
	
	// Kludge: demonstrating migration to new datastore as array
	int size = 999;
	String[] array;
	
	public ArrayStorage() {
		if (StoreToggles.isEnableArray) {
			array = new String[size];
			logger.trace("Initializing an array");
		}
		
		if (StoreToggles.isEnableArray) {
			logger.info("Array Enabled");
		}
		
		if (StoreToggles.isEnableHash) {
			logger.info("Hash Enabled");
		}
		
		if (StoreToggles.isUnderTest) {
			logger.info("System is under test");
		}
		
	}
	
	public int getReadInconsistencies() {
		return readInconsistencies;
	}
	
	//read from the datastore
	@Override
	public String barcode(String barcode) {
		
		logger.info("Read");
		
		if (StoreToggles.isEnableArray && StoreToggles.isEnableHash) {
			//get the expected value from the old datastore
			String expected = super.barcode(barcode);
			
			//should happen asynch
			//shadow read
			String actual = array[Integer.parseInt(barcode)];
			if(!expected.equals(actual)) {
				
				logger.warn("Read Inconsistency");
				readInconsistencies++;
				
				array[Integer.parseInt(barcode)] = expected;
				
				violation(barcode, expected, actual);
				
			}
		}
		
		if (StoreToggles.isEnableHash) {
			
			logger.trace("Read from Hash" + barcode);
			
			return super.barcode(barcode);
		}
		
		//read from new datastore
		return array[Integer.parseInt(barcode)];
		
		
		
	}

	//write to the datastore
	@Override
	public void put(String barcode, String item) {
		
		logger.info("Write");
		
		if (StoreToggles.isEnableHash) {
			// still write to the old HashStorage
			super.put(barcode, item);
		}
		
		if (StoreToggles.isEnableArray) {
			//shadow write
			array[Integer.parseInt(barcode)] = item;
		}
		
		if(checkConsistency() > 0) {
			logger.warn("Write Inconsistency");
		}
	}
	
	public void testingOnlyHashPut(String barcode, String item) {
		if (StoreToggles.isUnderTest) { 
			super.put(barcode, item);
		}
	}

	public void forklift() {
		
		if (! (StoreToggles.isEnableArray && StoreToggles.isEnableHash)) {
			return;
		}
		
		//copy over all the data that is in the hash
		for (String barcode : hashMap.keySet()) {
			array[Integer.parseInt(barcode)] = hashMap.get(barcode);
			
		}
	}
	
	public int checkConsistency() {
		
		if (! (StoreToggles.isEnableArray && StoreToggles.isEnableHash)) {
			return 0;
		}
		
		int inconsistency = 0;
		
		for (String barcode : hashMap.keySet()) {
			String expected = hashMap.get(barcode);
			String actual = array[Integer.parseInt(barcode)];
			
			if(!expected.equals(actual)) {
				
				logger.warn("Data inconsistency");
				//record the inconsistency
				inconsistency++;
				
				//print it
				violation(barcode, expected, actual);
				
				//correct it in the new datastore
				array[Integer.parseInt(barcode)] = expected;
			}
		}
		
		return inconsistency;
		
	}
	
	private void violation(String barcode, String expected, String actual) {
		logger.debug("Consistency Violation!\n" + 
				"barcode = " + barcode +
				"\n\t expected = " + expected
				+ "\n\t actual = " + actual);
				
	}
	
	public String[] getCopyArrayStorage() {
		if (StoreToggles.isEnableArray) {
			return array.clone();
		}
		
		return null;
	}

}
