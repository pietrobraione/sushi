package sushi.compile.path_condition_distance;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import sushi.util.ReflectionUtils;


public class CandidateBackbone {
	// We keep the direct and reverse mapping between visited objects and their origins 
	private final Map<ObjectMapWrapper, String> visitedObjects = new HashMap<ObjectMapWrapper, String>(); 
	private final Map<String, Object> visitedOrigins = new HashMap<String, Object>(); 

	private final Collection<String> invalidFieldPaths = new HashSet<String>(); 
	
	private void storeInBackbone(Object obj, String origin) {
		// If another origin already exist, this is an alias path
		// and then it shall not be stored
		if (!visitedObjects.containsKey(new ObjectMapWrapper(obj))) {
			visitedOrigins.put(origin, obj);		
			visitedObjects.put(new ObjectMapWrapper(obj), origin);
		}
	}

	public Object getVisitedObject(String origin) {
		return visitedOrigins.get(origin);
	}

	public String getOrigin(Object obj) {
		return visitedObjects.get(new ObjectMapWrapper(obj));
	}

	public void addInvalidFieldPath(String refPath) {
		invalidFieldPaths.add(refPath);
	}
	
	private static final class ObjectMapWrapper {
		private Object o;
		ObjectMapWrapper(Object o) { this.o = o; }
	
		@Override
		public boolean equals(Object obj) {
			if (obj == null) {
				return false;
			}
			if (this == obj) {
				return true;
			}
			if (!(obj instanceof ObjectMapWrapper)) {
				return false;
			}
			final ObjectMapWrapper omw = (ObjectMapWrapper) obj;
			return (this.o == omw.o);
		}
	
		@Override
		public int hashCode() {
			return System.identityHashCode(this.o);
		}
	}

	public Object retrieveOrVisitField(String origin, Map<String, Object> candidateObjects) 
			throws FieldNotInCandidateException, FieldDependsOnInvalidFieldPathException {
		assert (origin != null); 
		
		// We check whether this entire origin corresponds to an already visited object
		Object obj = getVisitedObject(origin);
		if (obj != null) {
			return obj;
		}
		
		String[] fields = origin.split("\\.");
		
		fields = rearrangeFieldsStringsWrtArrayAccesses(fields);
		
		String originPrefix = fields[0];
		boolean isStatic = originPrefix.matches("\\[.*\\]");
		if (!isStatic && !candidateObjects.containsKey(originPrefix)) {
			throw new SimilarityComputationException("Origin " + originPrefix + " does not correspond to any root object in candidate");
		}
		
		if (isStatic) {
			try {
				final String className = originPrefix.substring(1, originPrefix.length() - 1).replace('/', '.');
				final Field f = Class.forName(className).getDeclaredField(fields[1]);
				f.setAccessible(true);
				obj = f.get(null);
			} catch (NoSuchFieldException | SecurityException | ClassNotFoundException | IllegalArgumentException | IllegalAccessException e) {
				throw new SimilarityComputationException("Unexpected error while retrieving the value of a static field: " + originPrefix + "." + fields[1]);
			}
		} else {
			obj = candidateObjects.get(originPrefix);
		}
		for (int i = (isStatic ? 2 : 1); i < fields.length; i++) {
			if (obj == null) {
				throw new FieldNotInCandidateException();
			}
			
			if (invalidFieldPaths.contains(originPrefix)) {
				throw new FieldDependsOnInvalidFieldPathException(originPrefix);
			}

			originPrefix += "." +  fields[i];
			if (obj.getClass().isArray()) {
				obj = retrieveFromArray(obj, fields[i], candidateObjects);
			} else {
				Object hack = hack4StringJava6(obj, fields[i]); //GIO: TODO
				if (hack != null) {
					obj = hack;
					continue;
				}

				Field f = ReflectionUtils.getInheritedPrivateField(obj.getClass(), fields[i]);

				if (f == null) {
					throw new SimilarityComputationException("Field name " + fields[i] + " in origin " + origin + " does not exist in the corrsponding object");
				}
				f.setAccessible(true);
			
				try {
					obj = f.get(obj);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new SimilarityComputationException("Unexpected error while retrieving the value of a field");
				}
			}
		}
		
		storeInBackbone(obj, origin);
		
		return obj;
	}

