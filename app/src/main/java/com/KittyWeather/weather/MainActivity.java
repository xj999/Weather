package com.KittyWeather.weather;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import cn.sharesdk.framework.ShareSDK;
import cn.sharesdk.onekeyshare.OnekeyShare;

import com.KittyWeather.apapter.WeatherPagerAdapter;
import com.KittyWeather.app.Application;
import com.KittyWeather.bean.City;
import com.KittyWeather.bean.Index;
import com.KittyWeather.bean.RealTime;
import com.KittyWeather.bean.Weather;
import com.KittyWeather.bean.Weatherinfo;
import com.KittyWeather.db.CityDB;
import com.KittyWeather.fragment.FirstWeatherFragment;
import com.KittyWeather.fragment.SecondWeatherFragment;
import com.KittyWeather.indicator.CirclePageIndicator;
import com.KittyWeather.util.HttpUtils;
import com.KittyWeather.util.IphoneDialog;
import com.KittyWeather.util.NetUtil;
import com.KittyWeather.util.SharePreferenceUtil;
import com.KittyWeather.util.T;
import com.KittyWeather.util.TimeUtil;
import com.KittyWeather.weather.HttpSendData.CallBackListener;
import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.way.weather.R;

public class MainActivity extends FragmentActivity implements Application.EventHandler, OnClickListener, CallBackListener {
	public static final String UPDATE_WIDGET_WEATHER_ACTION = "com.way.action.update_weather";
	public static final String WEATHER_SIMPLE_URL = "http://www.weather.com.cn/data/sk/";// 简要天气信息
	public static final String WEATHER_BASE_URL = "http://weatherapi.market.xiaomi.com/wtr-v2/weather?cityId=";// 详细天气
	private static final String WEATHER_INFO_FILENAME = "_weather.json";
	private static final String SIMPLE_WEATHER_INFO_FILENAME = "_simple_weather.json";

	private static final int LOACTION_OK = 0;
	private static final int ON_NEW_INTENT = 1;
	private static final int UPDATE_EXISTS_CITY = 2;
	private static final int GET_WEATHER_RESULT = 3;
	private LocationClient mLocationClient;
	private CityDB mCityDB;
	private SharePreferenceUtil mSpUtil;
	private Application mApplication;
	private City mCurCity;
	private Weatherinfo mCurWeatherinfo;
	private Weather weather;
	RealTime realTime;
	private Gson mGson;
	// 主界面的4个按钮变量
	private ImageView mCityManagerBtn, mUpdateBtn, mLocationBtn, mShareBtn;
	// 进入条
	private ProgressBar mUpdateProgressBar;
	private TextView mTitleTextView;
	private City mNewIntentCity;
	private WeatherPagerAdapter mWeatherPagerAdapter;

	private TextView cityTv, timeTv, humidityTv, weekTv, index, temperatureTv, climateTv, windTv;
	private ImageView weatherImg;
	private ViewPager mViewPager;
	private List<Fragment> fragments;

	// 获取切换城市时的天气信息，LOACTION_OK城市已经获取，获取该城市天气信息，
	// ON_NEW_INTENT获取新城市天气
	// UPDATE_EXISTS_CITY退出城市，不获取天气
	// GET_WEATHER_RESULT获取到结果，进度条状态设置看不见
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case LOACTION_OK:

