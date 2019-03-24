package com.github.aztorius.confuzzion;

public class ByteClassLoader extends ClassLoader {
    public Class<?> load(String className, byte[] data) {
        return this.defineClass(className, data, 0, data.length);
    }
}
