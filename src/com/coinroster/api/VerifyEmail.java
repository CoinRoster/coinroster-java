package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;

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

//				Runtime rt = Runtime.getRuntime();
//				String[] add_email_to_mailing_list = {"curl", "-X POST", "https://us12.api.mailchimp.com/3.0/lists/9d79f3f468/members/" ,
//						"-H", "authorization:Bearer b526b109e2f18977b21c0a0f2595babf-us12", 
//						"-H", "Cache-Control:no-cache" ,
//						// "-H", "'Postman-Token: 88357e8d-f1d2-4e9d-b033-8c096b330884' " ,
//						"-d", "'{\"email_address\":\"" + email_address + "\", \"status\":\"subscribed\",\"merge_fields\": {}}"};
				
				try 
					{
					HttpResponse<String> response = Unirest.post("https://us12.api.mailchimp.com/3.0/lists/9d79f3f468/members/")
							  .header("Authorization", "Bearer b526b109e2f18977b21c0a0f2595babf-us12")
							  .header("Cache-Control", "no-cache")
							  .header("Postman-Token", "66f73ef5-35df-4969-aa9d-3380bfb16549")
							  .body("{\"email_address\":\"info@govindmohan.com\", \"status\":\"subscribed\"}")
							  .asString();
					String body = response.getBody();
					log("mailchimp response: " + body);
					}
				catch (Exception e)
					{
					log(e);
					}
				
				

				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}