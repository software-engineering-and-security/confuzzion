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

    public RandomGenerator() {
        rand = new Random();
    }

    public RandomGenerator(Random rand) {
        this.rand = rand;
    }

    public boolean nextBoolean() {
        return rand.nextBoolean();
    }

    public int nextInt() {
        return rand.nextInt();
    }

    public int nextUint() {
        return Math.abs(rand.nextInt());
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
            val = soot.jimple.IntConstant.v(this.nextUint() % 2);
        } else if (type == ByteType.v()) {
            val = soot.jimple.IntConstant.v(this.nextUint() % 256);
        } else if (type == CharType.v()) {
            val = soot.jimple.IntConstant.v(this.nextUint() % 256);
        } else if (type == DoubleType.v()) {
            val = soot.jimple.DoubleConstant.v(this.nextDouble());
        } else if (type == FloatType.v()) {
            val = soot.jimple.FloatConstant.v(this.nextFloat());
        } else if (type == IntType.v()) {
            val = soot.jimple.IntConstant.v(this.nextInt());
        } else if (type == LongType.v()) {
            val = soot.jimple.LongConstant.v(this.nextLong());
        } else if (type == ShortType.v()) {
            val = soot.jimple.IntConstant.v(this.nextInt());
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
