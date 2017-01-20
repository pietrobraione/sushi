package sushi.execution.merger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import sushi.configure.MergerParameters;
import sushi.exceptions.MergerException;
import sushi.execution.ExecutionResult;
import sushi.execution.Worker;
import sushi.logging.Logger;

public class MergerWorker extends Worker {
	private static final Logger logger = new Logger(MergerWorker.class);
	
	private final Merger merger;

	public MergerWorker(Merger merger) {
		this.merger = merger;
	}

	@Override
	public ExecutionResult call() throws MergerException {
		final MergerParameters p = this.merger.getInvocationParameters(this.taskNumber);

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
		int nTraces = 0;
		final TreeSet<Integer> mayBeCoveredBranches = new TreeSet<>();
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
						final Integer branchNumberGlobal = localToGlobal.get(branchNumberLocal); 
						w.write(branchNumberGlobal.toString());
						mayBeCoveredBranches.add(branchNumberGlobal);
					}
					w.newLine();
					++nTraces;
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
		
		//emits the branches to ignore file
		final Pattern pt;
		final boolean toCover;
		if (p.getBranchesToIgnore() != null) {
			pt = p.getBranchesToIgnore();
			toCover = false;
		} else if (p.getBranchesToCover() != null) {
			pt = p.getBranchesToCover();
			toCover = true;
		} else {
			pt = null;
			toCover = false;
		}
		int nBranchesToCover = branches.size();
		if (pt == null) {
			//creates an empty file
			try {
				Files.deleteIfExists(p.getBranchesToIgnoreFilePath());
				Files.createFile(p.getBranchesToIgnoreFilePath());
			} catch (IOException e) {
				logger.error("I/O error while deleting/creating " + p.getBranchesToIgnoreFilePath().toString());
				throw new MergerException(e);
			}
		} else {
			try (final BufferedWriter w = Files.newBufferedWriter(p.getBranchesToIgnoreFilePath())) {
				int branchNumber = 0;
				for (String branch : branches) {
					final Matcher m = pt.matcher(branch);
					if ((toCover ? !m.matches() : m.matches()) || !mayBeCoveredBranches.contains(branchNumber)) {
						w.write(Integer.toString(branchNumber));
						w.newLine();
						--nBranchesToCover;
					}
					++branchNumber;
				}
			} catch (IOException e) {
				logger.error("I/O error while writing " + p.getBranchesToIgnoreFilePath().toString());
				throw new MergerException(e);
			}
		}
		
		//emits the traces to ignore (empty) file
		try {
			Files.deleteIfExists(p.getTracesToIgnoreFilePath());
			Files.createFile(p.getTracesToIgnoreFilePath());
		} catch (IOException e) {
			logger.error("I/O error while deleting/creating " + p.getTracesToIgnoreFilePath().toString());
			throw new MergerException(e);
		}
		
		//some logging
		logger.info("Branches to cover: " + nBranchesToCover + ", traces to explore: " + nTraces);
		final ExecutionResult result = new ExecutionResult();
		result.setExitStatus(0);

		return result;
	}
}
