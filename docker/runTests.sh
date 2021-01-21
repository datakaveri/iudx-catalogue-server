#!/bin/bash

nohup mvn clean compile test-compile exec:java@catalogue-server & 
sleep 20
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticlePreprareTest
mv target/jacoco.exec ApiServerVerticlePreprareTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticleTest
mv target/jacoco.exec ApiServerVerticleTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=DatabaseServiceTest
mv target/jacoco.exec DatabaseServiceTest.exec
mvn test-compile surefire:test surefire-report:report -Dtest=ServerVerticleDeboardTest
mv target/jacoco.exec ServerVerticleDeboardTest.exec
