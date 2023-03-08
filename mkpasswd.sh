#!/bin/bash
# Copyright (c) 2022 The University of Manchester
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
