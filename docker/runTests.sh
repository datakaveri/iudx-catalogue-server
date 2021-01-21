#!/bin/bash

nohup mvn clean compile test-compile exec:java@catalogue-server & 
sleep 20
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticlePreprareTest
mv target/jacoco.exec target/ApiServerVerticlePreprareTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticleTest
mv target/jacoco.exec target/ApiServerVerticleTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=DatabaseServiceTest
mv target/jacoco.exec target/DatabaseServiceTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=ServerVerticleDeboardTest
mv target/jacoco.exec target/ServerVerticleDeboardTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=ConstraintsValidationTest,AuthenticationServiceTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,ValidatorServiceTest
