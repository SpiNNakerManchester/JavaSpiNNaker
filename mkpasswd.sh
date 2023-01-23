#!/bin/bash
set -- $(locale LC_MESSAGES)
yesexpr="$1"; noexpr="$2";
read -p "New username: " USERNAME
read -p "New password: " -s PASSWORD
echo ""
while true; do
    read -p "Admin (Yy/Nn)? " ADMIN
    if [[ $ADMIN =~ $yesexpr ]]; then 
        USERTRUST=3
        break; 
    fi
    if [[ $ADMIN =~ $noexpr ]]; then
        USERTRUST=2
    fi
done

ENCPW=$(htpasswd -bnBC 10 "" $PASSWORD | tr -d ':\n' | sed 's/$2y/$2a/')
echo $ENCPW

read -p "MySQL database: " MYSQLDB
read -p "MySQL username: " MYSQLUSER
read -p "MySQL password: " -s MYSQLPASS

mysql -D $MYSQLDB -u $MYSQLUSER -p$MYSQLPASS -e "INSERT INTO user_info(user_name, encrypted_password, trust_level) VALUES ('$USERNAME', '$ENCPW', $USERTRUST);"
