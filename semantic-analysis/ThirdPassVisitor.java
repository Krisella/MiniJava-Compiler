import syntaxtree.*;
import visitor.GJDepthFirst;
import java.util.*;

public class ThirdPassVisitor extends GJDepthFirst<String,String> {

	public SymbolTable symbolTable;

	public ThirdPassVisitor(SymbolTable s){
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
	
	public String visit(VarDeclaration v, String argu) throws Exception{
		String fieldName = v.f1.accept(this,null);
		String fieldType = v.f0.accept(this,null);
		return fieldType + "," + fieldName;
	}
	
	public String visit(MethodDeclaration m, String className) throws Exception{
		String pair;
		String type, name;
		String methodName = m.f2.accept(this,null);
		if(m.f7.present()){
			for(Node node: m.f7.nodes){
				pair = node.accept(this,className);
				String[] arr = pair.split(",");
				type = arr[0];
				name = arr[1];
				if(symbolTable.classMap.get(className).methods.get(methodName).argNames.contains(name))
					throw new Exception("Duplicate local variable " + name);
				symbolTable.addVarToMethod(className, methodName, name, type);
			}
		}
		if(m.f8.present()){
			for(Node node: m.f8.nodes){
				node.accept(this, className + "," + methodName);
			}
		}
		
		String returnType = m.f10.accept(this, className + "," + methodName);
		String methodReturn = symbolTable.classMap.get(className).methods.get(methodName).returnType;
		int found = 0;
		
		if(returnType.equals("int") || returnType.equals("int[]") || returnType.equals("boolean")){
			if(!returnType.equals(methodReturn))
				throw new Exception("Type mismatch: cannot convert from " + returnType + " to " +  symbolTable.classMap.get(className).methods.get(methodName).returnType);
		}else{
			if(!returnType.equals(methodReturn)){
				if(!methodReturn.equals("int") && !methodReturn.equals("int[]") && !methodReturn.equals("boolean")){
					ClassInfo temp = symbolTable.classMap.get(returnType);
					while(!temp.extendsClassName.equals("")){
						ClassInfo parentClass = symbolTable.classMap.get(temp.extendsClassName);
						if(parentClass.className.equals(methodReturn)){
							found = 1;
							break;
						}
					}
					if(found == 0)
						throw new Exception("Type mismatch: cannot convert from " + returnType + " to " + methodReturn);
				}
				else
					throw new Exception("Type mismatch: cannot convert from " + returnType + " to " + methodReturn);

			}
		}
		
		return className;
	}
	
	public String visit(ClassDeclaration n, String argu) throws Exception{
		String className = n.f1.accept(this,null);
		if(n.f4.present()){
			for(Node node: n.f4.nodes)
				node.accept(this,className);
		}
		return argu;
	}
	
	public String visit(ClassExtendsDeclaration n, String argu) throws Exception{
		String className = n.f1.accept(this,null);
		if(n.f6.present()){
			for(Node node: n.f6.nodes)
				node.accept(this,className);
		}
		return argu;
	}
	
	public String visit(MainClass n, String argu) throws Exception{
		String className = n.f1.accept(this,null);
		if(n.f15.present()){
			for(Node node: n.f15.nodes){
				node.accept(this, className);
			}
		}
		return argu;
	}
	
	public String visit(Statement s, String classAndMethod) throws Exception{
		return s.f0.accept(this, classAndMethod);
	}
	
	public String visit(AssignmentStatement s, String classAndMethod) throws Exception{
		String[] arr = classAndMethod.split(",");
		String methodName = "";
		String className = arr[0];
		if(arr.length > 1)
			methodName = arr[1];
		else
			methodName = "Main";
		int found = 0;
		
		String id = s.f0.accept(this,classAndMethod);
		String idType = symbolTable.checkAndGetIdType(className, methodName, id);
		if(idType == null)
			throw new Exception(id + " cannot be resolved to a variable");
		
		String expr_type = s.f2.accept(this,classAndMethod);
		
		if(idType.equals("int") || idType.equals("int[]") || idType.equals("boolean")){
			if(!idType.equals(expr_type))
				throw new Exception("Type mismatch: cannot convert from " + expr_type + " to " + idType);
		}else{
			if(!idType.equals(expr_type)){
				if(!expr_type.equals("int") && !expr_type.equals("int[]") && !expr_type.equals("boolean")){
					ClassInfo temp = symbolTable.classMap.get(expr_type);
					while(!temp.extendsClassName.equals("")){
						ClassInfo parentClass = symbolTable.classMap.get(temp.extendsClassName);
						if(parentClass.className.equals(idType)){
							found = 1;
							break;
						}
					}
					if(found == 0)
						throw new Exception("Type mismatch: cannot convert from " + expr_type + " to " + idType);
				}
				else
					throw new Exception("Type mismatch: cannot convert from " + expr_type + " to " + idType);

			}
		}
		return classAndMethod;
	}
	
	public String visit(ArrayAssignmentStatement a, String classAndMethod) throws Exception{
		
		List<String> arr = new ArrayList<String>(Arrays.asList(classAndMethod.split(",")));
		if(arr.size() == 1)
			arr.add("Main");
		String id = a.f0.accept(this, classAndMethod);
		String idType = symbolTable.checkAndGetIdType(arr.get(0), arr.get(1), id);
		if(idType == null)
			throw new Exception(id + " cannot be resolved to a type");
		
		String exp = a.f2.accept(this, classAndMethod);
		if(!exp.equals("int"))
			throw new Exception("Type mismatch: cannot convert from " + exp + " to int");
		
		String exp2 = a.f5.accept(this, classAndMethod);
		if(!exp2.equals("int"))
			throw new Exception("Type mismatch: cannot convert from " + exp2 + " to int");

		return classAndMethod;
	}
	
	public String visit(IfStatement i, String classAndMethod) throws Exception{
		
		String exp_type = i.f2.accept(this, classAndMethod);
		if(!exp_type.equals("boolean"))
			throw new Exception("Type mismatch: cannot convert from " + exp_type + " to boolean");
		
		i.f4.accept(this, classAndMethod);
		i.f6.accept(this, classAndMethod);
		
		return classAndMethod;
	}
	
	public String visit(WhileStatement w, String classAndMethod) throws Exception{
		
		String exp_type = w.f2.accept(this, classAndMethod);
		if(!exp_type.equals("boolean"))
			throw new Exception("Type mismatch: cannot convert from " + exp_type + " to boolean");
		
		w.f4.accept(this, classAndMethod);
		
		return classAndMethod;
	}
	
	public String visit(PrintStatement p, String classAndMethod) throws Exception{
		
		String exp_type = p.f2.accept(this, classAndMethod);
		if(!exp_type.equals("int"))
			throw new Exception("Type mismatch: cannot convert from " + exp_type + " to int");
		
		return classAndMethod;
	}
	
	public String visit(Expression exp, String classAndMethod) throws Exception{
		return exp.f0.accept(this, classAndMethod);
	}
	
	public String visit(PrimaryExpression p, String classAndMethod) throws Exception{
		List<String> arr = new ArrayList<String>(Arrays.asList(classAndMethod.split(",")));
		if(arr.size() == 1)
			arr.add("Main");
		if(p.f0.which == 0)
			return "int";
		if(p.f0.which == 1 || p.f0.which == 2)
			return "boolean";
		if(p.f0.which == 3){
			String id = p.f0.accept(this, classAndMethod);
			String type = symbolTable.checkAndGetIdType(arr.get(0), arr.get(1), id);
			if(type == null)
				throw new Exception(id + " cannot be resolved to a type");
			return type;
		}
		if(p.f0.which == 4){
			return arr.get(0);
		}
		
		return p.f0.accept(this,classAndMethod);
	}
	
	public String visit(ArrayAllocationExpression a, String classAndMethod) throws Exception{
		
		String type = a.f3.accept(this,classAndMethod);
		if(!type.equals("int"))
			throw new Exception("Type mismatch: cannot convert from " + type + " to int");
		return "int[]";
	}
	
	public String visit(AllocationExpression a, String classAndMethod) throws Exception{
		
		String id = a.f1.accept(this,classAndMethod);
		if(!symbolTable.classMap.containsKey(id))
			throw new Exception(id + " cannot be resolved to a type");
		return id;
	}
	
	public String visit(NotExpression n, String classAndMethod) throws Exception{
		
		String type = n.f1.accept(this,classAndMethod);
		if(!type.equals("boolean"))
			throw new Exception("The operator ! is undefined for the arguments type " + type);
		return "boolean";
	}
	
	public String visit(Clause c, String classAndMethod) throws Exception{
		return c.f0.accept(this,classAndMethod);
	}
	
	public String visit(AndExpression a, String classAndMethod) throws Exception{
		String type1 = a.f0.accept(this, classAndMethod);
		String type2 = a.f2.accept(this, classAndMethod);
		if(!type1.equals("boolean") || !type2.equals("boolean"))
			throw new Exception("The operator && is undefined for the argument types " + type1 + ", " + type2);
		return "boolean";
	}
	
	public String visit(CompareExpression c, String classAndMethod) throws Exception{
		String type1 = c.f0.accept(this,classAndMethod);
		String type2 = c.f2.accept(this,classAndMethod);
		if(!type1.equals("int") || !type2.equals("int"))
			throw new Exception("The operator < is undefined for the argument types " + type1 + ", " + type2);
		return "boolean";
	}
	
	public String visit(PlusExpression p, String classAndMethod) throws Exception{
		String type1 = p.f0.accept(this,classAndMethod);
		String type2 = p.f2.accept(this,classAndMethod);
		if(!type1.equals("int") || !type2.equals("int"))
			throw new Exception("The operator + is undefined for the argument types " + type1 + ", " + type2);
		return "int";
	}
	
	public String visit(MinusExpression m, String classAndMethod) throws Exception{
		String type1 = m.f0.accept(this,classAndMethod);
		String type2 = m.f2.accept(this,classAndMethod);
		if(!type1.equals("int") || !type2.equals("int"))
			throw new Exception("The operator - is undefined for the argument types " + type1 + ", " + type2);
		return "int";
	}
	
	public String visit(TimesExpression t, String classAndMethod) throws Exception{
		String type1 = t.f0.accept(this,classAndMethod);
		String type2 = t.f2.accept(this,classAndMethod);
		if(!type1.equals("int") || !type2.equals("int"))
			throw new Exception("The operator * is undefined for the argument types " + type1 + ", " + type2);
		return "int";
	}
	
	public String visit(ArrayLookup a, String classAndMethod) throws Exception{
		String type1 = a.f0.accept(this, classAndMethod);
		if(!type1.equals("int[]"))
			throw new Exception("The type of the expression must be an array type but it resolved to " + type1);
		String type2 = a.f2.accept(this, classAndMethod);
		if(!type2.equals("int"))
			throw new Exception("Type mismatch: cannot convert from " + type2 + " to int");
		return "int";
	}
	
	public String visit(ArrayLength a, String classAndMethod) throws Exception{
		String type = a.f0.accept(this, classAndMethod);
		if(!type.equals("int[]"))
			throw new Exception("The type of the expression must be an array type but it resolved to " + type);
		return "int";
	}
	
	public String visit(MessageSend m, String classAndMethod) throws Exception{
		String classType = m.f0.accept(this, classAndMethod);
		String methodName = m.f2.accept(this, classAndMethod);
		String arg_types = m.f4.accept(this, classAndMethod);
		Method toCompareMethod = new Method();
		String classOfFoundMethod = "";
		int found = 0;
		String[] arg_types_arr = {};
		if(arg_types != null)
			arg_types_arr = arg_types.split(",");
		else
			arg_types = "";
		if(classType.equals("int") || classType.equals("boolean"))
			throw new Exception("Cannot invoke " + methodName + "(" + arg_types + ") on the primitive type " + classType);
		if(classType.equals("int[]"))
			throw new Exception("Cannot invoke " + methodName + "(" + arg_types + ") on the array type int");
		
		ClassInfo curClass = symbolTable.classMap.get(classType);
		if(curClass.methods.containsKey(methodName)){
			found = 1;
			toCompareMethod = curClass.methods.get(methodName);
			classOfFoundMethod = classType;
		}else{
			ClassInfo temp = curClass;
			while(!temp.extendsClassName.equals("")){
				ClassInfo parentClass = symbolTable.classMap.get(temp.extendsClassName);
				
				if(parentClass.methods.containsKey(methodName)){
					found = 1;
					toCompareMethod = parentClass.methods.get(methodName);
					classOfFoundMethod = parentClass.className;
					break;
				}
				temp = parentClass;
			}
		}
		
		if(found == 0)
			throw new Exception("The method " + methodName + "(" + arg_types + ") is undefined for the type " + classType);
		if(!arg_types.equals("")){
			found = 0;
			if(arg_types_arr.length != toCompareMethod.argTypes.size())
				throw new Exception("The method " + methodName + "(" + toCompareMethod.argTypes.toString().replaceAll("\\[|\\]","") + ") in the type " + classOfFoundMethod + " is not applicable for the arguments (" + Arrays.toString(arg_types_arr).replaceAll("\\[|\\]","") + ")");
			for(int i=0; i<arg_types_arr.length; i++){
				if(arg_types_arr[i].equals("int") || arg_types_arr[i].equals("int[]") || arg_types_arr[i].equals("boolean")){
					if(!arg_types_arr[i].equals(toCompareMethod.argTypes.get(i)))
						throw new Exception("The method " + methodName + "(" + toCompareMethod.argTypes.toString().replaceAll("\\[|\\]","") + ") in the type " + classOfFoundMethod + " is not applicable for the arguments (" + Arrays.toString(arg_types_arr).replaceAll("\\[|\\]","") + ")");}
				else{
					if(!arg_types_arr[i].equals(toCompareMethod.argTypes.get(i))){
						if(!toCompareMethod.argTypes.get(i).equals("int") && !toCompareMethod.argTypes.get(i).equals("int[]") && !toCompareMethod.argTypes.get(i).equals("boolean")){
							ClassInfo temp = symbolTable.classMap.get(arg_types_arr[i]);
							while(!temp.extendsClassName.equals("")){
								ClassInfo parentClass = symbolTable.classMap.get(temp.extendsClassName);
								
								if(parentClass.className.equals(toCompareMethod.argTypes.get(i))){
									found = 1;
									break;
								}
								temp = parentClass;
							}
							if(found == 0)
								throw new Exception("The method " + methodName + "(" + toCompareMethod.argTypes.toString().replaceAll("\\[|\\]","") + ") in the type " + classOfFoundMethod + " is not applicable for the arguments (" + Arrays.toString(arg_types_arr).replaceAll("\\[|\\]","") + ")");
						}
						else
							throw new Exception("The method " + methodName + "(" + toCompareMethod.argTypes.toString().replaceAll("\\[|\\]","") + ") in the type " + classOfFoundMethod + " is not applicable for the arguments (" + Arrays.toString(arg_types_arr).replaceAll("\\[|\\]","") + ")");
					}
				}
			}
			}
		
		return toCompareMethod.returnType;
	}
	
	public String visit(ExpressionList l, String classAndMethod) throws Exception{
		String args = "";
		args += l.f0.accept(this,classAndMethod);
		String args2;
		args2 = l.f1.accept(this,classAndMethod);
		if(args2!=null){
			args += ",";
			args += args2;
		}
		return args;
	}
	
	public String visit(ExpressionTail t, String classAndMethod) throws Exception{
		String args = "";
		if(t.f0.present()){
			for(Node node: t.f0.nodes){
				args += node.accept(this,classAndMethod);
				args += ",";
			}
			return args;
		}
		return null;
	}
	
	public String visit(ExpressionTerm t, String classAndMethod) throws Exception{
		return t.f1.accept(this,classAndMethod);
	}
	
	public String visit(BracketExpression b, String classAndMethod) throws Exception{
		return b.f1.accept(this, classAndMethod);
	}
}
