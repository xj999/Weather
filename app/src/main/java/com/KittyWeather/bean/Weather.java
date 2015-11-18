package com.KittyWeather.bean;

import java.util.List;

import com.google.gson.annotations.Expose;

public class Weather {
	@Expose
	private Weatherinfo forecast;
	@Expose
	private RealTime realtime;
	private List<Index> index;





	public RealTime getRealtime() {
		return realtime;
	}


	public void setRealtime(RealTime realtime) {
		this.realtime = realtime;
	}


	public List<Index> getIndex() {
		return index;
	}


	public void setIndex(List<Index> index) {
		this.index = index;
	}


	public Weatherinfo getForecast() {
		return forecast;
	}


	public void setForecast(Weatherinfo forecast) {
		this.forecast = forecast;
	}


	@Override
	public String toString() {
		return "Weather [forecast=" + forecast + "]";
	}

}
