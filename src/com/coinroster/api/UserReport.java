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

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray user_report = new JSONArray();
			
			PreparedStatement select_all_users = sql_connection.prepareStatement("select * from user");
			ResultSet result_set = select_all_users.executeQuery();

			while (result_set.next())
				{
				String user_id = result_set.getString(1);
				String username = result_set.getString(2);
				String stored_password_hash = result_set.getString(3);
				int user_level = result_set.getInt(4);
				Long created = result_set.getLong(5);
				Long last_login = result_set.getLong(6);
				double btc_balance = result_set.getDouble(7);
				double rc_balance = result_set.getDouble(8);
				String ext_address = result_set.getString(9);
				String email_address = result_set.getString(10);
				String email_ver_key = result_set.getString(11);
				int email_ver_flag = result_set.getInt(12);
				int newsletter_flag = result_set.getInt(13);
				int referral_program = result_set.getInt(14);
				String referrer = result_set.getString(15);
				int ext_address_secure_flag = result_set.getInt(16);
				int free_play = result_set.getInt(17);
				Long last_active = result_set.getLong(18);
				int contest_status = result_set.getInt(19);
				String currency = result_set.getString(20);
				
				String referrer_username = "";
				
				if (referrer != null) referrer_username = db.get_username_for_id(referrer);
				if (email_address == null) email_address = "";
				
				JSONObject user = new JSONObject();
				
				user.put("created", created);
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
				user.put("free_play", free_play);
				user.put("last_active", last_active);
				user.put("currency", currency);
				
				user_report.put(user);
				}
			
			output.put("user_report", user_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}