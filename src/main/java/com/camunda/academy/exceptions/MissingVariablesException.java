package com.camunda.academy.exceptions;

public class MissingVariablesException extends RuntimeException {

  public MissingVariablesException(String variables) {
    super("The following process variables are necessary to complete the job: " + variables);
  }
}
