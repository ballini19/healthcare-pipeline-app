package com.hospital.pipeline.insurance;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "CheckEligibilityResponse", namespace = InsuranceNamespace.URI)
@XmlAccessorType(XmlAccessType.FIELD)
public class CheckEligibilityResponse {

    public boolean eligible;
    public String message;

    public CheckEligibilityResponse() {
    }

    public CheckEligibilityResponse(boolean eligible, String message) {
        this.eligible = eligible;
        this.message = message;
    }
}
