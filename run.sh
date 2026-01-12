echo "Building ..."
cd src/main/java
pwd
/usr/bin/javac -d ~/starter-snake-java/build/classes -cp "../../libs/*" `find ./ -name "*.java"`

echo "Starting ..."
cd ../../../build/classes
pwd
nohup java -cp "../../src/libs/*:." com.battlesnake.starter.Main >> /dev/null &