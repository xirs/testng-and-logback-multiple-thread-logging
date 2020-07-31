/*
 * Multi-threaded testing log will mess up each other, it's hard to find out which long belongs to which test
 *
 * This class act as a logback Appender *and* testng reporter
 *
 *   - since it's logback appender, all thread will call the same append function
 *     then it write the log into different file based on the *current* thread id
 *
 *   - after all test, the testng reporter will be called, logs are merged here
 *     using reporter just to ensure it's called after all tests are done
 *
 *
 * Usage:
 *
 *  set your project's log4j.properties to include com.fayaa.testnglog.GroupedLoggingAppender as one of the logger
 *
 *  java org.testng.TestNG -reporter com.fayaa.testnglog.GroupedLoggingAppender your_testng.xml
 *
 *
 * Other: same mechanism could be applied to other test/log library, or even other language
 *
 */

package com.fayaa.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.FileAppender;
import org.testng.IReporter;
import org.testng.ISuite;
import org.testng.xml.XmlSuite;

/*
 * This appender logs groups test outputs by test method
 *  so they don't mess up each other even they runs in parallel.
 *  magic is done by output file into different file for different threads
 *  then merge them in the end
 *
 * Set org.testng.TestNG -reporter com.fayya.logging.GroupedLoggingAppender
 *    when you run the test, and make sure the outputdir exists
 *
 * if you don't set anything, by default the reporter does nothing
 *
 */

public class GroupedLoggingAppender extends FileAppender<ILoggingEvent> implements IReporter {
    private final ConcurrentHashMap<Long, BufferedWriter> tid2file = new ConcurrentHashMap<Long, BufferedWriter>();
    private static String outputFile;
    private static String outputDir;
    private final String ext = "thread.log";

    public GroupedLoggingAppender() {

    }

    @Override
    public void generateReport(List<XmlSuite> xmlSuites, List<ISuite> suites, String outputDirectory) {
        if (outputFile==null) {
            return;
        }
        System.out.println("Reporter getting called! " + outputFile);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // we don't do any report generation here, just clean up the log files
        mergeLogFiles();
    }

    private void mergeLogFiles() {
        try {
            outputDir = new File(outputFile).getParent();
            File file = new File(outputDir);
            File[] files = file.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().endsWith(ext);
                }
            });

            List<Path> paths = new ArrayList<Path>();
            for (File f : files) {
                Path path = f.toPath();
                paths.add(path);
            }
            Collections.sort(paths);
            Path pathAll = FileSystems.getDefault().getPath(outputFile);
            for (Path path : paths) {
                Files.write(pathAll, Files.readAllBytes(path), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
                Files.delete(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        if (this.getFile() != null) {
            outputFile = this.getFile();
            outputDir = new File(outputFile).getParent();
            try {
                File file = new File(outputDir);
                File[] files = file.listFiles(new FileFilter() {
                    @Override
                    public boolean accept(File pathname) {
                        return pathname.getName().endsWith(ext);
                    }
                });
                List<Path> paths = new ArrayList<Path>();
                for (File f : files) {
                    Path path = f.toPath();
                    paths.add(path);
                }
                Collections.sort(paths);
                for (Path path : paths) {
                    Files.delete(path);
                }
            } catch (IOException e) {
                e.printStackTrace();
                //throw new RuntimeException(e);
            }
        }
        super.start();
    }

    @Override
    protected void append(ILoggingEvent loggingevent) {
        // note that AppenderBase.doAppend will invoke this method only if
        // this appender was successfully started.
        if (this.getFile()==null) {
            return ;
        }
        try {
            long tid = Thread.currentThread().getId();
            Path path = FileSystems.getDefault().getPath(getFileNameFromThreadID(tid));
            BufferedWriter bw = Files.newBufferedWriter(path, Charset.forName("UTF-8"), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
            String content = ((PatternLayoutEncoder) this.getEncoder()).getLayout().doLayout(loggingevent);
            bw.write(content, 0, content.length());
            //bw.write("\n");
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
            //throw new RuntimeException(e);
        }
    }

    private String getFileNameFromThreadID(long tid) {
        return String.format("%s.thread_output_%04d.%s", this.getFile(), tid, ext);
    }


}

