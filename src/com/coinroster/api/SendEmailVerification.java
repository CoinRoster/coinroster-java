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

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();

			String[] user_xref = db.select_user_xref("id", user_id);

			if (user_xref != null)
				{
				String
				
				to_address = user_xref[4], 
				to_user = db.get_username_for_id(user_id), 
				email_ver_key = user_xref[5],
				subject = "Verify your e-mail address",
				message_body = "Please <a href='" + Server.host + "/verify.html?" + email_ver_key + "'>click here</a> to verify your e-mail address.";

				Server.send_mail(to_address, to_user, subject, message_body);
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}