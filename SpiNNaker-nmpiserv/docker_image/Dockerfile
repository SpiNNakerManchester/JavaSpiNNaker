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

FROM quay.io/jupyter/minimal-notebook

LABEL maintainer="SpiNNaker Team"

ENV USER="${NB_USER}" \
    SHELL=/bin/bash \
    SSL_CERT_DIR=/etc/ssl/certs \
    XDG_CACHE_HOME=/tmp/cache/ \
    LAB_IMAGE_NAME=SpiNNaker

USER root

# Set the UK timezone (necessary to make r-base install noninteractive)
RUN ln -sf /usr/share/zoneinfo/Europe/London /etc/localtime

# Set to noninteractive for install
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update --yes && \
    apt-get install --yes \
    gcc \
    lsof \
    gcc-arm-none-eabi \
    openjdk-17-jdk \
    maven \
    ffmpeg \
    curl \
    make \
    vim  && \
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Install nodejs
ENV NODE_VERSION=16.13.0
ENV NVM_DIR=/root/.nvm
RUN mkdir $NVM_DIR && curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.40.2/install.sh | bash
RUN . "$NVM_DIR/nvm.sh" && \
    nvm install ${NODE_VERSION} && \
    nvm use v${NODE_VERSION} && \
    nvm alias default v${NODE_VERSION}
ENV PATH="/root/.nvm/versions/node/v${NODE_VERSION}/bin/:${PATH}"

# disable core dumps
RUN echo "* - core 0" >> /etc/security/limits.conf

# Setup install scripts
RUN mkdir -p /opt/spinnaker
COPY --chmod=755 activate /opt/spinnaker/activate
COPY --chmod=755 install_spinnaker_git.sh /opt/spinnaker/install_spinnaker_git.sh

WORKDIR $HOME
USER ${NB_UID}

ENV MPLCONFIGDIR=$HOME/.config/matplotlib
RUN mkdir -p $MPLCONFIGDIR

# Install git kernel
RUN /opt/spinnaker/install_spinnaker_git.sh

ADD --chown=spinnaker:spinnaker --chmod=0755 run_executor.sh $HOME/run_executor.sh
ADD --chown=spinnaker:spinnaker --chmod=0755 start_simulation.sh $HOME/start_simulation.sh
ADD --chown=spinnaker:spinnaker --chmod=0644 .spynnaker.cfg $HOME/.spynnaker.cfg

ENTRYPOINT [ "./start_simulation.sh" ]
