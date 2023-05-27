package main.java.dtu.mennesker.ServerFrame;

import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface Handler {
    Update apply(Client client, List<UUID> users) throws Exception;
}