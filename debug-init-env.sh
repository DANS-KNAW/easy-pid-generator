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
TEMPDIR=data

cp src/test/resources/database/database.sql home/install/db-tables.sql

mkdir data/database
echo 'urlid pidgen' > data/database/db.rc
echo 'url jdbc:hsqldb:file:data/database/db;shutdown=true' >> data/database/db.rc

java -jar lib/sqltool.jar --rcFile=data/database/db.rc pidgen home/install/db-tables.sql

touch $TEMPDIR/easy-pid-generator.log
echo "OK"
