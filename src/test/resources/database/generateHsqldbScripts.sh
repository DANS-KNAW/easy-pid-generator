#!/usr/bin/env bash

echo 'generating database script files for HSQLDB'

ROOT=src/test/resources/database
LIB=$ROOT/lib
SQLTOOLJAR=$LIB/sqltool.jar
HSQLDBJAR=$LIB/hsqldb.jar
RC=$LIB/db.rc

DB_PROPS_BACKUP=$ROOT/db-$(date  +"%Y-%m-%dT%H:%M:%S").properties
DB_SCRIPT_BACKUP=$ROOT/db-$(date  +"%Y-%m-%dT%H:%M:%S").script

mv $ROOT/db.properties $DB_PROPS_BACKUP
mv $ROOT/db.script $DB_SCRIPT_BACKUP

echo 'downloading tools'
mkdir $LIB
curl -s http://central.maven.org/maven2/org/hsqldb/sqltool/2.4.1/sqltool-2.4.1.jar > $SQLTOOLJAR
curl -s http://central.maven.org/maven2/org/hsqldb/hsqldb/2.4.1/hsqldb-2.4.1.jar > $HSQLDBJAR

echo 'urlid pidgen' > $RC
echo 'url jdbc:hsqldb:file:'$ROOT'/db;shutdown=true' >> $RC

echo 'generating database scripts'
# TODO rollback on failure
java -jar $SQLTOOLJAR --rcFile=$RC pidgen $ROOT/database.sql || exit 1

rm -rf $LIB
rm $DB_PROPS_BACKUP
rm $DB_SCRIPT_BACKUP

echo 'new database script files are generated'
