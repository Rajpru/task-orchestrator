package com.ailab.orchestrator.model.dto;

public class StatsResponse {
    private long running;
    private long waiting;
    private long blocked;
    private long succeeded;
    private long failed;

    public StatsResponse(long running, long waiting, long blocked, long succeeded, long failed) {
        this.running = running;
        this.waiting = waiting;
        this.blocked = blocked;
        this.succeeded = succeeded;
        this.failed = failed;
    }

    public long getRunning() { return running; }
    public long getWaiting() { return waiting; }
    public long getBlocked() { return blocked; }
    public long getSucceeded() { return succeeded; }
    public long getFailed() { return failed; }
}