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

    // FIXME Review descriptions
    private static final ConfigKey<String> AuthorizationProvider = new ConfigKey<String>("Authentication", String.class, "oauth2.authorization.provider", "",
            "OAuth2 provider name (GOOGLE, MICROSOFT, GITHUB, FACEBOOK or blank to custom)", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> AuthorizationLocation = new ConfigKey<String>("Authentication", String.class, "oauth2.authorization.location", "",
            "URL for OAuth2 authentication if provider is custom", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> TokenLocation = new ConfigKey<String>("Authentication", String.class, "oauth2.token.location", "",
            "URL of your OAuth2 provider to change code token to access token", true, ConfigKey.Scope.Global);
//    private static final ConfigKey<String> AuthGrantType = new ConfigKey<String>("Authentication", String.class, "oauth2.grant.type", "AUTHORIZATION_CODE",
//            "XX", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> ClientID = new ConfigKey<String>("Authentication", String.class, "oauth2.client.id", "",
            "Client ID to be used for OAuth2 authentication", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> ClientSecret = new ConfigKey<String>("Authentication", String.class, "oauth2.client.secret", "",
            "Client secret to be used for OAuth2 authentication", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> Scope = new ConfigKey<String>("Authentication", String.class, "oauth2.scope", "",
            "XXX", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> UserInfoEndPoint = new ConfigKey<String>("Authentication", String.class, "oauth2.userinfo.endpoint", "",
            "XX", true, ConfigKey.Scope.Global);
    private static final ConfigKey<String> UsernameAttribute = new ConfigKey<String>("Authentication", String.class, "oauth2.username.attribute", "login",
            "XX", true, ConfigKey.Scope.Global);
   
    public String generateAuthenticationUrl(String returnUrl) {
        try {
            AuthenticationRequestBuilder builder;
            if (getProviderType() != null) {
                builder = OAuthClientRequest.authorizationProvider(getProviderType());
            } else {
                builder = OAuthClientRequest.authorizationLocation(getAuthorizationLocation());
            }
            OAuthClientRequest request = builder
                    .setClientId(getClientID())
                    .setScope(Scope.value())
                    .setResponseType("code")
                    .setRedirectURI(returnUrl)
                    .buildQueryMessage();
           return request.getLocationUri();
        } catch (OAuthSystemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage());
        }
    }
    
    public UserAccount authenticate(String code, String domainPath) {
        Domain domain = _domainMgr.findDomainByPath(domainPath);
        if (domain == null) {
            throw new IllegalArgumentException("Invalid domain: " + domainPath);
        }

        try {
            TokenRequestBuilder builder;
            if (getProviderType() != null) {
                builder = OAuthClientRequest.tokenProvider(getProviderType());
            } else {
                builder = OAuthClientRequest.tokenLocation(TokenLocation.value());
            }
            OAuthClientRequest request = builder
                    .setClientId(getClientID())
                    .setClientSecret(ClientSecret.value())
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setCode(code)
                    .setScope(Scope.value())
                    .buildQueryMessage();
            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse tokenResponse;
            if (getProviderType() == OAuthProviderType.GITHUB) {
                tokenResponse = oAuthClient.accessToken(request, GitHubTokenResponse.class);
            } else {
                tokenResponse = oAuthClient.accessToken(request);
            }
            String accessToken = tokenResponse.getAccessToken();
            // FIXME Persist
            OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(UserInfoEndPoint.value())
            .setAccessToken(accessToken).buildQueryMessage();
    
            OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);
            if (resourceResponse.getResponseCode() != 200 || "application/json".equalsIgnoreCase(resourceResponse.getContentType())) {
                throw new CloudRuntimeException("Error getting user info in " + UserInfoEndPoint.value() +
                        ". Http status code = " + resourceResponse.getResponseCode() +
                        " content type = " + resourceResponse.getContentType());
            }
            Map<String, Object> json = JSONUtils.parseJSON(resourceResponse.getBody());
            String username = (String) json.get(UsernameAttribute.value());
            if (username == null) {
                // FIXME
                throw new IllegalArgumentException("username invalido");
            }
            if (username.contains("@")) {
                // remove content after @
                username = username.substring(0, username.indexOf('@'));
            }
            
            UserAccount userAcc = _userAccDao.getUserAccount(username, domain.getId());
            if (userAcc == null) {
                throw new IllegalArgumentException("usuario não encontrado");
            }
            return userAcc;
        } catch (OAuthSystemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage(), e);
        } catch (OAuthProblemException e) {
            throw new CloudRuntimeException(e.getLocalizedMessage(), e);
        }
    }

    @Override
    public boolean start() {
        if (!isProviderEnabled()) {
            s_logger.info("OAuth2 Authentication provider is not enabled");
        } else {
            if (getProviderType() != null) {
                s_logger.info("OAuth2 Authentication provider is using custom provider");
            } else {
                s_logger.info("OAuth2 Authentication provider is using provider " + getProviderType());
            }
        }
        return true;
    }

    public boolean isProviderEnabled() {
        if ((getProviderType() != null || (StringUtils.isNotBlank(getAuthorizationLocation()) && StringUtils.isNotBlank(TokenLocation.value()))
                && StringUtils.isNotBlank(ClientID.value()) && StringUtils.isNotBlank(ClientSecret.value()))) {
            return true;
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
                AuthorizationLocation,
                TokenLocation,
                ClientSecret,
                ClientID,
                Scope,
                UserInfoEndPoint,
                UsernameAttribute
        };
    }

    protected OAuthProviderType getProviderType() {
        try {
            if (StringUtils.isNotBlank(AuthorizationProvider.value())) {
                return OAuthProviderType.valueOf(AuthorizationProvider.value());
            }
        } catch (IllegalArgumentException e) {
            s_logger.warn("Unknown authorization provider: " + AuthorizationProvider.value());
        }
        return null;
    }
    
    protected String getAuthorizationLocation() {
        return AuthorizationLocation.value();
    }

    protected String getClientID() {
        return ClientID.value();
    }

}
