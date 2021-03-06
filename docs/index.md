---
title: Manual
layout: home
---

easy-pid-generator
==================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-pid-generator.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-pid-generator)

SYNOPSIS
--------

    easy-pid-generator exists {doi|urn} <identifier>
    easy-pid-generator generate {doi|urn}
    easy-pid-generator initialize {doi|urn} <seed>
    easy-pid-generator run-service


DESCRIPTION
-----------

Generate a Persistent Identifier (DOI or URN)


ARGUMENTS
---------

    Options:
      -h, --help      Show help message
      -v, --version   Show version of this program

    Subcommand: exists - Check if a specific PID has been minted by this easy-pid-generator
      -h, --help   Show help message

     trailing arguments:
      pid-type (required)   The type of the given PID, either 'doi' or 'urn'
      pid (required)        The PID to be checked
    ---

    Subcommand: generate - Generate a specified PID
       -h, --help   Show help message

     trailing arguments:
      pid-type (required)   The type of PID to be generated, either 'doi' or 'urn'
    ---

    Subcommand: initialize - Initialize a specified PID with a seed
       -h, --help   Show help message

     trailing arguments:
      pid-type (required)   The type of PID to be generated, either 'doi' or 'urn'
      seed (required)       The seed to use for this initialization
    ---

    Subcommand: run-service - Starts the EASY Pid Generator as a daemon that services HTTP requests
       -h, --help   Show help message
    ---


HTTP service
------------

The documentation of the HTTP interface can be <a href="../api.html" target="__blank">viewed in Swagger UI in a new tab</a>.


INSTALLATION AND CONFIGURATION
------------------------------
Currently this project is built as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/easy-pid-generator` and the configuration files to `/etc/opt/dans.knaw.nl/easy-pid-generator`. 

To install the module on systems that do not support RPM, you can copy and unarchive the tarball to the target host.
You will have to take care of placing the files in the correct locations for your system yourself. For instructions
on building the tarball, see next section.


BUILDING FROM SOURCE
--------------------

Prerequisites:

* Java 8 or higher
* Maven 3.3.3 or higher
* RPM

Steps:

        git clone https://github.com/DANS-KNAW/easy-pid-generator.git
        cd easy-pid-generator
        mvn install

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM 
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

    mvn clean install assembly:single
