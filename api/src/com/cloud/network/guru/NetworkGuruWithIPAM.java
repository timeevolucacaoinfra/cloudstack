package com.cloud.network.guru;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientAddressCapacityException;
import com.cloud.exception.InsufficientVirtualNetworkCapcityException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.network.IpAddress;
import com.cloud.network.Network;
import com.cloud.user.Account;

public interface NetworkGuruWithIPAM extends NetworkGuru {
    
    /**
     * 
     * @param network
     * @return
     * @throws InsufficientVirtualNetworkCapcityException
     * @throws InsufficientAddressCapacityException
     * @throws ConcurrentOperationException
     */
    IpAddress allocatePublicIp(Network network, Account owner, Long vlanId) throws ConcurrentOperationException, ResourceAllocationException, InsufficientAddressCapacityException;

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
