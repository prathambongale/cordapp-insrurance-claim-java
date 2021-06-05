package com.template.contracts;

import com.template.states.ClaimState;
import com.template.states.InsuranceState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;

import java.security.PublicKey;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

// **********************
// * Insurance Contract *
// **********************
public class InsuranceContract implements Contract {

    @Override
    public void verify(@NotNull LedgerTransaction tx) {
        if (tx != null && tx.getCommands().size() != 1)
            throw new IllegalArgumentException("Transaction must have one command");

        /* We can use the requireSingleCommand function to extract command data from transaction.
         * However, it is possible to have multiple commands in a signle transaction.*/
        final CommandWithParties<InsuranceContract.Commands> command = requireSingleCommand(tx.getCommands(),
                InsuranceContract.Commands.class);
        //final CommandData commandData = tx.getCommands().get(0).getValue();

        List<PublicKey> requiredSigners = command.getSigners();
        CommandData commandType = command.getValue();

        if (commandType instanceof InsuranceContract.Commands.InsuranceApplication) {
            verifyInitialApplication(tx, requiredSigners);
        }
    }

    private void verifyInitialApplication(LedgerTransaction tx, List<PublicKey> signers) {
        //Retrieve the output state of the transaction
        ClaimState output = tx.outputsOfType(ClaimState.class).get(0);

        //Using Corda DSL function requireThat to replicate conditions-checks
        requireThat(require -> {
            require.using("Inputs should be consumed when creating Insurance Application.",
                    tx.getInputStates().size() == 6);
            require.using("Only one output state should be created.",
                    tx.getOutputStates().size() == 1);

            ContractState outputState = tx.getOutput(0);

            require.using("Output must be a ClaimState", outputState instanceof InsuranceState);

            ClaimState claimReqState = (ClaimState) outputState;
            Party policeAgency = output.getPoliceNode();
            PublicKey policeAgencyKey = policeAgency.getOwningKey();

            require.using("The Claim value must be non-negative and should not be zero.",
                    output.getClaimAmount() > 0);

            return null;
        });
    }

    // Used to indicate the transaction's intent.
    public interface Commands extends CommandData {

        class InsuranceApplication implements InsuranceContract.Commands {}
    }
}
