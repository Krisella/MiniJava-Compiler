import syntaxtree.*;
import visitor.GJDepthFirst;

public class FirstPassVisitor extends GJDepthFirst<String, String> {

	public SymbolTable symbolTable;
	
	public FirstPassVisitor(){
		symbolTable = new SymbolTable();
	}
	
	public String visit(Identifier id, String argu) throws Exception{
		return id.f0.toString();
	}
	
	public String visit(ClassDeclaration n, String argu) throws Exception{
		String className = n.f1.accept(this, null);
		if(symbolTable.classMap.containsKey(className))
			throw new Exception("Class " + className + " already defined ");
		ClassInfo newClass = new ClassInfo(className, "");
		symbolTable.classMap.put(className, newClass);
		
		return className;
	}
	
	public String visit(ClassExtendsDeclaration n, String argu) throws Exception{
		
		String className = n.f1.accept(this, null);
		if(symbolTable.classMap.containsKey(className))
			throw new Exception(" Class " + className + " already defined ");
		String extendsName = n.f3.accept(this, null);
		if(!symbolTable.classMap.containsKey(extendsName))
			throw new Exception("Extended class " + extendsName + " not defined ");
		ClassInfo newClass = new ClassInfo(className, extendsName);
		symbolTable.AddChildClass(extendsName, className);
		symbolTable.classMap.put(className, newClass);
		
		return className;
	}
	
	public String visit(MainClass n, String argu) throws Exception{
		String className = n.f1.accept(this, null);
		if(symbolTable.classMap.containsKey(className))
			throw new Exception("Class " + className + " already defined ");
		ClassInfo newClass = new ClassInfo(className, "");
		symbolTable.classMap.put(className, newClass);
		
		return className;
	}
}
