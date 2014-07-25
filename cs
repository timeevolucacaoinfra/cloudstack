#!/bin/bash

case "$1" in
  run)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -pl :cloud-client-ui jetty:run
    ;;
  run-simulator)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -pl :cloud-client-ui jetty:run -Dsimulator
    ;;
  compile)
    mvn -P developer,systemvm clean install -DskipTests
    ;;
  compile-quick)
    mvn -P developer,systemvm -pl :cloud-server,:cloud-api,:cloud-plugin-network-networkapi,:cloud-plugin-network-globodns,:cloud-client-ui clean install -DskipTests
    ;;
  compile-simulator)
    mvn -Pdeveloper -Dsimulator clean package
    ;;
  deploydb)
    mvn -P developer -pl developer,tools/devcloud -Ddeploydb
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator
    ;;
  populatedb)
    python tools/marvin/marvin/deployDataCenter.py -i tools/marvin/marvin/cloudstack-local.cfg
    ;;
  db-migrate)
    [[ -z $WORKON_HOME ]] && WORKON_HOME=$HOME/.virtualenvs
    source $WORKON_HOME/cloudstack/bin/activate
    (cd setup/dbmigrate && db-migrate --env=localhost)
    ;;
  tag)
    cs_version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep '^[0-9]\.')
    tag_version=$(date +%Y%m%d%H%M)
    git tag $cs_version-$tag_version
    git push --tags
    echo "RELEASE/TAG: $cs_version-$tag_version"
    ;;
  package)
    [[ ! -f /etc/redhat-release ]] && echo "Opss... run this option only in RedHat OS. Exiting..." && exit 1
    [[ $# -ne 2 ]] && echo "You need to provide the tag from git... eg: $0 package 4.2.0-201402262000" && exit 1
    (cd packaging/centos63; ./package.sh -t $2)
    if [[ $? -eq 0 ]]; then
      if [[ -d /mnt/root/repository/centos64/x86_64 ]];then 
          echo -n "Copying files /root/cloudstack/dist/rpmbuild/RPMS/x86_64/cloudstack-[a-z]*-${2}.el6.x86_64.rpm to /mnt/root/repository/centos64/x86_64..."
          mv /root/cloudstack/dist/rpmbuild/RPMS/x86_64/cloudstack-[a-z]*-${2}.el6.x86_64.rpm /mnt/root/repository/centos64/x86_64/
          echo "done"
          createrepo -v /mnt/root/repository/centos64/x86_64
      else
          echo "The directory /mnt/root/repository/centos64/x86_64 does not exist... exiting."
      fi
    else
      echo "Please, fix the errors."
      exit 1
    fi
    ;;
  lazy)
    echo "In construction, bye!!!"
    exit 1
    ;;
  *)
    echo "Usage: $0 [action]
Actions:

RUN
    run               Run Jetty server in DEBUG mode
    run-simulator     Run Jetty w/ hypervisor simulator
    
COMPILE
    compile           Compile cloudstack
    compile-quick     Compile only globo elements
    compile-simulator Compile cloudstack w/ hypervisor simulator

DB
    deploydb          Create Required SQL Schema
    populatedb        Create a basic infrastructure on cloudstack
    db-migrate        SQL migrations

    tag               Create a git TAG
    package           Build RPM packages for cloudstack (management, usage, awsapi, common, etc) and create yum repo
    lazy              Do everything you need to run cloudstack on your machine
"
    exit 2
    ;;
esac
 
