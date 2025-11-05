package com.camunda.academy.services;

import com.camunda.academy.exceptions.InvalidCreditCardException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CreditCardService {

  Logger LOGGER = LoggerFactory.getLogger(CreditCardService.class);

  public void chargeAmount(String cardNumber, String cvc, String expiryDate, Double openAmount)
    throws InvalidCreditCardException {
    if (expiryDate.length() == 5) {
      LOGGER.info(
        "Credit card number: " + cardNumber + ", CVC: " + cvc + ", Expiry date: " + expiryDate
          + ", Open amount: " + openAmount);
    } else {
      LOGGER.error("The credit card's expiry date is invalid: " + expiryDate);

      throw new InvalidCreditCardException();
    }
  }
}