// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.xenserver.resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.Local;

import com.cloud.agent.api.to.SrTO;
import com.cloud.agent.api.to.VirtualNetworkTO;
import com.cloud.utils.Pair;
import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Network;
import com.xensource.xenapi.SR;
import com.xensource.xenapi.Task;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.VBD;
import com.xensource.xenapi.VDI;
import com.xensource.xenapi.VIF;
import com.xensource.xenapi.VM;

import org.apache.cloudstack.storage.to.VolumeObjectTO;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.MigrateWithStorageAnswer;
import com.cloud.agent.api.MigrateWithStorageCommand;
import com.cloud.agent.api.MigrateWithStorageCompleteAnswer;
import com.cloud.agent.api.MigrateWithStorageCompleteCommand;
import com.cloud.agent.api.MigrateWithStorageReceiveAnswer;
import com.cloud.agent.api.MigrateWithStorageReceiveCommand;
import com.cloud.agent.api.MigrateWithStorageSendAnswer;
import com.cloud.agent.api.MigrateWithStorageSendCommand;
import com.cloud.agent.api.storage.MigrateVolumeAnswer;
import com.cloud.agent.api.storage.MigrateVolumeCommand;
import com.cloud.agent.api.to.DiskTO;
import com.cloud.agent.api.to.NicTO;
import com.cloud.agent.api.to.StorageFilerTO;
import com.cloud.agent.api.to.VirtualMachineTO;
import com.cloud.agent.api.to.VolumeTO;
import com.cloud.network.Networks.TrafficType;
import com.cloud.resource.ServerResource;
import com.cloud.storage.Volume;
import com.cloud.utils.exception.CloudRuntimeException;

@Local(value = ServerResource.class)
public class XenServer610Resource extends XenServer600Resource {
    private static final Logger s_logger = Logger.getLogger(XenServer610Resource.class);

    public XenServer610Resource() {
        super();
    }

    @Override
    public Answer executeRequest(Command cmd) {
        if (cmd instanceof MigrateWithStorageCommand) {
            return execute((MigrateWithStorageCommand)cmd);
        } else if (cmd instanceof MigrateWithStorageReceiveCommand) {
            return execute((MigrateWithStorageReceiveCommand)cmd);
        } else if (cmd instanceof MigrateWithStorageSendCommand) {
            return execute((MigrateWithStorageSendCommand)cmd);
        } else if (cmd instanceof MigrateWithStorageCompleteCommand) {
            return execute((MigrateWithStorageCompleteCommand)cmd);
        } else if (cmd instanceof MigrateVolumeCommand) {
            return execute((MigrateVolumeCommand)cmd);
        } else {
            return super.executeRequest(cmd);
        }
    }

    private List<VolumeObjectTO> getUpdatedVolumePathsOfMigratedVm(Connection connection, VM migratedVm, DiskTO[] volumes) throws CloudRuntimeException {
        List<VolumeObjectTO> volumeToList = new ArrayList<VolumeObjectTO>();

        try {
            // Volume paths would have changed. Return that information.
            Set<VBD> vbds = migratedVm.getVBDs(connection);
            Map<String, VDI> deviceIdToVdiMap = new HashMap<String, VDI>();
            // get vdi:vbdr to a map
            for (VBD vbd : vbds) {
                VBD.Record vbdr = vbd.getRecord(connection);
                if (vbdr.type == Types.VbdType.DISK) {
                    VDI vdi = vbdr.VDI;
                    deviceIdToVdiMap.put(vbdr.userdevice, vdi);
                }
            }

            for (DiskTO volumeTo : volumes) {
                if (volumeTo.getType() != Volume.Type.ISO) {
                    VolumeObjectTO vol = (VolumeObjectTO)volumeTo.getData();
                    Long deviceId = volumeTo.getDiskSeq();
                    VDI vdi = deviceIdToVdiMap.get(deviceId.toString());
                    VolumeObjectTO newVol = new VolumeObjectTO();
                    newVol.setPath(vdi.getUuid(connection));
                    newVol.setId(vol.getId());
                    volumeToList.add(newVol);
                }
            }
        } catch (Exception e) {
            s_logger.error("Unable to get the updated VDI paths of the migrated vm " + e.toString(), e);
            throw new CloudRuntimeException("Unable to get the updated VDI paths of the migrated vm " + e.toString(), e);
        }

        return volumeToList;
    }

