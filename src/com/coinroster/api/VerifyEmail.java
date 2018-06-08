package com.coinroster.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
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
				boolean update_session_user_level = false;
				
				int user_level = user.getInt("user_level");
				
				if (user_level == 3) 
					{
					user_level = 0;
					update_session_user_level = true;
					}
				
				PreparedStatement update_email_ver_key = sql_connection.prepareStatement("update user set email_ver_flag = 1, level = ? where email_ver_key = ?");
				update_email_ver_key.setInt(1, user_level);
				update_email_ver_key.setString(2, email_ver_key);
				update_email_ver_key.executeUpdate();
				
				String email_address = user.getString("email_address");
				db.store_verified_email(user.getString("user_id"), email_address);
				
				if (update_session_user_level) 
					{
					session.update_user_level(user.getString("user_id"), user_level);
					}

				Runtime rt = Runtime.getRuntime();
				String[] add_email_to_mailing_list = {"curl -X POST https://us12.api.mailchimp.com/3.0/lists/9d79f3f468/members/",
						"-H 'Authorization: Bearer b526b109e2f18977b21c0a0f2595babf-us12'", "-H 'Cache-Control: no-cache'", 
						"-H 'Postman-Token: 88357e8d-f1d2-4e9d-b033-8c096b330884'", 
						"-d '{\"email_address\":\"" + email_address + "\", \"status\":\"subscribed\"}'"};
				Process proc = rt.exec(add_email_to_mailing_list);
				BufferedReader stdInput = new BufferedReader(new 
					     InputStreamReader(proc.getInputStream()));

					BufferedReader stdError = new BufferedReader(new 
					     InputStreamReader(proc.getErrorStream()));

					// read the output from the command
					log("add to mail list standard output:\n");
					String s = null;
					while ((s = stdInput.readLine()) != null) {
					    System.out.println(s);
					}

					// read any errors from the attempted command
					log("add to mail list (if any):\n");
					while ((s = stdError.readLine()) != null) {
					    System.out.println(s);
					}

				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}