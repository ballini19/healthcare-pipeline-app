package com.hospital.pipeline.domain;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("URINE")
public class UrineTestResult extends LabResult {

    public String collectionMethod = "MIDSTREAM";

    @Override
    public String category() {
        return "URINE";
    }
}
