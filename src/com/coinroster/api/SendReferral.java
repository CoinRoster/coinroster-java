package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class SendReferral extends Utils
	{
	public static String method_level = "standard";
	public SendReferral(MethodInstance method) throws Exception 
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
			
			referrer_provided_name = input.getString("referrer"),
			referrer_id = session.user_id(),

			referral_key = Server.generate_key(referrer_id),

			// should do a referral program lookup else break method
			
			email_contact_name = "Referral",
			email_address = to_valid_email(input.getString("email_address")),
			
			referral_code = input.getString("referral_code");

			if (referral_code.equals("")) referral_code = null;
			else if (db.select_promo(referral_code) == null)
				{
				output.put("error", "Invalid affiliate code");
				break method;
				}

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
			
			JSONObject referrer = db.select_user("id", referrer_id);
			
			String referrer_username = referrer.getString("username");
						
			double 
			
			referral_offer = referrer.getDouble("referral_offer"),
			referral_program = referral_offer; // referring user's referral_offer becomes the referral_program of the referred user
			
			PreparedStatement create_referral = sql_connection.prepareStatement("insert into referral(referral_key, referrer_id, referrer_username, email_address, referral_program, created, promo_code) values(?, ?, ?, ?, ?, ?, ?)");				
			create_referral.setString(1, referral_key);
			create_referral.setString(2, referrer_id);
			create_referral.setString(3, referrer_username);
			create_referral.setString(4, email_address);
			create_referral.setDouble(5, referral_program);
			create_referral.setLong(6, System.currentTimeMillis());
			create_referral.setString(7, referral_code);
			create_referral.executeUpdate();
			
			String
			
			subject = "",
			
			message_body = "You have been invited by <b>" + referrer_provided_name + "</b> to join CoinRoster - the leading Bitcoin Fantasy Sports community!";
			message_body += "<br/>";
			message_body += "<br/>";
			
			if (referral_code == null)
				{
				subject = "CoinRoster invitation from " + referrer_provided_name + "!";
				message_body += "<a href='" + Server.host + "/signup.html?" + referral_key + "'>Click here</a> to create an account.";
				}
			else // referrer has a promo code
				{
				JSONObject promo = db.select_promo(referral_code);
				
				String promo_description = promo.getString("description");
				
				subject = "Claim your " + promo_description + " on CoinRoster!";
				message_body += "<a href='" + Server.host + "/signup.html?" + referral_key + "'>Click here</a> to claim your <b>" + promo_description + "</b>";
				}

			Server.send_mail(email_address, email_contact_name, subject, message_body);

			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}