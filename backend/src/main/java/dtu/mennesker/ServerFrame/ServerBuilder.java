package dtu.mennesker.ServerFrame;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Function;

public class ServerBuilder {
    HashMap<String, Handler> handlers = new HashMap<>();
    Function<UUID, Update> onDisconnect;

    final int port;

    public ServerBuilder(int port) {
        this.port = port;
    }

    public ServerBuilder addHandler(String key, Handler handler) {
        this.handlers.put(key, handler);
        return this;
    }

    public ServerFrame start() {
        return new ServerFrame(handlers, onDisconnect, port);
    }

    public ServerBuilder setOnDisconnect(Function<UUID, Update> handler) {
        onDisconnect = (uuid) -> {
            try {
                return handler.apply(uuid);
            } catch (Exception e) {
                System.err.println("Failed to execute disconnect handler!");
                e.printStackTrace();
                return null;
            }
        };
        return this;
    }
}
