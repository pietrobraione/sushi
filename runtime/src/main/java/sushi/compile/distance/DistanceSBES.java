package sushi.compile.distance;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import sushi.compile.reflection.ObjectField;
import sushi.logging.Logger;
import sushi.util.ReflectionUtils;
import sushi.util.StringUtils;

public class DistanceSBES {
	public static final double DIFFERENT_CLASSES_WEIGHT = 1000.0d;

	private static final Logger logger = new Logger(DistanceSBES.class);

	private static final List<DistancePair> worklist = new LinkedList<DistancePair>();
	private static final Map<Object, Integer> visited = new IdentityHashMap<Object, Integer>();

	private DistanceSBES() {}
	
	public static double distance(HashSet<ObjectField> nullObjectFields, Object... objects) {
		double retVal = 0;
		for (int i = 0; i < objects.length - (objects.length % 2); i += 2) {
			retVal += distance2(nullObjectFields, objects[i], objects[i + 1]);
		}
		return retVal;
	}
	
	private static double distance2(HashSet<ObjectField> nullObjectFields, Object oPartial, Object oComplete) {
		logger.debug("distance between: " + oPartial + " and " + oComplete);
		if (oPartial == null && oComplete == null) {
			logger.debug("both null");
			return 0.0d;
		}
		else if (oPartial == null ^ oComplete == null) {
			logger.debug("one of the two is null");
			return ObjectDistance.getNullDistance(oPartial, oComplete);
		}
		
		Class<?> cPartial = oPartial.getClass();
		Class<?> cComplete = oComplete.getClass();

		if (!cPartial.getClass().equals(cComplete.getClass())) {
			// Do we want to penalize it? A penalty could affect the ability of
			// the technique to synthesize equivalent sequences..or not?
			return ReflectionUtils.classDistance(cPartial.getClass(), cComplete.getClass()) * DIFFERENT_CLASSES_WEIGHT;
		}
		if (cPartial.isArray() ^ cComplete.isArray()) {
			logger.debug("one of the two is an array");
			return Distance.ARRAY_CELL_FACTOR * 10;
		}
		
		worklist.clear();
		visited.clear();
		
		return calculate(oPartial, oComplete, nullObjectFields);
	}
	
