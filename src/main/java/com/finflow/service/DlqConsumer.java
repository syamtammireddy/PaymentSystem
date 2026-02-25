package com.finflow.service;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class DlqConsumer {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DlqConsumer.class);

    @KafkaListener(topics = "transaction-requests.DLT", groupId = "finflow-dlq-group")
    public void consumeDeadLetter(ConsumerRecord<String, Object> record) {
        log.error("[DLQ] Failed transaction received in Dead Letter Queue. Key: {}, Value: {}, Partition: {}, Offset: {}",
                record.key(), record.value(), record.partition(), record.offset());
        
    }
}
