package org.backingdata.twitter.crawler.streaming;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.json.DataObjectFactory;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import com.twitter.hbc.twitter4j.handler.StatusStreamHandler;
import com.twitter.hbc.twitter4j.message.DisconnectMessage;
import com.twitter.hbc.twitter4j.message.StallWarningMessage;

/**
 * STREAMING Cralwer of Twitter - retrieves all tweets matching some specific keywords and/or in some specific language.<br/>
 * 
 * Have a look at the main code to identify how to define the set of keywords / languages and Twitter credentials.<br/><br/>
 * 
 * For each keywords, for every 20,000 tweet retrieved, a .txt file is created containing one JSON Tweet 
 * per line (https://dev.twitter.com/overview/api/tweets). The name of such file has the following format:<br/>
 * 
 * *SHARED_STRING*_*SEQUENCE_ID*_*KEYWORD*_from_*CRAWLING_START_DATE*.txt<br/><br/>
 * Log files with crawling errors and messages are also created.<br/>
 * 
 * 
 * @author Francesco Ronzano
 *
 */
public class TwitterSTREAMHashtagCrawler {

	private static Logger logger = Logger.getLogger(TwitterSTREAMHashtagCrawler.class.getName());

	private String fileSharedName = "twitter_drugs_v1";

	// Authentication
	public List<String> consumerKey = new ArrayList<String>();
	public List<String> consumerSecret = new ArrayList<String>();
	public List<String> token = new ArrayList<String>();
	public List<String> tokenSecret = new ArrayList<String>();

	// Terms
	public List<String> trackTerms = new ArrayList<String>();
	public List<String> langList = new ArrayList<String>();
	public Map<String, Long> userMap = new HashMap<String, Long>();

