package cscie88.assignments;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import cscie88.assignments.app.contants.AppConstants;
import org.apache.avro.io.BinaryEncoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.flume.Event;
import org.apache.flume.EventDeliveryException;
import org.apache.flume.Transaction;
import org.apache.flume.conf.BatchSizeSupported;
import org.apache.flume.conf.Configurable;
import org.apache.flume.conf.ConfigurationException;
import org.apache.flume.conf.LogPrivacyUtil;
import org.apache.flume.formatter.output.BucketPath;
import org.apache.flume.instrumentation.kafka.KafkaSinkCounter;
import org.apache.flume.shared.kafka.KafkaSSLUtil;
import org.apache.flume.source.avro.AvroFlumeEvent;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.errors.ProducerFencedException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import static org.apache.flume.shared.kafka.KafkaSSLUtil.SSL_DISABLE_FQDN_CHECK;
import static org.apache.flume.shared.kafka.KafkaSSLUtil.isSSLEnabled;
import org.apache.flume.sink.AbstractSink;

public class FlumeKafkaSink extends AbstractSink implements Configurable, BatchSizeSupported {

  private static final Logger logger = LoggerFactory.getLogger(FlumeKafkaSink.class);

  private final Properties kafkaProps = new Properties();
  private KafkaProducer<String, byte[]> producer;
  private String topic;
  private int batchSize;
  private List<Future<RecordMetadata>> kafkaFutures;
  private KafkaSinkCounter counter;
  private boolean useAvroEventFormat;
  private String partitionHeader = null;
  private Integer staticPartitionId = null;
  private boolean allowTopicOverride;
  private String topicHeader = null;
  private String timestampHeader = null;
  private Map<String, String> headerMap;
  private boolean useKafkaTransactions = false;
  private Optional<SpecificDatumWriter<AvroFlumeEvent>> writer = Optional.absent();
  private Optional<ByteArrayOutputStream> tempOutStream = Optional.absent();
  private BinaryEncoder encoder = null;

  public String getTopic() {
    return topic;
  }

  public long getBatchSize() {
    return batchSize;
  }

  @Override
  public Status process() throws EventDeliveryException {
    Status result = Status.READY;
    Channel channel = getChannel();
    Transaction transaction = null;
    Event event = null;
    String eventTopic = null;
    String eventKey = null;

    try {
      long processedEvents = 0;

      transaction = channel.getTransaction();
      transaction.begin();
      if (useKafkaTransactions) {
        producer.beginTransaction();
      }

      kafkaFutures.clear();
      long batchStartTime = System.nanoTime();
      for (; processedEvents < batchSize; processedEvents += 1) {
        event = channel.take();

        if (event == null) {
          if (processedEvents == 0) {
            result = Status.BACKOFF;
            counter.incrementBatchEmptyCount();
          } else {
            counter.incrementBatchUnderflowCount();
          }
          break;
        }
        counter.incrementEventDrainAttemptCount();

        byte[] eventBody = event.getBody();
        Map<String, String> headers = event.getHeaders();

        if (allowTopicOverride) {
          eventTopic = headers.get(topicHeader);
          if (eventTopic == null) {
            eventTopic = BucketPath.escapeString(topic, event.getHeaders());
            logger.debug("{} was set to true but header {} was null. Producing to {}" +
                " topic instead.",
                new Object[]{AppConstants.ALLOW_TOPIC_OVERRIDE_HEADER,
                    topicHeader, eventTopic});
          }
        } else {
          eventTopic = topic;
        }

        eventKey = headers.get(AppConstants.KEY_HEADER);
        if (logger.isTraceEnabled()) {
          if (LogPrivacyUtil.allowLogRawData()) {
            logger.trace("{Event} " + eventTopic + " : " + eventKey + " : "
                + new String(eventBody, StandardCharsets.UTF_8));
          } else {
            logger.trace("{Event} " + eventTopic + " : " + eventKey);
          }
        }
        logger.debug("event #{}", processedEvents);

        // create a message and add to buffer
        long startTime = System.currentTimeMillis();

        Integer partitionId = null;
        try {
          ProducerRecord<String, byte[]> record;
          if (staticPartitionId != null) {
            partitionId = staticPartitionId;
          }
          //Allow a specified header to override a static ID
          if (partitionHeader != null) {
            String headerVal = event.getHeaders().get(partitionHeader);
            if (headerVal != null) {
              partitionId = Integer.parseInt(headerVal);
            }
          }
          Long timestamp = null;
          if (timestampHeader != null) {
            String value = headers.get(timestampHeader);
            if (value != null) {
              try {
                timestamp = Long.parseLong(value);
              } catch (Exception ex) {
                logger.warn("Invalid timestamp in header {} - {}", timestampHeader, value);
              }
            }
          }
          List<Header> kafkaHeaders = null;
          if (!headerMap.isEmpty()) {
            List<Header> tempHeaders = new ArrayList<>();
            for (Map.Entry<String, String> entry : headerMap.entrySet()) {
              String value = headers.get(entry.getKey());
              if (value != null) {
                tempHeaders.add(new RecordHeader(entry.getValue(),
                    value.getBytes(StandardCharsets.UTF_8)));
              }
            }
            if (!tempHeaders.isEmpty()) {
              kafkaHeaders = tempHeaders;
            }
          }

          if (partitionId != null) {
            record = new ProducerRecord<>(eventTopic, partitionId, timestamp, eventKey,
                serializeEvent(event, useAvroEventFormat), kafkaHeaders);
          } else {
            record = new ProducerRecord<>(eventTopic, null, timestamp, eventKey,
                serializeEvent(event, useAvroEventFormat), kafkaHeaders);
          }
          kafkaFutures.add(producer.send(record, new SinkCallback(startTime)));
        } catch (NumberFormatException ex) {
          throw new EventDeliveryException("Non integer partition id specified", ex);
        } catch (Exception ex) {
          throw new EventDeliveryException("Could not send event", ex);
        }
      }

      if (useKafkaTransactions) {
        producer.commitTransaction();
      } else {
        producer.flush();
        for (Future<RecordMetadata> future : kafkaFutures) {
          future.get();
        }
      }
      // publish batch and commit.
      if (processedEvents > 0) {
        long endTime = System.nanoTime();
        counter.addToKafkaEventSendTimer((endTime - batchStartTime) / (1000 * 1000));
        counter.addToEventDrainSuccessCount(processedEvents);
      }

      transaction.commit();

    } catch (Exception ex) {
      String errorMsg = "Failed to publish events";
      logger.error("Failed to publish events", ex);
      counter.incrementEventWriteOrChannelFail(ex);
      if (transaction != null) {
        try {
          kafkaFutures.clear();
          try {
            if (useKafkaTransactions) {
              producer.abortTransaction();
            }
          } catch (ProducerFencedException e) {
            logger.error("Could not rollback transaction as producer fenced", e);
          } finally {
            transaction.rollback();
            counter.incrementRollbackCount();
          }
        } catch (Exception e) {
          logger.error("Transaction rollback failed", e);
          throw Throwables.propagate(e);
        }
      }
      throw new EventDeliveryException(errorMsg, ex);
    } finally {
      if (transaction != null) {
        transaction.close();
      }
    }

    return result;
  }

