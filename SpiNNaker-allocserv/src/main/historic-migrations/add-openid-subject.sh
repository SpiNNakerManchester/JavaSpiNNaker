#!/usr/bin/env bash

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

set -e

if [[ $# -ne 1 ]]; then
	echo "illegal number of arguments" >&2
	exit 1
fi

src_db_1="$1"

: ${SQLITE:=sqlite}
migrate_1="$(dirname "$0")/add-openid-subject.sql"

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
