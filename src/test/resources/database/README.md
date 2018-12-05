Test database
=============

This directory contains various files related to the database that is used while testing.


`database.sql`
--------------
This SQL script contains the commands to generate the tables/indexes/etc. for this database. It is
a partial copy of the script in `scr/main/assembly/dist/install/db-tables.sql`, that is more suitable
for testing purposes. **Make sure these scripts stay in sync, to avoid differences in the behavior
while testing and on production!**

The script is mainly used to generate a database during the unit tests (see `src/test/scala`).
However, it is also used to create a database for testing with the [`run.sh` scripts]. See
[`generateHsqldbScripts.sh`](#generateHsqldbScripts.sh) for more info.


`db.properties` and `db.script`
-------------------------------
When testing with the [`run.sh` scripts], a database is required. `db.properties` and `db.script`
are the basis of this database and will be copied by `debug-init-env.sh` to the `data` directory
when setting up the test environment. These scripts are pre-generated based on the aforementioned
`database.sql` file.


`generateHsqldbScripts.sh`
--------------------------
Running this script will make sure that `db.properties` and `db.script` will be in sync with
`database.sql`. This is typically done when the latter changes. **Note that this script is run from
the project's root directory; _NOT_ from this directory.**

```
./src/test/resources/database/generateHsqldbScripts.sh
```


[`run.sh` scripts]: https://github.com/DANS-KNAW/dans-dev-tools/
