#!/bin/bash

mvn install -DskipTests

function waitFor {
    PID=$1
    COUNTDOWN=48
    while true; do
        sleep 10
        if kill -0 $PID >/dev/null 2>&1; then
            if [ $COUNTDOWN > 0 ]; then
                COUNTDOWN=`expr $COUNTDOWN - 1`
                continue
            fi
            echo Process $PID is still running...
            SURE=`jps | grep surefire | cut -f 1 -d " "`
            echo Surefire PID: $SURE
            if [ -n "$SURE" ]; then
                jstack $SURE
            fi
            jps -v
        else
            echo Finished OK...
            return
        fi
    done
}

mvn -f strings test&
waitFor $!

mvn -f generic test&
waitFor $!
