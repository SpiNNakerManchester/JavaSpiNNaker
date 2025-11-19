# Copyright (c) 2025 The University of Manchester
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Use Ubuntu
FROM ubuntu:noble

RUN apt update && apt install -y openjdk-21-jdk-headless maven git libltdl7

# Set the working directory in the container
WORKDIR /app

# Checkout the Java Code
RUN git clone --depth 1 --branch docker-compose https://github.com/SpiNNakerManchester/JavaSpiNNaker

# Make the Java Code
RUN mvn -f JavaSpiNNaker -DskipTests=true -pl SpiNNaker-nmpiserv -am package

# Get the binary into place
RUN cp JavaSpiNNaker/SpiNNaker-nmpiserv/target/SpiNNakerNMPI.war .

# Specify the command to start things
CMD [ "java", "-Djava.net.preferIPv4Stack=true", "-Djava.awt.headless=true", "-jar", "SpiNNakerNMPI.war", "--spring.config.location=/app/nmpi.properties", "-" , "--server.port=9090" ]
