package sushi.execution.jbse;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import sushi.Options;
import sushi.exceptions.JBSEException;
import sushi.logging.Logger;
import sushi.util.DirectoryUtils;

public final class JBSETraces extends JBSEAbstract {
	private static final Logger logger = new Logger(JBSETraces.class);
	
	private List<Integer> tasks = null;
	private List<Integer> methodNumbers = null;
	private List<Integer> traceNumbersLocal = null;
	private String[] traceIds = null;

	public JBSETraces(Options options) {
		super(options, true, false);
	}

	@Override
	public List<Integer> tasks() {
		if (this.tasks == null) {
			this.tasks = new ArrayList<>();
			this.methodNumbers = new ArrayList<>();
			this.traceNumbersLocal = new ArrayList<>();
			final ArrayList<Integer> traceNumbersGlobal = new ArrayList<>();
			try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.getMinimizerOutFilePath(this.options))) {
				String line;
				int task = 0;
				while ((line = r.readLine()) != null) {
					this.tasks.add(task);
					final String[] fields = line.split(",");
					traceNumbersGlobal.add(Integer.parseInt(fields[0].trim()));
					this.methodNumbers.add(Integer.parseInt(fields[1].trim()));
					this.traceNumbersLocal.add(Integer.parseInt(fields[2].trim()));
					++task;						
				}
			} catch (IOException e) {
				logger.error("Unable to find and open minimizer output file " + DirectoryUtils.getMinimizerOutFilePath(this.options).toString());
				throw new JBSEException(e);
			}

			this.traceIds = new String[traceNumbersGlobal.size()];
			try (final BufferedReader r = Files.newBufferedReader(DirectoryUtils.getTracesFilePath(this.options))) {
				String line;					
				int currentPos = 0;
				while ((line = r.readLine()) != null) {
					if (traceNumbersGlobal.contains(currentPos)) {
						final String[] fields = line.split(",");
						String traceId = fields[2].split(" ")[1].trim();
						this.traceIds[traceNumbersGlobal.indexOf(currentPos)] = traceId;
					}
					++currentPos;
				}
			} catch (IOException e) {
				logger.error("Unable to find and open traces output file " + DirectoryUtils.getTracesFilePath(this.options).toString());
				throw new JBSEException(e);
			}
		}

		return this.tasks;
	}

	@Override
	public JBSEParameters getInvocationParameters(int taskNumber) {
		final JBSEParameters p = super.getInvocationParameters(this.methodNumbers.get(taskNumber));
		p.setIdentifierSubregion(this.traceIds[taskNumber]);
		p.setTraceCounterStart(this.traceNumbersLocal.get(taskNumber));
		return p;
	}
	
	@Override
	public void reset() {
		this.tasks = null;
	}
	
	//TODO getTimeBudget(), degreeOfParallelism()
}
