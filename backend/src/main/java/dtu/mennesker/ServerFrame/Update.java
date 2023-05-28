package dtu.mennesker.ServerFrame;

import java.util.List;
import java.util.UUID;

public class Update {
    final String msg;
    final String name;
    List<UUID> limitTo;

    public Update(String name, String msg) {
        this.msg = msg;
        this.name = name;
    }

    public Update limitTo(List<UUID> limits) {
        this.limitTo = limits;
        return this;
    }
}
