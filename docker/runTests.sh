#!/bin/bash

nohup mvn clean compile exec:java@catalogue-server & 
sleep 40
<<<<<<< HEAD
mvn clean test
=======
mvn clean test -Dtest=ServerVerticleDeboardTest
mvn test -Dtest=ApiServerVerticlePreprareTest
mvn test -Dtest=ApiServerVerticleTest
mvn test -Dtest=ServerVerticleDeboardTest
mvn test -Dtest=ConstraintsValidationTest,ExceptionHandlerTest,QueryMapperTest,RespBuilderTest,QueryBuilderTest,AuthorizationRequestTest,JwtDataTest,DatabaseServiceImplTest,ElasticClientTest,QueryDecoderTest,SummarizerTest,DataBrokerServiceTest,GeocodingServiceTest,RatingServiceTest,ValidatorServiceTest,AuthenticationServiceTest,ValidatorServiceTest,RatingServiceTest,AuthenticationServiceImplTest,NLPSearchServiceImplTest,ValidatorServiceImplTest,AuditingServiceTest,JwtAuthServiceImplTest
>>>>>>> 3c020be738ecd0fb4aeefb15c0576b28ea5e78d8
