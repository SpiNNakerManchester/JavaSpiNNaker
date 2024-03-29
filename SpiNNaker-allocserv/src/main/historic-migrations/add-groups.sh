#!/usr/bin/env bash

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

set -e

if [[ $# -ne 2 ]]; then
	echo "illegal number of arguments" >&2
	exit 1
fi

src_db_1="$1"
src_db_2="$2"

: ${SQLITE:=sqlite}
migrate_1="$(dirname "$0")/add-groups.sql"
migrate_2="$(dirname "$0")/add-groups-historic.sql"

function modname {
	echo "$(dirname "$1")/mod-$(basename "$1")"
}
function applysql {
	cp "$1" "$2" && { $SQLITE "$2" <<EOF
.echo on
.bail on
.read $3
.exit
EOF
} || exit 2
}

applysql "$src_db_1" $(modname "$src_db_1") "$migrate_1"
applysql "$src_db_2" $(modname "$src_db_2") "$migrate_2"
