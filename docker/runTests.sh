#!/bin/bash

nohup mvn clean compile test-compile exec:java@catalogue-server & 
sleep 20
mvn clean test-compile surefire:test surefire-report:report -Dtest=ApiServerVerticlePreprareTest
