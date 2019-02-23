package com.coinroster;

import java.io.IOException;
import java.sql.Connection;

import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.api.SetupPropBet;
import com.coinroster.bots.BitcoinBot;

public class FixedOddsContest extends Utils{
	public static String method_level = "admin";
	private Connection sql_connection;
	private Session session;
	
	@SuppressWarnings("unused")
	private String session_token;
	
	public FixedOddsContest(Connection connection) {
		this.sql_connection = connection;
	}
	
	public void buildSession(String user_id) {
		session = new Session("", user_id);
		try {
			session_token = session.create_session(sql_connection, session, "ContestPoster", user_id, 0);
		} catch (Exception e) {
			Server.exception(e);
		}
		
	}
	
	public void postBitcoinContest(String title, Long registration_deadline, Long settlement_deadline) throws JSONException, IOException {
		JSONObject input = new JSONObject();
		JSONObject data = new JSONObject();
		JSONObject prop_data = new JSONObject();
		

		
		BitcoinBot bitcoinBot = new BitcoinBot(sql_connection);
		bitcoinBot.setup();
		
		prop_data.put("prop_type", "OVER_UNDER_BTC");
		prop_data.put("registration_deadline", registration_deadline);
		prop_data.put("settlement_deadline", settlement_deadline);
		prop_data.put("over_odds", 1.85);
		prop_data.put("under_odds", 1.85);
		prop_data.put("risk", 0.001);
		prop_data.put("over_under_value", bitcoinBot.getRealtimeIndex());
		prop_data.put("auto_settle", 1);
		prop_data.put("title", title);
		
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
		method.internal_caller = true;
		try {
			new SetupPropBet(method);
		} catch (Exception e) {
			Server.exception(e);
		}
			
		
	}
}
