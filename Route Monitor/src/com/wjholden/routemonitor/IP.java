package com.wjholden.routemonitor;

/**
 * 
 * @author William John Holden (wjholden@gmail.com)
 */
public class IP {
    public static String intToString(int i) {
        int ip[] = new int[4];
        for (int j = 0 ; j < ip.length ; j++) {
            ip[j] = (i >> (24 - 8 * j)) & 0xff;
        }
        return ip[0] + "." + ip[1] + "." + ip[2] + "." + ip[3];
    }
    
    public static int stringToInt(String i) {
        String ip[] = i.split("\\.");
        int j = 0;
        for (String k : ip) {
            j <<= 8;
            j |= Integer.parseInt(k);
        }
        return j;
    }
    
    public static void main(String args[]) {
        String[] tests = { "0.0.0.0", "255.255.255.255", "192.168.0.1" };
        for (String t : tests) {
            System.out.printf("%s = %s (%b)%n", t,
                    IP.intToString(IP.stringToInt(t)),
                    t.equals(IP.intToString(IP.stringToInt(t))));
        }
    }
}
