import syntaxtree.*;
import visitor.*;
import java.io.*;

class Main {
    public static void main (String [] args){

	FileInputStream fis = null;
	PrintWriter out = null;
	for(int i = 0; i<args.length; i++){
		
		try{
	    	fis = new FileInputStream(args[i]);
	    	System.out.println(args[i]);
	    	String[] arr = args[i].split(".java");
	    	String file = arr[0] + ".spg";
	    	out = new PrintWriter(file);}
		catch(FileNotFoundException ex){
		    System.err.println(ex.getMessage());
		}
		try{
			System.out.println("Generating intermediate code for file: " + args[i]);
		    MiniJavaParser parser = new MiniJavaParser(fis);

		    FirstPassVisitor eval = new FirstPassVisitor();
		    Goal root = parser.Goal();
		    root.accept(eval, null);
		    SpigletGenPass second = new SpigletGenPass(eval.symbolTable, eval.maxClassArgs);
		    root.accept(second,null);
		   
		    out.print(second.output.toString());
		    out.close();
			System.out.println("Code Generated Successfully");
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
