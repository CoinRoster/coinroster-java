package com.coinroster.bots;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import com.coinroster.DB;

public class BitcoinBot {
	private BigDecimal referenceRate;
	private Date referenceRateDate;
	private BigDecimal realtimeIndex;
	private Date realtimeIndexDate;
	private Long nextUpdate;
	private Connection sql_connection;
	private DB db;
	public String sport = "BITCOIN";
	
	public BitcoinBot(Connection sql_connection) throws IOException, JSONException{
		this.sql_connection = sql_connection;
		db = new DB(sql_connection);
	}
	
	public BigDecimal getReferenceRate() {
		return this.referenceRate;
	}
	public Date getRealtimeIndexDate() {
		return realtimeIndexDate;
	}

	public BigDecimal getRealtimeIndex() {
		return realtimeIndex;
	}

	public Date getReferenceRateDate() {
		return referenceRateDate;
	}
	
	public JSONObject getDataJSON() throws JSONException {
		
		JSONObject data = new JSONObject();
		data.accumulate("brr", this.getReferenceRate());
		data.accumulate("brrDate", this.getReferenceRateDate());
		data.accumulate("rti", this.getRealtimeIndex());
		data.accumulate("rtiDate", this.getRealtimeIndexDate());
		
		return data;
	}
	
	public void setup() throws IOException {
		Document page = Jsoup.connect("https://www.cmegroup.com/trading/cryptocurrency-indices/cf-bitcoin-reference-rate.html").get();
		Elements brr = page.select("#brrValue1");
		String brrString = brr.val();
		this.referenceRate = parseRate(brrString);
		
		Elements brrDate = page.select("#brrDate1");
		String brrDateString = brrDate.val();
		this.referenceRateDate = parseDate(brrDateString);
		
		Elements rti = page.select("#brtiValue");
		String rtiString = rti.val();
		this.realtimeIndex = parseRate(rtiString);
		
		Elements rtiDate = page.select("#brtiDate");
		String rtiDateString = rtiDate.val();
		this.realtimeIndexDate = parseDate(rtiDateString);
	}
	
	public Long getNextUpdate() {
		return this.nextUpdate;
	}
	
	private Date parseDate(String value) {
		return null;
	}
	
	private BigDecimal parseRate(String value) {
		BigDecimal money = new BigDecimal(value.replaceAll(",", ""));
		return money;
	}
	
}


