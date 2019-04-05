package com.github.aztorius.confuzzion;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;

import janala.instrument.SnoopInstructionTransformer;

public class ByteClassLoader extends ClassLoader {
    private ClassFileTransformer transformer;

    public ByteClassLoader() {
        super();
        transformer = new SnoopInstructionTransformer();
    }

    public Class<?> load(String className, byte[] data) {
        byte[] bytes = data;
        byte[] transformedBytes;
        try {
            transformedBytes = transformer.transform(this, className, null, null, data);
        } catch(IllegalClassFormatException e) {
            transformedBytes = null;
        }
        if (transformedBytes != null) {
            bytes = transformedBytes;
        }
        return this.defineClass(className, bytes, 0, bytes.length);
    }
}
