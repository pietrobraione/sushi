package sbst.runtool;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class Main {
	public static void main(String[] s) throws IOException {
		final TardisTool tool = new TardisTool();
		final RunTool runtool = new RunTool(tool, new InputStreamReader(System.in), new OutputStreamWriter(System.out));
		runtool.run();	
	}
}
