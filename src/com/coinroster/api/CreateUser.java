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
import com.coinroster.internal.UserMail;

public class CreateUser extends Utils
	{
	public static String method_level = "guest";
	public CreateUser(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			output.put("username_exists", "0");
			output.put("user_was_created", "0");
			
			// first try to process referral:
			
			String 
			
			referral_key = no_whitespace(input.getString("referral_key")),
			referrer_id = null,
			new_user_email_address = null,
			referral_program = null;
					
			String[] referral = null;
			
			if (referral_key.length() == 40)
				{
				referral = db.select_referral(referral_key);
				
				if (referral == null)
					{
					output.put("invalid_referrer", "1");
					break method;
					}
				else
					{
					referrer_id = referral[0];
					new_user_email_address = referral[2];
					referral_program = referral[3];
					}
				}

			String 

			username = no_whitespace(input.getString("username")),
			password = no_whitespace(input.getString("password")),
			
			new_user_id = Server.generate_key(username);
			
			int user_level_default = 0;

			// break method if bad credentials:

			if (username.length() < 4 || username.length() > 40 || password.length() < 8)
				{
				output.put("invalid_credentials", "1");
				break method;
				}

			// lock user table so that username lookup and new user creation are contiguous
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			// if username exists, unlock
			
			if (db.select_user("username", username) != null)
				{
				output.put("username_exists", "1");
				statement.execute("unlock tables");
				break method;
				}

			// user table is still locked | create new user:
			
			String new_password_hash = Server.SHA1(password + new_user_id);
			
			PreparedStatement create_user;

			if (referral == null) create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created) values(?, ?, ?, ?, ?)");
			else // we know the new user's email address
				{					
				int email_ver_flag = 1;
				
				create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created, email_address, email_ver_flag, referral_program, referrer) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
				create_user.setString(6, new_user_email_address);
				create_user.setInt(7, email_ver_flag);
				create_user.setInt(8, Integer.parseInt(referral_program));
				create_user.setString(9, referrer_id);
				}
			
			create_user.setString(1, new_user_id);
			create_user.setString(2, username);
			create_user.setString(3, new_password_hash);
			create_user.setInt(4, user_level_default);
			create_user.setLong(5, System.currentTimeMillis());
			
			create_user.executeUpdate();
			
			statement.execute("unlock tables");
			
			if (referral != null)
				{
				// delete all referral records associated with the referred email address:
	
				PreparedStatement delete_from_referral = sql_connection.prepareStatement("delete from referral where email_address = ?");
				delete_from_referral.setString(1, new_user_email_address);
				delete_from_referral.executeUpdate();
				
				db.store_verified_email(new_user_email_address);
				
				// send email to referrer:
				
				JSONObject referrer = db.select_user("id", referrer_id);
				
				String
				
				subject = "Successful referral",
				message_body = "<b>" + new_user_email_address + "</b> has signed up to CoinRoster!";
				
				new UserMail(referrer, subject, message_body);
				}

			// log the new user in:

			String new_session_token = session.create_session(sql_connection, session, username, new_user_id, user_level_default);

			method.response.new_session_token = new_session_token;
			
			output.put("user_was_created", "1");
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}