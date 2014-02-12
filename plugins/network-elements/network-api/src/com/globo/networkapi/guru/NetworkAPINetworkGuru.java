package com.globo.networkapi.guru;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.Network;
import com.cloud.network.NetworkProfile;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.vm.NicProfile;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;

public class NetworkAPINetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(NetworkAPINetworkGuru.class);
    
    // by default traffic type is TrafficType.Guest
    
    protected NetworkType _networkType = NetworkType.Advanced;
    
    public NetworkAPINetworkGuru() {
        _isolationMethods = new IsolationMethod[] { IsolationMethod.VLAN };
    }
    
    public boolean isMyNetworkType(NetworkType networkType) {
    	return this._networkType.equals(networkType);
    }
    
    public NetworkType getSupportedNetworkType() {
    	return this._networkType;
    }

	@Override
	protected boolean canHandle(NetworkOffering offering,
			NetworkType networkType, PhysicalNetwork physicalNetwork) {
		
        if (isMyNetworkType(networkType)
                && isMyTrafficType(offering.getTrafficType()) 
                && isMyIsolationMethod(physicalNetwork)) {
    		s_logger.debug("NetworkAPI can handle this network"
    				+ " with traffic type " + offering.getTrafficType()
    				+ " network type " + networkType 
    				+ " and physical network " + physicalNetwork);
            return true;
        } else {
            s_logger.debug("We only take care of "
            		+ getSupportedTrafficType() + " networks"
            		+ " with isolation method " + getIsolationMethods()
            		+ " and zone of type " + getSupportedNetworkType());
            return false;
        }
	}

	@Override
	public Network design(NetworkOffering offering, DeploymentPlan plan,
			Network userSpecified, Account owner) {
		s_logger.debug("Asking GuestNetworkGuru to design network " + userSpecified.getName());
		return super.design(offering, plan, userSpecified, owner);
	}

	@Override
	public Network implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws InsufficientVirtualNetworkCapcityException {
		s_logger.debug("Asking GuestNetworkGuru to implement network " + network.getName());
		return super.implement(network, offering, dest, context);
	}

	@Override
	public void reserve(NicProfile nic, Network network,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException {
		s_logger.debug("Asking GuestNetworkGuru to reserve nic " + nic.toString() +
				" for network " + network.getName());
		super.reserve(nic, network, vm, dest, context);
	}

	@Override
	public boolean release(NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			String reservationId) {
		s_logger.debug("Asking GuestNetworkGuru to release NIC " + nic.toString()
				+ " from VM " + vm.getInstanceName());
		return super.release(nic, vm, reservationId);
	}

	@Override
	public void shutdown(NetworkProfile profile, NetworkOffering offering) {
		s_logger.debug("Asking GuestNetworkGuru to shutdown network " + profile.getName());
		super.shutdown(profile, offering);
	}

	@Override
	public boolean trash(Network network, NetworkOffering offering,
			Account owner) {
		// TODO Release VLAN networks
		s_logger.debug("### Here we should release VLAN networks from NetworkAPI ###");
		s_logger.debug("VLAN networks released. Passing on to GuestNetworkGuru to trash network " + network.getName());
		return super.trash(network, offering, owner);
	}
	
	

}
