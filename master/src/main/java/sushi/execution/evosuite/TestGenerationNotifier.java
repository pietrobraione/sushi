package sushi.execution.evosuite;

@FunctionalInterface
public interface TestGenerationNotifier {
	void onTestGenerated(int taskNumber, int methodNumber, int localTraceNumber);
}
