#!/bin/bash

vers=`$JAVA_HOME/bin/java -version 2>&1 | grep "java version" | awk '{print $3}' | tr -d \"`
echo "using java $vers from $JAVA_HOME"

SCRIPT_HOME="$( cd -P "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

OSM=$1
GRAPH=$(basename "$OSM")
GRAPH="${GRAPH%.*}"-gh

JAVA_OPTS_IMPORT="-XX:PermSize=20m -XX:MaxPermSize=20m -Xmx1200m -Xms1200m"
SIZE=5000000     
GH_JAR_DIR=../graphhopper
JAR=$GH_JAR_DIR/target/graphhopper-1.0-SNAPSHOT-jar-with-dependencies.jar

if [ ! -d "$GH_JAR_DIR" ]; then
 echo "graphhopper folder not found? $GH_JAR_DIR"
 echo "try cloning graphhopper core into $GH_JAR_DIR before and run again"
 exit
fi

if [ ! -f "$JAR" ]; then
  echo "## now building graphhopper jar: $JAR"
  cd $GH_JAR_DIR && mvn -DskipTests=true assembly:assembly > /dev/null
  cd $SCRIPT_HOME
else
  echo "## existing jar found $JAR"
fi

#if [ ! -d "$GRAPH" ]; then
  echo "## now creating graph $GRAPH from $OSM,  java opts=$JAVA_OPTS_IMPORT"
  echo "## HINT: put the osm on an external usb drive which should speed up import time"
  $JAVA_HOME/bin/java $JAVA_OPTS_IMPORT -cp $JAR de.jetsli.graph.reader.OSMReader graph=$GRAPH osm=$OSM size=$SIZE dataaccess=mmap
#else
#  echo "## graph already exists at $GRAPH"
#fi
