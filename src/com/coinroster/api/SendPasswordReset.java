package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class SendPasswordReset extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public SendPasswordReset(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String email_address = no_whitespace(input.getString("email_address"));
			
			PreparedStatement select_user_xref = sql_connection.prepareStatement("select * from user_xref where email_address = ? and email_ver_flag = ?");
			select_user_xref.setString(1, email_address);
			select_user_xref.setInt(2, 1);
			ResultSet result_set = select_user_xref.executeQuery();

			while (result_set.next())
				{
				String 
				
				user_id = result_set.getString(1),
				reset_key = Server.generate_key(user_id);
				
				PreparedStatement create_password_reset = sql_connection.prepareStatement("insert into password_reset(reset_key, user_id, created) values(?, ?, ?)");
				create_password_reset.setString(1, reset_key);
				create_password_reset.setString(2, user_id);
				create_password_reset.setLong(3, System.currentTimeMillis());
				create_password_reset.executeUpdate();
				
				String
				
				to_address = email_address, 
				to_user = db.get_username_for_id(user_id),
				subject = "Password reset",
				message_body = "Please <a href='" + Server.host + "/reset.html?" + reset_key + "'>click here</a> to reset the password for <b>" + to_user + "</b>.";

				Server.send_mail(to_address, to_user, subject, message_body);
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}