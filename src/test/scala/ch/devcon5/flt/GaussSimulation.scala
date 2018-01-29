package ch.devcon5.flt

import ch.devcon5.flt.scenarios.ExampleScenario
import ch.devcon5.flt.loadmodel._
import io.gatling.http.Predef.http
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  *
  */
class GaussSimulation extends Simulation {

  val httpServer = http.baseURL("http://localhost:18080")
    .disableWarmUp
    .maxConnectionsPerHostLikeChrome
    .disableResponseChunksDiscarding
    .disableFollowRedirect

  val loadModel = gaussDistr(2)(10 minutes, 30000)
  loadModel.foreach(s => println(s))

  setUp(
    ExampleScenario.sayHello2minutes.inject(
      loadModel
    )

  ).protocols(httpServer)


}
