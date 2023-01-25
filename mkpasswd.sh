#!/bin/bash
# Copyright (c) 2022 The University of Manchester
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
