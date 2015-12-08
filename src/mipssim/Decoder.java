/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

/**
 *
 * @author daveti
 * Decoder (Disassembly)
 * Oct 2, 2015
 * root@davejingtian.org
 * http:/davejingtian.org
 */
public class Decoder {

    private final int startAddress;
    private final int stepAddress;
    private int address;
    private final boolean debug;
    private boolean breaked = false;
    private final Reg regFile;

    Decoder(int startAddress, int stepAddress, boolean debug, Reg regFile) {
        this.startAddress = startAddress;   // 256
        this.stepAddress = stepAddress;               // 4 for 32-bit
        this.debug = debug;
        this.regFile = regFile;
        address = startAddress;
    }

    public int decode(Binary bin) {
        boolean failure = false;
        boolean branch = false;
        boolean memory = false;
        String dest;
        String s = bin.bin;

        if (debug) {
            System.out.println("Debug: " + s);
        }

        if (breaked) {
            // Handle pure data
            // NOTE: since JDK has a problem converting binary string (-1) back
            // We need to to some hackish stuffs.
            Long decimal = Long.parseLong(s, 2);
            Data dat = new Data(decimal.intValue(), address);
            bin.genData(dat);

            // Change the PC
            address += stepAddress;

            return 0;
        }

        // Get the category
        String cat = s.substring(0, 3);
        int cate = Integer.parseInt(cat, 2);

        // Get the opcode
        String opc = s.substring(3, 6);
        int opco = Integer.parseInt(opc, 2);

        if (debug) {
            System.out.println("Debug: cat=" + cat + ", cate=" + Integer.toString(cate)
                    + ", opc=" + opc + ", opco=" + Integer.toString(opco));
        }

        // Get new instruction ready
        Instruction ins = new Instruction(cate+1, address);

        switch (cate) {
            case 0:
                // Parse the opcode
                switch (opco) {
                    case 0:
                        ins.opcode = "J";
                        // Get lower 28 bits of the target address
                        String low = s.substring(6) + "00";
                        // Get the higer 4 bits of the target address
                        String high = Integer.toBinaryString(address+1);
                        int padLen = stepAddress*8 - high.length();
                        String pad = "";
                        for (int i=0; i<padLen; i++)
                            pad += "0";
                        high = pad + high;
                        high = high.substring(0, 4);
                        // Set the target address
                        int addr = Integer.parseInt(high+low, 2);
                        ins.targetAdr = Integer.toString(addr);
                        break;
                        
                    case 1:
                        ins.opcode = "BEQ";
                        branch = true;
                        break;
                        
                    case 2:
                        ins.opcode = "BNE";
                        branch = true;
                        break;
                        
                    case 3:
                        ins.opcode = "BGTZ";
                        branch = true;
                        break;
                        
                    case 4:
                        ins.opcode = "SW";
                        memory = true;
                        break;
                        
                    case 5:
                        ins.opcode = "LW";
                        memory = true;
                        break;
                        
                    case 6:
                        ins.opcode = "BREAK";
                        breaked = true;
                        break;
                        
                    default:
                        System.out.println("Error: cate 1 unsupported opcode " + opc);
                        failure = true;
                        break;
                }
                // Keep processing
                if (branch) {
                    // Get rs, rt, offset
                    String rs = s.substring(6, 11);
                    String rt = s.substring(11, 16);
                    String offset = s.substring(16);
                    int rsNum = Integer.parseInt(rs, 2);
                    int rtNum = Integer.parseInt(rt, 2);
                    // NOTE: offset here is signed!
                    Long offL = Long.parseLong(offset, 2);
                    int offNum = offL.intValue();
                    if (regFile.isIdxValid(rsNum) && regFile.isIdxValid(rtNum)) {
                        ins.targetReg = regFile.getRegPrefix() + Integer.toString(rtNum);
                        ins.sourceReg = regFile.getRegPrefix() + Integer.toString(rsNum);
                        // NOTE: the offset needs shifting here
                        ins.offset = Integer.toString(offNum<<2);
                    } else {
                        System.out.println("Error: cate 1 branch invalid rs/rt " +
                                rs + "," + rt);
                        failure = true;
                    }
                }
                else if (memory) {
                    // Get base, rt, offset
                    String base = s.substring(6, 11);
                    String rt = s.substring(11, 16);
                    String offset = s.substring(16);
                    int baseNum = Integer.parseInt(base, 2);
                    int rtNum = Integer.parseInt(rt, 2);
                    // NOTE: offset is signed!
                    Long offL = Long.parseLong(offset, 2);
                    int offNum = offL.intValue();
                    if (regFile.isIdxValid(baseNum) && regFile.isIdxValid(rtNum)) {
                        ins.targetReg = regFile.getRegPrefix() + Integer.toString(rtNum);
                        ins.sourceReg = regFile.getRegPrefix() + Integer.toString(baseNum);
                        ins.offset = Integer.toString(offNum);
                    } else {
                        System.out.println("Error: cate 1 memory invalid base/rt " +
                                base + "," + rt);
                        failure = true;
                    }
                }
                
                break;

            case 1:
                // Parse the opcode
                switch (opco) {
                    case 0:
                        ins.opcode = "ADD";
                        break;

                    case 1:
                        ins.opcode = "SUB";
                        break;

                    case 2:
                        ins.opcode = "AND";
                        break;

                    case 3:
                        ins.opcode = "OR";
                        break;

                    case 4:
                        ins.opcode = "SRL";
                        break;

                    case 5:
                        ins.opcode = "SRA";
                        break;

                    default:
                        System.out.println("Error: cate 2 unsupported opcode " + opc);
                        failure = true;
                        break;
                }
                // Get dest, src1 and src2
                if (!failure) {
                    dest = s.substring(6, 11);
                    String src1 = s.substring(11, 16);
                    String src2 = s.substring(16, 21);
                    int dIdx = Integer.parseInt(dest, 2);
                    int idx1 = Integer.parseInt(src1, 2);
                    int idx2 = Integer.parseInt(src2, 2);
                    if ((regFile.isIdxValid(dIdx)) && (regFile.isIdxValid(idx1)) && (regFile.isIdxValid(idx2))) {
                        ins.targetReg = regFile.getRegPrefix() + Integer.toString(dIdx);
                        ins.sourceReg = regFile.getRegPrefix() + Integer.toString(idx1);
                        if (("SRL".equals(ins.opcode)) || ("SRA".equals(ins.opcode)))
                            ins.sourceImm = Integer.toString(idx2);
                        else
                            ins.source2nd = regFile.getRegPrefix() + Integer.toString(idx2);
                    } else {
                        System.out.println("Error: cate 2 invalid dest/src idx " + dest + "," + src1 + "," + src2);
                        failure = true;
                    }
                }
                break;

            case 2:
                // Parse the opcode
                switch (opco) {
                    case 0:
                        ins.opcode = "ADDI";
                        break;

                    case 1:
                        ins.opcode = "ANDI";
                        break;

                    case 2:
                        ins.opcode = "ORI";
                        break;

                    default:
                        System.out.println("Error: cate 3 unsupported opcode " + opc);
                        failure = true;
                        break;
                }
                // Get dest, src and immediate
                if (!failure) {
                    dest = s.substring(6, 11);
                    String src = s.substring(11, 16);
                    String imm = s.substring(16);
                    int dIdx = Integer.parseInt(dest, 2);
                    int sIdx = Integer.parseInt(src, 2);
                    // NOTE: offset is signed!
                    Long valL = Long.parseLong(imm, 2);
                    int val = valL.intValue();
                    if ((regFile.isIdxValid(dIdx)) && regFile.isIdxValid(sIdx)) {
                        ins.targetReg = regFile.getRegPrefix() + Integer.toString(dIdx);
                        ins.sourceReg = regFile.getRegPrefix() + Integer.toString(sIdx);
                        ins.sourceImm = Integer.toString(val);
                    } else {
                        System.out.println("Error: cate 3 invalid dest/src indexes " + dest + "," + src);
                        failure = true;
                    }
                }
                break;

            case 3:
                // Parse the opcode
                if (opco == 0) {
                    ins.opcode = "MULT";
                    ins.targetReg = "LO";
                } else if (opco == 1) {
                    ins.opcode = "DIV";
                    ins.targetReg = "LO";
                    ins.target2nd = "HI";
                } else {
                    System.out.println("Error: cate 4 unsupported opcode " + opc);
                    failure = true;
                    break;
                }
                // Get src
                String src1 = s.substring(6, 11);
                String src2 = s.substring(11, 16);
                int idx1 = Integer.parseInt(src1, 2);
                int idx2 = Integer.parseInt(src2, 2);
                if ((regFile.isIdxValid(idx1)) && (regFile.isIdxValid(idx2))) {
                    ins.sourceReg = regFile.getRegPrefix() + Integer.toString(idx1);
                    ins.source2nd = regFile.getRegPrefix() + Integer.toString(idx2);
                } else {
                    System.out.println("Error: cate 4 invalid src index " + src1 + "," + src2);
                    failure = true;
                }
                break;

            case 4:
                // Parse the opcode
                if (opco == 0) {
                    ins.opcode = "MFHI";
                    ins.sourceReg = "HI";
                } else if (opco == 1) {
                    ins.opcode = "MFLO";
                    ins.sourceReg = "LO";
                } else {
                    System.out.println("Error: cate 5 unsupported opcode " + opc);
                    failure = true;
                    break;
                }
                // Get dest
                dest = s.substring(6, 11);
                int idx = Integer.parseInt(dest, 2);
                if (regFile.isIdxValid(idx)) {
                    ins.targetReg = regFile.getRegPrefix() + Integer.toString(idx);
                } else {
                    System.out.println("Error: cate 5 invalid dest idx " + dest);
                    failure = true;
                }
                break;

            default:
                System.out.println("Error: unknown category " + cat);
                failure = true;
                break;
        }

        // Update the PC
        address += stepAddress;

        if (failure) {
            return -1;
        }

        bin.genInstruction(ins);
        return 0;
    }

}
