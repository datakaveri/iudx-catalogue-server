#!/bin/bash

# To be executed from project root
docker build -t iudx/cat-db:latest -f docker/db.dockerfile .
docker build -t iudx/cat-auth:latest -f docker/auth.dockerfile .
docker build -t iudx/cat-val:latest -f docker/val.dockerfile .
docker build -t iudx/cat-api:latest -f docker/api.dockerfile .
docker build -t iudx/cat-all:latest -f docker/all.dockerfile .
