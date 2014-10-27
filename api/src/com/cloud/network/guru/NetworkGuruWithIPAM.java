package com.cloud.network.guru;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;

public interface NetworkGuruWithIPAM extends NetworkGuru {
    
    /**
     * 
     * @param network
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    IpAddress allocate(Network network, IpAddress ipAddress) throws InsufficientVirtualNetworkCapcityException, InsufficientAddressCapacityException, ConcurrentOperationException;

    /**
     * When is is released, the NetworkGuru is informed via the
     * release() method to release any resources.
     *
     * @param nic nic that the vm is using to access the guest network.
     * @param vm virtual machine
     * @param reservationId reservation id passed to it in the ReservationContext
     * @return true if release is successful or false if unsuccessful.
     */
    boolean release(Network network, IpAddress ipAddress);

}
