package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

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

		method : {
			
//------------------------------------------------------------------------------------
		
			String email_address = to_valid_email(input.getString("email_address"));
			
			if (email_address == null)
				{
				output.put("error", "Invalid email address");
				break method;
				}
			
			PreparedStatement select_user = sql_connection.prepareStatement("select * from user where email_address = ? and email_ver_flag = ?");
			select_user.setString(1, email_address);
			select_user.setInt(2, 1);
			ResultSet result_set = select_user.executeQuery();

			while (result_set.next())
				{
				String 
				
				user_id = result_set.getString(1),
				username = result_set.getString(2),
				reset_key = Server.generate_key(user_id);
				
				PreparedStatement create_password_reset = sql_connection.prepareStatement("insert into password_reset(reset_key, user_id, created) values(?, ?, ?)");
				create_password_reset.setString(1, reset_key);
				create_password_reset.setString(2, user_id);
				create_password_reset.setLong(3, System.currentTimeMillis());
				create_password_reset.executeUpdate();
				
				String
				
				subject = "Password reset",
				message_body = "Please <a href='" + Server.host + "/reset.html?" + reset_key + "'>click here</a> to reset the password for <b>" + username + "</b>.";
				message_body += "<br/>";
				message_body += "This reset link can only be used once and will expire in 1 hour if not used.";
				message_body += "<br/>";
				message_body += "If you did not request a password reset, someone may be trying to access your account. We would advise that you reset your password with this link.";
				
				Server.send_mail(email_address, username, subject, message_body);
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}