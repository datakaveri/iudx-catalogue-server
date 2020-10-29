#!/bin/bash

# To be executed from project root
docker build -t iudx/cat-depl:latest -f docker/depl.dockerfile .
docker build -t iudx/cat-dev:latest -f docker/dev.dockerfile .
docker build -t iudx/cat-test:latest -f docker/test.dockerfile .