	// Blocking queue for tweets to process
	private BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);


	// Storage parameters
	private static File storageDir = null;
	private Map<String, PrintWriter> storageFileMap = new HashMap<String, PrintWriter>();
	private Map<String, Integer> storageFileCount = new HashMap<String, Integer>();
	private Map<String, Integer> storageFileId = new HashMap<String, Integer>();

	private PrintWriter logFile = null;
	private Integer logFileNumber = 0;
	private Integer totalTweetStoredCount = 0;

	private Integer changeFileNumTweets = 20000;
	private Integer changeLogFileNumTweets = 80000;

	// Date formatter
	private SimpleDateFormat sdf = new SimpleDateFormat("dd_M_yyyy__hh_mm_ss");

	private long pastFreeMem = 0l;
	public void printMemoryStatus() {
		int MegaBytes = 10241024;
		long totalMemory = Runtime.getRuntime().totalMemory() / MegaBytes;
		long freeMemory = Runtime.getRuntime().freeMemory() / MegaBytes;
		long maxMemory = Runtime.getRuntime().maxMemory() / MegaBytes;

		System.out.println("Total heap size: " + totalMemory + " Mb of which: "
				+ " 1) free mem: " + freeMemory + " Mb (before: " + pastFreeMem + "Mb)"
				+ " 2) Max mem: " + maxMemory + " Mb");
		pastFreeMem = freeMemory;
	}

	public void checkLogAndSotrageFiles() {

		// Init maps
		if(storageFileMap.size() == 0) {
			for(String entry : trackTerms) {
				storageFileMap.put(entry, null);
			}
			for(Entry<String, Long> entry : userMap.entrySet()) {
				storageFileMap.put("ACCOUNT_" + entry.getKey().toLowerCase(), null);
			}
		}
		if(storageFileCount.size() == 0) {
			for(String entry : trackTerms) {
				storageFileCount.put(entry, 0);
			}
			for(Entry<String, Long> entry : userMap.entrySet()) {
				storageFileCount.put("ACCOUNT_" + entry.getKey().toLowerCase(), 0);
			}
		}
		if(storageFileId.size() == 0) {
			for(String entry : trackTerms) {
				storageFileId.put(entry, 0);
			}
			for(Entry<String, Long> entry : userMap.entrySet()) {
				storageFileId.put("ACCOUNT_" + entry.getKey().toLowerCase(), 0);
			}
		}

		// If files are opened and the changeFileNumTweets is reached, these files are closed
		for(String entry : trackTerms) {
			String termString = entry;
			if( storageFileMap.containsKey(termString) && storageFileCount.containsKey(termString) && storageFileId.containsKey(termString) ) {

				if( storageFileMap.get(termString) != null && ((storageFileCount.get(termString) % this.changeFileNumTweets) == 0)) {
					storageFileMap.get(termString).flush();
					storageFileMap.get(termString).close();
					storageFileMap.put(termString, null);

					// Increase the number of storage count to not generate other files if the tweet is not stored
					Integer storageCountAppo = storageFileCount.get(termString);
					storageFileCount.put(termString, ++storageCountAppo);
				}
			}			
		}

		for(Entry<String, Long> entry : userMap.entrySet()) {
			String userString = entry.getKey().toLowerCase();
			if( storageFileMap.containsKey("ACCOUNT_" + userString) && storageFileCount.containsKey("ACCOUNT_" + userString) && storageFileId.containsKey("ACCOUNT_" + userString) ) {

				if( storageFileMap.get("ACCOUNT_" + userString) != null && ((storageFileCount.get("ACCOUNT_" + userString) % this.changeFileNumTweets) == 0)) {
					storageFileMap.get("ACCOUNT_" + userString).flush();
					storageFileMap.get("ACCOUNT_" + userString).close();
					storageFileMap.put("ACCOUNT_" + userString, null);

					// Increase the number of storage count to not generate other files if the tweet is not stored
					Integer storageCountAppo = storageFileCount.get("ACCOUNT_" + userString);
					storageFileCount.put("ACCOUNT_" + userString, ++storageCountAppo);
				}
			}
		}

		if(logFile != null && ( (totalTweetStoredCount % this.changeLogFileNumTweets) == 0) ) {
			logFile.flush();
			logFile.close();
			logFile = null;

			// Increase the number of storage count to not generate other log files if the tweet is not stored
			totalTweetStoredCount++;
		}

		// Storage and log - open new files if null
		for(String entry : trackTerms) {
			String termString = entry;
			if( storageFileMap.containsKey(termString) && storageFileCount.containsKey(termString) && storageFileId.containsKey(termString) ) {

				if(storageFileMap.get(termString) == null) {
					String fileName = storageDir.getAbsolutePath() + File.separator + fileSharedName + "_TERM_" + termString + "_" + ( storageFileId.get(termString) ) + "_from_" + sdf.format(new Date()) + ".txt";
					Integer fileId = storageFileId.get(termString);
					storageFileId.put(termString, fileId + 1);
					try {
						storageFileMap.put(termString, new PrintWriter(fileName, "UTF-8"));
					} catch (FileNotFoundException e) {
						logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}			
		}

		for(Entry<String, Long> entry : userMap.entrySet()) {
			String accountString = "ACCOUNT_" + entry.getKey().toLowerCase();
			if( storageFileMap.containsKey(accountString) && storageFileCount.containsKey(accountString) && storageFileId.containsKey(accountString) ) {

				if(storageFileMap.get(accountString) == null) {
					String fileName = storageDir.getAbsolutePath() + File.separator + fileSharedName + "_" + accountString + "_" + ( storageFileId.get(accountString) ) + "_from_" + sdf.format(new Date()) + ".txt";
					Integer fileId = storageFileId.get(accountString);
					storageFileId.put(accountString, fileId + 1);
					try {
						storageFileMap.put(accountString, new PrintWriter(fileName, "UTF-8"));
					} catch (FileNotFoundException e) {
						logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
						e.printStackTrace();
					} catch (UnsupportedEncodingException e) {
						logger.info("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
						e.printStackTrace();
					}
				}
			}			
		}

		// Log file	
		if(this.logFile == null) {
			String fileName = storageDir.getAbsolutePath() + File.separator + "LOG_" + fileSharedName + "_" + (logFileNumber++) + "_from_" + sdf.format(new Date()) + ".txt";
			try {
				this.logFile = new PrintWriter(fileName, "UTF-8");
			} catch (FileNotFoundException e) {
				logger.info("CANNOT OPEN LOG FILE: " + fileName + " - Exception: " + e.getMessage());
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				logger.info("CANNOT OPEN LOG FILE: " + fileName + " - Exception: " + e.getMessage());
				e.printStackTrace();
			}
		}

	}

	// Listener for Tweet stream
	@SuppressWarnings("unused")
	private StatusListener listener = new StatusStreamHandler() {

		public void onDeletionNotice(StatusDeletionNotice arg0) {
			// TODO Auto-generated method stub

		}

		public void onScrubGeo(long arg0, long arg1) {
			// TODO Auto-generated method stub

		}

		public void onStallWarning(StallWarning arg0) {
			// TODO Auto-generated method stub

		}

		public void onStatus(Status arg0) {
			// COPY CONTENTS BELOW:
			/*
			 * while( !queue.isEmpty() ) {...
			 */
		}

		public void onTrackLimitationNotice(int arg0) {
			checkLogAndSotrageFiles();

			try {
				logFile.write((new Date()).toString() + " - TRACK LIMITATION NOTICE: " + arg0 + "\n");
				logger.info((new Date()).toString() + " - TRACK LIMITATION NOTICE: " + arg0 + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}

		public void onException(Exception arg0) {
			checkLogAndSotrageFiles();

			try {
				logFile.write((new Date()).toString() + " - EXCEPTION: " + arg0.getMessage() + "\n");
				logger.info((new Date()).toString() + " - EXCEPTION: " + arg0.getMessage() + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}

		}

		public void onDisconnectMessage(DisconnectMessage message) {
			checkLogAndSotrageFiles();

			try {
				logFile.write((new Date()).toString() + " - DISCONNECT: CODE: " + message.getDisconnectCode() + ", REASON: " + message.getDisconnectReason() + "\n");
				logger.info((new Date()).toString() + " - DISCONNECT: CODE: " + message.getDisconnectCode() + ", REASON: " + message.getDisconnectReason() + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}

		public void onStallWarningMessage(StallWarningMessage warning) {
			checkLogAndSotrageFiles();

			try {
				logFile.write((new Date()).toString() + " - STALL WARNING: CODE: " + warning.hashCode() + ", REASON: " + warning.getMessage() + ", PERCENT FULL: " + warning.getPercentFull() + "\n");
				logger.info((new Date()).toString() + " - STALL WARNING: CODE: " + warning.hashCode() + ", REASON: " + warning.getMessage() + ", PERCENT FULL: " + warning.getPercentFull() + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}

		public void onUnknownMessageType(String msg) {
			checkLogAndSotrageFiles();

			try {
				logFile.write((new Date()).toString() + " - UNKNOWN MESSAGE: " + msg + "\n");
				logger.info((new Date()).toString() + " - UNKNOWN MESSAGE: " + msg + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}
	};

	@SuppressWarnings("static-access")
	public void startCrawling() {
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

		endpoint.trackTerms(this.trackTerms);
		logger.info("CRAWLING: " + this.trackTerms.size() + " TERMS:");
		for(String term : this.trackTerms) {
			logger.info("   TERM: " + term);
		}

		if(this.langList != null && this.langList.size() > 0) {
			endpoint.languages(this.langList);
			logger.info("CRAWLING: " + this.langList.size() + " LANGUAGES:");
			for(String language : this.langList) {
				logger.info("   LANGUAGE: " + language);
			}
		}

		if(this.userMap != null && this.userMap.size() > 0) {
			List<Long> userList = new ArrayList<Long>();
			for(Entry<String, Long> entry : this.userMap.entrySet()) {
				userList.add(new Long(entry.getValue()));
			}

			endpoint.followings(userList);
			logger.info("CRAWLING: " + userList.size() + " USERS:");
			for(Long user : userList) {
				logger.info("   USER: " + user);
			}
		}

		Authentication auth = new OAuth1(this.consumerKey.get(0), this.consumerSecret.get(0), this.token.get(0), this.tokenSecret.get(0));

		// Create a new BasicClient. By default gzip is enabled.
		BasicClient client = new ClientBuilder()
				.hosts(Constants.STREAM_HOST)
				.endpoint(endpoint)
				.authentication(auth)
				.processor(new StringDelimitedProcessor(queue))
				.build();

		// Establish a connection
		client.connect();

		// Loop to store tweets
		while(true) {
			this.checkLogAndSotrageFiles();

			while( !queue.isEmpty() ) {
				try {
					String msg;
					msg = queue.take();
					Status receivedStatus = DataObjectFactory.createStatus(msg);
					if(receivedStatus != null && receivedStatus.getCreatedAt() != null) {

						this.checkLogAndSotrageFiles();

						try {

							// Save to account file
							long userId = receivedStatus.getUser().getId();
							String userName = "";
							for(Entry<String, Long> entry_user : this.userMap.entrySet()) {
								if(entry_user.getValue() == userId) {
									userName = entry_user.getKey().toLowerCase();
								}
							}

							if(userName != null && !userName.equals("")) {
								for(Map.Entry<String, PrintWriter> entry_int : storageFileMap.entrySet()) {
									if(entry_int.getKey().equals("ACCOUNT_" + userName)) {
										entry_int.getValue().write(msg);
										entry_int.getValue().flush();
										totalTweetStoredCount++;

										if(totalTweetStoredCount % 10 == 0) {
											printMemoryStatus();
											System.gc();
											System.out.println("GARBAGE COLLECTOR CALLED: ");
											printMemoryStatus();
										}

										for(Map.Entry<String, Integer> entry_int_int : storageFileCount.entrySet()) {
											if(entry_int_int.getKey().equals("ACCOUNT_" + userName)) {
												Integer storageCount = entry_int_int.getValue();
												storageFileCount.put("ACCOUNT_" + userName, storageCount + 1);
												System.out.println("SAVE: " + userName + " tot: " + (storageCount + 1) + " - queue free places: " + queue.remainingCapacity());
											}
										}
									}
								}	
							}

							// Save to term file
							for(Map.Entry<String, PrintWriter> entry_int : storageFileMap.entrySet()) {
								// If it is a term
								if(entry_int != null && entry_int.getKey() != null && !entry_int.getKey().startsWith("ACCOUNT_")) {
									String tweetTextLowercased = receivedStatus.getText().toLowerCase();
									String termLowercased = entry_int.getKey().toLowerCase();

									// Check if tweet contains the term
									boolean containsTerm = false;
									Pattern pattern = Pattern.compile("(^|[\\s|\\W])" + Pattern.quote(termLowercased) + "([\\s|\\W]|$)", Pattern.CASE_INSENSITIVE);
									Matcher matcher = pattern.matcher(tweetTextLowercased);
									Integer numMatches = 0;
									while (matcher.find()) {
										numMatches++;
									}

									if(numMatches > 0) {
										containsTerm = true;
									}

									// Store tweet if it contains the term
									if(containsTerm == true) {
										entry_int.getValue().write(msg);
										entry_int.getValue().flush();
										totalTweetStoredCount++;

										if(totalTweetStoredCount % 100 == 0) {
											printMemoryStatus();
											System.gc();
											System.out.println("GARBAGE COLLECTOR CALLED: ");
											printMemoryStatus();
										}

										for(Map.Entry<String, Integer> entry_int_int : storageFileCount.entrySet()) {
											if(entry_int_int.getKey().equals(entry_int.getKey())) {
												Integer storageCount = entry_int_int.getValue();
												storageFileCount.put(entry_int.getKey(), storageCount + 1);
												System.out.println("SAVE: " + entry_int.getKey() + " tot: " + (storageCount + 1) + " - queue free places: " + queue.remainingCapacity());
											}
										}
									}
								}
							}

						} catch (Exception e) {
							System.out.println("Exception " + e.getMessage());
							e.printStackTrace();
						}
					}
					else {
						logFile.write(sdf.format(new Date()) + " - ERROR CODE: " + msg);
						logger.info(sdf.format(new Date()) + " - ERROR CODE: " + msg);	
						logFile.flush();
					}
				} catch (TwitterException e) {
					logger.info("ERROR WHILE PARSING TWEET: " + e.getMessage());
				} catch (InterruptedException e1) {
					logger.info("INTERRUPTED THREAD EXCEPTION: " + e1.getMessage());
				}
			}

			try {	
				Thread.currentThread().sleep(5000);
			} catch (InterruptedException e) {
				System.out.println("ERROR WHILE SLEEP PROCESSING MESSAGE THREAD: " + e.getMessage());
			}

		}
	}

	/**
	 * One argument is required, the full local path where data should be stored
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		if(args != null && args.length > 0 && args[0] != null && !args[0].equals("")) {
			try {
				File stgDir = new File(args[0]);
				if(stgDir != null && stgDir.exists() && stgDir.isDirectory()) {
					storageDir = stgDir;
				}
			}
			catch (Exception e) {
				e.printStackTrace();
				System.out.println("Exception setting storage dir: " + e.getMessage());
				logger.info("Exception setting storage dir: " + e.getMessage());	
			}
		}
		System.out.println("Storage dir: " + storageDir.getAbsolutePath());
		logger.info("Storage dir: " + storageDir.getAbsolutePath());


		TwitterSTREAMHashtagCrawler crawler = new TwitterSTREAMHashtagCrawler();

		// Add Twitter credentials
		crawler.consumerKey.add("PUT_CONSUMER_KEY");
		crawler.consumerSecret.add("PUT_CONSUMER_SECRET");
		crawler.token.add("PUT_ACCESS_TOKEN");
		crawler.tokenSecret.add("PUT_ACCESS_KEY");

		// Add one or more keywords to crawl
		crawler.trackTerms.add("#news");
		crawler.trackTerms.add("politics");
		// ...
		crawler.trackTerms.add("\"breaking news\"");
		
		// Optionally add one or more languages to filter tweets
		crawler.langList.add("it");
		crawler.langList.add("es");

		crawler.startCrawling();
	}

}
