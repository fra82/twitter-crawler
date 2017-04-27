package org.backingdata.twitter.crawler.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

/**
 * REST Cralwer of Twitter - by keyword(s)<br/>
 * It is possible to define:<br/>
 * - a list of keywords / hashtags to crawl<br/>
 * - a pool of Twitter API keys / tokens in order to speed up timeline cralwing<br/><br/>
 * 
 * As outcome of the crawling process, for each time-line to crawl a new .txt file is created containing one JSON Tweet 
 * per line (https://dev.twitter.com/overview/api/tweets). The name of such file has the following format:<br/>
 * 
 * *SHARED_STRING*_*KEYWORD*_upTo_*CURRENT_DATE*.txt<br/>
 * 
 * @author Francesco Ronzano
 *
 */
public class TwitterRESTKeywordSearchCrawler {

	private static Logger logger = Logger.getLogger(TwitterRESTKeywordSearchCrawler.class.getName());

	// Authentication
	private static List<String> consumerKey = new ArrayList<String>();
	private static List<String> consumerSecret = new ArrayList<String>();
	private static List<String> token = new ArrayList<String>();
	private static List<String> tokenSecret = new ArrayList<String>();

	// Terms
	private static Set<String> getKeywords = new HashSet<String>();

	// ******************************************************
	// PARAMETER TO SET: (do not modify other parameters)
	// 1) fileSharedName - string to add in the filename where twitter timelines are stored
	private static String fileSharedName = "twitter_keyword";
	// 3) Twitter app developer credential of each account
	// App developer credentials can be obtained by registering a Twitter App at: https://dev.twitter.com/
	static {
		// TweetRESTaccess1
		consumerKey.add("PUT_CONSUMER_KEY_1");
		consumerSecret.add("PUT_CONSUMER_SECRET_1");
		token.add("PUT_ACCESS_TOKEN_1");
		tokenSecret.add("PUT_ACCESS_KEY_1");

		// TweetRESTaccess2
		consumerKey.add("PUT_CONSUMER_KEY_2");
		consumerSecret.add("PUT_CONSUMER_SECRET_21");
		token.add("PUT_ACCESS_TOKEN_2");
		tokenSecret.add("PUT_ACCESS_KEY_2");

		// ...

		// TweetRESTaccessN
		consumerKey.add("PUT_CONSUMER_KEY_N");
		consumerSecret.add("PUT_CONSUMER_SECRET_N");
		token.add("PUT_ACCESS_TOKEN_N");
		tokenSecret.add("PUT_ACCESS_KEY_N");
	}

	// 4) Set terms to crawl
	static {
		getKeywords.add("#news");
		getKeywords.add("politics");
		// ...
		getKeywords.add("\"breaking news\"");
	}
	// 5) Set the directory where to store files, one for each user timeline:
	private static File storageDir = new File("/path/to/local/dir/where/to/store/data");

	// 6) Execute the program (main)
	// ******************************************************
	// ******************************************************

	// Blocking queue for tweets to process
	private static Integer sleepTimeInMilliseconds = 5000;

	// Date formatter
	private static SimpleDateFormat sdf = new SimpleDateFormat("dd_M_yyyy__hh_mm_ss");

	public static void startCrawling() {

		sleepTimeInMilliseconds = new Integer( ((int) (5000d / new Double(consumerKey.size()))) + 250);

		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setJSONStoreEnabled(true);

		TwitterFactory tf = new TwitterFactory(cb.build());

		List<Twitter> twitterList = new ArrayList<Twitter>();

		for(int i = 0; i < consumerKey.size(); i++) {
			Twitter twitter = tf.getInstance();
			AccessToken accessToken = new AccessToken(token.get(i), tokenSecret.get(i));
			twitter.setOAuthConsumer(consumerKey.get(i), consumerSecret.get(i));
			twitter.setOAuthAccessToken(accessToken);
			twitterList.add(twitter);
		}

		try {

			Integer accountCredentialsId = 0;

			if(getKeywords != null && getKeywords.size() > 0) {
				for(String entry : getKeywords) {
					if(entry != null && !entry.equals("")) {

						Integer storedKeywordTweets = 0;

						PrintWriter twitterKeywordPW = null;
						String fileName = storageDir.getAbsolutePath() + "/" + fileSharedName + "_" + entry.replaceAll("\\W+", "") + "_upTo_" + sdf.format(new Date()) + ".txt";
						try {
							twitterKeywordPW = new PrintWriter(fileName, "UTF-8");
						} catch (FileNotFoundException e) {
							logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
							e.printStackTrace();
						}

						logger.info("Start retrieving tweets with keyword: "  + entry);

						// ATTENTION: searching Spanish tweets!!!
						Query query = new Query(entry);
						query.lang("es");

						int numberOfTweets = 300;
						long lastID = Long.MAX_VALUE;

						ArrayList<Status> statusList = new ArrayList<Status>();
						while(statusList.size () < numberOfTweets) {

							if(storedKeywordTweets >= numberOfTweets) {
								break;
							}

							if(numberOfTweets - statusList.size() > 100) {
								query.setCount(100);
							}
							else{ 
								query.setCount(numberOfTweets - statusList.size());
							}

							try {
								Twitter currentAccountToQuery =  twitterList.get(accountCredentialsId);
								logger.info("Queried account: "  + accountCredentialsId);
								accountCredentialsId = (accountCredentialsId + 1) % consumerKey.size();

								QueryResult result = currentAccountToQuery.search(query);

								Thread.currentThread().sleep(sleepTimeInMilliseconds);

								if(result.getTweets() == null || result.getTweets().size() == 0) {
									logger.info("No tweets retrieved when paging - keyword: "  + entry);
									break;
								}
								else {
									logger.info(result.getTweets().size() + " results found when paging - total results: " + statusList.size() + " - keyword: "  + entry);
								}

								if(result.getTweets() != null && result.getTweets().size() > 0) {
									logger.info("Retrieved " + result.getTweets().size() + " tweets - keyword: "  + entry);
									for (int i = 0; i < result.getTweets().size(); i++) {
										Status status = result.getTweets().get(i);

										if(status != null && status.getCreatedAt() != null) {
											String msg = DataObjectFactory.getRawJSON(status);
											logger.info("   STORING > " + msg);

											twitterKeywordPW.write(msg + "\n");

											logger.info("STORED");
											storedKeywordTweets++;
											twitterKeywordPW.flush();
										}
									}
								}
								else {
									logger.info("Retrieved NO tweets - keyword: "  + entry);
								}

								logger.info("Gathered in total " + statusList.size() + " tweets - keyword: "  + entry);
								for (Status t: result.getTweets()) {
									if(t.getId() < lastID) {
										lastID = t.getId();
										query.setMaxId(lastID);
									}
								}
							}
							catch (TwitterException te) {
								te.printStackTrace();
								logger.info("ERROR: Couldn't connect: " + te.getMessage());
							}; 

							query.setMaxId(lastID-1);
						}




					}
				}
			}

		} catch (Exception e) {
			logger.info("Error generic: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		startCrawling();
	}

}
