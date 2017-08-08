#!/usr/bin/env bash
#
# Copyright (C) 2015 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e # abort when a command fails

TEMPDIR=data

if [ -e ${TEMPDIR} ]; then
    mv ${TEMPDIR} ${TEMPDIR}-$(date  +"%Y-%m-%d@%H:%M:%S")
fi
mkdir ${TEMPDIR}


echo -n "creating seed.db..."
sqlite3 $TEMPDIR/seed.db < src/test/resources/database/seed.sql

echo -n "Pre-creating log..."
touch $TEMPDIR/easy-pid-generator.log
echo "OK"
