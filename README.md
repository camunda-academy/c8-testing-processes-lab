# Camunda 8 - Testing Processes - Lab

Lab project for the **[Camunda 8 - Testing Processes with CPT (Java)](https://academy.camunda.com/c8-testing-processes)** course on Camunda Academy.

This course gives a detailed hands-on experience on testing processes using Camunda. During the course, you will review basic and advanced testing approaches - including process tests and mocked worker behavior - to better understand how to validate process applications and improve test reliability.

> **Difficulty**: Intermediate | **Time**: ~2 hours | **Platform**: Camunda 8.9.0+

## Overview

In this lab, you test process behavior for payment and hardware request scenarios. The test suite demonstrates several testing scopes and styles:
- Process tests against BPMN flows
- Worker-focused tests
- Tests with mocked external behavior

The project uses Testcontainers to start a Camunda runtime for process tests.

## What you will learn

- Explain core concepts of process testing in Camunda
- Distinguish testing scopes (unit, process, integration)
- Set up process tests with camunda-process-test and a container runtime
- Write readable tests that validate BPMN model execution
- Test workers together with process execution
- Mock external dependencies and task behavior

## Prerequisites

### Knowledge:
- Basic BPMN reading skills
- Familiarity with Camunda concepts (service tasks, user tasks)
- Java and Maven basics

### Tools:
- Java 21+
- Maven 3.x
- Docker Desktop (or any Docker API-compatible runtime)

If Docker is not running, tests that depend on Testcontainers will fail.

## Project Structure

```text
src/main/java/com/camunda/academy/
├── PaymentApplication.java                 # Entry point for the lab application
├── exceptions/
│   ├── InvalidCreditCardException.java     # Domain exception for invalid card data
│   └── MissingVariablesException.java      # Domain exception for missing process variables
├── handlers/
│   ├── CreditCardChargingHandler.java      # Worker handler for payment charging
│   └── CreditDeductionHandler.java         # Worker handler for credit deduction
└── services/
    ├── CreditCardService.java              # Service abstraction for credit card charging
    └── CustomerService.java                # Service abstraction for customer credit management

src/main/resources/
├── payment.bpmn                            # Payment process definition
├── hardwarerequest.bpmn                    # Hardware request process definition
└── application.properties                  # Application and client configuration

src/test/java/com/camunda/academy/
├── PaymentProcessTest.java                 # Process tests for payment flow
├── PaymentProcessTestWithMocks.java        # Payment tests with mocked dependencies
└── HardwareRequestProcessTest.java         # Process tests for hardware request flow

src/test/resources/
└── camunda-container-runtime.properties    # CPT container runtime test settings
```

## Build

```bash
mvn clean package
```

## Run Tests

Run all tests:

```bash
mvn test
```

Run payment process tests only:

```bash
mvn -Dtest=PaymentProcessTest test
mvn -Dtest=PaymentProcessTestWithMocks test
```

Run hardware request process tests only:

```bash
mvn -Dtest=HardwareRequestProcessTest test
```

Run all three lab test classes explicitly:

```bash
mvn -Dtest=PaymentProcessTest,PaymentProcessTestWithMocks,HardwareRequestProcessTest test
```

## Notes on Test Runtime

- The test runtime configuration is in `src/test/resources/camunda-container-runtime.properties`.
- This repository sets:

```properties
camunda.client.applyEnvironmentVariableOverrides=false
```

This keeps Camunda process test client settings deterministic in local test runs.

## Troubleshooting

### Tests fail with "Could not find a valid Docker environment"

Cause:
- Testcontainers cannot connect to a Docker API-compatible runtime.

Fix:
1. Start Docker Desktop (or Docker Engine) and wait until it is fully running.
2. Verify Docker is reachable:

```bash
docker version
docker ps
```

3. Re-run a single test class first:

```bash
mvn -Dtest=PaymentProcessTest test
```

### Tests fail while pulling `camunda/camunda:8.9.1`

Cause:
- Network, registry auth, or proxy issues prevent image pull.

Fix:
1. Pull the image manually to validate access:

```bash
docker pull camunda/camunda:8.9.1
```

2. If your company uses a proxy, configure Docker Desktop proxy settings and retry.
3. Confirm you have internet access to Docker Hub.

### Docker works in terminal but tests still fail

Possible reasons:
- IDE terminal/session does not see updated Docker environment.
- Docker started after the test JVM was already initialized.

Fix:
1. Open a new terminal session.
2. Run:

```bash
mvn -Dtest=PaymentProcessTestWithMocks test
```

3. If needed, restart the IDE and rerun tests.

### Quick verification flow

```bash
docker ps
mvn -Dtest=PaymentProcessTest test
mvn -Dtest=PaymentProcessTestWithMocks test
mvn -Dtest=HardwareRequestProcessTest test
```

## Relationship to the Develop Workers (Java) Lab

The related repository below focuses on implementing job workers and runtime behavior:
- https://github.com/camunda-academy/c8-develop-workers-java-lab

Together, the two labs provide complementary skills:
- Develop Workers (Java): building worker logic and worker configuration
- Testing Processes: validating BPMN orchestration and worker/process behavior through automated tests

## Useful Links

- Camunda Academy course: https://academy.camunda.com/c8-testing-processes
- Related workers lab: https://github.com/camunda-academy/c8-develop-workers-java-lab
- Camunda docs: https://docs.camunda.io/