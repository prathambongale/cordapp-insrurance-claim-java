package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableSet;
import com.template.contracts.TemplateContract;
import com.template.states.ClaimState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.util.UUID;

import static net.corda.core.contracts.ContractsDSL.requireThat;

// *******************
// * ApplyClaim flow *
// *******************
public class ApplyClaimFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class ClaimInitiator extends FlowLogic<SignedTransaction> {

        private Party applicantNode;
        private Party insurerNode;
        private Party policeNode;
        private Party repairNode;
        private Party doctorNode;
        private Party underwriterNode;
        private String fname;
        private String lname;
        private String address;
        private String insuranceID;
        private String type;
        private String subType;
        private Integer value;
        private String reason;
        private String firNo;
        private Integer approvedAmount;
        private String insuranceStatus;

        public ClaimInitiator(Party applicantNode, Party insurerNode, Party policeNode, Party repairNode,
                              Party doctorNode, Party underwriterNode, String fname, String lname, String address,
                              String insuranceID, String type, String subType, Integer value, String reason,
                              String firNo, Integer approvedAmount, String insuranceStatus) {
            this.applicantNode = applicantNode;
            this.insurerNode = insurerNode;
            this.policeNode = policeNode;
            this.repairNode = repairNode;
            this.doctorNode = doctorNode;
            this.underwriterNode = underwriterNode;
            this.fname = fname;
            this.lname = lname;
            this.address = address;
            this.insuranceID = insuranceID;
            this.type = type;
            this.subType = subType;
            this.value = value;
            this.reason = reason;
            this.firNo = firNo;
            this.approvedAmount = approvedAmount;
            this.insuranceStatus = insuranceStatus;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        private final ProgressTracker.Step CLAIM_APPLY = new ProgressTracker
                .Step("Applicant sends Insurance application to the Company.");
        private final ProgressTracker.Step REGISTER_FIR = new ProgressTracker.Step("Register FIR.");
        private final ProgressTracker.Step DAMAGE_EVALUATION = new ProgressTracker
                .Step("Sending Loan application to repair to check damage");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker
                .Step("Verifying contract constraints.");
        private final ProgressTracker.Step INSURANCE_UNDERWRITER = new ProgressTracker
                .Step("Insurance Company forwards application to their Underwriter");
        private final ProgressTracker.Step INSURANCE_UNDERWRITER_EVALUATION = new ProgressTracker
                .Step("Underwriter evaluates the Application and responds to Insurance Company");
        private final ProgressTracker.Step COMPANY_RESPONSE = new ProgressTracker
                .Step("nsurance Company responds to Applicant");
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
                CLAIM_APPLY,
                REGISTER_FIR,
                DAMAGE_EVALUATION,
                VERIFYING_TRANSACTION,
                INSURANCE_UNDERWRITER,
                INSURANCE_UNDERWRITER_EVALUATION,
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
            progressTracker.setCurrentStep(CLAIM_APPLY);

            //Generate an unsigned transaction
            Party applicantParty = getServiceHub().getMyInfo().getLegalIdentities().get(0);
            Party insurerParty = getServiceHub().getMyInfo().getLegalIdentities().get(1);
            Party policeParty = getServiceHub().getMyInfo().getLegalIdentities().get(2);
            Party repairParty = getServiceHub().getMyInfo().getLegalIdentities().get(3);
            Party doctorParty = getServiceHub().getMyInfo().getLegalIdentities().get(4);
            Party underwriterParty = getServiceHub().getMyInfo().getLegalIdentities().get(5);

            ClaimState claimState = new ClaimState(applicantParty, insurerParty, policeParty, repairParty, doctorParty,
                    underwriterParty, fname, lname, address, insuranceID, type, subType, value, reason, firNo,
                    approvedAmount, insuranceStatus, "", UUID.randomUUID());

            final Command initiateLoanCommand = new Command<>
                    (new TemplateContract.Commands.Send(), getOurIdentity().getOwningKey());

            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState((ContractState) claimState, TemplateContract.ID)
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

    @InitiatedBy(ClaimInitiator.class)
    public static class ClaimAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public ClaimAcceptor(FlowSession otherPartyFlow) {
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
