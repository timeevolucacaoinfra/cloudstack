// Licensed to the Apache Software Foundation (ASF) under one or more
// contributor license agreements.  See the NOTICE file distributed with
// this work for additional information regarding copyright ownership.
// The ASF licenses this file to You under the Apache License, Version 2.0
// (the "License"); you may not use this file except in compliance with
// the License.  You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.apache.cloudstack.oauth2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;
import javax.inject.Inject;

import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.framework.config.ConfigKey;
import org.apache.cloudstack.framework.config.Configurable;
import org.apache.cloudstack.oauth2.api.OAuth2LoginCmd;
import org.apache.cloudstack.oauth2.api.OAuth2RedirectCmd;
import org.apache.log4j.Logger;
import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.AuthenticationRequestBuilder;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest.TokenRequestBuilder;
import org.apache.oltu.oauth2.client.response.GitHubTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.OAuthProviderType;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.utils.JSONUtils;
import org.springframework.stereotype.Component;

import com.cloud.domain.Domain;
import com.cloud.user.DomainManager;
import com.cloud.user.UserAccount;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.utils.StringUtils;
import com.cloud.utils.component.AdapterBase;
import com.cloud.utils.exception.CloudRuntimeException;

@Component
@Local
public class OAuth2ManagerImpl extends AdapterBase implements OAuth2Manager, PluggableAPIAuthenticator, Configurable {

    public static final Logger s_logger = Logger.getLogger(OAuth2ManagerImpl.class);
    
    @Inject
    UserAccountDao _userAccDao;
    @Inject
    DomainManager _domainMgr;
    
    /* Constants */
    private String DEFAULT_ACCESS_SCOPE_GITHUB = "user";
    private String DEFAULT_USER_INFO_URL_GITHUB = "https://api.github.com/user";
    private String DEFAULT_USER_ATTRIBUTE_GITHUB = "login";
    private String DEFAULT_ACCESS_SCOPE_GOOGLE = "openid profile email";
    private String DEFAULT_USER_INFO_URL_GOOGLE = "https://www.googleapis.com/oauth2/v1/userinfo";
    private String DEFAULT_USER_ATTRIBUTE_GOOGLE = "email";


    /* Authorization Provider */
    private static final ConfigKey<String> AuthorizationProvider = new ConfigKey<String>("Authentication", String.class, "oauth2.authorization.provider", "",
            "OAuth2 provider name. Options are: GITHUB or GOOGLE. Leave it blank to use your own OAuth2 provider.", true, ConfigKey.Scope.Global);

    protected String getAuthorizationProvider() { return AuthorizationProvider.value(); }
    
    /* Client ID */
    private static final ConfigKey<String> ClientID = new ConfigKey<String>("Authentication", String.class, "oauth2.client.id", "",
            "Client ID to be used for OAuth2 authentication", true, ConfigKey.Scope.Global);

    protected String getClientID() { return ClientID.value(); }
    
    /* Client Secret */
    private static final ConfigKey<String> ClientSecret = new ConfigKey<String>("Authentication", String.class, "oauth2.client.secret", "",
            "Client Secret to be used for OAuth2 authentication", true, ConfigKey.Scope.Global);

    protected String getClientSecret() { return ClientSecret.value(); }

    /* Access Scope */
    private static final ConfigKey<String> AccessScope = new ConfigKey<String>("Authentication", String.class, "oauth2.access.scope", "",
            "Access scope the user will be prompted to accept by the OAuth2 provider. If set, it should be a list of values separated by commas.", true, ConfigKey.Scope.Global);
    
    protected String getAccessScope() { return AccessScope.value(); }

    /* Authorization URL */
    private static final ConfigKey<String> AuthorizationURL = new ConfigKey<String>("Authentication", String.class, "oauth2.url.authorization", "",
            "URL for OAuth2 authentication if you use your own OAuth2 provider. Otherwise, leave it blank.", true, ConfigKey.Scope.Global);
    
    protected String getAuthorizationURL() { return AuthorizationURL.value(); }
    
    /* Token URL */
    private static final ConfigKey<String> TokenURL = new ConfigKey<String>("Authentication", String.class, "oauth2.url.token", "",
            "URL for OAuth2 code validation if you use your own OAuth2 provider. Otherwise, leave it blank.", true, ConfigKey.Scope.Global);
    
