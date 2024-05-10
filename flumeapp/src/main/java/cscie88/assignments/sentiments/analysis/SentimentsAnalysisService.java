package cscie88.assignments.sentiments.analysis;

import java.util.Properties;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

public class SentimentsAnalysisService {
	public static StanfordCoreNLP processor;
	public static void initialize() {
		Properties properties = new Properties();
        properties.setProperty("annotators", "tokenize, ssplit, parse, sentiment");
        processor = new StanfordCoreNLP(properties);
	}

	/*
	 * Returns sentiment range: very negative, negative, neutral, positive, very positive 
	 */
	public static String analyseSentiment(String text) {
		String sentimentType = "Unknown";
		if (!(text == null || text.isEmpty())) {
		    Annotation an = processor.process(text);
		    for (CoreMap eachSentence : an.get(CoreAnnotations.SentencesAnnotation.class)) {
		        sentimentType = eachSentence.get(SentimentCoreAnnotations.SentimentClass.class);
		    }
		}
		return sentimentType;
	}
}
