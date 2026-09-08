package org.supermariowar.app;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Merge physical keys shared by players and retain the mapping chosen on key-down. */
final class TouchKeys {
    interface Mapper { int map(int player, int key); }
    private final Mapper mapper;
    private final TouchInput.Sink sink;
    private final Map<Integer, Integer> owners = new HashMap<>();
    private Set<Integer> held = new TreeSet<>();
    private boolean batching;

    TouchKeys(Mapper mapper, TouchInput.Sink sink) {
        this.mapper = mapper;
        this.sink = sink;
    }

    void change(int player, int key, boolean down) {
        int owner = (player << 16) | key;
        if (down) {
            if (!owners.containsKey(owner)) owners.put(owner, mapper.map(player, key));
        } else {
            owners.remove(owner);
        }
        update();
    }

    void beginBatch() { batching = true; }
    void endBatch() { batching = false; update(); }

    /** Reassert held physical keys after native input state was reset. */
    void resendHeld() {
        for (int key : held) sink.keyChanged(key, true);
    }

    private void update() {
        if (batching) return;
        Set<Integer> next = new TreeSet<>(owners.values());
        for (int key : held) if (!next.contains(key)) sink.keyChanged(key, false);
        for (int key : next) if (!held.contains(key)) sink.keyChanged(key, true);
        held = next;
    }
}
