package com.github.aztorius.confuzzion;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.Modifier;
import soot.ShortType;
import soot.Type;
import soot.Value;
import soot.VoidType;

import java.lang.Math;
import java.util.Random;

public class RandomGenerator {
    private Random rand;
    private long counter;
    private int poolBoolean[] = {0, 1};
    private int poolInt[] = {0, 1, -1, 2};
    private long poolLong[] = {0, 1, -1, 2};
    private int poolChar[] = {'a', 'A', '0'};
    private float poolFloat[] = {0.0f, 1.0f, -1.0f};
    private double poolDouble[] = {0.0, 1.0, -1.0};

    public RandomGenerator() {
        rand = new Random();
        counter = 0;
    }

    public RandomGenerator(Random rand) {
        this.rand = rand;
        this.counter = 0;
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
            //TODO: not a primitive type and not in a local
            //System.out.println("DEBUG: RG: Not a primitive type for constant generation");
        }

        return val;
    }

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

    public Type randType(Boolean canBeVoid) {
        if (rand.nextBoolean() && canBeVoid) {
            return VoidType.v();
        } else {
            return this.randPrimType();
        }
    }

    public int randModifiers(Boolean canBeStatic) {
        int modifiers = 0;

        if (rand.nextBoolean() && canBeStatic) {
            modifiers |= Modifier.STATIC;
        }

        if (rand.nextBoolean()) {
            modifiers |= Modifier.FINAL;
        }

        switch (this.nextUint() % 3) {
            case 0:
                modifiers |= Modifier.PUBLIC;
                break;
            case 1:
                modifiers |= Modifier.PRIVATE;
                break;
            default:
                modifiers |= Modifier.PROTECTED;
        }

        return modifiers;
    }
}
