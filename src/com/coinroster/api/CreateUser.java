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

public class CreateUser extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public CreateUser(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			output.put("username_exists", "0");
			output.put("user_was_created", "0");
			
			String 
			
			username = no_whitespace(input.getString("username")),
			password = no_whitespace(input.getString("password")),
			referral_key = no_whitespace(input.getString("referral_key"));
			
			// referral is needed later for new user_xref's:

			String[] referral = null;
			
			// validate referral key:

			if (referral_key.length() == 40)
				{
				referral = db.select_referral(referral_key);
				if (referral == null)
					{
					output.put("invalid_referrer", "1");
					break method;
					}
				}
			/*else // re-ad this 'else' block to only allow referred signups
				{
				output.put("invalid_referrer", "1");
				break method;
				}*/
			
			// break method if bad credentials:

			if (username.length() < 4 || username.length() > 40 || password.length() < 8)
				{
				output.put("invalid_credentials", "1");
				break method;
				}

			// lock user table so that username lookup and new user creation are contiguous
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write");

			// if username exists, unlock and break method
			
			if (db.select_user("username", username) != null)
				{
				statement.execute("unlock tables");
				output.put("username_exists", "1");
				break method;
				}

			// user table is still locked | create new user:
			
	        String 
	        
	        new_user_id = Server.generate_key(username),
	        new_password_hash = Server.SHA1(password + new_user_id);
	        
	        int user_level_default = 0;

	        PreparedStatement create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created) values(?, ?, ?, ?, ?)");	            
	        create_user.setString(1, new_user_id);
	        create_user.setString(2, username);
	        create_user.setString(3, new_password_hash);
	        create_user.setInt(4, user_level_default);
	        create_user.setLong(5, System.currentTimeMillis());
	        create_user.executeUpdate();
	        
	        // user table can be unlocked now since the username has been reserved:
	        
	        statement.execute("unlock tables");
	        
	        // if no referrer, create a default user_xref record
	        
	        if (referral_key.equals("")) 
	        	{	           
	        	PreparedStatement create_user_xref = sql_connection.prepareStatement("insert into user_xref(id) values(?)");
		        create_user_xref.setString(1, new_user_id);
		        create_user_xref.executeUpdate();
	        	}
	        
	        // else, if we know the referrer, we must record it | we also have a confirmed email address:
	        
	        else
	        	{
	        	// referral is selected at the top of the function
	        	
	        	String
	        	
	       		referrer_id = referral[0],
	    		email_address = referral[2],
	    		referral_program = referral[3];
	        	
	        	int email_ver_flag = 1;
	        	
	        	PreparedStatement create_user_xref = sql_connection.prepareStatement("insert into user_xref(id, email_address, email_ver_flag, referral_program, referrer) values(?, ?, ?, ?, ?)");
		        create_user_xref.setString(1, new_user_id);
		        create_user_xref.setString(2, email_address);
		        create_user_xref.setInt(3, email_ver_flag);
		        create_user_xref.setInt(4, Integer.parseInt(referral_program));
		        create_user_xref.setString(5, referrer_id);
		        create_user_xref.executeUpdate();
		        
		        // delete all referral records associated with the referred email address:

		        PreparedStatement delete_from_referral = sql_connection.prepareStatement("delete from referral where email_address = ?");
		        delete_from_referral.setString(1, email_address);
		        delete_from_referral.executeUpdate();
	            
		        db.store_verified_email(email_address);
		        
		        // send email to referrer:
		        
		        String[] referrer_xref = db.select_user_xref("id", referrer_id);
		        
		        String
		        
		        referrer_email = referrer_xref[4],
		        referrer_email_ver_flag = referrer_xref[6];
		        
		        if (referrer_email_ver_flag.equals("1"))
			        {
			        String
					
					to_address = referrer_email, 
					to_user = db.get_username_for_id(referrer_id),
					subject = "Successful referral",
					message_body = "<b>" + email_address + "</b> has signed up to CoinRoster!";
		
					Server.send_mail(to_address, to_user, subject, message_body);
			        }
	        	}
	        
	        // log the new user in:

	        String new_session_token = db.create_session(username, new_user_id, Integer.toString(user_level_default));

	        method.response.new_session_token = new_session_token;
	        
	        output.put("user_was_created", "1");
	        
	        output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}