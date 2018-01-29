package ch.devcon5.flt;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Random;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;

/**
 *
 */
public class ExampleService extends AbstractVerticle {

  private static final Logger LOG = getLogger(ExampleService.class);

  private Random random;
  private int loadFactor;

  @Override
  public void start(final Future<Void> startFuture) throws Exception {

    this.random = new Random();
    this.loadFactor = config().getInteger("loadFactor", 10_000);

    HttpServerOptions httpOpts = new HttpServerOptions().setPort(18080);
    HttpServer server = vertx.createHttpServer(httpOpts);

    Router router = Router.router(vertx);
    router.get("/hello").handler(this::response);

    server.requestHandler(router::accept).listen(done -> {
      if (done.succeeded()) {
        LOG.info("Serer listening on {}", httpOpts.getPort());
        startFuture.complete();
      } else {
        LOG.info("Server startup failed", done.cause());
        startFuture.fail(done.cause());
      }
    });
  }

  private void response(final RoutingContext ctx) {

    final long ts = System.nanoTime();

    double rs = random.nextDouble();
    long s = System.nanoTime();
    for (int i = 0; i < loadFactor; i++) {
      rs = Math.pow(random.nextDouble(), Math.sqrt(rs));
    }
    long e = System.nanoTime();
    ctx.response()
       .end(new JsonObject().put("ts", ts).put("result", rs).put("durationMicros", (e - s) / 1000).toBuffer());
  }
}
