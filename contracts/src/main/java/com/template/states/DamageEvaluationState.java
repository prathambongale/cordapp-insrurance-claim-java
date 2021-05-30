package com.template.states;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DamageEvaluationState {

    //private variables
    private Party applicantNode;
    private Party insurerNode;
    private Party repairNode;
    private Party doctorNode;
    private String fname;
    private String lname;
    private String address;
    private String insuranceID;
    private String type;
    private String subType;
    private Integer damageAmount;
    private String reason;
    private String insuranceStatus;
    private String referenceID;
    private UUID linearId;

    /* Constructor of your Claim state */
    public DamageEvaluationState(Party applicantNode, Party insurerNode, Party repairNode, Party doctorNode,
                                 String fname, String lname, String address, String insuranceID, String type,
                                 String subType, Integer damageAmount, String reason, String insuranceStatus,
                                 String referenceID, UUID linearId) {
        this.applicantNode = applicantNode;
        this.insurerNode = insurerNode;
        this.repairNode = repairNode;
        this.doctorNode = doctorNode;
        this.fname = fname;
        this.lname = lname;
        this.address = address;
        this.insuranceID = insuranceID;
        this.type = type;
        this.subType = subType;
        this.damageAmount = damageAmount;
        this.reason = reason;
        this.insuranceStatus = insuranceStatus;
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

    public Party getRepairNode() {
        return repairNode;
    }

    public Party getDoctorNode() {
        return doctorNode;
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

    public String getInsuranceID() {
        return insuranceID;
    }

    public String getType() {
        return type;
    }

    public String getSubType() {
        return subType;
    }

    public Integer getDamageAmount() {
        return damageAmount;
    }

    public String getReason() {
        return reason;
    }

    public String getInsuranceStatus() {
        return insuranceStatus;
    }

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
        return Arrays.asList(applicantNode, insurerNode, repairNode, doctorNode);
    }
}
