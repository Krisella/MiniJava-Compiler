import java.util.*;

import syntaxtree.*;
import visitor.GJDepthFirst;

public class SpigletGenPass extends GJDepthFirst<String, String>{
	
	public SymbolTable 		symbolTable;
	public StringBuilder	output;
	TempGen					tempGen;
	Integer 				labelCount;
	String 					lastObjectType;
	
	public SpigletGenPass(SymbolTable s, int maxClassArgs) {
		symbolTable = s;
		tempGen = new TempGen(maxClassArgs);
		labelCount = 1;
		output = new StringBuilder();
	}
	
	public String visit(MainClass m, String argu) throws Exception{
		
		int curTemp;
		output.append("MAIN\n");
		
		//List<ClassInfo> classesInfo = new ArrayList<ClassInfo>(symbolTable.classMap.values());	
		
		for(String curClass: symbolTable.classMap.keySet()){
			
			ArrayList<String> vTable = new ArrayList<String>();
			ArrayList<String> methods = new ArrayList<String>();
			ClassInfo classInfo = symbolTable.classMap.get(curClass);
			int fieldCount = classInfo.fields.size();
			
			for(String method: classInfo.methods.keySet()){
				methods.add(method);
			}
			int methodCount = classInfo.methods.size();

			if(classInfo.methods.containsKey("static"))
				continue;
			int label = tempGen.getNextTemp();
			output.append("MOVE TEMP " + String.valueOf(label) + " " + curClass + "_vTable\n");
			
			for(int i = 0; i < classInfo.parentClasses.size(); i++){
				ClassInfo parentClass = symbolTable.classMap.get(classInfo.parentClasses.get(i));
				for(String method: parentClass.methods.keySet()){
					if(classInfo.methods.containsKey(method)){
						vTable.add(classInfo.className + "_" + method);
						classInfo.vTable.add(method);
						methods.remove(method);
					}else{
						vTable.add(parentClass.className + "_" + method);
						classInfo.vTable.add(method);
						methodCount++;
					}
				}
				fieldCount+=parentClass.fields.size();
			}
			classInfo.objectFieldCount = fieldCount;
			for(int i=0; i<methods.size(); i++){
				vTable.add(classInfo.className+"_"+methods.get(i));
				classInfo.vTable.add(methods.get(i));
			}
			int vTableAddr = tempGen.getNextTemp();
			output.append("MOVE TEMP " + String.valueOf(vTableAddr) + " HALLOCATE " 
										+ String.valueOf(methodCount*4) + "\n");
			
			output.append("HSTORE TEMP " + String.valueOf(label) + " 0 TEMP " + String.valueOf(vTableAddr) + "\n");
			
			Integer offset = 0;
			for(int i = 0; i < vTable.size(); i++){
				curTemp = tempGen.getNextTemp();
				output.append("MOVE TEMP " + String.valueOf(curTemp) + " " + vTable.get(i) + "\n");
				output.append("HSTORE TEMP " + String.valueOf(vTableAddr) + " " + offset.toString() + " TEMP " + String.valueOf(curTemp) + "\n");
				offset+=4;
			}
		}
		
		String className = m.f1.accept(this, null);
		if(m.f14.present()){
			for(Node node: m.f14.nodes){
				String pair = node.accept(this,className+",static");
				String[] arr = pair.split(",");
				String type = arr[0];
				String name = arr[1];
				symbolTable.addVarToMethod(className, "static", name, type);
				curTemp = tempGen.getNextTemp();
				symbolTable.addTempToMethod(className, "static", name, curTemp);
			}
		}
		
		if(m.f15.present()){
			for(Node node: m.f15.nodes)
				node.accept(this,className+",static");
		}
		
		output.append("END \n\n");
		return className;
	}
	
