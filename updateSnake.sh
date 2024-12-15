#!/bin/bash

LOG_FILE="./logfile.log"
# Function to log messages with timestamps
log_message() {
    echo "$(date '+%Y-%m-%d %H:%M:%S') - $1" >> "$LOG_FILE"
}

cd /root/starter-snake-java/

git fetch origin

LOCAL_HASH=$(git rev-parse HEAD)
REMOTE_HASH=$(git rev-parse origin/master)

if [ "$LOCAL_HASH" != "$REMOTE_HASH" ]; then
        echo "Changes detected!"
        log_message "Changes detected!"
        git pull

        echo "Killing all other java processes ..."
        log_message "Killing all other java processes ..."
        killall java

        echo "Building ..."
        log_message "Building ..."
        cd src/main/java
        pwd
        /usr/bin/javac -d /root/scripts/build/classes -cp "../../libs/*" `find ./ -name "*.java"`

        echo "Starting ..."
        log_message "Starting ..."
        cd ../../../build/classes
        pwd
        nohup java -cp "../../src/libs/*:." com.battlesnake.starter.Snake >> ~/starter-snake-java/logs/output.log &

        echo "Finished"
        log_message "Finished"

else
        echo "No Changes detected."
fi
