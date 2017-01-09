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

			// start by setting up an unverified user (unless they are successfully referred):
			
			int 
			
			user_level = 3,
			email_ver_flag = 0;
			
			// first try to process referral:
			
			String 
			
			referral_key = no_whitespace(input.getString("referral_key")),
			referrer_id = null,
			email_address = null,
			referral_program = null;
					
			String[] referral = null;
			
			if (referral_key.length() > 0)
				{
				referral = db.select_referral(referral_key);
				
				if (referral == null)
					{
					output.put("error_message", "Invalid referrer key");
					break method;
					}
				else
					{
					referrer_id = referral[0];
					email_address = referral[2];
					referral_program = referral[3];
					user_level = 0;
					email_ver_flag = 1;
					}
				}
			else
				{
				email_address = no_whitespace(input.getString("email_address"));
				if (!is_valid_email(email_address))
					{
					output.put("error_message", "Invalid email address");
					break method;
					}
				}

			String
			
			username = no_whitespace(input.getString("username")),
			password = no_whitespace(input.getString("password")),
			
			new_user_id = Server.generate_key(username),
			email_ver_key = Server.generate_key(email_address);
			
			// break method if bad credentials:

			if (username.length() < 4 || username.length() > 40 || password.length() < 8)
				{
				output.put("error_message", "Invalid credentials");
				break method;
				}

			// lock user table so that username lookup and new user creation are contiguous
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			// if username exists, unlock
			
			if (db.select_user("username", username) != null)
				{
				output.put("error_message", "Username is taken");
				statement.execute("unlock tables");
				break method;
				}

			// user table is still locked | create new user:
			
			String new_password_hash = Server.SHA1(password + new_user_id);
			
			PreparedStatement create_user;

			if (referral == null) 
				{
				create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created, email_address, email_ver_flag, email_ver_key) values(?, ?, ?, ?, ?, ?, ?, ?)");
				create_user.setString(8, email_ver_key);
				}
			else
				{		
				create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created, email_address, email_ver_flag, referral_program, referrer) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
				create_user.setInt(8, Integer.parseInt(referral_program));
				create_user.setString(9, referrer_id);
				}
			
			create_user.setString(1, new_user_id);
			create_user.setString(2, username);
			create_user.setString(3, new_password_hash);
			create_user.setInt(4, user_level);
			create_user.setLong(5, System.currentTimeMillis());
			create_user.setString(6, email_address);
			create_user.setInt(7, email_ver_flag);
			
			create_user.executeUpdate();
			
			statement.execute("unlock tables");
			
			if (referral != null)
				{
				// delete all referral records associated with the referred email address:
	
				PreparedStatement delete_from_referral = sql_connection.prepareStatement("delete from referral where email_address = ?");
				delete_from_referral.setString(1, email_address);
				delete_from_referral.executeUpdate();
				
				db.store_verified_email(new_user_id, email_address);
				
				// send email to referrer:
				
				JSONObject referrer = db.select_user("id", referrer_id);
				
				String
				
				subject = "Successful referral",
				message_body = "<b>" + email_address + "</b> has signed up to CoinRoster!";
				
				new UserMail(referrer, subject, message_body);
				}

			// log the new user in:

			String new_session_token = session.create_session(sql_connection, session, username, new_user_id, user_level);

			method.response.new_session_token = new_session_token;
			
			// if user is unverified, send email verification:
			
			if (user_level == 3) db.send_verification_email(username, email_address, email_ver_key);
		
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}