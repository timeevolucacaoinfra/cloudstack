#!/bin/bash

echo "project_branch: ${project_branch}"
echo "globodns_host: ${globodns_host}"
echo "globodns_resolver_nameserver: ${globodns_resolver_nameserver}"

virtualenv_name='cloudstack'

project_basedir='/var/lib/jenkins/cloudstack'
globo_test_basedir="${project_basedir}/test/integration/globo"
#project_branch='develop'
maven_log='/tmp/cloudstack.log'
pip="/var/lib/jenkins/.virtualenvs/${virtualenv_name}/bin/pip"
python="/var/lib/jenkins/.virtualenvs/${virtualenv_name}/bin/python"
nosetests="/var/lib/jenkins/.virtualenvs/${virtualenv_name}/bin/nosetests"

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

# [[ ! -f /etc/lsb-release ]] && PrintLog ERROR "Opss... run this script only in Ubuntu. Exiting..." && exit 1

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

activateVirtualEnv() {
    PrintLog INFO "Switching to '${virtualenv_name}' virtualenv"
    source $WORKON_HOME/${virtualenv_name}/bin/activate
}

installPip() {

    activateVirtualEnv
    #required for test_dns_api.py
    ${pip} install beautifulsoup4==4.3.2 --extra-index-url=https://artifactory.globoi.com/artifactory/pypi/ --extra-index-url=https://artifactory.globoi.com/artifactory/api/pypi/pypi/simple --extra-index-url=https://pypi.python.org

    # Tries to install marvin.. just in case..
    [[ ! `${pip} freeze | grep -i Marvin` ]] && ${pip} install --allow-external mysql-connector-python ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz

    # Install marvin to ensure that we are using the correct version
    [[ `${pip} freeze | grep -i Marvin` ]] && echo "#### ${pip} install --upgrade --allow-external mysql-connector-python ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz"
    ${pip} install --upgrade --allow-external mysql-connector-python ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz

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

activateVirtualEnv

last_commit=$(git log -n 1 | grep commit | cut -d' ' -f2)
last_commit_file="/tmp/cloudstack-integration-tests-last-commit.txt"

PrintLog INFO "last git commit: ${last_commit}"

#vejo se o arquivo de ultimo commit existe
[[ ! -f "$last_commit_file" ]] && echo "" > ${last_commit_file}

saved_last_commit=$(cat ${last_commit_file} | head -1)

if [ "${last_commit}" != "${saved_last_commit}" ];
then

    PrintLog INFO "Compiling cloudstack..."

    mvn -Pdeveloper -Dsimulator clean install
    [[ $? -ne 0 ]] && PrintLog ERROR "Failed to compile ACS" && exit 1
    PrintLog INFO "Compiling and packing marvin..."
    mvn -P developer -pl :cloud-marvin
    [[ $? -ne 0 ]] && PrintLog ERROR "Failed to compile marvin" && exit 1

    echo "${last_commit}" > ${last_commit_file}

    installPip

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
    ${python} ${project_basedir}/tools/marvin/marvin/deployDataCenter.py -i ${project_basedir}/test/integration/globo/cfg/advanced-globo.cfg

    # Required restart
    WaitForInfrastructure
    ShutdownJetty
    PrintLog INFO "Removing log file '${maven_log}'"
    rm -f ${maven_log}

else
    PrintLog INFO "There were no code changes, so we don't need compile!!! yaayyyyyyyy"
fi

StartJetty

# Tests
PrintLog INFO "Sync marvin"
cd ${project_basedir}
pwd
mvn -Pdeveloper,marvin.sync -Dendpoint=localhost -pl :cloud-marvin

sleep 5

installPip

# check if Globo assets are in marvin tarball file
[[ ! `tar tvzf ${project_basedir}/tools/marvin/dist/Marvin-*.tar.gz | grep Globo` ]] && PrintLog ERROR "Tests will fail!!! Marvin tarball does not contain Globo files" && exit 1


${nosetests} --with-marvin --marvin-config=${globo_test_basedir}/demo.cfg --zone=Sandbox-simulator ${globo_test_basedir}/test_dns_api.py
retval=$?
if [[ $retval -ne 0 ]]; then
    PrintLog ERROR "Tests failed!!!"
    ShutdownJetty
    exit 1
fi

results_file=$(ls -tr /tmp/MarvinLogs/$(date +"%b_%d_%Y")*/results.txt | tail -1)
echo "Results file: ${results_file}"

tail -1 ${results_file} | grep -qw 'OK'
retval=$?
cat ${results_file}
if [[ $retval -eq 0 ]]; then
    ShutdownJetty
    PrintLog INFO "All steps and tests successfully passed"
    exit 0
else
    PrintLog ERROR "Tests failed!!!"
    exit 1
fi

