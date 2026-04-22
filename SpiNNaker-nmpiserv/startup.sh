#!/bin/bash
service nginx start
java -Djava.net.preferIPv4Stack=true -Djava.awt.headless=true -jar /app/SpiNNakerNMPI.war --spring.config.location=/app/nmpi.properties - --server.port=9090
service nginx stop
