package com.template.states;

import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ClaimInvestigationState {

    //private variables
    private Party applicantNode;
    private Party insurerNode;
    private Party policeNode;
    private Party repairNode;
    private Party doctorNode;
    private String fname;
    private String lname;
    private String address;
    private String insuranceID;
    private String type;
    private String subType;
    private Integer value;
    private String reason;
    private String firNo;
    private String insuranceStatus;
    private String referenceID;
    private UUID linearId;

    /* Constructor of your Claim state */
    public ClaimInvestigationState(Party applicantNode, Party insurerNode, Party policeNode, Party repairNode,
                                   Party doctorNode, String fname, String lname, String address, String insuranceID,
                                   String type, String subType, Integer value, String reason, String firNo,
                                   String insuranceStatus, String referenceID, UUID linearId) {
        this.applicantNode = applicantNode;
        this.insurerNode = insurerNode;
        this.policeNode = policeNode;
        this.repairNode = repairNode;
        this.doctorNode = doctorNode;
        this.fname = fname;
        this.lname = lname;
        this.address = address;
        this.insuranceID = insuranceID;
        this.type = type;
        this.subType = subType;
        this.value = value;
        this.reason = reason;
        this.firNo = firNo;
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

    public Party getPoliceNode() {
        return policeNode;
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

    public Integer getValue() {
        return value;
    }

    public String getReason() {
        return reason;
    }

    public String getFirNo() {
        return firNo;
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
        return Arrays.asList(applicantNode, insurerNode, policeNode, repairNode, doctorNode);
    }
}
