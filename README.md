# CMSC 137 Project
## About
Our own version of Draw My Thing as final requirement for CMSC 137

## Requirements
The following should be installed on your system for the project to work.
1. Java / javac
2. protobuf / protoc

## How to Use
1. Obtain the source code
```
git clone https://github.com/krowitz/cmsc137project/
```
2. Use protobuf
```
export CLASSPATH=protobuf-java-3.6.1.jar:$CLASSPATH
```
3. Compile the java files
```
javac *.java 
```
4.1 Run the game server if it is not already running
```
java IllusGameServer
```
4.2 Run the game client
```
java GameClient
```
