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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.backingdata.twitter.crawler.streaming.model.Bbox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.twitter.hbc.ClientBuilder;
import com.twitter.hbc.core.Constants;
import com.twitter.hbc.core.endpoint.Location;
import com.twitter.hbc.core.endpoint.Location.Coordinate;
import com.twitter.hbc.core.endpoint.StatusesFilterEndpoint;
import com.twitter.hbc.core.processor.StringDelimitedProcessor;
import com.twitter.hbc.httpclient.BasicClient;
import com.twitter.hbc.httpclient.auth.Authentication;
import com.twitter.hbc.httpclient.auth.OAuth1;
import com.twitter.hbc.twitter4j.handler.StatusStreamHandler;
import com.twitter.hbc.twitter4j.message.DisconnectMessage;
import com.twitter.hbc.twitter4j.message.StallWarningMessage;

import twitter4j.GeoLocation;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterException;
import twitter4j.json.DataObjectFactory;


/**
 * STREAMING Cralwer of Twitter - retrieves all tweets with geocoordinates / issued from a venue intersecting a set of bounding boxes.<br/>
 * 
 * Only tweets issued by venues with an area < 100kmq are considered.<br/>
 * 
 * Have a look at the main code to identify how to define the set of bounding boxes to crawl and the respective geo-coordinates as well as the Twitter credentials.<br/><br/>
 * 
 * For each bounding box, for every 20,000 tweet retrieved, a .txt file is created containing one JSON Tweet 
 * per line (https://dev.twitter.com/overview/api/tweets). The name of such file has the following format 
 * (tweets are stored in memory and then when 20,000 tweets are gathered all the bath of JSON is stored to the file):<br/>
 * 
 * *SHARED_STRING*_*SEQUENCE_ID*_*BOUNDING_BOX_NAME*_from_*CRAWLING_START_DATE*.txt<br/><br/>
 * Log files with crawling errors and messages are also created.<br/>
 * 
 * 
 * @author Francesco Ronzano
 *
 */
public class TwitterSTREAMBboxCrawler {
	
	private static Logger logger = LoggerFactory.getLogger(TwitterSTREAMBboxCrawler.class.getName());

	private String fileSharedName = "twitter_STOPes_bbox_v1";

	// Authentication
	public List<String> consumerKey = new ArrayList<String>();
	public List<String> consumerSecret = new ArrayList<String>();
	public List<String> token = new ArrayList<String>();
	public List<String> tokenSecret = new ArrayList<String>();

	// List of BBOX to crawl
	public Map<String, Bbox> trackBbox = new HashMap<String, Bbox>();

	// Blocking queue for tweets to process
	private BlockingQueue<String> queue = new LinkedBlockingQueue<String>(10000);

	// Storage parameters
	private static File storageDir = null;
	private Map<String, PrintWriter> storageFileMap = new HashMap<String, PrintWriter>(); // Pointer to file to store tweets of each location
	private Map<String, Integer> storageFileCount = new HashMap<String, Integer>(); // Counter of tweets stored for each location
	private Map<String, Integer> storageFileId = new HashMap<String, Integer>();
	private Map<String, Long> storageFileLastTimestamp = new HashMap<String, Long>();

	private PrintWriter logFile = null;
	private Integer logFileId = 0;
	private Integer totalTweetStoredCount = 1;

	private Integer changeFileNumTweets = 20000;
	private Integer changeLogFileNumTweets = 80000;
	private Long storeMaxOneTweetEveryXseconds = -5l;

	// Date formatter
	private SimpleDateFormat sdf = new SimpleDateFormat("dd_M_yyyy__hh_mm_ss");
	
	// Appo vars
	private Map<String, List<String>> storageFileTweetList = new HashMap<String, List<String>>(); // Pointer to String list to store tweets of each location
	private List<String> logFileList = new ArrayList<String>();
	
