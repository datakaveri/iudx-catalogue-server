#!/bin/bash

nohup mvn clean compile test-compile exec:java@catalogue-server & 
sleep 20
mvn test -Dtest=ApiServerVerticlePreprareTest
mvn test -Dtest=ApiServerVerticleTest
mvn test -Dtest=DatabaseServiceTest
mvn test -Dtest=ServerVerticleDeboardTest
