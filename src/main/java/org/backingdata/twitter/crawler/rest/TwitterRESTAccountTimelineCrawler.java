package org.backingdata.twitter.crawler.rest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import twitter4j.Paging;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

/**
 * REST Cralwer of Twitter timelines<br/>
 * It is possible to define:<br/>
 * - a list of time-lines to crawl<br/>
 * - a pool of Twitter API keys / tokens in order to speed up timeline cralwing<br/><br/>
 * 
 * As outcome of the crawling process, for each time-line to crawl a new .txt file is created containing one JSON Tweet 
 * per line (https://dev.twitter.com/overview/api/tweets). The name of such file has the following format:<br/>
 * 
 * *SHARED_STRING*_*ACCOUNT_NAME*__*ACCOUNT_ID*_upTo_*CURRENT_DATE*.txt<br/>
 * 
 * @author Francesco Ronzano
 *
 */
public class TwitterRESTAccountTimelineCrawler {

	private static Logger logger = Logger.getLogger(TwitterRESTAccountTimelineCrawler.class.getName());

	// Authentication
	private static List<String> consumerKey = new ArrayList<String>();
	private static List<String> consumerSecret = new ArrayList<String>();
	private static List<String> token = new ArrayList<String>();
	private static List<String> tokenSecret = new ArrayList<String>();

	// Terms
	private static Map<String, Long> getTimelines = new HashMap<String,Long>();

	// ******************************************************
	// PARAMETER TO PROPERLY SET: (do not modify other parameters)
	// 1) fileSharedName - string to add in the filename where twitter timelines are stored
	private static String fileSharedName = "twitter_timeline";
	
	// 2) Twitter app developer credential of each account
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
	// 3) Set pairs string (name), user id (Long) for each twitter account to crawl
	static {
		getTimelines.put("Repubblica", 18935802l);
		getTimelines.put("Sole24Ore", 420351046l);
		getTimelines.put("CorriereDellaSera", 395218906l);
	}
	// 4) Set the directory where to store files, one for each user timeline:
	private static File storageDir = new File("/path/to/local/dir/where/to/store/data");

	// 5) Execute the program (main)
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

			if(getTimelines != null && getTimelines.size() > 0) {
				for(Map.Entry<String, Long> entry : getTimelines.entrySet()) {
					if(entry.getKey() != null && !entry.getKey().equals("") && entry.getValue() != null) {
						String accountName = entry.getKey();
						Long userId = entry.getValue();

						Integer storedUSerTweets = 0;

						PrintWriter twitterTimelinePW = null;
						String fileName = storageDir.getAbsolutePath() + "/" + fileSharedName + "_" + accountName + "_" + userId + "_upTo_" + sdf.format(new Date()) + ".txt";
						try {
							twitterTimelinePW = new PrintWriter(fileName, "UTF-8");
						} catch (FileNotFoundException e) {
							logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
							e.printStackTrace();
						} catch (UnsupportedEncodingException e) {
							logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
							e.printStackTrace();
						}

						logger.info("Start retrieving tweets of user ID: "  + userId);
						// Paging
						Integer pageNum = 1;
						Integer elementsPerPage = 40;
						Boolean nextPage = true;

						while(nextPage) {
							Paging pagingInstance = new Paging();
							pagingInstance.setPage(pageNum);
							pagingInstance.setCount(elementsPerPage);
							try {
								logger.info("Retrieving tweets of user ID: "  + userId + ", page: " + pageNum + ". Tweets per page: " + elementsPerPage + ", already stored: " + storedUSerTweets);

								Twitter currentAccountToQuery =  twitterList.get(accountCredentialsId);
								logger.info("Queried account: "  + accountCredentialsId);
								accountCredentialsId = (accountCredentialsId + 1) % consumerKey.size();
								ResponseList<Status> timeline = currentAccountToQuery.getUserTimeline(userId, pagingInstance);
								pageNum++;
								
								Thread.sleep(sleepTimeInMilliseconds);

								if(timeline != null && timeline.size() > 0) {
									logger.info("Retrieved " + timeline.size() + " tweets (user ID: "  + userId + ", page: " + (pageNum - 1) + ". Tweets per page: " + elementsPerPage + ")");
									Iterator<Status> statusIter = timeline.iterator();
									while(statusIter.hasNext()) {
										Status status = statusIter.next();
										if(status != null && status.getCreatedAt() != null) {
											String msg = DataObjectFactory.getRawJSON(status);
											logger.info("   STORING > " + msg);
											
											twitterTimelinePW.write(msg + "\n");
											
											logger.info("STORED");
											storedUSerTweets++;
											twitterTimelinePW.flush();
											
										}
									}
								}
								else {
									logger.info("Retrieved NO tweets (user ID: "  + userId + ", page: " + (pageNum - 1) + ". Tweets per page: " + elementsPerPage + ", already stored: " + storedUSerTweets + ")");
									nextPage = false;
								}
							} catch (TwitterException e) {
								logger.info("Error while querying Twitter: " + e.getMessage());
								e.printStackTrace();
							}
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
