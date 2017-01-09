package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class ChangeEmailAddress extends Utils
	{
	public static String method_level = "standard";
	public ChangeEmailAddress(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			user_id = session.user_id(),
			email_address = no_whitespace(input.getString("email_address")),
			new_email_ver_key = Server.generate_key(email_address);
			
			if (!is_valid_email(email_address))
				{
				output.put("error_message", "Invalid email address");
				break method;
				}
			
			if (email_address.length() <= 60)
				{
				PreparedStatement change_email = sql_connection.prepareStatement("update user set email_address = ?, email_ver_key = ?, email_ver_flag = 0 where id = ?");
				change_email.setString(1, email_address);
				change_email.setString(2, new_email_ver_key);
				change_email.setString(3, user_id);
				change_email.executeUpdate();
				
				new SendEmailVerification(method);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}