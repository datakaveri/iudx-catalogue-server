#!/bin/bash

nohup mvn clean compile test-compile exec:java@catalogue-server & 
sleep 20
mvn test -Dtest=ApiServerVerticlePreprareTest
