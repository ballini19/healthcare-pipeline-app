package com.hospital.pipeline.lab;

/** One raw line of an analyzer file, before validation. Bean-style so the reader can bind it. */
public class LabRecord {

    private String testType;
    private String patientId;
    private String testName;
    private double value;
    private String unit;
    private double refLow;
    private double refHigh;

    public String getTestType() { return testType; }
    public void setTestType(String testType) { this.testType = testType; }
    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }
    public String getTestName() { return testName; }
    public void setTestName(String testName) { this.testName = testName; }
    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getRefLow() { return refLow; }
    public void setRefLow(double refLow) { this.refLow = refLow; }
    public double getRefHigh() { return refHigh; }
    public void setRefHigh(double refHigh) { this.refHigh = refHigh; }

    @Override
    public String toString() {
        return testType + "," + patientId + "," + testName + "," + value + "," + unit + "," + refLow + "," + refHigh;
    }
}
