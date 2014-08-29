#!/bin/bash

case "$1" in
  run)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -pl :cloud-client-ui jetty:run
    ;;
  run-simulator)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -Dsimulator -pl :cloud-client-ui jetty:run
    ;;
  compile)
    mvn -Pdeveloper,systemvm -Dsimulator clean install -DskipTests
    ;;
  compile-quick)
    mvn -Pdeveloper,systemvm -Dsimulator -pl :cloud-server,:cloud-api,:cloud-plugin-network-globonetwork,:cloud-plugin-network-globodns,:cloud-plugin-user-authenticator-oauth2,:cloud-client-ui install -DskipTests
    ;;
  deploydb)
    mvn -Pdeveloper -pl developer,tools/devcloud -Ddeploydb
    ;;
  deploydb-simulator)
    mvn -Pdeveloper -pl developer -Ddeploydb
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator
    ;;
  populatedb)
    python tools/marvin/marvin/deployDataCenter.py -i setup/dev/local-globo-xen.cfg
    ;;
  populatedb-simulator)
    python tools/marvin/marvin/deployDataCenter.py -i setup/dev/local-globo-sim.cfg
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
    # compile
    # if $?
    [[ $2 == 'compile' ]] && mvn -Pdeveloper -Dsimulator clean package -DskipTests
    # DB requisites
    mvn -P developer -pl developer,tools/devcloud -Ddeploydb
    mvn -Pdeveloper -pl developer -Ddeploydb-simulator
    [[ -z $WORKON_HOME ]] && WORKON_HOME=$HOME/.virtualenvs
    source $WORKON_HOME/cloudstack/bin/activate
    (cd setup/dbmigrate && db-migrate --env=localhost)
    # run simulator in background
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -pl :cloud-client-ui jetty:run -Dsimulator
    # populate
    # workon cloudstack
    # python tools/marvin/marvin/deployDataCenter.py -i test/integration/globo/cfg/advanced-globo.cfg
    # restart
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

DB
    deploydb          Create Required SQL Schema
    deploydb-simulator Create Required SQL Schema to use with simulator
    populatedb        Create a basic infrastructure on cloudstack
    populatedb-simulator  Create a basic infrastructure on cloudstack
    db-migrate        SQL migrations

    tag               Create a git TAG
    package           Build RPM packages for cloudstack (management, usage, awsapi, common, etc) and create yum repo
    lazy              Do everything you need to run cloudstack on your machine
"
    exit 2
    ;;
esac
 