	public synchronized void checkLogAndSotrageFiles() {

		// Init maps
		if(storageFileMap.size() == 0) {
			for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
				String key = entry.getKey();
				storageFileMap.put(key, null);
			}
		}
		if(storageFileTweetList.size() == 0) {
			for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
				String key = entry.getKey();
				storageFileTweetList.put(key, new ArrayList<String>());
			}
		}
		if(storageFileCount.size() == 0) {
			for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
				String key = entry.getKey();
				storageFileCount.put(key, 1);
			}
		}
		if(storageFileLastTimestamp.size() == 0) {
			for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
				String key = entry.getKey();
				storageFileLastTimestamp.put(key, 0l);
			}
		}
		if(storageFileId.size() == 0) {
			for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
				String key = entry.getKey();
				storageFileId.put(key, 0);
			}
		}


		// If files are opened and the changeFileNumTweets is reached, these files are closed
		for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
			String locationString = entry.getKey();
			if( storageFileMap.containsKey(locationString) && storageFileCount.containsKey(locationString) && storageFileId.containsKey(locationString) ) {

				if( storageFileMap.get(locationString) != null && ((storageFileCount.get(locationString) % this.changeFileNumTweets) == 0)) {
					
					// Store in log file all messages in logFileList
					for(String storageFileMessage : storageFileTweetList.get(locationString)) {
						if(storageFileMessage != null && storageFileMessage.trim().length() > 0) {
							storageFileMap.get(locationString).write(storageFileMessage + "\n");
						}
					}
					
					storageFileMap.get(locationString).flush();
					storageFileMap.get(locationString).close();
					storageFileMap.put(locationString, null);

					// Increase the number of storage count to not generate other files if the tweet is not stored
					Integer storageCountAppo = storageFileCount.get(locationString);
					storageFileCount.put(locationString, ++storageCountAppo);
					
					storageFileTweetList.put(locationString, new ArrayList<String>());
				}
			}			
		}

		if(logFile != null && ( (totalTweetStoredCount % this.changeLogFileNumTweets) == 0) ) {
			// Store in log file all messages in logFileList
			for(String logFileMessage : logFileList) {
				if(logFileMessage != null && logFileMessage.trim().length() > 0) {
					logFile.write(logFileMessage);
				}
			}
			
			logFile.flush();
			logFile.close();
			logFile = null;

			// Increase the number of storage count to not generate other log files if the tweet is not stored
			totalTweetStoredCount++;
		}


		// Storage and log - open new files if null
		for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
			String locationString = entry.getKey();
			if( storageFileMap.containsKey(locationString) && storageFileCount.containsKey(locationString) && storageFileId.containsKey(locationString) ) {

				if(storageFileMap.get(locationString) == null) {
					String fileName = storageDir.getAbsolutePath() + File.separator + fileSharedName + "_" + ( storageFileId.get(locationString) ) + "_" + locationString + "_from_" + sdf.format(new Date()) + ".txt";
					Integer fileId = storageFileId.get(locationString);
					storageFileId.put(locationString, fileId + 1);
					try {
						storageFileMap.put(locationString, new PrintWriter(fileName, "UTF-8"));
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

		if(logFile == null) {
			String fileName = storageDir.getAbsolutePath() + File.separator + "LOG_" + fileSharedName + "_" + ( logFileId++ ) + "_from_" + sdf.format(new Date()) + ".txt";
			try {
				logFile = new PrintWriter(fileName, "UTF-8");
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
				logFileList.add((new Date()).toString() + " - TRACK LIMITATION NOTICE: " + arg0 + "\n");
				logger.info((new Date()).toString() + " - TRACK LIMITATION NOTICE: " + arg0 + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}

		public void onException(Exception arg0) {
			checkLogAndSotrageFiles();

			try {
				logFileList.add((new Date()).toString() + " - EXCEPTION: " + arg0.getMessage() + "\n");
				logger.info((new Date()).toString() + " - EXCEPTION: " + arg0.getMessage() + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}

		}

		public void onDisconnectMessage(DisconnectMessage message) {
			checkLogAndSotrageFiles();

			try {
				logFileList.add((new Date()).toString() + " - DISCONNECT: CODE: " + message.getDisconnectCode() + ", REASON: " + message.getDisconnectReason() + "\n");
				logger.info((new Date()).toString() + " - DISCONNECT: CODE: " + message.getDisconnectCode() + ", REASON: " + message.getDisconnectReason() + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}

		public void onStallWarningMessage(StallWarningMessage warning) {
			checkLogAndSotrageFiles();

			try {
				logFileList.add((new Date()).toString() + " - STALL WARNING: CODE: " + warning.hashCode() + ", REASON: " + warning.getMessage() + ", PERCENT FULL: " + warning.getPercentFull() + "\n");
				logger.info((new Date()).toString() + " - STALL WARNING: CODE: " + warning.hashCode() + ", REASON: " + warning.getMessage() + ", PERCENT FULL: " + warning.getPercentFull() + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}

		public void onUnknownMessageType(String msg) {
			checkLogAndSotrageFiles();

			try {
				logFileList.add((new Date()).toString() + " - UNKNOWN MESSAGE: " + msg + "\n");
				logger.info((new Date()).toString() + " - UNKNOWN MESSAGE: " + msg + "\n");
			} catch (Exception e) {
				System.out.println("Exception LOG FILE");
				e.printStackTrace();
			}
		}
	};

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

	@SuppressWarnings("static-access")
	public void startCrawling() {
		StatusesFilterEndpoint endpoint = new StatusesFilterEndpoint();

		List<Location> locList = new ArrayList<Location>();
		for(Map.Entry<String, Bbox> entry : this.trackBbox.entrySet()) {
			Bbox bb = entry.getValue();
			if(bb != null && bb.getLngSW() != 0d && bb.getLatSW() != 0d && bb.getLngNE() != 0d && bb.getLatNE() != 0d) {
				Coordinate swCoord = new Coordinate(bb.getLngSW(), bb.getLatSW());
				Coordinate neCoord = new Coordinate(bb.getLngNE(), bb.getLatNE());
				Location loc = new Location(swCoord, neCoord);
				locList.add(loc);
			}
		}

		endpoint.locations(locList);

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
							// Check if the tweets has coordinates
							if(receivedStatus != null && receivedStatus.getGeoLocation() != null) {
								double latitudeVal = receivedStatus.getGeoLocation().getLatitude();
								double longitudeVal = receivedStatus.getGeoLocation().getLongitude();

								if(-90d <= latitudeVal && latitudeVal <= 90d && -180d <= longitudeVal && longitudeVal <= 180d) {

									boolean inOneBbox = false;

									for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
										String locationString = entry.getKey();
										Bbox bb = entry.getValue();
										if(bb != null && bb.isInBbox(longitudeVal, latitudeVal)) {
											for(Map.Entry<String, PrintWriter> entry_int : storageFileMap.entrySet()) {
												if(entry_int.getKey().equals(locationString)) {
													// Store to list
													storageFileTweetList.get(entry_int.getKey()).add(msg);
													// Store to file
													// entry_int.getValue().write(msg);
													// entry_int.getValue().flush();
													
													inOneBbox = true;
													totalTweetStoredCount++;

													if(totalTweetStoredCount % 100 == 0) {
														printMemoryStatus();
														System.gc();
														System.out.println("GARBAGE COLLECTOR CALLED: ");
														printMemoryStatus();
													}

													for(Map.Entry<String, Integer> entry_int_int : storageFileCount.entrySet()) {
														if(entry_int_int.getKey().equals(locationString)) {
															
															// Management of storeMaxOneTweetEveryXseconds
															if(storeMaxOneTweetEveryXseconds != null && storeMaxOneTweetEveryXseconds > 0l) {
																Long lastTimestamp = storageFileLastTimestamp.get(locationString);
																if(lastTimestamp != null && (System.currentTimeMillis() - lastTimestamp) < (storeMaxOneTweetEveryXseconds * 1000l)) {
																	System.out.println("SKIPPED TWEET FOR LOCATION: " + locationString + " - only  " + (System.currentTimeMillis() - lastTimestamp) + " ms (< " + storeMaxOneTweetEveryXseconds + "s)"
																			+ "since last tweet received - queue free places: " + queue.remainingCapacity());
																	continue;
																}
																else {
																	storageFileLastTimestamp.put(locationString, 0l);
																}
															}
															storageFileLastTimestamp.put(locationString, System.currentTimeMillis());
															
															
															Integer storageCount = entry_int_int.getValue();
															storageFileCount.put(locationString, storageCount + 1);
															System.out.println("SAVE (lat, lng) GEOLOCATED TWEET: " + locationString + " tot: " + (storageCount + 1) + " - queue free places: " + queue.remainingCapacity());
														}
													}
												}
											}							
										}
									}

									if(!inOneBbox) {
										logFileList.add("TWEET outside bboxes (lng: " + longitudeVal + ", lat: " + latitudeVal + ")\n");
										// System.out.println("WARN: TWEET outside bboxes (lng: " + longitudeVal + ", lat: " + latitudeVal + ") - queue free places: " + queue.remainingCapacity());
									}
								}
								else {
									logFileList.add("TWEET WRONG GEOCOORDINATES\n");
									// System.out.println("WARN: RECEIVED WRONG GEOCOORDINATES - queue free places: " + queue.remainingCapacity());
								}

							}
							else {
								// If there is a place intersecting
								if(receivedStatus != null && receivedStatus.getPlace() != null && receivedStatus.getPlace().getBoundingBoxCoordinates() != null) {
									GeoLocation[][] placeCoordinates = receivedStatus.getPlace().getBoundingBoxCoordinates();
									if(placeCoordinates != null && placeCoordinates.length > 0) {

										if(placeCoordinates.length >= 1) {
											GeoLocation[] plCoord = placeCoordinates[0];

											if(plCoord != null && plCoord.length == 4) {
												double minLat = Double.MAX_VALUE;
												double maxLat = Double.MIN_VALUE;
												double minLon= Double.MAX_VALUE;
												double maxLon = Double.MIN_VALUE;
												for(int j = 0; j < plCoord.length; j++) {
													GeoLocation plCoordInt = placeCoordinates[0][j];
													double latitude = plCoordInt.getLatitude();
													double longitude = plCoordInt.getLongitude();
													if(latitude < minLat) {
														minLat = latitude;
													}
													if(latitude > maxLat) {
														maxLat = latitude;
													}
													if(longitude < minLon) {
														minLon = longitude;
													}
													if(longitude > maxLon) {
														maxLon = longitude;
													}

													// System.out.println("       > POINT: (" + plCoordInt.getLatitude() + ", " + plCoordInt.getLongitude() + ")");
												}

												if(minLat != Double.MAX_VALUE && minLon != Double.MAX_VALUE && maxLat != Double.MIN_VALUE && maxLon != Double.MIN_VALUE) {
													double latitudeDiff = 0d;
													double longitudeDiff = 0d;

													if(maxLat >= 0d && minLat >= 0d) {
														latitudeDiff = maxLat - minLat;
													}
													else if(maxLat <= 0d && minLat <= 0d) {
														latitudeDiff = (-minLat) - (-maxLat);
													}
													else if(maxLat >= 0d && minLat <= 0d) {
														latitudeDiff = maxLat + (-minLat);
													}	


													if(maxLon >= 0d && minLon >= 0d) {
														longitudeDiff = maxLon - minLon;
													}
													else if(maxLon <= 0d && minLon <= 0d) {
														longitudeDiff = (-minLon) - (-maxLon);
													}
													else if(maxLon >= 0d && minLon <= 0d) {
														longitudeDiff = maxLon + (-minLon);
													}

													if(latitudeDiff > 0d && longitudeDiff > 0d) {
														latitudeDiff = (latitudeDiff / Bbox.constAreaKm);
														longitudeDiff = (longitudeDiff / Bbox.constAreaKm);
														double areaPlace = latitudeDiff * longitudeDiff;

														double areaInKmQuad = areaPlace;
														double maxAreaInKmQuad = 100d;
														
														if(areaInKmQuad <= 100d ) {
															
															// Check if one corner of the place BBOX is inside one of the BBOXes to crawl
															boolean inOneBbox = false;

															for(Map.Entry<String, Bbox> entry : trackBbox.entrySet()) {
																String locationString = entry.getKey();
																Bbox bb = entry.getValue();

																if(bb != null && 
																		(bb.isInBbox(minLon, minLat) || bb.isInBbox(minLon, maxLat) || bb.isInBbox(maxLon, minLat) || bb.isInBbox(maxLon, maxLat)) ) {
																	for(Map.Entry<String, PrintWriter> entry_int : storageFileMap.entrySet()) {
																		if(entry_int.getKey().equals(locationString)) {
																			// Store to list
																			storageFileTweetList.get(entry_int.getKey()).add(msg);
																			// Store to file
																			// entry_int.getValue().write(msg);
																			// entry_int.getValue().flush();
																			inOneBbox = true;
																			totalTweetStoredCount++;

																			if(totalTweetStoredCount % 100 == 0) {
																				printMemoryStatus();
																				System.gc();
																				System.out.println("GARBAGE COLLECTOR CALLED: ");
																				printMemoryStatus();
																			}

																			for(Map.Entry<String, Integer> entry_int_int : storageFileCount.entrySet()) {
																				if(entry_int_int.getKey().equals(locationString)) {
																					Integer storageCount = entry_int_int.getValue();
																					storageFileCount.put(locationString, storageCount + 1);
																					System.out.println("SAVE PLACE GEOLOCATED TWEET: " + locationString + " tot: " + (storageCount + 1) + " PLACE: " + ((receivedStatus.getPlace().getName() != null) ? receivedStatus.getPlace().getName() : "NULL") + " - queue free places: " + queue.remainingCapacity());
																				}
																			}
																		}
																	}							
																}
															}

															if(!inOneBbox) {
																logFileList.add("DISCARTED INTERSECTING PLACE: " + ((receivedStatus.getPlace().getName() != null) ? receivedStatus.getPlace().getName() : "NULL") +
																		"\n WITH AREA: " + areaInKmQuad + " Km2 (" + latitudeDiff + " * " + longitudeDiff + ") > NOT INTERSECTING BBOX.\n");
															}
															
														}
														else {
															logFileList.add("IGNORED INTERSECTING PLACE: " + ((receivedStatus.getPlace().getName() != null) ? receivedStatus.getPlace().getName() : "NULL") +
																	" WITH AREA: " + areaInKmQuad + " Km2 (" + latitudeDiff + " * " + longitudeDiff + ") > " + maxAreaInKmQuad + " km2 max area.\n");
														}
													}
												}
											}
											else {
												logFileList.add("PLACE WITH COORDINATE BBOX FIRST LINE OF LENGTH != 4 (" + plCoord.length + ") > " +
														"name: " + ((receivedStatus.getPlace().getName() != null) ? receivedStatus.getPlace().getName() : "NULL") +
														" - id: " + ((receivedStatus.getPlace().getId() != null) ? receivedStatus.getPlace().getId() : "NULL") +
														"\n");
											}

										}
										else {
											logFileList.add("PLACE WITH COORDINATE BBOX EMPTY > " +
													"name: " + ((receivedStatus.getPlace().getName() != null) ? receivedStatus.getPlace().getName() : "NULL") +
													" - id: " + ((receivedStatus.getPlace().getId() != null) ? receivedStatus.getPlace().getId() : "NULL") +
													"\n");
										}

									}
									else {
										logFileList.add("PLACE WITHOUT COORDINATES > " +
												"name: " + ((receivedStatus.getPlace().getName() != null) ? receivedStatus.getPlace().getName() : "NULL") +
												" - id: " + ((receivedStatus.getPlace().getId() != null) ? receivedStatus.getPlace().getId() : "NULL") +
												"\n");
									}
								}
								else {
									logFileList.add("TWEET NOT GEOLOCATED\n");
								}
							}
						} catch (Exception e) {
							logFileList.add("Exception " + ((e != null && e.getMessage() != null) ? e.getMessage() : "---") + "\n");
							System.out.println("Exception " + ((e != null && e.getMessage() != null) ? e.getMessage() : "---") + "\n");
							e.printStackTrace();
						}

					}
					else {
						logFileList.add(sdf.format(new Date()) + " - ERROR CODE: " + msg + "\n");
						logger.info(sdf.format(new Date()) + " - ERROR CODE: " + msg);
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


		TwitterSTREAMBboxCrawler crawler = new TwitterSTREAMBboxCrawler();
		
		// Add Twitter credentials
		crawler.consumerKey.add("PUT_CONSUMER_KEY");
		crawler.consumerSecret.add("PUT_CONSUMER_SECRET");
		crawler.token.add("PUT_ACCESS_TOKEN");
		crawler.tokenSecret.add("PUT_ACCESS_KEY");
		
		// Set bounding boxes to retrieve tweets by providing a name and the coordinates (lngSW, latSW, lngNE, latNE)
		crawler.trackBbox.put("BoundingBOX_name_1", new Bbox(2.1378493309020996d,41.386549170992275d,2.139415740966797d,41.387885371627185));
		crawler.trackBbox.put("BoundingBOX_name_2", new Bbox(2.1878493309020996d,42.386549170992275d,2.189415740966797d,42.387885371627185));
		
		// Print bounding boxe
		logger.info("AREAS:");
		for(Map.Entry<String, Bbox> entry : crawler.trackBbox.entrySet()) {
			logger.info("   > " + entry.getKey() + " --> Area: " + entry.getValue().getArea());
		}

		crawler.startCrawling();
	}

}
