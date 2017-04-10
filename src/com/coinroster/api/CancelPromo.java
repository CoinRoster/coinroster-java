package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UserMail;

public class CancelPromo extends Utils
	{
	public static String method_level = "admin";
	public CancelPromo(MethodInstance method) throws Exception 
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
			
			promo_code = input.getString("promo_code"),
			reason = input.getString("reason");
			
			JSONObject promo = db.select_promo(promo_code);
			
			if (reason.equals(""))
				{
				output.put("error", "No reason provided");
	            break method;
				}
			if (reason.length() > 200)
				{
				output.put("error", "Reason is too long");
	            break method;
				}
			
			if (promo == null)
				{
				output.put("error", "Invalid promo code");
	            break method;
				}
			
			if (promo.getInt("cancelled") == 1)
				{
				output.put("error", "Promo has already been cancelled");
	            break method;
				}
			
			if (!promo.isNull("referrer"))
				{
				String referrer_id = promo.getString("referrer");
				
				JSONObject referrer = db.select_user("id", referrer_id);
				
				PreparedStatement update_user = sql_connection.prepareStatement("update user set referral_promo_code = ? where id = ?");
				update_user.setString(1, null);
				update_user.setString(2, referrer_id);
				update_user.executeUpdate();
				
				String subject = "You promo code " + promo_code + " has been deactivated",
				
				message_body = "Hello <b><!--USERNAME--></b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Your promo code " + promo_code + " has been deactivated.";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "Reason: <b>" + reason + "</b>";
				message_body += "<br/>";
				message_body += "<br/>";
				message_body += "If this is unexpected or you disagree with this action, please reply to this email";
				
				new UserMail(referrer, subject, message_body);
				}
			
			PreparedStatement update_promo = sql_connection.prepareStatement("update promo set cancelled = 1, cancelled_by = ?, cancelled_reason = ? where promo_code = ?");
			update_promo.setString(1, session.user_id());
			update_promo.setString(2, reason);
			update_promo.setString(3, promo_code);
			update_promo.executeUpdate();
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}