package confuzzion;

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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private List<String> strClasses;
    private ArrayList<String> strMutants;
    private ArrayList<MethodComplexity> callableMethods;
    private long remainingCalls;

    private static final Logger logger = LoggerFactory.getLogger(RandomGenerator.class);
    private static final long callsBeforeRefreshProbabilites = 2000;

    /**
     * Constructor
     */
    public RandomGenerator(List<String> targetClasses) {
        this(targetClasses, new Random());
    }

    /**
     * Constructor with specified java.util.Random source
     * @param rand the Random source to use
     */
    public RandomGenerator(List<String> targetClasses, Random rand) {
        this.rand = rand;
        this.counter = 0;
        strClasses = new ArrayList<String>();
        strMutants = new ArrayList<String>();
        callableMethods = new ArrayList<MethodComplexity>();
        remainingCalls = 0;

        for (String strClass : targetClasses) {
            this.addStrClass(strClass);
        }
    }

    public void addStrClass(String className) {
        strClasses.add(className);
        SootClass sClass = Util.getOrLoadSootClass(className);
        List<SootMethod> methods = sClass.getMethods();

        for (SootMethod method : methods) {
            if (method.isPublic() && !method.isConstructor()) {
                callableMethods.add(new MethodComplexity(method));
                logger.info("Add callable method {}", method.getSignature());
            }
        }
    }

    public void addMethodCallStatus(SootMethod method, boolean success) {
        for (MethodComplexity mc : callableMethods) {
            if (mc.getMethod().equals(method)) {
                if (success) {
                    mc.newSuccess();
                } else {
                    mc.newFailure();
                }
                break;
            }
        }
    }

    public SootMethod getRandomExternalMethod() {
        if (ConfuzzionOptions.v().use_uniform_distribution_for_methods) {
            return callableMethods.get(this.nextUint(callableMethods.size())).getMethod();
        } else {
            if (remainingCalls-- <= 0) {
                remainingCalls = callsBeforeRefreshProbabilites;
                double all = 0.0;
                for (MethodComplexity mc : callableMethods) {
                    double value = mc.getScore();
                    all += value;
                    mc.setProbability(value);
                }

                double target = this.nextDouble();
                SootMethod targetMethod = null;
                for (MethodComplexity mc : callableMethods) {
                    Double d = mc.getProbability();
                    d /= all;
                    mc.setProbability(d);
                    if (targetMethod == null) {
                        if (target <= d) {
                            targetMethod = mc.getMethod();
                        } else {
                            target -= d;
                        }
                    }
                }

                if (targetMethod != null) {
                    return targetMethod;
                }
            } else { // Use old probabilities
                double target = this.nextDouble();
                for (MethodComplexity mc : callableMethods) {
                    Double d = mc.getProbability();
                    if (target <= d) {
                        return mc.getMethod();
                    } else {
                        target -= d;
                    }
                }
            }

            throw new RuntimeException("No method has been found in the boundary");
        }
    }

    public SootMethod getRandomMethod(String className) {
        // p = 1/100 => call a local method
        if (this.nextUint(100) == 0) {
            // Choose a method from this class or a further generated class
            int index = strMutants.indexOf(className);
            int random = this.nextUint(strMutants.size() - index);
            String classString = strMutants.get(random + index);
            SootClass sClass = Util.getOrLoadSootClass(classString);
            List<SootMethod> methods = new ArrayList<SootMethod>();
            for (SootMethod method : sClass.getMethods()) {
                if (!method.isConstructor() && !method.getName().startsWith("<")) {
                    methods.add(method);
                }
            }
            if (methods.size() <= 0) {
                // Fall back to external method
                return this.getRandomExternalMethod();
            }
            return methods.get(this.nextUint(methods.size()));
        } else {
            // Choose a method from a target class
            return this.getRandomExternalMethod();
        }
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

    /**
     * Get a pseudo-random number between 0 (included) and maxLimit (excluded)
     * @param maxLimit
     * @return
     */
    public int nextUint(int maxLimit) {
        return rand.nextInt(maxLimit);
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
     * @param  canBePrimitive if true result may be PrimType
     * @return           random type
     */
    public Type randType(String className, boolean canBeVoid, boolean canBePrimitive) {
        if (canBeVoid && rand.nextBoolean()) {
            return VoidType.v();
        } else if (canBePrimitive && rand.nextBoolean()) {
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
            types.add(this.randType(className, false, true));
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
     * Randomly choose a local of the specified type
     * @param locals
     * @param type
     * @return
     */
    public Local randLocal(Chain<Local> locals, Type type) {
        ArrayList<Local> localRefs = new ArrayList<Local>(10);
        for (Local loc : locals) {
            if (loc.getType() == type) {
                localRefs.add(loc);
            }
        }
        if (localRefs.size() <= 0) {
            return null;
        }
        return localRefs.get(this.nextUint(localRefs.size()));
    }

    /**
     * Randomly choose a local of type RefType that is a Mutant class or a target class
     * @param  locals chain of locals
     * @param  canBeAnyRefType if true then can return any RefType Local provided, not only a target class or Mutant class type
     * @return Local of type RefType
     */
    public Local randLocalRef(Chain<Local> locals, boolean canBeAnyRefType) {
        ArrayList<Local> localRefs = new ArrayList<Local>(10);
        for (Local loc : locals) {
            Type type = loc.getType();
            if (type instanceof RefType) {
                String className = ((RefType)type).getClassName();
                if (canBeAnyRefType || strClasses.contains(className) || strMutants.contains(className)) {
                    localRefs.add(loc);
                }
            }
        }
        if (localRefs.size() <= 0) {
            return null;
        }
        return localRefs.get(this.nextUint(localRefs.size()));
    }
}
