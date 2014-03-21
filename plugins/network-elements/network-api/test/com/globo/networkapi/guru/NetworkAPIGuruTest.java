package com.globo.networkapi.guru;

import static org.mockito.Mockito.when;

import java.io.IOException;

import javax.inject.Inject;

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

import com.cloud.exception.CloudException;
import com.cloud.user.Account;
import com.cloud.user.AccountManager;
import com.cloud.user.AccountVO;
import com.cloud.user.UserContext;
import com.cloud.user.UserContextInitializer;
import com.cloud.user.UserVO;
import com.cloud.utils.component.ComponentContext;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class NetworkAPIGuruTest {

	private static long domainId = 10L;
	private AccountVO acct = null;
	
	@Inject
	NetworkAPIGuru _napiGuru;
	
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
    public void removeNetworkInCSWhenNetworkDoesntExistInNetworkAPI() throws CloudException {
    	
    	// _napiGuru.trash(network, offering, acct);
    }
    
    @Configuration
    @ComponentScan(basePackageClasses = {NetworkAPIGuru.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {
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
