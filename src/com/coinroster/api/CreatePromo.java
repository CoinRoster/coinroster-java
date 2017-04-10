package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

public class CreatePromo extends Utils
	{
	public static String method_level = "admin";
	public CreatePromo(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String 
			
			promo_code = no_whitespace(input.getString("promo_code")),
			description = input.getString("description"),
			referrer_id = input.getString("referrer");
			
			double free_play_amount = input.getDouble("free_play_amount");
			
			int rollover_multiple = input.getInt("rollover_multiple");
			
			Long expires = input.getLong("expires");
			
			if (promo_code.equals(""))
				{
				output.put("error", "No promo code supplied.");
				break method;
				}
			if (promo_code.length() > 20)
				{
				output.put("error", "Promo code is too long.");
				break method;
				}
			if (db.select_promo(promo_code) != null) 
				{
				output.put("error", "Promo code [" + promo_code + "] already exists");
				break method;
				}
			
			if (description.equals(""))
				{
				output.put("error", "No description supplied.");
				break method;
				}
			if (description.length() > 50)
				{
				output.put("error", "Promo code is too long.");
				break method;
				}
			
			if (free_play_amount <= 0)
	        	{
	            output.put("error", "Free play amount cannot be <= 0");
	            break method;
	        	}
			
			if (rollover_multiple <= 0)
	        	{
	            output.put("error", "Rollover multiple cannot be <= 0");
	            break method;
	        	}
			
			JSONObject referrer = null;
			
			if (referrer_id.equals("")) referrer_id = null;
			else
				{
				referrer = db.select_user("id", referrer_id);
				if (referrer == null)
					{
					output.put("error", "Invalid affiliate");
		            break method;
					}
				else // valid referrer - only allow one promo code at a time
					{
					PreparedStatement check_for_promo  = sql_connection.prepareStatement("select promo_code from promo where referrer = ? and cancelled = 0");
					check_for_promo.setString(1, referrer_id);
					ResultSet result_set = check_for_promo.executeQuery();

					if (result_set.next())
						{
						String existing_promo_code = result_set.getString(1);
						output.put("error", "Affiliate already has an active promo code: " + existing_promo_code);
			            break method;
						}
					}
				}
			
			PreparedStatement create_promo = sql_connection.prepareStatement("insert into promo(created, expires, approved_by, promo_code, description, referrer, free_play_amount, rollover_multiple) values(?, ?, ?, ?, ?, ?, ?, ?)");
			create_promo.setLong(1, System.currentTimeMillis());
			create_promo.setLong(2, expires);
			create_promo.setString(3, session.user_id());
			create_promo.setString(4, promo_code);
			create_promo.setString(5, description);
			create_promo.setString(6, referrer_id);
			create_promo.setDouble(7, free_play_amount);
			create_promo.setDouble(8, rollover_multiple);
			create_promo.executeUpdate();
			
			if (referrer != null)
				{
				PreparedStatement update_user = sql_connection.prepareStatement("update user set referral_promo_code = ? where id = ?");
				update_user.setString(1, promo_code);
				update_user.setString(2, referrer_id);
				update_user.executeUpdate();
				
				String
				
				subject = "You have been approved for a CoinRoster promo code", 
				message_body = "";
				
				message_body += "Hello <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You have been approved for a CoinRoster promo code!";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Promo code: <b>" + promo_code + "</b>";
				message_body += "<br/>";
				message_body += "Free play amount: <b>" + format_btc(free_play_amount) + " BTC</b>";
				message_body += "<br/>";
				message_body += "Rollover requirement: <b>" + rollover_multiple + "x</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You will earn <b>" + (int) multiply(referrer.getDouble("referral_offer"), 100, 0) + "%</b> of the rake on any account that uses this promo code.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Please do not reply to this email.";
				
				new UserMail(referrer, subject, message_body);
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}