package ch.devcon5.flt;

import static org.slf4j.LoggerFactory.getLogger;

import ch.devcon5.flt.autoscaler.AutoScaler;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;

/**
 *
 */
public class Launcher extends AbstractVerticle{

  private static final Logger LOG = getLogger(Launcher.class);

  public static void main(String... args) {

    JsonObject config = new JsonObject().put("service", ExampleService.class.getName())
                                        //number of iteration cycles for synthetically increasing cpu load per thread
                                        //this setting is purely for the synthetic nature of application itself and not
                                        // for the elastic aspects of the system
                                        .put("loadFactor", 2_000)
                                        .put("minInstances", 1)
                                        .put("maxInstances", Runtime.getRuntime().availableProcessors() * 2)
                                        //threshold when a thread is considered overloaded (percent 0.0-1.0)
                                        //this setting affects the actual response times, especially when the
                                        //peak is reached (no more scaling happens)
                                        .put("threadLoadThreshold", .90)
                                        //how many times the threshold may be violated in the monitoring window
                                        // before the system scales up
                                        //this settings affects, how soon the system scales up and responds to load
                                        //a low value creates more instances very soon
                                        .put("maxThresholdViolations", 25)
                                        //how many cpu stats recordings are kept
                                        //same as the above value, this affects how soon the system scales up
                                        //a value higher that maxThresholdViolations compensates for fluctuation
                                        //in load
                                        //the monitorWindows must not be smaller than the maxThresholdViolations
                                        .put("monitorWindow", 35)
                                        //how many check cycles before scaling down
                                        //affects how fast the system scales down.
                                        //A low value results in almost immediate downscale
                                        .put("gracePeriodCycles", 2)
                                        //interval in ms for checking the cpu stats
                                        //this affects how fast a system responds to load.
                                        //a high value increases the inertness
                                        .put("checkInterval", 30000)
                                        //interval in ms for polling the threads for their cpu times
                                        //also affect how fast the system responds to load. This is the
                                        //distance of load measurements in the monitorWindow. Low values add more
                                        //datapoints in less time. The system may scale up/down too soon. High
                                        //values may miss short peaks of high load.
                                        .put("pollingInterval", 1000)
        ;

    LOG.info("Configuration\n{}",config.encodePrettily());

    Vertx vertx = Vertx.vertx();
    vertx.deployVerticle(Launcher.class.getName(), new DeploymentOptions().setConfig(config));
  }

  @Override
  public void start() throws Exception {

    vertx.deployVerticle(AutoScaler.class.getName(), new DeploymentOptions().setConfig(config()));
  }
}
