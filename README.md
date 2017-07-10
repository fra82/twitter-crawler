# twitter-crawler
Configurable Twitter Crawlers (Java based) useful to gather data by both the REST and STREAMING endpoints and based on [hbc-twitter4j](https://mvnrepository.com/artifact/com.twitter/hbc-twitter4j "hbc-twitter4j").

Core features:
 * timeline, keyword and user based crawling (REST endpoints) / keyword, user and bounding box based crawling (STREAMING endpoint)
 * complete configuration of crawlers by means of a property file (download the Jar, set up a property file and start crawling,as explained in [Set up Twitter crawlers](#setUpTwitterCralwers));
 * support to manage pools of [Twitter credentials ](https://apps.twitter.com/)
 * storage of results in text files (UTF-8) containing one tweet per line in [JSON format](https://dev.twitter.com/overview/api/tweets, "JSON format of tweet"). For instance for the keyword crawler stores the tweets retrieved in one file per keyword per 20,000 tweets;
 * storage of log files.


The following Twitter Crawlers are availabe:
 * **Account Timeline Crawler** (REST endpoint): exploit a pool of Twitter credentials to gather the latest tweets from the timelines of a collection of Twitter accounts (org.backingdata.twitter.crawler.rest.TwitterRESTAccountTimelineCrawler);
 * **Keyword Crawler** (REST endpoint) : exploit a pool of Twitter credentials to gather the latest tweets matching one or more keywords (hashtags, phrases) - (org.backingdata.twitter.crawler.rest.TwitterRESTKeywordSearchCrawler);
 * **ID list Crawler** (REST endpoint) : exploit a pool of Twitter credentials to gather the latest tweets matching one or more keywords (hashtags, phrases) - (org.backingdata.twitter.crawler.rest.TwitterRESTTweetIDlistCrawler);
 * **Keyword Filtered Crawler** (STREAMING endpoint): define a set of keywords (hashtags) to filter and store tweets from the [public stream (filter endpoint)](https://dev.twitter.com/streaming/reference/post/statuses/filter "public stream (filter endpoint)") - twitter can be optionally filtered also with respect to one or more languages (org.backingdata.twitter.crawler.streaming.TwitterSTREAMHashtagCrawler);
 * **Bounding box Filtered Crawler** (STREAMING endpoint): define a set of bounding boxes to filter and store tweets from the [public stream (filter endpoint)](https://dev.twitter.com/streaming/reference/post/statuses/filter "public stream (filter endpoint)") - (org.backingdata.twitter.crawler.streaming.TwitterSTREAMBboxCrawler).

Each one of these crawlers can be **set up in few simple steps by directly downloading and executing a JAR file** as explained in the next paragraph [Set up Twitter crawlers](#setUpTwitterCralwers); the configuration parameters of each crawler (Twitter credentials' pool, list of users, keywords, bounding boxes, etx.) can be specified by means of a **property file** (referred to as crawler.properties).

When using the Bounding box Filtered Crawler, you can rely on [this web interface](http://penggalian.org/bbox/ "bounding box Web interface") to easily define the coordinates of each bounding box.

The code implementig each Twitter Crawler is commented into details.

<a name="setUpTwitterCralwers"></a>
## Set up Twitter crawlers

In order to execute one of the previous crawlers you shuld carry out the following steps:

1. Download the latest JAR of the [twitter-crawler library](http://backingdata.org/twitter-crawler/0.3/twitter-crawler-0.3-bin.zip "twitter-crawler library") (currently in its version 3.0). Alternatively you can directly clone the code from GitHub and build the library by maven;
2. After downloading the library from [twitter-crawler library](http://backingdata.org/twitter-crawler/0.3/twitter-crawler-0.3-bin.zip "twitter-crawler library"), extract the file twitter-crawler-0.3-bin.zip;
3. Enter the folder 'twitter-crawler-0.3-bin' that contains the extracted library files and execute the following command:
```
java -cp twitter-crawler-0.3.jar FULLY_QUALIFIED_NAME_OF_CRAWLER_CLASS /full/local/path/to/crawler.properties
```

Depending on the crawler you are goning to use, the FULLY_QUALIFIED_NAME_OF_CRAWLER_CLASS is:
 * **Account Timeline Crawler** (REST endpoint): 
 ```org.backingdata.twitter.crawler.rest.TwitterRESTAccountTimelineCrawler```;
 * **Keyword Crawler** (REST endpoint): ```org.backingdata.twitter.crawler.rest.TwitterRESTKeywordSearchCrawler```;
 * **ID list Crawler** (REST endpoint): ```org.backingdata.twitter.crawler.rest.TwitterRESTTweetIDlistCrawler```;
 * **Keyword Filtered Crawler** (STREAMING endpoint): ```org.backingdata.twitter.crawler.streaming.TwitterSTREAMHashtagCrawler```;
 * **Bounding box Filtered Crawler** (STREAMING endpoint): ```org.backingdata.twitter.crawler.streaming.TwitterSTREAMBboxCrawler```.

For each one of the previous crawlers, all the crawling parameters (Twitter credentials' pool, list of users, keywords, bounding boxes, etx.) can be specified by means of the crawler.property file (```/full/local/path/to/crawler.properties```). In what follows, for each Twitter crawler, the format of the related configuration file si described.

### Configuration file format of **Account Timeline Crawler** (REST endpoint)
Example of the configuration file of an Account Timeline Crawler (create a crawler.properties file and copy&paste this content, then customize it by specifying your crawler configuration parameters):
```
# Pool of credentials to use to crawl (each pool is specified by four properties ending with
# the same number: integer from 1 to 150)
# Credentials of the pool are used in a round robin way.
#    - for streaming crawlers it is needed to specify only one pool of credentials
#    - for rest crawler, more credential pools are specified, faster is the crawling process
# IMPORTANT: it is possible to specify up to 150 credentials
consumerKey_1=PUT_YOUR_CONSUMER_KEY_NUM_1
consumerSecret_1=PUT_YOUR_CONSUMER_SECRET_NUM_1
token_1=PUT_YOUR_TOKEN_NUM_1
tokenSecret_1=PUT_YOUR_TOKEN_SECRET_NUM_1

consumerKey_2=PUT_YOUR_CONSUMER_KEY_NUM_2
consumerSecret_2=PUT_YOUR_CONSUMER_SECRET_NUM_2
token_2=PUT_YOUR_TOKEN_NUM_2
tokenSecret_2=PUT_YOUR_TOKEN_SECRET_NUM_2

####################################################################################
# REST Cralwer of Twitter - by account timeline
# HOWTO: a new file with all the most recent tweets of the timelines of the users specified is created
# Class: org.backingdata.twitter.crawler.rest.TwitterRESTAccountTimelineCrawler
#   - Full path of the txt file to read account IDs from (line format: ACCOUNT NAME <TAB> ACCOUNT_ID_LONG)
#   Example: 
#   bbc	808633423300624384
#   arxiv	808633423300624384
tweetTimeline.fullPathAccountIDs=
#   - Full path of the output folder to store crawling results (/full/path/to/output/dir)
tweetTimeline.fullOutputDirPath=
#   - Storage format: "json" to store one tweet per line as tweet JSON object or "tab" to store one tweet 
# per line as TWEET_ID<TAB>TWEET_TEXT
tweetTimeline.outputFormat=

```

Example of file with list of Twitter account to crawl (```tweetTimeline.fullPathAccountIDs```):
```
bbc	808633423300624384
arxiv	808633423300624384
```

 ### Configuration file format of  **Keyword Crawler** (REST endpoint)
Example of the configuration file of an Keyword Crawler (create a crawler.properties file and copy&paste this content, then customize it by specifying your crawler configuration parameters):
 
```
# Pool of credentials to use to crawl (each pool is specified by four properties ending with
# the same number: integer from 1 to 150)
# Credentials of the pool are used in a round robin way.
#    - for streaming crawlers it is needed to specify only one pool of credentials
#    - for rest crawler, more credential pools are specified, faster is the crawling process
# IMPORTANT: it is possible to specify up to 150 credentials
consumerKey_1=PUT_YOUR_CONSUMER_KEY_NUM_1
consumerSecret_1=PUT_YOUR_CONSUMER_SECRET_NUM_1
token_1=PUT_YOUR_TOKEN_NUM_1
tokenSecret_1=PUT_YOUR_TOKEN_SECRET_NUM_1

consumerKey_2=PUT_YOUR_CONSUMER_KEY_NUM_2
consumerSecret_2=PUT_YOUR_CONSUMER_SECRET_NUM_2
token_2=PUT_YOUR_TOKEN_NUM_2
tokenSecret_2=PUT_YOUR_TOKEN_SECRET_NUM_2

####################################################################################
# REST Cralwer of Twitter - by keyword(s)
# Class: org.backingdata.twitter.crawler.rest.TwitterRESTKeywordSearchCrawler
#   - Full path of the txt file to read terms from (one term ID per line)
tweetKeyword.fullPathKeywordList=
#   - Full path of the output folder to store crawling results 
tweetKeyword.fullOutputDirPath=
#   - Storage format: "json" to store one tweet per line as tweet JSON object or "tab" to store 
# one tweet per line as TWEET_ID<TAB>TWEET_TEXT
tweetID.outputFormat=
#   - If not empty, it is possible specify a language to retrieve only tweet of a specific language 
# (en, es, it, etc.) - if empty all tweet are retrieved, indipendently from their language
#    IMPORTANT: The language code may be formatted as ISO 639-1 alpha-2 (en), ISO 639-3 alpha-3 (msa), or ISO 639-1 alpha-2 combined with an ISO 3166-1 alpha-2 localization (zh-tw).
tweetID.languageFilter=

```

Example of file with list of keywords to crawl (```tweetKeyword.fullPathKeywordList```):
```
newcomer
madonna
```


 ### Configuration file format of **ID list Crawler** (REST endpoint)
Example of the configuration file of an ID list Crawler (create a crawler.properties file and copy&paste this content, then customize it by specifying your crawler configuration parameters):
 
```
# Pool of credentials to use to crawl (each pool is specified by four properties ending with
# the same number: integer from 1 to 150)
# Credentials of the pool are used in a round robin way.
#    - for streaming crawlers it is needed to specify only one pool of credentials
#    - for rest crawler, more credential pools are specified, faster is the crawling process
# IMPORTANT: it is possible to specify up to 150 credentials
consumerKey_1=PUT_YOUR_CONSUMER_KEY_NUM_1
consumerSecret_1=PUT_YOUR_CONSUMER_SECRET_NUM_1
token_1=PUT_YOUR_TOKEN_NUM_1
tokenSecret_1=PUT_YOUR_TOKEN_SECRET_NUM_1

consumerKey_2=PUT_YOUR_CONSUMER_KEY_NUM_2
consumerSecret_2=PUT_YOUR_CONSUMER_SECRET_NUM_2
token_2=PUT_YOUR_TOKEN_NUM_2
tokenSecret_2=PUT_YOUR_TOKEN_SECRET_NUM_2

####################################################################################
# REST Cralwer of Twitter - by list of tweet IDs (tweetID)
# Class: org.backingdata.twitter.crawler.rest.TwitterRESTTweetIDlistCrawler
#   - Full path of the txt file to read tweet IDs from (one tweet ID per line)
tweetID.fullPathTweetIDs=
#   - Full path of the output folder to store crawling results 
tweetID.fullOutputDirPath=
#   - Storage format: "json" to store one tweet per line as tweet JSON object or "tab" 
# to store one tweet per line as TWEET_ID<TAB>TWEET_TEXT
tweetID.outputFormat=json

```

Example of file with list of tweet IDs to crawl (```tweetID.fullPathTweetIDs```):
```
821388773837721602
884204044348207104
883176086162464769
```

 ### Configuration file format of **Keyword Filtered Crawler** (STREAMING endpoint)
Example of the configuration file of an Keyword Filtered Crawler (create a crawler.properties file and copy&paste this content, then customize it by specifying your crawler configuration parameters):
 
```
# Pool of credentials to use to crawl 
# For streaming crawlers it is needed to specify only one credential!!!
consumerKey_1=PUT_YOUR_CONSUMER_KEY_NUM_1
consumerSecret_1=PUT_YOUR_CONSUMER_SECRET_NUM_1
token_1=PUT_YOUR_TOKEN_NUM_1
tokenSecret_1=PUT_YOUR_TOKEN_SECRET_NUM_1

####################################################################################
# TREAMING Cralwer of Twitter - retrieves all tweets matching some specific keywords / users 
# and/or in some specific language.
# Class: org.backingdata.twitter.crawler.streaming.TwitterSTREAMHashtagCrawler
#   - Full path of the txt file to read terms from (one term ID per line)
tweetSTREAMkeyword.fullPathKeywordList=
#   - Full path of the txt file to read terms from (line format: ACCOUNT NAME <TAB> ACCOUNT_ID_LONG)
#   Example: 
#   bbc	662708106
#   arxiv	1149879325
tweetSTREAMkeyword.fullPathUserList=
#   - Full path of the output folder to store crawling results 
tweetSTREAMkeyword.fullOutputDirPath=
#   - Storage format: "json" to store one tweet per line as tweet JSON object or "tab" to store one tweet per line as TWEET_ID<TAB>TWEET_TEXT
tweetSTREAMkeyword.outputFormat=json
#   - If not empty, it is possible specify a comma separated language list to retrieve only tweet of a specific 
# language (en, es, it, etc.) - if empty all tweet are retrieved, indipendently from their language
#    IMPORTANT: The language code may be formatted as ISO 639-1 alpha-2 (en), ISO 639-3 alpha-3 (msa), or ISO 639-1 alpha-2 combined with an ISO 3166-1 alpha-2 localization (zh-tw).
tweetSTREAMkeyword.languageFilter=
#   - If not empty, it is possible specify a number of seconds - max number of tweet to store per seconds per user or keyword 
tweetSTREAMkeyword.limitByOneTweetPerXsec=

```
 
 
Example of file with list of keywords to crawl (```tweetSTREAMkeyword.fullPathKeywordList```):
```
newcomer
madonna
```

Example of file with list of Twitter account to crawl (```tweetSTREAMkeyword.fullPathUserList```):
```
bbc	808633423300624384
arxiv	808633423300624384
```
 
 
 ### Configuration file format of  **Bounding box Filtered Crawler** (STREAMING endpoint)
 Example of the configuration file of an Bounding box Filtered Crawler (create a crawler.properties file and copy&paste this content, then customize it by specifying your crawler configuration parameters):
 ```
# Pool of credentials to use to crawl 
# For streaming crawlers it is needed to specify only one credential!!!
consumerKey_1=PUT_YOUR_CONSUMER_KEY_NUM_1
consumerSecret_1=PUT_YOUR_CONSUMER_SECRET_NUM_1
token_1=PUT_YOUR_TOKEN_NUM_1
tokenSecret_1=PUT_YOUR_TOKEN_SECRET_NUM_1

####################################################################################
# STREAMING Cralwer of Twitter - retrieves all tweets generated inside a bounding box from a list and/or in some specific language.
# Class: org.backingdata.twitter.crawler.streaming.TwitterSTREAMBboxCrawler
#   - Full path of the txt file to read bounding boxes from (one bounding box per line - line format: BOUNDING BOX NAME <TAB> BOUNDING BOX COORDINATES)
#   BOUNDING BOX COORDINATES should be always a comma-separated list of doubles in the following order: lngSW, latSW, lngNE, latNE
#   Example: 
#   BBOX_NAME1	2.1390724182128906,41.363024324309784,2.1680831909179688,41.40565583808169
#   BBAX_NAME2	2.06268310546875,41.3532318743157,2.1028518676757812,41.37732380781499
tweetSTREAMbbox.fullPathBoundingBoxes=
#   - Full path of the output folder to store crawling results 
tweetSTREAMbbox.fullOutputDirPath=
#   - Storage format: "json" to store one tweet per line as tweet JSON object or "tab" to store one tweet per line as TWEET_ID<TAB>TWEET_TEXT
tweetSTREAMbbox.outputFormat=json
#   - If not empty, it is possible specify a comma separated language list to retrieve only tweet of a specific language (en, es, it, etc.) - if empty all tweet are retrieved, indipendently from their language
#    IMPORTANT: The language code may be formatted as ISO 639-1 alpha-2 (en), ISO 639-3 alpha-3 (msa), or ISO 639-1 alpha-2 combined with an ISO 3166-1 alpha-2 localization (zh-tw).
tweetSTREAMbbox.languageFilter=
 ```
 
 Example of file with list of bounding boxes to crawl (```tweetSTREAMkeyword.fullPathUserList```):
  ```
BBOX_NAME1	2.1390724182128906,41.363024324309784,2.1680831909179688,41.40565583808169
BBAX_NAME2	2.06268310546875,41.3532318743157,2.1028518676757812,41.37732380781499
BBAX_NAME3	2.17529296875,41.35310301646902,2.406005859375,41.6257084937525
   ```
 
