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

(function() {
	// put all code in a private scope, but not in document.ready event
	// to run this code before cloud.core.callback.js document.ready

	function removeParam(key, sourceURL) {
	    var rtn = sourceURL.split("?")[0],
	        param,
	        params_arr = [],
	        queryString = (sourceURL.indexOf("?") !== -1) ? sourceURL.split("?")[1] : "";
	    if (queryString !== "") {
	        params_arr = queryString.split("&");
	        for (var i = params_arr.length - 1; i >= 0; i -= 1) {
	            param = params_arr[i].split("=")[0];
	            if (param === key) {
	                params_arr.splice(i, 1);
	            }
	        }
	        rtn = rtn + "?" + params_arr.join("&");
	    }
	    return rtn;
	}

	// if direct=true, don't use oauth2
	if ($.urlParam("direct") !== "true") {
		var code = $.urlParam("code");
		var redirectUri = $.cookie('oauth_redirect');
		if (code && redirectUri) {
			// remove parameter code from url
			history.replaceState(null, null, removeParam("code", window.location.href));
			
			$.ajax({
	            url: clientApiUrl + '?command=oAuth2Login&response=json&code=' + code + '&redirect_uri=' + escape(redirectUri),
	            dataType: "json",
	            async: true,
	            success: function(json) {
	                g_loginResponse = json.loginresponse;
	            },
	            error: function(jqXHR) {
            		notice = parseXMLHttpResponse(jqXHR);
                    cloudStack.dialog.notice({
                        message: notice,
                        clickAction: onLogoutCallback
                    });
	            },
	            beforeSend: function(XMLHttpRequest) {
	                return true;
	            }
	        });
		} else {
			// don't put querystring in url
			var redirectUri = location.origin + location.pathname;
			$.cookie('oauth_redirect', redirectUri);
			$.ajax({
				url: clientApiUrl + "?command=oauthRedirect&response=json&redirect_uri=" + escape(redirectUri),
				dataType: "json",
				async: false,
				success: function(json) {
					// to ensure never will break
					var redirectUri = json && json.oauth2urlresponse &&
						json.oauth2urlresponse.authenticationurl && json.oauth2urlresponse.authenticationurl.redirectUri;
					if (redirectUri) {
						window.location = redirectUri;
					}
	            },
	            beforeSend: function(XMLHttpRequest) {
	                return true;
	            }
			});
		}
	}
}());
