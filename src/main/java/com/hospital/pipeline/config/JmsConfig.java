package com.hospital.pipeline.config;

import com.hospital.pipeline.booking.BookingMessage;
import com.hospital.pipeline.lab.AlertMessage;
import jakarta.jms.ConnectionFactory;
import java.util.Map;
import org.springframework.boot.autoconfigure.jms.DefaultJmsListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.jms.config.DefaultJmsListenerContainerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessageType;

@Configuration
@EnableJms
public class JmsConfig {

    public static final String BOOKING_QUEUE = "appointmentQueue";
    public static final String ALERT_TOPIC = "labAlerts";

    @Bean
    MessageConverter jmsMessageConverter() {
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setTargetType(MessageType.TEXT);
        converter.setTypeIdPropertyName("_type");
        converter.setTypeIdMappings(Map.of("booking", BookingMessage.class, "alert", AlertMessage.class));
        return converter;
    }

    @Bean
    JmsTemplate queueJmsTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(converter);
        return template;
    }

    /** Critical lab alerts go out on a topic at the highest JMS priority. */
    @Bean
    JmsTemplate topicJmsTemplate(ConnectionFactory connectionFactory, MessageConverter converter) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setMessageConverter(converter);
        template.setPubSubDomain(true);
        template.setExplicitQosEnabled(true);
        template.setPriority(9);
        return template;
    }

    /** One consumer per topic subscriber, otherwise every consumer would get its own copy. */
    @Bean
    DefaultJmsListenerContainerFactory topicListenerFactory(
            ConnectionFactory connectionFactory, DefaultJmsListenerContainerFactoryConfigurer configurer) {
        DefaultJmsListenerContainerFactory factory = new DefaultJmsListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setPubSubDomain(true);
        factory.setConcurrency("1-1");
        return factory;
    }
}
