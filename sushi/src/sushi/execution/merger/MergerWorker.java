package sushi.execution.merger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.TreeMap;

import sushi.exceptions.MergerException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.execution.listpaths.ListPathsWorker;
import sushi.logging.Logger;

public class MergerWorker extends Worker {
	private static final Logger logger = new Logger(ListPathsWorker.class);
	
	private final Merger listPaths;

	public MergerWorker(Merger listPaths) {
		this.listPaths = listPaths;
	}

	@Override
	public ExecutionResult call() throws MergerException {
		final MergerParameters p = this.listPaths.getInvocationParameters(this.taskNumber);

		final int methods;
		try {
			methods = (int) Files.lines(p.getMethodsFilePath()).count();
		} catch (IOException e) {
			logger.error("I/O error while reading " + p.getMethodsFilePath().toString());
			throw new MergerException(e);
		}
		
		try {
			Files.deleteIfExists(p.getCoverageFilePathGlobal());
			Files.createFile(p.getCoverageFilePathGlobal());
			Files.deleteIfExists(p.getTracesFilePathGlobal());
			Files.createFile(p.getTracesFilePathGlobal());
		} catch (IOException e) {
			logger.error("I/O error while deleting/creating " + p.getCoverageFilePathGlobal().toString());
			throw new MergerException(e);
		}

		final ArrayList<String> branches = new ArrayList<>();
		final TreeMap<String, Integer> branchNumbers = new TreeMap<>();
		for (int method = 0; method < methods; ++method) {
			final ArrayList<Integer> localToGlobal = new ArrayList<>();
			
			//parses the local branches file for the method
			try (final BufferedReader r = Files.newBufferedReader(p.getBranchesFilePathLocal(method))) {
				String line;
				while ((line = r.readLine()) != null) {
					final String branch = line.trim();
					int branchNumberGlobal;
					if (branchNumbers.containsKey(branch)) {
						branchNumberGlobal = branchNumbers.get(branch);
					} else {
						branches.add(branch);
						branchNumberGlobal = branches.size() - 1;
						branchNumbers.put(branch, branchNumberGlobal);
					}
					localToGlobal.add(branchNumberGlobal);
				}
			} catch (IOException e) {
				logger.error("I/O error while reading " + p.getBranchesFilePathLocal(method).toString());
				throw new MergerException(e);
			}
			
			//translates the local coverage file for the method and updates
			//the global coverage file
			try (final BufferedReader r = Files.newBufferedReader(p.getCoverageFilePathLocal(method));
				 final BufferedWriter w = Files.newBufferedWriter(p.getCoverageFilePathGlobal(), StandardOpenOption.APPEND)) {
				String line;
				while ((line = r.readLine()) != null) {
					w.write(Integer.toString(method));
					w.write(", ");
					final String[] fieldsRead = line.split(",");
					w.write(fieldsRead[0].trim());
					w.write(", ");
					w.write(fieldsRead[1].trim());
					for (int i = 2; i < fieldsRead.length; ++i) {
						w.write(", ");
						final int branchNumberLocal = Integer.parseInt(fieldsRead[i].trim());
						w.write(Integer.toString(localToGlobal.get(branchNumberLocal)));
					}
					w.newLine();
				}
			} catch (IOException e) {
				logger.error("I/O error while reading " + p.getCoverageFilePathLocal(method).toString() + " or writing " + p.getCoverageFilePathGlobal().toString());
				throw new MergerException(e);
			}

			//translates the local alltraces file for the method and updates
			//the global alltraces file
			try (final BufferedReader r = Files.newBufferedReader(p.getTracesFilePathLocal(method));
				 final BufferedWriter w = Files.newBufferedWriter(p.getTracesFilePathGlobal(), StandardOpenOption.APPEND)) {
				String line;
				while ((line = r.readLine()) != null) {
					w.write(Integer.toString(method));
					w.write(", ");
					final String[] fieldsRead = line.split(",");
					w.write(fieldsRead[0].trim());
					w.write(", ");
					w.write(fieldsRead[1].trim());
					w.newLine();
				}
			} catch (IOException e) {
				logger.error("I/O error while reading " + p.getTracesFilePathLocal(method).toString() + " or writing " + p.getTracesFilePathGlobal().toString());
				throw new MergerException(e);
			}
}
		
		//emits the global branches file
		try (final BufferedWriter w = Files.newBufferedWriter(p.getBranchesFilePathGlobal())) {
			for (String branch : branches) {
				w.write(branch);
				w.newLine();
			}
		} catch (IOException e) {
			logger.error("I/O error while writing " + p.getBranchesFilePathGlobal().toString());
			throw new MergerException(e);
		}
		
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(0);

		return result;
	}
}
