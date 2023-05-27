package main.java.dtu.mennesker.msgapp;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import main.java.dtu.mennesker.ServerFrame.Client;
import main.java.dtu.mennesker.ServerFrame.ServerBuilder;
import main.java.dtu.mennesker.ServerFrame.ServerFrame;
import main.java.dtu.mennesker.ServerFrame.Update;

public class ServerMain {
    public static void main(String[] args) {
        ServerFrame server = new ServerBuilder(9977)
                .addHandler("PostMsg", ServerMain::handlePostMsg)
                .addHandler("Connect", ServerMain::handleConnect)
                .setOnDisconnect(ServerMain::handleDisconnect)
                .start();

        server.waitDone();
    }

    static Update handlePostMsg(Client client, List<UUID> connected) throws Exception {
        String msgContent = client.readString();
        String sender = users.get(client.UUID()).username();
        return new Update("PostMsg", sender + ": " + msgContent);
    }

    static HashMap<UUID, User> users = new HashMap<>();

    static Update handleConnect(Client client, List<UUID> connected) throws Exception {

        String username = client.readString();
        users.put(client.UUID(), new User(client.UUID(), username));

        System.out.println("Connected to " + username);

        return new Update("UpdateUsers", users.values().stream().map(User::username).toList().toString());
    }

    static Update handleDisconnect(UUID uuid) {
        System.out.println("Disconnected " + users.remove(uuid).username());

        return new Update("UpdateUsers", users.values().stream().map(User::username).toList().toString());
    }
}

record User(
        UUID uuid,
        String username) {
}
