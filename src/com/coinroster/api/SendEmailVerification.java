package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class SendEmailVerification extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public SendEmailVerification(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();
			
			JSONObject user = db.select_user("id", user_id);

			if (user != null)
				{
				String
				
				email_address = user.getString("email_address"), 
				username = user.getString("username"), 
				email_ver_key = user.getString("email_ver_key"),
				subject = "Verify your e-mail address",
				message_body = "Please <a href='" + Server.host + "/verify.html?" + email_ver_key + "'>click here</a> to verify your e-mail address.";

				Server.send_mail(email_address, username, subject, message_body);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}