package com.camunda.academy;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Properties;
import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.camunda.academy.handlers.CreditCardChargingHandler;
import com.camunda.academy.handlers.CreditDeductionHandler;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.worker.JobWorker;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProvider;
import io.camunda.zeebe.client.impl.oauth.OAuthCredentialsProviderBuilder;

public class PaymentApplication {

    private static final Logger logger = LoggerFactory.getLogger(PaymentApplication.class);

    //Zeebe Client Credentials
    private static final String ZEEBE_PROPERTIES_PATH = "src/main/resources/application.properties";
    private static String ZEEBE_CLIENT_ID;
    private static String ZEEBE_CLIENT_SECRET;
    private static String ZEEBE_TOKEN_AUDIENCE;
    private static String ZEEBE_REST_ADDRESS;
    private static String ZEEBE_GRPC_ADDRESS;

    //Payment Application Details
    private static final int WORKER_TIMEOUT = 10;

    public static void main(String[] args){
        loadProperties();
        final OAuthCredentialsProvider credentialsProvider =
            new OAuthCredentialsProviderBuilder()
                .audience(ZEEBE_TOKEN_AUDIENCE)
                .clientId(ZEEBE_CLIENT_ID)
                .clientSecret(ZEEBE_CLIENT_SECRET)
                .build();

        try (final ZeebeClient client =
            ZeebeClient.newClientBuilder()
                .grpcAddress(URI.create(ZEEBE_GRPC_ADDRESS))
                .restAddress(URI.create(ZEEBE_REST_ADDRESS))
                .credentialsProvider(credentialsProvider)
                .build()) {

            //Start the Credit Deduction Worker
            final JobWorker creditDeductionWorker =
                client.newWorker()
                    .jobType("credit-deduction")
                    .handler(new CreditDeductionHandler())
                    .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                    .open();

            //Start the Credit Deduction Worker
            final JobWorker creditCardChargingWorker =
                client.newWorker()
                    .jobType("credit-card-charging")
                    .handler(new CreditCardChargingHandler())
                    .timeout(Duration.ofSeconds(WORKER_TIMEOUT).toMillis())
                    .open();

            //Wait for the Workers
            Scanner sc = new Scanner(System.in);
            sc.nextInt();
            sc.close();
            creditDeductionWorker.close();
            creditCardChargingWorker.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(ZEEBE_PROPERTIES_PATH)) {
            properties.load(input); 
            ZEEBE_CLIENT_ID = properties.getProperty("zeebe.client.cloud.clientId");
			ZEEBE_CLIENT_SECRET = properties.getProperty("zeebe.client.cloud.clientSecret");
			ZEEBE_REST_ADDRESS = properties.getProperty("ZEEBE_REST_ADDRESS");
    		ZEEBE_GRPC_ADDRESS = properties.getProperty("ZEEBE_GRPC_ADDRESS");
			ZEEBE_TOKEN_AUDIENCE = properties.getProperty("ZEEBE_TOKEN_AUDIENCE");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}