package compiler;
/**
 * SymbolTable.java represents the symbol table for kcc.java using a hashmap of Strings and Variables
 * @author Jared Rosenberger
 * @version 1.1
 * Assignment 5
 * CS322 - Compiler Construction
 * Spring 2024
 */
import java.util.HashMap;
public class SymbolTable {
    private HashMap<String, Variable> symbols;

    /**
     * The zero arg constructor initializes the hashmap
     */
    public SymbolTable() {
        symbols = new HashMap<String, Variable>();
    }//end constructor

    /**
     * addEntry adds a new entry to the hashmap
     * @param key is the String variable name
     * @param value is the Variable object that holds an int or string
     */
    public void addEntry(String key, Variable value) {
        symbols.put(key, value);
    }//end addEntry

    /**
     * getValue searches that hashmap for a given key
     * @param key is the name of the variable stored in the hashmap
     * @return is the Variable value associated with the given key
     */
    public Variable getValue(String key) {
        return symbols.get(key);
    }//end getValue

    public Variable remove(String key) {
        return symbols.remove(key);
    }//end remove

    /**
     * removePair allows the removal of a key value pair from the hasmap
     * @param key is the variable name to be removed
     * @param value is the variable value to be removed
     */
    public void removePair(String key, Variable value) {
        symbols.remove(key, value);
    }//end removePair

    /**
     * clearTable empties the hashmap
     */
    public void clearTable() {
        symbols.clear();
    }//end ClearTable

    /**
     * print prints out every key value pair in the hashmap
     */
    public void print() {
        System.out.println("Symbol Table Entries:");
        for(HashMap.Entry<String, Variable> e : symbols.entrySet()) {
            String key = e.getKey();
            Variable val = e.getValue();
            if(val.isString()){
                System.out.println(key + "\t" + val.getString());
            }
            else {
                System.out.println(key + "\t" + val.getInt());
            }
        }
    }//end print
}//end SymbolTable