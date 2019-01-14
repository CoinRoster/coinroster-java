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
 * Change a user's password.
 * 
 * @custom.access standard
 *
 */
public class ChangePassword extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Change a user's password.
	 * 
	 * @param method.input.old_password 
	 * @param method.input.new_password
	 */
	public ChangePassword(MethodInstance method) throws Exception 
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
			old_password = input.getString("old_password"),
			new_password = input.getString("new_password");
			
			if (new_password.length() < 8) break method;
			
			JSONObject user = db.select_user("id", user_id);
			
			if (user != null)
				{
				String stored_password_hash = user.getString("stored_password_hash");
				
				// authenticate old password:
				
				if (Server.SHA1(old_password + user_id).equals(stored_password_hash))
					{
					String new_password_hash = Server.SHA1(new_password + user_id);
					
					PreparedStatement change_password = sql_connection.prepareStatement("update user set password = ? where id = ?");
					change_password.setString(1, new_password_hash);
					change_password.setString(2, user_id);
					change_password.executeUpdate();
					
					output.put("status", "1");
					}
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}