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

echo -n "Pre-creating log and database..."
DATADIR=data

mkdir data/database
cp src/test/resources/database/db.properties $DATADIR/database/db.properties
cp src/test/resources/database/db.script $DATADIR/database/db.script

touch $DATADIR/easy-pid-generator.log
echo "OK"
