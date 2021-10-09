package com.mapi.utils;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

public class StringHash {
    private int[] crcTable = new int[256];
    private int crcPOLY = 0x04c11db7;

    public int[] getCrcTable() {
        return this.crcTable;
    }

    private boolean crcTableInitialized = false;

    long getUnsigned(int signed) {
        return signed >= 0 ? signed : 2 * (long) Integer.MAX_VALUE + 2 + signed;
    }

    public StringHash(){
        if(this.crcTableInitialized){
            return;
        }
        for(int i=0;i<this.crcTable.length;i++){
            int c = i<<24;

            for(int j=8;j>0;j--){
                c = c & 0xffffffff;
                if((c & 0x80000000)!=0){
                    c = (c<<1) ^ this.crcPOLY;
                }else {
                    c = c << 1;
                }
            }
            this.crcTable[i] = (int)(c & 0xffffffff);
        }
        this.crcTableInitialized = true;
    }

    public long getHash(String s){
        long hash = 0;
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        for(int k=0;k<bytes.length;k++){
            byte[] n = new byte[4];
            n[0] = bytes[k];
            int b = WereTransformUtils.bytes2Int(n, ByteOrder.LITTLE_ENDIAN);
            long t0 = hash >> 8;
            int i = (int) ((hash ^ b) & 0x000000FF);
            int c1 = this.crcTable[i];
            long c2 = getUnsigned(c1);
            hash = ((t0 & 0x00FFFFFF) ^ c2);
        }
        return hash;
    }

    public static void main(String[] args) {
        byte[] bs = {12, -95, -121, -33};
        int[] un = WereTransformUtils.bytes2UnsignedIntT(bs);
        for(int i=0;i<bs.length;i++){
            System.out.println(un[i]);
        }
        System.out.println(bs.toString());
    }
}
