package confuzzion;

import java.lang.ClassLoader;

public class ByteClassLoader extends ClassLoader {
    public ByteClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Class<?> load(String className, byte[] data) throws ClassNotFoundException {
        this.defineClass(className, data, 0, data.length, null);
        return this.loadClass(className);
    }
}
