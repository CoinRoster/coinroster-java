package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UserReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public UserReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray user_report = new JSONArray();
			
			PreparedStatement select_all_users = sql_connection.prepareStatement("select * from user");
			ResultSet result_set = select_all_users.executeQuery();

			while (result_set.next())
				{
				String user_id = result_set.getString(1);
				
				String[] user_xref = db.select_user_xref("id", user_id);

				String
				
				username = result_set.getString(2),
				user_level = result_set.getString(4),
				user_created = result_set.getString(5),
				last_login = result_set.getString(6),

				btc_balance = user_xref[1],
				rc_balance = user_xref[2],
				ext_address = user_xref[3],
				email_address = user_xref[4],
				email_ver_key = user_xref[5],
				email_ver_flag = user_xref[6],
				newsletter_flag = user_xref[7],
				referral_program = user_xref[8],
				referrer_id = user_xref[9],
				referrer_username = "";
		 
				if (referrer_id != null) referrer_username = db.get_username_for_id(referrer_id);
				if (email_address == null) email_address = "";
				
				JSONObject user = new JSONObject();
				
				user.put("created", user_created);
				user.put("last_login", last_login);
				user.put("user_id", user_id);
				user.put("username", username);
				user.put("user_level", user_level);
				user.put("btc_balance", btc_balance);
				user.put("rc_balance", rc_balance);
				user.put("email_address", email_address);
				user.put("email_ver_flag", email_ver_flag);
				user.put("newsletter_flag", newsletter_flag);
				user.put("referral_program", referral_program);
				user.put("referrer_username", referrer_username);
				
				user_report.put(user);
				}
			
			output.put("user_report", user_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}