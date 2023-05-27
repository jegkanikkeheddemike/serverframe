package main.java.dtu.mennesker.ServerFrame;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * ServerFrame
 */
public class ServerFrame {
    HashMap<String, Handler> handlers = new HashMap<>();
    Function<UUID, Update> onDisconnect;

    final int port;
    final Thread handlerThread;
    final Thread socketThread;
    AtomicBoolean running = new AtomicBoolean(true);
    final LinkedList<Client> clients = new LinkedList<>();

    ServerFrame(HashMap<String, Handler> handlers, Function<UUID, Update> onDisconnect, int port) {
        this.handlers = handlers;
        this.port = port;
        this.onDisconnect = onDisconnect;
        handlerThread = new Thread(this::run);
        socketThread = new Thread(this::runSocket);
        handlerThread.start();
        socketThread.start();
    }

    void shutdown() {
        running.set(false);
    }

    void run() {
        while (running.get()) {
            // Do it every 0.1 seconds
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                e.printStackTrace();
            }

            synchronized (clients) {

                clients.stream().filter(this::filterping).forEach(this::ping);

                List<Update> updates = clients.stream()
                        .map(Client::readCommand).filter(Objects::nonNull)
                        .map(this::handleCommand).filter(Objects::nonNull)
                        .toList();

                for (Update update : updates) {
                    for (Client client : clients) {
                        client.writeUpdate(update);
                    }
                }

                if (onDisconnect != null) {
                    List<Update> disconnects = clients.stream()
                            .filter(Client::isDisconnected).map(Client::UUID)
                            .map(onDisconnect).filter(Objects::nonNull)
                            .toList();

                    for (Update update : disconnects) {
                        for (Client client : clients) {
                            client.writeUpdate(update);
                        }
                    }
                }

                clients.removeIf(Client::isDisconnected);
            }
        }

        synchronized (doneWaiters) {
            for (Thread thread : doneWaiters) {
                synchronized (thread) {
                    thread.notify();
                }
            }
        }
    }

    boolean filterping(Client client) {
        return new Random().nextDouble() > 0.9;
    }

    void ping(Client client) {
        client.writeUpdate(new Update("PING", "no_repond"));
    }

    Update handleCommand(Client.Command command) {
        Handler handler = handlers.get(command.command());
        if (handler == null) {
            System.err.println("SERVER RECIEVED INVALID COMMAND: " + command.command());
            return null;
        }

        try {
            return handler.apply(command.caller(), clients.stream().map(Client::UUID).toList());
        } catch (Exception e) {

            command.caller().writeUpdate(new Update("error", e.getMessage()));
            System.err.println("Client failed to execute handler " + command.command() + " at " + e.getMessage());
            return null;
        }
    }

    ArrayList<Thread> doneWaiters = new ArrayList<>();

    public void waitDone() {
        Thread selfThread = Thread.currentThread();
        synchronized (doneWaiters) {
            doneWaiters.add(selfThread);
        }

        try {
            synchronized (selfThread) {
                selfThread.wait();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    void runSocket() {
        try (ServerSocket serverSocket = new ServerSocket(port);) {
            while (running.get()) {
                Socket clientSocket = serverSocket.accept();
                Client client = new Client(clientSocket);
                synchronized (clients) {
                    clients.add(client);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            shutdown();
        }
    }
}