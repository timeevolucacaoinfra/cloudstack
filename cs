#!/bin/bash

pkg_path(){
    [ -f /etc/redhat-release ] || return 1
    package=${1}
    package_name=$(rpm -qa | grep ${package})
    [[ -z ${package_name} ]] && echo "${1} not found! Please install it before continue!" >&2 && return 1
    echo $(rpm -ql ${package_name} | grep \/bin$ | sed 's/\/bin//g')
}

[[ -z $M2_HOME ]] && export M2_HOME=$(pkg_path apache-maven)
[[ -z $JAVA_HOME ]] && export JAVA_HOME=$(pkg_path java-1.7.0-openjdk-1.7)
[[ -z $CATALINA_HOME ]] && export CATALINA_HOME=/usr/share/tomcat6/
export PATH=${PATH}:${M2_HOME}/bin

gen_tag(){
    git checkout -q develop
    git pull -q
    cs_version=$(mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version | grep '^[0-9]\.')
    tag_version=$(date +%Y%m%d%H%M)
    git tag ${cs_version}-${tag_version}
    remote=$(cat .git/config  | awk -F\" '/\[remote/ {print $2}')
    git push ${remote} --tags
    git push --tags
    echo "RELEASE/TAG: ${cs_version}-${tag_version}"
}


gen_package(){
    tag=${1}
    REPOPATH=${2}

    [[ ! -f /etc/redhat-release ]] && echo "Opss... run this option only in RedHat OS. Exiting..." && return 1
    [[ ! -d ${REPOPATH} ]] && echo "The directory ${REPOPATH} does not exist... exiting." && return 1

    # export some shell environments variables
    # export VIRTUALENVWRAPPER_PYTHON=/usr/local/bin/python2.7
    export MAVEN_OPTS="-XX:MaxPermSize=800m -Xmx2g"
    # [[ ! -f ${virtualenv_file} ]] && echo "File ${virtualenv_file} does not exist!" && retun 1
    # source ${virtualenv_file}

    # git checkout ${tag}
    (cd packaging/centos63; ./package.sh -t ${tag})

    [[ $? -ne 0 ]] && echo "Failed to compile package. Please, fix errors." && return 1

    # keep last 3 builds
    echo "Removing old packages from yum repo ${REPOPATH}"
    rpms='agent awsapi baremetal-agent cli common management usage'
    for f in ${rpms}; do rm -f $(ls -1t cloudstack-${f}* 2>/dev/null | awk 'NR>5 {print}') ; done

    BUILDDIR='dist/rpmbuild'
    echo -n "Copying files ${BUILDDIR}/RPMS/x86_64/cloudstack-[a-z]*-${tag}.el6.x86_64.rpm to $REPOPATH..."
    mv ${BUILDDIR}/RPMS/x86_64/cloudstack-[a-z]*-${tag}.el6.x86_64.rpm $REPOPATH
    echo "done"
    # Create yum repo
    echo "Creating yum repo..."
    whoami=$(whoami)
    [[ ${whoami} == 'root' ]] && createrepo -q ${REPOPATH} || sudo createrepo -q ${REPOPATH}
}

continuos_delivery(){
    # Gen tag
    repo_path=${1}
    echo "Creating a new tag..."
    TAG=$(gen_tag | awk '/RELEASE\/TAG/ {print $2}')
    # Build package
    echo "Building tag ${TAG}"
    gen_package ${TAG} ${repo_path}
}

case "$1" in
  run)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -Dnonoss -Djava.awt.headless=true -pl :cloud-client-ui jetty:run
    ;;
  run-simulator)
    rm -f *.log
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -Dnonoss -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true -Dsimulator -pl :cloud-client-ui jetty:run
    ;;
  compile)
    mvn -Dnonoss -Pdeveloper,systemvm -Djava.awt.headless=true -Dsimulator clean install -DskipTests
    ;;
  compile-with-tests)
    mvn -Dnonoss -Pdeveloper,systemvm -Djava.awt.headless=true -Dsimulator clean install 
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
    gen_tag
    ;;
  package)
    [[ $# -ne 3 ]] && echo "Use: $0 package <TAG> <path_to_your_yum_repo>" && exit 1
    tag=$2
    repopath=$3
    gen_package ${tag} ${repopath}
    ;;
  cd)
    [[ $# -ne 2 ]] && echo "Use: $0 cd <path_to_your_yum_repo>" && exit 1
    repo_path=$2
    continuos_delivery ${repo_path}
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
    cd                tag + package + create yum repo
"
    exit 2
    ;;
esac
 
