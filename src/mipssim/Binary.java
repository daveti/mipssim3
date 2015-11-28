/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

/**
 *
 * @author daveti
 * Binary expression
 * Oct 3, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Binary {
    
    public Instruction ins;
    public Data dat;
    public boolean isDat;
    public String bin;
    
    Binary(String bin) {
        this.bin = bin;
        this.ins = null;
        this.dat = null;
    }
    
    public void genInstruction(Instruction ins) {
        this.ins = ins;
        this.dat = null;
        this.isDat = false;
    }
    
    public void genData(Data dat) {
        this.dat = dat;
        this.ins = null;
        this.isDat = true;
    }
    
    @Override
    public String toString() {
        String s = "bin=" + bin + ", isDat=" + isDat;
        if ((ins != null) && (!isDat))
            s += "(ins[" + ins + "])";
        if ((dat != null) && (isDat))
            s += "(dat[" + dat + "])";
        return s;
    }
    
}
