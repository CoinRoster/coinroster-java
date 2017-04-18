package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetAffiliatePromoDetails extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetAffiliatePromoDetails(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String user_id = session.user_id();
			
			JSONObject user = db.select_user("id", user_id);
			
			if (!user.isNull("referral_promo_code"))
				{
				String referral_promo_code = user.getString("referral_promo_code");
				
				JSONObject promo = db.select_promo(referral_promo_code);

				output.put("max_use", promo.get("max_use"));
				output.put("times_used", promo.get("times_used"));
				output.put("expires", promo.get("expires"));
				output.put("promo_code", referral_promo_code);
				output.put("description", promo.get("description"));
				output.put("free_play_amount", promo.get("free_play_amount"));
				output.put("rollover_multiple", promo.get("rollover_multiple"));
				
				output.put("status", "1");
				}
			else output.put("error", "You have not been approved for an affiliate promo code.");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}