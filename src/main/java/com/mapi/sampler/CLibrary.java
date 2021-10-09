package com.mapi.sampler;

import com.sun.jna.Library;
import com.sun.jna.Native;

import java.io.File;

public interface CLibrary extends Library {
    CLibrary INSTANCE = (CLibrary) Native.loadLibrary("/home/wuweihong/workspace/instance/encrptc.so", CLibrary.class);

    long retsetStates();

    void encryptc(byte[] namein, byte[] nameout, int nameint);

    void decryptc(byte[] dnamein, byte[] dnameout, int nameint);

}