	private static double calculate(Object oPartial, Object oComplete, HashSet<ObjectField> nullObjectFields) {
		double distance = 0.0d;
		double lazyInitDistance = 0.0d;
		
		worklist.add(new DistancePair(oPartial, oComplete));

		while (!worklist.isEmpty()) {
			DistancePair pair = worklist.remove(0);
			Object objPartial = pair.o1;
			Object objComplete = pair.o2;
			
			//========================================CORNER CASES========================================
			//------------------NULL-------------------
			if (objPartial == null && objComplete == null) {
				logger.debug("Both objects are null");
				continue;
			}
			else if (objPartial == null ^ objComplete == null) {
				logger.debug("One of the two objects is null");				
				String name = "";
				if (objPartial != null) {
					name = objPartial.getClass().getName(); 
				}
				else {
					name = objComplete.getClass().getName();
				}
				
				if (name.contains("$") && StringUtils.isNumeric(name.subSequence(name.indexOf('$') + 1, name.length()))) {
					logger.debug("One object is an anonymous class, lazy init?");
					lazyInitDistance += ObjectDistance.getNullDistance(oPartial, oComplete);
					continue; // lazy-init trick
				}
				else {
					distance += ObjectDistance.getNullDistance(objPartial, objComplete);
					logger.debug("Distance: " + distance);
					continue;
				}
			}
			
			//------------DIFFERENT CLASSES------------
			else if (!objPartial.getClass().equals(objComplete.getClass())) {
				logger.debug("different classes: " + objPartial.getClass() + " vs " + objComplete.getClass());
				distance += DIFFERENT_CLASSES_WEIGHT;
				logger.debug("Distance: " + distance);
				continue;
			}
			
			//----------------PRIMITIVE----------------
			// this definition of primitive contains also
			// primitive classes (e.g. Integer)
			else if (ReflectionUtils.isPrimitive(objPartial)) {
				distance += PrimitiveDistance.distance(objPartial, objComplete);
				logger.debug("Distance: " + distance);
				continue;
			}
			
			//------------------STRING-----------------
			else if (ReflectionUtils.isString(objPartial)) {
				logger.debug("Strings");
				distance += LevenshteinDistance.calculateDistance((String) objPartial, (String) objComplete);
				logger.debug("Distance: " + distance);
				continue;
			}
			
			//-----------------ARRAYS------------------
			else if (ReflectionUtils.isArray(objPartial)) {
				logger.debug("Arrays");
				distance += handleArray(objPartial, objComplete);
				logger.debug("Distance: " + distance);
				continue;
			}
			
			//----------CIRCULAR DEPENDENCIES----------
			else if (visited.put(objPartial, 1) != null && visited.put(objComplete, 2) != null) {
				continue;
			}
			
			//------------------OBJECT-----------------
			List<Field> fsPartial = ReflectionUtils.getInheritedPrivateFields(objPartial.getClass());
			List<Field> fsComplete = ReflectionUtils.getInheritedPrivateFields(objComplete.getClass());
			for (int i = 0; i < fsPartial.size(); i++) {
				try {
					Field fPartial = fsPartial.get(i);
					Field fComplete = fsComplete.get(i);
					
					fPartial.setAccessible(true);
					fComplete.setAccessible(true);
					
					// skip comparison of constants
					if (ReflectionUtils.isConstant(fPartial) && ReflectionUtils.isConstant(fComplete)) {
						logger.debug("Skip: " + Modifier.toString(fPartial.getModifiers()) + " " + fPartial.getType() + " " + fPartial.getName());
						continue;
					}
					else if (FieldFilter.exclude(fPartial) || FieldFilter.exclude(fComplete)) {
						logger.debug("Exclude: " + Modifier.toString(fPartial.getModifiers()) + " " + fPartial.getType() + " " + fPartial.getName());
						continue;
					}
					
					logger.debug("Comparing " + Modifier.toString(fPartial.getModifiers())  + " " + fPartial.getDeclaringClass().getCanonicalName() + "." + fPartial.getName() + " vs " +  
							Modifier.toString(fPartial.getModifiers())  + " " + fComplete.getDeclaringClass().getCanonicalName() + "." + fComplete.getName());
					
					ComparisonType type = getComparisonType(fPartial.getType(), fComplete.getType());
					switch (type) {
					case PRIMITIVE:
						// this definition of primitives contains only real 
						// primitive values (e.g int, char, ..) primitive 
						// classes (e.g. Integer) are treated as object and 
						// handled in the subsequent iteration as corner case
						distance += PrimitiveDistance.distance(fPartial, objPartial, fComplete, objComplete);
						break;
					case STRING:
						distance += LevenshteinDistance.calculateDistance((String) fPartial.get(objPartial), (String) fComplete.get(objComplete));
						break;
					case ARRAY:
						distance += handleArray(fPartial, objPartial, fComplete, objComplete);
						break;
					case OBJECT:
						// null values and corner cases are managed at the 
						// beginning of the iteration
						logger.debug("Adding to worklist: " + Modifier.toString(fPartial.getModifiers()) + " " + fPartial.getType() + " " + fPartial.getName());
						Object obj1value = fPartial.get(objPartial);
						Object obj2value = fComplete.get(objComplete);
						
						ObjectField of = new ObjectField(objPartial, fPartial);
						if (obj1value == null && !nullObjectFields.contains(of)) {
							logger.debug("Skip because we don't care");
							continue;
						}
						
						worklist.add(new DistancePair(obj1value, obj2value));
						break;
					default:
						logger.error("Unknown comparison type: " + type);
						break;
					}
				} catch (Exception e) {
					logger.error("Error during distance calculation", e);
				}
				logger.debug("Distance: " + distance);
			}
		}
		
		if (lazyInitDistance > 0.0d && distance > 0.0d) {
			return distance + lazyInitDistance;
		}
		else {
			return distance;
		}
	}
	
	private static double handleArray(Object objPartial, Object objComplete) {
		double distance = 0.0d;
		
		ComparisonType arrayType = getComparisonType(objPartial.getClass().getComponentType(), objPartial.getClass().getComponentType());
		switch (arrayType) {
		case OBJECT:
			try {
				Object[] fPartialCast = Object[].class.cast(objPartial);
				Object[] fCompleteCast = Object[].class.cast(objComplete);
				int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
				for (int i = 0; i < length; i++) {
					worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
				}
				distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
			}
			catch (IllegalArgumentException e) {
				logger.error("Error during cast", e);
			}
			break;
		case PRIMITIVE:
			distance += PrimitiveDistance.distance(objPartial, objComplete);
			break;
		case STRING:
			try {
				String[] fPartialCast = String[].class.cast(objPartial);
				String[] fCompleteCast = String[].class.cast(objComplete);
				int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
				for (int i = 0; i < length; i++) {
					distance += LevenshteinDistance.calculateDistance(fPartialCast[i], fCompleteCast[i]);
				}
				distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
			}
			catch (IllegalArgumentException e) {
				logger.error("Error during cast", e);
			}
			break;
		case ARRAY:
			int length = Math.min(Array.getLength(objPartial), Array.getLength(objComplete));
			for (int i = 0; i < length; i++) {
				distance += handleArray(Array.get(objPartial, i), Array.get(objComplete, i));
			}
			distance += (Math.max(Array.getLength(objPartial), Array.getLength(objComplete)) - length) * Distance.ARRAY_CELL_FACTOR;
			break;
		default:
			logger.error("Unknown comparison type: " + arrayType);
			break;
		}
		
		return distance;
	}

