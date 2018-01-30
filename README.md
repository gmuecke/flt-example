# Functional Load Testing - Example

This is an demo for my Functional Load Testing talk, which is not run during the talk due to time constraints
but mean for self study.

The example consists of an elastic service implemented in Vert.x that reacts on various load pressure by 
dynamically scaling up or down to emulate an elastic system. By various parameters the elastic behavior
can be configured.

## Starting the service

The service can be launched either using a Vert.x installation

```bash
vertx run -cp target/flt-example-1.0-SNAPSHOT.jar -conf config.json ch.devcon5.flt.Launcher
```

or you run the Launcher class directly from your IDE, modifying the hardcoded example config in
the Launcher class.

## The service configuration

The configuration for the Launcher follows this structure
```json
{
  "service" : "ch.devcon5.flt.ExampleService",
  "loadFactor" : 2000,
  "minInstances" : 1,
  "maxInstances" : 16,
  "threadLoadThreshold" : 0.6,
  "maxThresholdViolations" : 25,
  "monitorWindow" : 35,
  "gracePeriodCycles" : 2,
  "checkInterval" : 30000,
  "pollingInterval" : 1000
}
```

All the fields can be omit, reverting to default values. You only need to specify, what you
want to override.

- `service`: the fully qualified classname of the Verticle that provides the http endpoint that
 is tested. Default is: `ch.devcon5.flt.ExampleService`
 
- `loadFactor`: In order delay the response and emulate some CPU load, the service performs
some math calculations (something you shouldn't do in a event loop :-) ). The load factor
specifies how many loop-iterations are done for the math calculations. Higher values result
in longer response times. 
Default is: `2000`

- `minInstance`: The minimum instance of the `service` instances deployed. The system does
not scale down below this value. Default is: 1

- `maxInstance`: The maximum number of instances of the the `service`. The system does not 
scale up above this value: Default is: 2 x number of processors (including hyperthreading)

- `threadLoadThreshold`: a decimal value between 0 and 1 defining the Thread-load that
 is considered "overloaded". If enough such threshold violations (`maxThresholdViolations`)
 occur within the `monitorWindow`, an additional instance of `service` is deployed up to
 `maxIntance`. 
 Default is: 0.8 (80% of CPU time spent on thread during `pollingInterval`) 
 
- `maxThresholdViolations`: The number of violations of `threadLoadThreshold` withing `monitorWindow` 
that trigger a scaling up event. 
Default is: 5

- `monitorWindow`: The number of CPU load samples taken. The interval between samples is defined by
`pollingInterval`. 
Default is: 10.

- `gracePeriodCycles`: Number of `checkInterval` cycles that have to occur after a scale-up event 
before a scale down may occur. The grace period is required to prevent an immediate scale down.
Default is: 5

- `checkInterval`: The interval in ms when the load of the system is checked. This should be equal or larger 
than the `pollingInterval`. Scale up or scale down events can only happen when the check takes place. So
this value determines, how a fast a system can react to changing load. Larger value make the system less elastic,
lower value might result in premature scale-down. 
Default is: 5000ms

- `pollingInterval`: The interval in ms when CPU load statistics are determined. Each polling adds a value to
the `monitorWindow`. For polling, the cpu time for each event loop thread is determined to calculate the 
load for the thread. 
Default is: 1000 ms

To provide full monitoring coverage, the values _should_ follow the rule:
`checkInterval = monitorWindow * pollingInterval`. Nevertheless you may experiment with other values.

## The load profiles

There are 4 Simulations available, each using a different load model.

- Standard (ramp up & steady load)
- Gauss Distribution
- Poisson Distribution
- Binomial Distribution

### Standard 
The standard distribution is a "conventional" load model. It produces the highest
Requests per Test Run and the lowest Response times. It's good for benchmarking and Soak
testing. Yet it's not suitable for testing dynamic behavior of elastic systems.

### Gauss Distribution
Is a continuous distribution function. It's good for describing regular real-life loads 
during a certain period and it's non-steady nature. 
It is suitable for long-running scenarios, especially when being
combined with with gauss functions with different parameters, i.e. to an M-shaped load 
profile. 

### Binomial and Poisson Distribution
These two are discrete distributions that are good for describing the arrival rate of
independent users around a singular event 
(i.e. a go-live, a black friday, a marketing campaign) that produce a time-limited burst of
users. 
Due to the nature of the functions, they produce only a low number of steps. So for 
longer test runs, the length of a step (default 1 minute) has to be extended.

## Running the Tests

For each scenario a dedicated execution for the Gatling Maven Plugin is prepared. Execute
it on your terminal using one of the commands

```bash
mvn clean install 
mvn gatling:execute@standardTest
mvn gatling:execute@poissonTest
mvn gatling:execute@binomialTest
mvn gatling:execute@gaussTest
```

If the `@` execution does not work, you'll have to upgrade to a newer Maven version.
