package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetPoolDetails extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetPoolDetails(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			// method logic goes here
			
			int pool_id = input.getInt("pool_id");
			
			JSONObject pool = db.select_pool(pool_id);
			
			if (pool != null)
				{
				output.put("category", pool.get("category"));
				output.put("sub_category", pool.get("sub_category"));
				output.put("title", pool.get("title"));
				output.put("description", pool.get("description"));
				output.put("settlement_type", pool.get("settlement_type"));
				output.put("pay_table", pool.get("pay_table"));
				output.put("odds_table", pool.get("odds_table"));
				output.put("salary_cap", pool.get("salary_cap"));
				output.put("cost_per_entry", pool.get("cost_per_entry"));
				output.put("min_users", pool.get("min_users"));
				output.put("max_users", pool.get("max_users"));
				output.put("entries_per_user", pool.get("entries_per_user"));
				output.put("pool_status", pool.get("status"));
				output.put("roster_size", pool.get("roster_size"));
				
				output.put("status", "1");
				}
			else output.put("error", "Invalid pool ID");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}