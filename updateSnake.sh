#!/bin/bash

cd ~/starter-snake-java/

git fetch origin

LOCAL_HASH=$(git rev-parse HEAD)
REMOTE_HASH=$(git rev-parse origin/master)

if [ "$LOCAL_HASH" != "$REMOTE_HASH" ]; then
        echo "Changes detected!"
        git pull

        echo "Killing all other java processes ..."
        killall java

        ./run.sh

        echo "Finished"

else
        echo "No Changes detected."
fi
