package com.redhat.training.kafka.coreapi.producer;

import java.util.Properties;
import java.util.Random;
import java.util.logging.Logger;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class Producer {
    static final Logger LOG = Logger.getLogger(Producer.class.getName());

    public static final String[] quotes = {
        "\"I agree with everything you say, but I would attack to the death your right to say it.\" -- Tom Stoppard (1937 - )",
        "\"Any fool can tell the truth, but it requires a man of some sense to know how to lie well.\" -- Samuel Butler (1835 - 1902)",
        "\"There is no nonsense so gross that society will not, at some time, make a doctrine of it and defend it with every weapon of communal stupidity.\" -- Robertson Davies",
        "\"The nation behaves well if it treats the natural resources as assets which it must turn over to the next generation increased, and not impaired, in value.\" -- Theodore Roosevelt (1858 - 1919), Speech before the Colorado Live Stock Association, Denver, Colorado, August 19, 1910",
        "\"I wish you sunshine on your path and storms to season your journey. I wish you peace in the world in which you live... More I cannot wish you except perhaps love to make all the rest worthwhile.\" -- Robert A. Ward",
        "\"Fall seven times, stand up eight.\" -- Japanese Proverb",
        "\"I think the world is run by 'C' students.\" -- Al McGuire",
        "\"What makes the engine go? Desire, desire, desire.\" -- Stanley Kunitz, O Magazine, September 2003",
        "\"Do not pursue what is illusory - property and position: all that is gained at the expense of your nerves decade after decade and can be confiscated in one fell night. Live with a steady superiority over life - don't be afraid of misfortune, and do not yearn after happiness; it is after all, all the same: the bitter doesn't last forever, and the sweet never fills the cup to overflowing.\" -- Alexander Solzhenitsyn (1918 - )",
        "\"Anyone who goes to a psychiatrist ought to have his head examined.\" -- Samuel Goldwyn (1882 - 1974)",
        "\"An expert is a person who has made all the mistakes that can be made in a very narrow field.\" -- Niels Bohr (1885 - 1962)",
        "\"It's going to come true like you knew it, but it's not going to feel like you think.\" -- Rosie O'Donnell, Today Show interview, 04-08-08",
        "\"Your primary goal should be to have a great life. You can still have a good day, enjoy your child, and ultimately find happiness, whether your ex is acting like a jerk or a responsible person. Your happiness is not dependent upon someone else.\" -- Julie A., M.A. Ross and Judy Corcoran, Joint Custody with a Jerk: Raising a Child with an Uncooperative Ex, 2011",
        "\"You have to keep plugging away. We are all growing. There is no shortcut. You have to put time into it to build an audience\" -- John Gruber, How to Blog for Money by Learning from Comics, SXSW 2006",
        "\"We are advertis'd by our loving friends.\" -- William Shakespeare (1564 - 1616)",
        "\"We shall find peace. We shall hear the angels, we shall see the sky sparkling with diamonds.\" -- Anton Chekhov (1860 - 1904), 1897",
        "\"Do not be fooled into believing that because a man is rich he is necessarily smart. There is ample proof to the contrary.\" -- Julius Rosenwald (1862 - 1932)",
        "\"See, that's all you're thinking about, is winning. You're confirming your sense of self- worth through outward reward instead of through inner appreciation.\" -- Barbara Hall, Northern Exposure, Gran Prix, 1994",
        "\"If you think you can do a thing or think you can't do a thing, you're right.\" -- Henry Ford (1863 - 1947), (attributed)",
        "\"I am here and you will know that I am the best and will hear me.\" -- Leontyne Price, O Magazine, December 2003 ",
    };

    public static Properties configProperties() {
        Config cf = ConfigProvider.getConfig();
        Properties props = new Properties();

        // Standard mandatory configs.
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, cf.getOptionalValue("security.protocol", String.class).orElse("SSL"));
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, cf.getValue("ssl.truststore.location", String.class));
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, cf.getValue("ssl.truststore.password", String.class));
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, cf.getValue("kafka.bootstrap.server", String.class));

        // Fixed config, not changeable.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.IntegerSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, org.apache.kafka.common.serialization.StringSerializer.class.getName());

        // Optional stuff.
        props.put(ProducerConfig.ACKS_CONFIG, cf.getOptionalValue("producer.acks", String.class).orElse("all"));
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, cf.getOptionalValue("producer.batch", String.class).orElse("16384"));
        props.put(ProducerConfig.LINGER_MS_CONFIG, cf.getOptionalValue("producer.linger", String.class).orElse("0"));
        props.put(ProducerConfig.RETRIES_CONFIG, cf.getOptionalValue("producer.retries", String.class).orElse("2147483647"));
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, cf.getOptionalValue("producer.max-inflight", String.class).orElse("5"));
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, cf.getOptionalValue("producer.idempotent", String.class).orElse("true"));
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, cf.getOptionalValue("producer.delivery-timeout", String.class).orElse("120000"));

        return props;
    }
    public static void main(String... args) {
        int sendSize = ConfigProvider.getConfig().getOptionalValue("producer.num-records-per-roll", Integer.class).orElse(100);
        int waitAfterBatch = ConfigProvider.getConfig().getOptionalValue("producer.wait-after-roll", Integer.class).orElse(500);
        int howManyrolls = ConfigProvider.getConfig().getOptionalValue("producer.num-rolls", Integer.class).orElse(1);
        String topic = ConfigProvider.getConfig().getOptionalValue("producer.topic", String.class).orElse("test-topic");

        Random rnd = new Random();
        KafkaProducer<Integer, String> prod = new KafkaProducer<>(configProperties());

        for (int x = 0; x < howManyrolls; x++) {
            for (int y = 0; y < sendSize; y++) {
                int idx = rnd.nextInt(quotes.length);
                ProducerRecord<Integer, String> rec = new ProducerRecord<>(topic, y, quotes[idx]);
                prod.send(rec, new Callback() {
                    public void onCompletion(RecordMetadata rm, Exception e) {
                        if (e != null) {
                            LOG.warning(e.getMessage());
                        } else {
                            LOG.info(String.format("Sent: T:%s P:%d K:%d V:%s", rm.topic(), rm.partition(), rec.key(), rec.value()));
                        }
                    }
                });
            }
            if (x < (howManyrolls - 1)) {
                LOG.info(String.format("Trying to sleep for %d ms...", waitAfterBatch));;
                try {
                    Thread.sleep(waitAfterBatch);
                } catch (InterruptedException ie) {
                    LOG.warning("Interrupted: " + ie.getMessage());
                }
            }
        }

        prod.close();
    }
}
