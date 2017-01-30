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

public class CategoryReport extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public CategoryReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			JSONArray category_report = new JSONArray();
			
			PreparedStatement select_categories = sql_connection.prepareStatement("select * from category");
			ResultSet category_rs = select_categories.executeQuery();

			while (category_rs.next())
				{
				String category_code = category_rs.getString(1);
				String category_description = category_rs.getString(2);
				int category_active_flag = category_rs.getInt(3);
				
				JSONObject category = new JSONObject();
				
				category.put("code", category_code);
				category.put("description", category_description);
				category.put("active_flag", category_active_flag);
				
				PreparedStatement select_sub_categories = sql_connection.prepareStatement("select * from sub_category where category = ?");
				select_sub_categories.setString(1, category_code);
				ResultSet sub_category_rs = select_sub_categories.executeQuery();
				
				JSONArray sub_categories = new JSONArray();
				
				while (sub_category_rs.next())
					{
					String sub_category_code = sub_category_rs.getString(2);
					String sub_category_description = sub_category_rs.getString(3);
					int sub_category_active_flag = category_rs.getInt(4);
					String sub_category_tile_image = sub_category_rs.getString(5);
					
					JSONObject sub_category = new JSONObject();
					
					sub_category.put("code", sub_category_code);
					sub_category.put("description", sub_category_description);
					sub_category.put("active_flag", sub_category_active_flag);
					sub_category.put("tile_image", sub_category_tile_image);
					
					sub_categories.put(sub_category);
					}
				
				category.put("sub_categories", sub_categories);
				category_report.put(category);
				}
			
			output.put("category_report", category_report);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}