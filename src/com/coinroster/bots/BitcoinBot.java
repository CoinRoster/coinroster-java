package com.coinroster.bots;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.Utils;
import com.coinroster.DB;
import com.coinroster.Server;


public class BitcoinBot extends Utils {
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
	public void setup(){

		String rtiDate = "-";
		try {
			while (true) {
				String stmt = "select id, price, date_updated from bitcoin_reference order by id DESC limit 1";
				
				PreparedStatement get_data = sql_connection.prepareStatement(stmt);
				ResultSet data = get_data.executeQuery();
				
				data.next();
				
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
			cal.setTime(this.realtimeIndexDate);
			cal.add(Calendar.DATE, 1);
			
			this.nextRefUpdate = cal.getTime().getTime();
		}catch (Exception e){
			Server.exception(e);
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
	
	public ArrayList<JSONObject> createPariMutuels() throws SQLException, JSONException{
		
		ArrayList<JSONObject> contest_list = new ArrayList<JSONObject>();
		
		DB db = new DB(sql_connection);
		
		Date c_date = new Date(System.currentTimeMillis()); //time of price index.
		Calendar cal = Calendar.getInstance();

		cal.setTime(c_date);
		cal.add(Calendar.MINUTE, 74); 
		cal.add(Calendar.SECOND, 30); //Registration time.
		Date d_date = cal.getTime();
		Long deadline = d_date.getTime();
		
		cal.add(Calendar.SECOND, 30);
		cal.add(Calendar.HOUR_OF_DAY, 24); //From registration deadline to settlement.
		d_date = cal.getTime();	
		Long settlement = d_date.getTime();
		
		JSONArray prop_contests = db.getRosterTemplates("BITCOINS");
		
		for(int i = 0; i < prop_contests.length(); i++){
			JSONObject contest = prop_contests.getJSONObject(i);
			String title = c_date.toString() + " | " + contest.getString("title");
			contest.put("title", title);
			contest.put("odds_source", "n/a");
			contest.put("registration_deadline", deadline);
			
			
			JSONObject prop_data = new JSONObject(contest.getString("prop_data"));
			prop_data.put("BTC_index", this.realtimeIndex.toString());
			prop_data.put("settlement_deadline", settlement);
			
			 
			String prop_type = prop_data.getString("prop_type");
			JSONArray option_table = null;
			
			if (prop_type == "HIGHER_LOWER") {
				option_table = buildHighLowTable(contest);
				contest.put("option_table", option_table);
			}else if (prop_type == "HIGHER_LOWER_FIXED") {
				option_table = buildHighLowFixedTable(contest);
				contest.put("option_table", option_table);
				prop_data.put("risk", 0.0001);
			}
			
			contest.put("prop_data", prop_data.toString());
			contest_list.add(contest);

		}
		return contest_list;
	}
	
	private JSONArray buildHighLowFixedTable(JSONObject contest)  throws JSONException{
		
		JSONArray option_table = new JSONArray(); 
		JSONObject lower = new JSONObject();
		lower.put("odds","1.9");
		lower.put("description", "Lower");
		lower.put("id", 1);
		option_table.put(lower);

		
		JSONObject same = new JSONObject();
		same.put("odds","10");
		same.put("description", this.realtimeIndex.toString());
		same.put("id", 2);
		option_table.put(same);
		
		//Not sure about these table values, but should work for now.
		JSONObject higher = new JSONObject();
		higher.put("odds","1.9");
		higher.put("description", "Higher");
		higher.put("id", 3);
		option_table.put(higher);

		
		
		return option_table;
	}
	
	private JSONArray buildHighLowTable(JSONObject contest)  throws JSONException{
		
		JSONArray option_table = new JSONArray(); 
		JSONObject lower = new JSONObject();
		lower.put("description", "Lower");
		lower.put("id", 1);
		option_table.put(lower);

		
		JSONObject same = new JSONObject();
		same.put("description", this.realtimeIndex.toString());
		same.put("id", 2);
		option_table.put(same);
		
		//Not sure about these table values, but should work for now.
		JSONObject higher = new JSONObject();
		higher.put("description", "Higher");
		higher.put("id", 3);
		option_table.put(higher);
		
		return option_table;
	}
	
	public JSONObject chooseWinnerHigherLower(int contest_id, JSONObject prop_data, JSONArray option_table) throws JSONException {
		
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		//String prop_type = prop_data.getString("prop_type");
		int winning_outcome;
		

		BigDecimal start_index = new BigDecimal(prop_data.getString("BTC_index"));
		if (start_index.compareTo(this.realtimeIndex) < 0) { 
			//higher
			winning_outcome = 3;
			fields.put("winning_outcome", winning_outcome);
		}
		else if (start_index.compareTo(this.realtimeIndex) == 0) {
			//same
			winning_outcome = 2;
			fields.put("winning_outcome", winning_outcome);
		} else {
			//lower
			winning_outcome = 1;
			fields.put("winning_outcome", winning_outcome);
		}

		return fields;
	}
}