    protected MigrateWithStorageAnswer execute(MigrateWithStorageCommand cmd) {
        Connection connection = getConnection();
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        List<Pair<VolumeTO, StorageFilerTO>> volumeToFiler = cmd.getVolumeToFilerAsList();
        final String vmName = vmSpec.getName();
        Task task = null;

        try {
            prepareISO(connection, vmSpec.getName());

            // Get the list of networks and recreate VLAN, if required.
            for (NicTO nicTo : vmSpec.getNics()) {
                getNetwork(connection, nicTo);
            }

            Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");
            Network networkForSm = getNativeNetworkForTraffic(connection, TrafficType.Storage, null).getNetwork();
            Host host = Host.getByUuid(connection, _host.uuid);
            Map<String, String> token = host.migrateReceive(connection, networkForSm, other);

            // Get the vm to migrate.
            Set<VM> vms = VM.getByNameLabel(connection, vmSpec.getName());
            VM vmToMigrate = vms.iterator().next();

            // Create the vif map. The vm stays in the same cluster so we have to pass an empty vif map.
            Map<VIF, Network> vifMap = new HashMap<VIF, Network>();
            Map<VDI, SR> vdiMap = new HashMap<VDI, SR>();
            for (Pair<VolumeTO, StorageFilerTO> entry : volumeToFiler) {
                VolumeTO volume = entry.first();
                StorageFilerTO filerTo = entry.second();
                vdiMap.put(getVDIbyUuid(connection, volume.getPath()), getStorageRepository(connection, filerTo.getUuid()));
            }

            // Check migration with storage is possible.
            task = vmToMigrate.assertCanMigrateAsync(connection, token, true, vdiMap, vifMap, other);
            try {
                // poll every 1 seconds
                long timeout = (_migratewait) * 1000L;
                waitForTask(connection, task, 1000, timeout);
                checkForSuccess(connection, task);
            } catch (Types.HandleInvalid e) {
                s_logger.error("Error while checking if vm " + vmName + " can be migrated to the destination host " + host, e);
                throw new CloudRuntimeException("Error while checking if vm " + vmName + " can be migrated to the " + "destination host " + host, e);
            }

            // Migrate now.
            task = vmToMigrate.migrateSendAsync(connection, token, true, vdiMap, vifMap, other);
            try {
                // poll every 1 seconds.
                long timeout = (_migratewait) * 1000L;
                waitForTask(connection, task, 1000, timeout);
                checkForSuccess(connection, task);
            } catch (Types.HandleInvalid e) {
                s_logger.error("Error while migrating vm " + vmName + " to the destination host " + host, e);
                throw new CloudRuntimeException("Error while migrating vm " + vmName + " to the destination host " + host, e);
            }

            // Volume paths would have changed. Return that information.
            List<VolumeObjectTO> volumeToList = getUpdatedVolumePathsOfMigratedVm(connection, vmToMigrate, vmSpec.getDisks());
            vmToMigrate.setAffinity(connection, host);
            return new MigrateWithStorageAnswer(cmd, volumeToList);
        } catch (Exception e) {
            s_logger.warn("Catch Exception " + e.getClass().getName() + ". Storage motion failed due to " + e.toString(), e);
            return new MigrateWithStorageAnswer(cmd, e);
        } finally {
            if (task != null) {
                try {
                    task.destroy(connection);
                } catch (Exception e) {
                    s_logger.debug("Unable to destroy task " + task.toString() + " on host " + _host.uuid + " due to " + e.toString());
                }
            }
        }
    }

