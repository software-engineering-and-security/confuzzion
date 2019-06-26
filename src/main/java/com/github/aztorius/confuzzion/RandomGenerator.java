package com.github.aztorius.confuzzion;

import soot.BooleanType;
import soot.ByteType;
import soot.CharType;
import soot.DoubleType;
import soot.FloatType;
import soot.IntType;
import soot.Local;
import soot.LongType;
import soot.Modifier;
import soot.RefType;
import soot.ShortType;
import soot.SootClass;
import soot.SootField;
import soot.SootMethod;
import soot.Type;
import soot.Value;
import soot.VoidType;
import soot.jimple.ClassConstant;
import soot.jimple.StringConstant;
import soot.util.Chain;

import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
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
    private ArrayList<String> strMutants;

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
        //strClasses.add("java.lang.invoke.MethodHandles");
        strClasses.add("java.util.concurrent.atomic.AtomicReferenceFieldUpdater");
        strClasses.add("java.lang.Integer");
        strClasses.add("java.lang.String");
        strMutants = new ArrayList<String>();
    }

    public void addStrClass(String className) {
        strClasses.add(className);
    }

    public void addStrMutant(String className) {
        strMutants.add(className);
    }

    public void removeStrMutant(String className) {
        strMutants.remove(className);
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
            val = soot.jimple.IntConstant.v(this.nextUint(256));
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

    /**
     * Select a random class string
     * @param className
     * @param can_be_itself if true then result can be className
     * @return
     */
    public String randClassName(String className, boolean can_be_itself) {
        // Choose a class from strClasses
        String classString = strClasses.get(this.nextUint(strClasses.size()));
        if (this.nextBoolean()) {
            // Choose a class generated after current one
            int index = strMutants.indexOf(className);
            int random = this.nextUint(strMutants.size() - index);
            if (random != 0 || can_be_itself) {
                classString = strMutants.get(random + index);
            } //else: skip choosing our own class
        }
        return classString;
    }

    public Value randClassConstant(String className) {
        String classString = this.randClassName(className, true);
        SootClass sClass = Util.getOrLoadSootClass(classString);
        return ClassConstant.fromType(sClass.getType());
    }

    /**
     * Randomly generate a StringConstant from field/methods/classes names
     * @param className
     * @return
     */
    public Value randString(String className) {
        // Choose a random class
        SootClass sootClass = Util.getOrLoadSootClass(this.randClassName(className, true));
        String value = null;
        switch(this.nextUint(3)) {
        case 0:
            // Choose a random field name
            SootField field = this.randElement(sootClass.getFields());
            if (field != null) {
                value = field.getName();
                break;
            } //else: fall through
        case 1:
            // Choose a random method name
            List<SootMethod> methods = sootClass.getMethods();
            if (methods.size() > 0) {
                SootMethod method = methods.get(this.nextUint(methods.size()));
                value = method.getName();
                break;
            } //else: fall through
        case 2:
        default:
            // Choose the className
            value = sootClass.getShortName();
        }
        return StringConstant.v(value);
    }

    /**
     * Return a random element in chain
     * @param <E>
     * @param chain
     * @return
     */
    public <E> E randElement(Chain<E> chain) {
        if (chain.size() == 0) {
            return null;
        }
        int index = this.nextUint(chain.size());
        for (E element : chain) {
            if (index == 0) {
                return element;
            } else {
                index--;
            }
        }
        return null;
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

        return types[this.nextUint(types.length)];
    }

    /**
     * Randomly choose a reference type
     * @param  className current class name
     * @return           random RefType
     */
    public Type randRefType(String className) {
        return Util.getOrLoadSootClass(this.randClassName(className, false)).getType();
    }

    /**
     * Randomly choose a type between VoidType, RefType and PrimType
     * @param  className current class name
     * @param  canBeVoid if true result may be VoidType
     * @return           random type
     */
    public Type randType(String className, Boolean canBeVoid) {
        if (rand.nextBoolean() && canBeVoid) {
            return VoidType.v();
        } else if (rand.nextBoolean()) {
            return this.randPrimType();
        } else {
            return this.randRefType(className);
        }
    }

    /**
     * Generate a list of random types
     * @param className current class name
     * @param number number of types to return
     * @return
     */
    public List<Type> randTypes(String className, int number) {
        ArrayList<Type> types = new ArrayList<Type>(number);
        for (int i = 0; i < number; i++) {
            types.add(this.randType(className, false));
        }
        return types;
    }

    /**
     * Randomly choose modifiers
     * @param  canBeStatic if true result may have the static modifier
     * @param  canBeVolatile if true result may have the volatile modifier
     * @return             random modifiers
     */
    public int randModifiers(Boolean canBeStatic, Boolean canBeVolatile) {
        int modifiers = 0;

        if (canBeStatic && rand.nextBoolean()) {
            modifiers |= Modifier.STATIC;
        }

        if (canBeVolatile && rand.nextBoolean()) {
            modifiers |= Modifier.VOLATILE;
        }

        if (rand.nextBoolean()) {
            modifiers |= Modifier.FINAL;
        }

        switch (this.nextUint(4)) {
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

    /**
     * Randomly choose a local from a Chain<Local>
     * @param  locals chain of locals
     * @return        reference to one Local or null
     */
    public Local randLocal(Chain<Local> locals) {
        if (locals.size() <= 0) {
            return null;
        }
        int choice = this.nextUint(locals.size());
        for (Local loc : locals) {
            if (choice <= 0) {
                return loc;
            }
            choice--;
        }
        return null;
    }

    /**
     * Randomly choose a local of type RefType from a Chain<Local>
     * @param  locals chain of locals
     * @return        reference to a Local of type RefType
     */
    public Local randLocalRef(Chain<Local> locals) {
        ArrayList<Local> localRefs = new ArrayList<Local>();
        for (Local loc : locals) {
            if (loc.getType() instanceof RefType) {
                localRefs.add(loc);
            }
        }
        if (localRefs.size() <= 0) {
            return null;
        }
        return localRefs.get(this.nextUint(localRefs.size()));
    }
}
