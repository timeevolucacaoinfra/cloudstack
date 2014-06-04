package com.globo.networkapi.guru;

import java.util.List;

import javax.inject.Inject;
import javax.naming.ConfigurationException;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.deploy.DeployDestination;
import com.cloud.deploy.DeploymentPlan;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.network.Network;
import com.cloud.network.Network.Provider;
import com.cloud.network.NetworkProfile;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.network.router.VirtualRouter;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.offering.NetworkOffering;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.User;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.VirtualMachineProfile;
import com.globo.networkapi.NetworkAPIVipAccVO;
import com.globo.networkapi.dao.NetworkAPIVipAccDao;
import com.globo.networkapi.manager.NetworkAPIService;

public class NetworkAPIGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(NetworkAPIGuru.class);
    
    // by default traffic type is TrafficType.Guest
    
    @Inject
    NetworkAPIService _networkAPIService;
    @Inject
    VpcVirtualNetworkApplianceManager _routerMgr;
    @Inject
    AccountManager _accountMgr;
    @Inject
    NetworkAPIVipAccDao _napiVipDao;

    protected NetworkType _networkType = NetworkType.Advanced;
    
    public NetworkAPIGuru() {
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
                && offering.getGuestType() == Network.GuestType.Shared
                && isMyTrafficType(offering.getTrafficType()) 
                && isMyIsolationMethod(physicalNetwork)) {

        	if (_networkModel.isProviderEnabledInZone(physicalNetwork.getDataCenterId(), Provider.NetworkAPI.getName())) {
        		s_logger.debug("NetworkAPI can handle this network"
        				+ " with traffic type " + offering.getTrafficType()
        				+ " guest type " + Network.GuestType.Shared
        				+ " network type " + networkType 
        				+ " and physical network " + physicalNetwork);
        		return true;
        	} else {
        		s_logger.debug("NetworkAPI is not enabled for zone" + physicalNetwork.getDataCenterId());
                return false;
        	}
        } else {
            s_logger.debug("We only take care of "
            		+ getSupportedTrafficType() + " " + Network.GuestType.Shared + " networks"
            		+ " with isolation method " + getIsolationMethods()
            		+ " and zone of type " + getSupportedNetworkType());
            return false;
        }
	}

	@Override
	public Network design(NetworkOffering offering, DeploymentPlan plan,
			Network userSpecified, Account owner) {
		
		s_logger.debug("Asking GuestNetworkGuru to design network " + userSpecified.getName());
		NetworkVO network = (NetworkVO) super.design(offering, plan, userSpecified, owner);
		if (network == null) {
			return null;
		}
		// we want implement method be called.
		network.setState(Network.State.Allocated);
		return network;
	}

	@Override
	public Network implement(Network network, NetworkOffering offering,
			DeployDestination dest, ReservationContext context)
			throws InsufficientVirtualNetworkCapcityException {
		s_logger.debug("Creating network " + network.getName() + " in equipment using NetworkAPI");
		
		try {
			_networkAPIService.implementNetwork(network);
		} catch (ConfigurationException e) {
			throw new CloudRuntimeException("Unable to activate network " + network, e);
		}
		return super.implement(network, offering, dest, context);
	}

	@Override
	public NicProfile allocate(Network network, NicProfile nic,
			VirtualMachineProfile vm)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException {
		
		NicProfile nicProf = super.allocate(network, nic, vm);
		
		s_logger.debug("Registering NIC " + nic.toString() + " from VM " + vm.toString() + " in Network API");
		_networkAPIService.registerNicInNetworkAPI(nic, vm, network);
		
		return nicProf;
	}

	@Override
	public void reserve(NicProfile nic, Network network,
			VirtualMachineProfile vm,
			DeployDestination dest, ReservationContext context)
			throws InsufficientVirtualNetworkCapcityException,
			InsufficientAddressCapacityException {
		s_logger.debug("Asking GuestNetworkGuru to reserve nic " + nic.toString() +
				" for network " + network.getName());

		super.reserve(nic, network, vm, dest, context);

		_networkAPIService.validateNic(nic, vm, network);

	}
	
	@Override
	public void deallocate(Network network, NicProfile nic,
			VirtualMachineProfile vm) {

		s_logger.debug("Asking GuestNetworkGuru to deallocate NIC " + nic.toString()
				+ " from VM " + vm.getInstanceName());
		
		// FIXME When NetworkAPI use a effective LoadBalancerImplementation
		// move the code bellow to NetworkElement
		long networkId = nic.getNetworkId();
		List<NetworkAPIVipAccVO> vips = _napiVipDao.findByNetwork(networkId);
		for (NetworkAPIVipAccVO vip: vips) {
			NicVO nicVO = _nicDao.findById(nic.getId());
			_networkAPIService.disassociateNicFromVip(vip.getNapiVipId(), nicVO);
		}

		_networkAPIService.unregisterNicInNetworkAPI(nic, vm);
		
		super.deallocate(network, nic, vm);
	}

	@Override
	public void shutdown(NetworkProfile profile, NetworkOffering offering) {
		
		List<NetworkAPIVipAccVO> vips = _napiVipDao.findByNetwork(profile.getId());
	    if (vips != null && !vips.isEmpty()) {
	    	throw new CloudRuntimeException("There is VIPs related to this network. Network destroyed will be aborted. Delete VIP before.");
	    }

		try {
			List<VirtualRouter> routers = _routerMgr.getRoutersForNetwork(profile.getId());
			for (VirtualRouter router: routers) {
					_routerMgr.destroyRouter(router.getId(), _accountMgr.getAccount(Account.ACCOUNT_ID_SYSTEM), User.UID_SYSTEM);
			}

			s_logger.debug("Removing networks from NetworkAPI");
			_networkAPIService.removeNetworkFromNetworkAPI(profile);
		
			s_logger.debug("Asking GuestNetworkGuru to shutdown network " + profile.getName());
			super.shutdown(profile, offering);
			
		} catch (ResourceUnavailableException e) {
			throw new CloudRuntimeException(e);
		} catch (ConcurrentOperationException e) {
			throw new CloudRuntimeException(e);
		}
	}

	@Override
	public boolean trash(Network network, NetworkOffering offering) {
		
		s_logger.debug("Deallocating VLAN networks from NetworkAPI");
		_networkAPIService.deallocateVlanFromNetworkAPI(network);
		
		s_logger.debug("VLAN networks released. Passing on to GuestNetworkGuru to trash network " + network.getName());
		return super.trash(network, offering);
	}
}
