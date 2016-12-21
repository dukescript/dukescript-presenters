#!/bin/bash

mvn install -DskipTests

mvn test&
PID=$!

while true; do
    sleep 480
    if kill -0 $PID >/dev/null 2>&1; then
        echo Process $PID is still running...
        SURE=`jps | grep surefire | cut -f 1 -d " "`
        echo Surefire PID: $SURE
        if [ -n "$SURE" ]; then
            jstack $SURE
        fi
    else
        echo Finished OK...
        exit 0
    fi
done