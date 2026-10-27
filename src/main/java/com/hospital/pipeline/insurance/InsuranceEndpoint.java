package com.hospital.pipeline.insurance;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

/**
 * Mock of the legacy insurance gateway. Ids starting with INV are rejected and ids starting
 * with SLOW answer after a delay, to show that a slow insurer no longer stalls registration.
 */
@Endpoint
public class InsuranceEndpoint {

    @PayloadRoot(namespace = InsuranceNamespace.URI, localPart = "CheckEligibilityRequest")
    @ResponsePayload
    public CheckEligibilityResponse check(@RequestPayload CheckEligibilityRequest request) throws InterruptedException {
        String id = request.insuranceId == null ? "" : request.insuranceId.trim().toUpperCase();
        if (id.startsWith("SLOW")) {
            Thread.sleep(1500);
        }
        if (id.isEmpty() || id.startsWith("INV")) {
            return new CheckEligibilityResponse(false, "Policy not found or expired");
        }
        return new CheckEligibilityResponse(true, "Coverage active");
    }
}
