/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

/**
 *
 * @author daveti
 * Data
 * Oct 2, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Data {
    
    public int data;
    public int address;
    
    Data (int d, int addr) {
        data = d;
        address = addr;
    }
    
    @Override
    public String toString() {
        String s = "data=" + Integer.toString(data) +
                ", address=" + Integer.toString(address);
        return s;
    }
  
}
