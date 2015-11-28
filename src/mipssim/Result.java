/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mipssim;

/**
 *
 * @author daveti
 * Result
 * Nov 23, 2015
 * root@davejingtian.org
 * http://davejingtian.org
 */
public class Result {
    
    public int result;
    public int memory;
    public String destination;
    public boolean isDivResult;
    public int remainder;
    public int quotient;
    public int clock;
    
    Result (int res, int mem, String dest, boolean isDiv, int rem, int quo, int clc) {
        result = res;
        memory = mem;
        destination = dest;
        isDivResult = isDiv;
        remainder = rem;
        quotient = quo;
        clock = clc;
    }
    
    @Override
    public String toString() {
        String s;
        if (isDivResult)
            s = "remainder=" + Integer.toString(remainder) +
                    ", quotient=" + Integer.toString(quotient);
        else if (memory != -1)
            s = "result=" + Integer.toString(result) +
                    "memory=" + memory;
        else
            s = "result=" + Integer.toString(result) +
                    "destination=" + destination;
        return s;
    }
  
}