    protected MigrateWithStorageReceiveAnswer execute(MigrateWithStorageReceiveCommand cmd) {
        Connection connection = getConnection();
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        List<Pair<VolumeTO, StorageFilerTO>> volumeToFiler = cmd.getVolumeToFilerAsList();

        try {
            // Get a map of all the SRs to which the vdis will be migrated.
            List<Pair<VolumeTO, SrTO>> volumeToSr = new ArrayList<>();
            for (Pair<VolumeTO, StorageFilerTO> entry : volumeToFiler) {
                VolumeTO volume = entry.first();
                StorageFilerTO filerTo = entry.second();
                SR sr = getStorageRepository(connection, filerTo.getUuid());
                volumeToSr.add(new Pair<>(volume, new SrTO(sr.toWireString())));
            }

            // Get the list of networks to which the vifs will attach.
            List<Pair<NicTO, VirtualNetworkTO>> nicToNetwork = new ArrayList<>();
            for (NicTO nicTo : vmSpec.getNics()) {
                Network network = getNetwork(connection, nicTo);
                nicToNetwork.add(new Pair<>(nicTo, new VirtualNetworkTO(network.toWireString())));
            }

            Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");
            Network network = getNativeNetworkForTraffic(connection, TrafficType.Storage, null).getNetwork();
            Host host = Host.getByUuid(connection, _host.uuid);
            Map<String, String> token = host.migrateReceive(connection, network, other);

            return new MigrateWithStorageReceiveAnswer(cmd, volumeToSr, nicToNetwork, token);
        } catch (CloudRuntimeException e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageReceiveAnswer(cmd, e);
        } catch (Exception e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageReceiveAnswer(cmd, e);
        }
    }

