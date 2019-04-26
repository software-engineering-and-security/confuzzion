package confuzziontest;

import com.github.aztorius.confuzzion.BodyMutation;
import com.github.aztorius.confuzzion.ByteClassLoader;
import com.github.aztorius.confuzzion.Contract;
import com.github.aztorius.confuzzion.ContractCheckException;
import com.github.aztorius.confuzzion.ContractTypeConfusion;
import com.github.aztorius.confuzzion.Mutant;
import com.github.aztorius.confuzzion.Mutation;
import com.github.aztorius.confuzzion.Program;
import com.github.aztorius.confuzzion.RandomGenerator;

import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class ConfuzzionMain {
    public static void main(String args[]) {
        ConfuzzionMain conf = new ConfuzzionMain();
        conf.start(10, 10, true);
    }

    public void start(int constants_retry, int mainloop_turn, boolean verbose) {
        RandomGenerator rand = new RandomGenerator();
        ArrayList<Contract> contracts = new ArrayList<Contract>();
        contracts.add(new ContractTypeConfusion());

        Program currentProg = new Program("Test", rand);

        for (int loop1 = 0; loop1 < mainloop_turn; loop1++) {
            // Random mutation (program level | class level | method level)
            Mutation mutation = currentProg.randomMutation();
            for (int loop2 = 0; loop2 < constants_retry; loop2++) {
                // TODO: change constants in mutation units taken from a pool
                try {
                    if (verbose) {
                        System.out.println(
                            "===Program Test: iter1 " + loop1 +
                            " iter2 " + loop2 + "===");
                    }
                    // Instantiation and launch
                    currentProg.genAndLaunch(verbose);
                    // Continue if no exception else try other constants
                    break;
                } catch(Throwable e) {
                    e.printStackTrace();
                }
            }
            // Add contracts checks
            ArrayList<BodyMutation> contractsMutations =
                currentProg.addContractsChecks(contracts, mutation);
            try {
                // Instantiation and launch
                currentProg.genAndLaunch(verbose);
                // Remove contracts checks for next turn
                currentProg.removeContractsChecks(contractsMutations);
            } catch(ContractCheckException e) {
                // TODO: add the program source code to the result list
                // save the resulting class file ?
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                e.printStackTrace();
            } catch(Throwable e) {
                // Remove contracts checks
                currentProg.removeContractsChecks(contractsMutations);
                // Bad sample, revert mutation
                mutation.undo();
                e.printStackTrace();
            }
        }
    }
}
