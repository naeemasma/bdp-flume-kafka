package cscie88.assignments.app.contants;

import java.time.format.DateTimeFormatter;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.ProducerConfig;

public class AppConstants {

	public static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd:HH");
    public static final String ISO_HOUR_FORMAT_APPEND = ":00:00.00Z";
    public static final char ISO_HOUR_FORMAT_T = 'T';
    public static final String ISO_HOUR_FORMAT_Z = "Z";
    public static final char CHAR_SPACE = ' ';
    public static final String STRING_EMPTY ="";
    public static final char HOUR_FORMAT_COLON = ':';    
	public static final String CONSUMER_KEY = "consumerKey";
	public static final String CONSUMER_SECRET = "consumerSecret";
	public static final String ACCESS_TOKEN = "accessToken";
	public static final String ACCESS_TOKEN_SECRET = "accessTokenSecret";
	public static final String KEYWORDS = "keywords";
	public static final String OR = "OR";
	public static final String COMMA = ",";
	public static final String AND = "AND";
	public static final char LEFT_BRACKET = '(';
	public static final char RIGHT_BRACKET = ')';
	public static final String DATA = "data";
	public static final String TEXT = "text";
	public static final String COLON = ":";
	public static final int DATE_LEN = 19;
	
	public static final String[] COUNTRIES = {"China", "India", "United States", "Indonesia", "Pakistan", "Nigeria", "Brazil", "Bangladesh", "Russia",
    "Mexico"};
	
	public static final String[][] COUNTRIES_GEOLOC = {{"31.1667,121.4667", "23.1288,113.2590", "39.9050,116.3914"},
			{"28.6600,77.2300", "18.9667,72.8333", "22.5411,88.3378"},
			{"42.429752,-71.071022", "27.109644,-82.448792", "31.284788,-92.471176"},
			{"-6.2146,106.8451", "-7.2458,112.7378", "-6.9500,107.5667"},
			{"-6.9500,107.5667","31.5497,74.3436","31.4180,73.0790"},
			{"6.4500,3.4000", "12.0000,8.5167", "7.3964,3.9167"},
			{"-23.5504,-46.6339", "-22.9083,-43.1964", "-19.9281,-43.9419"},
			{"23.7289,90.3944","22.3350,91.8325","22.8167,89.5500"},
			{"55.7558,37.6178","59.9500,30.3167", "55.0333,82.9167"},
			{"19.4333,-99.1333","20.6767,-103.3475","25.6667,-100.3000"}};
	
	public static final String CONSUMER_KEY_VAL = "<API Key>";
	public static final String CONSUMER_SECRET_VAL = "<API Key Secret>";
	public static final String ACCESS_TOKEN_VAL = "<Access Token>";
	public static final String ACCESS_TOKEN_SECRET_VAL = "<Access Token Secret>";
	public static final String KEYWORDS_VAL = "netflix,disneyPlus,hulu,primeVideo,vudu,hbo";
	public static final char CHAR_HASH = '#';
	
	public static final String BOOTSTRAP_SERVERS_CONFIG_VAL = "kafka-broker.cloud:9092";
	public static final String TOPIC_CONFIG_VAL = "tweets"; 

	public static final String SECURITY_PROTOCOL_SASL_SSL = "SASL_SSL";
	public static final String JAAS_CONFIG = "sasl.jaas.config";
	public static final String JAAS_CONFIG_VAL = "org.apache.kafka.common.security.plain.PlainLoginModule required username='<Kafka API Key>' password='<Kafka API Secret>';";
	public static final String SASL_MECHANISM = "sasl.mechanism";
	public static final String SASL_MECHANISM_PLAIN = "PLAIN";
	
	public static final String KAFKA_PREFIX = "kafka.";
	public static final String KAFKA_PRODUCER_PREFIX = KAFKA_PREFIX + "producer.";

	  /* Properties */
	
	  public static final String TOPIC_CONFIG = KAFKA_PREFIX + "topic";
	  public static final String BATCH_SIZE = "flumeBatchSize";
	  public static final String BOOTSTRAP_SERVERS_CONFIG =
	      KAFKA_PREFIX + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
	
	  public static final String TRANSACTIONAL_ID = KAFKA_PREFIX + "producer." +  ProducerConfig.TRANSACTIONAL_ID_CONFIG;
	
	  public static final String KEY_HEADER = "key";
	  public static final String DEFAULT_TOPIC_OVERRIDE_HEADER = "topic";
	  public static final String TOPIC_OVERRIDE_HEADER = "topicHeader";
	  public static final String TIMESTAMP_HEADER = "timestampHeader";
	  public static final String ALLOW_TOPIC_OVERRIDE_HEADER = "allowTopicOverride";
	  public static final boolean DEFAULT_ALLOW_TOPIC_OVERRIDE_HEADER = true;
	  public static final String KAFKA_HEADER = "header.";
	
	  public static final String AVRO_EVENT = "useFlumeEventFormat";
	  public static final boolean DEFAULT_AVRO_EVENT = false;
	
	  public static final String PARTITION_HEADER_NAME = "partitionIdHeader";
	  public static final String STATIC_PARTITION_CONF = "defaultPartitionId";
	
	  public static final String DEFAULT_KEY_SERIALIZER =
	      "org.apache.kafka.common.serialization.StringSerializer";
	  public static final String DEFAULT_VALUE_SERIAIZER =
	      "org.apache.kafka.common.serialization.ByteArraySerializer";
	
	  public static final int DEFAULT_BATCH_SIZE = 100;
	  public static final String DEFAULT_TOPIC = "default-flume-topic";
	  public static final String DEFAULT_ACKS = "1";
	  public static final String DEFAULT_ACKS_ALL = "all";
	  public static final String CLIENT_DNS_LOOKUP = "client.dns.lookup";
	  public static final String USE_ALL_DNS_IPS = "use_all_dns_ips";
	  public static final String SESSION_TIMEOUT_MS = "session.timeout.ms";
	  public static final String SESSION_TIMEOUT_MS_DEFAULT = "45000";
	  
}
