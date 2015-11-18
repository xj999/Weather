package com.KittyWeather.util;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import android.util.Log;

public class HttpUtils {
	private static final String LogTag = "pm25";
	private static HttpClient mHttpClent = new DefaultHttpClient();

	public static String getText(String uri) {
		Log.d(LogTag,
				String.format("request get uri:%s", new Object[] { uri }));
		HttpGet httpGet = new HttpGet(uri);
		try {
			HttpResponse httpResponse = mHttpClent.execute(httpGet);
			Log.d(LogTag, String.format("get StatusCode:%s", httpResponse
					.getStatusLine().getStatusCode()));
			String result = EntityUtils.toString(httpResponse.getEntity(),"utf_8");
			return result;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}
