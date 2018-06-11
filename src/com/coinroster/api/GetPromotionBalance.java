package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;

public class GetPromotionBalance {

	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetPromotionBalance(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
			JSONObject internal_promotions = db.select_user("username", "internal_promotions");
			output.put("internal_promo_balance", internal_promotions.getDouble("btc_balance"));
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
		
}
