#!/bin/bash
# ==================================================================
##
##  The user who will run this script, needs to have a ssh key pair on
##  VirtualBox host
##
##
##
## 16/Jul/2014 - timeevolucaoinfra@corp.globo.com
# ==================================================================
# baixar a systemvms e restaurar a partir desse ponto

VBoxManage='/usr/bin/VBoxManage'
vbox_host='10.2.162.11'
vbox_user='vbox'
vbox_vm_ci_string='CI.*DevCloud2.*Clone4'

snapshot_use_name='ci-ready'
virtualenv_name='cloudstack'

project_basedir='/var/lib/jenkins/cloudstack'
project_branch='4.3.0-globo'
maven_log='/tmp/cloudstack.log'

devcloud_host='10.2.162.19'

debug=1

[[ ! -f /etc/lsb-release ]] && PrintLog ERROR "Opss... run this script only in Ubuntu. Exiting..." && exit 1

PrintLog() {
    level=$1
    msg=$2
    timestamp=$(date +"%d/%b/%Y:%H:%M:%S %z")
    echo "[${timestamp}] [${level}] ${msg}"
}

TestConnection() {
    # check if ssh connection has no problem and get the Xen CI UUID
    [[ $debug ]] && PrintLog DEBUG "Trying to connect to ${vbox_user}@${vbox_host}"
    [[ $debug ]] && PrintLog DEBUG "ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l ${vbox_user} ${vbox_host}"
    if ! ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host ":"; then
        PrintLog ERROR "Failed to log in ${vbox_user}@${vbox_host}"
        exit 1
    fi
}

ShutdownVM() {
    vm_uuid=$1
    [[ -n $2 ]] && shutdowncmd='poweroff' || shutdowncmd='acpipowerbutton'
    [[ $debug ]] && PrintLog DEBUG "VBoxManage controlvm ${vm_uuid} ${shutdowncmd}"
    cmd_output=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "${VBoxManage} controlvm ${vm_uuid} ${shutdowncmd}")
}

RestoreSnapshot() {
    vm_uuid=$1
    [[ $debug ]] && PrintLog DEBUG "RemoteSshCmd VBoxManage --nologo snapshot ${vm_uuid} restore $snapshot_use_name"
    cmd_output=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "${VBoxManage} --nologo snapshot ${vm_uuid} restore $snapshot_use_name")
}

TurnOnVM() {
    vm_uuid=$1
    [[ $debug ]] && PrintLog DEBUG "VBoxManage startvm ${vm_uuid}"
    cmd_output=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "${VBoxManage} --nologo startvm ${vm_uuid} --type headless")
}

ListVM() {
    [[ $debug ]] && PrintLog DEBUG "VBoxManage --nologo list vms | grep -w '${vbox_vm_ci_string}' | grep -oP '\w+\-\w+\-\w+\-\w+\-\w+'"    
    vm_uuid=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "${VBoxManage} --nologo list vms | grep -w '${vbox_vm_ci_string}' | grep -oP '\w+\-\w+\-\w+\-\w+\-\w+'")
}

WaitforJettyReady() {
    max_retries=15
    sleep_time=8
    ret_count=1
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

WaitForVMState() {
    vm_uuid=$1
    wait_for_state=$2
    max_retries=10
    sleep_time=5
    ret_count=1
    while [ $ret_count -le $max_retries ]; do
        vm_status=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "${VBoxManage} --nologo showvminfo ${vm_uuid} | awk '/State/ {print \$2}'")
        if [[ $vm_status = $wait_for_state ]]; then
            [[ $debug ]] && echo "[DEBUG] VM state is already in '${wait_for_state}'"
            return 1
        else
            [[ $debug ]] && echo -ne "[DEBUG] VM state ${vm_uuid} in '${vm_status}' state, still waiting for '${wait_for_state}' state... sleeping more ${sleep_time}(s) count ${ret_count}/${max_retries}\r"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    done
    PrintLog ERROR "Vm ${vm_uuid} still in '${vm_status}' state, not in '${wait_for_state}' state"
    exit 1
}

WaitforSSH() {
    host_to_connect=$1
    max_retries=8
    sleep_time=3
    ret_count=1
    [[ $debug ]] && echo -ne "[DEBUG] Trying to connect to ${host_to_connect}..."
    while [ $ret_count -le $max_retries ]; do
        if echo "QUIT" | nc -w 2 ${host_to_connect} 22 >/dev/null 2>&1; then
            [[ $debug ]] && echo "ok"
            return 1
        else
            [[ $debug ]] && echo -ne "not ready yet...sleeping more ${sleep_time}(s) count ${ret_count}/${max_retries}\r"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    done
    return 0
}

# Main
TestConnection
ListVM
[[ -z $vm_uuid ]] && PrintLog ERROR "There is no VM with string '${vbox_vm_ci_string}' on name... exiting!" && exit 1
[[ $debug ]] && PrintLog DEBUG "VM UUID '${vm_uuid}'"

ShutdownVM $vm_uuid 
WaitForVMState $vm_uuid powered
[[ $debug ]] && PrintLog DEBUG "Removing log file '${maven_log}'"
rm -f ${maven_log}
RestoreSnapshot $vm_uuid
[[ $? -ne 0 ]] && PrintLog ERROR "Failed to restore snapshot '${snapshot_use_name} for vm ${vm_uuid}...exiting!" && exit 1
WaitForVMState $vm_uuid powered
TurnOnVM $vm_uuid
[[ $debug ]] && PrintLog DEBUG "Change work dir to ${project_basedir}"

[[ ! -d $project_basedir ]] && PrintLog ERROR "Directory ${project_basedir} does not exist...exit" && exit 1
cd ${project_basedir}
[[ $debug ]] && PrintLog INFO "Checking out to branch '${project_branch}'"
git checkout ${project_branch}
[[ $debug ]] && PrintLog INFO "Switching to '${virtualenv_name}' virtualenv"
source $WORKON_HOME/${virtualenv_name}/bin/activate
[[ $debug ]] && PrintLog INFO "Creating SQL schema"
mvn -q -P developer -pl developer,tools/devcloud -Ddeploydb >/dev/null 2>/dev/null
[[ $debug ]] && PrintLog INFO "Doing some required SQL migrations"
# mvn -P developer,systemvm clean install -DskipTests
(cd setup/dbmigrate && db-migrate >/dev/null)
WaitForVMState $vm_uuid running
WaitforSSH ${devcloud_host}
[[ $debug ]] && PrintLog INFO "Starting cloudstack..."
MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn --log-file ${maven_log} -pl :cloud-client-ui jetty:run >/dev/null &
[[ $debug ]] && PrintLog INFO "Creating an advanced zone..."
WaitforJettyReady
# python ${project_basedir}/tools/marvin/marvin/deployDataCenter.py -i ${project_basedir}/test/integration/globo/cfg/devcloud.cfg
# nosetests --with-marvin --marvin-config=demo.cfg --load test_dns_api.py
echo "[INFO] Kill cloudstack..."
# kill $(ps wwwaux | awk '/[j]ava.*cloud-client-ui jetty:run/ {print $2}')
