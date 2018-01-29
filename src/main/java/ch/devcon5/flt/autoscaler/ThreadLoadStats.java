package ch.devcon5.flt.autoscaler;

import io.vertx.core.json.JsonObject;

/**
 *
 */
public class ThreadLoadStats {

  private final long threadId;
  private final String threadName;
  private long intervalNanos;
  private long userTimeNanos;
  private long totalTimeNanos;

  public static ThreadLoadStats between(final ThreadCpuTime begin, final ThreadCpuTime end) {

    assert begin.id == end.id : "Could not calculate stats between two different thread";

    long interval = end.timestamp - begin.timestamp;
    long usrTime = end.userTime - begin.userTime;
    long cpuTime = end.totalTime - begin.totalTime;

    return new ThreadLoadStats(end.id, end.name, interval, usrTime, cpuTime);
  }


  public ThreadLoadStats(long threadId, String threadName, long intervalNanos, long userTimeNanos, long
      totalTimeNanos) {
    this.threadId = threadId;
    this.threadName = threadName;
    this.intervalNanos = intervalNanos;
    this.userTimeNanos = userTimeNanos;
    this.totalTimeNanos = totalTimeNanos;

  }

  public long getIntervalNanos() {

    return intervalNanos;
  }

  public JsonObject toJson() {

    return new JsonObject().put("threadId", this.threadId)
                           .put("threadName", this.threadName)
                           .put("intervalNanos", this.intervalNanos)
                           .put("userTimeNanos", getUserTimeNanos())
                           .put("systemTimeNanos", getSystemTimeNanos())
                           .put("totalTimeNanos", getTotalTimeNanos())
                           .put("user", getUserPercent())
                           .put("system", getSystemPercent())
                           .put("total", getTotalPercent());

  }

  public long getUserTimeNanos() {

    return userTimeNanos;
  }

  public long getSystemTimeNanos() {

    return this.totalTimeNanos - this.userTimeNanos;
  }

  public long getTotalTimeNanos() {

    return totalTimeNanos;
  }

  public double getUserPercent() {

    return (double) userTimeNanos / intervalNanos;
  }

  public double getSystemPercent() {

    return getSystemTimeNanos() / intervalNanos;
  }

  public double getTotalPercent() {

    return (double) totalTimeNanos / intervalNanos;
  }

  public long getThreadId() {

    return threadId;
  }

  public String getThreadName() {

    return threadName;
  }

  @Override
  public String toString() {

    return new StringBuilder(128)
        .append('[')
        .append(this.threadName)
        .append(']')
        .append(" tot: ").append(String.format("%.02f", 100d * getTotalPercent())).append("% ")
        .append(" usr: ").append(String.format("%.02f", 100d * getUserPercent())).append("% ")
        .append(" sys: ").append(String.format("%.02f", 100d * getSystemPercent())).append("% ")
        .append('\t')
        .append("\ttot: ").append(getTotalTimeNanos() / 1000).append(" μs ")
        .append("\tusr: ").append(getUserTimeNanos() / 1000).append(" μs ")
        .append("\tsys: ").append(getSystemTimeNanos() / 1000).append(" μs ")
        .toString();
  }
}
