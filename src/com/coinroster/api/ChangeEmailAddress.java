package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

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
			email_address = to_valid_email(input.getString("email_address")),
			new_email_ver_key = Server.generate_key(email_address);
			
			int email_ver_flag = 0;
			
			if (email_address == null)
				{
				output.put("error", "Invalid email address");
				break method;
				}

			if (email_address.length() > 60)
				{
				output.put("error", "Email address is too long");
				break method;
				}
			
			PreparedStatement check_email_address = sql_connection.prepareStatement("select * from verified_email where email_address = ?");
			check_email_address.setString(1, email_address);
			ResultSet result_set = check_email_address.executeQuery();
			
			if (result_set.next())
				{
				String email_address_owner_id = result_set.getString(1);
				
				if (email_address_owner_id.equals(user_id)) // verified email belongs to active user
					{
					new_email_ver_key = null;
					email_ver_flag = 1;
					}
				else // email does not belong to active user
					{
					output.put("error", "Email is in use by another user");
					break method;
					}
				}
			
			PreparedStatement change_email = sql_connection.prepareStatement("update user set email_address = ?, email_ver_key = ?, email_ver_flag = ? where id = ?");
			change_email.setString(1, email_address);
			change_email.setString(2, new_email_ver_key);
			change_email.setInt(3, email_ver_flag);
			change_email.setString(4, user_id);
			change_email.executeUpdate();

			if (email_ver_flag == 0) new SendEmailVerification(method);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}