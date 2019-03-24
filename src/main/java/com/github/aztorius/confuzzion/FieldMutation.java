package com.github.aztorius.confuzzion;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.LongType;
import soot.ShortType;
import soot.SootField;
import soot.Type;

//TODO: add support for reference types of other objects
public class FieldMutation implements Mutation {
    @Override
    public void apply(Mutant mut, RandomGenerator rand, MutationType mtype) {
        switch (mtype) {
            case ADD:
                String name = "field" + mut.nextInt();
                Type type = rand.randPrimType();
                int modifiers = 0; //TODO: random
                mut.getSootClass().addField(new SootField(name, type, modifiers));
                break;
            case CHANGE:
                //TODO: implement
                break;
            case REMOVE:
                //TODO: implement
                break;
            default:
                break;
        }
    }
}
