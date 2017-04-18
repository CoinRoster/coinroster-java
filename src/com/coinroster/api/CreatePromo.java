package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.Date;

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
			
			int 
			
			rollover_multiple = input.getInt("rollover_multiple"),
			max_use = input.getInt("max_use");
			
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

			if (max_use < 0)
	        	{
	            output.put("error", "Max use cannot be < 0");
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
			
			PreparedStatement create_promo = sql_connection.prepareStatement("insert into promo(created, expires, approved_by, promo_code, description, referrer, free_play_amount, rollover_multiple, max_use) values(?, ?, ?, ?, ?, ?, ?, ?, ?)");
			create_promo.setLong(1, System.currentTimeMillis());
			create_promo.setLong(2, expires);
			create_promo.setString(3, session.user_id());
			create_promo.setString(4, promo_code);
			create_promo.setString(5, description);
			create_promo.setString(6, referrer_id);
			create_promo.setDouble(7, free_play_amount);
			create_promo.setDouble(8, rollover_multiple);
			create_promo.setInt(9, max_use);
			create_promo.executeUpdate();
			
			if (referrer != null)
				{
				// write referral_promo_code to user record
				
				PreparedStatement update_user = sql_connection.prepareStatement("update user set referral_promo_code = ? where id = ?");
				update_user.setString(1, promo_code);
				update_user.setString(2, referrer_id);
				update_user.executeUpdate();
				
				// approve user's open promo_request if applicable
				
				PreparedStatement approve_promo_request = sql_connection.prepareStatement("update promo_request set approved = 1 where approved = 0 and denied = 0 and created_by = ?");
				approve_promo_request.setString(1, referrer_id);
				approve_promo_request.executeUpdate();
				
				String
				
				subject = "You have been approved for an Affiliate promo code", 
				
				message_body = "Hi <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You have been approved for a CoinRoster promo code!";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Promo code: <b>" + promo_code + "</b>";
				message_body += "<br/>";
				message_body += "Free play amount: <b>" + format_btc(free_play_amount) + " BTC</b>";
				message_body += "<br/>";
				message_body += "Playing requirement: <b>" + rollover_multiple + "x</b>";
				
				if (expires > 0)
					{
					String
					
					date = new SimpleDateFormat("dd/MM/yyyy").format(new Date(expires)),
					time = new SimpleDateFormat("HH:mm:ss").format(new Date(expires));
					
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "This code expires on <b>" + date + "</b> at <b>" + time + " EST</b>";
					}

				if (max_use > 0)
					{
					message_body += "<br/>";
					message_body += "<br/>";
					message_body += "This code can be used <b>" + max_use + "</b> times.";
					}
				
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "You will earn <b>" + (int) multiply(referrer.getDouble("referral_offer"), 100, 0) + "%</b> of the rake on any account that uses this promo code once their playing requirement has been met.";
				
				new UserMail(referrer, subject, message_body);
				}
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}