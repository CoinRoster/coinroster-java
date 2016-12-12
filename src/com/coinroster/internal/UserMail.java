package com.coinroster.internal;

import org.json.JSONObject;

import com.coinroster.Server;

public class UserMail 
	{
	public UserMail(JSONObject user, String subject, String message_body)
		{
		try {
			if (user.getInt("email_ver_flag") == 1)
				{
				String 
				
				email_address = user.getString("email_address"),
				username = user.getString("username");
				
				message_body = message_body.replace("<!--USERNAME-->", username);
				Server.send_mail(email_address, username, subject, message_body);	
				}
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
