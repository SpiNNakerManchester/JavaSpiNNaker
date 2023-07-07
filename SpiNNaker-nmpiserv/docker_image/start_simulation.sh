#!/bin/bash
url=$1
shift

if [ $url ]
then
	echo "Starting NMPI Client from $url"
	cd /home/spinnaker/
	/usr/bin/wget $url
	/home/spinnaker/run_executor.sh $@
	echo "Simulation Complete - exiting"
else
	echo "NMPI URL missing - not starting"
fi
