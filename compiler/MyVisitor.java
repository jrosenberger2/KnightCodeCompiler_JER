/**
 * MyVisitor is the method visitor used by kcc.java to walk the parse tree, it returns a string of java asm code
 * @author Jared Rosenberger
 * @version 2.3
 * Assignment 5
 * CS322 - Compiler Construction
 * Spring 2024
 */
package compiler;
import org.antlr.v4.runtime.tree.ParseTree;
//import org.objectweb.asm.Opcodes;
import lexparse.KnightCodeBaseVisitor;
import lexparse.KnightCodeParser;
import lexparse.KnightCodeParser.*;

public class MyVisitor extends KnightCodeBaseVisitor<String>{
    private SymbolTable symbols;//Symbol Table to hold identifiers and vartypes
    private SymbolTable index;//Symbol Table to hold identifiers and index locations
    //private File output;
    //private String asmCode;//output string for asm code
    private int storageIndex;//index for bytecode storage

    /**
     * One arg constructor allows a symbol table to be passed in
     * @param sym is the symbol table passed into MyVisitor
     */
    public MyVisitor(SymbolTable sym) {
        symbols = sym;
        //asmCode = "";
        storageIndex = 1;
        index = new SymbolTable();
    }//end Constructor

    /*  
    /**
     * 2 arg constructor allwos a symbol table and file to be passed in
     * @param sym
     * @param out
     
    public MyVisitor(SymbolTable sym, File out) {
        symbols = sym;
        output = out;
        asmCode = "";
        storageIndex = 1;
        index = new SymbolTable();
    }//end Constructor
    */

    /**
     * {@inheritDoc}
     * visitFile is used to start walking the parse tree by kcc.java
     */
    @Override
    public String visitFile(KnightCodeParser.FileContext ctx) { 
        System.out.println("Walking parse tree...");
        visitDeclare((DeclareContext) ctx.getChild(2));
        return visitBody((BodyContext) ctx.getChild(3));
    }//end visitFile

    /**
     * {@inheritDoc}
     * visitDeclare is used put identifiers and vartypes into the symbol table
     */
    @Override
    public String visitDeclare(KnightCodeParser.DeclareContext ctx) {
        System.out.println("visiting DECLARE");
        //System.out.println("Declare has " + ctx.getChildCount() + " kids");
        if(ctx.getChildCount() >=2) {
            for(int i = 1; i<ctx.getChildCount(); i++) {
                ParseTree tempNode = ctx.getChild(i);
                //System.out.println("Entered a Var with " + tempNode.getChildCount() + " children." + tempNode.getText());
                if(tempNode.getChild(0).getText().compareTo("INTEGER") == 0) {
                    Variable temp = new Variable(0);
                    String idName = visitIdentifier((IdentifierContext) tempNode.getChild(1));
                    symbols.addEntry(idName, temp);
                }
                else {
                    Variable temp = new Variable("\"\"");
                    String idname = visitIdentifier((IdentifierContext) tempNode.getChild(1));
                    symbols.addEntry(idname, temp);
                }
            }
        }
        //System.out.println("Symbol Table after declare:");
        //symbols.print();
        return "";//visitChildren(ctx);
    }//end visitDeclare

    /**
     * {@inheritDoc}
     * visitBody is used to walk the rest of the tree and return suitable asm code for what is in the kc program
     */
    @Override 
    public String visitBody(KnightCodeParser.BodyContext ctx) { 
        System.out.println("visiting Body");
        //System.out.println(ctx.toStringTree());
        String code = "";
        //Loop visits every stat child of the body
        for(int i = 1; i<ctx.getChildCount()-1; i++) {
            ParseTree tempNode = ctx.getChild(i);//the stat child
            //System.out.println(tempNode.toStringTree());
            code += visitStat((StatContext) tempNode);
        }
        System.out.println("Done!");
        return code; 
    }//end visitBody

