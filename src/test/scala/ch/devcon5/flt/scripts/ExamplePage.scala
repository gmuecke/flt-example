package ch.devcon5.flt.scripts

import io.gatling.core.Predef._
import io.gatling.http.Predef._

/**
  *
  */
object ExamplePage {


  def hello = http("Say Hello")
                .get("/hello")


}
