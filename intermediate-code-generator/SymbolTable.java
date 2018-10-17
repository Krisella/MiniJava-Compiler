import java.util.*;

public class SymbolTable {
	
	HashMap<String, ClassInfo> classMap;
	
	public SymbolTable(){
		classMap = new HashMap<String, ClassInfo>();  
	}	
	
	public void AddChildClass(String className, String toBeAdded){
		ClassInfo updatedClass = classMap.get(className);
		updatedClass.childClasses.add(toBeAdded);
		classMap.put(className, updatedClass);
	}
	
	public boolean checkFieldName(String className, String fieldName){
		ClassInfo temp = classMap.get(className);
		return temp.fields.containsKey(fieldName);
	}
	
	public void addFieldToClass(String className, String fieldName, String fieldType){
		ClassInfo updatedClass = classMap.get(className);
		Fields newField = new Fields();
		newField.fieldName = fieldName;
		newField.type = fieldType;
		updatedClass.fields.put(fieldName, newField);
		classMap.put(className, updatedClass);
	}
	
	public void addFieldToObjectTable(String className, String fieldName, String fieldType){
		ClassInfo updatedClass = classMap.get(className);
		updatedClass.objectFieldTable.add(fieldName);
		classMap.put(className, updatedClass);
	}
	
	public String checkAndGetIdType(String className, String methodName, String identifier){
		ClassInfo curClass = classMap.get(className);
		Method method = curClass.methods.get(methodName);
		if(method.vars.containsKey(identifier))
			return method.varTypes.get(identifier).type;
		if(method.argNames.contains(identifier)){
			int index = method.argNames.indexOf(identifier);
			return method.argTypes.get(index);
		}
		if(curClass.fields.containsKey(identifier))
			return curClass.fields.get(identifier).type;
		
		ClassInfo temp = curClass;
		while(!temp.extendsClassName.equals("")){
			ClassInfo ParentClass = classMap.get(temp.extendsClassName);
			
			if(ParentClass.fields.containsKey(identifier))
				return ParentClass.fields.get(identifier).type;
			
			temp = ParentClass;
		}
		return null;
	}
	
	public void addVarToMethod(String className, String methodName, String fieldName, String fieldType){
		Method updatedMethod = classMap.get(className).methods.get(methodName);
		Fields newField = new Fields();
		newField.fieldName = fieldName;
		newField.type = fieldType;
		updatedMethod.varTypes.put(fieldName, newField);
		classMap.get(className).methods.put(methodName, updatedMethod);
	}
	
	public void addTempToMethod(String className, String methodName, String fieldName, Integer curTemp){
		Method updatedMethod = classMap.get(className).methods.get(methodName);
		updatedMethod.vars.put(fieldName, curTemp);
	}
	
}

class ClassInfo{

	String className;
	String extendsClassName;
	ArrayList<String> childClasses;
	ArrayList<String>  parentClasses;
	LinkedHashMap<String,Fields> fields;
	LinkedHashMap<String,Method> methods;
	ArrayList<String> objectFieldTable;
	ArrayList<String> vTable;
	Integer objectFieldCount;

	public ClassInfo(String name, String extendsName){
		className = name;
		extendsClassName = extendsName;
		childClasses = new ArrayList<String>();
		parentClasses = new ArrayList<String>();
		fields = new LinkedHashMap<String,Fields>();
		methods = new LinkedHashMap<String,Method>();
		objectFieldTable = new ArrayList<String>();
		objectFieldCount = 0;
		vTable = new ArrayList<String>();
	}
	
}

class Fields{
	
	String fieldName;
	String type;
	
	public Fields(){
		fieldName = "";
		type = "";
	}
}

class Method{
	
	String methodName;
	String returnType;
	ArrayList<String> argTypes;
	ArrayList<String> argNames;
	HashMap<String,Integer> vars;
	HashMap<String,Fields> varTypes;
	
	public Method(){
		methodName = "";
		returnType = "";
		argTypes = new ArrayList<String>();
		argNames = new ArrayList<String>();
		vars = new HashMap<String,Integer>();
		varTypes = new HashMap<String,Fields>();
	}
}
	