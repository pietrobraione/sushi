package sbst.runtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class TardisTool implements ITestingTool {
	private static final String HOME           = "/home/tardis";
	private static final String TARDIS         = HOME + "/lib/tardis-master-0.1.0-SNAPSHOT.jar";
	private static final String SUSHI_LIB      = HOME + "/lib/sushi-lib-0.2.0-SNAPSHOT.jar";
	private static final String JBSE_LIB       = HOME + "/lib/jbse-0.9.0-SNAPSHOT-shaded.jar";
	private static final String EVOSUITE_LIB   = HOME + "/lib/evosuite-shaded-1.0.6-SNAPSHOT.jar";
	private static final String ARGS4J_LIB     = HOME + "/lib/args4j-2.32.jar";
	private static final String JAVAPARSER_LIB = HOME + "/lib/javaparser-core-3.4.0.jar";
	private static final String TOOLS_LIB      = "/usr/lib/jvm/java-8-openjdk-amd64/lib/tools.jar";
	private static final String Z3_BIN         = "/usr/bin/z3";
	private static final String TMP_DIR        = HOME + "/temp/data";
	private static final String OUT_DIR        = HOME + "/temp/testcases";
	
	private final String classPathTardis;
	private String classPathSUT;

	public TardisTool() {
		final StringBuilder sb = new StringBuilder();
		sb.append(TARDIS); sb.append(":");
		sb.append(SUSHI_LIB); sb.append(":");
		sb.append(JBSE_LIB); sb.append(":");
		sb.append(EVOSUITE_LIB); sb.append(":");
		sb.append(ARGS4J_LIB); sb.append(":");
		sb.append(JAVAPARSER_LIB); sb.append(":");
		sb.append(TOOLS_LIB);
		this.classPathTardis = sb.toString();
	}

	@Override
	public List<File> getExtraClassPath() {
		return Collections.emptyList();
	}
	
	@Override
	public void initialize(File src, File bin, List<File> classPath) {
		final StringBuilder sb = new StringBuilder();
		sb.append(bin);
		for (File f : classPath) {
			sb.append(":");
			sb.append(f);
		}
		this.classPathSUT = sb.toString();
	}

	@Override
	public void run(String cName, long timeBudget) {
		try {
			final ProcessBuilder pbuilder = new ProcessBuilder(
					"java", "-Xms16G", "-Xmx16G", 
					"-classpath", this.classPathTardis,
					"tardis.Main", 
					"-sushi_lib", SUSHI_LIB, "-jbse_lib", JBSE_LIB, "-evosuite", EVOSUITE_LIB, "-z3", Z3_BIN,
					"-use_mosa", "-num_mosa_targets", "5", 
					"-num_threads", "5", "-max_depth", "50",
					"-tmp_base", TMP_DIR, "-out", OUT_DIR, 
					"-global_time_budget_duration", Long.toString(timeBudget), "-global_time_budget_unit", "SECONDS",
					"-classes", this.classPathSUT, "-target_class", cName.replace('.', '/')
			);

			pbuilder.redirectErrorStream(true);

			final Process process;
			final InputStreamReader stdout;
			final InputStream stderr;
			final OutputStreamWriter stdin;
			process = pbuilder.start();
			stderr = process.getErrorStream();
			stdout = new InputStreamReader(process.getInputStream());
			stdin = new OutputStreamWriter(process.getOutputStream());

			final PrintStream dump = new PrintStream(new FileOutputStream(new File(TMP_DIR + "/" + new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss").format(new Date()) + ".txt"), false));
			dump.println(pbuilder.command().toString());
			final BufferedReader reader = new BufferedReader(stdout);
			String line = null;
			while ((line = reader.readLine()) != null) {
				dump.println(line);
			}
			dump.close();
			reader.close();

			process.waitFor();

			if (stdout != null) stdout.close();
			if (stdin != null) stdin.close();
			if (stderr != null) stderr.close();
			if (process !=null) process.destroy();
		} catch (IOException | InterruptedException e) {

		}
	}
}
