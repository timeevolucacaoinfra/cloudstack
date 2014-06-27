package com.globo.dnsapi.dao;

import com.cloud.utils.db.GenericDao;
import com.globo.dnsapi.DnsAPINetworkVO;

public interface DnsAPINetworkDao extends GenericDao<DnsAPINetworkVO, Long> {

	DnsAPINetworkVO findByNetworkId(long networkId);
}