	private String[] rearrangeFieldsStringsWrtArrayAccesses(String[] fields) {
		List<String> fieldsRefined = new ArrayList<>();
		fieldsRefined.add(fields[0]); //fields[0] cannot be an array access specifier

		String arrayAccessSpecifier = "";
		int unmatched = 0;
		for (int i = 1; i < fields.length; i++) { 
			if (fields[i].charAt(0) == '[') {
				unmatched++;
			}
			if (unmatched > 0) {
				if (fields[i].charAt(0) != '[') {
					arrayAccessSpecifier += ".";
				}
				arrayAccessSpecifier += fields[i];
			} else {
				fieldsRefined.add(fields[i]);
			}
			if (fields[i].charAt(fields[i].length() - 1) == ']') {
				unmatched--;
				if (unmatched == 0) {
					fieldsRefined.add(arrayAccessSpecifier);
					arrayAccessSpecifier = "";
				}
			}
		}
		
		return fieldsRefined.toArray(new String[0]);
	}

	private Object retrieveFromArray(Object obj, String fieldSpec, Map<String, Object> candidateObjects) 
			throws FieldNotInCandidateException, FieldDependsOnInvalidFieldPathException {
		if (fieldSpec.equals("length")) {
			return Array.getLength(obj);
		}
		else if (fieldSpec.matches("\\[.*\\]")) {
			String indexString = fieldSpec.substring(1, fieldSpec.length() - 1);
			
			int index = 0;

			while (indexString.indexOf('{') >= 0) {
				int startOrigin = indexString.indexOf('{');
				
				int endOrigin = indexString.indexOf(')', startOrigin);
				if (endOrigin < 0) {
					endOrigin = indexString.length();
				}
				
				String origin = indexString.substring(startOrigin, endOrigin);

				Object o = retrieveOrVisitField(origin, candidateObjects);
				if (o instanceof Integer) {
					index += (Integer) o;
					indexString = indexString.substring(0, startOrigin) +  /*(Integer) o +*/ indexString.substring(endOrigin);
				} else {
					throw new SimilarityComputationException("Unexpected type (" + o.getClass() +") while retrieving the value of an array index: " + origin + "." + fieldSpec);
				}

			}
			
			//TODO: Fix to support evaluation of arbitrary expressions
			int last = 0;
			while (indexString.indexOf('1', last) >= 0) {
				index += 1;
				last = indexString.indexOf('1', last) + 1;
			}

			/*ScriptEngineManager mgr = new ScriptEngineManager();
		    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		    Object ev;
		    try {
		    	ev = engine.eval(indexString);
			} catch (ScriptException e) {
				throw new SimilarityComputationException("Cannot evaluate an array index out of expression " + indexString + " for " + fieldSpec);
			}
		    if (ev instanceof Integer) {
				index = (Integer) ev;
			} else {
				throw new SimilarityComputationException("Unexpected type (" + ev.getClass() +") while retrieving the value of an array index: " + indexString + "." + fieldSpec);
			}*/
		    
		    try {
		    	return Array.get(obj, index);
		    } catch (ArrayIndexOutOfBoundsException e) {
		    	throw new FieldNotInCandidateException();
		    }
		    
		} else {
			throw new SimilarityComputationException("Unexpected field or indexSpec in array object: " +  fieldSpec);					
		}
	}

	private Object hack4StringJava6(Object obj, String fname) {
		if (obj instanceof String) {
			if ("offset".equals(fname)) {
				return 0;
			}
			else if ("count".equals(fname)) {
				try {
					return String.class.getMethod("length", (Class<?>[]) null).invoke(obj, (Object[]) null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| NoSuchMethodException | SecurityException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return null;
	}
}
