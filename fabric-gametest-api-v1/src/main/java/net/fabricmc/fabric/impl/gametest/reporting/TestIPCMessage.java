package net.fabricmc.fabric.impl.gametest.reporting;

import com.google.gson.JsonObject;

/**
 * IPC messages sent from Fabric API to loom.
 */
public sealed interface TestIPCMessage permits
		TestIPCMessage.PushGroup,
		TestIPCMessage.PopGroup,
		TestIPCMessage.StartTest,
		TestIPCMessage.CompleteTest {

	void write(JsonObject json);

	record PushGroup(String name) implements TestIPCMessage {
		@Override
		public void write(JsonObject json) {
			json.addProperty("type", "push_group");
			json.addProperty("name", name);
		}
	}

	record PopGroup() implements TestIPCMessage {
		@Override
		public void write(JsonObject json) {
			json.addProperty("type", "pop_group");
		}
	}

	record StartTest(String name) implements TestIPCMessage {
		@Override
		public void write(JsonObject json) {
			json.addProperty("type", "start_test");
			json.addProperty("name", name);
		}
	}

	record CompleteTest(TestResult result) implements TestIPCMessage {
		@Override
		public void write(JsonObject json) {
			json.addProperty("type", "complete_test");
			json.addProperty("result", result.name);
		}
	}

	enum TestResult {
		SUCCEEDED,
		FAILED,
		SKIPPED;

		final String name = name().toLowerCase();
	}
}
