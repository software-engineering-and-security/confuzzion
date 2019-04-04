package confuzziontest;

import com.github.aztorius.confuzzion.GenerationResult;
import com.github.aztorius.confuzzion.Mutant;
import com.github.aztorius.confuzzion.RandomGenerator;

import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;

import org.junit.runner.RunWith;

import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Assume;

@RunWith(JQF.class)
public class ConfuzzionLauncher {
    @Fuzz
    public void fuzz(@From(ConfuzzionGenerator.class) GenerationResult res) {
        try {
            Mutant mut = res.getMutant();
            Class<?> clazz = mut.toClass(mut.getSootClass());
            Assume.assumeNotNull(clazz);
            clazz.newInstance();
        } catch (IllegalAccessException|InstantiationException e) {
            Assume.assumeNoException(e);
        } // catch (Throwable e) {
        //     e.printStackTrace();
        // }
    }
}
