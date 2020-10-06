package org.apache.plc4x.java.can.listener;

import org.apache.plc4x.java.socketcan.readwrite.SocketCANFrame;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CompositeCallback implements Callback {

    private List<Callback> callbacks = new CopyOnWriteArrayList<>();

    @Override
    public void receive(SocketCANFrame frame) {
        callbacks.forEach(callback -> callback.receive(frame));
    }

    public boolean addCallback(Callback callback) {
        return callbacks.add(callback);
    }

    public boolean removeCallback(Callback callback) {
        return callbacks.remove(callback);
    }
}

