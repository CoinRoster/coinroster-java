package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class GetUserAutoplays extends Utils {
	public static String method_level = "standard";
	
	public GetUserAutoplays(MethodInstance method) throws Exception 
		{
		JSONObject output = method.output;
		
		Session session = method.session;
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
			ResultSet rs = null;
			JSONArray autoplays = new JSONArray();
			PreparedStatement get_autoplays = sql_connection.prepareStatement("select * from autoplay where user_id = ?");
			get_autoplays.setString(1, session.user_id());
			rs = get_autoplays.executeQuery();
			
			while(rs.next()){
				
				JSONObject entry = new JSONObject();
				
				int id = rs.getInt(1);
				int contest_template_id = rs.getInt(3);
				ResultSet contest_template = null;
				PreparedStatement get_contest_data = sql_connection.prepareStatement("select sub_category, title, description, settlement_type, pay_table, cost_per_entry from contest_template where id = ?");
				get_contest_data.setInt(1, contest_template_id);
				contest_template = get_contest_data.executeQuery();
				String sport, title, description, settlement_type, pay_table;
				double cost_per_entry;
				if(contest_template.next()){
					sport = contest_template.getString(1);
					title = contest_template.getString(2);
					description = contest_template.getString(3);
					settlement_type = contest_template.getString(4);
					pay_table = contest_template.getString(5);
					if(contest_template.wasNull()){
						pay_table = null;
					}
					cost_per_entry = contest_template.getDouble(6);
				}
				else{
					output.put("error", "could not find data from contest_template_table with id = " + contest_template_id);
					output.put("status", "0");
					break method;
				}
				String start_date = rs.getDate(4).toString();
				String end_date = rs.getDate(5).toString();
				String algorithm = rs.getString(6);
				int num_rosters = rs.getInt(7);
				boolean active;
				if(rs.getInt(8) == 1) active = true;
				else active = false;
				
				entry.put("id", id);
				entry.put("sport", sport);
				entry.put("contest_title", title);
				entry.put("contest_desc", description);
				entry.put("settlement_type", settlement_type);
				entry.put("pay_table", pay_table);
				entry.put("cost_per_entry", cost_per_entry);
				entry.put("start_date", start_date);
				entry.put("end_date", end_date);
				entry.put("algorithm", algorithm);
				entry.put("num_rosters", num_rosters);
				entry.put("active", active);
				
				autoplays.put(entry);
				
			}
			
			output.put("entries", autoplays);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}
