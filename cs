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
  tag)
    cs_version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep '^[0-9]\.')
    tag_version=$(date +%Y%m%d%H%M)
    git tag $cs_version-$tag_version
    git push --tags
    echo "RELEASE/TAG: $cs_version-$tag_version"
    ;;
  package)
    [[ -f /etc/redhat-release ]] || echo "Opss... run this option only in RedHat OS. Exiting..." && exit 1
    [[ $# -ne 2 ]] && echo "You need to provide the tag from git... eg: $0 package 4.2.0-201402262000" && exit 1
    (cd packaging/centos63; ./package.sh -t $2)
    ;;
  createrepo)
    [[ -f /etc/redhat-release ]] || echo "Opss... run this option only in RedHat OS. Exiting..." && exit 1
    createrepo /mnt/root/repository/centos64/x86_64
    ;;
  *)
    echo "Usage: $0 {run|run-debug|compile|compile-quick|deploydb|populatedb|tag}"
    exit 2
esac
 
