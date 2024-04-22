/**
 * MyVisitor is the method visitor used by kcc.java to walk the parse tree, it returns a string of java asm code
 * @author Jared Rosenberger
 * @version 2.4
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
    private int storageIndex;//index for bytecode storage

    /**
     * defualt constructor initializes class variables
     */
    public MyVisitor() {
        symbols = new SymbolTable();
        storageIndex = 1;
        index = new SymbolTable();
    }//end Constructor

    /**
     * {@inheritDoc}
     * visitFile is used to start walking the parse tree by kcc.java
     * @param ctx is the starting point of the parse tree
     * @return the String containing all asm commands needed for the .kc file
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
     * @param ctx is the DECLARE node of the parse tree
     * @return an empty String
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
        return "";
    }//end visitDeclare

    /**
     * {@inheritDoc}
     * visitBody is used to visit each statement in the body of the kc code
     * @param ctx is the BODY node of the parse tree
     * @return a String containing all asm commands needed for the body of the .kc file
     */
    @Override 
    public String visitBody(KnightCodeParser.BodyContext ctx) { 
        System.out.println("Visiting Body");
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
     * @param ctx is a STAT node of the parse tree
     * @return a String containing all asm commands needed for a given STAT in the .kc file
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
            //System.out.println("Found SET");
            code += visitSetvar((SetvarContext) childNode);
        }
        //Checks for PRINT keyword
        else if(childContent.substring(0,5).equals("PRINT")) {
            //System.out.println("Found PRINT");
            code += visitPrint((PrintContext) childNode);
        }
        //Checks for READ keyword
        else if(childContent.substring(0,4).equals("READ")) {
            //System.out.println("Found READ");
            code += visitRead((ReadContext) childNode);
        }
        //Checks for IF keyword
        else if(childContent.substring(0,2).equals("IF")) {
            //System.out.println("Found IF");
            code += visitDecision((DecisionContext) childNode);
        }
        //Checks for WHILE keyword
        else if(childContent.substring(0,5).equals("WHILE")) {
            //System.out.println("Found WHILE");
            code += visitLoop((LoopContext) childNode);
        }
        //Checks for parentheses
        else if(childContent.indexOf('(') != -1) {
            //System.out.println("Found Parentheses");
            code += visitParenthesis((ParenthesisContext) childNode);
        }
        //Checks for MULT symbol
        else if(childContent.indexOf('*') != -1) {
            //System.out.println("Found MULT");
            code += visitMultiplication((MultiplicationContext) childNode);
        }
        //Checks for DIV symbol
        else if(childContent.indexOf('/') != -1) {
            //System.out.println("Found DIV");
            code += visitDivision((DivisionContext) childNode);
        }
        //Checks for ADD symbol
        else if(childContent.indexOf('+') != -1) {
            //System.out.println("Found ADD");
            code += visitAddition((AdditionContext) childNode);
        }
        //Checks for SUB symbol
        else if(childContent.indexOf('-') != -1) {
            //System.out.println("Found SUB");
            code += visitSubtraction((SubtractionContext) childNode);
        }
        //There must be a COMP symbol if there is a valid statement
        else {//if(childContent.indexOf('>') != -1 || childContent.indexOf('<') != -1 || childContent.indexOf('=') != -1 || childContent.indexOf("<>") != -1) {
            //System.out.println("Found COMP");
            code += visitComp((CompContext) childNode);
        }
        return code;
    }//end visitStat

    /**
     * {@inheritDoc}
     * visitSetvar is used to set the values of any variable in the symbol table and/or its bytecode index in the index table
     * @param ctx is a SET node of the parse tree 
     * @return a String containing all asm commands needed for a given SET in the .kc file
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
        else if(exprTxt.indexOf('(') != -1) {
            //System.out.println("Found Parentheses");
            if(symbols.getValue(id) != null && ctx.getChild(3).getChild(0).getText().equals("(")) {
                String midTxt = ctx.getChild(3).getChild(1).getText();
                if(midTxt.indexOf("*") != -1) {
                    code += visitMultiplication((MultiplicationContext) ctx.getChild(3).getChild(1));
                }
                else if(midTxt.indexOf("/") != -1) {
                    code += visitDivision((DivisionContext) ctx.getChild(3).getChild(1));
                }
                else if(midTxt.indexOf("+") != -1) {
                    code += visitAddition((AdditionContext) ctx.getChild(3).getChild(1));
                }
                else if(midTxt.indexOf("-") != -1) {
                    code += visitSubtraction((SubtractionContext) ctx.getChild(3).getChild(1));
                }
                else {
                    Variable temp = symbols.getValue(id);
                    temp.setInt((Integer.parseInt(midTxt)));
                    symbols.addEntry(id, temp);
                }
            }
            else {
                try {
                    code += visitParenthesis((ParenthesisContext) ctx.getChild(3).getChild(0));
                } catch(ClassCastException e) {}
                try {
                    code += visitParenthesis((ParenthesisContext) ctx.getChild(3).getChild(2));
                } catch(ClassCastException e) {}
                finally {
                    if(symbols.getValue(id) != null) {
                        index.addEntry(id, new Variable(storageIndex-1));
                        symbols.remove(id);
                    }
                }
            }
        }
        else if(exprTxt.indexOf('*') != -1) {
            //System.out.println("Found MULT");
            code += visitMultiplication((MultiplicationContext)ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            } 
                //index.getValue(id).setInt(storageIndex-1);
        }
        else if(exprTxt.indexOf('/') != -1) {
            //System.out.println("Found DIV");
            code += visitDivision((DivisionContext) ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
                //index.getValue(id).setInt(storageIndex-1);
        }
        else if(exprTxt.indexOf('+') != -1) {
            //System.out.println("Found ADD");
            code += visitAddition((AdditionContext) ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
               // index.getValue(id).setInt(storageIndex-1);
        }
        else {
            //System.out.println("Found SUB");
            code += visitSubtraction((SubtractionContext) ctx.getChild(3));
            if(symbols.getValue(id) != null) {
                index.addEntry(id, new Variable(storageIndex-1));
                symbols.remove(id);
            }
                //index.getValue(id).setInt(storageIndex-1);
        }
        return code; 
    }//end visitSetvar

    /**
     * {@inheritDoc}
     * visitPrint is used to return asm code for printing a string or variable
     * @param ctx is a PRINT node of the parse tree
     * @return a String containing all asm commands needed for a given PRINT in the .kc file
     */
    @Override 
    public String visitPrint(KnightCodeParser.PrintContext ctx) { 
        System.out.println("Visiting PRINT");
        //System.out.println("print has " + ctx.getChildCount());
        String content = ctx.getChild(1).getText();
        //System.out.println(content);
        String code = "";
        //Checks for saved variable
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
        //Prints the String val
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
     * visitRead is used to visit the read command, create or load a Scanner, read the next line, and save modified variables/inputs in the index table
     * @param ctx is a READ node of the parse tree
     * @return a String containing all asm commands needed for a given READ in the .kc file
     */
    @Override 
    public String visitRead(KnightCodeParser.ReadContext ctx) { 
        System.out.println("Visiting READ");
        //System.out.println(ctx.toStringTree());
        String code = "";
        String id = ctx.getChild(0).getText();
        int scanIndex =-1;
        //Checks if there is a scanner object already
        if(index.getValue(id) == null) {
            scanIndex = storageIndex;
            storageIndex++;
        
            //Boilerplate scanner init asm code
            code += "mv.visitTypeInsn(Opcodes.NEW, \"java/util/Scanner\");\n";
            code += "mv.visitInsn(Opcodes.DUP);\n";
            code += "mv.visitFieldInsn(Opcodes.GETSTATIC, \"java/lang/System\", \"in\", \"Ljava/io/InputStream;\");\n";
            code += "mv.visitMethodInsn(Opcodes.INVOKESPECIAL, \"java/util/Scanner\", \"<init>\", \"(Ljava/io/InputStream;)V\", false);\n";
            code += "mv.visitVarInsn(Opcodes.ASTORE,"+scanIndex+");\n";
            index.addEntry(id, new Variable(scanIndex));
        }
        else
            scanIndex = index.getValue(id).getInt();
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
     * @param ctx is a DECISION node of the parse tree
     * @return a String containing all asm commands needed for a given DECISION in the .kc file
     */
    @Override 
    public String visitDecision(KnightCodeParser.DecisionContext ctx) { 
        System.out.println("Visiting IF");
        //System.out.println(ctx.getText());
        String code = "";
        int a=-1;
        int b=-1;
        int aIndex=-1;
        int bIndex=-1;
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
        return code += visitComp((CompContext) ctx.getChild(2)); 
    }//end visitDecision

    /**
     * {@inheritDoc}
     * visitLoop returns suitable asm code for a while loop
     * @param ctx is a LOOP node of the parse tree
     * @return a String containing all asm commands needed for a given LOOP in the .kc file
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
        code += "Label loop = new Label();\n";
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
        return code;
    }//visitLoop

    /**
     * {@inheritDoc}
     * visitParenthesis handles cases of two expressions divided with parentheses
     * @param ctx is a Parenthesis node of the parse tree
     * @return a String containing all asm commands needed for a given parenthesis in the .kc file
     */
    @Override 
    public String visitParenthesis(KnightCodeParser.ParenthesisContext ctx) { 
        System.out.println("Visiting Parenthesis");
        String code = "";
        //System.out.println("Parenthesis has " + ctx.getChildCount());
        //System.out.println(ctx.toStringTree());
        String exprTxt = ctx.getChild(1).getText();
        //System.out.println(exprTxt);
        if(exprTxt.indexOf('*') != -1) {
            code += visitMultiplication((MultiplicationContext) ctx.getChild(1));
        }
        else if(exprTxt.indexOf('/') != -1) {
            code += visitDivision((DivisionContext) ctx.getChild(1));
        }
        if(exprTxt.indexOf('+') != -1) {
            code += visitAddition((AdditionContext) ctx.getChild(1));
        }
        else {
            code += visitSubtraction((SubtractionContext) ctx.getChild(1));
        }
        ParseTree grandParent = ctx.getParent();
        String secondOp = grandParent.getChild(1).getText();
        //System.out.println(secondOp);
        int treeSide = -1;
        if(grandParent.getChild(0).getText().indexOf("(") != -1)
            treeSide = 2;
        else
            treeSide = 0;
        String term = grandParent.getChild(treeSide).getText();
        //Uses to secondOp to check 2nd operation
        if(secondOp.equals("*")) {
            //ASM to MUL 1st op result with 2nd op term
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            if(index.getValue(term) != null)
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+index.getValue(term).getInt()+");\n";
            else {
                code += "mv.visitIntInsn(Opcodes.BIPUSH, "+Integer.parseInt(term)+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            }
            code += "mv.visitInsn(Opcodes.IMUL);\n";
            if(index.getValue(grandParent.getParent().getChild(1).getText()) != null) {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(grandParent.getParent().getChild(1).getText()).getInt()+");\n";
            }
            else {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
            }
        }
        else if(secondOp.equals("/")) {
            //ASM to DIV 1st op result with 2nd op term
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            if(index.getValue(term) != null)
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+index.getValue(term).getInt()+");\n";
            else {
                code += "mv.visitIntInsn(Opcodes.BIPUSH, "+Integer.parseInt(term)+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            }
            code += "mv.visitInsn(Opcodes.IDIV);\n";
            if(index.getValue(grandParent.getParent().getChild(1).getText()) != null) {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(grandParent.getParent().getChild(1).getText()).getInt()+");\n";
            }
            else {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
            }
        }
        else if(secondOp.equals("+")) {
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            //ASM to ADD 1st op result with 2nd op term
            if(index.getValue(term) != null)
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+index.getValue(term).getInt()+");\n";
            else {
                code += "mv.visitIntInsn(Opcodes.BIPUSH, "+Integer.parseInt(term)+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            }
            code += "mv.visitInsn(Opcodes.IADD);\n";
            if(index.getValue(grandParent.getParent().getChild(1).getText()) != null) {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(grandParent.getParent().getChild(1).getText()).getInt()+");\n";
            }
            else {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
            }
        }
        else {
            //ASM to SUB 1st op result with 2nd op term
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            if(index.getValue(term) != null)
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+index.getValue(term).getInt()+");\n";
            else {
                code += "mv.visitIntInsn(Opcodes.BIPUSH, "+Integer.parseInt(term)+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
            }
            code += "mv.visitInsn(Opcodes.ISUB);\n";
            if(index.getValue(grandParent.getParent().getChild(1).getText()) != null) {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(grandParent.getParent().getChild(1).getText()).getInt()+");\n";
            }
            else {
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                storageIndex++;
            }
        }
        return code; 
    }//end visitParenthesis

    /**
     * getNumbers is used to get the terms of an expression and return asm code for the pushes/loads
     * @param ctx is a EXPR node of the parse tree
     * @return a String containing all asm commands needed to load the 2 numbers in an expr
     */
    private String getNumbers(ParseTree ctx) {
        String code = "";
        int a=-1;
        int b=-1;
        int aIndex=-1;
        int bIndex=-1;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks if a is an unstored variable
        if(symbols.getValue(termA) != null) {
            a = symbols.remove(termA).getInt();
            code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            aIndex = storageIndex;
            index.addEntry(termA, new Variable(aIndex));
            storageIndex++;
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+(aIndex)+");\n";
        }
        //Checks if a is a var that has been used already
        else if(index.getValue(termA) != null) {
            aIndex = index.getValue(termA).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+aIndex+");\n";
        }
        //Parses int a from the tree
        else {
            try {
                a = Integer.parseInt(termA);
                code += "mv.visitIntInsn(Opcodes.BIPUSH,"+a+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                aIndex = storageIndex;
                index.addEntry(termA, new Variable(aIndex));
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+aIndex+");\n";
            } catch(NumberFormatException e) {}
        }
        //Checks if b is an unused variable
        if(symbols.getValue(termB) != null) {
            b = symbols.remove(termB).getInt();
            code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            bIndex = storageIndex;
            index.addEntry(termB, new Variable(bIndex));
            storageIndex++;
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+bIndex+");\n";
        }
        //Checks if be is a var that has been used already
        else if(index.getValue(termB) != null) {
            bIndex = index.getValue(termB).getInt();
            code += "mv.visitVarInsn(Opcodes.ILOAD,"+bIndex+");\n";
        }
        //Parses int b from the tree
        else {
            try {
                b = Integer.parseInt(termB);
                code += "mv.visitIntInsn(Opcodes.BIPUSH,"+b+");\n";
                code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
                bIndex = storageIndex;
                index.addEntry(termB, new Variable(bIndex));
                storageIndex++;
                code += "mv.visitVarInsn(Opcodes.ILOAD,"+bIndex+");\n";
            } catch(NumberFormatException e) {}
        }
        return code;
    }//end getNumbers


    /**
     * {@inheritDoc}
     * visitMultiplication visits an mult expr, returns bytecode for the operation, and saves the product in the index table
     * @param ctx is a MULT node of the parse tree
     * @return a String containing all asm commands needed for a given MULT in the .kc file
     */
    @Override 
    public String visitMultiplication(KnightCodeParser.MultiplicationContext ctx) { 
        System.out.println("Visiting MULT");
        String code = "";
        code += getNumbers(ctx);
        //code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.IMUL);\n";
        //System.out.println(ctx.getParent().getChild(1).getText());
        //System.out.println(index.getValue(ctx.getParent().getChild(1).getText()).getInt());
        if(index.getValue(ctx.getParent().getChild(1).getText()) != null) {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(ctx.getParent().getChild(1).getText()).getInt()+");\n";
        }
        else {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            storageIndex++;
        }
        return code;
    }//end visitMutlitplication

    /**
     * {@inheritDoc}
     * visitDivision visits a division expr, returns bytecode for the division, and saves the quotient in the index table
     * @param ctx is a DIV node of the parse tree
     * @return a String containing all asm commands needed for a given DIV in the .kc file
     */
    @Override 
    public String visitDivision(KnightCodeParser.DivisionContext ctx) { 
        System.out.println("Visiting DIV");
        String code = "";
        code += getNumbers(ctx);
        //code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.IDIV);\n";
        if(index.getValue(ctx.getParent().getChild(1).getText()) != null) {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(ctx.getParent().getChild(1).getText()).getInt()+");\n";
        }
        else {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            storageIndex++;
        }
        return code; 
    }//end visitDivision

    /**
     * {@inheritDoc}
     * visitAddition visits an addition expr, returns bytecode for the addtion, and saves the sum in the index table
     * @param ctx is a ADD node of the parse tree
     * @return a String containing all asm commands needed for a given ADD in the .kc file
     */
    @Override 
    public String visitAddition(KnightCodeParser.AdditionContext ctx) {
        System.out.println("Visiting ADD");
        String code = "";
        int aIndex =-1;
        int bIndex =-1;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks for an iinc situation
        if(index.getValue(termA) != null) {
            aIndex = index.getValue(termA).getInt();
            //Checks iinc code is needed
            if(termB.equals("1")) {
                code += "mv.visitIincInsn("+aIndex+", 1);\n";
                return code;
            }
        }
        //Parses int a from the tree
        else if(index.getValue(termB) != null) {
            bIndex =index.getValue(termB).getInt();
            if(termA.equals("1")) {
                code += "mv.visitIincInsn("+bIndex+", 1);\n";
                return code;
            }
        }
        else {
            code += getNumbers(ctx);
        }
        //code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.IADD);\n";
        if(index.getValue(ctx.getParent().getChild(1).getText()) != null) {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(ctx.getParent().getChild(1).getText()).getInt()+");\n";
        }
        else {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            storageIndex++;
        }
        return code;
    }//end visitAddition

    /**
     * {@inheritDoc}
     * visitSubtraction visits a subtraction expr, returns bytecode for the subtraction, and saves the sum in the index table
     * @param ctx is a SUB node of the parse tree
     * @return a String containing all asm commands needed for a given SUB in the .kc file
     */
    @Override 
    public String visitSubtraction(KnightCodeParser.SubtractionContext ctx) { 
        System.out.println("Visiting SUB");
        String code = "";
        int aIndex=-1;
        String termA = ctx.getChild(0).getText();
        String termB = ctx.getChild(2).getText();
        //Checks for an iinc situation
        if(index.getValue(termA) != null) {
            aIndex = index.getValue(termA).getInt();
            if(termB.equals("1")) {
                code += "mv.visitIincInsn("+aIndex+", -1);\n";
                return code;
            }
        }
        //Parses int b from the tree
        else {
            code += getNumbers(ctx);
        }
        //code += "mv.visitVarInsn(Opcodes.ILOAD,"+(storageIndex-1)+");\n";
        code += "mv.visitInsn(Opcodes.ISUB);\n";
        if(index.getValue(ctx.getParent().getChild(1).getText()) != null) {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+index.getValue(ctx.getParent().getChild(1).getText()).getInt()+");\n";
        }
        else {
            code += "mv.visitVarInsn(Opcodes.ISTORE,"+storageIndex+");\n";
            storageIndex++;
        }
        return code; 
    }//end visitSubtraction

    /**
     * {@inheritDoc}
     * visitComp returns appropriate comparison asm code for comparisons in if or loop statements
     * @param ctx is a COMP node of the parse tree
     * @return a String containing all asm commands needed for a given COMP in the .kc file
     */
    @Override 
    public String visitComp(KnightCodeParser.CompContext ctx) { 
        System.out.println("Visiting COMP");
        //System.out.println(ctx.toStringTree());
        String code = "";
        String comp = ctx.getText();
        String label = "";
        //System.out.println(ctx.getParent().getChild(0).getText().equals("IF"));
        if(ctx.getParent().getChild(0).getText().equals("IF")) {
            label = "endIf";
        }
        else {
            label = "endLoop";
        }
        code += "Label "+label+" = new Label();\n";
        String childName = ctx.getParent().getChild(5).getText();
        if(ctx.getParent().getText().contains("ELSE")) {
            code += "Label elseblock = new Label();\n";
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
            code += "mv.visitJumpInsn(Opcodes.GOTO, endIf);\n";
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
            //System.out.println(label);
            //System.out.println(label.equals("endLoop"));
            if(label.equals("endLoop")) {
                code += "mv.visitJumpInsn(Opcodes.GOTO, loop);\n";
            }
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
            //System.out.println(label);
            //System.out.println(label.equals("endLoop"));
            if(label.equals("endLoop")) {
                System.out.println("Loop code added");
                code += "mv.visitJumpInsn(Opcodes.GOTO, loop);\n";
            }
            code += "mv.visitLabel("+label+");\n";
        }
        return code;
    }//end visitComp

    /**
     * {@inheritDoc}
     * visitIdentifer returns the txt of a given ID ndoe
     * @param ctx is an ID node of the parse tree
     * @return a String containing the ID
     */
    @Override 
    public String visitIdentifier(KnightCodeParser.IdentifierContext ctx) { 
        //System.out.println("visitIdentifier: " + ctx.ID().getText());
        return ctx.ID().getText();
    }//end visitIdentifier
    
}//end MyVisitor