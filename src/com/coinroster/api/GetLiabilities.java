package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetLiabilities extends Utils{


	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetLiabilities(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);
		
		method : {
			
//------------------------------------------------------------------------------------
			JSONObject internal_liability = db.select_user("username", "internal_liability");
			JSONObject internal_progressive = db.select_user("username", "internal_progressive");
			JSONObject internal_contest = db.select_user("username", "internal_contest_asset");

			output.put("liability_rc_balance", internal_liability.getDouble("rc_balance"));
			output.put("liability_btc_balance", internal_liability.getDouble("btc_balance"));
			output.put("progressive_balance", internal_progressive.getDouble("btc_balance"));
			output.put("contest_balance", internal_contest.getDouble("btc_balance"));
			output.put("status", "1");
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
		
}
