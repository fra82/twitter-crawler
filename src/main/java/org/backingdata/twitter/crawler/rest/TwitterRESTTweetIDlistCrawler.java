package org.backingdata.twitter.crawler.rest;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.backingdata.twitter.crawler.util.CredentialObject;
import org.backingdata.twitter.crawler.util.PropertyManager;
import org.backingdata.twitter.crawler.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

/**
 * REST Cralwer of Twitter - by list of tweet IDs<br/>
 * It is possible to define:<br/>
 * - the full local path of a local text file containing a list of tweet IDs (one per line)<br/>
 * - a pool of Twitter API keys / tokens in order to speed up timeline cralwing<br/><br/>
 * 
 * As outcome of the crawling process, for each time-line to crawl a new .txt file is created containing one JSON Tweet 
 * per line (https://dev.twitter.com/overview/api/tweets).
 * 
 * @author Francesco Ronzano
 *
 */
public class TwitterRESTTweetIDlistCrawler {

	private static Logger logger = LoggerFactory.getLogger(TwitterRESTTweetIDlistCrawler.class.getName());

	// Authentication
	private static List<String> consumerKey = new ArrayList<String>();
	private static List<String> consumerSecret = new ArrayList<String>();
	private static List<String> token = new ArrayList<String>();
	private static List<String> tokenSecret = new ArrayList<String>();

	// Full local path of a local text file containing a list of tweet IDs (one per line)
	private static String fullPathOfTweetIDfile = "";

	// Tweet IDs
	private static Set<String> tweetIDset = new HashSet<String>();

	// Output directory
	private static String outputDirPath = "";
	
	// Output format
	private static String outpuTweetFormat = "";


	private static String fileSharedName = "tweet_by_ID";

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

