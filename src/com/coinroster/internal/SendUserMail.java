package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.coinroster.Server;

public class SendUserMail 
	{
	public SendUserMail(Connection sql_connection, String user_id, String username, String subject, String message_body)
		{
		try {
			PreparedStatement select_email = sql_connection.prepareStatement("select email_address, email_ver_flag from user_xref where id = ?");
			select_email.setString(1, user_id);
			ResultSet email = select_email.executeQuery();

			if (email.next())
				{
				String email_address = email.getString(1);
				int email_ver_flag = email.getInt(2);
				
				if (email_address != null && email_ver_flag == 1) Server.send_mail(email_address, username, subject, message_body);
				}		
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		}
	}
