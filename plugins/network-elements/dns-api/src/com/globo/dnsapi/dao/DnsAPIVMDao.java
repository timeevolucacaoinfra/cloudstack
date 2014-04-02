package com.globo.dnsapi.dao;

import com.cloud.utils.db.GenericDao;
import com.globo.dnsapi.DnsAPIVMVO;

public interface DnsAPIVMDao extends GenericDao<DnsAPIVMVO, Long> {

	DnsAPIVMVO findByVMId(long vmId);
}
