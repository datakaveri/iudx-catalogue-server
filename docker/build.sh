#!/bin/bash

# To be executed from project root
docker build -t iudx/cat-prod:latest -f docker/prod.dockerfile .
docker build -t iudx/cat-dev:latest -f docker/dev.dockerfile .
docker build -t iudx/cat-test:latest -f docker/test.dockerfile .
