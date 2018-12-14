package com.coinroster.bots;


import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import com.coinroster.DB;
import com.coinroster.Server;

public class BitcoinBot {
	private BigDecimal referenceRate;
	private Date referenceRateDate;
	private BigDecimal realtimeIndex;
	private Date realtimeIndexDate;
	private Long nextRefUpdate;
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
	
	public Long getNextRefUpdate() {
		return this.nextRefUpdate;
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
	public void setup() {

		String rtiDate = "-";
		try {
			while (true) {
				String stmt = "select id, price, date_updated from bitcoin_reference order by id DESC limit 1";
				
				PreparedStatement get_data = sql_connection.prepareStatement(stmt);
				ResultSet data = get_data.executeQuery();
				
				rtiDate = data.getString(3);
				
				if (rtiDate != "-") {				
					String rti = data.getString(2);
					this.realtimeIndex  = parseRate(rti);
					this.realtimeIndexDate = parseDate(rtiDate);
					break;
				} else {
					Thread.sleep(100);
				}
			}
			Calendar cal = Calendar.getInstance();
			cal.setTime(this.referenceRateDate);
			cal.add(Calendar.DATE, 1);
			
			this.nextRefUpdate = cal.getTime().getTime();
		}catch (Exception e){
			Server.exception(e);
		}finally {
			if (sql_connection != null){
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
			}
		}
		
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
	
	public JSONObject createPariMutuel(Long deadline, String date, JSONObject contest) throws JSONException{
		
		JSONArray option_table = new JSONArray(); 
		
		//Not sure about these table values, but should work for now.
		JSONObject higher = new JSONObject();
		higher.put("description", "Higher");
		higher.put("id", 3);
		option_table.put(higher);
		
		JSONObject same = new JSONObject();
		same.put("description", this.realtimeIndex.toString());
		same.put("id", 2);
		option_table.put(same);
		
		JSONObject lower = new JSONObject();
		lower.put("description", "Lower");
		lower.put("id", 1);
		option_table.put(lower);
	
		contest.put("option_table", option_table);
		
		//Add the current index to the prop data.
		JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
		
		prop_data.put("BTC_index", this.realtimeIndex.toString());
		
		contest.put("prop_data", prop_data);
		
		return contest;
	}
	
	
	public JSONObject settlePariMutuel(int contest_id, JSONObject scoring_rules, JSONObject prop_data, JSONArray option_table) throws Exception{
		
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		String prop_type = prop_data.getString("prop_type");
		int winning_outcome;
		
		switch(prop_type){
		
			case "HIGHER_LOWER":
				BigDecimal start_index = new BigDecimal(prop_data.getString("BTC_index"));
				if (start_index.compareTo(this.realtimeIndex) < 0) { 
					//higher
					winning_outcome = 3;
					fields.put("winning_outcome", winning_outcome);
					return fields;
				}
				else if (start_index.compareTo(this.realtimeIndex) == 0) {
					//same
					winning_outcome = 2;
					fields.put("winning_outcome", winning_outcome);
					return fields;
				} else {
					//lower
					winning_outcome = 1;
					fields.put("winning_outcome", winning_outcome);
					return fields;
				}
				
		}
		return fields;
	}
	
}


