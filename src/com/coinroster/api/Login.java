package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.CallCGS;

public class Login extends Utils
	{
	public static String method_level = "guest";
	public Login(MethodInstance method) throws Exception 
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
			
			JSONObject user = db.select_user("username", username);
			
			if (user != null)
				{
				String
				
				user_id = user.getString("user_id"),
				stored_password_hash = user.getString("stored_password_hash");
				
				int user_level = user.getInt("user_level");
								
				// Call CGS -----------------------------------------------------------------
				if (user.getString("cgs_address") == null || user.getString("cgs_address").equals(""))
					{
					
					
					Statement statement = sql_connection.createStatement();
					statement.execute("lock tables user write");
					try {
						lock: {
							log("generating user cgs address");
							JSONObject rpc_call = new JSONObject();
							
							rpc_call.put("method", "newAccount");
							
							JSONObject rpc_method_params = new JSONObject();
		
							rpc_method_params.put("craccount", user.getString("username"));
							rpc_method_params.put("type", "btc");
							
							rpc_call.put("params", rpc_method_params);
							
							CallCGS call = new CallCGS(rpc_call);
							
							JSONObject 
							
							result = call.get_result();
							
							if (result != null) 
								{
								log(result.toString());
								}
							else
								{
								log(call.get_error().toString());
								break lock;
								}
							String cgs_address = result.getString("account");
							PreparedStatement update_user_cgs_address = sql_connection.prepareStatement("update user set cgs_address = ? where username = ?");
							update_user_cgs_address.setString(1, cgs_address);
							update_user_cgs_address.setString(2, user.getString("username"));
							update_user_cgs_address.executeUpdate();
							}
						}
					catch (Exception e)
						{
						log("ERROR CREATING CGS ADDRESS FOR" + session.username());
						Server.exception(e);
						}
					finally
						{
						statement.execute("unlock tables");
						}

						// end CallCGS --------------------------------------------------------------
						}
				
				
				// if internal account, break method:
				
				if (user_level == 2) break method;
				
				if (SHA1(password + user_id).equals(stored_password_hash)) 
					{
					String new_session_token = session.create_session(sql_connection, session, username, user_id, user_level);

					method.response.set_cookie("session_token", new_session_token);
					
					output.put("status", "1");
					}
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}