package com.globo.networkapi.manager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.cloudstack.acl.ControlledEntity.ACLType;
import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.owasp.esapi.waf.ConfigurationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.cloud.agent.AgentManager;
import com.cloud.agent.api.Answer;
import com.cloud.configuration.Config;
import com.cloud.configuration.ConfigurationManager;
import com.cloud.dc.DataCenterVO;
import com.cloud.dc.dao.DataCenterDao;
import com.cloud.dc.dao.HostPodDao;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudException;
import com.cloud.exception.InvalidParameterValueException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.host.Host.Type;
import com.cloud.host.HostVO;
import com.cloud.host.dao.HostDao;
import com.cloud.network.NetworkManager;
import com.cloud.network.NetworkModel;
import com.cloud.network.NetworkService;
import com.cloud.network.dao.NetworkDao;
import com.cloud.network.dao.NetworkServiceMapDao;
import com.cloud.network.dao.PhysicalNetworkDao;
import com.cloud.network.dao.PhysicalNetworkVO;
import com.cloud.offerings.dao.NetworkOfferingDao;
import com.cloud.resource.ResourceManager;
import com.cloud.server.ConfigurationServer;
import com.cloud.user.AccountManager;
import com.cloud.user.DomainManager;
import com.cloud.user.UserContext;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.component.ComponentContext;
import com.globo.networkapi.NetworkAPIEnvironmentVO;
import com.globo.networkapi.commands.CreateNewVlanInNetworkAPICommand;
import com.globo.networkapi.commands.DeallocateVlanFromNetworkAPICommand;
import com.globo.networkapi.dao.NetworkAPIEnvironmentDao;
import com.globo.networkapi.dao.NetworkAPINetworkDao;
import com.globo.networkapi.resource.NetworkAPIResource;
import com.globo.networkapi.response.NetworkAPIVlanResponse;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)// NOTE #1
@ContextConfiguration(loader = AnnotationConfigContextLoader.class) // NOTE #2
public class NetworkAPIManagerTest {

    private static long zoneId = 5L;
    private static long networkOfferingId = 10L;
    private static long napiEnvironmentId = 120L;
    private static long physicalNetworkId = 200L;
    private static long napiHostId = 7L;
	
	@Inject
	NetworkAPIService _napiService;
 
	@Inject
	DataCenterDao _dcDao;
	
	@Inject
	PhysicalNetworkDao _physicalNetworkDao;
	
	@Inject
	NetworkAPIEnvironmentDao _napiEnvironmentDao;
	
	@Inject
	ConfigurationServer _configServer;
	
	@Inject
	AgentManager _agentMgr;

	@Inject
	ResourceManager _resourceMgr;
 
    @BeforeClass
    public static void setUp() throws ConfigurationException {
    }
 
    @Before
    public void testSetUp() {
        ComponentContext.initComponentsLifeCycle();  // NOTE #3
//        AccountVO acct = new AccountVO(200L);
//        acct.setType(Account.ACCOUNT_TYPE_NORMAL);
//        acct.setAccountName("user");
//        acct.setDomainId(domainId);
// 
//        UserContext.registerContext(1, acct, null, true);
 
//        when(_acctMgr.finalizeOwner((Account) anyObject(), anyString(), anyLong(), anyLong())).thenReturn(acct);
//        when(_processor.getType()).thenReturn("mock");
//        when(_accountDao.findByIdIncludingRemoved(0L)).thenReturn(acct);
 
//        AffinityGroupVO group = new AffinityGroupVO("group1", "mock", "mock group", domainId, 200L);
//        Mockito.when(_affinityGroupDao.persist(Mockito.any(AffinityGroupVO.class))).thenReturn(group);
//        Mockito.when(_affinityGroupDao.findById(Mockito.anyLong())).thenReturn(group);
//        Mockito.when(_affinityGroupDao.findByAccountAndName(Mockito.anyLong(), Mockito.anyString())).thenReturn(group);
//        Mockito.when(_affinityGroupDao.lockRow(Mockito.anyLong(), anyBoolean())).thenReturn(group);
//        Mockito.when(_affinityGroupDao.expunge(Mockito.anyLong())).thenReturn(true);
//        Mockito.when(_eventDao.persist(Mockito.any(EventVO.class))).thenReturn(new EventVO());
    } 
 
//    @Test(expected = ResourceInUseException.class)
//    public void deleteAffinityGroupInUse() throws ResourceInUseException {
//        List<AffinityGroupVMMapVO> affinityGroupVmMap = new ArrayList<AffinityGroupVMMapVO>();
//        AffinityGroupVMMapVO mapVO = new AffinityGroupVMMapVO(20L, 10L);
//        affinityGroupVmMap.add(mapVO);
//        when(_affinityGroupVMMapDao.listByAffinityGroup(20L)).thenReturn(affinityGroupVmMap);
// 
//        AffinityGroupVO groupVO = new AffinityGroupVO();
//        when(_groupDao.findById(20L)).thenReturn(groupVO);
//        when(_groupDao.lockRow(20L, true)).thenReturn(groupVO);
// 
//        _affinityService.deleteAffinityGroup(20L, "user", domainId, null);
//    }
    
