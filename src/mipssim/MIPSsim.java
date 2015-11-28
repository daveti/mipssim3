/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author daveti
 * MIPS simulator 3
 * Support pipelines and basic score boards
 * Nov 22, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 * 
 * input: sample.txt
 * output: disassembly.txt, simulation.txt
 */
public class MIPSsim {
    
    private static final boolean debug = false;

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        // Init memory
        ArrayList<Binary> memory = new ArrayList<>();
        
        // Init regsiters
        Reg registers = new Reg(32);
        if (debug)
            registers.dumpReg();
        
        // Init decoder
        Decoder decoder = new Decoder(256, 4, false, registers);
        
        // Init executor
        Executor executor = new Executor(memory, registers);
 
        try {
            // Load instructions
            Utils.readParseInput(memory, 1);
        } catch (IOException ex) {
            Logger.getLogger(MIPSsim.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        // Start the disassembly
        Utils.redirectOutputDis();
        for (Binary b : memory) {
            if (decoder.decode(b) != 0)
                System.out.println("Error: decoding failure for binary " + b);
        }
        
        // Generate the disassembly file
        Utils.genDisassembly(memory);
        
        // Start the simulation
        Utils.redirectOutputSim();
        executor.pipeline();
    }
    
}
