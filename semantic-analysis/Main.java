import syntaxtree.*;
import visitor.*;
import java.io.*;

class Main {
    public static void main (String [] args){

	FileInputStream fis = null;
	for(int i = 0; i<args.length; i++){
		
		try{
	    	fis = new FileInputStream(args[i]);}
		catch(FileNotFoundException ex){
		    System.err.println(ex.getMessage());
		}
		try{
			System.out.println("Checking file: " + args[i]);
		    MiniJavaParser parser = new MiniJavaParser(fis);
		    System.err.println("Program parsed successfully.");
		    FirstPassVisitor eval = new FirstPassVisitor();
		    Goal root = parser.Goal();
		    root.accept(eval, null);
		    SecondPassVisitor second = new SecondPassVisitor(eval.symbolTable);
		    root.accept(second,null);
		    ThirdPassVisitor third = new ThirdPassVisitor(second.symbolTable);
		    root.accept(third,null);
		    System.out.println("No Errors");
		    System.out.println();
		}
		catch(ParseException ex){
		    System.out.println(ex.getMessage());
		}
		
		catch(Exception exception){
			System.out.println(exception.getMessage());
			System.out.println();
		}
		finally{
		    try{
			if(fis != null) fis.close();
		    }
		    catch(IOException ex){
			System.err.println(ex.getMessage());
		    }
		}
		}

    }
}
