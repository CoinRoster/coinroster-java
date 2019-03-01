package com.coinroster.bots;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.TimeZone;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.Utils;
import com.coinroster.Server;


public class CryptoBot extends Utils {
	private BigDecimal btc_index;
	private Date btc_date;
	private BigDecimal eth_index;
	private Date eth_date;
	private Connection sql_connection;
	//private DB db;
	
	public CryptoBot(Connection sql_connection) throws IOException, JSONException{
		this.sql_connection = sql_connection;
		//this.db = new DB(sql_connection);

	}
	
	public Date getBitcoinIndexDate() {
		return btc_date;
	}

	public BigDecimal getBitcoinIndex() {
		return btc_index;
	}
	
	public Date getEthereumIndexDate() {
		return eth_date;
	}

	public BigDecimal getEthereumIndex() {
		return eth_index;
	}
		
	public JSONObject getDataJSON() throws JSONException {
		
		JSONObject data = new JSONObject();
		data.accumulate("rti", this.getBitcoinIndex().toString());
		data.accumulate("rtiDate", this.getBitcoinIndexDate().toString());

		
		return data;
	}
	
	public void loadAllData() {
		this.loadBitcoinData();
		this.loadEthereumData();
	}
	
	//Scrape data from cmegroup website. Determine when the next update should be.
	public void loadBitcoinData(){

		try {
			String stmt = "select id, price, date_updated from bitcoin_reference order by id DESC limit 1";
			
			PreparedStatement get_data = sql_connection.prepareStatement(stmt);
			ResultSet data = get_data.executeQuery();
			
			data.next();
			
			String rtiDate = data.getString(3);
	
			String rti = data.getString(2);
			this.btc_index = parseRate(rti);
			this.btc_date = parseDate(rtiDate);
			
		}catch (Exception e){
			Server.exception(e);
		}
		
	}
	
	//Scrape data from cmegroup website. Determine when the next update should be.
	public void loadEthereumData(){

		try {
			String stmt = "select id, price, date_updated from ethereum_reference order by id DESC limit 1";
			
			PreparedStatement get_data = sql_connection.prepareStatement(stmt);
			ResultSet data = get_data.executeQuery();
			
			data.next();
			
			String rtiDate = data.getString(3);
							
			String rti = data.getString(2);
			this.eth_index = parseRate(rti);
			this.eth_date = parseDate(rtiDate);
			
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
	
	
	
	public JSONObject chooseBitcoinUnderOverWinner(int contest_id, JSONObject prop_data, JSONArray option_table) throws JSONException {
		
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		//String prop_type = prop_data.getString("prop_type");
		int winning_outcome;
		

		BigDecimal start_index = new BigDecimal(prop_data.getString("over_under_value"));
		if (start_index.compareTo(this.btc_index) <= 0) {
			//higher
			winning_outcome = 1;
			fields.put("winning_outcome", winning_outcome);
		} else {
			//lower
			winning_outcome = 2;
			fields.put("winning_outcome", winning_outcome);
		}

		return fields;
	}
	
public JSONObject chooseEthereumUnderOverWinner(int contest_id, JSONObject prop_data, JSONArray option_table) throws JSONException {
		
		JSONObject fields = new JSONObject();
		fields.put("contest_id", contest_id);
		//String prop_type = prop_data.getString("prop_type");
		int winning_outcome;
		

		BigDecimal start_index = new BigDecimal(prop_data.getString("over_under_value"));
		if (start_index.compareTo(this.eth_index) <= 0) {
			//higher
			winning_outcome = 1;
			fields.put("winning_outcome", winning_outcome);
		} else {
			//lower
			winning_outcome = 2;
			fields.put("winning_outcome", winning_outcome);
		}

		return fields;
	}
}


