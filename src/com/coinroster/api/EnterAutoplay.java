package com.coinroster.api;

import java.sql.Connection;
import java.util.Date;
import java.sql.PreparedStatement;
import java.text.SimpleDateFormat;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

/**
 * Enter an autoplay entry with a chosen player selection algorithm.
 * 
 * @custom.access standard
 *
 */
public class EnterAutoplay extends Utils {
	
	public static String method_level = "standard";
	
	/**
	 * Enter an autoplay entry with a chosen player selection algorithm.
	 * 
	 * @param method.input.data Contains the actual input variables
	 * @param method.input.data.process Which autoplay operation is to be performed
	 * @param method.input.data.autoplay_id For operations on existing autoplays
	 * @param method.input.data.start_date Date to start the autoplay contests
	 * @param method.input.data.end_date Date to stop the autoplay contests
	 * @param method.input.data.algorithm Which player selection algorithm to use
	 * @param method.input.data.num_rosters Maximum number of rosters to be placed in autoplay
	 * @param method.input.data.active Set the autoplay active
	 * @param method.input.data.contest_id New contest to add to autoplay
	 * @throws Exception
	 */
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
			
			input = input.getJSONObject("data");
			
			switch(input.getString("process")){
				
				case "REMOVE":
					PreparedStatement delete_autoplay = sql_connection.prepareStatement("delete from autoplay where id = ?");
					delete_autoplay.setInt(1, input.getInt("autoplay_id"));
					delete_autoplay.executeUpdate();
					msg = "Your autoplay entry has been deleted.";
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
					int active = 0;
					if(input.getBoolean("active"))
						active = 1;
					
					PreparedStatement modify = sql_connection.prepareStatement("update autoplay set start_date = ?, end_date = ?, algorithm = ?, num_rosters = ?, active = ? where id = ?");
					modify.setString(1, mysql_start);
					modify.setString(2, mysql_end);
					modify.setString(3, algorithm);
					modify.setInt(4, num_rosters);
					modify.setInt(5, active);
					modify.setInt(6, input.getInt("autoplay_id"));
					
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