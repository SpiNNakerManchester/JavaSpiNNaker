#!/bin/bash
docker compose -f JavaSpiNNaker/SpiNNaker-nmpiserv/docker-compose.yml --env-file /home/nmpi/local.env build
