easy-pid-generator
==================
[![Build Status](https://travis-ci.org/DANS-KNAW/easy-pid-generator.png?branch=master)](https://travis-ci.org/DANS-KNAW/easy-pid-generator)

SYNOPSIS
--------

    easy-pid-generator generate {doi|urn}
    easy-pid-generator initialize {doi|urn} <seed>
    easy-pid-generator run-service


DESCRIPTION
-----------

Generate a Persistent Identifier (DOI or URN)


ARGUMENTS
---------

    Options:
      --help      Show help message
      --version   Show version of this program
    
    Subcommand: generate - Generate a specified PID
          --help   Show help message
    
     trailing arguments:
      pid-type (required)   The type of PID to be generated, either 'doi' or 'urn'
    ---
    
    Subcommand: initialize - Initialize a specified PID with a seed
          --help   Show help message
    
     trailing arguments:
      pid-type (required)   The type of PID to be generated, either 'doi' or 'urn'
      seed (required)       The seed to use for this initialization
    ---
    
    Subcommand: run-service - Starts the EASY Pid Generator as a daemon that services HTTP requests
          --help   Show help message
    ---
