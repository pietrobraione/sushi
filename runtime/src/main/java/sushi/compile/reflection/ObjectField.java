package sushi.compile.reflection;

import static java.lang.System.identityHashCode;

import java.lang.reflect.Field;

public class ObjectField {
	
	private final Object obj;
	private Field fld;

	public ObjectField(Object obj, String fldName) {
		this.obj = obj;
		try {
        	Class<?> clazz = obj.getClass();
        	this.fld = null;
        	do {
        		try {
        			this.fld = clazz.getDeclaredField(fldName);
        			break;
        		} catch (NoSuchFieldException e) {
        			clazz = clazz.getSuperclass();
        		}
        	} while (clazz != null);
        	if (this.fld == null) {
        		throw new NoSuchFieldException();
        	}
		} catch (NoSuchFieldException | SecurityException e) {
			throw new RuntimeException(e);
		}
	}
	
	public ObjectField(Object obj, Field fldName) {
		this.obj = obj;
		this.fld = fldName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fld == null) ? 0 : fld.hashCode());
		result = prime * result + ((obj == null) ? 0 : identityHashCode(obj));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final ObjectField other = (ObjectField) obj;
		if (this.fld == null) {
			if (other.fld != null) {
				return false;
			}
		} else if (!fld.equals(other.fld)) {
			return false;
		}
		if (this.obj == null) {
			if (other.obj != null) {
				return false;
			}
		} else if (this.obj != other.obj) {
			return false;
		}
		return true;
	}

}
