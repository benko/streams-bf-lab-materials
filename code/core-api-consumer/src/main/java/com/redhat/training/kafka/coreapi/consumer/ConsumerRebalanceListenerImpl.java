package com.redhat.training.kafka.coreapi.consumer;

import java.util.Collection;

import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsumerRebalanceListenerImpl implements ConsumerRebalanceListener {
    private final Logger LOG = LoggerFactory.getLogger(ConsumerRebalanceListenerImpl.class.getName());

    @Override
    public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        LOG.info("NEW PARTITIONS ASSIGNED: " + partitions.toString());
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        LOG.info("OLD PARTITIONS REVOKED: " + partitions.toString());
    }
}
