package com.KittyWeather.weather;

import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.KittyWeather.apapter.CityAdapter;
import com.KittyWeather.apapter.SearchCityAdapter;
import com.KittyWeather.app.Application;
import com.KittyWeather.bean.City;
import com.KittyWeather.db.CityDB;
import com.KittyWeather.plistview.BladeView;
import com.KittyWeather.plistview.PinnedHeaderListView;
import com.KittyWeather.plistview.BladeView.OnItemClickListener;
import com.KittyWeather.util.L;
import com.KittyWeather.util.NetUtil;
import com.KittyWeather.util.T;
import com.way.weather.R;

public class SelectCtiyActivity extends Activity implements TextWatcher, OnClickListener, Application.EventHandler {
	// 编辑框控件
	private EditText mSearchEditText;
	// private Button mCancelSearchBtn;
	// 编辑框上的清除按钮
	private ImageButton mClearSearchBtn;

	private View mCityContainer;
	private View mSearchContainer;
	private PinnedHeaderListView mCityListView;
	private BladeView mLetter;
	private ListView mSearchListView;
	private List<City> mCities;
	private SearchCityAdapter mSearchCityAdapter;
	private CityAdapter mCityAdapter;
	// 首字母集
	private List<String> mSections;
	// 根据首字母存放数据
	private Map<String, List<City>> mMap;
	// 首字母位置集
	private List<Integer> mPositions;
	// 首字母对应的位置
	private Map<String, Integer> mIndexer;
	private CityDB mCityDB;
	private Application mApplication;
	private InputMethodManager mInputMethodManager;

	private TextView mTitleTextView;
	private ProgressBar mTitleProgressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.biz_plugin_weather_select_city);
		Application.mListeners.add(this);
		initView();
		initData();
	}

	private void initView() {
		mTitleTextView = (TextView) findViewById(R.id.title_name);
		mTitleProgressBar = (ProgressBar) findViewById(R.id.title_update_progress);
		mTitleProgressBar.setVisibility(View.VISIBLE);
		// 获取城市的名字并且显示在标题上
		mTitleTextView.setText(Application.getInstance().getSharePreferenceUtil().getCity());

		mSearchEditText = (EditText) findViewById(R.id.search_edit);
		// 增加编辑框的修改事件
		mSearchEditText.addTextChangedListener(this);
		// 清除按钮
		mClearSearchBtn = (ImageButton) findViewById(R.id.ib_clear_text);
		mClearSearchBtn.setOnClickListener(this);

		mCityContainer = findViewById(R.id.city_content_container);
		mSearchContainer = findViewById(R.id.search_content_container);
		mCityListView = (PinnedHeaderListView) findViewById(R.id.citys_list);
		mCityListView.setEmptyView(findViewById(R.id.citys_list_empty));
		mLetter = (BladeView) findViewById(R.id.citys_bladeview);

		// 设置BladeView的点击事件
		mLetter.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(String s) {
				if (mIndexer.get(s) != null) {
					mCityListView.setSelection(mIndexer.get(s));
				}
			}
		});
		mLetter.setVisibility(View.GONE);
		mSearchListView = (ListView) findViewById(R.id.search_list);
		mSearchListView.setEmptyView(findViewById(R.id.search_empty));
		mSearchContainer.setVisibility(View.GONE);
		mSearchListView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				// TODO Auto-generated method stub
				mInputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
				return false;
			}
		});
		// 点击某个城市，返回相应城市的主界面。
		mCityListView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				L.i(mCityAdapter.getItem(position).toString());
				startActivity(mCityAdapter.getItem(position));
			}
		});

		mSearchListView.setOnItemClickListener(new android.widget.AdapterView.OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				// TODO Auto-generated method stub
				L.i(mSearchCityAdapter.getItem(position).toString());
				startActivity(mSearchCityAdapter.getItem(position));
			}
		});
	}

	private void startActivity(City city) {
		Intent i = new Intent();
		i.putExtra("city", city);
		setResult(RESULT_OK, i);
		finish();
	}

	private void initData() {
		mApplication = Application.getInstance();
		mCityDB = mApplication.getCityDB();
		mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		if (mApplication.isCityListComplite()) {
			mCities = mApplication.getCityList();
			mSections = mApplication.getSections();
			mMap = mApplication.getMap();
			mPositions = mApplication.getPositions();
			mIndexer = mApplication.getIndexer();

			mCityAdapter = new CityAdapter(SelectCtiyActivity.this, mCities, mMap, mSections, mPositions);
			mCityListView.setAdapter(mCityAdapter);
			mCityListView.setOnScrollListener(mCityAdapter);
			mCityListView.setPinnedHeaderView(LayoutInflater.from(SelectCtiyActivity.this).inflate(R.layout.biz_plugin_weather_list_group_item, mCityListView, false));
			mTitleProgressBar.setVisibility(View.GONE);
			mLetter.setVisibility(View.VISIBLE);

		}
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		// do nothing
	}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
		mSearchCityAdapter = new SearchCityAdapter(SelectCtiyActivity.this, mCities);
		mSearchListView.setAdapter(mSearchCityAdapter);
		mSearchListView.setTextFilterEnabled(true);
		if (mCities.size() < 1 || TextUtils.isEmpty(s)) {
			mCityContainer.setVisibility(View.VISIBLE);
			mSearchContainer.setVisibility(View.INVISIBLE);
			mClearSearchBtn.setVisibility(View.GONE);
		} else {
			mClearSearchBtn.setVisibility(View.VISIBLE);
			mCityContainer.setVisibility(View.INVISIBLE);
			mSearchContainer.setVisibility(View.VISIBLE);
			mSearchCityAdapter.getFilter().filter(s);
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		// 如何搜索字符串长度为0，是否隐藏输入法
		// if(TextUtils.isEmpty(s)){
		// mInputMethodManager.hideSoftInputFromWindow(
		// mSearchEditText.getWindowToken(), 0);
		// }

	}

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.ib_clear_text:
			if (!TextUtils.isEmpty(mSearchEditText.getText().toString())) {
				mSearchEditText.setText("");
				mInputMethodManager.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
			}
			break;
		default:
			break;
		}
	}

	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		Application.mListeners.remove(this);
	}

	@Override
	public void onCityComplite() {
		// 城市列表加载完的回调函数
		mCities = mApplication.getCityList();
		mSections = mApplication.getSections();
		mMap = mApplication.getMap();
		mPositions = mApplication.getPositions();
		mIndexer = mApplication.getIndexer();

		mCityAdapter = new CityAdapter(SelectCtiyActivity.this, mCities, mMap, mSections, mPositions);
		mLetter.setVisibility(View.VISIBLE);
		mCityListView.setAdapter(mCityAdapter);
		mCityListView.setOnScrollListener(mCityAdapter);
		mCityListView.setPinnedHeaderView(LayoutInflater.from(SelectCtiyActivity.this).inflate(R.layout.biz_plugin_weather_list_group_item, mCityListView, false));
		// mActionBar.setProgressBarVisibility(View.INVISIBLE);
		mTitleProgressBar.setVisibility(View.GONE);
	}

	@Override
	public void onNetChange() {
		if (!NetUtil.isNetConnected(this))
			T.showLong(this, R.string.net_err);
	}
}
