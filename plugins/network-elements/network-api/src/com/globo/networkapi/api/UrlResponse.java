package com.globo.networkapi.api;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseResponse;

import com.cloud.serializer.Param;
import com.google.gson.annotations.SerializedName;

public class UrlResponse extends BaseResponse {

	@SerializedName(ApiConstants.URL) @Param(description = "url")
	private String url;

	public void setUrl(String url) {
		this.url = url;
	}

}
