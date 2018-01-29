package ch.devcon5.flt.autoscaler;

import static java.util.stream.Collectors.toMap;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 *
 */
public class ThreadCpuTime {

  private static ThreadMXBean tmx = ManagementFactory.getThreadMXBean();

  final long id;
  final String name;
  final long timestamp;
  final long userTime;
  final long totalTime;

  ThreadCpuTime(final long id, final String name, final long timestamp, final long userTime, final long totalTime) {

    this.id = id;
    this.name = name;
    this.timestamp = timestamp;
    this.userTime = userTime;
    this.totalTime = totalTime;
  }

  public static Map<Long, ThreadCpuTime> capture(Predicate<ThreadInfo> filter) {

    return Arrays.stream(tmx.getAllThreadIds())
                 .mapToObj(tmx::getThreadInfo)
                 .filter(filter)
                 .map(info -> ThreadCpuTime.capture(info.getThreadId()))
                 .collect(toMap(ThreadCpuTime::getId, Function.identity()));
  }

  public static ThreadCpuTime capture(long threadId) {

    final long timestamp = System.nanoTime();
    final long userTime = tmx.getThreadUserTime(threadId);
    final long totalTime = tmx.getThreadCpuTime(threadId);
    final String name = tmx.getThreadInfo(threadId).getThreadName();

    return new ThreadCpuTime(threadId, name, timestamp, userTime, totalTime);
  }

  public long getId() {

    return id;
  }

  public String getName() {

    return name;
  }

  public long getTimestamp() {

    return timestamp;
  }

  public long getUserTime() {

    return userTime;
  }

  public long getSystemTime() {

    return totalTime - userTime;
  }

  public long getTotalTime() {

    return totalTime;
  }
}