    /**
     * {@inheritDoc}
     * visitStat is used to visit all statements and return apropriate asm code
     */
    @Override 
    public String visitStat(KnightCodeParser.StatContext ctx) { 
        System.out.println("Visiting STAT");
        String code = "";
        ParseTree childNode = ctx.getChild(0);
        String childContent = childNode.getText();
        //System.out.println("childNode is " + childNode.hashCode());
        //Checks for setVar keyword
        if(childContent.substring(0, 3).equals("SET")) {
            System.out.println("Found SET");
            code += visitSetvar((SetvarContext) childNode);
        }
        //Checks for PRINT keyword
        else if(childContent.substring(0,5).equals("PRINT")) {
            System.out.println("Found PRINT");
            code += visitPrint((PrintContext) childNode);
        }
        //Checks for READ keyword
        else if(childContent.substring(0,4).equals("READ")) {
            System.out.println("Found READ");
            code += visitRead((ReadContext) childNode);
        }
        //Checks for IF keyword
        else if(childContent.substring(0,2).equals("IF")) {
            System.out.println("Found IF");
            code += visitDecision((DecisionContext) childNode);
        }
         //Checks for WHILE keyword
         else if(childContent.substring(0,5).equals("WHILE")) {
            System.out.println("Found WHILE");
            code += visitLoop((LoopContext) childNode);
        }
        //Checks for MULT symbol
        else if(childContent.indexOf('*') != -1) {
            System.out.println("Found MULT");
            code += visitMultiplication((MultiplicationContext) childNode);
        }
        //Checks for DIV symbol
        else if(childContent.indexOf('/') != -1) {
            System.out.println("Found DIV");
            code += visitDivision((DivisionContext) childNode);
        }
        //Checks for ADD symbol
        else if(childContent.indexOf('+') != -1) {
            System.out.println("Found ADD");
            code += visitAddition((AdditionContext) childNode);
        }
        //Checks for SUB symbol
        else if(childContent.indexOf('-') != -1) {
            System.out.println("Found SUB");
            code += visitSubtraction((SubtractionContext) childNode);
        }
        //Checks for COMP symbol
        else if(childContent.indexOf('>') != -1 || childContent.indexOf('<') != -1 || childContent.indexOf('=') != -1 || childContent.indexOf("<>") != -1) {
            System.out.println("Found COMP");
            code += visitComp((CompContext) childNode);
        }
        return code;
    }//end visitStat

