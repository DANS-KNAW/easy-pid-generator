#!/usr/bin/env bash

NAME="easy-pid-generator"
EXEC="/usr/bin/jsvc"
APPHOME="/opt/easy-pid-generator"
JAVA_HOME="/usr/lib/jvm/jre"
CLASSPATH="$APPHOME/bin/$NAME.jar:`echo $APPHOME/lib/*.jar | sed 's/ /:/g'`"
CLASS="nl.knaw.dans.easy.pid.microservice.ServiceStarter"
ARGS=""
USER="easy_pid_generator"
PID="/var/run/$NAME.pid"
OUTFILE="/var/log/$NAME/$NAME.out"
ERRFILE="/var/log/$NAME/$NAME.err"

jsvc_exec() {
    cd $APPHOME
    $EXEC -home $JAVA_HOME -cp $CLASSPATH -user $USER -outfile $OUTFILE -errfile $ERRFILE -pidfile $PID \
          -Dapp.home=$APPHOME -Dlogback.configurationFile=$APPHOME/cfg/logback.xml $1 $CLASS $ARGS
}

case "$1" in
    start)
        echo "Starting $NAME ..."
        jsvc_exec
        echo "$NAME has started."
    ;;
    stop)
        echo "Stopping $NAME ..."
        jsvc_exec "-stop"
        echo "$NAME has stopped."
    ;;
    restart)
        if [ -f "$PID" ]; then
            echo "Restarting $NAME ..."
            jsvc_exec "-stop"
            jsvc_exec
            echo "$NAME has restarted."
        else
            echo "$NAME is not running, no action taken"
            exit 1
        fi
    ;;
    status)
        if [ -f "$PID" ]
        then
            echo "$NAME (pid `cat $PID`) is running"
        else
            echo "$NAME is stopped"
        fi
    ;;
    *)
    echo "Usage: sudo service $NAME {start|stop|restart|status}" >&2
    exit 3
esac
