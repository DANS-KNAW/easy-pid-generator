#!/bin/sh
#  
# Script for running EASY PID Generator Service in the foreground. Only intended
# for troubleshooting purposes. Use the service script in the install directory
# to start it as a daemon.
#

java -Dlogback.configurationFile=$EASY_PID_GENERATOR_HOME/cfg/logback.xml \
     -cp $EASY_PID_GENERATOR_HOME/bin/jetty-runner.jar org.mortbay.jetty.runner.Runner \
     --port {{ easy_pid_generator_port }} $EASY_PID_GENERATOR_HOME/bin/easy-pid-generator.war 
