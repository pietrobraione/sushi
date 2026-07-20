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

import sushi.exceptions.CoordinatorException;

/**
 * A {@link Thread} that listens for the output produced by 
 * an instance of EvoSuite, and when this produces a test
 * notifies a {@link TestGenerationListener}.
 * 
 * @author Pietro Braione
 */
public final class TestDetector extends Thread {
    private static final Pattern PATTERN_EMITTED_TEST = Pattern.compile("^.*\\* EMITTED TEST CASE .*_(\\d+)_(\\d+)_Test, .*$");
    
    private final int taskNumber;
	private final BufferedReader evosuiteReader;
	private final BufferedWriter logFileWriter;
	private final TestGenerationListener testGenerationListener;
	
	public TestDetector(int taskNumber, InputStream evosuiteInputStream, Path logFilePath, TestGenerationListener testGenerationListener) throws IOException {
		this.taskNumber = taskNumber;
		this.evosuiteReader = new BufferedReader(new InputStreamReader(evosuiteInputStream));
		this.logFileWriter = Files.newBufferedWriter(logFilePath);
		this.testGenerationListener = testGenerationListener;
	}
	
	@Override
	public void run() {
        //reads/copies the standard input and detects the generated tests
        try {
            String line;
            while ((line = this.evosuiteReader.readLine()) != null) {
                if (Thread.interrupted()) {
                    return;
                }
                
                //copies the line to the EvoSuite log file
                this.logFileWriter.write(line);
                this.logFileWriter.newLine();
                
                //check if the read line reports the output of a test case
                //by EvoSuite and in the positive case alerts the coordinator 
                //to emit the test case in the destination directory 
                final Matcher matcherEmittedTest = PATTERN_EMITTED_TEST.matcher(line);
                if (matcherEmittedTest.matches()) {
                    final int methodNumber = Integer.parseInt(matcherEmittedTest.group(1));
                    final int localTraceNumber = Integer.parseInt(matcherEmittedTest.group(2));
                    try { 
                    	this.testGenerationListener.onTestGenerated(this.taskNumber, methodNumber, localTraceNumber);
                    } catch (CoordinatorException e) {
                    	//the coordinator failed in putting the Evosuite test in the
                    	//destination directory: just returns
                    	return;
                    }
                }
            }
        } catch (IOException e) {
        	//nothing to do, an abrupt closure of the stream is possible
        } finally {
        	try {
				this.logFileWriter.close();
			} catch (IOException e) {
				//nothing to do
			}
        }
	}
}
