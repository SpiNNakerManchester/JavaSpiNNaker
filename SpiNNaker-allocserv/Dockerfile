# Copyright (c) 2025 The University of Manchester
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.

# Use Ubuntu
FROM ubuntu:noble

RUN apt update && apt install -y openjdk-21-jdk-headless maven git ping

# Set the working directory in the container
WORKDIR /app

# Checkout the Java Code
RUN git clone --depth 1 https://github.com/SpiNNakerManchester/JavaSpiNNaker

# Make the Java Code
RUN mvn -f JavaSpiNNaker -DskipTests=true -pl SpiNNaker-allocserv -am package

# Get the binary into place
RUN cp JavaSpiNNaker/SpiNNaker-allocserv/target/spalloc.war .

# Specify the command to start things
CMD [ "java", "-Djava.net.preferIPv4Stack=true", "--add-opens=java.base/java.util.concurrent=ALL-UNNAMED", "--add-opens=java.base/java.lang=ALL-UNNAMED", "-jar", "spalloc.war" ]
