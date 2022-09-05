package xyz.ahmetflix.chattingclient;

public class Timer {

    float ticksPerSecond;

    private double lastHRTime;

    public int elapsedTicks;

    public float renderPartialTicks;

    public float timerSpeed = 1.0F;

    public float elapsedPartialTicks;

    private long lastSyncSysClock;

    private long lastSyncHRClock;

    private long counter;

    private double timeSyncAdjustment = 1.0D;

    public Timer(float tps) {
        this.ticksPerSecond = tps;
        this.lastSyncSysClock = System.currentTimeMillis();
        this.lastSyncHRClock = System.nanoTime() / 1000000L;
    }

    /**
     * Updates all fields of the Timer using the current time
     */
    public void updateTimer() {
        long i = System.currentTimeMillis();
        long j = i - this.lastSyncSysClock;
        long k = System.nanoTime() / 1000000L;
        double d0 = (double)k / 1000.0D;

        if (j <= 1000L && j >= 0L) {
            this.counter += j;

            if (this.counter > 1000L) {
                long l = k - this.lastSyncHRClock;
                double d1 = (double)this.counter / (double)l;
                this.timeSyncAdjustment += (d1 - this.timeSyncAdjustment) * 0.20000000298023224D;
                this.lastSyncHRClock = k;
                this.counter = 0L;
            }

            if (this.counter < 0L) {
                this.lastSyncHRClock = k;
            }
        } else {
            this.lastHRTime = d0;
        }

        this.lastSyncSysClock = i;
        double d2 = (d0 - this.lastHRTime) * this.timeSyncAdjustment;
        this.lastHRTime = d0;
        d2 = d2 < 0 ? 0 : (d2 > 1 ? 1 : d2);
        this.elapsedPartialTicks = (float)((double)this.elapsedPartialTicks + d2 * (double)this.timerSpeed * (double)this.ticksPerSecond);
        this.elapsedTicks = (int)this.elapsedPartialTicks;
        this.elapsedPartialTicks -= (float)this.elapsedTicks;

        if (this.elapsedTicks > 10)
        {
            this.elapsedTicks = 10;
        }

        this.renderPartialTicks = this.elapsedPartialTicks;
    }
}
