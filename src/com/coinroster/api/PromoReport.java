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

public class PromoReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public PromoReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			String active_cancelled = input.getString("active_cancelled");
			
			JSONArray promo_report = new JSONArray();
			
			PreparedStatement select_all_promos = null;
			
			if (active_cancelled.equals("active")) select_all_promos = sql_connection.prepareStatement("select promo_code from promo where cancelled = 0");
			else if (active_cancelled.equals("cancelled")) select_all_promos = sql_connection.prepareStatement("select promo_code from promo where cancelled > 0");
			
			ResultSet result_set = select_all_promos.executeQuery();

			while (result_set.next())
				{
				String promo_code = result_set.getString(1);

				JSONObject promo = db.select_promo(promo_code);
				
				String approved_by = promo.getString("approved_by");
				approved_by = db.get_username_for_id(approved_by);
				promo.put("approved_by", approved_by);

				if (!promo.isNull("referrer"))
					{
					String referrer = promo.getString("referrer");
					referrer = db.get_username_for_id(referrer);
					promo.put("referrer", referrer);
					}
				else promo.put("referrer", "");

				if (!promo.isNull("cancelled_by"))
					{
					String cancelled_by = promo.getString("cancelled_by");
					cancelled_by = db.get_username_for_id(cancelled_by);
					promo.put("cancelled_by", cancelled_by);
					}
				else promo.put("cancelled_by", "");
				
				promo_report.put(promo);
				}
			
			output.put("promo_report", promo_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}