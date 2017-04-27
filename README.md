# twitter-crawler
Configurable Twitter Crawlers (Java based) useful to gather data by both the REST and STREAMING endpoints and based on [hbc-twitter4j](https://mvnrepository.com/artifact/com.twitter/hbc-twitter4j "hbc-twitter4j").  

The following Twitter Crawlers are availabe:
 * **Account Timeline Crawler** (REST endpoint): exploit a pool of Twitter credentials to gather the latest tweets from a the tilemines of a collection of Twitter accounts (org.backingdata.twitter.crawler.rest.TwitterRESTAccountTimelineCrawler);
 * **Keyword Crawler** (REST endpoint) : exploit a pool of Twitter credentials to gather the latest tweets matching one or more keywords (hashtags, phrases) - (org.backingdata.twitter.crawler.rest.TwitterRESTKeywordSearchCrawler);
 * **Keyword Filtered Crawler** (STREAMING endpoint): define a set of keywords (hashtags) to filter and store tweets from the [public stream (filter endpoint)](https://dev.twitter.com/streaming/reference/post/statuses/filter "public stream (filter endpoint)") - twitter can be optionally filtered also with respect to one or more languages (org.backingdata.twitter.crawler.streaming.TwitterSTREAMHashtagCrawler);
 * **Bounding box Filtered Crawler** (STREAMING endpoint): define a set of bounding boxes to filter and store tweets from the [public stream (filter endpoint)](https://dev.twitter.com/streaming/reference/post/statuses/filter "public stream (filter endpoint)") - (org.backingdata.twitter.crawler.streaming.TwitterSTREAMBboxCrawler).
 
 The code implementig each Twitter Crawler is commented.
 
 Each crawler properly manages:
 * storage of results in different '.txt' files (UTF-8) containing one tweet per line in [JSON format](https://dev.twitter.com/overview/api/tweets, "JSON format of tweet"). For instance for the keyword crawler, one file per keyword per 20,000 tweets retrieved is created;
 * storage of log files.
 
When using the Bounding box Filtered Crawler, you can rely on [this web interface](http://penggalian.org/bbox/, "bounding box Web interface") to easily define the coordinates of each bounding box.
 
 


