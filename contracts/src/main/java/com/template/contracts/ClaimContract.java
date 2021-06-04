package com.template.contracts;

import com.template.states.ClaimState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// ******************
// * Claim Contract *
// ******************

/**
 * This contract enforces rules regarding the creation of a valid [ClaimState].
 *
 * For a new [Claim] to be created onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Claim].
 * - An Create() command with the public keys of both the party.
 *
 *  For verified [Claim] to be created onto the ledger, a transaction is required which takes:
 * - One input states: the old [Claim].
 * - One output state: the new [Claim].
 *
 * All contracts must sub-class the [Contract] interface.
 */
public class ClaimContract implements Contract {

    // This is used to identify our contract when building a transaction.
    public static final String CLAIM_CONTRACT_ID = "com.template.contracts.ClaimContract";
    public ArrayList<String> claimSubType = new ArrayList<>();
    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    @Override
    public void verify(LedgerTransaction tx) {

        if (tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        /* We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.*/
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        //final CommandData commandData = tx.getCommands().get(0).getValue();

        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof Commands.ClaimApplication) {
            verifyInitialClaim(tx, requiredSigners);
        }
    }

    private void verifyInitialClaim(LedgerTransaction tx, List<PublicKey> signers) {
        //Retrieve the output state of the transaction
        ClaimState output = tx.outputsOfType(ClaimState.class).get(0);

        //Using Corda DSL function requireThat to replicate conditions-checks
        requireThat(require -> {
            require.using("No inputs should be consumed when creating Claim Application.",
                    tx.getInputStates().size() == 0);
            require.using("Only one output state should be created.",
                    tx.getOutputStates().size() == 1);

            ContractState outputState = tx.getOutput(0);

            require.using("Output must be a ClaimState", outputState instanceof ClaimState);

            ClaimState claimReqState = (ClaimState) outputState;
            Party policeAgency = output.getPoliceNode();
            PublicKey policeAgencyKey = policeAgency.getOwningKey();

            require.using("The Claim value must be non-negative and should not be zero.",
                    output.getClaimAmount() > 0);

            if (claimSubType.contains(output.getSubType())) {
                require.using("The Claim value must be non-negative and should not be zero.",
                        signers.contains(policeAgencyKey));
            }

            return null;
        });
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {

        class ClaimApplication implements ClaimContract.Commands {}
    }
}
