package cscie88.assignments.model;

public class TweetEvent {
	Long id;
	String tweet;
	String createdAt;
	String createdDateHour;
	String location;
	String geoLocation; //geopoint
	String sentimentType;
	String hashTag;
	String testLocation;
	String testGeoLocation;
	
	public String getTweet() {
		return tweet;
	}
	public void setTweet(String tweet) {
		this.tweet = tweet;
	}
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getCreatedAt() {
		return createdAt;
	}
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	public String getCreatedDateHour() {
		return createdDateHour;
	}
	public void setCreatedDateHour(String createdDateHour) {
		this.createdDateHour = createdDateHour;
	}	
	public String getGeoLocation() {
		return geoLocation;
	}
	public void setGeoLocation(String geoLocation) {
		this.geoLocation = geoLocation;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}	
	
	public String getSentimentType() {
		return sentimentType;
	}
	public void setSentimentType(String sentimentType) {
		this.sentimentType = sentimentType;
	}
	public String getHashTag() {
		return hashTag;
	}
	public void setHashTag(String hashTag) {
		this.hashTag = hashTag;
	}	
	public String getTestLocation() {
		return testLocation;
	}
	public void setTestLocation(String testLocation) {
		this.testLocation = testLocation;
	}
	public String getTestGeoLocation() {
		return testGeoLocation;
	}
	public void setTestGeoLocation(String testGeoLocation) {
		this.testGeoLocation = testGeoLocation;
	}
	public TweetEvent(Long id, String tweet, String createdAt, String createdDateHour) {
		super();
		this.tweet = tweet;
		this.id = id;
		this.createdAt = createdAt;
		this.createdDateHour = createdDateHour;
	}
	public TweetEvent() {
		super();
	}
	@Override
	public String toString() {
		return "TweetEvent [id=" + id + ", createdAt=" + createdAt + ", tweet=" + tweet + ", createdDateHour=" + createdDateHour 
				+ ", location=" + location+ ", geoLocation=(" + geoLocation + "), testLocation=" + testLocation+ ", testGeoLocation=(" + testGeoLocation+ ")]";
	}
}