			File storageDir = new File(outputDirPath);
			PrintWriter twitterIDPW = null;
			String fileName = storageDir.getAbsolutePath() + File.separator + fileSharedName + "_" + sdf.format(new Date()) + ".txt";
			try {
				twitterIDPW = new PrintWriter(fileName, "UTF-8");
			} catch (FileNotFoundException e) {
				System.out.println("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				System.out.println("CANNOT OPEN FILE: " + fileName + " - Exception: " + e.getMessage());
				e.printStackTrace();
			}

			System.out.println("Storing tweets to: '" + fileName + "'");

			Integer accountCredentialsId = 0;

			List<String> tweetToStore = new ArrayList<String>();
			if(tweetIDset != null && tweetIDset.size() > 0) {
				for(String entry : tweetIDset) {
					if(entry != null && !entry.equals("")) {

						try {
							Twitter currentAccountToQuery =  twitterList.get(accountCredentialsId);
							logger.debug("Queried account: "  + accountCredentialsId);
							accountCredentialsId = (accountCredentialsId + 1) % consumerKey.size();

							Status status = currentAccountToQuery.showStatus(Long.valueOf(entry));
							
							if(status != null && status.getCreatedAt() != null) {
								String msg = DataObjectFactory.getRawJSON(status);
								if(msg == null) {
									System.out.println("ERROR > INVALID TWEET RETRIEVED!");
									continue;
								}
								tweetToStore.add(msg);
								System.out.println(" - Retrieved tweet with ID: " + entry + " waiting " + sleepTimeInMilliseconds + " milliseconds...");
							}

							Thread.currentThread().sleep(sleepTimeInMilliseconds);

						}
						catch (TwitterException te) {
							System.out.println("ERROR: Couldn't connect: " + te.getMessage());
							te.printStackTrace();
						}; 

					}
				}
			}
			
			// Store to file
			System.out.println("\nStoring " + tweetToStore.size() + " tweets in " + outpuTweetFormat + " format:");
			int storageCount = 0;
			for(String tweet : tweetToStore)  {
				
				if(tweet != null) {
					if(outpuTweetFormat.equals("tab")) {
						Status status = DataObjectFactory.createStatus(tweet);
						twitterIDPW.write(status.getId() + "\t" + ((status.getText() != null) ? status.getText().replace("\n", " ") : "") + "\n");
						storageCount++;
					}
					else {
						twitterIDPW.write(tweet + "\n");
						storageCount++;
					}
				}
				
			}
			
			twitterIDPW.flush();
			
			System.out.println(storageCount + " tweet stored to file: " + fileName);
			System.out.println("Execution terminated.");
			
		} catch (Exception e) {
			System.out.println("Error generic: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {

		if(args == null || args.length == 0 || args[0] == null || args[0].trim().equals("")) {
			System.out.println("Please, specify the full local path to the crawler ptoperty file as first argument!");
			return;
		}

		File crawlerPropertyFile = new File(args[0].trim());
		if(crawlerPropertyFile == null || !crawlerPropertyFile.exists() || !crawlerPropertyFile.isFile()) {
			System.out.println("The path of the crawler ptoperty file (first argument) is wrongly specified > PATH: '" + ((args[0] != null) ? args[0].trim() : "NULL") + "'");
			return;
		}


		// Load information from property file
		PropertyManager propManager = new PropertyManager();
		propManager.setPropertyFilePath(args[0].trim());

		// Load credential objects
		System.out.println("Loading twitter API credentials from the property file at '" + args[0].trim() + "':");
		List<CredentialObject> credentialObjList = PropertyUtil.loadCredentialObjects(propManager);
		if(credentialObjList != null && credentialObjList.size() > 0) {
			for(CredentialObject credentialObj : credentialObjList) {
				if(credentialObj != null && credentialObj.isValid()) {
					consumerKey.add(credentialObj.getConsumerKey());
					consumerSecret.add(credentialObj.getConsumerSecret());
					token.add(credentialObj.getToken());
					tokenSecret.add(credentialObj.getTokenSecret());
				}
				else {
					System.out.println("      - ERROR > INVALID CREDENTIAL SET: " + ((credentialObj != null) ? credentialObj.toString() : "NULL OBJECT"));
				}
			}
		}

		// Load full path of tweetID file
		try {
			String tweetIDlistFilePath = propManager.getProperty(PropertyManager.RESTtweetIDlistFilePath);
			File tweetIDfile = new File(tweetIDlistFilePath);
			if(tweetIDfile == null || !tweetIDfile.exists() || !tweetIDfile.isFile()) {
				System.out.println("ERROR: Tweet ID input file path (property '" + PropertyManager.RESTtweetIDlistFilePath + "')"
						+ " wrongly specified > PATH: '" + ((tweetIDlistFilePath != null) ? tweetIDlistFilePath : "NULL") + "'");
				if(tweetIDfile != null && !tweetIDfile.exists()) {
					System.out.println("      The file does not exist!"); 
				}
				if(tweetIDfile != null && tweetIDfile.exists() && !tweetIDfile.isFile()) {
					System.out.println("      The path does not point to a valid file!"); 
				}
				return;
			}
			else {
				fullPathOfTweetIDfile = tweetIDlistFilePath;
			}
		} catch (Exception e) {
			System.out.println("ERROR: Tweet ID input file path (property '" + PropertyManager.RESTtweetIDlistFilePath + "')"
					+ " wrongly specified - exception: " + ((e.getMessage() != null) ? e.getMessage() : "NULL"));
			return;
		}

		// Load full path of output directory
		try {
			String outputDirectoryFilePath = propManager.getProperty(PropertyManager.RESTtweetIDfullPathOfOutputDir);
			File outputDirFile = new File(outputDirectoryFilePath);
			if(outputDirFile == null || !outputDirFile.exists() || !outputDirFile.isDirectory()) {
				System.out.println("ERROR: output directory full path (property '" + PropertyManager.RESTtweetIDfullPathOfOutputDir + "')"
						+ " wrongly specified > PATH: '" + ((outputDirectoryFilePath != null) ? outputDirectoryFilePath : "NULL") + "'");
				if(outputDirFile != null && !outputDirFile.exists()) {
					System.out.println("      The directory does not exist!"); 
				}
				if(outputDirFile != null && outputDirFile.exists() && !outputDirFile.isDirectory()) {
					System.out.println("      The path does not point to a valid directory!"); 
				}
				return;
			}
			else {
				outputDirPath = outputDirectoryFilePath;
			}
		} catch (Exception e) {
			System.out.println("ERROR: output directory full path (property '" + PropertyManager.RESTtweetIDfullPathOfOutputDir + "')"
					+ " wrongly specified - exception: " + ((e.getMessage() != null) ? e.getMessage() : "NULL"));
			return;
		}
		
		// Output format
		try {
			String outputFormat = propManager.getProperty(PropertyManager.RESTtweetIDoutputFormat);
			
			if(outputFormat != null && outputFormat.trim().toLowerCase().equals("json")) {
				outpuTweetFormat = "json";
			}
			else if(outputFormat != null && outputFormat.trim().toLowerCase().equals("tab")) {
				outpuTweetFormat = "tab";
			}
			else {
				outpuTweetFormat = "json";
				System.out.println("Impossible to read the '" + PropertyManager.RESTtweetIDoutputFormat + "' property - set to: " + outpuTweetFormat);
			}
			
		} catch (Exception e) {
			System.out.println("ERROR: output format (property '" + PropertyManager.RESTtweetIDoutputFormat + "') - exception: " + ((e.getMessage() != null) ? e.getMessage() : "NULL"));
			return;
		}


		// Loading tweet IDs from file
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(new File(fullPathOfTweetIDfile)), "UTF-8"));