    protected String getTokenURL() { return TokenURL.value(); }
    
    /* User Info URL */
    private static final ConfigKey<String> UserInfoURL = new ConfigKey<String>("Authentication", String.class, "oauth2.url.user", "",
            "URL to retrieve user information if you use your own OAuth2 provider. Otherwise, leave it blank.", true, ConfigKey.Scope.Global);
    
    protected String getUserInfoURL() { return UserInfoURL.value(); }
    
    /* User Attribute */
    private static final ConfigKey<String> UserAttribute = new ConfigKey<String>("Authentication", String.class, "oauth2.user.attribute", "",
            "Attribute to be used for user authentication if you use your own OAuth2 provider. Otherwise, leave it blank.", true, ConfigKey.Scope.Global);
    
    protected String getUserAttribute() { return UserAttribute.value(); }

    /* OAuth2 Domain */
    private static final ConfigKey<String> UserDomain = new ConfigKey<String>("Authentication", String.class, "oauth2.user.domain", "/",
            "Domain name of OAuth2 Users", true, ConfigKey.Scope.Global);
    
    protected String getUserDomain() { return UserDomain.value(); }

    
    /* Implementation */
    public String generateAuthenticationUrl(String returnUrl) {
        if (!isProviderEnabled()) {
            return null;
        }
        try {
            AuthenticationRequestBuilder builder;
            if (getProviderType() != null) {
                builder = OAuthClientRequest.authorizationProvider(getProviderType());
            } else {
                builder = OAuthClientRequest.authorizationLocation(getAuthorizationURL());
            }
            OAuthClientRequest request = builder
                    .setClientId(getClientID())
                    .setScope(getAccessScopeWithProvider())
                    .setResponseType("code")
                    .setRedirectURI(returnUrl)
                    .buildQueryMessage();
           return request.getLocationUri();
        } catch (OAuthSystemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage());
        }
    }
    
    protected String changeCodeToAccessToken(String code, String redirectUri) {
        try {
            TokenRequestBuilder builder;
            if (getProviderType() != null) {
                builder = OAuthClientRequest.tokenProvider(getProviderType());
            } else {
                builder = OAuthClientRequest.tokenLocation(getTokenURL());
            }
            OAuthClientRequest request = builder
                    .setClientId(getClientID())
                    .setClientSecret(getClientSecret())
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setCode(code)
                    .setRedirectURI(redirectUri)
                    .setScope(getAccessScopeWithProvider())
                    .buildBodyMessage();
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse tokenResponse;
            if (getProviderType() == OAuthProviderType.GITHUB) {
                tokenResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
            } else {
                tokenResponse = oAuthClient.accessToken(request);
            }
            String accessToken = tokenResponse.getAccessToken();
            return accessToken;
        } catch (OAuthSystemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage(), e);
        } catch (OAuthProblemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage(), e);
        }
    }
    
    protected String requestUsernameFromUserInfoProviderAPI(String accessToken) {
        try {
            OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(getUserInfoURLWithProvider())
            .setAccessToken(accessToken).buildHeaderMessage();
    
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            if (resourceResponse.getResponseCode() != 200 || "application/json".equalsIgnoreCase(resourceResponse.getContentType())) {
                throw new CloudRuntimeException("Error getting user info in " + getUserInfoURLWithProvider() +
                        ". Http status code = " + resourceResponse.getResponseCode() +
                        " content type = " + resourceResponse.getContentType());
            }
            Map<String, Object> json = JSONUtils.parseJSON(resourceResponse.getBody());
            String username = (String) json.get(getUserAttributeWithProvider());
            if (StringUtils.isNotBlank(username)) {
                if (username.contains("@")) {
                    // remove content after @
                    username = username.substring(0, username.indexOf('@'));
                }
            }
            return username;
        } catch (OAuthSystemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage(), e);
        } catch (OAuthProblemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage(), e);
        }
    }
    
    protected Domain getUserDomainVO() {
        Domain domain = _domainMgr.findDomainByPath(getUserDomain());
        return domain;
    }
    
    public UserAccount authenticate(String code, String redirectUri) {

        String accessToken = changeCodeToAccessToken(code, redirectUri);
        String username = requestUsernameFromUserInfoProviderAPI(accessToken);
        if (username == null) {
            return null;
        }
        
        Domain domain = getUserDomainVO();
        UserAccount userAcc = _userAccDao.getUserAccount(username, domain.getId());
        if (userAcc == null) {
            throw new IllegalArgumentException("User " + username + " not found. Contact administrator.");
        }
        return userAcc;
    }

    @Override
    public boolean start() {
        if (!isProviderEnabled()) {
            s_logger.info("OAuth2 Authentication provider is not enabled");
        } else {
            if (getProviderType() == null) {
                s_logger.info("OAuth2 Authentication provider is using custom provider");
            } else {
                s_logger.info("OAuth2 Authentication provider is using provider " + getProviderType());
            }
        }
        return true;
    }

    public boolean isProviderEnabled() {
        if ((getProviderType() != null || (StringUtils.isNotBlank(getAuthorizationURL()) && StringUtils.isNotBlank(getTokenURL()))
                && StringUtils.isNotBlank(getClientID()) && StringUtils.isNotBlank(getClientSecret()))) {
            // check domain is valid
            if (getUserDomainVO() != null) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public List<Class<?>> getCommands() {
        List<Class<?>> cmdList = new ArrayList<Class<?>>();
        cmdList.add(OAuth2LoginCmd.class);
        cmdList.add(OAuth2RedirectCmd.class);
        return cmdList;
    }

    @Override
    public String getConfigComponentName() {
        return OAuth2ManagerImpl.class.getSimpleName();
    }

    @Override
    public ConfigKey<?>[] getConfigKeys() {
        return new ConfigKey<?>[] {
                AuthorizationProvider,
                AuthorizationURL,
                TokenURL,
                ClientSecret,
                ClientID,
                AccessScope,
                UserInfoURL,
                UserAttribute,
                UserDomain
        };
    }

    protected OAuthProviderType getProviderType() {
        try {
            if (StringUtils.isNotBlank(getAuthorizationProvider())) {
                return OAuthProviderType.valueOf(getAuthorizationProvider());
            }
        } catch (IllegalArgumentException e) {
            s_logger.warn("Unknown authorization provider: " + getAuthorizationProvider());
        }
        return null;
    }

    protected String getAccessScopeWithProvider() {
        if (StringUtils.isNotBlank(getAccessScope())) {
            // If it's set, return whatever was set
            return getAccessScope();
        } else {
            // If it's blank, return according to provider
            OAuthProviderType providerType = getProviderType();
            if (providerType == null) {
                // Custom provider
                return getAccessScope();
            } else {
                switch (providerType) {
                    case GITHUB:
                        return DEFAULT_ACCESS_SCOPE_GITHUB;
                    case GOOGLE:
                        return DEFAULT_ACCESS_SCOPE_GOOGLE;
                    default:
                        return getAccessScope();
                }
            }
        }
    }
    
    protected String getUserInfoURLWithProvider() {
        if (StringUtils.isNotBlank(getUserInfoURL())) {
            // If it's set, return whatever was set
            return getUserInfoURL();
        } else {
            // If it's blank, return according to provider
            OAuthProviderType providerType = getProviderType();
            if (providerType == null) {
                // Custom provider
                return getUserInfoURL();
            } else {
                switch (providerType) {
                    case GITHUB:
                        return DEFAULT_USER_INFO_URL_GITHUB;
                    case GOOGLE:
                        return DEFAULT_USER_INFO_URL_GOOGLE;
                    default:
                        return getUserInfoURL();
                }
            }
        }        
    }
    
    protected String getUserAttributeWithProvider() {
        if (StringUtils.isNotBlank(getUserAttribute())) {
            // If it's set, return whatever was set
            return getUserAttribute();
        } else {
            // If it's blank, return according to provider
            OAuthProviderType providerType = getProviderType();
            if (providerType == null) {
                // Custom provider
                return getUserAttribute();
            } else {
                switch (providerType) {
                    case GITHUB:
                        return DEFAULT_USER_ATTRIBUTE_GITHUB;
                    case GOOGLE:
                        return DEFAULT_USER_ATTRIBUTE_GOOGLE;
                    default:
                        return getUserAttribute();
                }
            }
        }        
    }
}