	public String visit(ArrayAllocationExpression a, String classAndMethod) throws Exception{
		
		Integer tempSize = Integer.valueOf(a.f3.accept(this, classAndMethod));
		Integer curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+tempSize.toString()+" 0\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		output.append("ERROR\n");
		curTemp = tempGen.getNextTemp();
		output.append("L"+labelCount.toString()+" NOOP\n"+" MOVE TEMP "+curTemp.toString()+" PLUS TEMP "+tempSize.toString()+" 1\n");
		labelCount++;
		Integer allocSize = tempGen.getNextTemp();
		output.append("MOVE TEMP "+allocSize.toString()+" TIMES TEMP "+curTemp.toString()+" 4\n");
		Integer arrayAddr = tempGen.getNextTemp();
		output.append("MOVE TEMP "+arrayAddr.toString()+" HALLOCATE TEMP "+allocSize.toString()+"\n");
		Integer offset = tempGen.getNextTemp();
		output.append("MOVE TEMP "+offset.toString()+" 4\n");
		curTemp = tempGen.getNextTemp();
		output.append("L"+labelCount.toString()+" NOOP\n");
		Integer jumpLabel = labelCount;
		labelCount++;
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+offset.toString()+" TEMP "+allocSize.toString()+"\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		Integer curAddr = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curAddr.toString()+" PLUS TEMP "+arrayAddr.toString()+" TEMP "+offset.toString()+"\n");
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" 0\n");
		output.append("HSTORE TEMP "+curAddr.toString()+" 0 TEMP "+curTemp.toString()+"\n");
		output.append("MOVE TEMP "+offset.toString()+" PLUS TEMP "+offset.toString()+" 4\n");
		output.append("JUMP L"+jumpLabel.toString()+"\n");
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;
		output.append("HSTORE TEMP "+arrayAddr.toString()+" 0 TEMP "+tempSize.toString()+"\n");
		
