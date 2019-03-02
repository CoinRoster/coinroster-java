package com.coinroster;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.api.SetupPropBet;

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
	
	public void postCryptoContest(JSONObject prop_data) throws JSONException, IOException, SQLException {
		JSONObject input = new JSONObject();
		JSONObject data = new JSONObject();

		data.put("sub_category", "CRYPTO");
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
