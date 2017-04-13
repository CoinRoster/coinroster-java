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

			// first, a quick length check of username and password
			
			String
			
			username = no_whitespace(input.getString("username")),
			password = no_whitespace(input.getString("password"));

			if (username.length() < 4 || username.length() > 40 || password.length() < 8)
				{
				output.put("error", "Invalid credentials");
				break method;
				}
			
			// some defaults for setting up an unverified user (unless they are successfully referred):

			String
			
			new_user_id = Server.generate_key(username),
			email_ver_key = Server.generate_key(username),
			referrer_id = null,
			email_address = null;
			
			int 
			
			user_level = 3,
			email_ver_flag = 0;

			double referral_program = 0;
			
			// next, try to process referral:
			
			String 
			
			referral_key = no_whitespace(input.getString("referral_key")),
			promo_code = no_whitespace(input.getString("promo_code"));
			
			if (promo_code.length() > 100)
				{
				output.put("error", "Promo code is too long");
				break method;
				}
			
			JSONObject 
			
			referral = null,
			referrer = null;
			
			if (referral_key.length() > 0)
				{
				referral = db.select_referral(referral_key);
				
				if (referral == null)
					{
					output.put("error", "Invalid referrer key");
					break method;
					}
				else
					{
					referrer_id = referral.getString("referrer_id");
					email_address = referral.getString("email_address");
					referral_program = referral.getDouble("referral_program");
					if (!referral.isNull("promo_code")) promo_code = referral.getString("promo_code");
					user_level = 0;
					email_ver_flag = 1;
					email_ver_key = null;
					referrer = db.select_user("id", referrer_id);
					}
				}
			else
				{
				email_address = to_valid_email(input.getString("email_address"));
				
				if (email_address == null)
					{
					output.put("error", "Invalid email address");
					break method;
					}
				
				if (db.email_in_use(email_address))
					{
					output.put("error", "Email is in use");
					break method;
					}
				}
			
			JSONObject promo = null;
	
			if (promo_code.equals("")) promo_code = null;
			else // we have a promo code from referral link or user-supplied code
				{
				promo = db.select_promo(promo_code);
				
				if (promo == null) // user-supplied code does not bonus a record
					{
					output.put("error", "Invalid promo code");
					break method;
					}
				
				if (promo.getInt("cancelled") > 0)
					{
					// we only show an error if the user actively supplied a cancelled code
					// if the cancelled code came from a referral link, we process the signup without the promo
					
					if (referrer_id == null) 
						{
						output.put("error", "This promo code has been cancelled");
						break method;
						}
					else promo = null;
					}
				else if (referrer_id == null && !promo.isNull("referrer")) // only gets here if promo has not been cancelled
					{
					referrer_id = promo.getString("referrer");
					referrer = db.select_user("id", referrer_id);
					referral_program = referrer.getDouble("referral_program");
					}
				}
			
			// at this point we should have our referrer_id, referral_program, and promo object where applicable
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, transaction write");
			
			boolean success = false;

			try {
				lock : {

					if (db.select_user("username", username) != null) // must be in lock clause to be contiguous with account creation
						{
						output.put("error", "Username is taken");
						break lock;
						}
	
					// username is available - create user
					
					String new_password_hash = Server.SHA1(password + new_user_id);
					
					PreparedStatement create_user;
	
					if (referrer_id == null) create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created, email_address, email_ver_flag, email_ver_key, referral_offer, promo_code, deposit_bonus_cap, deposit_bonus_rollover_multiple) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
					else // we have referrer_id & referral_program from a referral link or promo code
						{		
						create_user = sql_connection.prepareStatement("insert into user(id, username, password, level, created, email_address, email_ver_flag, email_ver_key, referral_offer, promo_code, deposit_bonus_cap, deposit_bonus_rollover_multiple, referral_program, referrer) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
						create_user.setDouble(13, referral_program);
						create_user.setString(14, referrer_id);
						}

					double default_referral_offer = Double.parseDouble(Server.control.get("default_referral_offer"));
					double deposit_bonus_cap = Double.parseDouble(Server.control.get("deposit_bonus_cap"));
					int deposit_bonus_rollover_multiple = Integer.parseInt(Server.control.get("deposit_bonus_rollover_multiple"));
							
					create_user.setString(1, new_user_id);
					create_user.setString(2, username);
					create_user.setString(3, new_password_hash);
					create_user.setInt(4, user_level);
					create_user.setLong(5, System.currentTimeMillis());
					create_user.setString(6, email_address);
					create_user.setInt(7, email_ver_flag);
					create_user.setString(8, email_ver_key);
					create_user.setDouble(9, default_referral_offer);
					create_user.setString(10, promo_code);
					create_user.setDouble(11, deposit_bonus_cap);
					create_user.setInt(12, deposit_bonus_rollover_multiple);
					
					create_user.executeUpdate();
					
					if (promo != null) // the promo object made it through validation
						{
						double free_play_amount = promo.getDouble("free_play_amount");
						int rollover_multiple = promo.getInt("rollover_multiple");
						double rollover_quota = multiply(free_play_amount, rollover_multiple, 0);

						PreparedStatement update_user = sql_connection.prepareStatement("update user set promo_code = ?, withdrawal_locked = 1, rollover_quota = ?, btc_balance = ? where id = ?");
						update_user.setString(1, promo_code);
						update_user.setDouble(2, rollover_quota);
						update_user.setDouble(3, free_play_amount);
						update_user.setString(4, new_user_id);
						update_user.executeUpdate();
						
						// increase liability (goes further negative)
						
						JSONObject liability_account = db.select_user("username", "internal_liability");
						String liability_account_id = liability_account.getString("user_id");
						double btc_liability_balance = liability_account.getDouble("btc_balance");
						Double new_btc_liability_balance = subtract(btc_liability_balance, free_play_amount, 0);
						db.update_btc_balance(liability_account_id, new_btc_liability_balance);
						
						Long transaction_timestamp = System.currentTimeMillis();
						String transaction_type = "BTC-PROMO-DEPOSIT";
						String memo = "Promo code: " + promo_code;
						
						PreparedStatement new_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");				
						new_transaction.setLong(1, transaction_timestamp);
						new_transaction.setString(2, liability_account_id);
						new_transaction.setString(3, transaction_type);
						new_transaction.setString(4, liability_account_id);
						new_transaction.setString(5, new_user_id);
						new_transaction.setDouble(6, free_play_amount);
						new_transaction.setString(7, "BTC");
						new_transaction.setString(8, "BTC");
						new_transaction.setString(9, memo);
						new_transaction.executeUpdate();
						}
					
					success = true;
					}
				}
			catch (Exception e)
				{
				Server.exception(e);
				output.put("error_message", "Unable to create user");
				}
			finally
				{
				statement.execute("unlock tables");
				}
			
			if (success)
				{
				// send email to affiliate:
				
				if (referrer_id != null) // either from referral record or promo code
					{
					String
					
					subject = null,
					message_body = null,
					income_message = "You will earn <b>" + (int) multiply(referrer.getDouble("referral_offer"), 100, 0) + "%</b> of the rake from this account once their playing requirement has been met.";
					
					if (referral != null)
						{
						// delete all referral records associated with the referred email address:
			
						PreparedStatement delete_from_referral = sql_connection.prepareStatement("delete from referral where email_address = ?");
						delete_from_referral.setString(1, email_address);
						delete_from_referral.executeUpdate();
						
						db.store_verified_email(new_user_id, email_address);
						
						subject = "Successful referral!";
						message_body = "<b>" + email_address + "</b> has signed up to CoinRoster!";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += income_message;
						}
					else if (promo != null)
						{
						subject = "Someone used your promo code!";
						message_body = "<b>" + username + "</b> has signed up to CoinRoster using your promo code!";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += income_message;
						}
					
					new UserMail(referrer, subject, message_body);
					}
				
				// send email to unverified users:
								
				if (user_level == 3) // user must verify their email
					{
					if (promo == null) db.send_verification_email(username, email_address, email_ver_key); // standard verification email
					else // user must verify, but we have the promo carrot to dangle in front of them
						{
						String promo_description = promo.getString("description"),
						
						subject = "Claim your " + promo_description + "!",

						message_body = "Welcome to CoinRoster <b>" + username + "</b>!";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += "Please <a href='" + Server.host + "/verify.html?" + email_ver_key + "'>click here</a> to verify your e-mail address and claim your " + promo_description + "!";

						Server.send_mail(email_address, username, subject, message_body);
						}
					}
			
				// log the new user in:
	
				String new_session_token = session.create_session(sql_connection, session, username, new_user_id, user_level);
	
				method.response.new_session_token = new_session_token;
				
				output.put("status", "1");
				}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}