		return arrayAddr.toString();
	}
	
	public String visit(AllocationExpression a, String classAndMethod) throws Exception{
		
		String classType = a.f1.accept(this, classAndMethod);
		lastObjectType = classType;
		Integer curTemp;
		ClassInfo classInfo = symbolTable.classMap.get(classType);
		int fieldCount = classInfo.objectFieldCount;
		Integer objectSize = fieldCount*4 + 4;
		Integer objectAddr = tempGen.getNextTemp();
		
		output.append("MOVE TEMP "+objectAddr.toString()+" HALLOCATE "+objectSize.toString()+"\n");
		Integer offset = tempGen.getNextTemp();
		output.append("MOVE TEMP "+offset.toString()+" 4\n");
		output.append("L"+labelCount+" NOOP\n");
		Integer jumpLabel = labelCount;
		labelCount++;
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+offset.toString()+" "+objectSize.toString()+"\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		Integer curAddr = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curAddr.toString()+" PLUS TEMP "+objectAddr.toString()+" TEMP "+offset.toString()+"\n");
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" 0\n");
		output.append("HSTORE TEMP "+curAddr.toString()+" 0 TEMP "+curTemp.toString()+"\n");
		output.append("MOVE TEMP "+offset.toString()+" PLUS TEMP "+offset.toString()+" 4\n");
		output.append("JUMP L"+jumpLabel.toString()+"\n");
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" "+classInfo.className+"_vTable\n");
		Integer vTableAddr = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+vTableAddr.toString()+" TEMP "+curTemp.toString()+" 0\n");
		output.append("HSTORE TEMP "+objectAddr.toString()+" 0 TEMP "+vTableAddr.toString()+"\n");
		return objectAddr.toString();
	}
	
	public String visit(ThisExpression t, String argu) throws Exception{
		return Integer.toString(0);
	}
	
	public String visit(TrueLiteral t, String argu) throws Exception{
		Integer temp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+temp.toString()+" 1\n");
		return temp.toString();
	}
	
	public String visit(FalseLiteral f, String argu) throws Exception{
		Integer temp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+temp.toString()+" 0\n");
		return temp.toString();
	}
	
	public String visit(IntegerLiteral i, String argu) throws Exception{
		Integer temp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+temp.toString()+" "+i.f0.toString()+"\n");
		return temp.toString();
	}
	
	public String visit(PrimaryExpression p, String classAndMethod) throws Exception{
		String[] arr = classAndMethod.split(",");
		String className = arr[0];
		String methodName = arr[1];
		if(p.f0.which == 3){
			String id = p.f0.accept(this, classAndMethod);
			ClassInfo classInfo = symbolTable.classMap.get(className);
			Method method = classInfo.methods.get(methodName);
			
			String type = symbolTable.checkAndGetIdType(className, methodName, id);
			if(!type.equals("int") && !type.equals("int[]") && !type.equals("boolean"))
				lastObjectType = type;
			if(method.vars.containsKey(id))
				return method.vars.get(id).toString();
			else if(method.argNames.contains(id))
				return Integer.toString(method.argNames.indexOf(id)+1);
			else{
				int index = classInfo.objectFieldTable.lastIndexOf(id)+1;
				Integer curTemp = tempGen.getNextTemp();
				output.append("HLOAD TEMP "+curTemp.toString()+" TEMP 0 "+Integer.toString(index*4)+"\n" );
				return curTemp.toString();
			}
		}else
			return p.f0.accept(this, classAndMethod);
	}
	
	public String visit(Identifier i, String argu) throws Exception{
		return i.f0.toString();
	}
	
	public String visit(NotExpression n, String argu) throws Exception{
		Integer expr = Integer.valueOf(n.f1.accept(this, argu));
		Integer trueLit = tempGen.getNextTemp();
		output.append("MOVE TEMP "+trueLit.toString()+" 1\n");
		Integer temp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+temp.toString()+" MINUS TEMP "+trueLit.toString()+" TEMP "+expr.toString()+"\n");
		return temp.toString();
	}
	
	public String visit(Expression e, String argu) throws Exception{
		return e.f0.accept(this, argu);
	}
	
	public String visit(AndExpression a, String argu) throws Exception{
		Integer temp1 = Integer.valueOf(a.f0.accept(this, argu));
		Integer temp2 = Integer.valueOf(a.f2.accept(this, argu));
		Integer resTemp = tempGen.getNextTemp();
		
		output.append("MOVE TEMP "+resTemp.toString()+" TEMP "+temp1.toString()+"\n");
		output.append("CJUMP TEMP "+temp1.toString()+" L"+labelCount.toString()+"\n");
		output.append("MOVE TEMP "+resTemp.toString()+" TEMP "+temp2.toString()+"\n");
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;
		return resTemp.toString();
	}
	
	public String visit(CompareExpression c, String argu) throws Exception{
		String temp1 = c.f0.accept(this, argu);
		String temp2 = c.f2.accept(this, argu);
		
		Integer resTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+resTemp.toString()+" LT TEMP "+temp1+" TEMP "+temp2+"\n");
		return resTemp.toString();
	}
	
	public String visit(PlusExpression p, String argu) throws Exception{
		String temp1 = p.f0.accept(this, argu);
		String temp2 = p.f2.accept(this, argu);
		
		Integer resTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+resTemp.toString()+" PLUS TEMP "+temp1+" TEMP "+temp2+"\n");
		return resTemp.toString();
	}
	
	public String visit(MinusExpression m, String argu) throws Exception{
		String temp1 = m.f0.accept(this, argu);
		String temp2 = m.f2.accept(this, argu);
		
		Integer resTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+resTemp.toString()+" MINUS TEMP "+temp1+" TEMP "+temp2+"\n");
		return resTemp.toString();
	}
	
	public String visit(TimesExpression t, String argu) throws Exception{
		String temp1 = t.f0.accept(this, argu);
		String temp2 = t.f2.accept(this, argu);
		
		Integer resTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+resTemp.toString()+" TIMES TEMP "+temp1+" TEMP "+temp2+"\n");
		return resTemp.toString();
	}
	
	public String visit(ArrayLookup a, String argu) throws Exception{
		String arrayAddr = a.f0.accept(this, argu);
		String arrayIndex = a.f2.accept(this, argu);
		Integer resTemp;
		
		Integer curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+arrayIndex+" 0\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		output.append("ERROR\n");
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;
		Integer arrayLength = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+arrayLength.toString()+" TEMP "+arrayAddr+" 0\n");
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+arrayIndex+" TEMP "+arrayLength+"\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		output.append("JUMP L"+Integer.toString(labelCount+1)+"\n");
		output.append("L"+labelCount.toString()+" ERROR\n");
		labelCount++;
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" TIMES TEMP "+arrayIndex+" 4\n");
		Integer offset = tempGen.getNextTemp();
		output.append("MOVE TEMP "+offset.toString()+" PLUS TEMP "+curTemp.toString()+" 4\n");
		Integer loadAddr = tempGen.getNextTemp();
		output.append("MOVE TEMP "+loadAddr.toString()+" PLUS TEMP "+arrayAddr+" TEMP "+offset+"\n");
		resTemp = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+resTemp.toString()+" TEMP "+loadAddr.toString()+" 0\n");
		return resTemp.toString();
	}
	
	public String visit(ArrayLength a, String argu) throws Exception{
		String arrayAddr = a.f0.accept(this, argu);
		Integer resTemp = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+resTemp.toString()+" TEMP "+arrayAddr+" 0\n");
		return resTemp.toString();
	}
	
	public String visit(MessageSend m, String classAndMethod) throws Exception{
		Integer resTemp;
		String objectAddr = m.f0.accept(this, classAndMethod);
		String type = lastObjectType;
		String methodName = m.f2.accept(this, classAndMethod);
		
		ClassInfo classInfo = symbolTable.classMap.get(type);
		int methodIndex = classInfo.vTable.indexOf(methodName);
		Integer vTableOffset = methodIndex*4;
		
		Integer curTemp = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+curTemp.toString()+" TEMP "+objectAddr+" 0\n");
		Integer functionAddr = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+functionAddr.toString()+" TEMP "+curTemp.toString()+" "+vTableOffset.toString()+"\n");
		
		String arg_temps = m.f4.accept(this, classAndMethod);
		String[] arg_temps_arr = {};
		if(arg_temps!=null)
			arg_temps_arr = arg_temps.split(",");
		resTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+resTemp.toString()+" CALL TEMP "+functionAddr.toString()+" ( TEMP "+objectAddr);
		for(String temp: arg_temps_arr)
			output.append(" TEMP "+temp);
		output.append(" )\n");
		return resTemp.toString();
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
	
	public String visit(BracketExpression b, String argu) throws Exception{
		return b.f1.accept(this, argu);
	}
	
	public String visit(Clause c, String argu) throws Exception{
		return c.f0.accept(this, argu);
	}
	
	public String visit(AssignmentStatement a, String classAndMethod) throws Exception{
		String []arr = classAndMethod.split(",");
		String className = arr[0];
		String methodName = arr[1];
		Integer toStoreTemp = 0;
		
		String exprTemp = a.f2.accept(this, classAndMethod);
		String id = a.f0.accept(this, classAndMethod);
		
		ClassInfo classInfo = symbolTable.classMap.get(className);
		Method method = classInfo.methods.get(methodName);
		
		if(method.vars.containsKey(id)){
			toStoreTemp = method.vars.get(id);
			output.append("MOVE TEMP "+toStoreTemp.toString()+" TEMP "+exprTemp+"\n");
		}else if(method.argNames.contains(id)){
			toStoreTemp = method.argNames.indexOf(id) + 1;
			output.append("MOVE TEMP "+toStoreTemp.toString()+" TEMP "+exprTemp+"\n");
		}else if(classInfo.objectFieldTable.contains(id)){
			toStoreTemp = classInfo.objectFieldTable.lastIndexOf(id) + 1;
			Integer offset = toStoreTemp * 4;
			output.append("HSTORE TEMP 0 "+offset.toString()+" TEMP "+exprTemp+"\n");
		}
		return toStoreTemp.toString();
	}
	
	public String visit(ArrayAssignmentStatement a, String classAndMethod) throws Exception{
		String id = a.f0.accept(this, classAndMethod);
		String index = a.f2.accept(this, classAndMethod);
		String exprTemp = a.f5.accept(this,classAndMethod);
		Integer arrayAddr = 0;
		
		String []arr = classAndMethod.split(",");
		String className = arr[0];
		String methodName = arr[1];
		
		ClassInfo classInfo = symbolTable.classMap.get(className);
		Method method = classInfo.methods.get(methodName);

		if(method.vars.containsKey(id)){
			arrayAddr = method.vars.get(id);
		}else if(method.argNames.contains(id)){
			arrayAddr = method.argNames.indexOf(id) + 1;
		}else if(classInfo.objectFieldTable.contains(id)){
			Integer temp = classInfo.objectFieldTable.lastIndexOf(id) + 1;
			Integer offset = temp * 4;
			arrayAddr = tempGen.getNextTemp();
			output.append("HLOAD TEMP "+arrayAddr.toString()+" TEMP 0 "+offset.toString()+"\n");
		}
		
		Integer arrayLength = tempGen.getNextTemp();
		output.append("HLOAD TEMP "+arrayLength.toString()+" TEMP "+arrayAddr.toString()+" 0\n");
		Integer curTemp = tempGen.getNextTemp();
		
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+index+" 0\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		output.append("ERROR\n");
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;

		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" LT TEMP "+index+" TEMP "+arrayLength+"\n");
		output.append("CJUMP TEMP "+curTemp.toString()+" L"+labelCount.toString()+"\n");
		Integer cjumpLabel = labelCount;
		labelCount++;
		output.append("JUMP L"+Integer.toString(labelCount)+"\n");
		Integer jumpLabel = labelCount;
		labelCount++;
		output.append("L"+cjumpLabel.toString()+" ERROR\n");
		output.append("L"+jumpLabel.toString()+" NOOP\n");
		curTemp = tempGen.getNextTemp();
		output.append("MOVE TEMP "+curTemp.toString()+" TIMES TEMP "+index+" 4\n");
		Integer offset = tempGen.getNextTemp();
		output.append("MOVE TEMP "+offset.toString()+" PLUS TEMP "+curTemp.toString()+" 4\n");
		Integer loadAddr = tempGen.getNextTemp();
		output.append("MOVE TEMP "+loadAddr.toString()+" PLUS TEMP "+arrayAddr+" TEMP "+offset+"\n");
		output.append("HSTORE TEMP "+loadAddr.toString()+" 0 TEMP "+exprTemp.toString()+"\n");
		return arrayAddr.toString();
	}
	
	public String visit(IfStatement i, String argu) throws Exception{
		
		String exprTemp = i.f2.accept(this, argu);
		Integer cjumpLabel;
		Integer jumpLabel;
		output.append("CJUMP TEMP "+exprTemp.toString()+" L"+labelCount.toString()+"\n");
		cjumpLabel = labelCount;
		labelCount++;
		i.f4.accept(this, argu);
		output.append("JUMP L"+Integer.toString(labelCount)+"\n");
		jumpLabel = labelCount;
		labelCount++;
		output.append("L"+cjumpLabel.toString()+" NOOP\n");
		i.f6.accept(this, argu);
		output.append("L"+jumpLabel.toString()+" NOOP\n");
		return exprTemp;
	}
	
	public String visit(WhileStatement w, String argu) throws Exception{
		
		Integer jumpLabel = labelCount;
		output.append("L"+labelCount.toString()+" NOOP\n");
		labelCount++;
		String exprTemp = w.f2.accept(this, argu);
		output.append("CJUMP TEMP "+exprTemp.toString()+" L"+Integer.toString(labelCount)+"\n");
		Integer cjumpLabel = labelCount;
		labelCount++;
		w.f4.accept(this, argu);
		output.append("JUMP L"+jumpLabel.toString()+"\n");
		output.append("L"+cjumpLabel.toString()+" NOOP\n");
		return exprTemp;
	}
	
	public String visit(PrintStatement p, String argu) throws Exception{
		String tempExpr = p.f2.accept(this, argu);
		output.append("PRINT TEMP "+tempExpr.toString()+"\n");
		return tempExpr.toString();
	}
	
	public String visit(Block b, String argu) throws Exception{
		
		for(Node n: b.f1.nodes){
			n.accept(this, argu);
		}
		return argu;
	}
		
	public String visit(MethodDeclaration m, String className) throws Exception{
		
		String pair;
		String type, name;
		String methodName = m.f2.accept(this,null);
		Integer curTemp;
		Method method = symbolTable.classMap.get(className).methods.get(methodName);

		if(m.f7.present()){
			for(Node node: m.f7.nodes){
				pair = node.accept(this,className);
				String[] arr = pair.split(",");
				type = arr[0];
				name = arr[1];
				symbolTable.addVarToMethod(className, methodName, name, type);
				curTemp = tempGen.getNextTemp();
				symbolTable.addTempToMethod(className, methodName, name, curTemp);
			}
		}
		
		output.append(className+"_"+methodName+" [ "+Integer.toString(method.argNames.size()+1)+" ]\n");
		output.append("BEGIN\n");
		if(m.f8.present()){
			for(Node n: m.f8.nodes)
				n.accept(this,className+","+methodName);
		}
		
		String exprTemp = m.f10.accept(this, className+","+methodName);
		output.append("RETURN TEMP "+exprTemp.toString()+"\n");
		output.append("END\n\n");
		return className;
	}
	
	public String visit(Type t, String argu) throws Exception{
		return t.f0.accept(this,null);
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
}


