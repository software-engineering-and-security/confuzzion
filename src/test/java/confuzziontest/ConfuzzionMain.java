package confuzziontest;

import com.github.aztorius.confuzzion.ByteClassLoader;
import com.github.aztorius.confuzzion.Mutant;
import com.github.aztorius.confuzzion.RandomGenerator;

import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConfuzzionMain {
    public static void main(String args[]) {
        ConfuzzionMain conf = new ConfuzzionMain();
        conf.start();
    }

    public void start() {
        RandomGenerator rand = new RandomGenerator();
        for (int i = 0; i < 10; i++) {
            Mutant mut = new Mutant();
            mut.generate(rand);
            mut.toStdOut();
            byte[] array = mut.toClass(mut.getSootClass());
            ByteClassLoader loader = new ByteClassLoader(
                Thread.currentThread().getContextClassLoader());
            Class<?> clazz = loader.load("Test", array);
            try {
                Method[] methods = clazz.getMethods();
                //method.invoke(clazz.newInstance());
                clazz.newInstance();
            } catch(IllegalAccessException|InstantiationException e) {
                e.printStackTrace();
            }
        }
    }
}
