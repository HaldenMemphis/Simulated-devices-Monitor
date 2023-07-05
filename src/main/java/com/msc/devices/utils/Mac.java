package com.msc.devices.utils;

import java.util.Random;


/**
 * @program: Simulated-devices-Monitor
 * @description: generate Mac
 * @author: yfliu
 * @create: 2023-07-05 14:31
 **/
public class Mac {


    public static String generateRandomMacAddress() {
        Random random = new Random();
        byte[] macAddr = new byte[6];
        random.nextBytes(macAddr);

        macAddr[0] = (byte)(macAddr[0] & (byte)254);  //zeroing last 2 bytes to make it unicast and locally adminstrated

        StringBuilder sb = new StringBuilder(18);
        for(byte b : macAddr){

            if(sb.length() > 0)
                sb.append(":");

            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
