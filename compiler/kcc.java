/**
 * kcc.java accepts a .kc file and the name of the output file. kcc.java compiles the .kc file into a java .class file 
 * @author Jared Rosenberger
 * @version 2.0
 * Assignment 5
 * CS322 - Compiler Construction
 * Spring 2024
 */
package compiler;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
//ANTLR packages
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
//import org.antlr.v4.gui.Trees;

import lexparse.*;
public class kcc{
    public static void main(String[] args){
        //Test arg length
        if(args.length !=2) {
            System.out.println("The program takes 2 args; the .kc file to compile and the name of the output program");
            System.exit(1);
        }
        CharStream input;
        KnightCodeLexer lexer;
        CommonTokenStream tokens;
        KnightCodeParser parser;
        //try/catch for File creation
        try{
            input = CharStreams.fromFileName(args[0]);  //get the input
            lexer = new KnightCodeLexer(input); //create the lexer
            tokens = new CommonTokenStream(lexer); //create the token stream
            parser = new KnightCodeParser(tokens); //create the parser
            ParseTree tree = parser.file();  //set the start location of the parser
            
            SymbolTable vars = new SymbolTable();
            //parse true output file name and generation file name
            String genName = "Gen" + args[1].substring(args[1].length()-1);
            String outputName = args[1].substring(args[1].indexOf("/")+1);
            File output = new File("output/" + genName + ".java");
            String boilerplate = "";
            String code = "";
            //Trees.inspect(tree, parser);  //displays the parse tree
            //Try/catch for the FileOutputStream
            try {
                FileOutputStream outStream = new FileOutputStream(output);
                //boilerplate is the base asm code needed at the beginning and end of each generating file
                boilerplate = "package output;\nimport org.objectweb.asm.*;\nimport java.io.File;\nimport java.io.FileOutputStream;\nimport java.io.IOException;\n";
                boilerplate += "public class " + genName + " {\npublic static void main(String[] args) {\n";
                boilerplate += "System.out.println(\"Generating Bytecode...\");\nClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);\n";
                boilerplate += "cw.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC,\"output/" + outputName + "\", null, \"java/lang/Object\",null);\n";
                boilerplate += "MethodVisitor mv = cw.visitMethod(Opcodes.ACC_PUBLIC+Opcodes.ACC_STATIC, \"main\", \"([Ljava/lang/String;)V\", null, null);\n";
                boilerplate += "mv.visitCode();\n";
                outStream.write(boilerplate.getBytes());
                boilerplate = "";

                MyVisitor visit = new MyVisitor(vars);//, output);
                //asm code for current program
                code += visit.visit(tree);
                outStream.write(code.getBytes());
                //generic ending code for generating file
                boilerplate += "\nmv.visitInsn(Opcodes.RETURN);\n";
                boilerplate += "mv.visitMaxs(0,0);\n";
                boilerplate += "mv.visitEnd();\n";
                boilerplate += "cw.visitEnd();\n\n";
                boilerplate += "byte[] b = cw.toByteArray();\ntry{";
                boilerplate += "File out = new File(\"output/" +outputName+".class\");\n";
                boilerplate += "FileOutputStream byteOut = new FileOutputStream(out);\n";
                boilerplate += "byteOut.write(b);\n";
                boilerplate += "byteOut.close();\n}catch(IOException e){System.out.println(e.getMessage());}\nSystem.out.println(\"Done!\");\n}\n}";
                outStream.write(boilerplate.getBytes());
                outStream.close();
            } catch(FileNotFoundException e) {System.out.println(e.getMessage());}
            System.out.println("Generating File...");
            //Runtime methods calls javac and java on the outputfile
            Process compile = Runtime.getRuntime().exec("javac output/" + genName + ".java");
            try {
                compile.waitFor();
                Runtime.getRuntime().exec("java output." + genName);
            }catch(InterruptedException e){System.out.println(e.getMessage());}
            
            System.out.println("Done!");
            //vars.print();
        }
        catch(IOException e){System.out.println(e.getMessage());}   
    }//end main
}//end kcc