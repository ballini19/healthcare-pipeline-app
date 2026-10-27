package com.hospital.pipeline.insurance;

import org.springframework.core.env.Environment;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

/** SOAP client for the insurance gateway. Called by the booking worker, never by an HTTP thread. */
@Component
public class InsuranceClient {

    private final Environment env;
    private final WebServiceTemplate template;

    public InsuranceClient(Environment env) {
        this.env = env;
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(CheckEligibilityRequest.class, CheckEligibilityResponse.class);
        try {
            marshaller.afterPropertiesSet();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot build the SOAP marshaller", e);
        }
        this.template = new WebServiceTemplate();
        this.template.setMarshaller(marshaller);
        this.template.setUnmarshaller(marshaller);
    }

    public boolean isEligible(String insuranceId, String patientName) {
        CheckEligibilityResponse response = (CheckEligibilityResponse) template.marshalSendAndReceive(
                endpoint(), new CheckEligibilityRequest(insuranceId, patientName));
        return response != null && response.eligible;
    }

    private String endpoint() {
        String configured = env.getProperty("insurance.url");
        if (configured != null) return configured;
        return "http://localhost:" + env.getProperty("local.server.port",
                env.getProperty("server.port", "8080")) + "/ws";
    }
}
