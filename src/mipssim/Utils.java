/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;

/**
 *
 * @author davet
 * MIPSsim I/O related implementations
 * Feb 14, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Utils {

    private final static String instructionInput = "sample.txt";
    private final static String simOutput = "simulation.txt";
    private final static String disOutput = "disassembly.txt";
    private final static int instructionEnum = 1;
    private final static int registerEnum = 2;
    private final static int datamemEnum = 3;
    private final static boolean debug = false;


    public static void readParseInput(ArrayList<Binary> mem, int n) throws FileNotFoundException, IOException {

        String inputFile;

        switch (n) {
            case instructionEnum:
                inputFile = instructionInput;
                break;

            default:
                System.out.println("Error: unknown enum " + n);
                return;
        }

        try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {

            String line = br.readLine();
            while (line != null) {
                if (debug)
                    System.out.println("Debug: line=" + line);
               
                // Remove all the tailing
                line = line.trim();
                
                // Load into the memory
                Binary bin = new Binary(line);
                mem.add(bin);

                line = br.readLine();
            }
        }
    }
    
    public static void genDisassembly(ArrayList<Binary> mem) {
        for (Binary bin : mem) {
            String s = bin.bin;
            if (bin.isDat)
                s += "\t" + bin.dat.address + "\t" + bin.dat.data;
            else
                s += "\t" + bin.ins.address + "\t" + bin.ins.dump();
            System.out.println(s);
        }
    }

    /**
     * Set the output
     */
    public static void redirectOutputSim() {
        try {
            System.setOut(new PrintStream(new File(simOutput)));
        } catch (Exception e) {
        }
    }

    public static void redirectOutputDis() {
        try {
            System.setOut(new PrintStream(new File(disOutput)));
        } catch (Exception e) {
        }
    }

}