    @Test
    public void revertNetworkAPICreationWhenFailureNetworkCreation() throws CloudException {
    	
    	DataCenterVO dc = new DataCenterVO(0L, null, null, null, null, null, null, null, null, null, null, null, null);
    	when(_dcDao.findById(anyLong())).thenReturn(dc);
    	
    	List<PhysicalNetworkVO> pNtwList = new ArrayList<PhysicalNetworkVO>();
    	pNtwList.add(new PhysicalNetworkVO(physicalNetworkId, zoneId, null, null, null, null, null));
    	when(_physicalNetworkDao.listByZone(zoneId)).thenReturn(pNtwList);
    	String networkName = "MockTestNetwork";
    	when(_napiEnvironmentDao.findByPhysicalNetworkIdAndEnvironmentId(physicalNetworkId, napiEnvironmentId)).thenReturn(new NetworkAPIEnvironmentVO(physicalNetworkId, networkName, napiEnvironmentId));

    	when(_configServer.getConfigValue(Config.NetworkAPIUsername.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("test");
    	when(_configServer.getConfigValue(Config.NetworkAPIPassword.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("test");
    	when(_configServer.getConfigValue(Config.NetworkAPIUrl.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("test");
    	when(_configServer.getConfigValue(Config.NetworkAPIReadTimeout.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120");
    	when(_configServer.getConfigValue(Config.NetworkAPIConnectionTimeout.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120");
    	when(_configServer.getConfigValue(Config.NetworkAPINumberOfRetries.key(), Config.ConfigurationParameterScope.global.name(), null)).thenReturn("120");
    	
    	HostVO napiHost = new HostVO(napiHostId, null, null, null, null, null, null, 
    			null, null, null, null, null, null, null, null, null, null, zoneId, null,
    			0L, 0L, null, null, null, 0L, null);
    	when(_resourceMgr.addHost(eq(1l), any(NetworkAPIResource.class), any(Type.class), anyMapOf(String.class, String.class))).thenReturn(napiHost);
    	
    	Answer answer = new NetworkAPIVlanResponse(new CreateNewVlanInNetworkAPICommand(), null, null, null, null, null, null, null, false);
    	when(_agentMgr.easySend(eq(napiHostId), any(CreateNewVlanInNetworkAPICommand.class))).thenReturn(answer);
    	
    	when(_physicalNetworkDao.findById(physicalNetworkId)).thenReturn(null);
    	
    	UserContext.registerContext(1l, null, null, true);
    	
    	try {
	    	_napiService.createNetwork(networkName, networkName, zoneId, networkOfferingId, napiEnvironmentId, null, 
	    			ACLType.Domain, null, null, null, null, true, null);
	    	// This command must throw InvalidParameterValueException, otherwise fails
	    	Assert.fail();
    	} catch (ResourceAllocationException e) {
		   verify(_agentMgr, atLeastOnce()).easySend(eq(napiHostId), any(DeallocateVlanFromNetworkAPICommand.class));
    	}
    }
  
    @Configuration
    @ComponentScan(basePackageClasses = {NetworkAPIManager.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration { // NOTE #4
 
//        @Bean
//        public AccountDao accountDao() {
//            return Mockito.mock(AccountDao.class);
//        }
    	
    	@Bean
    	public DomainDao domainDao() {
    		return Mockito.mock(DomainDao.class);
    	}
    	@Bean
    	public HostDao hostDao() {
    		return Mockito.mock(HostDao.class);
    	}
    	@Bean
    	public DataCenterDao dataCenterDao() {
    		return Mockito.mock(DataCenterDao.class);
    	}
    	@Bean
    	public HostPodDao hostPodDao() {
    		return Mockito.mock(HostPodDao.class);
    	}
    	@Bean
    	public PhysicalNetworkDao physicalNetworkDao() {
    		return Mockito.mock(PhysicalNetworkDao.class);
    	}
    	@Bean
    	public NetworkOfferingDao networkOfferingDao() {
    		return Mockito.mock(NetworkOfferingDao.class);
    	}
    	@Bean
    	public UserDao userDao() {
    		return Mockito.mock(UserDao.class);
    	}
    	@Bean
    	public NetworkDao networkDao() {
    		return Mockito.mock(NetworkDao.class);
    	}
    	@Bean
    	public NetworkServiceMapDao networkServiceMapDao() {
    		return Mockito.mock(NetworkServiceMapDao.class);
    	}
    	@Bean
    	public NetworkAPINetworkDao networkAPINetworkDao() {
    		return Mockito.mock(NetworkAPINetworkDao.class);
    	}
    	@Bean
    	public NetworkAPIEnvironmentDao networkAPIEnvironmentDao() {
    		return Mockito.mock(NetworkAPIEnvironmentDao.class);
    	}
    	@Bean
    	public NetworkModel networkModel() {
    		return Mockito.mock(NetworkModel.class);
    	}
    	@Bean
    	public AgentManager agentManager() {
    		return Mockito.mock(AgentManager.class);
    	}
    	@Bean
    	public ConfigurationManager configurationManager() {
    		return Mockito.mock(ConfigurationManager.class);
    	}
    	@Bean
    	public ResourceManager resourceManager() {
    		return Mockito.mock(ResourceManager.class);
    	}
    	@Bean
    	public DomainManager domainManager() {
    		return Mockito.mock(DomainManager.class);
    	}
    	@Bean
    	public NetworkManager networkManager() {
    		return Mockito.mock(NetworkManager.class);
    	}
    	@Bean
    	public AccountManager accountManager() {
    		return Mockito.mock(AccountManager.class);
    	}
    	@Bean
    	public ConfigurationServer configurationServer() {
    		return Mockito.mock(ConfigurationServer.class);
    	}
    	@Bean
    	public NetworkService networkService() {
    		return Mockito.mock(NetworkService.class);
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