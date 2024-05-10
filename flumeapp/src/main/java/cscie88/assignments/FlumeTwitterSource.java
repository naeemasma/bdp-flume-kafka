package cscie88.assignments;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.apache.flume.channel.ChannelProcessor;
import org.apache.flume.conf.Configurable;
import org.apache.flume.event.EventBuilder;
import org.apache.flume.source.AbstractSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import cscie88.assignments.app.contants.AppConstants;
import cscie88.assignments.model.TweetEvent;
import cscie88.assignments.sentiments.analysis.SentimentsAnalysisService;
import twitter4j.GeoLocation;
import twitter4j.HashtagEntity;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;

import org.apache.flume.*;

public class FlumeTwitterSource extends AbstractSource
    implements EventDrivenSource, Configurable {
	
	StringBuffer queryCriteria = new StringBuffer();
	String[] keywords = null;
       		
	Twitter twitterIns = null;
	
	public void configure(Context cntxt) {
		
		ConfigurationBuilder configBuilder = new ConfigurationBuilder();
		configBuilder.setOAuthConsumerKey(System.getProperty(AppConstants.CONSUMER_KEY, AppConstants.CONSUMER_KEY_VAL));
		configBuilder.setOAuthConsumerSecret(System.getProperty(AppConstants.CONSUMER_SECRET, AppConstants.CONSUMER_SECRET_VAL));
		configBuilder.setOAuthAccessToken(System.getProperty(AppConstants.ACCESS_TOKEN, AppConstants.ACCESS_TOKEN_VAL));
		configBuilder.setOAuthAccessTokenSecret(System.getProperty(AppConstants.ACCESS_TOKEN_SECRET, AppConstants.ACCESS_TOKEN_SECRET_VAL));
		configBuilder.setJSONStoreEnabled(true);
		keywords = System.getProperty(AppConstants.KEYWORDS, AppConstants.KEYWORDS_VAL).split(AppConstants.COMMA);

		for (String ht: keywords) { 
			if(queryCriteria.toString().length()>0)queryCriteria.append(AppConstants.CHAR_SPACE + AppConstants.OR + AppConstants.CHAR_SPACE);
			queryCriteria.append(AppConstants.CHAR_HASH +ht);
		} 
		
		twitterIns = new TwitterFactory(configBuilder.build()).getInstance();
		
		SentimentsAnalysisService.initialize();
	}
	
	public void start() {
		// The Channel is a passive store that holds the Event until that Event is consumed by a Sink.
		final ChannelProcessor channelProcessor = getChannelProcessor();
		List<Status> tweetsStatus = null;				
		try {
			ObjectMapper o = new ObjectMapper();
			Random random = new Random();
			int rand = 0;
            Query qry = null;
			QueryResult rslt;
			do {
				qry = new Query(queryCriteria.toString());
				do {
					rslt = twitterIns.search(qry);				
					tweetsStatus = rslt.getTweets();
					for (Status tweetStatus: tweetsStatus)
					{
						LocalDateTime createdAt = LocalDateTime.ofInstant(tweetStatus.getCreatedAt().toInstant(), 
								ZoneId.systemDefault());
						String createdAt_fmt = createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME).substring(0,AppConstants.DATE_LEN);
						String createdDateHr = (createdAt.format(AppConstants.HOUR_FORMATTER).replace(AppConstants.HOUR_FORMAT_COLON, 
								AppConstants.ISO_HOUR_FORMAT_T)+AppConstants.ISO_HOUR_FORMAT_APPEND).substring(0, AppConstants.DATE_LEN);
						TweetEvent te = new TweetEvent(tweetStatus.getId(), tweetStatus.getText(), createdAt_fmt, createdDateHr);
						if(tweetStatus.getPlace()!=null)te.setLocation(tweetStatus.getUser().getLocation());
						if(tweetStatus.getGeoLocation()!=null)te.setGeoLocation(tweetStatus.getGeoLocation().getLatitude()
								+AppConstants.COMMA+tweetStatus.getGeoLocation().getLatitude());					
						if(tweetStatus.getPlace()!=null) {
							if(tweetStatus.getPlace().getCountry()!=null)te.setLocation(tweetStatus.getPlace().getCountry());
							if(tweetStatus.getPlace().getGeometryCoordinates()!=null) {
								GeoLocation[][] lo = tweetStatus.getPlace().getGeometryCoordinates();
								te.setGeoLocation(lo[0][0].getLatitude()+AppConstants.COMMA+lo[0][0].getLongitude());
							}
							if(tweetStatus.getPlace().getBoundingBoxCoordinates()!=null) {
								GeoLocation[][] lo = tweetStatus.getPlace().getBoundingBoxCoordinates();
								te.setGeoLocation(lo[0][0].getLatitude()+AppConstants.COMMA+lo[0][0].getLongitude());
							}
						}
						for(HashtagEntity ht:tweetStatus.getHashtagEntities()) {for(int i=0; i<keywords.length;i++){
							if(keywords[i].equalsIgnoreCase(ht.getText())) {te.setHashTag(keywords[i]);break;}}}
						te.setSentimentType(SentimentsAnalysisService.analyseSentiment(te.getTweet()));
						
						rand = random.nextInt(10);				    
						te.setTestLocation(AppConstants.COUNTRIES[rand]);
					    te.setTestGeoLocation(AppConstants.COUNTRIES_GEOLOC[rand][random.nextInt(3)]);
					    String str = o.writeValueAsString(te);
					    Event tweetEvent = EventBuilder.withBody( str.getBytes());
						channelProcessor.processEvent(tweetEvent);
					}
			  } while ((qry = rslt.nextQuery()) != null); 
		  } while (true);
		}catch (TwitterException extw) {
    		extw.printStackTrace();
    		System.out.println("Exception searching tweets - " + extw.getMessage());
        }catch (Exception ex) {
            ex.printStackTrace();
        }
	}
	
	public void stop() {
		super.stop();
	}
}
