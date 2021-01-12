#!/bin/bash

nohup mvn clean compile test-compile exec:java@catalogue-server & 
sleep 20
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticlePreprareTest
mvn test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticleTest
mvn test-compile surefire:test surefire-report:report -Dtest=DatabaseServiceTest
mvn test-compile surefire:test surefire-report:report -Dtest=ServerVerticleDeboardTest
