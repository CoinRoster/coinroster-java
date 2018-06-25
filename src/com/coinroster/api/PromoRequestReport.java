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

public class PromoRequestReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public PromoRequestReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
	
			JSONArray promo_request_report = new JSONArray();
			
			// select all "open" promo requests (not yet approved, not yet denied)
			
			PreparedStatement select_promo_requests = sql_connection.prepareStatement("select id from promo_request where approved = 0 and denied = 0");
			ResultSet result_set = select_promo_requests.executeQuery();

			while (result_set.next())
				{
				int request_id = result_set.getInt(1);
				
				JSONObject promo_request = db.select_promo_request(request_id);
				
				String 
				
				created_by = promo_request.getString("created_by"),
				created_by_username = db.get_username_for_id(created_by);
				promo_request.put("created_by_username", created_by_username);

				promo_request_report.put(promo_request);
				}
			
			output.put("promo_request_report", promo_request_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}