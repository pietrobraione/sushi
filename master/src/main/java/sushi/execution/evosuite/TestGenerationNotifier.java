package sushi.execution.evosuite;

import sushi.exceptions.CoordinatorException;

@FunctionalInterface
public interface TestGenerationNotifier {
	void onTestGenerated(int taskNumber, int methodNumber, int localTraceNumber) throws CoordinatorException;
}
