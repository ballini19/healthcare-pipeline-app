package com.hospital.pipeline.config;

import com.hospital.pipeline.insurance.InsuranceNamespace;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/** Publishes the mock insurance SOAP service under /ws, with its WSDL at /ws/insurance.wsdl. */
@EnableWs
@Configuration
public class WsConfig {

    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "insurance")
    DefaultWsdl11Definition insuranceWsdl(XsdSchema insuranceSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("InsurancePort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace(InsuranceNamespace.URI);
        wsdl.setSchema(insuranceSchema);
        return wsdl;
    }

    @Bean
    XsdSchema insuranceSchema() {
        return new SimpleXsdSchema(new ClassPathResource("insurance.xsd"));
    }
}
