package ch.devcon5.flt.autoscaler;

import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.management.ThreadInfo;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import org.slf4j.Logger;

/**
 *
 */
public class AutoScaler extends AbstractVerticle {

  private static final Logger LOG = getLogger(AutoScaler.class);
  private int minInstances;
  private int maxInstances;

  private Set<String> deploymentIds = new HashSet<>();
  private String deploymentVerticle;

  private int maxThresholdViolations;
  private double loadThreshold;

  private Map<Long, ThreadCpuTime> lastCpuTime = new HashMap<>();
  private Map<Long, Deque<ThreadLoadStats>> cpuTimeHistory = new HashMap<>();

  private int gracePeriod;
  private int monitorWindow;
  private int graceCount = 0;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {

    this.deploymentVerticle = config().getString("service", "ch.devcon5.flt.ExampleService");
    this.minInstances = config().getInteger("minInstances", 1);
    this.maxInstances = config().getInteger("maxInstances", Runtime.getRuntime().availableProcessors() * 2);
    this.loadThreshold = config().getDouble("threadLoadThreshold", 0.8);
    this.gracePeriod = config().getInteger("gracePeriodCycles", 5);
    this.monitorWindow = config().getInteger("monitorWindow", 10);
    this.maxThresholdViolations = Math.min(config().getInteger("maxThresholdViolations", 5), this.monitorWindow);

    long checkInterval = config().getLong("checkInterval", 5000L);
    long pollingInterval = config().getLong("pollingInterval", 1000L);

    vertx.setPeriodic(pollingInterval, this::pollEventLoopThreads);
    vertx.setPeriodic(checkInterval, this::checkLoad);

    LOG.info("Initializing {} instances", minInstances);
    for (int i = 0; i < minInstances; i++) {
      scaleUp();
    }
    LOG.info("Load Threshold: {} % per Thread", loadThreshold * 100d);
    LOG.info("AutoScaler initialized");
  }

  /**
   * Captures the cpu time history of each event loop thread
   */
  private void pollEventLoopThreads(long timer) {

    ThreadCpuTime.capture(this::isEventLoopThread).forEach((id, info) -> {
      final ThreadCpuTime oldInfo = lastCpuTime.put(id, info);
      if (oldInfo != null) {
        cpuTimeHistory.computeIfAbsent(id, k -> new ArrayDeque<>());

        final Deque<ThreadLoadStats> threadHistory = cpuTimeHistory.get(id);
        threadHistory.addFirst(ThreadLoadStats.between(oldInfo, info));
        if (threadHistory.size() > monitorWindow) {
          threadHistory.removeLast();
        }

      }
    });
  }

  private boolean isEventLoopThread(final ThreadInfo info) {

    return info.getThreadName().startsWith("vert.x-eventloop-thread");
  }

  private void checkLoad(long timer) {

    cpuTimeHistory.values().stream().map(Deque::getFirst).forEach(stats -> LOG.info("ThreadLoad: {}", stats));
    List<Long> threadLoad = cpuTimeHistory.values().stream().map(this::countThresholdViolations).collect(toList());
    LOG.info("Threads Overload Status: {}", threadLoad);

    if (threadLoad.stream().anyMatch(this::maxThresholdsHit)) {

      LOG.info("Scaling up ({} -> {})", deploymentIds.size(), deploymentIds.size() + 1);
      scaleUp();

      cpuTimeHistory.values().forEach(Collection::clear);
      graceCount = gracePeriod;

    } else if (!cpuTimeHistory.isEmpty() && !(graceCount > 0) && cpuTimeHistory.values()
                                                                             .stream()
                                                                             .map(this::countThresholdViolations)
                                                                             .noneMatch(c -> c > 0)) {
      LOG.info("Scaling down ({} -> {})", deploymentIds.size(), deploymentIds.size() -1 );
      scaleDown();
    }
    if (graceCount > 0) {
      graceCount--;
    }
  }

  private void scaleUp() {

    if (deploymentIds.size() < maxInstances) {
      vertx.deployVerticle(deploymentVerticle, done -> {
        if (done.succeeded()) {
          LOG.info("Instance deployed: {}", done.result());
          deploymentIds.add(done.result());
        }
      });
    } else {
      LOG.info("Scale up canceled, deployment limit reached");
    }
  }

  private void scaleDown() {

    if (deploymentIds.size() > minInstances) {
      final String deploymentId = deploymentIds.iterator().next();
      LOG.info("Undeploying {}", deploymentId);
      vertx.undeploy(deploymentId, done -> deploymentIds.remove(deploymentId));
    } else {
      LOG.info("Scale down canceled, Minimum number of instances reached");
    }
  }

  private boolean maxThresholdsHit(final Long c) {

    return c >= maxThresholdViolations;
  }

  private long countThresholdViolations(final Deque<ThreadLoadStats> q) {

    return q.stream().filter(t -> t.getTotalPercent() > loadThreshold).count();
  }
}