			String str;
			while ((str = in.readLine()) != null) {
				if(!str.trim().equals("")) {
					tweetIDset.add(str.trim());
				}
			}

			in.close();
		}
		catch (Exception e) {
			System.out.println("Exception reading Tweet IDs from file: " +  e.getMessage() + " > PATH: '" + ((fullPathOfTweetIDfile != null) ? fullPathOfTweetIDfile : "NULL") + "'");
			return;
		}


		// Printing arguments:
		System.out.println("\n***************************************************************************************");
		System.out.println("******************** LOADED PARAMETERS ************************************************");
		System.out.println("   > Property file loaded from path: '" + ((args[0].trim() != null) ? args[0].trim() : "NULL") + "'");
		System.out.println("        PROPERTIES:");
		System.out.println("           - NUMBER OF TWITTER API CREDENTIALS: " + ((consumerKey != null) ? consumerKey.size() : "ERROR"));
		System.out.println("           - PATH OF LIST OF TWEET ID TO CRAWL: '" + ((fullPathOfTweetIDfile != null) ? fullPathOfTweetIDfile : "NULL") + "'");
		System.out.println("           - PATH OF CRAWLER OUTPUT FOLDER: '" + ((outputDirPath != null) ? outputDirPath : "NULL") + "'");
		System.out.println("           - OUTPUT FORMAT: '" + ((outpuTweetFormat != null) ? outpuTweetFormat : "NULL") + "'");
		System.out.println("   -");
		System.out.println("   NUMBER OF TWEET IDs / LINES READ FROM THE LIST: " + ((tweetIDset != null) ? tweetIDset.size() : "READING ERROR"));
		System.out.println("***************************************************************************************\n");		
		
		if(tweetIDset == null || tweetIDset.size() == 0) {
			System.out.println("Empty list of Tweet IDs to crawl > EXIT");
			return;
		}
		
		if(consumerKey == null || consumerKey.size() == 0) {
			System.out.println("Empty list of valid Twitter API credentials > EXIT");
			return;
		}
		

		System.out.println("-----------------------------------------------------------------------------------");
		System.out.println("YOU'RE GOING TO USE " + ((consumerKey != null) ? consumerKey.size() : "ERROR") + " TWITTER DEVELOPER CREDENTIAL(S).");
		System.out.println("INCREASE YOUR CREDENTIAL NUMBER IN THE CONFIGURATION FILE IF YOU NEED TO INCREASE CRAWLING SPEED");
		System.out.println("-----------------------------------------------------------------------------------\n");
		
		
		try {
			Thread.sleep(4000);
		} catch (InterruptedException e) {
			/* Do nothing */
		}

		startCrawling();
	}

}
