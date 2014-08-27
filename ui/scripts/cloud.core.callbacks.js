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
$.urlParam = function(name) {
    var results = new RegExp('[\\?&]' + name + '=([^&#]*)').exec(window.location.href);
    if (!results) {
        return 0;
    }
    return results[1] || 0;
}

/*
This file is meant to help with implementing single signon integration.  If you are using the
cloud.com default UI, there is no need to touch this file.
*/

/*
This callback function is called when either the session has timed out for the user,
the session ID has been changed (i.e. another user logging into the UI via a different tab),
or it's the first time the user has come to this page.
*/

function onLogoutCallback() {
    g_loginResponse = null; //clear single signon variable g_loginResponse


    return true; // return true means the login page will show
    /*
	window.location.replace("http://www.google.com"); //redirect to a different location
  return false;	//return false means it will stay in the location window.location.replace() sets it to (i.e. "http://www.google.com")
	*/
}

var g_loginResponse = null;

/*
For single signon purposes, you just need to make sure that after a successful login, you set the
global variable "g_loginResponse"

You can also pass in a special param called loginUrl that is pregenerated and sent to the CloudStack, it will
automatically log you in.

Below is a sample login attempt
*/

var clientApiUrl = "/client/api";
var clientConsoleUrl = "/client/console";


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

//FIXME: Colocar num arquivo js a parte
if ($.urlParam("direct") !== "true") {
	var code = $.urlParam("code");
	if (code) {
		// remove parameter code from url
		history.replaceState(null, null, removeParam("code", window.location.href));

		$.ajax({
            url: clientApiUrl + '?command=oAuth2Login&response=json&code=' + $.urlParam("code"),
            dataType: "json",
            async: false,
            success: function(json) {
                g_loginResponse = json.loginresponse;
            },
            error: function() {
                onLogoutCallback();
                // This means the login failed.  You should redirect to your login page.
            }
        });
	} else {
		// don't put querystring in url
		var redirectUri = location.origin + location.pathname;
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
			}
		});
	}
}

$(document).ready(function() {

    var url = $.urlParam("loginUrl");
    if (url != undefined && url != null && url.length > 0) {
        url = unescape(clientApiUrl + "?" + url);
        $.ajax({
            url: url,
            dataType: "json",
            async: false,
            success: function(json) {
                g_loginResponse = json.loginresponse;
            },
            error: function() {
                onLogoutCallback();
                // This means the login failed.  You should redirect to your login page.
            },
            beforeSend: function(XMLHttpRequest) {
                return true;
            }
        });
    }
});