    /**
     * {@inheritDoc}
     * visitSetvar is used to set the values of any variable in the symbol table and/or its asm index in the index table
     */
    @Override 
    public String visitSetvar(KnightCodeParser.SetvarContext ctx) { 
        System.out.println("Visiting SET");
        String code = "";
        String id = ctx.getChild(1).getText();
        //System.out.println("ID Token: " + id);
        String exprTxt = ctx.getChild(3).getText();
        //Checks for var set to one number/String
        if(symbols.getValue(id) != null && ctx.getChild(3).getChildCount() == 1) {
            Variable temp = symbols.getValue(id);
            if(!temp.isString()) {
                temp.setInt((Integer.parseInt(ctx.getChild(3).getChild(0).getText())));
                symbols.addEntry(id, temp);
            }
            else {
                temp.setString(exprTxt);
                symbols.addEntry(id, temp);
            }
        }
        //Next section checks for expr keywords and calls the appropriate method
        else if(exprTxt.indexOf('*') != -1) {
            System.out.println("Found MULT");
            code += visitMultiplication((MultiplicationContext)ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
            else 
                index.getValue(id).setInt(storageIndex-1);
        }
        else if(exprTxt.indexOf('/') != -1) {
            System.out.println("Found DIV");
            code += visitDivision((DivisionContext) ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
            else 
                index.getValue(id).setInt(storageIndex-1);
        }
        else if(exprTxt.indexOf('+') != -1) {
            System.out.println("Found ADD");
            code += visitAddition((AdditionContext) ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
            else 
                index.getValue(id).setInt(storageIndex-1);
        }
        else if(exprTxt.indexOf('-') != -1) {
            System.out.println("Found SUB");
            code += visitSubtraction((SubtractionContext) ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
            else 
                index.getValue(id).setInt(storageIndex-1);
        }
        return code; 
    }//end visitSetvar

    /**
     * {@inheritDoc}
     * visitPrint is used to return asm code for printing a string or variable
     */
    @Override 
    public String visitPrint(KnightCodeParser.PrintContext ctx) { 
        System.out.println("Visiting PRINT");
        //System.out.println("print has " + ctx.getChildCount());
        String content = ctx.getChild(1).getText();
        //System.out.println(content);
        String code = "";
        //Checks for saved variable
        //try {
        if(index.getValue(content) != null) {
            //System.out.println(symbols.getValue(ctx.ID().getText()).getInt());
            //System.out.println("In if statement");
            Variable temp = index.getValue(content);
            //Checks if the saved variable is a string or not
            if(temp.isString()) {
                code += "mv.visitFieldInsn(Opcodes.GETSTATIC, \"java/lang/System\", \"out\", \"Ljava/io/PrintStream;\");\n";
                code += "mv.visitVarInsn(Opcodes.ALOAD,"+temp.getInt()+");\n";
                code += "mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, \"java/io/PrintStream\", \"println\", \"(Ljava/lang/String;)V\", false);\n";
            }
            else {
                code += "mv.visitFieldInsn(Opcodes.GETSTATIC, \"java/lang/System\", \"out\", \"Ljava/io/PrintStream;\");\n";
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+temp.getInt()+");\n";
                code += "mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, \"java/io/PrintStream\", \"println\", \"(I)V\", false);\n";
            }
        }
        //Checks for unsaved variables
        /* 
        else if(symbols.getValue(ctx.ID().getText()) != null) {
            code += "mv.visitFieldInsn(Opcodes.GETSTATIC, \"java/lang/System\", \"out\", \"Ljava/io/PrintStream;\");\n";
            code += "mv.visitLdcInsn((String)\""+symbols.remove(ctx.ID().getText()).getString()+"\");\n";
            code += "mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, \"java/io/PrintStream\", \"println\", \"(Ljava/lang/String;)V\", false);\n";
        }
        */
        //prints the given string
        else {
            //System.out.println("In else statement");
            //System.out.println(ctx.getText());
            code += "mv.visitFieldInsn(Opcodes.GETSTATIC, \"java/lang/System\", \"out\", \"Ljava/io/PrintStream;\");\n";
            code += "mv.visitLdcInsn((String)"+content+");\n";
            code += "mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, \"java/io/PrintStream\", \"println\", \"(Ljava/lang/String;)V\", false);\n";
        }
        //}catch(NullPointerException e) {System.out.println("Null pointer EXCEPTION");}        
        return code;
    }//end visitPrint

    /**
     * {@inheritDoc}
     * visitRead is used to visit the read command, return the appropriate asm code, and save modified variables in the index table
     */
    @Override 
    public String visitRead(KnightCodeParser.ReadContext ctx) { 
        System.out.println("Visiting READ");
        //System.out.println(ctx.toStringTree());
        String code = "";
        int scanIndex = storageIndex;
        storageIndex++;
        //Boilerplate scanner asm code
        code += "mv.visitTypeInsn(Opcodes.NEW, \"java/util/Scanner\");\n";
        code += "mv.visitInsn(Opcodes.DUP);\n";
        code += "mv.visitFieldInsn(Opcodes.GETSTATIC, \"java/lang/System\", \"in\", \"Ljava/io/InputStream;\");\n";
        code += "mv.visitMethodInsn(Opcodes.INVOKESPECIAL, \"java/util/Scanner\", \"<init>\", \"(Ljava/io/InputStream;)V\", false);\n";
        code += "mv.visitVarInsn(Opcodes.ASTORE,"+scanIndex+");\n";      
        String idName = ctx.getChild(1).getText();
        //reads nextline if the ID is found in the symbol table
        if(symbols.getValue(idName) != null) {
            code += "mv.visitVarInsn(Opcodes.ALOAD,"+scanIndex+");\n";
            code += "mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, \"java/util/Scanner\", \"nextLine\", \"()Ljava/lang/String;\", false);\n";
            code += "mv.visitVarInsn(Opcodes.ASTORE,"+storageIndex+");\n";
            storageIndex++;
            Variable temp = symbols.remove(idName);
            //System.out.println("var is string: " + temp.isString());
            //If var is a string then index is saved in index table
            if(temp.isString()) {
                temp.setInt(storageIndex-1);
                index.addEntry(idName, temp);
            }
            //if var is int, the parseInt code is added to the code before index is saved
            else {
                code += "mv.visitVarInsn(Opcodes.ALOAD,"+(storageIndex-1)+");\n";
                code += "mv.visitMethodInsn(Opcodes.INVOKESTATIC, \"java/lang/Integer\", \"parseInt\", \"(Ljava/lang/String;)I\", false);\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                temp.setInt(storageIndex);
                index.addEntry(idName, temp);
                storageIndex++;
            }
        }
        return code; 
    }//end visitRead

    /**
     * {@inheritDoc}
     * visitDecision returns suitable asm code for an if, then, else block  
     */
    @Override 
    public String visitDecision(KnightCodeParser.DecisionContext ctx) { 
        System.out.println("Visiting IF");
        System.out.println(ctx.getText());
        String code = "";
        int a=0;
        int b=0;
        int aIndex=0;
        int bIndex=0;
        String termA = ctx.getChild(1).getText();
        String termB = ctx.getChild(3).getText();
        //System.out.println(termA);
        //System.out.println(termB);
        //Checks if a is a variable
        if(index.getValue(termA) != null) {
            aIndex = index.getValue(termA).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+aIndex+");\n";
        }
        else {
            try {
                a = Integer.parseInt(termA);
                code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
            } catch(NumberFormatException e) {}
        }
        //Checks if b is a variable
        if(index.getValue(termB) != null) {
            bIndex = index.getValue(termB).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+bIndex+");\n";
        }
        //Parses ints a&b from the tree
        else {
            try {
                b = Integer.parseInt(termB);
                code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
            } catch(NumberFormatException e) {}
        }
        //String comp = ctx.getChild(2).getText();

        /* 
        else {
            //STAT Loop for no ELSE Block
            code += "Label endIf = new Label();\n";
            if(comp.equals(">")) {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPLE, endIf);\n";
            }
            else if(comp.equals("<")) {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPGE, endIf);\n";
            }
            else if(comp.equals("=")) {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPNE, endIf);\n";
            }
            else {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPEQ, endIf);\n";
            }
            while(i<ctx.getChildCount()) {
                System.out.println(ctx.getChild(i).getText());
                code += visitStat((StatContext)ctx.getChild(i));
                i++;
            }
            code += "mv.visitLabel(endIf);\n";
        }
        */
        return code += visitComp((CompContext) ctx.getChild(2)); 
    }//end visitDecision

    /**
     * {@inheritDoc}
     * visitLoop returns suitable asm code for a while loop
     */
    @Override 
    public String visitLoop(KnightCodeParser.LoopContext ctx) { 
        System.out.println("Visiting WHILE");
        String code = "";
        
        int a=-1;
        int b=-1;
        int aIndex=0;
        int bIndex=0;
        String termA = ctx.getChild(1).getText();
        String termB = ctx.getChild(3).getText();
        //System.out.println(termA);
        //System.out.println(termB);
        code += "Label loop = new Label();\nLabel endLoop = new Label();\n";
        code += "mv.visitLabel(loop);\n";
        //Checks if a is a variable
        if(index.getValue(termA) != null) {
            aIndex = index.getValue(termA).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+aIndex+");\n";
        }
        else {
            try {
                a = Integer.parseInt(termA);
                code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
            } catch(NumberFormatException e) {}
        }
        //Checks if b is a variable
        if(index.getValue(termB) != null) {
            bIndex = index.getValue(termB).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+bIndex+");\n";
        }
        //Parses ints a&b from the tree
        else {
            try {
                b = Integer.parseInt(termB);
                if(b!=0)
                    code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
            } catch(NumberFormatException e) {}
        }
        //String comp = ctx.getChild(2).getText();
        //System.out.println(code.substring(code.length()-11,code.length()-3));
        code += visitComp((CompContext)ctx.getChild(2));
        return code; //+= "mv.visitLabel(endLoop);\n";
    }//visitLoop

    /**
     * {@inheritDoc}
     * visitMultiplication visits an mult expr, returns bytecode for the operation, and saves the product in the index table
     */
    @Override 
    public String visitMultiplication(KnightCodeParser.MultiplicationContext ctx) { 
        System.out.println("Visiting MULT");
        String code = "";
        //System.out.println(ctx.getChildCount());
        int a=0;
        int b=0;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks if a is a variable
        if(symbols.getValue(termA) != null) {
            a = symbols.remove(termA).getInt();
        }
        //Checks if b is a variable
        if(symbols.getValue(termB) != null) {
            b = symbols.remove(termB).getInt();
        }
        //Parses ints a&b from the tree
        else {
            try {
                a = Integer.parseInt(termA);
            } catch(NumberFormatException e) {}
            try {
                b = Integer.parseInt(termB);
        
            } catch(NumberFormatException e) {}
        }
        code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        index.addEntry(termA, new Variable(storageIndex));
        storageIndex++;
        code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        index.addEntry(termB, new Variable(storageIndex));
        storageIndex++;
        code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-2)+");\n";
        code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.IMUL);\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        storageIndex++;
        return code;
    }//end visitMutlitplication

    /**
     * {@inheritDoc}
     * visitDivision visits a division expr, returns bytecode for the division, and saves the quotient in the index table
     */
    @Override 
    public String visitDivision(KnightCodeParser.DivisionContext ctx) { 
        System.out.println("Visiting DIV");
        String code = "";
        int a=0;
        int b=0;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks if a is a variable
        if(symbols.getValue(termA) != null) {
            a = symbols.remove(termA).getInt();
        }
        //Checks if b is a variable
        if(symbols.getValue(termB) != null) {
            b = symbols.remove(termB).getInt();
        }
        //Parses ints a&b from the tree
        else {
            try {
                a = Integer.parseInt(termA);
            } catch(NumberFormatException e) {}
            try {
                b = Integer.parseInt(termB);
        
            } catch(NumberFormatException e) {}
        }
        code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        index.addEntry(termA, new Variable(storageIndex));
        storageIndex++;
        code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        index.addEntry(termB, new Variable(storageIndex));
        storageIndex++;
        code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-2)+");\n";
        code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.IDIV);\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        storageIndex++;
        return code;
    }//end visitDivision

