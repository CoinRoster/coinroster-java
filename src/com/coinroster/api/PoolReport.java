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

public class PoolReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public PoolReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray pool_report = new JSONArray();
			
			PreparedStatement select_all_users = sql_connection.prepareStatement("select * from pool order by id desc");
			ResultSet result_set = select_all_users.executeQuery();

			while (result_set.next())
				{
				int id = result_set.getInt(1);
				Long created = result_set.getLong(2);
				String created_by = result_set.getString(3);
				String category = result_set.getString(4);
				String sub_category = result_set.getString(5);
				String title = result_set.getString(6);
				String description = result_set.getString(7);
				String settlement_type = result_set.getString(8);
				String pay_table = result_set.getString(9);
				String odds_table = result_set.getString(10);
				double rake = result_set.getDouble(11);
				double salary_cap = result_set.getDouble(12);
				double cost_per_entry = result_set.getDouble(13);
				int min_users = result_set.getInt(14);
				int max_users = result_set.getInt(15);
				int entries_per_user = result_set.getInt(16);
				Long registration_deadline = result_set.getLong(17);
				int status = result_set.getInt(18);
				int roster_size = result_set.getInt(19);
				
				created_by = db.get_username_for_id(created_by);
				
				JSONObject pool = new JSONObject();

				pool.put("id", id);
				pool.put("created", created);
				pool.put("created_by", created_by);
				pool.put("category", category);
				pool.put("sub_category", sub_category);
				pool.put("title", title);
				pool.put("description", description);
				pool.put("settlement_type", settlement_type);
				pool.put("pay_table", pay_table);
				pool.put("odds_table", odds_table);
				pool.put("rake", rake);
				pool.put("salary_cap", salary_cap);
				pool.put("cost_per_entry", cost_per_entry);
				pool.put("min_users", min_users);
				pool.put("max_users", max_users);
				pool.put("entries_per_user", entries_per_user);
				pool.put("registration_deadline", registration_deadline);
				pool.put("roster_size", roster_size);
				pool.put("status", status);
				
				pool_report.put(pool);
				}
			
			output.put("pool_report", pool_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}