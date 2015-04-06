#!/bin/bash

virtualenv_name='cloudstack'

project_basedir='/var/lib/jenkins/cloudstack'
globo_test_basedir="${project_basedir}/test/integration/globo"
project_branch='4.4.2-globo'
maven_log='/tmp/cloudstack.log'

[[ -z $WORKON_HOME ]] && WORKON_HOME=$JENKINS_HOME/.virtualenvs

export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-i386
export PATH="$JAVA_HOME/bin:$PATH"

debug=1

PrintLog() {
    level=$1
    msg=$2
    timestamp=$(date +"%d/%b/%Y:%H:%M:%S %z")
    echo "[${timestamp}] [${level}] ${msg}"
}

[[ ! -f /etc/lsb-release ]] && PrintLog ERROR "Opss... run this script only in Ubuntu. Exiting..." && exit 1

StartJetty() {
    max_retries=18
    sleep_time=10
    ret_count=1
    PrintLog INFO "Starting cloudstack w/ simulator..."
    MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn --log-file ${maven_log} -pl client jetty:run -Dsimulator >/dev/null &
    [[ $debug ]] && PrintLog DEBUG "Checking if jetty is ready..."
    [[ ! -f $maven_log ]] && sleep 5
    while [ $ret_count -le $max_retries ]; do
        if grep -q '\[INFO\] Started Jetty Server' ${maven_log}; then
            [[ $debug ]] && PrintLog INFO "Jetty is running and ready"
            return 1
        else
            [[ $debug ]] && PrintLog DEBUG "Jetty is not ready yet... sleeping more ${sleep_time}sec (${ret_count}/${max_retries})"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    done
    PrintLog ERROR "Jetty is not ready after waiting for $((${max_retries}*${sleep_time})) sec."
    exit 1
}

ShutdownJetty() {
    max_retries=7
    sleep_time=3
    ret_count=1
    PrintLog INFO "Stopping cloudstack..."
    kill $(ps wwwaux | awk '/[m]aven.*jetty:run -Dsimulator/ {print $2}') 2>/dev/null
    [[ $? -ne 0 ]] && PrintLog WARN "Failed to stop jetty"
    while [ $ret_count -le $max_retries ]; do
        if [[ -z $(ps wwwaux | awk '/[m]aven.*jetty:run -Dsimulator/ {print $2}') ]]; then
            return 1
        else
            [[ $debug ]] && PrintLog DEBUG "Jetty is alive, waiting more ${sleep_time} (${ret_count}/${max_retries})"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    done
    PrintLog WARN "Kill -9 to jetty process!!!"
    kill -9 $(ps wwwaux | awk '/[m]aven.*jetty:run -Dsimulator/ {print $2}')
}

WaitForInfrastructure() {
    max_retries=22
    sleep_time=10
    ret_count=1
    PrintLog INFO "Waiting for infrastructure..."
    while [ $ret_count -le $max_retries ]; do
        if grep -q 'server resources successfully discovered by SimulatorSecondaryDiscoverer' ${maven_log}; then
            [[ $debug ]] && PrintLog INFO "Infrastructure is ready"
            return 1
        else
            [[ $debug ]] && PrintLog DEBUG "Infrasctructure is not ready yet... sleeping more ${sleep_time}sec (${ret_count}/${max_retries})"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    done
    PrintLog ERROR "Infrastructure was not ready in $((${max_retries}*${sleep_time})) seconds..."
    exit 1
}

installMarvin() {
    # Tries to install marvin.. just in case..
    /var/lib/jenkins/.virtualenvs/cloudstack/bin/pip install --allow-external mysql-connector-python ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz

    # Install marvin to ensure that we are using the correct version
    echo "#### /var/lib/jenkins/.virtualenvs/cloudstack/bin/pip install --upgrade --allow-external mysql-connector-python ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz"
    /var/lib/jenkins/.virtualenvs/cloudstack/bin/pip install --upgrade --allow-external mysql-connector-python ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz

    ls ~/.virtualenvs/cloudstack/lib/python2.7/site-packages/marvin/cloudstackAPI/ | grep addG
}

# Checkout repository, compile, use virtualenv and sync the mavin commands
ShutdownJetty
PrintLog INFO "Removing log file '${maven_log}'"
rm -f ${maven_log}
[[ $debug ]] && PrintLog DEBUG "Change work dir to ${project_basedir}"
[[ ! -d $project_basedir ]] && PrintLog ERROR "Directory ${project_basedir} does not exist...exit" && exit 1
cd ${project_basedir}
PrintLog INFO "Checking out to branch '${project_branch}'"
git checkout ${project_branch} >/dev/null 2>/dev/null
PrintLog INFO "Pulling latest modifications"
git pull
PrintLog INFO "Switching to '${virtualenv_name}' virtualenv"
source $WORKON_HOME/${virtualenv_name}/bin/activate

PrintLog INFO "Compiling cloudstack..."
mvn -Pdeveloper -Dsimulator clean install
[[ $? -ne 0 ]] && PrintLog ERROR "Failed to compile ACS" && exit 1
PrintLog INFO "Compiling and packing marvin..."
mvn -P developer -pl :cloud-marvin
[[ $? -ne 0 ]] && PrintLog ERROR "Failed to compile marvin" && exit 1

installMarvin

# Deploy DB, Populate DB and create infra structure
PrintLog INFO "Creating SQL schema"
mvn -q -P developer -pl developer -Ddeploydb >/dev/null 2>/dev/null
[[ $? -ne 0 ]] && PrintLog ERROR "Failed to deploy DB" && exit 1
mvn -Pdeveloper -pl developer -Ddeploydb-simulator >/dev/null 2>/dev/null
[[ $? -ne 0 ]] && PrintLog ERROR "Failed to deploy DB simulator" && exit 1
PrintLog INFO "Doing some required SQL migrations"
if [ -d "/var/lib/jenkins/cloudstack-deploy" ];
then
    (cd /var/lib/jenkins/cloudstack-deploy/dbmigrate && db-migrate >/dev/null)
    cd -
else
    echo "OPS... could not find migrate"
fi
StartJetty
PrintLog INFO "Creating an advanced zone..."
python ${project_basedir}/tools/marvin/marvin/deployDataCenter.py -i ${project_basedir}/test/integration/globo/cfg/advanced-globo.cfg

# Required restart
WaitForInfrastructure
ShutdownJetty
PrintLog INFO "Removing log file '${maven_log}'"
rm -f ${maven_log}
StartJetty

# Tests
PrintLog INFO "Sync marvin"
mvn -Pdeveloper,marvin.sync -Dendpoint=localhost -pl :cloud-marvin

#installMarvin

nosetests --with-marvin --marvin-config=${globo_test_basedir}/demo.cfg --zone=Sandbox-simulator ${globo_test_basedir}/test_dns_api.py
results_file=$(ls -tr /tmp/[0-9]*/results.txt|tail -1)
tail -1 ${results_file} | grep -qw 'OK'
retval=$?
if [[ $retval -eq 0 ]]; then
    ShutdownJetty
    PrintLog INFO "All steps and tests successfully passed"
    exit 0
else
    PrintLog ERROR "Tests failed!!!"
    cat ${results_file}
    exit 1
fi
