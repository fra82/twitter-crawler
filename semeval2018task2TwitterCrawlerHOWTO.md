[Multilingual Emoji Prediction (SemEval-2018 Task 2)](https://competitions.codalab.org/competitions/17344)

# Twitter Crawler

Ready-to-execute Twitter crawler that, given an input a list of Tweet IDs, relies on the [Twitter REST API](https://dev.twitter.com/rest/public) to retrieve the corresponding [Tweet objects in JSON format](https://dev.twitter.com/overview/api/tweets). 
The Twitter crawler is provided as a downloadable JAR archive. Even if not directly needed since we provide the executable version of the crawler, user can access the Java source code on [GitHub / twitter-crawler project](https://github.com/fra82/twitter-crawler).


## Crawler Overview

The Twitter crawler takes as input:
 * a **Tweet IDs text file** including a list of Tweet IDs to download, one ID per line. [Download an example of Tweet IDs text file](http://backingdata.org/semeval2018crawler/files/exampleOftweetIDsFile.list).  
 * a **configuration text file** useful to specify:
     * the pool of Twitter developer app credentials to use to crawl. One or more app credentials can be created by accessing the [Twitter App Console](https://apps.twitter.com/). You can specify up to 150 Twitter developer app credentials; more you provide, faster the crawling process will be
     * the full local path of the ***Tweet IDs text file*** to read tweet IDs from (one tweet ID per line) 
     * the full local path of the output folder to store crawling results 


 ## Before Executing the Crawler
 
The Twitter crawler has to be executed by Java 1.7 or newer. As a consequence, as a prerequisite for the exectuion of the Twitter crawler you need to have [Java](https://www.java.com/en/download/) installed.


 ## Crawler Execution
 
 1. Download the Twitter Crawler from the following download URL: http://backingdata.org/semeval2018crawler/files/semeval-2018-task2-twitter-crawler-0.4.tar.gz
 2. Decompress the tar.gz archive and open the folder 'semeval-2018-task2-twitter-crawler-0.4'
 3. Download the ***Tweet IDs text file*** from [Multilingual Emoji Prediction (SemEval-2018 Task 2)](https://competitions.codalab.org/competitions/17117)
 4. By means of a text editor, modify the ***configuration text file*** named 'crawler.properties' present in the folder 'semeval-2018-task2-twitter-crawler-0.4', by specifying:
     * one or a pool of Twitter developer app credentials (create your app credentials [here](https://apps.twitter.com/)). Each Twitter developer app credential is made of: a consumer key, a consumer secret, a token and a token secret. More Twitter developer app credentials you provide, faster the crawling process will be
     * the full path of the ***Tweet IDs text file*** as value of the property: *tweetID.fullPathTweetIDs*
     * the full path of the output folder to store crawling results as value of the property: *tweetID.fullOutputDirPath*
     * leave the property: *tweetID.fullOutputDirPath* equal to 'json'
 5. Start the crawler by executing the following command from the folder 'semeval-2018-task2-twitter-crawler-0.4':
```
java -cp './twitter-crawler-0.4.jar:./lib/*' org.backingdata.twitter.crawler.rest.TwitterRESTTweetIDlistCrawler /full/local/path/to/semeval-2018-task2-twitter-crawler-0.4/crawler.properties
```
 6. During the execution, several messages concerning the status of the crawling process will be displayed in the standard output
 7. Once terminated the cralwing process, the results will be stored in a text file (one [Tweet object in JSON format](https://dev.twitter.com/overview/api/tweets) per line) in the output folder specified by means of the ***configuration text file***
 
 ## Support</h2>
If you need any help, please [send us an email](mailto:francesco.ronzano@upf.edu?Subject=Semeval2018_Crawler_Issue). 





