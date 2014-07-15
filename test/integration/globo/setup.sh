#!/bin/bash
# deixar semi pronto o banco e as systemvms baixadas e restaurar a partir desse ponto, ou faz o teste completo?
# ou fazer o teste completo uma vez por dia

vboxmanage='/usr/bin/vboxmanage'
vbox_host='10.2.162.11'
vbox_user='vbox'
vbox_vm_ci_string='CI.*DevCloud2'
snapshot_use_name='ci_ready'
virtualenv_name='cloudstack'


project_basedir='/Users/okama/Projetos/workspace/cloudstack_github_globocom'
project_branch='4.3.0-globo'
maven_log='/tmp/cloudstack.log'

devcloud_host='10.2.162.19'

debug=0

ShutdownVMCI() {
    vm_uuid=$1
    [[ $debug ]] && echo "[DEBUG] VBoxManage  controlvm ${vm_uuid} poweroff"
    cmd_output=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "VBoxManage controlvm ${vm_uuid} poweroff")
}

RestoreSnapshot() {
    vm_uuid=$1
    [[ $debug ]] && echo "[DEBUG] RemoteSshCmd vboxmanage --nologo snapshot ${vm_uuid} restore $snapshot_use_name"
    cmd_output=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "VBoxManage --nologo snapshot ${vm_uuid} restore $snapshot_use_name")
}

TurnOnVMCI() {
    vm_uuid=$1
    [[ $debug ]] && echo "[DEBUG] VBoxManage startvm ${vm_uuid}"
    cmd_output=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "VBoxManage --nologo startvm ${vm_uuid}")
}


ListCIVM() {
    # check if ssh connection has no problem and get the Xen CI UUID
    [[ $debug ]] && echo "[DEBUG] Trying to connect to ${vbox_user}@${vbox_host}"
    [[ $debug ]] && echo "[DEBUG] ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l ${vbox_user} ${vbox_host}"
    [[ $debug ]] && echo "[DEBUG] vboxmanage --nologo list vms | grep 'CI.*DevCloud2.*Clone' | grep -oP '\w+\-\w+\-\w+\-\w+\-\w+'"
    if ! vm_uuid=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "vboxmanage --nologo list vms | grep 'CI.*DevCloud2.*Clone' | grep -oP '\w+\-\w+\-\w+\-\w+\-\w+'"); then    
        echo "[ERROR] Failed to log in ${vbox_user}@${vbox_host}"
        exit 1
    fi
}

JettyisAlreadyRunning() {
    max_retries=8
    sleep_time=3
    ret_count=0
    [[ $debug ]] && echo -ne "[DEBUG] Checking if jetty is already running and ready..."
    while [ $ret_count -lt $max_retries ]; do
        if grep -q '\[INFO\] Started Jetty Server' ${maven_log}; then
            [[ $debug ]] && echo "ok"
            return 1
        else
            [[ $debug ]] && echo -ne "not ready yet... sleeping more ${sleep_time}(s) count ${ret_count}/${max_retries}\r"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    return 0
    done
}

WaitForVMState() {
    vm_uuid=$1
    wait_for_state=$2
    max_retries=8
    sleep_time=3
    ret_count=0
    # [[ $debug ]] && echo -ne "[DEBUG] Checking VM Status ${vm_uuid}..."
    while [ $ret_count -lt $max_retries ]; do
        vm_status=$(ssh -q -oBatchMode=yes -oPasswordAuthentication=no -l $vbox_user $vbox_host "vboxmanage --nologo showvminfo ${vm_uuid} | awk '/State/ {print \$2}'")
        if [[ $vm_status = $wait_for_state ]]; then
            [[ $debug ]] && echo "[DEBUG] Checking VM Status ${vm_uuid}... '${wait_for_state}'"
            return 1
        else
            [[ $debug ]] && echo -ne "[DEBUG] Checking VM Status ${vm_uuid}...not in '${wait_for_state}' state yet...waiting for '${wait_for_state}' state... sleeping more ${sleep_time}(s) count ${ret_count}/${max_retries}\r"
            sleep $sleep_time
            ret_count=$[$ret_count+1]
        fi
    done
    echo "[ERROR] Vm ${vm_uuid} still in '${vm_status}' state not in '${wait_for_state}' state"
    exit 1
}

WaitforSSH() {
    # host_to_connect=$1
    host_to_connect='10.2.162.19'
    max_retries=8
    sleep_time=3
    ret_count=0
    [[ $debug ]] && echo -ne "[DEBUG] Trying to connect to ${host_to_connect}..."
    while [ $ret_count -lt $max_retries ]; do
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
# [[ ! -f /etc/lsb-release ]] && echo "Opss... run this option only in Ubuntu. Exiting..." && exit 1
ListCIVM
ShutdownVMCI $vm_uuid
WaitForVMState $vm_uuid powered
rm ${maven_log}
RestoreSnapshot $vm_uuid
WaitForVMState $vm_uuid saved
TurnOnVMCI $vm_uuid

[[ $debug ]] && echo "[DEBUG] Change work dir to ${project_basedir}"
cd ${project_basedir}
[[ $debug ]] && echo "[DEBUG] Checking out to branch '${project_branch}'"
git checkout ${project_branch}
[[ $debug ]] && echo "[DEBUG] Switching to '${virtualenv_name}' virtualenv"
source $WORKON_HOME/${virtualenv_name}/bin/activate
[[ $debug ]] && echo "[DEBUG] Creating SQL schema"
# mvn -P developer -pl developer,tools/devcloud -Ddeploydb
[[ $debug ]] && echo "[DEBUG] Doing some required SQL migrations"
# (cd setup/dbmigrate && db-migrate)
WaitForVMState $vm_uuid running
WaitforSSH ${devcloud_host}
[[ $debug ]] && echo "[DEBUG] Starting cloudstack..."
# MAVEN_OPTS="-Xmx2048m -XX:MaxPermSize=512m -Xdebug -Xrunjdwp:transport=dt_socket,address=8787,server=y,suspend=n" mvn -pl :cloud-client-ui jetty:run
[[ $debug ]] && echo "[DEBUG] Creating an advanced zone..."
JettyisAlreadyRunning
# python tools/marvin/marvin/deployDataCenter.py -i tools/marvin/marvin/cloudstack-local.cfg
# shutdown