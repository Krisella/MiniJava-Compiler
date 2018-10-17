import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class SecondPassVisitor extends GJDepthFirst<String, String> {
	
	public SymbolTable symbolTable;

	public SecondPassVisitor(SymbolTable s){
		symbolTable = s;
	}
	
	public String visit(Identifier id, String argu) throws Exception{
		return id.f0.toString();
	}
	
	public String visit(Type t, String argu) throws Exception{
		String temp = t.f0.accept(this,null);
		if(!temp.equals("int[]") && !temp.equals("boolean") && !temp.equals("int"))
			if(!symbolTable.classMap.containsKey(temp))
				throw new Exception(temp + " does not name a type");
		return temp;
	}
	
	public String visit(ArrayType ar, String argu) throws Exception{
		return "int[]";
	}
	
	public String visit(BooleanType bool, String argu) throws Exception{
		return "boolean";
	}
	
	public String visit(IntegerType i, String argu) throws Exception{
		return "int";
	}
	
	public String visit(VarDeclaration v, String className) throws Exception{
		
		String fieldName = v.f1.accept(this,null);
		if(symbolTable.checkFieldName(className, fieldName))
			throw new Exception("FieldName " + fieldName + " in class " + className + " already declared");
		String fieldType = v.f0.accept(this,null);
		if(!fieldType.equals("int[]") && !fieldType.equals("boolean") && !fieldType.equals("int"))
			if(!symbolTable.classMap.containsKey(fieldType))
				throw new Exception("Class name " + fieldType + " has not been declared");
		symbolTable.addFieldToClass(className, fieldName, fieldType);
		return fieldName;
	}
	
	public String visit(FormalParameterTerm p, String argu) throws Exception{
		return p.f1.accept(this,null);
	}
	
	public String visit(FormalParameter p, String argu) throws Exception{
		String type = p.f0.accept(this,null);
		String name = p.f1.accept(this,null);
		return type + "," + name;
	}
	
	public String visit(FormalParameterTail t, String argu) throws Exception{
		String args = "";
		if(t.f0.present()){
			for(Node node: t.f0.nodes){
				args += node.accept(this,null);
				args += ",";
			}
			return args;
		}
		return null;
	}
	
	public String visit(FormalParameterList l, String argu) throws Exception{
		
		String args = "";
		args += l.f0.accept(this,null);
		String args2;
		args2 = l.f1.accept(this,null);
		if(args2!=null){
			args += ",";
			args += args2;
		}
		return args;
	}
	
	public String visit(MethodDeclaration m, String className) throws Exception{
		
		ArrayList<String> args_array = new ArrayList<String>();
		ClassInfo curClass = symbolTable.classMap.get(className);

		String methodName = m.f2.accept(this,null);
		String returnType = m.f1.accept(this,null);
		
		String args = m.f4.accept(this,null);
		
		if(curClass.methods.containsKey(methodName))
			throw new Exception("multiple declaration of function " + methodName);
		ClassInfo temp = curClass;
		if(args!=null)
			args_array = new ArrayList<String>(Arrays.asList(args.split(",")));

		while(!temp.extendsClassName.equals("")){
			ClassInfo parentClass = symbolTable.classMap.get(temp.extendsClassName);
			if(parentClass.methods.containsKey(methodName)){
				
				Method temp_method = parentClass.methods.get(methodName);
				
				if (temp_method.returnType.equals(returnType)){
					
					if(args!=null)
						if(args_array.size()/2 == temp_method.argTypes.size()){
							int k=0;
							for(int i=0; i<temp_method.argTypes.size(); i++){
								if(!temp_method.argTypes.get(i).equals(args_array.get(k)))
									throw new Exception("Invalid redeclaration of method " + methodName + " , already defined in class " + parentClass.className);
								k+=2;
							}
								
						}else
							throw new Exception("Invalid redeclaration of method " + methodName + " , already defined in class " + parentClass.className);
				}
				else
					throw new Exception("Invalid redeclaration of method " + methodName + " , already defined in class " + parentClass.className);
			}
			temp = parentClass;
		}

		if(args!=null){
			Method method = new Method();
			for(int i=0; i<args_array.size(); i+=2){
				method.argTypes.add(args_array.get(i));
				method.argNames.add(args_array.get(i+1));
			}
			method.returnType = returnType;
			method.methodName = methodName;
			curClass.methods.put(methodName, method);
			symbolTable.classMap.put(className, curClass);
		}else{
			Method method = new Method();
			method.returnType = returnType;
			method.methodName = methodName;
			curClass.methods.put(methodName, method);
			symbolTable.classMap.put(className, curClass);
		}
		
		return className;
	}
	
	public String visit(ClassDeclaration n, String argu) throws Exception{
		
		String className = n.f1.accept(this,null);
		if(n.f3.present()){
			for(Node node: n.f3.nodes)
				node.accept(this,className);
		}
		
		if(n.f4.present()){
			for(Node node: n.f4.nodes)
				node.accept(this,className);
		}
		return argu;
	}
	
	public String visit(ClassExtendsDeclaration n, String argu) throws Exception{
		String className = n.f1.accept(this,null);
		if(n.f5.present()){
			for(Node node: n.f5.nodes)
				node.accept(this,className);
		}
		
		if(n.f6.present()){
			for(Node node: n.f6.nodes)
				node.accept(this,className);
		}
		return argu;
	}
	
	public String visit(MainClass n, String argu) throws Exception{
		String className = n.f1.accept(this,null);
		if(n.f14.present()){
			for(Node node: n.f14.nodes)
				node.accept(this,className);
		}
		
		ClassInfo main = symbolTable.classMap.get(className);
		Method method = new Method();
		method.methodName = "Main";
		main.methods.put(method.methodName, method);

		return argu;
	}
}


