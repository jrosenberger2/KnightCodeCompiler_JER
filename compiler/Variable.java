package compiler;
/**
 * Variable.java serves to store an int or String val and is used in SymbolTable.java
 * @author Jared Rosenberger
 * @version 1.1
 * Assignment 5
 * CS322 - Compiler Construction
 * Spring 2024
 */
public class Variable {
    private int num;
    private String word;

    /**
     * int arg constructor stores an int in the Variable object
     * @param value is the int to store
     */
    public Variable(int value) {
        num = value;
        word = null;
    }//end int constructor

    /**
     * String arg constructor stores a string in the Variable object
     * @param str is the String to be stored
     */
    public Variable(String str) {
        word = str;
    }//end String constructor

    /**
     * isString is used to determined if a variable object has a string or an int stored
     * @return true if a string is stored & false otherwise
     */
    public boolean isString() {
        if(word != null)
            return true;
        else
            return false;
    }//end isString

    /**
     * getString gives the user the stored String value
     * @return the stored string value
     */
    public String getString() {
        return word;
    }//end getString

    /**
     * getInt gives the user the stored int value
     * @return the stored int value
     */
    public int getInt() {
        return num;
    }//end getInt

    public void clear() {
        num = 0;
        word = "";
    }//end clear

    public void setString(String str) {
        word = str;
    }//end setString

    public void setInt(int num) {
        this.num = num;
    }//end setInt
}//end Variable