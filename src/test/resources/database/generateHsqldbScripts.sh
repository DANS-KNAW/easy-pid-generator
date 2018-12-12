#!/usr/bin/env bash

echo 'generating database script files for HSQLDB'

ROOT=src/test/resources/database
LIB=$ROOT/lib
SQLTOOLJAR=$LIB/sqltool.jar
HSQLDBJAR=$LIB/hsqldb.jar
RC=$LIB/db.rc

DB_PROPS=$ROOT/db.properties
DB_SCRIPT=$ROOT/db.script

DB_PROPS_BACKUP=$ROOT/db-$(date  +"%Y-%m-%dT%H:%M:%S").properties
DB_SCRIPT_BACKUP=$ROOT/db-$(date  +"%Y-%m-%dT%H:%M:%S").script

# rename the original files to put them back later, if necessary
[ -f $DB_PROPS ] && mv $DB_PROPS $DB_PROPS_BACKUP
[ -f $DB_SCRIPT ] && mv $DB_SCRIPT $DB_SCRIPT_BACKUP

echo 'downloading tools'
# download necessary libraries
mkdir $LIB
curl -s http://central.maven.org/maven2/org/hsqldb/sqltool/2.4.1/sqltool-2.4.1.jar > $SQLTOOLJAR
curl -s http://central.maven.org/maven2/org/hsqldb/hsqldb/2.4.1/hsqldb-2.4.1.jar > $HSQLDBJAR

# write the db.rc file
echo 'urlid pidgen' > $RC
echo 'url jdbc:hsqldb:file:'$ROOT'/db;shutdown=true' >> $RC

echo 'generating database scripts'
# generate new database files
java -jar $SQLTOOLJAR --rcFile=$RC pidgen $ROOT/database.sql

if [[ $? -eq 0 ]]; then
    # remove lib directory and the original files that now have been replaced
    rm -rf $LIB
    [ -f $DB_PROPS_BACKUP ] && rm $DB_PROPS_BACKUP
    [ -f $DB_SCRIPT_BACKUP ] && rm $DB_SCRIPT_BACKUP

    echo 'new database script files are generated'
else
    echo 'generating database failed'

    # remove lib directory and newly generated files
    rm -rf $LIB
    [ -f $DB_PROPS ] && rm $DB_PROPS
    [ -f $DB_SCRIPT ] && rm $DB_SCRIPT

    # put back the original files
    [ -f $DB_PROPS_BACKUP ] && mv $DB_PROPS_BACKUP $DB_PROPS
    [ -f $DB_SCRIPT_BACKUP ] && mv $DB_SCRIPT_BACKUP $DB_SCRIPT

    echo 'changes have been rolled back'
fi
