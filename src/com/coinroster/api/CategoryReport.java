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
	public static String method_level = "guest";
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
			
			// !! SECURITY !! this method can be called by any user (guest through to admin)
			
			// if not admin, all inactive categories and sub_categories are hidden
			
			// admin_panel is also a criterion for admin report so that admin users can experience the normal report from their account panes
			
			String request_source = input.getString("request_source");
			
			boolean is_admin = session.user_level() != null && session.user_level().equals("1") && request_source.equals("admin_panel");
			
			JSONArray category_report = new JSONArray();
			
			PreparedStatement select_categories = sql_connection.prepareStatement("select * from category");
			ResultSet category_rs = select_categories.executeQuery();

			while (category_rs.next())
				{
				String category_code = category_rs.getString(1);
				String category_description = category_rs.getString(2);
				int position = category_rs.getInt(3);
				
				JSONObject category = new JSONObject();
				
				category.put("code", category_code);
				category.put("description", category_description);
				category.put("position", position);
				
				PreparedStatement select_sub_categories = sql_connection.prepareStatement("select * from sub_category where category = ? order by created asc");
				select_sub_categories.setString(1, category_code);
				ResultSet sub_category_rs = select_sub_categories.executeQuery();
				
				JSONArray sub_categories = new JSONArray();
				
				boolean category_active = false;
				
				while (sub_category_rs.next())
					{
					String sub_category_id = sub_category_rs.getString(1);
					// String category = sub_category_rs.getString(2);
					String sub_category_code = sub_category_rs.getString(3);
					String sub_category_description = sub_category_rs.getString(4);
					int active_flag = sub_category_rs.getInt(5);
					String image_name = sub_category_rs.getString(6);
					Long created = sub_category_rs.getLong(7);
					
					if (!is_admin && active_flag == 0) continue;
					
					PreparedStatement count_contests = sql_connection.prepareStatement("select status, count(*) from contest where category = ? and sub_category = ? group by status");
					count_contests.setString(1, category_code);
					count_contests.setString(2, sub_category_code);
					ResultSet count_contests_rs = count_contests.executeQuery();

					int 
					
					open_contests = 0,
					in_play_contests = 0;
					
					while (count_contests_rs.next())
						{
						int 
						
						contest_status = count_contests_rs.getInt(1),
						number_of_contests = count_contests_rs.getInt(2);
						
						switch (contest_status)
			                {
			                case 1: // Reg open
			                	open_contests = number_of_contests;
			                    break;
			                case 2: // In play
			                	in_play_contests = number_of_contests;
			                    break;
			                }
						}
					
					
					JSONObject sub_category = new JSONObject();

					sub_category.put("id", sub_category_id);
					sub_category.put("code", sub_category_code);
					sub_category.put("description", sub_category_description);
					sub_category.put("active_flag", active_flag);
					sub_category.put("open_contests", open_contests);
					sub_category.put("in_play_contests", in_play_contests);
					sub_category.put("image_name", image_name);
					sub_category.put("created", created);
					
					sub_categories.put(sub_category);
					
					category_active = true;
					}
				
				if (!is_admin && !category_active) continue;
				
				category.put("sub_categories", sub_categories);
				category_report.put(category);
				}
			
			output.put("category_report", category_report);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}