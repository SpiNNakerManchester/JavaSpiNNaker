docker exec -it spalloc-mysql-container /usr/bin/mysql -D spalloc_history -u spalloc -p -e "source .temp.sql"
