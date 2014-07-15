package com.globo.dnsapi.element;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import javax.inject.Inject;

import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.engine.orchestration.service.NetworkOrchestrationService;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.AgentManager;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.deploy.DeployDestination;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceUnavailableException;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.projects.ProjectManager;
import com.cloud.resource.ResourceManager;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.vm.Nic;
import com.cloud.vm.NicProfile;
import com.cloud.vm.NicVO;
import com.cloud.vm.ReservationContext;
import com.cloud.vm.ReservationContextImpl;
import com.cloud.vm.VirtualMachine;
import com.cloud.vm.VirtualMachineProfile;
import com.cloud.vm.dao.NicDao;
import com.cloud.vm.dao.VMInstanceDao;
import com.globo.dnsapi.DnsAPINetworkVO;
import com.globo.dnsapi.dao.DnsAPINetworkDao;
import com.globo.dnsapi.dao.DnsAPIVirtualMachineDao;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class DnsAPIElementTest {

    private static long zoneId = 5L;
    private static long networkOfferingId = 10L;
    private static long napiEnvironmentId = 120L;
    private static long physicalNetworkId = 200L;
    private static long napiHostId = 7L;
    private static long domainId = 10L;
    private AccountVO acct = null;
	private UserVO user = null;
	
	@Inject
	DataCenterDao _datacenterDao;
	
	@Inject
	DnsAPIElement _dnsapiElement;
	
	@Inject
	DnsAPINetworkDao _dnsapiNetworkDao;

	@Inject
	AccountManager _acctMgr;

	@Before
	public void setUp() throws Exception {
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
	public void tearDown() throws Exception {
    	CallContext.unregister();
    	acct = null;
    }

    @Test
	public void testGetCapabilities() {
		//fail("Not yet implemented");
	}

	@Test(expected=InvalidParameterValueException.class)
	public void testOnlyLowerCaseCharactersAreNotAllowed() throws ConcurrentOperationException, ResourceUnavailableException, InsufficientCapacityException {
		Network network = mock(Network.class);
		when(network.getDataCenterId()).thenReturn(domainId);
		when(network.getId()).thenReturn(1l);
		NicProfile nic = new NicProfile();
		VirtualMachineProfile vm = mock(VirtualMachineProfile.class);
		when(vm.getHostName()).thenReturn("UPPERCASENAME");
		when(vm.getType()).thenReturn(VirtualMachine.Type.User);
		when(_datacenterDao.findById(domainId)).thenReturn(mock(DataCenterVO.class));
		when(_dnsapiNetworkDao.findByNetworkId(network.getId())).thenReturn(new DnsAPINetworkVO());
		DeployDestination dest = new DeployDestination();
		ReservationContext context = new ReservationContextImpl(null, null, user);
		_dnsapiElement.prepare(network, nic, vm, dest, context);
	}

    @Configuration
    @ComponentScan(basePackageClasses = {DnsAPIElement.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {

    	@Bean
    	public DnsAPINetworkDao dnsapiNetworkDao() {
    		return mock(DnsAPINetworkDao.class);
    	}
    	@Bean
    	public DnsAPIVirtualMachineDao dnsAPIVirtualMachineDao() {
    		return mock(DnsAPIVirtualMachineDao.class);
    	}
    	@Bean
    	public HostDao hostDao() {
    		return mock(HostDao.class);
    	}
    	@Bean
    	public DataCenterDao dataCenterDao() {
    		return mock(DataCenterDao.class);
    	}
    	@Bean
    	public PhysicalNetworkDao physicalNetworkDao() {
    		return mock(PhysicalNetworkDao.class);
    	}
    	@Bean
    	public NetworkDao networkDao() {
    		return mock(NetworkDao.class);
    	}
    	@Bean
    	public ConfigurationDao configurationDao() {
    		return mock(ConfigurationDao.class);
    	}
    	@Bean
    	public AgentManager agentManager() {
    		return mock(AgentManager.class);
    	}
    	@Bean
    	public ResourceManager resourceManager() {
    		return mock(ResourceManager.class);
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
