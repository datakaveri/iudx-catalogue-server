# Dockerfile for voc-server

FROM openjdk:11

RUN apt update && apt install git --assume-yes \
    && apt install maven --assume-yes \
    && apt install python3 --assume-yes \
    && apt install python3-pip --assume-yes \
    && pip3 install requests
