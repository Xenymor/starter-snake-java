#!/bin/bash

cd /root/starter-snake-java/

git fetch origin

LOCAL_HASH=$(git rev-parse HEAD)
REMOTE_HASH=$(git rev-parse origin/master)

if [ "$LOCAL_HASH" != "$REMOTE_HASH" ]; then
        echo "Changes detected!"
        log_message "Changes detected!"
        git pull

        echo "Killing all other java processes ..."
        killall java

        echo "Building ..."
        cd src/main/java
        pwd
        /usr/bin/javac -d /root/starter-snake-java/build/classes -cp "../../libs/*" `find ./ -name "*.java"`

        echo "Starting ..."
        cd ../../../build/classes
        pwd
        nohup java -cp "../../src/libs/*:." com.battlesnake.starter.Main >> /dev/null &

        echo "Finished"

else
        echo "No Changes detected."
fi
