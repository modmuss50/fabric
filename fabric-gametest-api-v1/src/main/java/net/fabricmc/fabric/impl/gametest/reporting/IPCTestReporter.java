package net.fabricmc.fabric.impl.gametest.reporting;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

import com.google.gson.JsonObject;

public record IPCTestReporter(IPCClient ipc) implements TestReporter {
	@Override
	public void sendMessage(TestIPCMessage message) {
		JsonObject json = new JsonObject();
		json.addProperty("version", 1);
		message.write(json);
		ipc.accept(json.toString());
	}

	public static TestReporter create() {
		String path = System.getProperty("fabric.gameTest.reporting.ipcPath");
		if (path == null) {
			return message -> {
				// NOP
			};
		}

		try {
			return new IPCTestReporter(new IPCClient(Paths.get(path)));
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to create IPC client", e);
		}
	}
}