  @Override
  public synchronized void start() {
    // instantiate the producer
    producer = new KafkaProducer<>(kafkaProps);
    if (useKafkaTransactions) {
      logger.info("Transactions enabled, initializing transactions");
      producer.initTransactions();
    }
    counter.start();
    super.start();
  }

  @Override
  public synchronized void stop() {
    producer.close();
    counter.stop();
    logger.info("Kafka Sink {} stopped. Metrics: {}", getName(), counter);
    super.stop();
  }

  @Override
  public void configure(Context context) {

    String topicStr = context.getString(AppConstants.TOPIC_CONFIG);
    if(topicStr == null || topicStr.trim().isEmpty())
    	topicStr = System.getProperty(AppConstants.TOPIC_CONFIG, AppConstants.TOPIC_CONFIG_VAL);
    if (topicStr == null || topicStr.isEmpty()) {
      topicStr = AppConstants.DEFAULT_TOPIC;
      logger.warn("Topic was not specified. Using {} as the topic.", topicStr);
    } else {
      logger.info("Using the static topic {}. This may be overridden by event headers", topicStr);
    }

    topic = topicStr;

    timestampHeader = context.getString(AppConstants.TIMESTAMP_HEADER);

    headerMap = context.getSubProperties(AppConstants.KAFKA_HEADER);

    batchSize = context.getInteger(AppConstants.BATCH_SIZE, AppConstants.DEFAULT_BATCH_SIZE);

    if (logger.isDebugEnabled()) {
      logger.debug("Using batch size: {}", batchSize);
    }

    useAvroEventFormat = context.getBoolean(AppConstants.AVRO_EVENT,
                                            AppConstants.DEFAULT_AVRO_EVENT);

    partitionHeader = context.getString(AppConstants.PARTITION_HEADER_NAME);
    staticPartitionId = context.getInteger(AppConstants.STATIC_PARTITION_CONF);

    allowTopicOverride = context.getBoolean(AppConstants.ALLOW_TOPIC_OVERRIDE_HEADER,
                                          AppConstants.DEFAULT_ALLOW_TOPIC_OVERRIDE_HEADER);

    topicHeader = context.getString(AppConstants.TOPIC_OVERRIDE_HEADER,
                                    AppConstants.DEFAULT_TOPIC_OVERRIDE_HEADER);

    String transactionalID = context.getString(AppConstants.TRANSACTIONAL_ID);
    if (transactionalID != null) {
      try {
        context.put(AppConstants.TRANSACTIONAL_ID, InetAddress.getLocalHost().getCanonicalHostName() +
                Thread.currentThread().getName() + transactionalID);
        useKafkaTransactions = true;
      } catch (UnknownHostException e) {
        throw new ConfigurationException("Unable to configure transactional id, as cannot work out hostname", e);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(AppConstants.AVRO_EVENT + " set to: {}", useAvroEventFormat);
    }

    kafkaFutures = new LinkedList<Future<RecordMetadata>>();

    String bootStrapServers = context.getString(AppConstants.BOOTSTRAP_SERVERS_CONFIG);
    if(bootStrapServers == null || bootStrapServers.trim().isEmpty())
    	bootStrapServers = System.getProperty(AppConstants.BOOTSTRAP_SERVERS_CONFIG, AppConstants.BOOTSTRAP_SERVERS_CONFIG_VAL);
    if (bootStrapServers == null || bootStrapServers.isEmpty()) {
      throw new ConfigurationException("Bootstrap Servers must be specified");
    }

    setProducerProps(context, bootStrapServers);

    if (logger.isDebugEnabled() && LogPrivacyUtil.allowLogPrintConfig()) {
      logger.debug("Kafka producer properties: {}", kafkaProps);
    }

    if (counter == null) {
      counter = new KafkaSinkCounter(getName());
    }
  }
  
  private void setProducerProps(Context context, String bootStrapServers) {
    kafkaProps.clear();
    kafkaProps.put(ProducerConfig.ACKS_CONFIG, AppConstants.DEFAULT_ACKS);
    kafkaProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, AppConstants.DEFAULT_KEY_SERIALIZER);
    kafkaProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AppConstants.DEFAULT_VALUE_SERIAIZER);
    kafkaProps.putAll(context.getSubProperties(AppConstants.KAFKA_PRODUCER_PREFIX));
    kafkaProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootStrapServers);
    if (isSSLEnabled(kafkaProps) && "true".equalsIgnoreCase(kafkaProps.getProperty(SSL_DISABLE_FQDN_CHECK))) {
      kafkaProps.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
    }    
    KafkaSSLUtil.addGlobalSSLParameters(kafkaProps);
    
    String jass_config = context.getString(AppConstants.JAAS_CONFIG);
    if(jass_config == null || jass_config.trim().isEmpty())
    	jass_config = System.getProperty(AppConstants.JAAS_CONFIG, AppConstants.JAAS_CONFIG_VAL);
    if(jass_config!=null && !jass_config.isEmpty()) {
    	kafkaProps.put(AppConstants.JAAS_CONFIG, jass_config);
        kafkaProps.put(AppConstants.SASL_MECHANISM, System.getProperty(AppConstants.SASL_MECHANISM, AppConstants.SASL_MECHANISM_PLAIN));
        kafkaProps.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, System.getProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, 
        		AppConstants.SECURITY_PROTOCOL_SASL_SSL));
    }    
    // Required for correctness in Apache Kafka clients prior to 2.6
    kafkaProps.put(AppConstants.CLIENT_DNS_LOOKUP, System.getProperty(AppConstants.CLIENT_DNS_LOOKUP, AppConstants.USE_ALL_DNS_IPS));

    // Best practice for higher availability in Apache Kafka clients prior to 3.0
    kafkaProps.put(AppConstants.SESSION_TIMEOUT_MS, Integer.parseInt(System.getProperty(AppConstants.SESSION_TIMEOUT_MS, AppConstants.SESSION_TIMEOUT_MS_DEFAULT)));
  }

  protected Properties getKafkaProps() {
    return kafkaProps;
  }

  private byte[] serializeEvent(Event event, boolean useAvroEventFormat) throws IOException {
    byte[] bytes;
    if (useAvroEventFormat) {
      if (!tempOutStream.isPresent()) {
        tempOutStream = Optional.of(new ByteArrayOutputStream());
      }
      if (!writer.isPresent()) {
        writer = Optional.of(new SpecificDatumWriter<AvroFlumeEvent>(AvroFlumeEvent.class));
      }
      tempOutStream.get().reset();
      AvroFlumeEvent e = new AvroFlumeEvent(toCharSeqMap(event.getHeaders()),
                                            ByteBuffer.wrap(event.getBody()));
      encoder = EncoderFactory.get().directBinaryEncoder(tempOutStream.get(), encoder);
      writer.get().write(e, encoder);
      encoder.flush();
      bytes = tempOutStream.get().toByteArray();
    } else {
      bytes = event.getBody();
    }
    return bytes;
  }

  private static Map<CharSequence, CharSequence> toCharSeqMap(Map<String, String> stringMap) {
    Map<CharSequence, CharSequence> charSeqMap = new HashMap<CharSequence, CharSequence>();
    for (Map.Entry<String, String> entry : stringMap.entrySet()) {
      charSeqMap.put(entry.getKey(), entry.getValue());
    }
    return charSeqMap;
  }
}

class SinkCallback implements Callback {
  private static final Logger logger = LoggerFactory.getLogger(SinkCallback.class);
  private long startTime;

  public SinkCallback(long startTime) {
    this.startTime = startTime;
  }

  public void onCompletion(RecordMetadata metadata, Exception exception) {
    if (exception != null) {
      logger.warn("Error sending message to Kafka {} ", exception.getMessage());
    }

    if (logger.isDebugEnabled()) {
      long eventElapsedTime = System.currentTimeMillis() - startTime;
      if (metadata != null) {
        logger.debug("Acked message partition:{} ofset:{}", metadata.partition(),
                metadata.offset());
      }
      logger.debug("Elapsed time for send: {}", eventElapsedTime);
    }
  }
}