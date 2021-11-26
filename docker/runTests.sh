#!/bin/bash

nohup mvn clean compile exec:java@catalogue-server & 
sleep 20
mvn clean test -Dtest=ServerVerticleDeboardTest
mvn clean test -Dtest=ApiServerVerticlePreprareTest
mv target/jacoco.exec target/ApiServerVerticlePreprareTest.exec
mvn clean test -Dtest=ApiServerVerticleTest
mv target/jacoco.exec target/ApiServerVerticleTest.exec
#mvn test-compile surefire:test surefire-report:report -Dtest=DatabaseServiceTest
#mv target/jacoco.exec target/DatabaseServiceTest.exec
#mvn test-compile surefire:test surefire-report:report -Dtest=ConstraintsValidationTest,AuthenticationServiceTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,ValidatorServiceTest
#mv target/jacoco.exec target/jacoco2.exec
mvn clean test -Dtest=ServerVerticleDeboardTest
mv target/jacoco.exec target/ServerVerticleDeboardTest.exec
