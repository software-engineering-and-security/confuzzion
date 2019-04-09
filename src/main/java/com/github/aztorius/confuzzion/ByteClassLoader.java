package com.github.aztorius.confuzzion;

import java.lang.ClassLoader;

public class ByteClassLoader extends ClassLoader {
    public ByteClassLoader() {
        super(Thread.currentThread().getContextClassLoader());
    }

    public Class<?> load(String className, byte[] data) {
        return this.defineClass(className, data, 0, data.length);
    }
}
