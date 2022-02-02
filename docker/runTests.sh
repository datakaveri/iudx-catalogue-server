#!/bin/bash

nohup mvn clean compile exec:java@catalogue-server & 
sleep 20
mvn clean test -Dtest=ServerVerticleDeboardTest
mvn test -Dtest=ApiServerVerticlePreprareTest
mvn test -Dtest=ApiServerVerticleTest
mvn test -Dtest=ServerVerticleDeboardTest
mvn test -Dtest=DatabaseServiceTest
mv target/jacoco.exec target/DatabaseServiceTest.exec
mvn test -Dtest=ConstraintsValidationTest,AuthenticationServiceTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,ValidatorServiceTest
mv target/jacoco.exec target/jacoco2.exec
