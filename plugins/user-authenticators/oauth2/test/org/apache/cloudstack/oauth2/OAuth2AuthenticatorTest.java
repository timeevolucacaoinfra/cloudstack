package org.apache.cloudstack.oauth2;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URLEncoder;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;

import org.apache.cloudstack.test.utils.SpringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.component.ComponentContext;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class OAuth2AuthenticatorTest {
	
	@Inject
	private OAuth2ManagerImpl _oAuth2Auth;
	
	private final String AUTHLOCATION = "https://www.example.com/oauth/authorize";
	private final String CLIENTID = "djAjpUQ53oXGY1SiUGFM";
	
	@Before
	public void setUp() throws Exception {
		ComponentContext.initComponentsLifeCycle();
		_oAuth2Auth = spy(_oAuth2Auth);
		
		when(_oAuth2Auth.getAuthorizationLocation()).thenReturn(AUTHLOCATION);
		when(_oAuth2Auth.getClientID()).thenReturn(CLIENTID);
	}
	
	@After
	public void testDown() throws Exception {
	}
	
	@Test
	public void testGenerateAuthenticateUrl() throws Exception {
		String returnUrl = "http://localhost:8080/client";
		String encodedUrl = URLEncoder.encode(returnUrl, "UTF-8");
		
		String authUrl = _oAuth2Auth.generateAuthenticationUrl(returnUrl);
		assertNotNull(authUrl);
		assertEquals(AUTHLOCATION + "?redirect_uri=" + encodedUrl + "&client_id=" + CLIENTID, authUrl);
	}
	
	@Test
	public void testAuthenticateThrowsRedirectUrl() throws Exception {
		String returnUrl = "http://localhost:8080/client";
		String encodedUrl = URLEncoder.encode(returnUrl, "UTF-8");
		
		HttpServletRequest req = mock(HttpServletRequest.class);
		
		when(req.getRequestURL()).thenReturn(new StringBuffer(returnUrl));
		
//		try {
//			_oAuth2Auth.authenticate(req);
//			fail();
//		} catch (RedirectResponseException ex) {
//			assertEquals(AUTHLOCATION + "?redirect_uri=" + encodedUrl + "&client_id=" + CLIENTID, ex.getRedirectUrl());
//		}
	}
	
	
    @Configuration
    @ComponentScan(basePackageClasses = {OAuth2ManagerImpl.class}, includeFilters = {@Filter(value = TestConfiguration.Library.class, type = FilterType.CUSTOM)}, useDefaultFilters = false)
    public static class TestConfiguration extends SpringUtils.CloudStackTestConfiguration {    

        @Bean
        public UserAccountDao userAccountDao() {
            return mock(UserAccountDao.class);
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
