package main.java.dtu.mennesker.ServerFrame;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

public class Client {
    private final UUID id;
    private final Socket socket;
    private final InputStream readStream;
    final OutputStream writeStream;
    private boolean disconnected = false;

    Client(Socket socket) throws IOException {
        id = UUID.randomUUID();
        this.socket = socket;
        readStream = socket.getInputStream();
        writeStream = socket.getOutputStream();
    }

    Command readCommand() {
        try {
            if (readStream.available() == 0) {
                return null;
            }
            return new Command(readString(), this);
        } catch (Exception e) {
            disconnect();
        }
        return null;
    }

    public String readString() throws IOException {
        ArrayList<Byte> buffer = new ArrayList<>();
        while (true) {
            byte nextByte = (byte) readStream.read();
            if (nextByte == '\n') {
                break;
            }
            buffer.add(nextByte);
        }
        byte[] bytes = new byte[buffer.size()];
        for (int i = 0; i < buffer.size(); i++) {
            bytes[i] = buffer.get(i);
        }

        return new String(bytes);
    }

    public void disconnect() {
        try {
            socket.close();
        } catch (Exception ignore) {
            // Ignore
        }
        disconnected = true;
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    void writeUpdate(Update update) {
        try {
            writeStream.write(update.name.getBytes());
            writeStream.write('\n');
            writeStream.write(update.msg.getBytes());
            writeStream.write('\n');
        } catch (Exception e) {
            disconnect();
        }
    }

    record Command(String command, Client caller) {
    }

    public UUID UUID() {
        return id;
    }
}