				String cityName = (String) msg.obj;
				mCurCity = mCityDB.getCity(cityName);
				mSpUtil.setCity(mCurCity.getCity());
				cityTv.setText(mCurCity.getCity());
				updateWeather(true);
				break;
			case ON_NEW_INTENT:
				mCurCity = mNewIntentCity;
				mSpUtil.setCity(mCurCity.getCity());
				cityTv.setText(mCurCity.getCity());
				updateWeather(true);
				break;
			case UPDATE_EXISTS_CITY:
				String sPCityName = mSpUtil.getCity();
				mCurCity = mCityDB.getCity(sPCityName);
				updateWeather(false);
				break;
			case GET_WEATHER_RESULT:
				mSpUtil.setTimeSamp(System.currentTimeMillis());// 保存一下更新的时间戳
				updateWeatherInfo();
				updateWidgetWeather();
				mUpdateBtn.setVisibility(View.VISIBLE);
				mUpdateProgressBar.setVisibility(View.GONE);
				break;
			default:
				break;
			}
		}

	};

	// 获取天气信息
	private void updateWidgetWeather() {
		sendBroadcast(new Intent(UPDATE_WIDGET_WEATHER_ACTION));
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ShareSDK.initSDK(this);
		// 初始化数据
		initData();
		// 初始化界面
		initView();

	}

	@Override
	// Activity进程结束
	protected void onDestroy() {
		super.onDestroy();
		ShareSDK.stopSDK(this);
	}

	private void startActivityForResult() {
		Intent i = new Intent(this, SelectCtiyActivity.class);
		startActivityForResult(i, 0);
	}

	// 初始化主页面
	private void initView() {
		mCityManagerBtn = (ImageView) findViewById(R.id.title_city_manager);
		mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
		mShareBtn = (ImageView) findViewById(R.id.title_share);
		mLocationBtn = (ImageView) findViewById(R.id.title_location);
		mCityManagerBtn.setOnClickListener(this);
		mUpdateBtn.setOnClickListener(this);
		mShareBtn.setOnClickListener(this);
		mLocationBtn.setOnClickListener(this);
		mUpdateProgressBar = (ProgressBar) findViewById(R.id.title_update_progress);
		mTitleTextView = (TextView) findViewById(R.id.title_city_name);

		cityTv = (TextView) findViewById(R.id.city);
		timeTv = (TextView) findViewById(R.id.time);
		timeTv.setText(TimeUtil.getDayString(mSpUtil.getTimeSamp()) + mSpUtil.getTime() + "发布");
		humidityTv = (TextView) findViewById(R.id.humidity);
		weekTv = (TextView) findViewById(R.id.week_today);
		weekTv.setText("今天 " + TimeUtil.getWeek(0, TimeUtil.XING_QI));
		temperatureTv = (TextView) findViewById(R.id.temperature);
		climateTv = (TextView) findViewById(R.id.climate);
		windTv = (TextView) findViewById(R.id.wind);
		index = (TextView) findViewById(R.id.index);
		weatherImg = (ImageView) findViewById(R.id.weather_img);
		fragments = new ArrayList<Fragment>();
		fragments.add(new FirstWeatherFragment());
		fragments.add(new SecondWeatherFragment());
		mViewPager = (ViewPager) findViewById(R.id.viewpager);
		mWeatherPagerAdapter = new WeatherPagerAdapter(getSupportFragmentManager(), fragments);
		mViewPager.setAdapter(mWeatherPagerAdapter);
		((CirclePageIndicator) findViewById(R.id.indicator)).setViewPager(mViewPager);
		if (TextUtils.isEmpty(mSpUtil.getCity())) {
			if (NetUtil.isNetConnected(this)) {
				mLocationClient.start();
				mLocationClient.requestLocation();
				T.showShort(this, "正在定位...");
				mUpdateBtn.setVisibility(View.GONE);
				mUpdateProgressBar.setVisibility(View.VISIBLE);
			} else {
				T.showShort(this, R.string.net_err);
			}
		} else {
			mHandler.sendEmptyMessage(UPDATE_EXISTS_CITY);
		}
	}

	// 初始化数据
	private void initData() {
		Application.mListeners.add(this);
		// 设置获取的当前实例
		mApplication = Application.getInstance();
		// 设置获取的SharePreferenceUtil类里面相应参数
		mSpUtil = mApplication.getSharePreferenceUtil();
		// 设置获的取地理位置
		mLocationClient = mApplication.getLocationClient();

		mLocationClient.registerLocationListener(mLocationListener);

		mCityDB = mApplication.getCityDB();

		mGson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
	}

	// 更新天气信息
	private void updateWeather(final boolean isRefresh) {
		if (!NetUtil.isNetConnected(this) && isRefresh) {
			T.showLong(this, R.string.net_err);
			return;
		}
		if (mCurCity == null) {
			T.showLong(mApplication, "未找到此城市,请重新定位或选择...");
			return;
		}
		// T.showShort(this, "正在刷新天气...");
		timeTv.setText("同步中...");
		mTitleTextView.setText(mCurCity.getCity() + "天气");
		mUpdateBtn.setVisibility(View.GONE);
		mUpdateProgressBar.setVisibility(View.VISIBLE);
		// 启动线程获取天气信息
		new Thread() {
			@Override
			public void run() {
				super.run();
				getWeatherInfo(isRefresh);
//				getSimpleWeatherInfo(isRefresh);

				if (mCurWeatherinfo != null)
				mHandler.sendEmptyMessage(GET_WEATHER_RESULT);
			}

		}.start();
	}

	private void getWeatherInfo(boolean isRefresh) {
		String result;
		if (!isRefresh) {
			if (mApplication.getmCurWeatherinfo() != null) {// 读取内存中的信息
				mCurWeatherinfo = mApplication.getmCurWeatherinfo();
				return;// 直接返回，不继续执行
			}
			result = getInfoFromFile(WEATHER_INFO_FILENAME);// 文件中的信息
			if (!TextUtils.isEmpty(result)) {
				parseWeatherInfo(result, false);
				return;
			}
		}

		String url = WEATHER_BASE_URL + mCurCity.getNumber();
		String weatherResult = HttpUtils.getText(url);
		if (TextUtils.isEmpty(weatherResult))
			weatherResult = getInfoFromFile(WEATHER_INFO_FILENAME);
		parseWeatherInfo(weatherResult, true);
	}


	private void parseWeatherInfo(String result, boolean isRefreshWeather) {
		mCurWeatherinfo = null;
		mApplication.setmCurWeatherinfo(null);
		if (!TextUtils.isEmpty(result) && !result.contains("页面没有找到")) {
			// L.i(result);
			Weather weathers = mGson.fromJson(result, Weather.class);
			mCurWeatherinfo = weathers.getForecast();
		    realTime=weathers.getRealtime();
			JSONObject s;
			JSONArray d;
			weather=weathers;
			try {
				s=new JSONObject(result);
				d = getJSONArray(s,"index");
				List<Index> indexs = new ArrayList<Index>();
				if(d != null && d.length() > 0)
				{
					
					for(int i = 0;i  < d.length(); i++)
					{
						if(!d.isNull(i))
						{
							try {
								JSONObject tjson = d.getJSONObject(i);
								Index indexitem = parseIndex(tjson);
								indexs.add(indexitem); 
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					}
				}
				weather.setIndex(indexs);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			result = "";
		}
		if (isRefreshWeather && !TextUtils.isEmpty(result))
			save2File(result, WEATHER_INFO_FILENAME);
	}
	
	
	
	@Override
	public void onBackPressed() {
		System.exit(0);
		super.onBackPressed();
	}

	public static Index parseIndex(JSONObject aJson) {
		Index t = new Index();
		String code = getJSONString(aJson, "code");
		String details = getJSONString(aJson, "details");
		String name = getJSONString(aJson, "name");
		String index = getJSONString(aJson, "index");
		t.setCode(code);
		t.setDetails(details);
		t.setIndex(index);
		t.setName(name);
		
		return t;
	}
	public final static String getJSONString(JSONObject aObject, String name) {
		String ret = null;
		if (aObject != null) {
			try {
				Object obj = aObject.isNull(name) ? null : aObject.get(name);
				if (obj != null && (obj instanceof String || obj instanceof Integer)) {
					ret = String.valueOf(obj);
				}
			} catch (JSONException e) {
			}
		}
		return ret;
	}
	public final static JSONArray getJSONArray(JSONObject aObject, String name) {
		JSONArray ret = null;
		if (aObject != null) {
			try {
				Object obj = aObject.isNull(name) ? null : aObject.get(name);
				if (obj != null && obj instanceof JSONArray) {
					ret = (JSONArray) obj;
				}
			} catch (JSONException e) {
			}
		}
		return ret;
	}

	// 把信息保存到文件中
	private boolean save2File(String result, String fileName) {
		try {
			FileOutputStream fos = MainActivity.this.openFileOutput(fileName, MODE_PRIVATE);
			fos.write(result.toString().getBytes());
			fos.close();
			return true;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	// 从文件中获取信息
	private String getInfoFromFile(String fileName) {
		String result = "";
		try {
			FileInputStream fis = openFileInput(fileName);
			byte[] buffer = new byte[fis.available()];// 本地文件可以实例化buffer，网络文件不可行
			fis.read(buffer);
			result = new String(buffer);
			fis.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}

	// 更新天气界面
	private void updateWeatherInfo() {
		if (mCurWeatherinfo != null) {
			mApplication.setmCurWeatherinfo(mCurWeatherinfo);// 保存到全局变量中
			temperatureTv.setText(mCurWeatherinfo.getTemp1());
			cityTv.setText(mCurWeatherinfo.getCity());

			String wind = mCurWeatherinfo.getWind1();
			if (wind.contains("转")) {
				String[] strs = wind.split("转");
				wind = strs[0];
			}
			windTv.setText(wind);
			index.setText("Tips:\n" + weather.getIndex().get(0).getDetails());
			String climate = mCurWeatherinfo.getWeather1();
			climateTv.setText(climate);
			mSpUtil.setSimpleClimate(climate);
			String[] strs = { "晴", "晴" };
			if (climate.contains("转")) {// 天气带转字，取前面那部分
				strs = climate.split("转");
				climate = strs[0];
				if (climate.contains("到")) {// 如果转字前面那部分带到字，则取它的后部分
					strs = climate.split("到");
					climate = strs[1];
				}
			}
			if (mApplication.getWeatherIconMap().containsKey(climate)) {
				int iconRes = mApplication.getWeatherIconMap().get(climate);
				weatherImg.setImageResource(iconRes);
			} else {
				// do nothing 没有这样的天气图片

			}
				mSpUtil.setSimpleTemp(realTime.getTemp());
				mSpUtil.setTime(realTime.getTime());
				timeTv.setText(TimeUtil.getDayString(mSpUtil.getTimeSamp()) + realTime.getTime() + "发布");
				humidityTv.setText("湿度:" + realTime.getSD());
			if (fragments.size() > 0) {
				((FirstWeatherFragment) mWeatherPagerAdapter.getItem(0)).updateWeather(mCurWeatherinfo);
				((SecondWeatherFragment) mWeatherPagerAdapter.getItem(1)).updateWeather(mCurWeatherinfo);
			}
		} else {
			temperatureTv.setText("N/A");
			cityTv.setText(mCurCity.getCity());
			windTv.setText("N/A");
			climateTv.setText("N/A");
			weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
			T.showLong(mApplication, "获取天气信息失败");
		}
	}

	// 请求服务器，获取返回数据
	private String connServerForResult(String url) {
		HttpGet httpRequest = new HttpGet(url);
		String strResult = "";
		if (NetUtil.isNetConnected(MainActivity.this)) {
			try {
				// HttpClient对象
				HttpClient httpClient = new DefaultHttpClient();
				// 获得HttpResponse对象
				HttpResponse httpResponse = httpClient.execute(httpRequest);
				if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK)
					// 取得返回的数据
					strResult = EntityUtils.toString(httpResponse.getEntity());

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return strResult; // 返回结果
	}

	BDLocationListener mLocationListener = new BDLocationListener() {

		@Override
		public void onReceivePoi(BDLocation arg0) {
			// do nothing
		}

		@Override
		public void onReceiveLocation(BDLocation location) {
			// mActionBar.setProgressBarVisibility(View.GONE);
			mUpdateBtn.setVisibility(View.VISIBLE);
			mUpdateProgressBar.setVisibility(View.GONE);
			if (location == null || TextUtils.isEmpty(location.getCity())) {
				// T.showShort(getApplicationContext(), "location = null");
				final Dialog dialog = IphoneDialog.getTwoBtnDialog(MainActivity.this, "定位失败", "是否手动选择城市?");
				((Button) dialog.findViewById(R.id.ok)).setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View v) {
						startActivityForResult();
						dialog.dismiss();
					}
				});
				dialog.show();
				return;
			}
			String cityName = location.getCity();
			mLocationClient.stop();
			Message msg = mHandler.obtainMessage();
			msg.what = LOACTION_OK;
			msg.obj = cityName;
			mHandler.sendMessage(msg);// 更新天气
		}
	};

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 0 && resultCode == RESULT_OK) {
			mNewIntentCity = (City) data.getSerializableExtra("city");
			mHandler.sendEmptyMessage(ON_NEW_INTENT);
		}
	}

	@Override
	public void onCityComplite() {
		// do nothing
	}

	@Override
	public void onNetChange() {
		if (!NetUtil.isNetConnected(this))
			T.showLong(this, R.string.net_err);
		// else if (!TextUtils.isEmpty(mSpUtil.getCity())) {
		// String sPCityName = mSpUtil.getCity();
		// mCurCity = mCityDB.getCity(sPCityName);
		// getWeatherInfo(true, true);
		// }
	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.title_city_manager:
			startActivityForResult();
			break;
		case R.id.title_location:
			if (NetUtil.isNetConnected(this)) {
				if (!mLocationClient.isStarted())
					mLocationClient.start();
				mLocationClient.requestLocation();
				T.showShort(this, "正在定位...");
			} else {
				T.showShort(this, R.string.net_err);
			}
			break;
		case R.id.title_share:
			if (NetUtil.isNetConnected(this)) {
				String shareText = mSpUtil.getCity() + temperatureTv.getText() + " " + climateTv.getText() + "\n" + index.getText() + ""; // 要分享的内容
				showOnekeyshare(null, true, shareText);
			} else {
				T.showShort(this, R.string.net_err);
			}
			break;
		case R.id.title_update_btn:
			if (NetUtil.isNetConnected(this)) {
				if (TextUtils.isEmpty(mSpUtil.getCity())) {
					T.showShort(this, "请先选择城市或定位！");
				} else {
					String sPCityName = mSpUtil.getCity();
					mCurCity = mCityDB.getCity(sPCityName);
					updateWeather(true);
				}
			} else {
				T.showShort(this, R.string.net_err);
			}
			break;

		default:
			break;
		}
	}

	public void showOnekeyshare(String platform, boolean silent, String shareText) {
		OnekeyShare oks = new OnekeyShare();

		// 分享时Notification的图标和文字
		oks.setNotification(R.drawable.ic_launcher, getString(R.string.app_name));

		// title标题，印象笔记、邮箱、信息、微信、人人网和QQ空间使用
		oks.setTitle(getString(R.string.app_name));
		// text是分享文本，所有平台都需要这个字段
		oks.setText(shareText);
		// site是分享此内容的网站名称，仅在QQ空间使用
		oks.setSite(getString(R.string.app_name));
		// 是否直接分享（true则直接分享）
		oks.setSilent(silent);
		// 指定分享平台，和slient一起使用可以直接分享到指定的平台
		if (platform != null) {
			oks.setPlatform(platform);
		}
		// 去除注释可通过OneKeyShareCallback来捕获快捷分享的处理结果
		// oks.setCallback(new OneKeyShareCallback());

		oks.show(this);
	}

	@Override
	public void callBack(String result) {
		// TODO Auto-generated method stub

	}

}
