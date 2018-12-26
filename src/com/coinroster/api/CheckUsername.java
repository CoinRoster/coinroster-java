package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Validate a username for user creation
 * 
 * @custom.access guest
 *
 */
public class CheckUsername extends Utils
	{
	public static String method_level = "guest";
	
	/**
	 * Validate a username for user creation
	 * 
	 * @param method
	 * @throws Exception
	 */
	@SuppressWarnings("unused")
	public CheckUsername(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String username = no_whitespace(input.getString("username"));

			if (username.length() < 4 || username.length() > 40) 
				{
				output.put("error", "Invalid username");
				break method;
				}
			
			if (db.select_user("username", username) != null) 
				{
				output.put("error", "Username is taken");
				break method;
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}