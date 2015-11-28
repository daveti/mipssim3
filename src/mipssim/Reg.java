/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author daveti
 * A register file class
 * Oct 2, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 * 
 */
public class Reg {
    
    private final Map regFile;
    private final int regNum;
    private final String regPre = "R";
    private final String regVal = "0";
    private final int regSep = 8;
    private final String regSpe1 = "HI";
    private final String regSpe2 = "LO";
    private final boolean debug = false;
    
    public int getRegSep() {
        return regSep;
    }
    
    Reg(int num) {
        // Create the map
        regNum = num;
        regFile = new HashMap<>();
        // Init the reg file
        String tmp;
        for (int i=0; i<num; i++) {
            tmp = regPre + Integer.toString(i);
            //if (debug)
            //  System.out.println("Debug: tmp=" + tmp + ", val=" + regVal);
            regFile.put(tmp, regVal);
        }
        // Add 2 special regs
        regFile.put(regSpe1, regVal);
        regFile.put(regSpe2, regVal);
        
        if (debug)
            System.out.println("Debug: total size of registers is " + regFile.size());
    }
    
    // Helpers
    
    public int getRegHiVal() {
        String val = (String) regFile.get(regSpe1);
        return Integer.parseInt(val);
    }
    
    public int getRegLoVal() {
        String val = (String) regFile.get(regSpe2);
        return Integer.parseInt(val);
    }
    
    public void setRegHiVal(int val) {
        regFile.put(regSpe1, Integer.toString(val));
    }
    
    public void setRegLoVal(int val) {
        regFile.put(regSpe2, Integer.toString(val));
    }
    
    public String getRegPrefix() {
        return regPre;
    }
    
    public boolean isIdxValid(int idx) {
        if (idx >= regNum) {
            System.out.println("Error: invalid register index: " + Integer.toString(idx));
            return false;
        }
        
        return true;
    }
    
    public int getRegVal(int idx) {
        if (!isIdxValid(idx))
            return -1;
        /*
        if (debug)
            System.out.println("Debug: reg=" + (regPre+Integer.toString(idx)) +
                    ", val=" + regFile.get(regPre+Integer.toString(idx)));
        */
        
        String val = (String) regFile.get(regPre+Integer.toString(idx));
        return Integer.parseInt(val);
    }
    
    public int getRegVal(String reg) {
        String val = (String) regFile.get(reg);
        return Integer.parseInt(val);
    }
    
    public void setRegVal(int idx, int val) {
        if (!isIdxValid(idx))
            return;
        
        regFile.put(regPre+Integer.toString(idx), Integer.toString(val));
    }
    
    public void setRegVal(String reg, int val) {
        regFile.put(reg, Integer.toString(val));
    }
    
    public void dumpReg() {
        int rep = regNum / regSep;
        int rem = regNum % regSep;
        String tmp;
        String tmp2;
        for (int i=0; i<rep; i++) {
            tmp = "";
            for (int j=0; j<regSep; j++) {
                tmp += String.format("%8d", getRegVal(i*regSep+j));
            }
            tmp2 = String.format("R%02d:", i*regSep);
            System.out.println(tmp2 + tmp);
        }
        
        if (rem != 0) {
            // Handle the left ones
            tmp = "";
            for (int k=1; k<=rem; k++)
                tmp += String.format("%8d", getRegVal(rep*regSep+k));
            tmp2= String.format("R%02d:", (rep+1)*regSep);
            System.out.println(tmp2 + tmp);
        }
        
        // Dump special registers.
        tmp = String.format("%8d", getRegHiVal());
        System.out.println(regSpe1 + ":" + tmp);
        tmp = String.format("%8d", getRegLoVal());
        System.out.println(regSpe2 + ":" + tmp);
    }
    
}
