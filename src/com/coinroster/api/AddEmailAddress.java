package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Add another email address for a user.
 * 
 * @custom.access standard
 * 
 */
public class AddEmailAddress extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Add another email address for a user.
	 * 
	 * @param method.input.email_address
	 * @throws Exception
	 */
	public AddEmailAddress(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			user_id = session.user_id(),
			email_address = to_valid_email(input.getString("email_address")),
			new_email_ver_key = Server.generate_key(email_address);

			if (email_address == null)
				{
				output.put("error", "Invalid email address");
				break method;
				}
			if (!db.email_in_use(email_address))
				{
				output.put("error", "Email is in use by another user");
				break method;
				}
			
			int newsletter_flag = input.getInt("newsletter_flag"); // 1 for true, 0 for false
			
			if (email_address.length() <= 60)
				{
				PreparedStatement add_email = sql_connection.prepareStatement("update user set email_address = ?, email_ver_key = ?, email_ver_flag = 0, newsletter_flag = ? where id = ?");
				add_email.setString(1, email_address);
				add_email.setString(2, new_email_ver_key);
				add_email.setInt(3, newsletter_flag);
				add_email.setString(4, user_id);
				add_email.executeUpdate();
				
				new SendEmailVerification(method);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	} 