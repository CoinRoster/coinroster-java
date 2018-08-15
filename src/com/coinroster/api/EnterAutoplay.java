package com.coinroster.api;

import java.sql.Connection;
import java.util.Date;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class EnterAutoplay extends Utils {
	public static String method_level = "standard";
	
	public EnterAutoplay(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
			String msg = null;
			SimpleDateFormat input_pattern =  new SimpleDateFormat("MM/dd/yyyy");
			SimpleDateFormat output_pattern = new SimpleDateFormat("yyyy-MM-dd");
			
			switch(input.getString("process")){
				
				case "REMOVE":
					PreparedStatement delete_autoplay = sql_connection.prepareStatement("delete from autoplay where id = ?");
					delete_autoplay.setInt(1, input.getInt("autoplay_id"));
					msg = "Your autoplay entry has been deleted.";
					break;
				
					
				case "DEACTIVATE":
					PreparedStatement deactivate = sql_connection.prepareStatement("update autoplay set active = 0 where id = ?");
					deactivate.setInt(1, input.getInt("autoplay_id"));
					msg = "Your autoplay entry has been deactivated.";
					break;
				
					
				case "MODIFY":
					
					//convert the dates coming from the JSON into Date object for mysql db
					String start_date = input.getString("start_date");
					String end_date = input.getString("end_date");
					Date start = input_pattern.parse(start_date);
					Date end = input_pattern.parse(end_date);	
					String mysql_start = output_pattern.format(start);
					String mysql_end = output_pattern.format(end);
					String algorithm = input.getString("algorithm");
					int num_rosters = input.getInt("num_rosters");
					
					PreparedStatement modify = sql_connection.prepareStatement("update autoplay set start_date = ?, end_date = ?, algorithm = ?, num_rosters = ? where id = ?");
					modify.setString(1, mysql_start);
					modify.setString(2, mysql_end);
					modify.setString(3, algorithm);
					modify.setInt(4, num_rosters);
					modify.setInt(5, input.getInt("autoplay_id"));
					modify.executeUpdate();
					
					msg = "Your autoplay entry has been modified.";
					break;
				
					
				case "INSERT":
					
					String user_id = session.user_id();
					int contest_template_id = input.getInt("contest_id");
					
					//convert the dates coming from the JSON into Date object for mysql db
					
					start_date = input.getString("start_date");
					end_date = input.getString("end_date");
					start = input_pattern.parse(start_date);
					end = input_pattern.parse(end_date);	
					mysql_start = output_pattern.format(start);
					mysql_end = output_pattern.format(end);
					
					algorithm = input.getString("algorithm");
					num_rosters = input.getInt("num_rosters");
					
					PreparedStatement enter_autoplay = sql_connection.prepareStatement("insert into autoplay(user_id, contest_template_id, start_date, end_date, algorithm, num_rosters, active) "
																						+ "values (?, ?, ?, ?, ?, ?, ?)");
				
					enter_autoplay.setString(1, user_id);
					enter_autoplay.setInt(2, contest_template_id);
					enter_autoplay.setString(3, mysql_start);
					enter_autoplay.setString(4, mysql_end);
					enter_autoplay.setString(5, algorithm);
					enter_autoplay.setInt(6, num_rosters);
					enter_autoplay.setInt(7, 1);
					enter_autoplay.executeUpdate();
					
					msg = "Your autoplay entry has been entered.";
					break;
					
				default:
					output.put("status", "0");
					msg = "unexpected process value";
					output.put("error", msg);
					break method;
					
			}
			
			output.put("msg", msg);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}