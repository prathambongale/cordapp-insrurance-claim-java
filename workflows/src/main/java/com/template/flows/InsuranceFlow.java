package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import com.template.contracts.TemplateContract;
import com.template.states.ClaimState;
import com.template.states.InsuranceState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.UUID;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// ***********************
// * ApplyInsurance flow *
// ***********************
public class InsuranceFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class InsuranceInitiator extends FlowLogic<SignedTransaction> {

        private Party applicantNode;
        private Party insurerNode;
        private String fname;
        private String lname;
        private String address;
        private String itemName;
        private String itemDescription;
        private Integer faceValue;

        public InsuranceInitiator(Party applicantNode, Party insurerNode, String fname, String lname, String address,
                              String itemName, String itemDescription, Integer faceValue) {
            this.applicantNode = applicantNode;
            this.insurerNode = insurerNode;
            this.fname = fname;
            this.lname = lname;
            this.address = address;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        private final ProgressTracker.Step APPLY_INSURANCE = new ProgressTracker
                .Step("Applicant sends Insurance application to the Company.");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker
                .Step("Verifying contract constraints.");
        private final ProgressTracker.Step EVALUATION_USER = new ProgressTracker
                .Step("Underwriter evaluates the Application and responds to Insurance Company");
        private final ProgressTracker.Step COMPANY_RESPONSE = new ProgressTracker
                .Step("Insurance Company responds to Applicant");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker
                .Step("Signing transaction with our private key.");

        private final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker
                .Step("Gathering the counterparty's signature.") {
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker
                .Step("Obtaining notary signature and recording transaction.") {
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        /**
         * The progress tracker provides checkpoints indicating the progress of
         * the flow to observers.
         */
        private final ProgressTracker progressTracker = new ProgressTracker(
                APPLY_INSURANCE,
                EVALUATION_USER,
                COMPANY_RESPONSE,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        );


        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Obtain a reference to the notary we want to use.
            // We retrieve the notary identity from the network map.
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

            //Stage 1
            progressTracker.setCurrentStep(APPLY_INSURANCE);

            //Generate an unsigned transaction
            Party applicantParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            Party insurerParty = getServiceHub().getMyInfo().getLegalIdentities().get(1);

            InsuranceState insuranceState = new InsuranceState(applicantParty, insurerParty, fname, lname, address,
                    itemName, itemDescription, faceValue, "", UUID.randomUUID());

            final Command initiateLoanCommand = new Command<>
                    (new TemplateContract.Commands.Send(), getOurIdentity().getOwningKey());

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState((ContractState) insuranceState, TemplateContract.ID)
                    .addCommand(initiateLoanCommand);

            //Stage 2
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
            // Verify that the transaction is valid
            txBuilder.verify(getServiceHub());

            //stage 3
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);
            // Sign the transaction.
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            //Stage 4
            progressTracker.setCurrentStep(GATHERING_SIGS);
            // Send the state to the counterparty, and receive it back with their signature.
            FlowSession otherPartySession = initiateFlow(insurerNode);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    ImmutableSet.of(otherPartySession), CollectSignaturesFlow.Companion.tracker()));

            //stage 5
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);
            //Notarise and record the transaction in both party vaults.
            return subFlow(new FinalityFlow(fullySignedTx, otherPartySession));

        }
    }

    @InitiatedBy(ApplyClaimFlow.ClaimInitiator.class)
    public static class InsuranceAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public InsuranceAcceptor(FlowSession otherPartyFlow) {
            this.otherPartyFlow = otherPartyFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                public SignTxFlow(FlowSession otherSideSession, ProgressTracker progressTracker) {
                    super(otherSideSession, progressTracker);
                }

                @Override
                protected void checkTransaction(SignedTransaction stx) throws FlowException {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using("This must be a transaction between insurance company and applicant " +
                                "(ApplyClaim transaction).", output instanceof ClaimState);
                        return null;
                    });
                }
            }
            return subFlow(new SignTxFlow(otherPartyFlow, SignTransactionFlow.Companion.tracker()));
        }
    }
}
