package sushi.compile.distance;

import java.lang.reflect.Field;

import sushi.util.ArrayUtils;
import sushi.util.ReflectionUtils;

public class PrimitiveDistance {
	private static final double NAN_WEIGHT = 2000.0d;

	public static double distance(Field f1, Object obj1, Field f2, Object obj2) {
		double distance = 0.0d;
		
		Class<?> f1Type = f1.getType().getComponentType() == null ? f1.getType() : f1.getType().getComponentType();
		try {
			if (f1Type.equals(int.class)) {
				distance = PrimitiveDistance.intDistance(f1.getInt(obj1), f2.getInt(obj2));
			}
			else if (f1Type.equals(char.class)) {
				distance = PrimitiveDistance.charDistance(f1.getChar(obj1), f2.getChar(obj2));
			}
			else if (f1Type.equals(short.class)) {
				distance = PrimitiveDistance.shortDistance(f1.getShort(obj1), f2.getShort(obj2));
			}
			else if (f1Type.equals(long.class)) {
				distance = PrimitiveDistance.longDistance(f1.getLong(obj1), f2.getLong(obj2));
			}
			else if (f1Type.equals(float.class)) {
				distance = PrimitiveDistance.floatDistance(f1.getFloat(obj1), f2.getFloat(obj2));
			}
			else if (f1Type.equals(double.class)) {
				distance = PrimitiveDistance.doubleDistance(f1.getDouble(obj1), f2.getDouble(obj2));
			}
			else if (f1Type.equals(boolean.class)) {
				distance = PrimitiveDistance.booleanDistance(f1.getBoolean(obj1), f2.getBoolean(obj2));
			}
			else if (f1Type.equals(byte.class)) {
				distance = PrimitiveDistance.byteDistance(f1.getByte(obj1), f2.getByte(obj2));
			}
			
			// Primitive class objects
			else if (ReflectionUtils.primitives.contains(f1Type)) {
				distance = distance(obj1, obj2);
			}
			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		return distance;
	}
	
	public static double distance(Object obj1, Object obj2) {
		double distance = 0.0d;
		
		Class<?> clazz = obj1.getClass().getComponentType() == null ? obj1.getClass() : obj1.getClass().getComponentType();
		try {
			if (clazz.equals(int.class) || clazz.equals(Integer.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Integer.class)) {
						distance = intDistance(ArrayUtils.toPrimitive((Integer[]) obj1), ArrayUtils.toPrimitive((Integer[]) obj2));
					}
					else {
						distance = intDistance((int[]) obj1, (int[]) obj2);
					}
				}
				else {
					distance = intDistance((int) obj1, (int) obj2);
				}
			}
			else if (clazz.equals(char.class) || clazz.equals(Character.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Character.class)) {
						distance = charDistance(ArrayUtils.toPrimitive((Character[]) obj1), ArrayUtils.toPrimitive((Character[]) obj2));
					}
					else {
						distance = charDistance((char[]) obj1, (char[]) obj2);
					}
				}
				else {
					distance = charDistance((char) obj1, (char) obj2);
				}
			}
			else if (clazz.equals(short.class) || clazz.equals(Short.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Short.class)) {
						distance = shortDistance(ArrayUtils.toPrimitive((Short[]) obj1), ArrayUtils.toPrimitive((Short[]) obj2));
					}
					else {
						distance = shortDistance((short[]) obj1, (short[]) obj2);
					}
				}
				else {
					distance = shortDistance((short) obj1, (short) obj2);
				}
			}
			else if (clazz.equals(long.class) || clazz.equals(Long.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Long.class)) {
						distance = longDistance(ArrayUtils.toPrimitive((Long[]) obj1), ArrayUtils.toPrimitive((Long[]) obj2));
					}
					else {
						distance = longDistance((long[]) obj1, (long[]) obj2);
					}
				}
				else {
					distance = longDistance((long) obj1, (long) obj2);
				}
			}
			else if (clazz.equals(float.class) || clazz.equals(Float.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Float.class)) {
						distance = floatDistance(ArrayUtils.toPrimitive((Float[]) obj1), ArrayUtils.toPrimitive((Float[]) obj2));
					}
					else {
						distance = floatDistance((float[]) obj1, (float[]) obj2);
					}
				}
				else {
					distance = floatDistance((float) obj1, (float) obj2);
				}
			}
			else if (clazz.equals(double.class) || clazz.equals(Double.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Double.class)) {
						distance = doubleDistance(ArrayUtils.toPrimitive((Double[]) obj1), ArrayUtils.toPrimitive((Double[]) obj2));
					}
					else {
						distance = doubleDistance((double[]) obj1, (double[]) obj2);
					}
				}
				else {
					distance = doubleDistance((double) obj1, (double) obj2);
				}
			}
			else if (clazz.equals(boolean.class) || clazz.equals(Boolean.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Boolean.class)) {
						distance = booleanDistance(ArrayUtils.toPrimitive((Boolean[]) obj1), ArrayUtils.toPrimitive((Boolean[]) obj2));
					}
					else {
						distance = booleanDistance((boolean[]) obj1, (boolean[]) obj2);
					}
				}
				else {
					distance = booleanDistance((boolean) obj1, (boolean) obj2);
				}
			}
			else if (clazz.equals(byte.class) || clazz.equals(Byte.class)) {
				if (obj1.getClass().isArray()) {
					if (clazz.equals(Byte.class)) {
						distance = byteDistance(ArrayUtils.toPrimitive((Byte[]) obj1), ArrayUtils.toPrimitive((Byte[]) obj2));
					}
					else {
						distance = byteDistance((byte[]) obj1, (byte[]) obj2);
					}
				}
				else {
					distance = byteDistance((byte) obj1, (byte) obj2);
				}
			}	
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return distance;
	}
	
	private static double intDistance(int a, int b) {
		return Math.abs(a - b);
	}
	
	private static double intDistance(int[] a, int[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += intDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}

	private static double shortDistance(short a, short b) {
		return Math.abs(a - b);
	}
	
	private static double shortDistance(short[] a, short[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += shortDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}

	private static double longDistance(long a, long b) {
		return Math.abs(a - b);
	}
	
	private static double longDistance(long[] a, long[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += longDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}

	private static double floatDistance(float a, float b) {
		if (Float.isNaN(a) && Float.isNaN(b)) { 
			return 0;
		}
		if (Float.isNaN(a) || Float.isNaN(b)) { 
			return NAN_WEIGHT;
		}
		if (Float.isInfinite(a) && Float.isInfinite(b)) { 
			return 0;
		}
		if (Float.isInfinite(a) || Float.isInfinite(b)) { 
			return NAN_WEIGHT;
		}
		return Math.abs(a - b);
	}

	private static double floatDistance(float[] a, float[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += floatDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}
	
	private static double doubleDistance(double a, double b) {
		if (Double.isNaN(a) && Double.isNaN(b)) { 
			return 0;
		}
		if (Double.isNaN(a) || Double.isNaN(b)) { 
			return NAN_WEIGHT;
		}
		if (Double.isInfinite(a) && Double.isInfinite(b)) { 
			return 0;
		}
		if (Double.isInfinite(a) || Double.isInfinite(b)) { 
			return NAN_WEIGHT;
		}
		return Math.abs(a - b);
	}
	
	private static double doubleDistance(double[] a, double[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += doubleDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}
	
	private static double byteDistance(byte a, byte b) {
		return Math.abs(a - b);
	}
	
	private static double byteDistance(byte[] a, byte[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += byteDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}
	
	private static double charDistance(char a, char b) {
		String stringF1 = new String(new char[] { a });
		String stringF2 = new String(new char[] { b });
		return LevenshteinDistance.calculateDistance(stringF1, stringF2);
	}
	
	private static double charDistance(char[] a, char[] b) {
		String stringF1 = new String(a);
		String stringF2 = new String(b);
		return LevenshteinDistance.calculateDistance(stringF1, stringF2);
	}

	private static double booleanDistance(boolean a, boolean b) {
		return a ^ b ? 1 : 0;
	}
	
	private static double booleanDistance(boolean[] a, boolean[] b) {
		double distance = 0;
		for (int i = 0; i < Math.min(a.length, b.length); i++) {
			distance += booleanDistance(a[i], b[i]);
		}
		distance += Math.abs(a.length - b.length) * Distance.ARRAY_CELL_FACTOR;
		return distance;
	}

}
