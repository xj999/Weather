package com.KittyWeather.weather;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

public class HttpSendData {

	public static final String URL = "";

	public static void getValueData(final Map<String, String> map,
			final String url, final CallBackListener listener) {

		new Thread() {
			public void run() {
				String result = "";
				try {
					HttpParams httpParams = new BasicHttpParams();
					HttpConnectionParams.setConnectionTimeout(httpParams, 50000);
					HttpConnectionParams.setSoTimeout(httpParams, 50000);
					HttpClient httpClient = new DefaultHttpClient(httpParams);
					HttpPost httpPost = new HttpPost(url);
					List<BasicNameValuePair> list = new ArrayList<BasicNameValuePair>();
					if (map!=null) {
						
					Iterator<String> it = map.keySet().iterator();
					while (it.hasNext()) {
						String next = it.next();
						list.add(new BasicNameValuePair(next, map.get(next)));
					}

					httpPost.setEntity(new UrlEncodedFormEntity(list, "UTF-8"));
					}
					HttpResponse response = httpClient.execute(httpPost);
//					if (response.getStatusLine().getStatusCode() == 200) {
//						HttpEntity entity = response.getEntity();
//						// result = EntityUtils.toString(entity);
//						InputStream is = entity.getContent();
//						byte[] data = StreamTool.read(is);
//						result = new String(data);
//
//						if (is != null)
//							is.close();
//						data = null;
//						// BufferedReader reader = new BufferedReader(new
//						// InputStreamReader(is, "UTF-8"));
//						// result = reader.readLine();
//					} else {
//						result = "��ݻ�ȡʧ��";
//					}
					result = EntityUtils.toString(response.getEntity(),"utf_8");
				} catch (Exception e) {
					e.printStackTrace();
					result = "��ݻ�ȡʧ��";
				} finally {
					listener.callBack(result);
				}
			};
		}.start();

	}

	public interface CallBackListener {
		public void callBack(String result);
	}

}
