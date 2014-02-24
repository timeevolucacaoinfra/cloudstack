#!/bin/bash

case "$1" in
  run)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m" mvn -pl :cloud-client-ui jetty:run
    ;;
  run-debug)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -pl :cloud-client-ui jetty:run
    ;;
  compile)
    mvn -P developer,systemvm clean install -DskipTests
    ;;
  compile-quick)
    mvn -P developer,systemvm -pl :cloud-server,:cloud-api,:cloud-plugin-network-networkapi,:cloud-client-ui clean install -DskipTests
    ;;
  deploydb)
    mvn -P developer -pl developer,tools/devcloud -Ddeploydb
    ;;
  populatedb)
    python tools/marvin/marvin/deployDataCenter.py -i tools/marvin/marvin/cloudstack-local.cfg
    ;;
  *)
    echo "Usage: $0 {run|run-debug|compile|compile-quick|deploydb|populatedb}"
    exit 2
esac
 
