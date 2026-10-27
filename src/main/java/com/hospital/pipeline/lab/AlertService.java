package com.hospital.pipeline.lab;

import com.hospital.pipeline.config.JmsConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

/** Publishes critical results to the topic and keeps what a physician-facing subscriber receives. */
@Service
public class AlertService {

    private static final int KEEP = 100;

    private final JmsTemplate topic;
    private final LinkedList<AlertMessage> received = new LinkedList<>();
    private final AtomicLong total = new AtomicLong();

    public AlertService(JmsTemplate topicJmsTemplate) {
        this.topic = topicJmsTemplate;
    }

    public void publish(AlertMessage alert) {
        topic.convertAndSend(JmsConfig.ALERT_TOPIC, alert);
    }

    @JmsListener(destination = JmsConfig.ALERT_TOPIC, containerFactory = "topicListenerFactory")
    public void onAlert(AlertMessage alert) {
        synchronized (received) {
            received.addFirst(alert);
            if (received.size() > KEEP) received.removeLast();
        }
        total.incrementAndGet();
    }

    public List<AlertMessage> latest() {
        synchronized (received) {
            return Collections.unmodifiableList(new ArrayList<>(received));
        }
    }

    public long total() {
        return total.get();
    }

    public void clear() {
        synchronized (received) {
            received.clear();
        }
        total.set(0);
    }
}
