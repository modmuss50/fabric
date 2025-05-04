package net.fabricmc.fabric.impl.gametest.reporting;

import java.io.Closeable;

public interface TestReporter {
	TestReporter INSTANCE = IPCTestReporter.create();

	static Group group(String name) {
		INSTANCE.sendMessage(new TestIPCMessage.PushGroup(name));
		return () -> INSTANCE.sendMessage(new TestIPCMessage.PopGroup());
	}

	static Test test(String name) {
		INSTANCE.sendMessage(new TestIPCMessage.StartTest(name));
		return new Test() {
			@Override
			public void success() {
				INSTANCE.sendMessage(new TestIPCMessage.CompleteTest(TestIPCMessage.TestResult.SUCCEEDED));
			}

			@Override
			public void fail() {
				INSTANCE.sendMessage(new TestIPCMessage.CompleteTest(TestIPCMessage.TestResult.FAILED));
			}

			@Override
			public void skip() {
				INSTANCE.sendMessage(new TestIPCMessage.CompleteTest(TestIPCMessage.TestResult.SKIPPED));
			}
		};
	}

	void sendMessage(TestIPCMessage message);

	interface Group extends Closeable {
		@Override
		void close();
	}

	interface Test {
		void success();

		void fail();

		void skip();
	}
}
