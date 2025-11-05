package com.camunda.academy.services;


public class CustomerService {
  public double deductCredit(double customerCredit, double amount) {
    return customerCredit > amount ? 0.0 : amount - customerCredit;
  }
}
