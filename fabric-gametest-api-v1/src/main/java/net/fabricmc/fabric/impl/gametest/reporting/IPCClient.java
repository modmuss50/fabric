package net.fabricmc.fabric.impl.gametest.reporting;

import java.io.Closeable;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IPCClient implements Consumer<String>, Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger(IPCClient.class);

	private final Path path;
	private final SocketChannel socketChannel;

	public IPCClient(Path path) throws IOException {
		LOGGER.info("Creating IPC client for path: {}", path);
		this.path = path;
		socketChannel = setupChannel();
	}

	private SocketChannel setupChannel() throws IOException {
		final UnixDomainSocketAddress address = UnixDomainSocketAddress.of(path);
		return SocketChannel.open(address);
	}

	@Override
	public void accept(String s) {
		synchronized (socketChannel) {
			LOGGER.info("Sending IPC message: {}", s);

			ByteBuffer buf = ByteBuffer.wrap((s + "\n").getBytes(StandardCharsets.UTF_8));

			while (buf.hasRemaining()) {
				try {
					socketChannel.write(buf);
				} catch (IOException e) {
					throw new UncheckedIOException("Failed to write to IPC socket", e);
				}
			}
		}
	}

	@Override
	public void close() throws IOException {
		synchronized (socketChannel) {
			socketChannel.close();
		}
	}
}
