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
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.Networks.TrafficType;
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
        s_logger.trace("O logging está funcionando");
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
    		s_logger.trace("NetworkAPI can handling network with traffic type " + offering.getTrafficType() +
    				" network type " + networkType + " and physical network " + physicalNetwork);
            return true;
        } else {
            s_logger.trace("We only take care of " + getSupportedTrafficType() + " networks with isolation method " + getIsolationMethods() + " and zone of type " + getSupportedNetworkType());
            return false;
        }
	}

	/* (non-Javadoc)
	 * @see com.cloud.network.guru.GuestNetworkGuru#design(com.cloud.offering.NetworkOffering, com.cloud.deploy.DeploymentPlan, com.cloud.network.Network, com.cloud.user.Account)
	 */
	@Override
	public Network design(NetworkOffering offering, DeploymentPlan plan,
			Network userSpecified, Account owner) {
		// TODO Auto-generated method stub
		s_logger.info("Design method called with args " + offering + " " + plan + " " + userSpecified + " " + owner);
		return super.design(offering, plan, userSpecified, owner);
	}

	/* (non-Javadoc)
	 * @see com.cloud.network.guru.GuestNetworkGuru#implement(com.cloud.network.Network, com.cloud.offering.NetworkOffering, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
	 */
	@Override
	public Network implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws InsufficientVirtualNetworkCapcityException {
		// TODO Auto-generated method stub
		s_logger.info("Implement method called with args " + network + " " + offering + " " + dest + " " + context);
		return super.implement(network, offering, dest, context);
	}

	/* (non-Javadoc)
	 * @see com.cloud.network.guru.GuestNetworkGuru#reserve(com.cloud.vm.NicProfile, com.cloud.network.Network, com.cloud.vm.VirtualMachineProfile, com.cloud.deploy.DeployDestination, com.cloud.vm.ReservationContext)
	 */
	@Override
	public void reserve(NicProfile nic, Network network,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			DeployDestination dest, ReservationContext context)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException {
		// TODO Auto-generated method stub
		s_logger.info("reserve method called with args " + network + " " + vm + " " + dest + " " + context);
		super.reserve(nic, network, vm, dest, context);
	}

	/* (non-Javadoc)
	 * @see com.cloud.network.guru.GuestNetworkGuru#release(com.cloud.vm.NicProfile, com.cloud.vm.VirtualMachineProfile, java.lang.String)
	 */
	@Override
	public boolean release(NicProfile nic,
			VirtualMachineProfile<? extends VirtualMachine> vm,
			String reservationId) {
		// TODO Auto-generated method stub
		s_logger.info("release method called with args " + nic + " " + vm + " " + reservationId);
		return super.release(nic, vm, reservationId);
	}

	/* (non-Javadoc)
	 * @see com.cloud.network.guru.GuestNetworkGuru#shutdown(com.cloud.network.NetworkProfile, com.cloud.offering.NetworkOffering)
	 */
	@Override
	public void shutdown(NetworkProfile profile, NetworkOffering offering) {
		// TODO Auto-generated method stub
		s_logger.info("shutdown method called with args " + profile + " " + profile);
		super.shutdown(profile, offering);
	}

	/* (non-Javadoc)
	 * @see com.cloud.network.guru.GuestNetworkGuru#trash(com.cloud.network.Network, com.cloud.offering.NetworkOffering, com.cloud.user.Account)
	 */
	@Override
	public boolean trash(Network network, NetworkOffering offering,
			Account owner) {
		// TODO Auto-generated method stub
		s_logger.info("trash method called with args " + network + " " + network + " " + owner);
		return super.trash(network, offering, owner);
	}
	
	

}
