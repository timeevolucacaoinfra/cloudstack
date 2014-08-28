// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.oauth2.api;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.oauth2.OAuth2Manager;
import org.apache.cloudstack.oauth2.response.OAuth2UrlResponse;
import org.apache.log4j.Logger;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.UserAccount;

@APICommand(name = "oAuth2Login", description = "Authenticates using OAuth2", responseObject = OAuth2UrlResponse.class, since = "4.5.0")
public class OAuth2LoginCmd extends BaseCmd implements APIAuthenticator {
	public static final Logger s_logger = Logger
			.getLogger(OAuth2LoginCmd.class.getName());
	
	private static final String s_name = "oauth2urlresponse";

    @Inject
    ApiServerService _apiServer;

    @Inject
	OAuth2Manager _oauth2Manager;

    @Override
    public void execute() throws ServerApiException {
        // We should never reach here
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication api, cannot be used directly");
    }

    @Override
    public String authenticate(final String command, final Map<String, Object[]> params, final HttpSession session, final String remoteAddress, final String responseType, final StringBuilder auditTrailSb, final HttpServletResponse resp) throws ServerApiException {
        // FIXME Put string constants in ApiConstants
        if (params.containsKey("code") && params.containsKey("redirect_uri")) {
            String code = String.valueOf(params.get("code")[0]);
            String redirectUri = String.valueOf(params.get("redirect_uri")[0]);
            UserAccount userAcc = _oauth2Manager.authenticate(code, redirectUri);
            if (userAcc != null) {
                try {
                    if (_apiServer.verifyUser(userAcc.getId())) {
                        return ApiResponseSerializer.toSerializedString(
                                _apiServer.loginUser(session, userAcc.getUsername(), userAcc.getPassword(), userAcc.getDomainId(), null, remoteAddress, params),
                                responseType);
                    }
                } catch (final CloudAuthenticationException ignored) {
                }
            }
        }
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                "Unable to authenticate or retrieve user while performing OAuth2 based SSO",
                params, responseType));
    }
	
	@Override
	public String getCommandName() {
		return s_name;
	}

	@Override
	public long getEntityOwnerId() {
	    return Account.ACCOUNT_TYPE_NORMAL;
	}

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        for (PluggableAPIAuthenticator authManager: authenticators) {
            if (authManager instanceof OAuth2Manager) {
                _oauth2Manager = (OAuth2Manager) authManager;
            }
        }
        if (_oauth2Manager == null) {
            s_logger.error("No suitable Pluggable Authentication Manager found for SAML2 Login Cmd");
        }
    }

}
