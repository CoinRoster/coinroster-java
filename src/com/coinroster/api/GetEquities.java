package com.coinroster.api;

import java.sql.Connection;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetEquities extends Utils{


	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public GetEquities(MethodInstance method) throws Exception 
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
			JSONObject internal_asset = db.select_user("username", "internal_asset");

			output.put("promo_balance", internal_promotions.getDouble("btc_balance"));
			output.put("asset_balance", internal_asset.getDouble("btc_balance"));
			output.put("status", "1");
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
		
}
