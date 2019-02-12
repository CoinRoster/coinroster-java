package com.coinroster;

import java.io.IOException;
import java.sql.Connection;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.api.SetupPropBet;
import com.coinroster.bots.BitcoinBot;

public class FixedOddsContest extends Utils{
	public static String method_level = "admin";
	private Connection sql_connection;
	private String user_id = "2f2e0234b461dba8c89ce950f1045869f41fb73c";
	private int user_level = 0;
	private Session session;
	
	@SuppressWarnings("unused")
	private String session_token;
	
	public FixedOddsContest(Connection connection) {
		this.sql_connection = connection;
	}
	
	public void buildSession() {
		session = new Session("");
		try {
			session_token = session.create_session(sql_connection, session, "ContestPoster", user_id, user_level);
		} catch (Exception e) {
			Server.exception(e);
		}
		
	}
	
	public void postBitcoinContest() throws JSONException, IOException {
		JSONObject input = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject prop_data = new JSONObject();
		
		Date c_date = new Date(System.currentTimeMillis()); //time of price index.
		
		Calendar cal = Calendar.getInstance();

		cal.setTime(c_date);
		cal.add(Calendar.MINUTE, 74); 
		cal.add(Calendar.SECOND, 30); //Registration time.
		Date d_date = cal.getTime();
		Long registration_deadline = d_date.getTime();
		
		cal.add(Calendar.SECOND, 30);
		cal.add(Calendar.HOUR_OF_DAY, 24); //From registration deadline to settlement.
		d_date = cal.getTime();
		Long settlement_deadline = d_date.getTime();
		
		BitcoinBot bitcoinBot = new BitcoinBot(sql_connection);
		bitcoinBot.setup();
		
		prop_data.put("prop_type", "OVER_UNDER_BTC");
		prop_data.put("registration_deadline", registration_deadline);
		prop_data.put("settlement_deadline", settlement_deadline);
		prop_data.put("over_odds", 1.85);
		prop_data.put("under_odds", 1.85);
		prop_data.put("risk", 0.001);
		prop_data.put("over_under_value", bitcoinBot.getRealtimeIndex());
		
		data.put("sub_category", "BITCOINS");
		data.put("private", false);
		data.put("prop_data", prop_data);
		input.put("data", data);
		
			
		MethodInstance method = new MethodInstance();
		JSONObject output = new JSONObject("{\"status\":\"0\"}");
		method.session = session;
		method.input = input;	
		method.output = output;
		method.sql_connection = sql_connection;
		method.response = null;
		try {
			new SetupPropBet(method);
		} catch (Exception e) {
			Server.exception(e);
		}
			
		
	}
}
