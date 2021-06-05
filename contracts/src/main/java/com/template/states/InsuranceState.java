package com.template.states;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// *******************
// * Insurance State *
// *******************
public class InsuranceState implements ContractState {

    //private variables
    private Party applicantNode;
    private Party insurerNode;
    private String fname;
    private String lname;
    private String address;
    private String itemName;
    private String itemDescription;
    private Integer faceValue;
    private String referenceID;
    private UUID linearId;

    /* Constructor of your Insurance state */
    public InsuranceState(Party applicantNode, Party insurerNode, String fname, String lname, String address,
                          String itemName, String itemDescription, Integer faceValue, String referenceID,
                          UUID linearId) {
        this.applicantNode = applicantNode;
        this.insurerNode = insurerNode;
        this.fname = fname;
        this.lname = lname;
        this.address = address;
        this.itemName = itemName;
        this.itemDescription = itemDescription;
        this.faceValue = faceValue;
        this.referenceID = referenceID;
        this.linearId = linearId;
    }

    //getters
    public Party getApplicantNode() {
        return applicantNode;
    }

    public Party getInsurerNode() {
        return insurerNode;
    }

    public String getFname() {
        return fname;
    }

    public String getLname() {
        return lname;
    }

    public String getAddress() {
        return address;
    }

    public String getItemName() { return itemName; }

    public String getItemDescription() { return itemDescription; }

    public Integer getFaceValue() { return faceValue; }

    public String getReferenceID() {
        return referenceID;
    }

    public UUID getLinearId() {
        return linearId;
    }

    /* This method will indicate who are the participants and required signers when
     * this state is used in a transaction. */
    //@Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(applicantNode, insurerNode);
    }
}
