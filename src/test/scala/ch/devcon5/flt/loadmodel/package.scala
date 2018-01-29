package ch.devcon5.flt

import ch.devcon5.flt.loadmodel.MathFunctions._
import io.gatling.core.Predef.{constantUsersPerSec, rampUsersPerSec}
import io.gatling.core.controller.inject.InjectionStep

import scala.language.{postfixOps, reflectiveCalls}
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.math.{Pi, exp, pow, sqrt}

/**
  * Defines load functions to distribute users
  */
package object loadmodel {

  object MathFunctions {

    /**
      * Constant distribution, always returns a constant value, no matter the input
      * @param v
      *  the constant value
      * @param x
      *  x-value, it's not used
      * @return
      */
    def constant(v : Double)(x : Double): Double ={
      v
    }

    /**
      * Linear distribution
      * @param slope
      *  the how steep the line should progress
      * @param offset
      *  the offset that is added to the result
      * @param x-value to calculate a value for
      * @return
      */
    def linear(slope : Double, offset : Double)(x  : Double) : Double ={
        slope*x + offset
    }

    /**
      * The Gauss distribution function
      *
      * @param sigma standard deviation
      * @param mu    mean or expectation
      * @param x     x-value to apply the gauss distribution function to
      * @return
      */
    def gauss(sigma: Double, mu: Double)(x: Double): Double = {
      (1 / (sigma * sqrt(2 * Pi))) * exp(-0.5 * pow((x - mu) / sigma, 2))
    }

    /**
      * The M-Shape load function is the sum of two gauss-distribution-function
      *
      * @param sigma1
      * sigma of the first gauss-distribution-function
      * @param mu1
      * mu of the first gauss-distribution-function
      * @param sigma2
      * sigma of the second gauss-distribution-function
      * @param mu2
      * mu of the second gauss-distribution-function (should be greater that mu1)
      * @param x
      * x-value to apply the gauss distribution function to
      * @return
      */
    def mshape(sigma1: Double, mu1: Double, sigma2: Double, mu2: Double)(x: Double): Double = {
      (gauss(sigma1, mu1)(x) + gauss(sigma2, mu2)(x)) / 2
    }

    /**
      * Calculates the factorial of n, i.e. 3! = 3 * 2 * 1. The method uses a non-recursive approach
      *
      * @param n
      * the number to calculate the factorial for
      * @return
      */
    def fac(n: Int): Int = (1 /: (1 to n)) (_ * _)


    //implicit non-recursive factorial calculation, see http://www.scalaclass.com/node/63
    implicit def factorial(n: Int) = new {
      def ! = fac(n)
    }

    /**
      * Binomial distribution. This is a discrete function, therefore results will always ressemble a step-function
      *
      * @param k
      * the number of samples.
      * @param p
      * probability of the event (i.e. that the user actually arrives at the system)
      * @return
      */
    def binomial(k: Int, p: Double)(n: Double): Double = {

      if (k > n) 0
      else ((n.toInt !) / ((k !) * (n.toInt - k) !)) * pow(p, k) * pow(1 - p, n - k)
    }

    /**
      * Possion distribution. This is a discrete function. Useful for modelling arrival rate for a singular event.
      * @param l
      *           lambda
      * @param n
      *   probability at point x
      * @return
      */
    def poisson(l : Double)(n : Double) : Double = {
      exp(-l)*pow(l, n) / (n.toInt!).toDouble
    }


  }

  /**
    * Generates a step-function that distributes the load. Each step has a constant rate of users
    *
    * @param duration
    * the total duration of the injection list
    * @param totalUsers
    * the total number of users that should be distributed over the whole period
    * @param distrFun
    * the distribution function to determine the user rate for each step
    * @param stepFun
    * the function that determines the number of steps for the given duration. Default is one step per minute
    * @return
    */
  def stepUserRate(duration: FiniteDuration,
                   totalUsers: Int,
                   distrFun: (Int) => (Double => Double),
                   stepFun: (Duration) => (Int) = (d) => d.toMinutes.toInt): List[InjectionStep] = {
    val steps = stepFun(duration)
    val stepDuration = duration / steps

    //we use perSecond here as the rate is specified as constUsersPerSec
    def fun(x: Double) = totalUsers * distrFun(steps)(x) / stepDuration.toSeconds

    List.range(0, steps).map(step => constantUsersPerSec(fun(step)) during stepDuration)
  }

  /**
    * Generates a continuous function that distributes the load. Each stap has a linear approximation of the continuous function by applying ramp-injection
    *
    * @param duration
    * the total duration of the injection list
    * @param totalUsers
    * the total number of users that should be distributed over the whole period
    * @param distrFun
    * the distribution function to determine the user rate for each step
    * @param stepFun
    * the function that determines the number of steps for the given duration. Default is one step per minute
    * @return
    */
  def continuousUserRate(duration: FiniteDuration,
                         totalUsers: Int,
                         distrFun: (Int) => (Double => Double),
                         stepFun: (Duration) => (Int) = (d) => d.toMinutes.toInt): List[InjectionStep] = {

    val steps = stepFun(duration)
    val stepDuration = duration / steps

    //we use perSecond here as the rate is specified as rampUsersPerSec
    def fun(x: Double) : Double =  {
      totalUsers * distrFun(steps)(x) / stepDuration.toSeconds
    }

    List.range(0, steps).map(step => {
      println(step + " : " + fun(step) + " -> " + fun(step + 1))
      rampUsersPerSec(fun(step)) to fun(step + 1) during stepDuration
    })
  }

  /**
    * Creates a list of gauss-distributed user-rate steps
    *
    * @param sigma
    * the standard deviation, use higher values for flatter curves and lower value for higher peaks. Default is 4
    * @param muPercent
    * position of the peak relative to the total duration, default is 0.5 (middle)
    * @param duration
    * the total duration of the injections steps
    * @param totalUsers
    * the total number of users. Effective number of users can be 25% lower as users distribute over infinity
    * @return
    */
  def gaussStep(sigma: Double = 4, muPercent: Double = 0.5)(duration: FiniteDuration, totalUsers: Int) =
    stepUserRate(duration, totalUsers, steps => gauss(sigma, steps * muPercent))

  /**
    * Creates a list of gauss-distributed user-rate steps with continuous user rate
    *
    * @param sigma
    * the standard deviation, use higher values for flatter curves and lower value for higher peaks. Default is 4
    * @param muPercent
    * position of the peak relative to the total duration, default is 0.5 (middle)
    * @param duration
    * the total duration of the injections steps
    * @param totalUsers
    * the total number of users. Effective number of users can be 25% lower as users distribute over infinity
    * @return
    */
  def gaussDistr(sigma: Double = 4, muPercent: Double = 0.5)
                     (duration: FiniteDuration, totalUsers: Int) =
    continuousUserRate(duration, totalUsers, steps => gauss(sigma, steps * muPercent))

  def mshapeDistr(sigma1: Double = 4, sigma2: Double = 4, mu1Percent: Double = 0.25, mu2Percent: Double = 0.75)
                      (duration: FiniteDuration, totalUsers: Int) =
    continuousUserRate(duration, totalUsers, steps => mshape(sigma1, steps * mu1Percent, sigma2, steps * mu2Percent))

  def poissonDistr(lambda : Int => Double = steps => steps / 3)
                       (duration: FiniteDuration, totalUsers: Int) =
    continuousUserRate(duration, totalUsers, steps => poisson(lambda(steps)))

  def binomialDistr(k : Int => Int = steps => steps / 2, p : Double = 0.05)
                  (duration: FiniteDuration, totalUsers: Int) =
    continuousUserRate(duration, totalUsers, steps => binomial(k(steps),p))
}
