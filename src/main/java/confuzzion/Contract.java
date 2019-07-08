package confuzzion;

import soot.Body;

/**
 * Represents a property
 */
public interface Contract {
    /**
     * Add Jimple code inside Body to check the contract property
     * @param  body The body where we need to check property
     * @return      A BodyMutation corresponding to changes done for the
     *              contract checks
     */
    public BodyMutation applyCheck(Body body);
}
