package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdatePassword extends Utils
	{
	public static String method_level = "guest";
	public UpdatePassword(MethodInstance method) throws Exception 
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
			
			reset_key = no_whitespace(input.getString("reset_key")),
			password = no_whitespace(input.getString("password"));
			
			if (reset_key.length() != 40) break method;
			if (password.length() < 8) break method;
			
			String[] password_reset = db.select_password_reset(reset_key);
			
			if (password_reset == null)
				{
				output.put("error", "Invalid reset key");
				break method;
				}
			else
				{
				PreparedStatement delete_password_reset = sql_connection.prepareStatement("delete from password_reset where reset_key = ?");
				delete_password_reset.setString(1, reset_key);
				delete_password_reset.executeUpdate();
				
				String
				
				user_id = password_reset[1],
				created = password_reset[2];
				
				Long key_age = System.currentTimeMillis() - Long.parseLong(created);

				if (key_age < 60 * Server.minute)
					{
					String new_password_hash = Server.SHA1(password + user_id);
					
					PreparedStatement reset_password = sql_connection.prepareStatement("update user set password = ? where id = ?");
					reset_password.setString(1, new_password_hash);
					reset_password.setString(2, user_id);
					reset_password.executeUpdate();
					
					// log the user in:
					
					JSONObject user = db.select_user("id", user_id);
					
					String username = user.getString("username");
					
					int user_level = user.getInt("user_level");
					
					String new_session_token = session.create_session(sql_connection, session, username, user_id, user_level);

					method.response.set_cookie("session_token", new_session_token);
					
					output.put("status", "1");
					}
				else output.put("error", "Expired reset key");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}