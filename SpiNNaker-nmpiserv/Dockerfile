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

ARG DOCKER_GROUP_ID
ARG USER_ID
ENV DOCKER_GROUP_ID $DOCKER_GROUP_ID
ENV USER_ID $USER_ID

RUN apt update && apt install -y openjdk-21-jdk-headless maven git libltdl7 nginx

COPY --chown=www-data nginx.conf /etc/nginx/nginx.conf

RUN groupadd -g $DOCKER_GROUP_ID docker
RUN usermod -a -G docker www-data

# Set the working directory in the container
WORKDIR /app

# Checkout the Java Code
RUN git clone --depth 1 --branch docker-compose https://github.com/SpiNNakerManchester/JavaSpiNNaker

# Make the Java Code
RUN mvn -f JavaSpiNNaker -DskipTests=true -pl SpiNNaker-nmpiserv -am package

# Get the binary into place
RUN cp JavaSpiNNaker/SpiNNaker-nmpiserv/target/SpiNNakerNMPI.war .

COPY --chmod=0755 startup.sh /app/startup.sh

# Specify the command to start things
CMD [ "/app/startup.sh" ]
