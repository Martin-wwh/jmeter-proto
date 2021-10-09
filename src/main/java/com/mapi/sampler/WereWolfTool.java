package com.mapi.sampler;

import com.mapi.sampler.CLibrary;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public class WereWolfTool {

    public WereWolfTool(){
        CLibrary.INSTANCE.retsetStates();
    }

    public byte[] encrypt(byte[] data, long pid){
        short len_data = (short) data.length;
        byte[] head_length_bytes = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort(len_data).array();
        byte[] pid_bytes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN).putLong(pid).array(); //小端字节序，无符号整型
        byte[] b_pid = Arrays.copyOfRange(pid_bytes, 0, 4);
        byte[] pidAndDataIn = new byte[b_pid.length+data.length];
        byte[] pidAndDataOut = new byte[b_pid.length+data.length];
        int len = 0;
        System.arraycopy(b_pid, 0, pidAndDataIn, len, b_pid.length);
        len += b_pid.length;
        System.arraycopy(data, 0, pidAndDataIn, len, data.length);
        int lengthIn = pidAndDataIn.length;
        CLibrary.INSTANCE.encryptc(pidAndDataIn, pidAndDataOut, lengthIn);
        byte[] result = new byte[head_length_bytes.length+pidAndDataOut.length];
        len = 0;
        System.arraycopy(head_length_bytes, 0, result, len, head_length_bytes.length);
        len += head_length_bytes.length;
        System.arraycopy(pidAndDataOut, 0, result, len, pidAndDataOut.length);
        return result;
    }

    public byte[] decrypt(byte[] data){
        byte[] pidAndDataIn = data;
        byte[] pidAndDataOut = new byte[data.length];
        CLibrary.INSTANCE.decryptc(pidAndDataIn, pidAndDataOut, pidAndDataIn.length);
        return pidAndDataOut;
    }



    public static void main(String[] args) {
        new WereWolfTool();
    }

}
