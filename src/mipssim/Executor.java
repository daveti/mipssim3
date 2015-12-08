/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

import java.util.ArrayList;

/**
 *
 * @author daveti
 * Execute instructions (Simulation)
 * Oct 3, 2015
 * Support pipeline
 * Nov 23, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Executor {

    private final ArrayList<Binary> mem;
    private final Reg regFile;
    private final boolean debug = false;
    private final boolean debug2 = false;
    private final boolean debug3 = false;
    private final boolean debug4 = false;
    private final boolean debug5 = false;
    private final boolean debug6 = false;
    private int pc;
    private int cycle;
    private int order;
    private int dataIdx;
    private int dataNum;

    // Score board buffers
    private Queue<Instruction> buff1;
    private final Queue<Instruction> buff2;
    private final Queue<Instruction> buff3;
    private final Queue<Instruction> buff4;
    private final Queue<Instruction> buff5;
    private final Queue<Instruction> buff6;
    private final Queue<Instruction> buff7;
    private final Queue<Instruction> buff8;
    private final Queue<Instruction> buff9;
    private final Queue<Instruction> buff10;
    private final Queue<Instruction> buff11;
    private final Queue<Instruction> buff12;
    private final Queue<Instruction> traceBoard;    // used by IF
    private final Queue<Instruction> scoreBoard;    // used by IS
    private final Queue<Instruction> buff1Board;    // used by IS
    private final Queue<Instruction> buff7Board;    // used by DIV

    // For IF
    private Instruction waiting;
    private Instruction executed;
    private boolean ifStall;
    private boolean cleanExe;

    // Misc.
    private final int ifMax = 4;
    private final int isPerBufMax = 2;
    private final int wbPerBufMax = 1;

    Executor(ArrayList<Binary> mem, Reg regFile) {
        this.mem = mem;
        this.regFile = regFile;
        this.pc = 0;
        this.cycle = 0;
        this.order = 0;

        // Init buffers
        buff1 = new Queue(8);
        buff2 = new Queue(2);
        buff3 = new Queue(2);
        buff4 = new Queue(2);
        buff5 = new Queue(2);
        buff6 = new Queue(2);   // The capacity should be 1
        buff7 = new Queue(2);   // We set it 2 to allow the parallism
        buff8 = new Queue(2);   // The same below...
        buff9 = new Queue(2);
        buff10 = new Queue(2);
        buff11 = new Queue(2);
        buff12 = new Queue(2);
        traceBoard = new Queue();
        scoreBoard = new Queue();
        buff1Board = new Queue(8);
        buff7Board = new Queue();

        waiting = null;
        executed = null;
        ifStall = false;
        cleanExe = false;
    }

    private int getDataIdxMem() {
        for (Binary bin : mem) {
            if (bin.isDat) {
                return mem.indexOf(bin);
            }
        }

        return -1;
    }

    private int getDataNumMem() {
        int t = 0;
        for (Binary bin : mem) {
            if (bin.isDat) {
                t++;
            }
        }

        return t;
    }

    private int getInsIdxMemAddr(int addr) {
        for (Binary bin : mem) {
            if (bin.isDat) {
                break;
            }
            if (bin.ins.address == addr) {
                return mem.indexOf(bin);
            }
        }

        return -1;
    }

    private int getDataIdxMemAddr(int addr) {
        for (Binary bin : mem) {
            if ((bin.isDat) && (bin.dat.address == addr)) {
                return mem.indexOf(bin);
            }
        }

        return -1;
    }

    private boolean waitForNextCycle(Queue<Instruction> q) {
        return q.peek().clock >= cycle;
    }

    private void wbUnit() {
        // Mark instructions to be done
        // buff10 for LW - no SW pushed into here!
        // buff7 for DIV
        // buff12 for MULT
        // buff9 for others
        Instruction i;
        if (!buff10.isEmpty() && !waitForNextCycle(buff10)) {
            i = buff10.dequeue();
            regFile.setRegVal(i.result.destination, i.result.result);
            i.done = true;
        }
        if (!buff7.isEmpty() && !waitForNextCycle(buff7)) {
            i = buff7.dequeue();
            regFile.setRegLoVal(i.result.quotient);
            regFile.setRegHiVal(i.result.remainder);
            i.done = true;
        }
        if (!buff12.isEmpty() && !waitForNextCycle(buff12)) {
            i = buff12.dequeue();
            regFile.setRegLoVal(i.result.result);
            // HACK: our MULT would not update "HI"
            i.done = true;
        }
        if (!buff9.isEmpty() && !waitForNextCycle(buff9)) {
            i = buff9.dequeue();
            regFile.setRegVal(i.result.destination, i.result.result);
            i.done = true;
        }
    }

    private void mul3Unit() {
        // buff11 -> buff12
        if (buff11.isEmpty() || buff12.isFull() || waitForNextCycle(buff11)) {
            return;
        }

        Instruction i = buff11.dequeue();
        // Compute the result
        // Get rs, rt values
        int rs = regFile.getRegVal(i.sourceReg);
        int rt = regFile.getRegVal(i.source2nd);
        // (HI,LO) <- rs*rt
        long mul = (long) rs * (long) rt;
        // Split the 64 bits into 2 32-bits
        String hex = Long.toHexString(mul);
        if (debug) {
            System.out.println("Debug: hex=" + hex);
        }
        int padLen = 16 - hex.length();
        String pad = "";
        for (int j = 0; j < padLen; j++) {
            pad += "0";
        }
        hex = pad + hex;
        String hiStr = hex.substring(0, 8);
        String loStr = hex.substring(8);
        Long hiL = Long.parseLong(hiStr, 16);
        int hiVal = hiL.intValue();
        Long loL = Long.parseLong(loStr, 16);
        int loVal = loL.intValue();

        // Generate the result
        // NOTE: the hi value will be discarded per requirement
        i.result = new Result(loVal, -1, "LO", false, -1, -1, cycle);
        i.setClock(cycle);
        buff12.enqueue(i);
    }

    private void mul2Unit() {
        // buff8 -> buff11
        if (buff8.isEmpty() || buff11.isFull() || waitForNextCycle(buff8)) {
            return;
        }

        Instruction i = buff8.dequeue();
        // Leave the computation to the next stage
        i.setClock(cycle);
        buff11.enqueue(i);
    }

    private void memUnit() {
        // buff6 -> buff10
        if (buff6.isEmpty() || buff10.isFull() || waitForNextCycle(buff6)) {
            return;
        }

        Instruction i = buff6.dequeue();
        // Compute the result
        int res = -1;
        switch (i.opcode) {
            case "SW":
                // Write the result into memory
                int rt = i.result.result;
                int idx = i.result.memory;
                if (idx != -1) {
                    mem.get(idx).dat.data = rt;
                } else {
                    System.out.println("Error: invalid idx from getDataIdxMemAddr for SW");
                }
                break;

            case "LW":
                // Load the memory into reg
                int idxL = i.result.memory;
                if (idxL != -1) {
                    res = mem.get(idxL).dat.data;
                    i.result.result = res;
                    i.setClock(cycle);
                    buff10.enqueue(i);
                } else {
                    System.out.println("Error invalid idx from getDataIdxMemAddr for LW");
                }
                break;
                
            default:
                System.out.println("Error: unsupported opcode in buff6" + i.opcode);
                break;
        }
    }

    private void alu1Unit() {
        // buff5 -> buff9
        if (buff5.isEmpty() || buff9.isFull() || waitForNextCycle(buff5)) {
            return;
        }

        Instruction i = buff5.dequeue();
        if (debug)
            System.out.println("Debug: buff5 dequeue - " + i + " at cycle" + cycle);
        // Compute the result
        int res = -1;
        switch (i.category) {
            case 2:
                int src1 = regFile.getRegVal(i.sourceReg);
                int src2;
                if (("SRL".equals(i.opcode)) || ("SRA".equals(i.opcode)))
                    src2 = Integer.parseInt(i.sourceImm);
                else
                    src2 = regFile.getRegVal(i.source2nd);
                
                switch (i.opcode) {
                    case "ADD":
                        // rt <- rs1 + rs2
                        res = src1 + src2;
                        break;

                    case "SUB":
                        res = src1 - src2;
                        break;

                    case "AND":
                        res = src1 & src2;
                        break;

                    case "OR":
                        res = src1 | src2;
                        break;

                    case "SRL":
                        // Logical right shift
                        res = src1 >>> src2;
                        break;

                    case "SRA":
                        // Arithmetic right shift
                        res = src1 >> src2;
                        break;

                    default:
                        System.out.println("Error: cate 2 unsupported opcode " + i.opcode);
                        break;
                }
                break;

            case 3:
                int src = regFile.getRegVal(i.sourceReg);
                int imm = Integer.parseInt(i.sourceImm);
                switch (i.opcode) {
                    case "ADDI":
                        // rt <- rs + imm
                        res = src + imm;
                        break;

                    case "ANDI":
                        // rt <- rs AND imm
                        res = src & imm;
                        break;

                    case "ORI":
                        // rt <- rs OR imm
                        res = src | imm;
                        break;

                    default:
                        System.out.println("Error: cate 3 unsupported opcode " + i.opcode);
                        break;
                }
                break;

            case 5:
                switch (i.opcode) {
                    case "MFHI":
                        // Copy HI to targetReg
                        int hiVal = regFile.getRegHiVal();
                        res = hiVal;
                        break;

                    case "MFLO":
                        // Copy LO to targetReg
                        int loVal = regFile.getRegLoVal();
                        res = loVal;
                        break;

                    default:
                        System.out.println("Error: cate 5 unsupported opcode " + i.opcode);
                        break;
                }
                break;

            default:
                break;
        }

        // Generate the result and push it to WB
        i.result = new Result(res, -1, i.targetReg, false, -1, -1, cycle);
        i.bufPhase = 9;
        i.setClock(cycle);
        buff9.enqueue(i);
    }

    private void mul1Unit() {
        // buff4 -> buff8
        if (buff4.isEmpty() || buff8.isFull() || waitForNextCycle(buff4)) {
            return;
        }

        Instruction i = buff4.dequeue();
        // Leave the computation to the next stage
        i.setClock(cycle);
        buff8.enqueue(i);
    }

    private void divUnit() {
        // Peek buff7Board to see if any div is done
        if ((!buff7Board.isEmpty()) && ((cycle-buff7Board.peek().clock) >= 3)) {
            Instruction i = buff7Board.dequeue();
            i.setClock(cycle);
            buff7.enqueue(i);
            if (debug5)
                System.out.println("Debug5: i=" + i + " done" +
                        "with clock=" +  i.clock + " at cycle=" + cycle);
        }
        
        // buff3 -> buff7
        if (buff3.isEmpty() || buff7.isFull() || waitForNextCycle(buff3)) {
            return;
        }

        Instruction i = buff3.dequeue();
        // Compute the remainder and quotient
        int rs = regFile.getRegVal(i.sourceReg);
        int rt = regFile.getRegVal(i.source2nd);
        // (HI,LO) <- rs/rt
        int quo = rs / rt;
        int rem = rs % rt;

        // Generate the result
        i.result = new Result(-1, -1, null, true, rem, quo, cycle);
        i.setClock(cycle);

        // Enqueue to buff7Board before 4 cycles are passed
        buff7Board.enqueue(i);
    }

    private void alu2Unit() {
        // buff2 -> buff6
        if (buff2.isEmpty() || buff6.isFull() || waitForNextCycle(buff2)) {
            return;
        }

        Instruction i = buff2.dequeue();
        i.setClock(cycle);
        // Compute the result - just the memory address
        int res = -1;
        switch (i.opcode) {
            case "SW":
                // Get rt, base, offset values
                int rt = regFile.getRegVal(i.targetReg);
                int base = regFile.getRegVal(i.sourceReg);
                int offset = Integer.parseInt(i.offset);
                // memory[base+offset] <- rt
                int idx = getDataIdxMemAddr(base + offset);
                if (idx != -1) {
                    i.result = new Result(rt, idx, null, false, -1, -1, cycle);
                    // mem.get(idx).dat.data = rt;
                    // Mark this SW done to parallel the IS unit
                    i.done = true;
                } else {
                    System.out.println("Error: invalid idx from getDataIdxMemAddr for SW");
                }
                break;

            case "LW":
                // Get base, offset values
                int baseL = regFile.getRegVal(i.sourceReg);
                int offsetL = Integer.parseInt(i.offset);
                // rt <- memory[base+offset]
                int idxL = getDataIdxMemAddr(baseL + offsetL);
                if (idxL != -1) {
                    //res = mem.get(idxL).dat.data;
                    i.result = new Result(-1, idxL, i.targetReg, false, -1, -1, cycle);
                    i.setClock(cycle);
                    //buff10.enqueue(i);
                } else {
                    System.out.println("Error invalid idx from getDataIdxMemAddr for LW");
                }
                break;
                
            default:
                System.out.println("Error: unsupported opcode in buff6" + i.opcode);
                break;
        }
        buff6.enqueue(i);
    }
    
    private boolean readyToDigest4Alu1(Instruction i) {
        switch (i.bufPhase) {
            case 9:
                if (i.clock < cycle)
                    return true;
                break;
                
            case 5:
                if ((!buff9.isFull()) && (i.clock < cycle))
                    return true;
                break;
                
            default:
                break;
        }
        
        return false;
    }

    private boolean noHazardWAR(String name, String src1, String src2, String dst) {
        // No WAR with earlier not-issued - buff1Board
        // No WAR wtih issued but not-finished - scoreBoard (simulation cycle 26-28)
        // Check dst against instruction's src regs
        // Skip SW - since all the regs are read-only
        if ("SW".equals(name)) {
            return true;
        }

        // Hack for DIV
        String dst2 = null;
        if ("DIV".equals(name)) {
            dst = "LO";
            dst2 = "HI";
        }
        
        ArrayList<Instruction> warList = new ArrayList();
        for (Instruction i : buff1Board)
            warList.add(i);
        for (Instruction i : scoreBoard)
            if (!i.done)
                warList.add(i);
        //ArrayList<Instruction> buffList = buff1Board.toArrayList();
        if (debug6)
            System.out.println("Debug6: warList=" + warList);
        for (Instruction i : warList) {
            switch (i.opcode) {
                case "LW":
                    if (dst.equals(i.sourceReg)) {
                        return false;
                    }
                    break;

                case "SW":
                    if ((dst.equals(i.sourceReg)) || (dst.equals(i.targetReg))) {
                        if (debug2)
                            System.out.println("Debug: WAR with " + i + " for name " +
                                    name + " at cycle " + cycle);
                        return false;
                    }
                    break;

                case "ADD":
                case "SUB":
                case "AND":
                case "OR":
                    if ((dst.equals(i.sourceReg)) || (dst.equals(i.source2nd))) {
                        if (!readyToDigest4Alu1(i))
                            return false;
                    }
                    break;
                    
                case "MULT":
                case "DIV":
                    if ((dst.equals(i.sourceReg)) || (dst.equals(i.source2nd))) {
                        return false;
                    }
                    break;
                    
                case "SRL":
                case "SRA":
                    if ((dst.equals(i.sourceReg)) && (!readyToDigest4Alu1(i)))
                        return false;
                    break;

                case "ADDI":
                case "ANDI":
                case "ORI":
                    if ((dst.equals(i.sourceReg)) && (!readyToDigest4Alu1(i))) {
                        if (debug4)
                            System.out.println("Debug4: name=" + name + ", src1=" +
                                    src1 + ", src2=" + src2 + ", dst" + dst +
                                    " WAR with i=" + i);
                        return false;
                    }
                    break;

                case "MFHI":
                    if (("HI".equals(dst)) || ("DIV".equals(name))) {
                        if (!readyToDigest4Alu1(i))
                            return false;
                    }
                    break;

                case "MFLO":
                    if (("LO".equals(dst)) && (!readyToDigest4Alu1(i))) {
                        return false;
                    }
                    break;

                default:
                    System.out.println("Error: unsupported opcode in buff1Board " + i.opcode);
                    break;
            }
        }

        return true;
    }

    private boolean noHazardWAW(String name, String src1, String src2, String dst) {
        // No WAW either with issued but not finished - scoreBoard
        // or earlier not-issued - buff1Board
        // Check the dst against instruciton's dst
        // Skip SW
        if ("SW".equals(name)) {
            return true;
        }

        // Hack for DIV
        String dst2 = null;
        if ("DIV".equals(name)) {
            dst = "LO";
            dst2 = "HI";
        }

        ArrayList<Instruction> wawList = new ArrayList();
        ArrayList<Instruction> scoreList = scoreBoard.toArrayList();
        for (Instruction i : scoreList) {
            if (!i.done) {
                wawList.add(i);
            }
        }
        ArrayList<Instruction> buffList = buff1Board.toArrayList();
        for (Instruction i : buffList) {
            wawList.add(i);
        }

        for (Instruction i : wawList) {
            if (dst.equals(i.targetReg)) {
                return false;
            }
            // Only DIV would WAW DIV for reg "HI"
            if (("DIV".equals(name)) && ("DIV".equals(i.opcode))) {
                return false;
            }
        }

        return true;
    }

    private boolean noHazardRAW(String name, String src1, String src2, String dst) {
        // No RAW either with issued but not-finished - scoreBoard
        // or earlier not-issued - buff1Board
        // Check src1/src2 against instruction's dst
        ArrayList<Instruction> rawList = new ArrayList();
        ArrayList<Instruction> scoreList = scoreBoard.toArrayList();
        for (Instruction i : scoreList) {
            if (!i.done) {
                rawList.add(i);
            }
        }
        ArrayList<Instruction> buffList = buff1Board.toArrayList();
        for (Instruction i : buffList) {
            rawList.add(i);
        }

        for (Instruction i : rawList) {
            // Skip SW since not dst involved
            if ("SW".equals(i.opcode)) {
                continue;
            }
            if (src1.equals(i.targetReg)) {
                return false;
            }
            if ((src2 != null) && (src2.equals(i.targetReg))) {
                return false;
            }
            if ("DIV".equals(i.opcode)) {
                if (src1.equals(i.target2nd)) {
                    return false;
                }
                if ((src2 != null) && (src2.equals(i.target2nd))) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean noHazard(String name, String src1, String src2, String dst) {
        boolean noRAW = noHazardRAW(name, src1, src2, dst);
        boolean noWAW = noHazardWAW(name, src1, src2, dst);
        boolean noWAR = noHazardWAR(name, src1, src2, dst);
        if (debug6)
            System.out.println("Debug: noRAW " + noRAW + ", noWAW " + noWAW +
                    ", noWAR " + noWAR + ", for i: " + name +
                    " at cycle " + cycle);
        return noRAW && noWAW && noWAR;
    }

    private boolean noHazardMem(String name, String src, String dst) {
        // 1. For LW/SW, all of the source registers are ready
        // 2. The LW must wait for all previous SW are issued
        // 3. The SW must be issued in order
        ArrayList<Instruction> scoreList = scoreBoard.toArrayList();
        for (Instruction i : scoreList) {
            if (!i.done) {
                if (null != name) {
                    switch (name) {
                        case "LW":
                            // Check src reg
                            // against non-SW target reg
                            if ((!"SW".equals(i.opcode)) && (!"".equals(i.targetReg)) && (src.equals(i.targetReg))) {
                                return false;
                            }
                            break;
                        case "SW":
                            // Check both src and dst regs
                            // against non-SW target reg
                            if ((!"SW".equals(i.opcode)) && (!"".equals(i.targetReg)) && (src.equals(i.targetReg))) {
                                return false;
                            }
                            if ((!"SW".equals(i.opcode)) && (!"".equals(i.targetReg)) && (dst.equals(i.targetReg))) {
                                return false;
                            }
                            break;
                    }
                }
            }
        }

        ArrayList<Instruction> buffList = buff1Board.toArrayList();
        for (Instruction i : buffList) {
            if ("SW".equals(i.opcode)) {
                if (("LW".equals(name)) || ("SW".equals(name))) // Not needed
                {
                    return false;
                }
            }
        }

        return true;
    }

    private void isUnit() {
        // Look at the buff1
        if (buff1.isEmpty()) {
            return;
        }

        // Play with arrayList rather than Queue
        ArrayList<Instruction> buff1List = buff1.toArrayList();
        if (debug)
            System.out.println("Debug: " + buff1List);

        // NOTE that we cannot remove items while iterating!
        buff1Board.clearqueue();
        boolean issued = false;
        String src1, src2, dst;
        for (Instruction i : buff1List) {
            if (i.clock >= cycle) {
                buff1Board.enqueue(i);
                continue;
            }

            issued = false;
            switch (i.category) {
                case 1:
                    if (buff2.isFull()) {
                        break;
                    }

                    switch (i.opcode) {
                        case "LW":
                            src1 = i.sourceReg;
                            dst = i.targetReg;
                            if (noHazardMem("LW", src1, dst) && noHazard("LW", src1, null, dst)) {
                                buff2.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            }
                            break;

                        case "SW":
                            src1 = i.targetReg;
                            dst = i.sourceReg;
                            if (noHazardMem("SW", src1, dst) && noHazard("SW", src1, dst, null)) {
                                buff2.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            }
                            break;

                        default:
                            System.out.println("Error: cate 1 unsupported opcode in IS: " + i.opcode);
                            break;
                    }
                    break;

                case 2:
                    if (buff5.isFull()) {
                        break;
                    }

                    switch (i.opcode) {
                        case "ADD":
                        case "SUB":
                        case "AND":
                        case "OR":
                        case "SRL":
                        case "SRA":
                            src1 = i.sourceReg;
                            if (("SRL".equals(i.opcode)) || ("SRA".equals(i.opcode)))
                                src2 = null;
                            else
                                src2 = i.source2nd;
                            dst = i.targetReg;
                            if (noHazard(i.opcode, src1, src2, dst)) {
                                i.bufPhase = 5;
                                buff5.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                                if (debug)
                                    System.out.println("Debug: buff5 - " + buff5);
                            }
                            break;

                        default:
                            System.out.println("Error: cate 2 unsupported opcode in IS: " + i.opcode);
                            break;
                    }
                    break;

                case 3:
                    if (buff5.isFull()) {
                        break;
                    }

                    switch (i.opcode) {
                        case "ADDI":
                        case "ANDI":
                        case "ORI":
                            src1 = i.sourceReg;
                            dst = i.targetReg;
                            if (noHazard(i.opcode, src1, null, dst)) {
                                i.bufPhase = 5;
                                buff5.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            } else {
                                if (debug2)
                                    System.out.println("Debug: hazard for i - " + i + " at cycle " + cycle);
                            }
                            break;

                        default:
                            System.out.println("Error: cate 3 unsupported opcode in IS: " + i.opcode);
                            break;
                    }
                    break;

                case 4:
                    switch (i.opcode) {
                        case "MULT":
                            if (buff4.isFull()) {
                                break;
                            }

                            src1 = i.sourceReg;
                            src2 = i.source2nd;
                            dst = "LO";
                            if (noHazard("MULT", src1, src2, dst)) {
                                buff4.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            }
                            break;

                        case "DIV":
                            if (buff3.isFull()) {
                                break;
                            }

                            src1 = i.sourceReg;
                            src2 = i.source2nd;
                            dst = null; // Hack!
                            if (noHazard("DIV", src1, src2, dst)) {
                                buff3.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            }
                            break;

                        default:
                            System.out.println("Error: cate 4 unsupported opcode in IS: " + i.opcode);
                            break;
                    }
                    break;

                case 5:
                    if (buff5.isFull()) {
                        break;
                    }

                    switch (i.opcode) {
                        case "MFHI":
                            src1 = "HI";
                            dst = i.targetReg;
                            if (noHazard("MFHI", src1, null, dst)) {
                                i.bufPhase = 5;
                                buff5.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            }
                            break;

                        case "MFLO":
                            src1 = "LO";
                            dst = i.targetReg;
                            if (noHazard("MFLO", src1, null, dst)) {
                                i.bufPhase = 5;
                                buff5.enqueue(i);
                                scoreBoard.enqueue(i);
                                issued = true;
                            }
                            break;

                        default:
                            System.out.println("Error: cate 5 unsupported opcode in IS: " + i.opcode);
                            break;
                    }
                    break;

                default:
                    System.out.println("Error: unknown category " + Integer.toString(i.category));
                    break;
            }

            // Add into buff1Board
            if (!issued) {
                buff1Board.enqueue(i);
            } else {
                // Need to update the clock
                i.setClock(cycle);
                if (debug)
                    System.out.println("Debug: issued - " + i);
            }
        }

        // Update the buff1 to be buff1Board
        // since we have gone thru the original buff1 and
        // all issued are moved into scoreBard
        buff1 = buff1Board;
    }

    /* Remove the finished instructions */
    private void updateScoreBoard() {
        // I admit that the code below could be the ugliest I have ever written...
        // partially due to the fact that I am writing in Java...
        // But who cares...
        // At least, I do not care:)
        // Nov 25, 2015
        // daveti
        ArrayList<Instruction> sbArray = scoreBoard.toArrayList();
        scoreBoard.clearqueue();
        for (Instruction i : sbArray) {
            if (i.done == false) {
                scoreBoard.enqueue(i);
            }
        }

    }

    private boolean keepIfStall() {
        // Go thur the trace buffer to make sure all the
        // instructions in the fly before the branch is done
        if (traceBoard.isEmpty() || waiting == null)
            return false;
        
        for (Instruction i : traceBoard) {
            if (debug3)
                System.out.println("Debug: i - " + i + ", done=" + i.done);
            if (i.done == false) {
                if (debug3)
                    System.out.println("Debug: wt " + waiting.targetReg +
                            ", ws " + waiting.sourceReg + ", tR " + i.targetReg);
                if (waiting.targetReg.equals(i.targetReg))
                    return true;
                if (waiting.sourceReg.equals(i.targetReg))
                    return true;
            }
        }

        return false;
    }

    /* Return the new target address */
    private int processBranch(Instruction ins) {
        if (ins.category == 1) {
            switch (ins.opcode) {
                case "J":
                    return getInsIdxMemAddr(Integer.parseInt(ins.targetAdr));

                case "BEQ":
                case "BNE":
                case "BGTZ":
                    // Get rt, rs, offset values
                    int rtB = regFile.getRegVal(ins.targetReg);
                    int rsB = regFile.getRegVal(ins.sourceReg);
                    int offsetB = Integer.parseInt(ins.offset);
                    int addr;
                    if (("BEQ".equals(ins.opcode) && (rtB == rsB))
                            || ("BNE".equals(ins.opcode) && (rtB != rsB))
                            || ("BGTZ".equals(ins.opcode) && (rsB > 0))) {
                        // Branch to 18-bit(offset)+address(nextIns)
                        addr = offsetB + mem.get(pc + 1).ins.address;
                        return getInsIdxMemAddr(addr);
                    } else {
                        return pc + 1;
                    }

                default:
                    System.out.println("Error: unsupported branch opcode " + ins.opcode);
                    break;
            }
        }

        return -1;
    }

    /* Return 1 to stop the whole simulation */
    private int ifUnit() {
        Binary bin;
        Instruction ins;
        
        if (debug)
            System.out.println("Debug: ifStall=" + ifStall + " at cycle " + cycle);

        // Check for print cleaning left in the last cycle
        if (cleanExe) {
            executed = null;
            waiting = null;
            cleanExe = false;
        }

        // Check for stall
        if (buff1.isFull()) {
            if (debug)
                System.out.println("Debug: buff1 is full at cycle " + cycle);
            return 0;
        }
        if ((ifStall) && (keepIfStall())) {
            if (debug)
                System.out.println("Debug: IF stall at cycle " + cycle);
            return 0;
        }

        // Continue from stall
        if (ifStall) {
            if (debug3) {
                System.out.println("Debug: IF stall reset at cycle " + cycle);
                System.out.println("Debug: waiting - " + waiting);
                System.out.println("Debug: traceBoard - " + traceBoard);
            }
            // Clear the trace board
            traceBoard.clearqueue();
            // Update IF buffer
            executed = waiting;
            waiting = null;
            // Execute the executed
            pc = processBranch(executed);
            cleanExe = true;
            ifStall = false;
            return 0;
        }

        // Go thru the max possible fetching
        int i;
        for (i = 0; i < ifMax; i++) {
            // Read in the instruction
            bin = mem.get(pc);
            if (bin.isDat) {
                System.out.println("Error: data during execution");
                break;
            }
            ins = bin.ins;
            
            // NOTE: mark the instruction as new!
            ins.done = false;

            // Decode
            if (ins.category == 1) {
                switch (ins.opcode) {
                    case "J":
                        executed = ins;
                        pc = processBranch(ins);
                        cleanExe = true;
                        return 0;

                    case "BEQ":
                    case "BNE":
                    case "BGTZ":
                        waiting = ins;
                        ifStall = true;
                        return 0;

                    case "SW":
                    case "LW":
                        break;

                    case "BREAK":
                        // Execute the break now
                        executed = ins;
                        // Stop fetching and simulation
                        return 1;

                    default:
                        System.out.println("Error: cate 1 unsupported opcode " + ins.opcode);
                        break;
                }
            }

            // Add the instruction into the buff1 and traceBoard
            ins.setClock(cycle);
            ins.setOrder(order);
            buff1.enqueue(ins);
            traceBoard.enqueue(ins);

            // Update the pc and order
            pc++;
            order++;
            
            // Check if the buff1 is full
            if (buff1.isFull())
                return 0;
        }

        return 0;
    }

    public void pipeline() {
        int end;

        // After decoding, the memory is not empty
        dataIdx = getDataIdxMem();
        dataNum = getDataNumMem();

        while (true) {
            // Start clock
            cycle += 1;

            // NOTE: all of thoese stages are in parallel!
            // We use serial simulation with the help of the
            // clock control for each instruciton to simulate
            // a pipeline.
            // Nov 23, 2015
            // daveti
            // Fetch
            end = ifUnit();
            
            // Try to call Mem early~
            alu2Unit();
            //alu1Unit();
            memUnit();

            // Issue
            isUnit();

            // AluX
            alu1Unit();
            //alu2Unit();

            // Div
            divUnit();

            // MulX
            mul1Unit();
            mul2Unit();
            mul3Unit();

            // Mem and WB
            //memUnit();
            wbUnit();

            // Dump the simulation results
            dumpSim();

            // Update the score board
            updateScoreBoard();

            if (end == 1) {
                break;
            }
        }

    }

    private void dumpBuff12() {
        if (buff12.isEmpty()) {
            System.out.println("Buf12:");
            return;
        }
        
        Instruction i = buff12.peek();
        if (i.clock > cycle)
            System.out.println("Buf12:");
        else
            System.out.println("Buf12: [" + i.result.result + "]");
    }

    private void dumpBuff11() {
        if (buff11.isEmpty()) {
            System.out.println("Buf11:");
            return;
        }
        
        Instruction i = buff11.peek();
        if (i.clock > cycle)
            System.out.println("Buf11:");
        else
            System.out.println("Buf11: [" + i.dump() + "]");
    }

    private void dumpBuff10() {
        if (buff10.isEmpty()) {
            System.out.println("Buf10:");
            return;
        }
        
        Instruction i = buff10.peek();
        if (i.clock > cycle)
            System.out.println("Buf10:");
        else
            System.out.println("Buf10: [" + i.result.result + ", " + i.result.destination + "]");
    }

    private void dumpBuff9() {
        if (buff9.isEmpty()) {
            System.out.println("Buf9:");
            return;
        }
        
        Instruction i = buff9.peek();
        if (i.clock > cycle)
            System.out.println("Buf9:");
        else
            System.out.println("Buf9: [" + i.result.result + ", " + i.result.destination + "]");
    }

    private void dumpBuff8() {
        if (buff8.isEmpty()) {
            System.out.println("Buf8:");
            return;
        }
        
        Instruction i = buff8.peek();
        if (i.clock > cycle)
            System.out.println("Buf8:");
        else
            System.out.println("Buf8: [" + i.dump() + "]");
    }

    private void dumpBuff7() {
        if (buff7.isEmpty()) {
            System.out.println("Buf7:");
            return;
        }
        
        Instruction i = buff7.peek();
        if (i.clock > cycle)
            System.out.println("Buf7:");
        else
            System.out.println("Buf7: [" + i.result.remainder + ", " + i.result.quotient + "]");
    }

    private void dumpBuff6() {
        if (buff6.isEmpty()) {
            System.out.println("Buf6:");
            return;
        }
        
        Instruction i = buff6.peek();
        if (i.clock > cycle)
            System.out.println("Buf6:");
        else
            System.out.println("Buf6: [" + i.dump() + "]");
    }
    
    private void dumpBuffX(Queue<Instruction> q, int len) {
        ArrayList<Instruction> buffList =  q.toArrayList();
        int j = 0;
        for (Instruction i : buffList) {
            System.out.println("\tEntry " + j + ": [" + i.dump() + "]");
            j++;
        }
        
        while (j < len) {
            System.out.println("\tEntry " + j + ":");
            j++;
        }
    }

    private void dumpBuff5() {
        dumpBuffX(buff5, 2);
    }

    private void dumpBuff4() {
        dumpBuffX(buff4, 2);
    }

    private void dumpBuff3() {
        dumpBuffX(buff3, 2);
    }

    private void dumpBuff2() {
        dumpBuffX(buff2, 2);
    }

    private void dumpBuff1() {
        dumpBuffX(buff1, 8);
    }

    private void dumpIf() {
        if (waiting != null)
            System.out.println("\tWaiting: [" + waiting.dump() + "]");
        else
            System.out.println("\tWaiting:");
        if (executed != null)
            System.out.println("\tExecuted: [" + executed.dump() + "]");
        else
            System.out.println("\tExecuted:");
    }

    private void dumpData() {
        int rep = dataNum / regFile.getRegSep();
        int rem = dataNum % regFile.getRegSep();
        if (debug) {
            System.out.println("dataNum=" + dataNum + ", sep="
                    + regFile.getRegSep() + ", rep=" + rep + ", rem=" + rem);
        }
        String tmp;
        String tmp2;
        for (int i = 0; i < rep; i++) {
            tmp = "";
            for (int j = 0; j < regFile.getRegSep(); j++) {
                tmp += String.format("%8d", mem.get(dataIdx + i * regFile.getRegSep() + j).dat.data);
            }
            tmp2 = String.format("%d:", mem.get(dataIdx + i * regFile.getRegSep()).dat.address);
            System.out.println(tmp2 + tmp);
        }

        if (rem != 0) {
            // Handle the left ones
            tmp = "";
            for (int k = 1; k <= rem; k++) {
                tmp += String.format("%8d", mem.get(dataIdx + rep * regFile.getRegSep() + k).dat.data);
            }
            tmp2 = String.format("%d:", mem.get(dataIdx + rep * regFile.getRegSep()).dat.address);
            System.out.println(tmp2 + tmp);
        }
    }

    private void dumpSim() {

        System.out.println("--------------------");
        System.out.println("Cycle " + cycle + ":");
        System.out.println();

        // Dump IF
        System.out.println("IF:");
        dumpIf();
        // Dump buffs
        System.out.println("Buf1:");
        dumpBuff1();
        System.out.println("Buf2:");
        dumpBuff2();
        System.out.println("Buf3:");
        dumpBuff3();
        System.out.println("Buf4:");
        dumpBuff4();
        System.out.println("Buf5:");
        dumpBuff5();
        dumpBuff6();
        dumpBuff7();
        dumpBuff8();
        dumpBuff9();
        dumpBuff10();
        dumpBuff11();
        dumpBuff12();
        System.out.println();

        // Dump the reg files
        System.out.println("Registers");
        regFile.dumpReg();
        System.out.println();

        // Dump the data
        System.out.println("Data");
        dumpData();
        System.out.println();
    }

}
