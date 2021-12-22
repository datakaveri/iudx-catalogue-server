#!/bin/bash

nohup mvn clean compile exec:java@catalogue-server & 
sleep 20
mvn test-compile surefire:test surefire-report:report -Dtest=ServerVerticleDeboardTest
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticlePreprareTest
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticleTest
mvn test-compile surefire:test surefire-report:report -Dtest=ServerVerticleDeboardTest
mvn test-compile surefire:test surefire-report:report -Dtest=DatabaseServiceTest
mv target/jacoco.exec target/DatabaseServiceTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=ConstraintsValidationTest,AuthenticationServiceTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,ValidatorServiceTest
mv target/jacoco.exec target/jacoco2.exec
