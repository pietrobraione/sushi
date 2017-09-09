package sushi.compile.distance;

import java.lang.reflect.Field;

public class FieldFilter {

	public static boolean exclude(Field f1) {
		if (f1.getName().equals("modCount") && f1.getType().equals(int.class)) {
			return true;
		}
		else if (f1.getName().equals("hashSeed") && f1.getType().equals(int.class)) {
			return true;
		}
		else if (f1.getName().startsWith("expected") && f1.getType().equals(int.class)) {
			return true;
		}
		else if (f1.getName().equals("threshold") && f1.getType().equals(int.class)) {
			return true;
		}
		return false;
	}

}
