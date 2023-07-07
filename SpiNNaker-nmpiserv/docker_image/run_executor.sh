source /home/spinnaker/spinnaker/bin/activate
echo $VIRTUAL_ENV
echo $@
/usr/bin/java -Djava.awt.headless=true $@ >& run.out
