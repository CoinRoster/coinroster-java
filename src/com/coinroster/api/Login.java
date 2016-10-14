package com.coinroster.api;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class Login extends Utils
	{
	public static String method_level = "guest";
	public Login(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			username = no_whitespace(input.getString("username")),
			password = no_whitespace(input.getString("password"));

			// if the client already has a session:
			
			if (session.active()) 
				{
				// if the session belongs to the submitted username, do nothing:
				
				if (username.equals(session.username()))
					{
					output.put("status", "1");
					break method;
					}
				
				// else, remove the existing session token:
				
				else Server.kill_session(session.token());
				}
			
			// break method if bad credentials:
			
			if (username.length() < 4 || username.length() > 40 || password.length() < 8) break method;
	
			// log in:
			
			String[] user = db.select_user("username", username);
			
			if (user != null)
				{
				String
				
				user_id = user[0],
				stored_password_hash = user[2],
				user_level = user[3];
				
				// if internal account, break method:
				
				if (user_level.equals("2")) break method;
				
				// check password:
				
				if (SHA1(password + user_id).equals(stored_password_hash)) 
					{
					// create session:
					
					String new_session_token = db.create_session(username, user_id, user_level);
					
					// update browser cookie:
	
					method.response.new_session_token = new_session_token;
					
					output.put("status", "1");
					}
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}