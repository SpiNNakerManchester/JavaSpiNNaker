#!/bin/bash
startmonth=$1 startyear=$2 endmonth=$3 endyear=$4 envsubst < get_history.sql | docker exec -i spalloc-mysql-container bash -c 'cat > .temp.sql  && cat .temp.sql' 
./mysql_history.sh 
