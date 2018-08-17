package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Utils;

public class GetApplicableContests_Autoplay extends Utils {
	public static String method_level = "standard";
	
	public GetApplicableContests_Autoplay(MethodInstance method) throws Exception 
		{
		JSONObject output = method.output;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
			try{
			
				ResultSet rs = null;
				JSONArray autoplays = new JSONArray();
				
				PreparedStatement get_contest_data = sql_connection.prepareStatement("select sub_category, title, description, settlement_type, pay_table, cost_per_entry, id "
																				   + "from contest_template where ((sub_category = ? and prop_data LIKE ?) "
																				   + "or sub_category != ?) "
																				   + "and active = 1 and roster_size != 0");
				get_contest_data.setString(1, "GOLF");
				get_contest_data.setString(2, "%tournament%");
				get_contest_data.setString(3, "GOLF");
				
				rs = get_contest_data.executeQuery();
				
				while(rs.next()){
					
					String sport = rs.getString(1);
					String title = rs.getString(2);
					String description = rs.getString(3);
					String settlement_type = rs.getString(4);
					String pay_table = rs.getString(5);
					if(rs.wasNull()){
						pay_table = null;
					}
					double cost_per_entry = rs.getDouble(6);
					int id = rs.getInt(7);
					
					JSONObject entry = new JSONObject();
					entry.put("id", id);
					entry.put("sport", sport);
					entry.put("contest_title", title);
					entry.put("contest_desc", description);
					entry.put("settlement_type", settlement_type);
					entry.put("pay_table", pay_table);
					entry.put("cost_per_entry", cost_per_entry);
	
					autoplays.put(entry);
						
				}
				
				output.put("contests", autoplays);
				output.put("status", "1");
			}
			catch(Exception e){
				Server.exception(e);
				output.put("error", e.getMessage());
				output.put("status", "0");
				break method;
			}
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}

