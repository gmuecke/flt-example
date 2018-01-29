package ch.devcon5.flt

import ch.devcon5.flt.loadmodel._
import ch.devcon5.flt.scenarios.ExampleScenario
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef.http

import scala.concurrent.duration._
import scala.language.postfixOps


/**
  *
  */
class StandardSimulation extends Simulation {

  val httpServer = http.baseURL("http://localhost:18080")
    .disableWarmUp
    .maxConnectionsPerHostLikeChrome
    .disableResponseChunksDiscarding
    .disableFollowRedirect


  setUp(

    ExampleScenario.sayHello2minutes.inject(
      rampUsersPerSec(0.1) to 50 during(2 minutes),
      constantUsersPerSec(50) during(10 minutes)
    )

  ).protocols(httpServer)


}
