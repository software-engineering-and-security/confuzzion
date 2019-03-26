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

    public int nextInt() {
        return rand.nextInt();
    }

    public int nextUint() {
        return Math.abs(rand.nextInt());
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

        return types[Math.abs(rand.nextInt()) % types.length];
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
