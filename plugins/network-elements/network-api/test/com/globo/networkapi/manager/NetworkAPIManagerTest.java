package com.globo.networkapi.manager;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Assert;
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

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.PermissionDeniedException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.Host;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.Network.Provider;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.NetworkVO;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.resource.ServerResource;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
import com.cloud.user.UserContextInitializer;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.Transaction;
import com.globo.networkapi.NetworkAPIEnvironmentVO;
import com.globo.networkapi.commands.CreateNewVlanInNetworkAPICommand;
import com.globo.networkapi.commands.DeallocateVlanFromNetworkAPICommand;
import com.globo.networkapi.dao.NetworkAPIEnvironmentDao;
import com.globo.networkapi.dao.NetworkAPINetworkDao;
import com.globo.networkapi.response.NetworkAPIVlanResponse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class NetworkAPIManagerTest {

    private static long zoneId = 5L;
    private static long networkOfferingId = 10L;
    private static long napiEnvironmentId = 120L;
    private static long physicalNetworkId = 200L;
    private static long napiHostId = 7L;
    private static long domainId = 10L;
    private AccountVO acct = null;
	
	@Inject
	NetworkAPIService _napiService;
 
	@Inject
	DataCenterDao _dcDao;
	
	@Inject
	PhysicalNetworkDao _physicalNetworkDao;
	
	@Inject
	NetworkAPIEnvironmentDao _napiEnvironmentDao;
	
	@Inject
	HostDao _hostDao;
	
	@Inject
	ConfigurationServer _configServer;
	
	@Inject
	AgentManager _agentMgr;

	@Inject
	ResourceManager _resourceMgr;
	
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
        
        UserContext.registerContext(1, acct, null, true);
        when(_acctMgr.getSystemAccount()).thenReturn(new AccountVO());
        when(_acctMgr.getSystemUser()).thenReturn(new UserVO());
    }
    
    @After
    public void testTearDown() {
    	UserContext.unregisterContext();
    	acct = null;
    }
 
    @Test
    public void revertNetworkAPICreationWhenFailureNetworkCreation() throws CloudException {

    	DataCenterVO dc = new DataCenterVO(0L, null, null, null, null, null, null, null, null, null, null, null, null);
    	when(_dcDao.findById(anyLong())).thenReturn(dc);
    	
    	List<PhysicalNetworkVO> pNtwList = new ArrayList<PhysicalNetworkVO>();
    	pNtwList.add(new PhysicalNetworkVO(physicalNetworkId, zoneId, null, null, null, null, null));
    	when(_physicalNetworkDao.listByZone(zoneId)).thenReturn(pNtwList);
    	String networkName = "MockTestNetwork";
    	when(_napiEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(physicalNetworkId, napiEnvironmentId)).thenReturn(new NetworkAPIEnvironmentVO(physicalNetworkId, networkName, napiEnvironmentId));

    	when(_configServer.getConfigValue(Config.NetworkAPIReadTimeout.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120");
    	when(_configServer.getConfigValue(Config.NetworkAPIConnectionTimeout.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120");
    	when(_configServer.getConfigValue(Config.NetworkAPINumberOfRetries.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120");
    	
    	HostVO napiHost = new HostVO(napiHostId, null, null, null, null, null, null, 
    			null, null, null, null, null, null, null, null, null, null, zoneId, null,
    			0L, 0L, null, null, null, 0L, null);    	
    	when(_hostDao.findByTypeNameAndZoneId(zoneId, Provider.NetworkAPI.getName(), Host.Type.L2Networking)).thenReturn(napiHost);
    	
    	Answer answer = new NetworkAPIVlanResponse(new CreateNewVlanInNetworkAPICommand(), null, null, null, null, null, null, null, false);
    	when(_agentMgr.easySend(eq(napiHostId), any(CreateNewVlanInNetworkAPICommand.class))).thenReturn(answer);
    	
    	when(_physicalNetworkDao.findById(physicalNetworkId)).thenReturn(null);
    	
    	try {
	    	_napiService.createNetwork(networkName, networkName, zoneId, networkOfferingId, napiEnvironmentId, null, 
	    			ACLType.Domain, null, null, null, null, true, null);
	    	// This command must throw InvalidParameterValueException, otherwise fails
	    	Assert.fail();
    	} catch (ResourceAllocationException e) {
		   verify(_agentMgr, atLeastOnce()).easySend(eq(napiHostId), any(DeallocateVlanFromNetworkAPICommand.class));
    	}
    }
    
    @Test
    public void checkPermissionsBeforeCreateVlanOnNetworkAPI() throws CloudException {
    	try {
    		when(_acctMgr.finalizeOwner(eq(acct), eq(acct.getAccountName()), eq(domainId), anyLong())).thenThrow(new PermissionDeniedException(""));

    		acct.setDomainId(domainId+1);
        	_napiService.createNetwork("net-name", "display-name", zoneId, networkOfferingId, napiEnvironmentId, null, ACLType.Domain, acct.getAccountName(), null, domainId, null, true, null);
        	fail();
    	} catch (PermissionDeniedException e) {
    		verify(_agentMgr, never()).easySend(any(Long.class), any(Command.class));
    	}
    }

    @Test(expected = InvalidParameterValueException.class)
    public void addNetworkAPIHostInvalidParameters() throws CloudException {
    	
    	String username = null;
    	String password = null;
    	String url = null;
    	
    	UserContext.registerContext(1l, null, null, true);
    	
	    _napiService.addNetworkAPIHost(physicalNetworkId, username, password, url); 
    }
    
    @Test(expected = InvalidParameterValueException.class)
    public void addNetworkAPIHostEmptyParameters() throws CloudException {
    	
    	String username = "";
    	String password = "";
    	String url = "";
    	
    	UserContext.registerContext(1l, null, null, true);
    	
	    _napiService.addNetworkAPIHost(physicalNetworkId, username, password, url); 
    }
      
    @Test
    public void addNetworkAPIHost() throws CloudException {
    	
    	String username = "testUser";
    	String password = "testPwd";
    	String url = "testUrl";
    	
    	PhysicalNetworkVO pNtwk = new PhysicalNetworkVO(physicalNetworkId, zoneId, null, null, null, null, null);
    	when(_physicalNetworkDao.findById(physicalNetworkId)).thenReturn(pNtwk);
    	
    	when(_configServer.getConfigValue(Config.NetworkAPIReadTimeout.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120000");
    	when(_configServer.getConfigValue(Config.NetworkAPIConnectionTimeout.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120000");
    	when(_configServer.getConfigValue(Config.NetworkAPINumberOfRetries.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("0");
    	
    	HostVO napiHost = new HostVO(1L, "NetworkAPI", null, "Up", "L2Networking", "", null, 
    			null, "", null, null, null, null, null, null, null, null, zoneId, null,
    			0L, 0L, null, null, null, 0L, null);

    	when(_resourceMgr.addHost(eq(zoneId), any(ServerResource.class), eq(Host.Type.L2Networking), anyMapOf(String.class, String.class))).thenReturn(napiHost);
    	
    	Transaction tx = Transaction.open(Transaction.CLOUD_DB);
    	try {
	    	UserContext.registerContext(1l, null, null, true);
	    	
		    Host host = _napiService.addNetworkAPIHost(physicalNetworkId, username, password, url);
		    assertNotNull(host);
		    assertEquals(host.getDataCenterId(), zoneId);
		    assertEquals(host.getName(), "NetworkAPI");
    	} finally {
    		tx.rollback();
    	}
    }
    
    @Configuration
    @ComponentScan(basePackageClasses = {NetworkAPIManager.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {
    	
    	@Bean
    	public DomainDao domainDao() {
    		return mock(DomainDao.class);
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
    	public HostPodDao hostPodDao() {
    		return mock(HostPodDao.class);
    	}
    	@Bean
    	public PhysicalNetworkDao physicalNetworkDao() {
    		return mock(PhysicalNetworkDao.class);
    	}
    	@Bean
    	public NetworkOfferingDao networkOfferingDao() {
    		return mock(NetworkOfferingDao.class);
    	}
    	@Bean
    	public UserDao userDao() {
    		return mock(UserDao.class);
    	}
    	@Bean
    	public NetworkDao networkDao() {
    		return mock(NetworkDao.class);
    	}
    	@Bean
    	public NetworkServiceMapDao networkServiceMapDao() {
    		return mock(NetworkServiceMapDao.class);
    	}
    	@Bean
    	public NetworkAPINetworkDao networkAPINetworkDao() {
    		return mock(NetworkAPINetworkDao.class);
    	}
    	@Bean
    	public NetworkAPIEnvironmentDao networkAPIEnvironmentDao() {
    		return mock(NetworkAPIEnvironmentDao.class);
    	}
    	@Bean
    	public NetworkModel networkModel() {
    		return mock(NetworkModel.class);
    	}
    	@Bean
    	public AgentManager agentManager() {
    		return mock(AgentManager.class);
    	}
    	@Bean
    	public ConfigurationManager configurationManager() {
    		return mock(ConfigurationManager.class);
    	}
    	@Bean
    	public ResourceManager resourceManager() {
    		return mock(ResourceManager.class);
    	}
    	@Bean
    	public DomainManager domainManager() {
    		return mock(DomainManager.class);
    	}
    	@Bean
    	public NetworkManager networkManager() {
    		return mock(NetworkManager.class);
    	}
    	@Bean
    	public AccountManager accountManager() {
    		return mock(AccountManager.class);
    	}
    	@Bean
    	public ConfigurationServer configurationServer() {
    		return mock(ConfigurationServer.class);
    	}
    	@Bean
    	public NetworkService networkService() {
    		return mock(NetworkService.class);
    	}
    	
    	@Bean
    	public UserContextInitializer userContextInitializer() {
    		return new UserContextInitializer();
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
