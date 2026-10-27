package com.hospital.pipeline.insurance;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CheckEligibilityRequest", namespace = InsuranceNamespace.URI)
@XmlAccessorType(XmlAccessType.FIELD)
public class CheckEligibilityRequest {

    public String insuranceId;
    public String patientName;

    public CheckEligibilityRequest() {
    }

    public CheckEligibilityRequest(String insuranceId, String patientName) {
        this.insuranceId = insuranceId;
        this.patientName = patientName;
    }
}
