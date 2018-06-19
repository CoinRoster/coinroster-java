package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
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
	public static String method_level = "standard";
	public CreatePromo(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			String 
			
			user_id = session.user_id(),
			description = input.getString("description");
			
			JSONObject 
			
			referrer = null,
			internal_liability = db.select_user("username", "internal_liability"),
			user = db.select_user("id", user_id),
			from_account;
			
			// maybe find a better way to do this down the line
			// p.s.: this used to check if the user was admin but 
			// this entails that admins can't generate user promo codes.
			// Drwawback is that this desc cannot be used in the admin panel
			// (not that it should anyway)
			if (!description.equals("User Generated Promo Code"))
				{
				log("admin promo code creation");
				from_account = db.select_user("username", "internal_promotions");
				}
			else
				{
				log("standard user promo code creation");
				from_account = user;
				}

			log(from_account.toString());
			
//------------------------------------------------------------------------------------
		
			String 
			
			promo_code = no_whitespace(input.getString("promo_code")),
			referrer_id = input.getString("referrer"),			
			
			ext_address = "",
			
			created_by = session.user_id(),
			from_account_id = db.get_id_for_username(from_account.getString("username")),
			internal_liability_id = db.get_id_for_username("internal_liability"),
			from_currency = "BTC",
			to_currency = "BTC";
			
			Long transaction_timestamp = System.currentTimeMillis();
			
			double free_play_amount = input.getDouble("free_play_amount");
			
			int 
			
			rollover_multiple = input.getInt("rollover_multiple"),
			max_use = input.getInt("max_use"),
			pending_flag = 0;
			
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
			
			Double 
			
			btc_balance = from_account.getDouble("btc_balance"),
			btc_liability_balance = internal_liability.getDouble("btc_balance"),
			promotion_amount = multiply(free_play_amount, max_use, 0);

			log("account balance: " + btc_balance);
			
			// check if internal_promotion has enough to cover amount
			if (promotion_amount > btc_balance)
				{
				String error_account = ((from_account.getString("username")).equals("internal_promotions"))?
						"the internal promotions balance":"your account balance";
				output.put("error", "this promotion exceeds " +  error_account);
				break method;
				}
			
			if (referrer_id.equals("") && from_account.getString("username").equals("internal_promotions"))
				{
				referrer_id = null;	
				}
			else if (referrer_id.equals("") )
				{
				referrer = db.select_user("id", from_account_id);
				}
			else
				{
				// created by user; user_id is not passed to frontend via SSI
				referrer = db.select_user("id", db.get_username_for_id(referrer_id));
				

				/*else // valid referrer - only allow one promo code at a time
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
					}*/
				}
			if (referrer == null && !from_account.getString("username").equals("internal_promotions"))
				{
				output.put("error", "Invalid affiliate");
	            break method;
				}
			// -------------------------------------------------------------------------------
			

				
			
		    PreparedStatement internal_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag, ext_address) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
			internal_transaction.setLong(1, transaction_timestamp);
			internal_transaction.setString(2, created_by);
			internal_transaction.setString(3, "BTC-WITHDRAWAL");
			internal_transaction.setString(4, from_account_id);
			internal_transaction.setString(5, internal_liability_id);
			internal_transaction.setDouble(6, promotion_amount);
			internal_transaction.setString(7, from_currency);
			internal_transaction.setString(8, to_currency);
			internal_transaction.setString(9, "Promo Creation");
			internal_transaction.setInt(10, pending_flag);
			internal_transaction.setString(11, ext_address);
			internal_transaction.execute();
		
			Double new_promo_balance = subtract(btc_balance, promotion_amount, 0);	
			db.update_btc_balance(from_account_id, new_promo_balance);
			
			Double new_liability_balance = add(btc_liability_balance, promotion_amount, 0);	
			db.update_btc_balance(internal_liability_id, new_liability_balance);
			
			// -------------------------------------------------------------------------------
			
			
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