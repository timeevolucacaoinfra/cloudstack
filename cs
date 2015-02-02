#!/bin/bash

case "$1" in
  run)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -Dnonoss -Djava.awt.headless=true -pl :cloud-client-ui jetty:run
    ;;
  run-simulator)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -Dnonoss -Djava.awt.headless=true -Dsimulator -pl :cloud-client-ui jetty:run
    ;;
  compile)
    mvn -Dnonoss -Pdeveloper,systemvm -Djava.awt.headless=true -Dsimulator clean install -DskipTests
    ;;
  compile-changes)
    mvn -Dnonoss -Pdeveloper,systemvm -Djava.awt.headless=true -Dsimulator install -DskipTests
    ;;
  compile-quick)
    mvn -Dnonoss -Pdeveloper,systemvm -Djava.awt.headless=true -Dsimulator -pl :cloud-server,:cloud-api,:cloud-plugin-network-globonetwork,:cloud-plugin-network-globodns,:cloud-plugin-user-authenticator-oauth2,:cloud-client-ui install -DskipTests
    ;;
  update-js)
    cp -R ui/scripts client/target/generated-webapp/
    ;;
  deploydb)
    mvn -Dnonoss -Pdeveloper -pl developer,tools/devcloud -Ddeploydb
    ;;
  deploydb-simulator)
    mvn -Dnonoss -Pdeveloper -pl developer -Ddeploydb
    mvn -Dnonoss -Pdeveloper -pl developer -Ddeploydb-simulator
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
    [[ $# -ne 3 ]] && echo "You need to provide the tag from git and yum repo path. eg: $0 package 4.2.0-201402262000 /path/to/your/yum/repo" && exit 1
    # export some shell environments variables
    export VIRTUALENVWRAPPER_PYTHON=/usr/local/bin/python2.7
    export M2_HOME=/opt/apache-maven-3.0.5
    export PATH=${M2_HOME}/bin:${PATH}:/mnt/utils/bin
    export JAVA_HOME=/usr/lib/jvm/java-1.7.0-openjdk-1.7.0.3.x86_64/jre
    export CATALINA_HOME=/usr/share/tomcat6/
    export MAVEN_OPTS="-XX:MaxPermSize=800m -Xmx2g"
    source /usr/local/bin/virtualenvwrapper.sh

    REPOPATH=$3
    git checkout $2
    (cd packaging/centos63; ./package.sh -t $2)
    if [[ $? -eq 0 ]]; then
      if [[ -d $REPOPATH ]];then
          BUILDDIR='dist/rpmbuild'
          echo -n "Copying files ${BUILDDIR}/RPMS/x86_64/cloudstack-[a-z]*-${2}.el6.x86_64.rpm to $REPOPATH..."
          mv ${BUILDDIR}/RPMS/x86_64/cloudstack-[a-z]*-${2}.el6.x86_64.rpm $REPOPATH
          echo "done"
          createrepo -v $REPOPATH
      else
          echo "The directory $REPOPATH does not exist... exiting."
      fi
    else
      echo "Please, fix the errors."
      exit 1
    fi
    ;;
  *)
    echo "Usage: $0 [action]
Actions:

RUN
    run               Run Jetty server in DEBUG mode
    run-simulator     Run Jetty w/ hypervisor simulator
    
COMPILE
    compile           Clean and Compile entire cloudstack
    compile-changes   Compile only changes in all cloudstack
    compile-quick     Compile only globo elements
    update-js         Update Javascript only

DB
    deploydb          Create Required SQL Schema
    deploydb-simulator Create Required SQL Schema to use with simulator
    db-migrate        SQL migrations

    tag               Create a git TAG
    package           Build RPM packages for cloudstack (management, usage, awsapi, common, etc) and create yum repo
"
    exit 2
    ;;
esac
 
