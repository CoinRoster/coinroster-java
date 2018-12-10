package com.coinroster.bots;


import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;

//import com.coinroster.DB;
import com.coinroster.Server;

public class BitcoinBot {
	private BigDecimal referenceRate;
	private Date referenceRateDate;
	private BigDecimal realtimeIndex;
	private Date realtimeIndexDate;
	private Date nextUpdate;
	private Connection sql_connection;
	//private DB db;
	
	public BitcoinBot(Connection sql_connection) throws IOException, JSONException{
		this.sql_connection = sql_connection;
		//this.db = new DB(sql_connection);
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
		data.accumulate("brr", this.getReferenceRate().toString());
		data.accumulate("brrDate", this.getReferenceRateDate().toString());
		data.accumulate("rti", this.getRealtimeIndex().toString());
		data.accumulate("rtiDate", this.getRealtimeIndexDate().toString());
		
		return data;
	}
	
	//Scrape data from cmegroup website. Determine when the next update should be.
	public void scrape() throws IOException, JSONException, InterruptedException {

		String rtiDate = "-";
		 
		while (true) {
			String jsonString = Jsoup.connect("https://www.cmegroup.com/CmeWS/mvc/Bitcoin/All").ignoreContentType(true).execute().body();
			
			JSONObject json = new JSONObject(jsonString);
			
			rtiDate = json.getJSONObject("realTimeIndex").getString("date");
			
			if (rtiDate != "-") {
				String brr = String.valueOf(json.getJSONObject("referenceRate").getLong("value"));
				this.referenceRate = parseRate(brr);
				
				String brrDate = json.getJSONObject("referenceRate").getString("date");
				this.referenceRateDate = parseDate(brrDate);
				
				String rti = String.valueOf(json.getJSONObject("realTimeIndex").getLong("value"));
				this.realtimeIndex  = parseRate(rti);
				
				this.realtimeIndexDate = parseDate(rtiDate);
				break;
			} else {
				Thread.sleep(1000);
			}
		}
		Calendar cal = Calendar.getInstance();
		cal.setTime(this.referenceRateDate);
		cal.add(Calendar.DATE, 1);
		
		this.nextUpdate = cal.getTime();
	}
	
	public Date getNextUpdate() {
		return this.nextUpdate;
	}
	
	private Date parseDate(String value) {
		
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		Date date = null;
		
		try {
			date = formatter.parse(value);
			
		} catch (ParseException e) {
			Server.exception(e);
		}
		
		return date;
	}
	
	private BigDecimal parseRate(String value) {
		BigDecimal money = new BigDecimal(value);
		return money;
	}
	
	public void updateDB() throws SQLException {
		Timestamp date = new Timestamp(this.referenceRateDate.getTime());
		PreparedStatement stmnt = sql_connection.prepareStatement("insert ignore into bitcoin_reference(price,  date_updated) VALUES (?, ?)");
		stmnt.setDouble(1, this.getReferenceRate().doubleValue());
		stmnt.setTimestamp(2, date);
		stmnt.executeUpdate();
	}
	
}


