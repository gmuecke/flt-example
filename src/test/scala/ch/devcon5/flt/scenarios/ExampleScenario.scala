package ch.devcon5.flt.scenarios

import ch.devcon5.flt.scripts.ExamplePage
import io.gatling.core.Predef._

import scala.concurrent.duration._
import scala.language.postfixOps
import scala.languageFeature._

/**
  *
  */
object ExampleScenario {

  def sayHello20times =
    scenario("say hello 20x")
      //1
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      //11
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      .exec(ExamplePage.hello).pace(1 seconds)
      //21

  def sayHello2minutes =
    scenario("say hello 2mins")
        .during(2 minutes) {
          exec(ExamplePage.hello).pace(1 seconds)
        }
}
