package com.github.aztorius.confuzzion;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.ShortType;
import soot.Type;

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

    public int randInt() {
        return rand.nextInt();
    }

    public int randUint() {
        return Math.abs(rand.nextInt());
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
}
