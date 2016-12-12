package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class VerifyEmail extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public VerifyEmail(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String email_ver_key = no_whitespace(input.getString("email_ver_key"));
			
			if (email_ver_key.length() != 40) break method;
			
			JSONObject user = db.select_user("email_ver_key", email_ver_key);

			if (user != null)
				{
				PreparedStatement update_email_ver_key = sql_connection.prepareStatement("update user set email_ver_flag = 1 where email_ver_key = ?");
				update_email_ver_key.setString(1, email_ver_key);
				update_email_ver_key.executeUpdate();
				
				String email_address = user.getString("email_address");
				db.store_verified_email(email_address);

				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}