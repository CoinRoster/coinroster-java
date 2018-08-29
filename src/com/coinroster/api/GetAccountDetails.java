package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetAccountDetails extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetAccountDetails(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();
			
			JSONObject user = db.select_user("id", user_id);
			
			if (user != null)
				{
				PreparedStatement get_in_play_balance = sql_connection.prepareStatement("select sum(amount) from entry inner join contest on entry.contest_id = contest.id where entry.user_id = ? and contest.status < 3");
				get_in_play_balance.setString(1, user_id);
				ResultSet in_play_balance_rs = get_in_play_balance.executeQuery();
				in_play_balance_rs.next();
				
				double in_play_balance = in_play_balance_rs.getDouble(1);
				
				output.put("btc_balance", user.get("btc_balance"));
				output.put("rc_balance", user.get("rc_balance"));
				output.put("in_play_balance", in_play_balance);
				output.put("ext_address", user.get("ext_address"));
				output.put("email_address", user.get("email_address"));
				output.put("email_ver_flag", user.get("email_ver_flag"));
				output.put("newsletter_flag", user.get("newsletter_flag"));
				output.put("ext_address_secure_flag", user.get("ext_address_secure_flag"));
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}