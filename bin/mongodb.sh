#!/bin/bash

docker run --name mongodb  -p 27017-27019:27017-27019 -d mongo:latest