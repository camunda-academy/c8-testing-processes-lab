package com.camunda.academy.exceptions;

public class InvalidCreditCardException extends Exception {

  public InvalidCreditCardException() {
    super("Invalid credit card expiry date");
  }
}