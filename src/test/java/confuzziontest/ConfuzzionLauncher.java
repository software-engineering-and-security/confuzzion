package confuzziontest;

import com.github.aztorius.confuzzion.Mutant;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;

import org.junit.runner.RunWith;

import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.Assume.*;

@RunWith(JQF.class)
public class ConfuzzionLauncher {
    @Fuzz
    public void fuzz(@From(ConfuzzionGenerator.class) Mutant mut) {
        Boolean bb = true;
        Class<?> clazz = mut.toClass(mut.getSootClass());
        try {
            //Method method = clazz.getMethod("Test");
            //method.invoke(clazz.newInstance());
            clazz.newInstance();
        } catch(IllegalAccessException|InstantiationException e) {
            bb = false;
        }
        assumeTrue(bb);
    }
}
