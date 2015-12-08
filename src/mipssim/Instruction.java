/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

/**
 *
 * @author daveti
 * Instructions
 * Oct 2, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Instruction {
    
    public int category;
    public String opcode;
    public String targetReg;
    public String target2nd;
    public String sourceReg;
    public String source2nd;
    public String sourceImm;
    public String targetAdr;
    public String offset;
    public int clock;
    public int order;
    public int address;
    public boolean done;
    public Result result;
    public int bufPhase;
    
    public Instruction(int cat, String op, String tarReg, String srcReg,
            String src2nd, String srcImm, String tarAdr, String off, int addr) {
        category = cat;
        opcode = op;
        targetReg = tarReg;
        target2nd = "";
        sourceReg = srcReg;
        source2nd = src2nd;
        sourceImm = srcImm;
        targetAdr = tarAdr;
        offset = off;
        address = addr;
        clock = 0;
        order = 0;
        done = false;
        result = null;
        bufPhase = -1;
    }
    
    public Instruction(int cat, int addr) {
        category = cat;
        opcode = "";
        targetReg = "";
        target2nd = "";
        sourceReg = "";
        source2nd = "";
        sourceImm = "";
        targetAdr = "";
        offset = "";
        address = addr;
        clock = 0;
        order = 0;
        done = false;
        result = null;
        bufPhase = -1;
    }
    
    /**
     * This function set the clock number for this instruction
     * @param clc - the cycle number when the instruction is issued
     * Nov 23, 2015
     * daveti
     */
    public void setClock(int clc) {
        clock = clc;
    }
    
    public void setOrder(int ord) {
        order = ord;
    }
    
    public void setResult(Result res) {
        result = res;
    }
    
    public void setDone() {
        done = true;
    }
    
    public String dump() {
        String s = "";
        switch (category) {
            case 1:
                switch (opcode) {
                    case "J":
                        s += opcode + " #" + targetAdr;
                        break;
                        
                    case "BEQ":
                    case "BNE":
                        s += opcode + " " + sourceReg + ", " + targetReg + ", #" + offset;
                        break;
                        
                    case "BGTZ":
                        s += opcode + " " + sourceReg + ", #" + offset;
                        break;
                        
                    case "SW":                      
                    case "LW":
                        s += opcode + " " + targetReg + ", " + offset + "(" + sourceReg + ")";
                        break;
                        
                    case "BREAK":
                        s += opcode;
                        break;
                        
                    default:
                        System.out.println("Error: unknown category-1 opcode: " + opcode);
                        return null;
                }
                break;
                
            case 2:
                if (("SRL".equals(opcode)) || ("SRA".equals(opcode)))
                    s += opcode + " " + targetReg + ", " + sourceReg + ", #" + sourceImm;
                else
                    s += opcode + " " + targetReg + ", " + sourceReg + ", " + source2nd;
                break;
                
            case 3:
                s += opcode + " " + targetReg + ", " + sourceReg + ", #" + sourceImm;
                break;
                
            case 4:
                s += opcode + " " + sourceReg + ", " + source2nd;
                break;
                
            case 5:
                s += opcode + " " + targetReg;
                break;
                
            default:
                System.out.println("Error: unknown category: " + Integer.toString(category));
                return null;
        }
        
        return s;
    }
    
    @Override
    public String toString() {
        return (dump() + ", address=" + Integer.toString(address));
    }
    
}
