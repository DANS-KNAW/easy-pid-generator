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

When started with the subcommand `run-service` a REST API becomes available.

### GET (service status)
_URI:_ `http://test.dans.knaw.nl:20140/`

Return a simple message to indicate that the service is up and running

#### Response body
a short message indicating the service is up and running

#### Response statuses
`200 OK` - service is up and running


### GET (check existence of PID)
_URI:_ `http://test.dans.knaw.nl:20140/{doi|urn}/{pid}`

Checks if the given PID is already minted

#### Response body
when the PID was already minted - nothing
when the PID was not yet minted - a short message indicating this fact

#### Response statuses
`204 No Content` - PID was already minted
`404 Not Found` - PID was not yet minted


### POST (generate new PID)
_URI:_ `http://test.dans.knaw.nl:20140/create?type={doi|urn}`

Generates and mints a new PID of the kind specified by `type`

#### Response body
the generated PID

#### Response statuses
`201 Created` - operation successful<br>
`400 Bad Request` - `type` must be either `doi` or `urn`<br>
`500 Internal Server Error` - server internal error, try later and if problem persists please contact us


### POST (initialize the generator for a certain kind of PID)
_URI:_ `http://test.dans.knaw.nl:20140/init?type={doi|urn}&seed={s}` where {s} is a (64-bit) integer seed

Initializes a certain kind of PID with a seed, such that new PIDs of this kind can be generated

#### Response body
a message to confirm a successful initialization

#### Response statuses
`201 Created` - operation successful<br>
`400 Bad Request` - `type` must be either `doi` or `urn`, `seed` must be a (64-bit) integer<br>
`409 Confict` - this kind of PID is already initialized<br>
`500 Internal Server Error` - server internal error, try later and if problem persists please contact us


EXAMPLES
--------

    curl http://test.dans.knaw.nl:20140/
    curl http://test.dans.knaw.nl:20140/create?type=doi
    curl http://test.dans.knaw.nl:20140/init?type=doi&seed=1073741824
