#!/bin/bash
docker compose -f JavaSpiNNaker/SpiNNaker-allocserv/docker-compose.yml --env-file /home/spalloc/local.env up -d
