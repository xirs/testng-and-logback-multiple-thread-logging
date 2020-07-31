testng-and-logback-multiple-thread-logging
========================================
This Project is coming from https://github.com/realfun/testng-and-log4j-multiple-thread-logging, but it use log4j.

A logback example is provided here (to be respected for original author, README/package name etc is reused as much as possible) 

*NOTICE*: this project requires Java 1.7+ and Maven

Same mechanism could apply for other test framework even other language.

Issue To Resolve
----------------------------------------

Using logback for testng logging is quite common, for tests running in parallel in testng, the log outputs to the console will be messed up with each other. This makes failure investigation a little bit harder.

Sure you can specify different logger for different class, or even each per test method, but it doesn't solve the problem when a 3rd-party lib that is logging in its own logback logger, and most-likely they don't design interface that allow you to pass your test class's "logger" instance for each function call.


My Solution
----------------------------------------

*Main idea*: the logging in each thread is *NOT* messed up!

So, we can log based on current thread id and merge them together after. To do this, I created a class called GroupedLoggingAppender which extends AppenderSkeleton and implements IReporter:
    * inside append function(from AppenderSkeleton), write to differnt file for different thread
    * inside generateReport(from IReporter) function, merge all the files

Usage
----------------------------------------

Have a try:

    * Checkout this project then do `mvn clean install`
    * you will find 100 lines of output which is messed up on the console as it's from 10 parallel threads
    * Then `cat log/fayaa.threadgroup.log`
    * you will see 100 lines with output grouped by thread-id


Detail setup, refer to files in this project:

    * set your logback.xml, add GroupedLoggingAppender
    * if you use testng.xml, set listner similar to testng/testng.xml in this project, use command like this:
         java -Dlogback.configurationFile=src/main/resources/logback.xml -cp xxx org.testng.TestNG testng/testng.xml 
    * if you use command line TestNG, use command like this:
         java -Dlogback.configurationFile=src/main/resources/logback.xml -cp xxx org.testng.TestNG -reporter com.fayaa.logging.GroupedLoggingAppender
    * if you use maven-surefire, set listner(yes, not reporter) and system properties similar to pom.xml in this project



