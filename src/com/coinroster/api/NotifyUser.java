package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class NotifyUser extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public NotifyUser(MethodInstance method) throws Exception 
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
			
			user_id = input.getString("user_id"),
			subject = input.getString("subject"),
			message = input.getString("message").trim();
			
			if (subject.equals(""))
				{
	            output.put("error", "Error: subject is empty");
	        	break method;
				}
			if (message.equals(""))
				{
	            output.put("error", "Error: message is empty");
	        	break method;
				}
			
			JSONObject user = db.select_user("id", user_id);
			
			if (user == null)
				{
                output.put("error", "Invalid user");
            	break method;
				}

			String 
			
			email_address = user.getString("email_address"),
			username = user.getString("username");
			
			if (!email_address.contains("@"))
				{
	            output.put("error", "Error: user does not have an email address on file");
	        	break method;
				}

			message = message.replace("<!--USERNAME-->", username);
			
			while (message.endsWith("<p>&nbsp;</p>")) message = message.substring(0, message.lastIndexOf("<p>&nbsp;</p>")).trim();
		
			log(username);
			log(email_address);
			log(subject);
			log(message);
			
			Server.send_mail(email_address, username, subject, message);	
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}