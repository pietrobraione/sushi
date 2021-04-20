package sushi.execution.evosuite;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TestDetector extends Thread {
    private static final Pattern PATTERN_EMITTED_TEST = Pattern.compile("^.*\\* EMITTED TEST CASE: .*EvoSuiteWrapper_(\\d+)_(\\d+), \\w+\\z");
    
    private final int taskNumber;
	private final BufferedReader evosuiteReader;
	private final BufferedWriter logFileWriter;
	private final TestGenerationNotifier testEmissionNotifier;
	
	public TestDetector(int taskNumber, InputStream evosuiteInputStream, Path logFilePath, TestGenerationNotifier testEmissionNotifier) throws IOException {
		this.taskNumber = taskNumber;
		this.evosuiteReader = new BufferedReader(new InputStreamReader(evosuiteInputStream));
		this.logFileWriter = Files.newBufferedWriter(logFilePath);
		this.testEmissionNotifier = testEmissionNotifier;
	}
	
	@Override
	public void run() {
        //reads/copies the standard input and detects the generated tests
        try {
            String line;
            while ((line = this.evosuiteReader.readLine()) != null) {
                if (Thread.interrupted()) {
                    break;
                }
                
                //copies the line to the EvoSuite log file
                this.logFileWriter.write(line);
                this.logFileWriter.newLine();
                
                //check if the read line reports the emission of a test case
                //and in the positive case alerts the coordinator
                final Matcher matcherEmittedTest = PATTERN_EMITTED_TEST.matcher(line);
                if (matcherEmittedTest.matches()) {
                    final int methodNumber = Integer.parseInt(matcherEmittedTest.group(1));
                    final int localTraceNumber = Integer.parseInt(matcherEmittedTest.group(2));
                    this.testEmissionNotifier.onTestGenerated(this.taskNumber, methodNumber, localTraceNumber);
                }
            }
        } catch (IOException e) {
        	//nothing to do, an abrupt closure of the stream is possible
        } finally {
            try {
                this.logFileWriter.close();
            } catch (IOException e) {
            	//nothing to do, an abrupt closure of the stream is possible
            }
        }
	}
}
