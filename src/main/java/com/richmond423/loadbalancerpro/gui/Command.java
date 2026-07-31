package com.richmond423.loadbalancerpro.gui;

import org.json.JSONObject;
import java.util.concurrent.CompletableFuture;

/**
 * Legacy asynchronous command contract retained for {@code CloudManager} compatibility.
 *
 * <p>The retired JavaFX desktop simulator no longer consumes this type. Moving the public
 * contract to another package would be a separate compatibility change.</p>
 */
public interface Command {
    void execute();
    void undo();
    boolean canUndo();
    String getDescription();
    Status getStatus();
    String getId();

    default CompletableFuture<Void> executeAsync() {
        return CompletableFuture.runAsync(this::execute);
    }

    default CompletableFuture<Void> undoAsync() {
        return CompletableFuture.runAsync(() -> {
            if (canUndo()) undo();
        });
    }

    default JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("description", getDescription());
        json.put("status", getStatus().name());
        json.put("version", 2);
        return json;
    }

    enum Status {
        PENDING, COMPLETED, FAILED
    }
}