    protected MigrateWithStorageSendAnswer execute(MigrateWithStorageSendCommand cmd) {
        Connection connection = getConnection();
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();
        List<Pair<VolumeTO, SrTO>> volumeToSr = cmd.getVolumeToSr();
        List<Pair<NicTO, VirtualNetworkTO>> nicToNetwork = cmd.getNicToNetwork();
        Map<String, String> token = cmd.getToken();
        final String vmName = vmSpec.getName();
        Set<VolumeTO> volumeToSet = null;
        boolean migrated = false;
        Task task = null;
        try {
            Set<VM> vms = VM.getByNameLabel(connection, vmSpec.getName());
            VM vmToMigrate = vms.iterator().next();
            Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");

            // Create the vdi map which tells what volumes of the vm need to go on which sr on the destination.
            Map<VDI, SR> vdiMap = new HashMap<VDI, SR>();
            for (Pair<VolumeTO, SrTO> pair : volumeToSr) {
                VDI vdi = getVDIbyUuid(connection, pair.first().getPath());
                vdiMap.put(vdi, getSR(pair.second().getRef()));
            }

            // Create the vif map.
            Map<VIF, Network> vifMap = new HashMap<VIF, Network>();
            for (Pair<NicTO, VirtualNetworkTO> pair : nicToNetwork) {
                VIF vif = getVifByMac(connection, vmToMigrate, pair.first().getMac());
                vifMap.put(vif, getNetwork(pair.second().getRef()));
            }

            // Check migration with storage is possible.
            task = vmToMigrate.assertCanMigrateAsync(connection, token, true, vdiMap, vifMap, other);
            try {
                // poll every 1 seconds.
                long timeout = (_migratewait) * 1000L;
                waitForTask(connection, task, 1000, timeout);
                checkForSuccess(connection, task);
            } catch (Types.HandleInvalid e) {
                s_logger.error("Error while checking if vm " + vmName + " can be migrated.", e);
                throw new CloudRuntimeException("Error while checking if vm " + vmName + " can be migrated.", e);
            }

            // Migrate now.
            task = vmToMigrate.migrateSendAsync(connection, token, true, vdiMap, vifMap, other);
            try {
                // poll every 1 seconds.
                long timeout = (_migratewait) * 1000L;
                waitForTask(connection, task, 1000, timeout);
                checkForSuccess(connection, task);
            } catch (Types.HandleInvalid e) {
                s_logger.error("Error while migrating vm " + vmName, e);
                throw new CloudRuntimeException("Error while migrating vm " + vmName, e);
            }

            migrated = true;
            return new MigrateWithStorageSendAnswer(cmd, volumeToSet);
        } catch (CloudRuntimeException e) {
            s_logger.error("Migration of vm " + vmName + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageSendAnswer(cmd, e);
        } catch (Exception e) {
            s_logger.error("Migration of vm " + vmName + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageSendAnswer(cmd, e);
        } finally {
            if (task != null) {
                try {
                    task.destroy(connection);
                } catch (Exception e) {
                    s_logger.debug("Unable to destroy task " + task.toString() + " on host " + _host.uuid + " due to " + e.toString());
                }
            }
        }
    }

    protected MigrateWithStorageCompleteAnswer execute(MigrateWithStorageCompleteCommand cmd) {
        Connection connection = getConnection();
        VirtualMachineTO vmSpec = cmd.getVirtualMachine();

        try {
            Host host = Host.getByUuid(connection, _host.uuid);
            Set<VM> vms = VM.getByNameLabel(connection, vmSpec.getName());
            VM migratedVm = vms.iterator().next();

            // Check the vm is present on the new host.
            if (migratedVm == null) {
                throw new CloudRuntimeException("Couldn't find the migrated vm " + vmSpec.getName() + " on the destination host.");
            }

            // Volume paths would have changed. Return that information.
            List<VolumeObjectTO> volumeToSet = getUpdatedVolumePathsOfMigratedVm(connection, migratedVm, vmSpec.getDisks());
            migratedVm.setAffinity(connection, host);

            return new MigrateWithStorageCompleteAnswer(cmd, volumeToSet);
        } catch (CloudRuntimeException e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageCompleteAnswer(cmd, e);
        } catch (Exception e) {
            s_logger.error("Migration of vm " + vmSpec.getName() + " with storage failed due to " + e.toString(), e);
            return new MigrateWithStorageCompleteAnswer(cmd, e);
        }
    }

    protected MigrateVolumeAnswer execute(MigrateVolumeCommand cmd) {
        Connection connection = getConnection();
        String volumeUUID = cmd.getVolumePath();
        StorageFilerTO poolTO = cmd.getPool();

        try {
            SR destinationPool = getStorageRepository(connection, poolTO.getUuid());
            VDI srcVolume = getVDIbyUuid(connection, volumeUUID);
            Map<String, String> other = new HashMap<String, String>();
            other.put("live", "true");

            // Live migrate the vdi across pool.
            Task task = srcVolume.poolMigrateAsync(connection, destinationPool, other);
            long timeout = (_migratewait) * 1000L;
            waitForTask(connection, task, 1000, timeout);
            checkForSuccess(connection, task);
            VDI dvdi = Types.toVDI(task, connection);

            return new MigrateVolumeAnswer(cmd, true, null, dvdi.getUuid(connection));
        } catch (Exception e) {
            String msg = "Catch Exception " + e.getClass().getName() + " due to " + e.toString();
            s_logger.error(msg, e);
            return new MigrateVolumeAnswer(cmd, false, msg, null);
        }
    }

    @Override
    protected void plugDom0Vif(Connection conn, VIF dom0Vif) throws XmlRpcException, XenAPIException {
        // do nothing. In xenserver 6.1 and beyond this step isn't needed.
    }

    private SR getSR(String ref){
        return Types.toSR(ref);
    }

    private Network getNetwork(String ref){
        return Types.toNetwork(ref);
    }
}
