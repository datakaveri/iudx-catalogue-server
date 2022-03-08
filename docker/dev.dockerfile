ARG VERSION="0.0.1-SNAPSHOT"

FROM maven:3-openjdk-11-slim as builder

WORKDIR /usr/share/app
COPY pom.xml .
RUN mvn clean package
COPY src src
RUN mvn clean package -Dmaven.test.skip=true


FROM openjdk:11-jre-slim-buster

ARG VERSION
ENV JAR="iudx.catalogue.server-dev-${VERSION}-fat.jar"

WORKDIR /usr/share/app
COPY docs docs
COPY --from=builder /usr/share/app/target/${JAR} ./fatjar.jar
RUN useradd -r -u 1001 -g root catuser
USER catuser
