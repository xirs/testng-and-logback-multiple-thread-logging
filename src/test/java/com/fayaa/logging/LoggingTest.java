package com.fayaa.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;

public class LoggingTest {
    private Logger logger = LoggerFactory.getLogger(LoggingTest.class);

    @BeforeClass
    public void initLoggingTest() {
        System.out.println(System.getProperty("outputdir"));
        logger.info("initLoggingTest from thread " + Thread.currentThread().getId());
    }

    @AfterClass
    public void cleanupLoggingTest() {
        System.out.println(System.getProperty("outputdir"));
        logger.info("cleanupLoggingTest from thread " + Thread.currentThread().getId());
    }

    @Test (invocationCount=100, threadPoolSize=10)
    public void testFoo() {
        logger.info("logging from thread " + Thread.currentThread().getId());
    }
}