    /**
     * {@inheritDoc}
     * visitAddition visits an addition expr, returns bytecode for the addtion, and saves the sum in the index table
     */
    @Override 
    public String visitAddition(KnightCodeParser.AdditionContext ctx) {
        System.out.println("Visiting ADD");
        String code = "";
        //System.out.println(ctx.getChildCount());
        int a=0;
        int b=0;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks if a is a variable
        if(symbols.getValue(termA) != null) {
            a = symbols.remove(termA).getInt();
        }
        //Checks if b is a variable
        if(symbols.getValue(termB) != null) {
            b = symbols.remove(termB).getInt();
        }
        //Parses ints a&b from the tree
        else {
            try {
                a = Integer.parseInt(termA);
            } catch(NumberFormatException e) {}
            try {
                b = Integer.parseInt(termB);
        
            } catch(NumberFormatException e) {}
        }
        code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        index.addEntry(termA, new Variable(storageIndex));
        storageIndex++;
        code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        index.addEntry(termB, new Variable(storageIndex));
        storageIndex++;
        code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-2)+");\n";
        code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.IADD);\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        storageIndex++;
        return code; 
    }//end visitAddition

    /**
     * {@inheritDoc}
     * visitSubtraction visits a subtraction expr, returns bytecode for the subtraction, and saves the sum in the index table
     */
    @Override 
    public String visitSubtraction(KnightCodeParser.SubtractionContext ctx) { 
        System.out.println("Visiting SUB");
        String code = "";
        int a=-1;
        int b=-1;
        int aIndex=0;
        int bIndex=0;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks if a is a variable
        if(symbols.getValue(termA) != null) {
            a = symbols.remove(termA).getInt();
            code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            index.addEntry(termA, new Variable(storageIndex));
            storageIndex++;
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        }
        else if(index.getValue(termA) != null) {
            aIndex = index.getValue(termA).getInt();
            if(termB.equals("1")) {
                code += "mv.visitIincInsn("+aIndex+", -1);\n";
                return code;
            }
            else
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+aIndex+");\n";
        }
        else {
            try {
                a = Integer.parseInt(termA);
                code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                //index.addEntry(termA, new Variable(storageIndex));
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            } catch(NumberFormatException e) {}
        }
        //Checks if b is a variable
        if(symbols.getValue(termB) != null) {
            b = symbols.remove(termB).getInt();
            code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            index.addEntry(termB, new Variable(storageIndex));
            storageIndex++;
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        }
        else if(index.getValue(termB) != null) {
            bIndex = index.getValue(termB).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+bIndex+");\n";
        }
        //Parses ints b from the tree
        else {
            try {
                b = Integer.parseInt(termB);
                    code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
                    code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                //index.addEntry(termB, new Variable(storageIndex));
                    storageIndex++;
                    code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            } catch(NumberFormatException e) {}
        }
        //code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.ISUB);\n";
        code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
        storageIndex++;
        return code; 
    }//end visitSubtraction

    @Override 
    public String visitComp(KnightCodeParser.CompContext ctx) { 
        System.out.println("Visiting COMP");
        //System.out.println(ctx.toStringTree());
        String code = "";
        String comp = ctx.getText();
        String label = "";
        if(ctx.getParent().getChild(0).getText().equals("IF")) {
            label = "endIf";
        }
        else
            label = "endLoop";
        String childName = ctx.getParent().getChild(5).getText();
        if(ctx.getParent().getText().contains("ELSE")) {
            code += "Label elseblock = new Label();\nLabel endIf = new Label();\n";
            if(ctx.getParent().getChild(1).getText().equals("0") || ctx.getParent().getChild(3).getText().equals("0") ){
                if(comp.equals(">")) {
                    code += "mv.visitJumpInsn(Opcodes.IFLE, elseblock);\n";
                }
                else if(comp.equals("<")) {
                    code += "mv.visitJumpInsn(Opcodes.IFGE, elseblock);\n";
                }
                else if(comp.equals("=")) {
                    code += "mv.visitJumpInsn(Opcodes.IFNE, elseblock);\n";
                }
                else {
                    code += "mv.visitJumpInsn(Opcodes.IFEQ, elseblock);\n";
                }
            }
            else {
                if(comp.equals(">")) {
                    code += "mv.visitJumpInsn(Opcodes.IF_ICMPLE, elseblock);\n";
                }
                else if(comp.equals("<")) {
                    code += "mv.visitJumpInsn(Opcodes.IF_ICMPGE, elseblock);\n";
                }
                else if(comp.equals("=")) {
                    code += "mv.visitJumpInsn(Opcodes.IF_ICMPNE, elseblock);\n";
                }
                else {
                    code += "mv.visitJumpInsn(Opcodes.IF_ICMPEQ, elseblock);\n";
                }
            }
            //The IF Block
            int i=5;
            while(!childName.equals("ELSE")) {
                code += visitStat((StatContext)ctx.getParent().getChild(i));
                i++;
                childName = ctx.getParent().getChild(i).getText();
            }
            //The ELSE Block
            code += "mv.visitLabel(elseblock);\n";
            for(int j=i+1; j<ctx.getParent().getChildCount()-1; j++) {
                code += visitStat((StatContext)ctx.getParent().getChild(j));
            }
            code += "mv.visitLabel(endIf);\n";
        }
        else if(ctx.getParent().getChild(1).getText().equals("0") || ctx.getParent().getChild(3).getText().equals("0") ){
            if(comp.equals(">")) {
                code += "mv.visitJumpInsn(Opcodes.IFLE,"+label+");\n";
            }
            else if(comp.equals("<")) {
                code += "mv.visitJumpInsn(Opcodes.IFGE,"+label+");\n";
            }
            else if(comp.equals("=")) {
                code += "mv.visitJumpInsn(Opcodes.IFNE,"+label+");\n";
            }
            else {
                code += "mv.visitJumpInsn(Opcodes.IFEQ,"+label+");\n";
            }    
            for(int i=5; i<ctx.getParent().getChildCount()-1; i++) {
                code += visitStat((StatContext) ctx.getParent().getChild(i));
            }
            if(label.equals("endLoop"));
                code += "mv.visitJumpInsn(Opcodes.GOTO, loop);\n";
            code += "mv.visitLabel("+label+");\n";
        }
        else {
            if(comp.equals(">")) {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPLE, "+label+");\n";
            }
            else if(comp.equals("<")) {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPGE,"+label+");\n";
            }
            else if(comp.equals("=")) {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPNE, "+label+");\n";
            }
            else {
                code += "mv.visitJumpInsn(Opcodes.IF_ICMPEQ,"+label+");\n";
            }
            for(int i=5; i<ctx.getParent().getChildCount()-1; i++) {
                code += visitStat((StatContext) ctx.getParent().getChild(i));
            }
            if(label.equals("endLoop"));
                code += "mv.visitJumpInsn(Opcodes.GOTO, loop);\n";
            code += "mv.visitLabel("+label+");\n";
        }
        return code;
    }//end visitComp

    /**
     * {@inheritDoc}
     * visitIdentifer returns the txt of a given ID ndoe
     */
    @Override 
    public String visitIdentifier(KnightCodeParser.IdentifierContext ctx) { 
        //System.out.println("visitIdentifier: " + ctx.ID().getText());
        return ctx.ID().getText();
    }//end visitIdentifier

}//end MyVisitor