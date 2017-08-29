package com.google.devtools.build.lib.remote;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

class AsynchronousFileStream extends OutputStream {
  private final AtomicInteger position;
  private final AsynchronousFileChannel channel;

  private static AsynchronousFileChannel openChannel(Path logPath) {
    if (logPath != null) {
      try {
        return AsynchronousFileChannel.open(
            logPath,
            StandardOpenOption.WRITE,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
      } catch (IOException ex) {
        System.err.println("Warning: Failed to open grpc log file: " + ex.toString());
      }
    }
    return null;
  }

  public AsynchronousFileStream(String logFilename) {
    Path logPath = logFilename == null ? null : Paths.get(logFilename);
    channel = openChannel(logPath);
    position = new AtomicInteger(0);
  }

  @Override
  public void write(int b) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void write(byte[] data) throws IOException {
    if (channel == null || !channel.isOpen()) {
      return;
    }

    ByteBuffer message = ByteBuffer.wrap(data);
    int curLogPosition = position.getAndAdd(data.length);
    channel.write(message, curLogPosition);
  }

  @Override
  public void close() throws IOException {
    if (channel != null) {
      channel.force(false);
      channel.close();
    }
  }
}
