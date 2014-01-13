package com.globo.networkapi.guru;

import org.apache.log4j.Logger;

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.network.Network;
import com.cloud.network.PhysicalNetwork;
import com.cloud.network.Network.GuestType;
import com.cloud.network.Network.Service;
import com.cloud.network.PhysicalNetwork.IsolationMethod;
import com.cloud.network.guru.GuestNetworkGuru;
import com.cloud.offering.NetworkOffering;

public class NetworkAPINetworkGuru extends GuestNetworkGuru {
    private static final Logger s_logger = Logger.getLogger(NetworkAPINetworkGuru.class);
    
    public NetworkAPINetworkGuru() {
        _isolationMethods = new IsolationMethod[] { IsolationMethod.VLAN };
        System.out.println("\n\n\n\n\n\n*********** FUI CHAMADO");
        s_logger.trace("O logging está funcionando");
    }

	@Override
	protected boolean canHandle(NetworkOffering offering,
			NetworkType networkType, PhysicalNetwork physicalNetwork) {
		s_logger.trace("Asking for handling network with offering " + offering +
				" network type " + networkType + " and physical network " + physicalNetwork);
        // This guru handles only Guest Isolated network that supports Source nat service
        if (networkType == NetworkType.Advanced
                && isMyTrafficType(offering.getTrafficType()) 
                && isMyIsolationMethod(physicalNetwork)) {
            return true;
        } else {
            s_logger.trace("We only take care of Guest networks of type   " + GuestType.Shared + " in zone of type " + NetworkType.Advanced);
            return true;
        }
	}

}
