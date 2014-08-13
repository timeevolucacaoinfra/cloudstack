/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.globo.globonetwork.cloudstack.guru;

import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.owasp.esapi.waf.ConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.VlanDao;
import com.cloud.exception.CloudException;
import com.cloud.network.IpAddressManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.dao.IPAddressDao;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.router.VpcVirtualNetworkApplianceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.dao.NicDao;
import com.globo.globonetwork.cloudstack.dao.GloboNetworkVipAccDao;
import com.globo.globonetwork.cloudstack.manager.GloboNetworkService;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class GloboNetworkGuruTest {

	private static long domainId = 10L;
	private AccountVO acct = null;
	private UserVO user = null;
	
	@Inject
	GloboNetworkGuru _globoNetworkGuru;
	
	@Inject
	GloboNetworkService _globoNetworkService;
	
	@Inject
	AccountManager _acctMgr; 
	
    @BeforeClass
    public static void setUp() throws ConfigurationException {
    }
 
    @Before
    public void testSetUp() {
        ComponentContext.initComponentsLifeCycle();
        
        acct = new AccountVO(200L);
        acct.setType(Account.ACCOUNT_TYPE_NORMAL);
        acct.setAccountName("user");
        acct.setDomainId(domainId);

        user = new UserVO();
        user.setUsername("user");
        user.setAccountId(acct.getAccountId());

        CallContext.register(user, acct);
        when(_acctMgr.getSystemAccount()).thenReturn(this.acct);
        when(_acctMgr.getSystemUser()).thenReturn(this.user);
    }
    
    @After
    public void testTearDown() {
    	CallContext.unregister();
    	acct = null;
    }
    
    @Test
    public void removeNetworkInCSWhenNetworkDoesntExistInGloboNetwork() throws CloudException {
    	
    	// _napiGuru.trash(network, offering, acct);
    }
    
    @Configuration
    @ComponentScan(basePackageClasses = {GloboNetworkGuru.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {
    	
    	@Bean
    	public GloboNetworkGuru globoNetworkGuru() {
    		return new GloboNetworkGuru();
    	}
    	@Bean
    	public NetworkOrchestrationService networkManager() {
    		return mock(NetworkOrchestrationService.class);
    	}
    	@Bean
    	public DataCenterDao dataCenterDao() {
    		return mock(DataCenterDao.class);
    	}
    	@Bean
    	public VlanDao vlanDao() {
    		return mock(VlanDao.class);
    	}
    	@Bean
    	public NicDao nicDao() {
    		return mock(NicDao.class);
    	}
    	@Bean
    	public ConfigurationDao configurationDao() {
    		return mock(ConfigurationDao.class);
    	}
    	@Bean
    	public NetworkDao networkDao() {
    		return mock(NetworkDao.class);
    	}
    	@Bean
    	public IPAddressDao ipAddressDao() {
    		return mock(IPAddressDao.class);
    	}
    	@Bean
    	public PhysicalNetworkDao physicalNetworkDao() {
    		return mock(PhysicalNetworkDao.class);
    	}
    	@Bean
    	public GloboNetworkVipAccDao globoNetworkVipAccDao() {
    	   return mock(GloboNetworkVipAccDao.class);
    	}
    	@Bean
    	public VpcVirtualNetworkApplianceManager vpcVirtualNetworkApplianceManager() {
    		return mock(VpcVirtualNetworkApplianceManager.class);
    	}
    	@Bean
    	public IpAddressManager ipAddressManager() {
    		return mock(IpAddressManager.class);
    	}
    	@Bean
    	public ConfigurationServer configurationServer() {
    		return mock(ConfigurationServer.class);
    	}
    	@Bean
    	public GloboNetworkService globoNetworkService() {
    		return mock(GloboNetworkService.class);
    	}
    	@Bean
    	public NetworkModel networkModel() {
    		return mock(NetworkModel.class);
    	}
    	@Bean
    	public AccountManager accountManager() {
    		return mock(AccountManager.class);
    	}
    	
    	public static class Library implements TypeFilter {
    		 
            @Override
            public boolean match(MetadataReader mdr, MetadataReaderFactory arg1) throws IOException {
                ComponentScan cs = TestConfiguration.class.getAnnotation(ComponentScan.class);
                return SpringUtils.includedInBasePackageClasses(mdr.getClassMetadata().getClassName(), cs);
            }
        }
    }


}
