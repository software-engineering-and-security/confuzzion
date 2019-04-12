package com.github.aztorius.confuzzion;

import java.lang.ClassLoader;

public class ByteClassLoader extends ClassLoader {
    public ByteClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> load(String className, byte[] data) {
        try {
            this.defineClass(className, data, 0, data.length, null);
            return this.loadClass(className);
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }
}
