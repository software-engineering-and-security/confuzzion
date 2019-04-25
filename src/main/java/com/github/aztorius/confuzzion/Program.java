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
        Mutant firstMutant = new Mutant(classBaseName + "0");
        firstMutant.genEmptyClass(rand);
        this.mutants.add(firstMutant);
    }

    public ArrayList<BodyMutation> addContractsChecks(
        ArrayList<Contract> contracts,
        Mutation mutation) {
        Body body = null;
        if (mutation instanceof MethodMutation) {
            // Get the body and put checks at the end
            MethodMutation mMutation = (MethodMutation)mutation;
            body = mMutation.getBody();
        } else {
            // TODO: if ClassMutation, put checks inside constructors
            // TODO: if ProgramMutation, put checks somewhere ?
            throw new IllegalArgumentException("mutation is unknown");
        }
        ArrayList<BodyMutation> mutations = new ArrayList<BodyMutation>();
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
        int idMutant = rand.nextUint(mutants.size());
        SootClass sClass = mutants.get(idMutant).getSootClass();
        int idMethod = rand.nextUint(sClass.getMethods().size());
        return sClass.getMethods().get(idMethod);
    }

    private MethodMutation randomMethodMutation(SootMethod method) {
        MethodMutation mutation = null;
        switch (rand.nextUint(2)) {
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

    public Mutation randomMutation() {
        Mutation mutation = null;
        switch (rand.randLimits(0.99, 0.995, 1.0)) {
        case 0: // P = 0,99 : Method/Body level mutation
            SootMethod method = this.randomSootMethod();
            mutation = this.randomMethodMutation(method);
            break;
        case 1: // P = 0,005 : Class level mutation
            //TODO
            break;
        case 2: // P = 0,005 : Program level mutation
        default:
            //TODO
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
            byte[] array = mut.toClass(mut.getSootClass());
            ByteClassLoader loader = new ByteClassLoader(
                Thread.currentThread().getContextClassLoader());
            Class<?> clazz = loader.load(classBaseName + i, array);
            Method[] methods = clazz.getMethods();
            clazz.newInstance();
            //TODO: invoke methods ?: method.invoke(clazz.newInstance());
        }
    }
}
