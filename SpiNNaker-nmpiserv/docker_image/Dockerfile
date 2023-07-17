# Copyright (c) 2023 The University of Manchester
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

FROM ubuntu:jammy

USER root

RUN apt-get update -qq && apt-get install -yq --no-install-recommends software-properties-common gpg-agent
RUN add-apt-repository ppa:openjdk-r/ppa
RUN add-apt-repository ppa:deadsnakes/ppa
RUN apt-get update -qq && \
	DEBIAN_FRONTEND=noninteractive apt-get install -yq --no-install-recommends \
	ca-certificates curl git python3.10 python3.10-tk python3-distutils gcc-arm-none-eabi libnewlib-arm-none-eabi make \
	openjdk-11-jdk maven locales locales-all iputils-ping tzdata graphviz libsqlite3-dev freeglut3-dev \
	rsync g++ libstring-crc32-perl binutils lib32z1 netbase wget jq unzip

RUN locale-gen en_GB.UTF-8
ENV LANG C.UTF-8
ENV TZ="Etc/UTC"
RUN update-locale LANG=en_GB.UTF-8

RUN rm /bin/sh && ln -s /bin/bash /bin/sh
RUN curl -O https://bootstrap.pypa.io/get-pip.py
RUN python3.10 get-pip.py
RUN pip3.10 install virtualenv
RUN addgroup --gid 115 spinnaker
RUN adduser --uid 112 --gid 115 spinnaker
RUN ln -s /usr/bin/python3.10 /usr/bin/python
RUN ln -s /usr/bin/pip3.10 /usr/bin/pip

USER spinnaker
WORKDIR /home/spinnaker
RUN python -m virtualenv spinnaker
ADD --chown=spinnaker:spinnaker activate /home/spinnaker/spinnaker/bin/
WORKDIR /home/spinnaker/spinnaker
RUN git clone https://github.com/SpiNNakerManchester/SupportScripts.git support
RUN support/install.sh 8 -y
RUN git clone https://github.com/SpiNNakerManchester/JavaSpiNNaker
RUN support/gitcheckout.sh quota_remotespinnaker_docker
RUN /bin/bash -c "source bin/activate && pip install 'numpy == 1.21.6' 'scipy == 1.10.0'"
RUN /bin/bash -c "source bin/activate && cd SpiNNUtils && python setup.py develop"
RUN /bin/bash -c "source bin/activate && pip install matplotlib"
RUN /bin/bash -c "source bin/activate && support/automatic_make.sh"
RUN /bin/bash -c "source bin/activate && support/setup.sh"
RUN /bin/bash -c "source bin/activate && python -m spynnaker.pyNN.setup_pynn"
RUN mvn -f JavaSpiNNaker -pl -SpiNNaker-allocserv -DskipTests package

ADD --chown=spinnaker:spinnaker --chmod=0755 run_executor.sh /home/spinnaker/run_executor.sh
ADD --chown=spinnaker:spinnaker --chmod=0755 start_simulation.sh /home/spinnaker/start_simulation.sh
ADD --chown=spinnaker:spinnaker --chmod=0644 .spynnaker.cfg /home/spinnaker/.spynnaker.cfg

WORKDIR /home/spinnaker
ENTRYPOINT [ "/home/spinnaker/start_simulation.sh" ]
