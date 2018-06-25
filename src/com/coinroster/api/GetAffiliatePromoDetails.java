package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
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
			
			PreparedStatement select_promo_ids  = sql_connection.prepareStatement("select * from promo where referrer = ? and cancelled = 0");
			select_promo_ids.setString(1, user_id);
			ResultSet result_set = select_promo_ids.executeQuery();
			
			JSONArray promos = new JSONArray();

			while (result_set.next()) 
				{
				int id = result_set.getInt(1);
				long expires = result_set.getLong(3);
				String promo_code = result_set.getString(7);
				String description = result_set.getString(8);
				double free_play_amount = result_set.getDouble(10);
				int rollover_multiple = result_set.getInt(11);
				int max_use = result_set.getInt(13);
				int times_used = result_set.getInt(14);
				
				JSONObject promo = new JSONObject();
				
				promo.put("id", id);
				promo.put("expires", expires);
				promo.put("promo_code", promo_code);
				promo.put("description", description);
				promo.put("free_play_amount", free_play_amount);
				promo.put("rollover_multiple", rollover_multiple);
				promo.put("max_use", max_use);
				promo.put("times_used", times_used);
				
				promos.put(promo);
				}

			output.put("promos", promos);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}