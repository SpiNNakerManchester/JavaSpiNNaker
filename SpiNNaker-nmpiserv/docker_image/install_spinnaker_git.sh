#!/bin/bash
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
# Install the release version
version=$1
pyver=$(python -c "import sys; print(f'{sys.version_info.major}.{sys.version_info.minor}');")
VENV_PATH=$HOME/spinnaker/
python -m venv $VENV_PATH
cp /opt/spinnaker/activate $VENV_PATH/bin/
source $VENV_PATH/bin/activate
pip install --upgrade wheel setuptools pip
cd $VENV_PATH
git clone https://github.com/SpiNNakerManchester/SupportScripts ./support
git clone https://github.com/SpiNNakerManchester/SpiNNUtils
cd SpiNNUtils && python setup.py develop && cd ..
./support/install.sh all -y
./support/gitcheckout.sh docker-compose
./support/setup.sh
./support/automatic_make.sh
mvn -f JavaSpiNNaker -DskipTests clean package
python -m spynnaker.pyNN.setup_pynn
