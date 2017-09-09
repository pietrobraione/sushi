package sushi.compile.reflection;

import java.lang.reflect.Field;

public class AccessibleObject {
    private final Object target;
    
    public AccessibleObject(Object o) {
        target = o;
    }
    
    public void set(String fieldName, Object value) {
        try {
        	Class<?> clazz = target.getClass();
        	Field p = null;
        	do {
        		try {
        			p = clazz.getDeclaredField(fieldName);
        			break;
        		} catch (NoSuchFieldException e) {
        			clazz = clazz.getSuperclass();
        		}
        	} while (clazz != null);
        	if (p == null) { 
        		throw new NoSuchFieldException();
        	}
            p.setAccessible(true);
            p.set(target, value);
        } catch (IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    public AccessibleObject get(String fieldName) {
        try {
        	Class<?> clazz = target.getClass();
        	Field p = null;
        	do {
        		try {
        			p = clazz.getDeclaredField(fieldName);
        			break;
        		} catch (NoSuchFieldException e) {
        			clazz = clazz.getSuperclass();
        		}
        	} while(clazz != null);
        	if (p == null) {
        		throw new NoSuchFieldException();
        	}
            p.setAccessible(true);
            return new AccessibleObject(p.get(target));
        } catch (IllegalArgumentException | IllegalAccessException
            | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException(e);
        }
    }
    
    public Object getValue() {
        return target;
    }
}
