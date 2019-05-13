package com.github.aztorius.confuzzion;

import soot.Body;
import soot.SootClass;
import soot.SootMethod;

import java.lang.reflect.Method;
import java.util.ArrayList;

/* A Program is a collection of classes with at least one "main" class.
 * Each class is represented as a Mutant that can evolve.
 * The evolution is represented as a Mutation and apply only to one of the
 * Mutant.
 */
public class Program {
    private String classBaseName;
    private ArrayList<Mutant> mutants;
    private RandomGenerator rand;

    public Program(String classBaseName, RandomGenerator rand) {
        // Initialize class fields
        this.classBaseName = classBaseName;
        this.rand = rand;
        this.mutants = new ArrayList<Mutant>();

        // Create first empty Mutant (main class)
        MutantGenerator generator = new MutantGenerator(rand, classBaseName + "0");
        Mutant firstMutant = generator.genEmptyClass();
        this.mutants.add(firstMutant);
    }

    public Mutant genNewClass() {
        MutantGenerator generator = new MutantGenerator(rand, classBaseName + mutants.size());
        Mutant addedMutant = generator.genEmptyClass();
        this.mutants.add(addedMutant);
        return addedMutant;
    }

    public void removeClass(Mutant mutant) {
        mutants.remove(mutant);
    }

    public ArrayList<BodyMutation> addContractsChecks(
        ArrayList<Contract> contracts,
        Mutation mutation) {
        Body body = null;
        ArrayList<BodyMutation> mutations = new ArrayList<BodyMutation>();

        if (mutation instanceof MethodMutation) {
            // Get the body and put checks at the end
            MethodMutation mMutation = (MethodMutation)mutation;
            body = mMutation.getBody();
        } else if (mutation instanceof ClassMutation) {
            // TODO: put checks inside constructors
            return mutations; //TODO: remove
        } else if (mutation instanceof ProgramMutation) {
            // TODO: if ProgramMutation, put checks somewhere ?
            return mutations; //TODO: remove
        } else {
            throw new IllegalArgumentException("mutation is unknown");
        }

        for (Contract contract : contracts) {
            // Apply contract check on the body
            mutations.add(contract.applyCheck(body));
        }

        return mutations;
    }

    public void removeContractsChecks(ArrayList<BodyMutation> mutations) {
        // Remove contracts checks
        for (BodyMutation mutation : mutations) {
            mutation.undo();
        }
    }

    private SootMethod randomSootMethod() {
        SootClass sClass = this.randomSootClass();
        int idMethod = rand.nextUint(sClass.getMethods().size());
        return sClass.getMethods().get(idMethod);
    }

    private SootClass randomSootClass() {
        int idMutant = rand.nextUint(mutants.size());
        return mutants.get(idMutant).getSootClass();
    }

    private MethodMutation randomMethodMutation(SootMethod method) throws MutationException {
        MethodMutation mutation = null;
        switch (rand.randLimits(0.1, 1.0)) {
        case 0:
            mutation = new AddLocalMutation(rand, method);
            break;
        case 1:
        default:
            mutation = new CallMethodMutation(rand, method);
            break;
        }
        return mutation;
    }

    private ClassMutation randomClassMutation(SootClass sootClass) throws MutationException {
        ClassMutation mutation = null;
        switch (rand.randLimits(0.5, 1.0)) {
        case 0:
            mutation = new AddFieldMutation(rand, sootClass);
            break;
        case 1:
        default:
            mutation = new AddMethodMutation(rand, sootClass);
            break;
        }
        return mutation;
    }

    private ProgramMutation randomProgramMutation() throws MutationException {
        ProgramMutation mutation = null;
        switch (rand.nextUint(1)) {
        case 0:
        default:
            mutation = new AddClassMutation(rand, this);
            break;
        }
        return mutation;
    }

    public Mutation randomMutation() throws MutationException {
        Mutation mutation = null;
        switch (rand.randLimits(0.01, 0.05, 1.0)) {
        case 0: // P = 0,005 : Program level mutation
            mutation = this.randomProgramMutation();
            break;
        case 1: // P = 0,005 : Class level mutation
            SootClass sootClass = this.randomSootClass();
            mutation = this.randomClassMutation(sootClass);
            break;
        case 2: // P = 0,99 : Method/Body level mutation
        default:
            SootMethod method = this.randomSootMethod();
            mutation = this.randomMethodMutation(method);
            break;
        }
        return mutation;
    }

    /* Instatiate the main class and call all methods on it.
     */
    public void genAndLaunch(boolean verbose) throws Exception {
        for (int i = mutants.size() - 1; i >= 0; i--) {
            Mutant mut = mutants.get(i);
            if (verbose) {
                System.out.println("===Class " + classBaseName + i + "===");
                mut.toStdOut();
            }
            byte[] array = mut.toClass();
            ByteClassLoader loader = new ByteClassLoader(
                Thread.currentThread().getContextClassLoader());
            Class<?> clazz = loader.load(classBaseName + i, array);
            Method[] methods = clazz.getMethods();
            clazz.newInstance();
            //TODO: invoke methods ?: method.invoke(clazz.newInstance());
        }
    }

    public void saveToFolder(String folder) {
        for (Mutant mut : mutants) {
            mut.toClassFile(folder);
            mut.toJimpleFile(folder);
        }
    }

    @Override
    public String toString() {
        String str = new String();
        for (Mutant mut : mutants) {
            str += mut.toString();
        }
        return str;
    }
}
