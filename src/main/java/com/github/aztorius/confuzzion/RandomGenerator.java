package com.github.aztorius.confuzzion;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.Modifier;
import soot.Scene;
import soot.ShortType;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.ClassConstant;

import java.lang.Math;
import java.util.ArrayList;
import java.util.Random;

/**
 * A Random Generator with custom methods for Soot objects
 */
public class RandomGenerator {
    private Random rand;
    private long counter;
    private int poolBoolean[] = {0, 1};
    private int poolInt[] = {0, 1, -1, 2};
    private long poolLong[] = {0, 1, -1, 2};
    private int poolChar[] = {'a', 'A', '0'};
    private float poolFloat[] = {0.0f, 1.0f, -1.0f};
    private double poolDouble[] = {0.0, 1.0, -1.0};

    private ArrayList<String> strClasses;

    /**
     * Constructor
     */
    public RandomGenerator() {
        this(new Random());
    }

    /**
     * Constructor with specified java.util.Random source
     * @param rand the Random source to use
     */
    public RandomGenerator(Random rand) {
        this.rand = rand;
        this.counter = 0;
        strClasses = new ArrayList<String>();
        strClasses.add("java.io.ByteArrayOutputStream");
        strClasses.add("java.util.concurrent.ForkJoinPool");
        strClasses.add("java.lang.invoke.MethodHandles");
        strClasses.add("java.util.concurrent.atomic.AtomicReferenceFieldUpdater");
    }

    public void addStrClass(String className) {
        strClasses.add(className);
    }

    public String getClassName() {
        return strClasses.get(this.nextUint(strClasses.size()));
    }

    private int getIntFromPool(int[] pool) {
        return pool[this.nextUint(pool.length)];
    }

    private long getLongFromPool(long[] pool) {
        return pool[this.nextUint(pool.length)];
    }

    private float getFloatFromPool(float[] pool) {
        return pool[this.nextUint(pool.length)];
    }

    private double getDoubleFromPool(double[] pool) {
        return pool[this.nextUint(pool.length)];
    }

    public long nextIncrement() {
        return counter++;
    }

    public boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public int nextInt() {
        return rand.nextInt();
    }

    public int nextUint() {
        int result = Math.abs(rand.nextInt());
        if (result < 0) {
            result = 0;
        }
        return result;
    }

    public int nextUint(int maxLimit) {
        return this.nextUint() % maxLimit;
    }

    public float nextFloat() {
        return rand.nextFloat();
    }

    public double nextDouble() {
        return rand.nextDouble();
    }

    public long nextLong() {
        return rand.nextLong();
    }

    /**
     * Returns a random number depending on each cumulative probability.
     * Ex: randLimits(0.1, 0.5, 1.0) can return 0, 1 or 2 with probabilities
     * 0.1, 0.4, 0.5
     * @param  ...limits list of probabilities that ends with 1.0
     * @return           random number depending on probabilities
     */
    public int randLimits(double ...limits) {
        double rr = rand.nextDouble();

        int i = 0;
        for (double lim : limits) {
            if (rr <= lim) {
                return i;
            }
            i++;
        }

        return i;
    }

    /**
     * Randomly generate a constant for the appropriate type
     * @param  type Type of the constant
     * @return      Value that is a constant of appropriate type
     */
    public Value randConstant(Type type) {
        Value val = null;

        if (type == BooleanType.v()) {
            val = soot.jimple.IntConstant.v(this.getIntFromPool(poolBoolean));
        } else if (type == ByteType.v()) {
            val = soot.jimple.IntConstant.v(this.nextUint() % 256);
        } else if (type == CharType.v()) {
            val = soot.jimple.IntConstant.v(this.getIntFromPool(poolChar));
        } else if (type == DoubleType.v()) {
            val = soot.jimple.DoubleConstant.v(this.getDoubleFromPool(poolDouble));
        } else if (type == FloatType.v()) {
            val = soot.jimple.FloatConstant.v(this.getFloatFromPool(poolFloat));
        } else if (type == IntType.v()) {
            val = soot.jimple.IntConstant.v(this.getIntFromPool(poolInt));
        } else if (type == LongType.v()) {
            val = soot.jimple.LongConstant.v(this.getLongFromPool(poolLong));
        } else if (type == ShortType.v()) {
            val = soot.jimple.IntConstant.v(this.getIntFromPool(poolInt));
        } else {
            // Should not happen !
        }

        return val;
    }

    public Value randClass() {
        String classString = strClasses.get(this.nextUint(strClasses.size()));
        return ClassConstant.v(classString.replace(".", "/"));
    }

    /**
     * Randomly select a primitive type
     * @return A random primitive type
     */
    public Type randPrimType() {
        Type[] types = {
            BooleanType.v(),
            ByteType.v(),
            CharType.v(),
            DoubleType.v(),
            FloatType.v(),
            IntType.v(),
            LongType.v(),
            ShortType.v()
        };

        return types[this.nextUint() % types.length];
    }

    public Type randRefType() {
        String strClass = strClasses.get(this.nextUint(strClasses.size()));
        Scene.v().loadClassAndSupport(strClass);
        return Scene.v().getRefType(strClass);
    }

    /**
     * Randomly choose a type between VoidType, RefType and PrimType
     * @param  canBeVoid if true result may be VoidType
     * @return           random type
     */
    public Type randType(Boolean canBeVoid) {
        if (rand.nextBoolean() && canBeVoid) {
            return VoidType.v();
        } else if (rand.nextBoolean()) {
            return this.randPrimType();
        } else {
            return this.randRefType();
        }
    }

    /**
     * Randomly choose modifiers
     * @param  canBeStatic if true result may have the static modifier
     * @return             random modifiers
     */
    public int randModifiers(Boolean canBeStatic) {
        int modifiers = 0;

        if (rand.nextBoolean() && canBeStatic) {
            modifiers |= Modifier.STATIC;
        }

        if (rand.nextBoolean()) {
            modifiers |= Modifier.FINAL;
        }

        switch (this.nextUint() % 4) {
        case 0:
            /* no modifier : java default */
            break;
        case 1:
            modifiers |= Modifier.PUBLIC;
            break;
        case 2:
            modifiers |= Modifier.PRIVATE;
            break;
        default:
            modifiers |= Modifier.PROTECTED;
        }

        return modifiers;
    }
}
