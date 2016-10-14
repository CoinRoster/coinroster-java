package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class ChangeNewsletterFlag extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public ChangeNewsletterFlag(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			user_id = session.user_id(),
			current_flag_state = input.getString("current_flag_state"); // 1 for subscribed, 0 for unsubscribed
			
			String new_flag_state = "0";
			
			if (current_flag_state.equals("1")) new_flag_state = "0";
			else new_flag_state = "1";

			PreparedStatement set_newsletter_flag = sql_connection.prepareStatement("update user_xref set newsletter_flag = ? where id = ?");
			set_newsletter_flag.setInt(1, Integer.parseInt(new_flag_state));
			set_newsletter_flag.setString(2, user_id);
			set_newsletter_flag.executeUpdate();
	        
	        output.put("newsletter_flag", new_flag_state);

	        output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}