	private static double handleArray(Field fPartial, Object objPartial, Field fComplete, Object objComplete) {
		double distance = 0.0d;
		
		ComparisonType arrayType = getComparisonType(fPartial.getType().getComponentType(), fComplete.getType().getComponentType());
		switch (arrayType) {
		case OBJECT:
			try {
				Object[] fPartialCast = Object[].class.cast(fPartial.get(objPartial));
				Object[] fCompleteCast = Object[].class.cast(fComplete.get(objComplete));
				int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
				for (int i = 0; i < length; i++) {
					worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
				}
				// trick: if two arrays have different length, but that
				// difference is not used (that is, it is null), then they are equivalent
				boolean nonNull = false;
				if (Array.getLength(fPartialCast) > Array.getLength(fCompleteCast)) {
					for (int i = length; i < fPartialCast.length; i++) {
						if (fPartialCast[i] != null) {
							nonNull = true;
						}
					}
				}
				else if (Array.getLength(fPartialCast) < Array.getLength(fCompleteCast)) {
					for (int i = length; i < fCompleteCast.length; i++) {
						if (fCompleteCast[i] != null) {
							nonNull = true;
						}
					}
				}
				if (nonNull) {
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				logger.error("Error during cast", e);
			}
			break;
		case PRIMITIVE:
			try {
				Class<?> fPartialType = fPartial.getType().getComponentType() == null ? fPartial.getType() : fPartial.getType().getComponentType();
				if (fPartialType.equals(int.class)) {
					int[] fPartialCast = int[].class.cast(fPartial.get(objPartial));
					int[] fCompleteCast = int[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(char.class)) {
					char[] fPartialCast = char[].class.cast(fPartial.get(objPartial));
					char[] fCompleteCast = char[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(short.class)) {
					short[] fPartialCast = short[].class.cast(fPartial.get(objPartial));
					short[] fCompleteCast = short[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(long.class)) {
					long[] fPartialCast = long[].class.cast(fPartial.get(objPartial));
					long[] fCompleteCast = long[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(float.class)) {
					float[] fPartialCast = float[].class.cast(fPartial.get(objPartial));
					float[] fCompleteCast = float[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(double.class)) {
					double[] fPartialCast = double[].class.cast(fPartial.get(objPartial));
					double[] fCompleteCast = double[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(boolean.class)) {
					boolean[] fPartialCast = boolean[].class.cast(fPartial.get(objPartial));
					boolean[] fCompleteCast = boolean[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}
				else if (fPartialType.equals(byte.class)) {
					byte[] fPartialCast = byte[].class.cast(fPartial.get(objPartial));
					byte[] fCompleteCast = byte[].class.cast(fComplete.get(objComplete));
					
					int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
					for (int i = 0; i < length; i++) {
						worklist.add(new DistancePair(fPartialCast[i], fCompleteCast[i]));
					}
					distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
				}		
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				logger.error("Error during cast", e);
			}
			//distance += PrimitiveDistance.distance(f1, obj1, f2, obj2);
			break;
		case STRING:
			try {
				String[] fPartialCast = String[].class.cast(fPartial.get(objPartial));
				String[] fCompleteCast = String[].class.cast(fComplete.get(objComplete));
				int length = Math.min(Array.getLength(fPartialCast), Array.getLength(fCompleteCast));
				for (int i = 0; i < length; i++) {
					distance += LevenshteinDistance.calculateDistance(fPartialCast[i], fCompleteCast[i]);
				}
				distance += (Math.max(Array.getLength(fPartialCast), Array.getLength(fCompleteCast)) - length) * Distance.ARRAY_CELL_FACTOR;
			}
			catch (IllegalArgumentException | IllegalAccessException e) {
				logger.error("Error during cast", e);
			}
			break;
		case ARRAY:
			
			break;
		default:
			logger.error("Unknown comparison type: " + arrayType);
			break;
		}
		
		return distance;
	}

	private static ComparisonType getComparisonType(Class<?> f1, Class<?> f2) {
		if (f1.isPrimitive()) {
			return ComparisonType.PRIMITIVE;
		}
		else if (f1.isArray()) {
			return ComparisonType.ARRAY;
		}
		else if (f1.equals(String.class)) {
			return ComparisonType.STRING;
		}
		return ComparisonType.OBJECT;
	}
	
}

enum ComparisonType {
	ARRAY, PRIMITIVE, STRING, OBJECT
}

class DistancePair {
	Object o1;
	Object o2;

	public DistancePair(Object o1, Object o2) {
		this.o1 = o1;
		this.o2 = o2;
	}
}