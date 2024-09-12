package com.redhat.training.kafka.coreapi.consumer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Consumer {
    static final Logger LOG = LoggerFactory.getLogger(Consumer.class.getName());

    public static Properties configProperties() {
        Config cf = ConfigProvider.getConfig();
        Properties props = new Properties();

        // Standard mandatory configs.
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cf.getValue("bootstrap.server", String.class));
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, cf.getOptionalValue("security.protocol", String.class).orElse("PLAINTEXT"));
        if (props.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG).equals("SSL")) {
            props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, cf.getValue("ssl.truststore.location", String.class));
            props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, cf.getValue("ssl.truststore.password", String.class));
        }

        // Fixed config, not changeable.
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.IntegerDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringDeserializer.class.getName());

        // Optional stuff.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, cf.getOptionalValue("consumer.group-id", String.class).orElse("test-app"));
        if (cf.getOptionalValue("consumer.instance-id", String.class).isPresent()) {
            props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, cf.getValue("consumer.instance-id", String.class));
        }
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, cf.getOptionalValue("consumer.auto-commit", String.class).orElse("true"));
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, cf.getOptionalValue("consumer.ac-interval", String.class).orElse("5000"));
        props.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, cf.getOptionalValue("consumer.fetch-min-bytes", String.class).orElse("1"));
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, cf.getOptionalValue("consumer.max-poll-recs", String.class).orElse("500"));

        switch (cf.getOptionalValue("consumer.assignment-strategy", String.class).orElse("cooperative")) {
            case "range":
                props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                            org.apache.kafka.clients.consumer.RangeAssignor.class.getName());
                break;
            case "rr":
                props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                            org.apache.kafka.clients.consumer.RoundRobinAssignor.class.getName());
                break;
            case "sticky":
                props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                            org.apache.kafka.clients.consumer.StickyAssignor.class.getName());
                break;
            case "cooperative":
                props.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG,
                            org.apache.kafka.clients.consumer.CooperativeStickyAssignor.class.getName());
        }

        props.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, cf.getOptionalValue("consumer.heartbeat-interval", String.class).orElse("3000"));
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, cf.getOptionalValue("consumer.session-timeout", String.class).orElse("45000"));
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, cf.getOptionalValue("consumer.auto-offset-reset", String.class).orElse("latest"));

        return props;
    }

    public static void main(String... args) {
        // the remaining configurations
        String topic = ConfigProvider.getConfig().getOptionalValue("consumer.topic", String.class).orElse("test-topic");
        int pollPeriod = ConfigProvider.getConfig().getOptionalValue("consumer.poll-period", Integer.class).orElse(1000);
        int waitAfterRecord = ConfigProvider.getConfig().getOptionalValue("consumer.wait-after-record", Integer.class).orElse(0);
        int waitAfterRecv = ConfigProvider.getConfig().getOptionalValue("consumer.wait-after-batch", Integer.class).orElse(0);
        int waitPeriod = ConfigProvider.getConfig().getOptionalValue("consumer.wait-period", Integer.class).orElse(5000);
        int localId = ConfigProvider.getConfig().getOptionalValue("consumer.local-id", Integer.class).orElse(-1);
        boolean truncPayload = ConfigProvider.getConfig().getOptionalValue("consumer.payload-trunc", Boolean.class).orElse(false);
        int ackEveryNum = 0;
        if (ConfigProvider.getConfig().getOptionalValue("consumer.ack-every-x-msgs", Integer.class).isPresent()) {
            LOG.warn("ack-every-x-msgs is set, turning autocommit off.");
            System.setProperty("consumer.auto-commit", "false");
            ackEveryNum = ConfigProvider.getConfig().getValue("consumer.ack-every-x-msgs", Integer.class);
        }

        // keep a payload log for each run, truncate it
        LOG.info("Opening payload log...");
        PrintWriter pl;
        try {
            String logfile = "payload.log";
            if (localId > -1) {
                logfile = "payload-" + localId + ".log";
            }
            File payloadLog = new File(logfile);
            if (truncPayload) {
                LOG.info("Truncating payload log per request.");
                payloadLog.delete();
                payloadLog.createNewFile();
            }
            pl = new PrintWriter(payloadLog);
        } catch (IOException ioe) {
            throw new RuntimeException("Could not (re)create payload log: " + ioe.getMessage());
        }

        // create a consumer and subscribe to topic
        KafkaConsumer<Integer, String> kc = new KafkaConsumer<>(configProperties());
        kc.subscribe(Collections.singletonList(topic), new ConsumerRebalanceListenerImpl());

        LOG.info(String.format("Starting to poll for records of up to %d ms...", pollPeriod));
        boolean exitRequest = false;
        int recsSeen = 0;
        while (true) {
            ConsumerRecords<Integer, String> recs = kc.poll(Duration.ofMillis(pollPeriod));
            for (ConsumerRecord<Integer, String> rec : recs) {
                recsSeen++;
                if (rec.value().equals("quit")) {
                    LOG.info("Received \"quit\" message. Exiting.");
                    exitRequest = true;
                    break;
                }
                if (rec.value().equals("crash")) {
                    LOG.info("Received \"crash\" message. Crashing.");
                    throw new RuntimeException("User requested crash.");
                }
                if (rec.value().equals("wait")) {
                    LOG.info(String.format("Received \"wait\" message. Yielding for %d ms.", waitPeriod));
                    try {
                        Thread.sleep(waitPeriod);
                    } catch (InterruptedException ie) {
                        LOG.warn("Interrupted in wait-upon-request: " + ie.getMessage());
                    }
                    break;
                }

                // so it wasn't a control message - make sense of what we received
                LOG.info(String.format("Received: T:%s P:%d K:%d V:%s", rec.topic(), rec.partition(), rec.key(), rec.value()));
                pl.println(String.format("%s,%d,%d,%s", rec.topic(), rec.partition(), rec.key(), rec.value()));
                pl.flush();

                if (ackEveryNum != 0 && (recsSeen % ackEveryNum) == 0) {
                    LOG.info("Seen {} records, committing offsets as ackEveryNum == {}", recsSeen, ackEveryNum);
                    kc.commitSync();
                }
                try {
                    Thread.sleep(waitAfterRecord);
                } catch (InterruptedException ie) {
                    LOG.warn("Interrupted in sleep-after-record: " + ie.getMessage());
                }
                }
            if (exitRequest) {
                break;
            }
            try {
                Thread.sleep(waitAfterRecv);
            } catch (InterruptedException ie) {
                LOG.warn("Interrupted in sleep-after-recv: " + ie.getMessage());
            }
        };
        kc.close();
        pl.close();
    }
}
