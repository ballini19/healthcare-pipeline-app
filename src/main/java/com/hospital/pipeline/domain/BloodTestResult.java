package com.hospital.pipeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("BLOOD")
public class BloodTestResult extends LabResult {

    public String sampleType = "VENOUS";

    @Override
    public String category() {
        return "BLOOD";
    }
}
