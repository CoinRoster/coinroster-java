package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class IncludePrivateContests extends Utils
	{
	public static String method_level = "guest";
	public IncludePrivateContests(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		Session session = method.session;
		Connection sql_connection = method.sql_connection;
		
		method : {
		
//------------------------------------------------------------------------------------
			
			JSONArray active_subs = input.getJSONArray("active_subs");
			JSONObject counts = new JSONObject();
			
			for(int i = 0; i < active_subs.length(); i++){
				
				String sub_category_code = active_subs.getString(i).toUpperCase();
				Utils.log("checking sub: " + sub_category_code);
				PreparedStatement count_contests = sql_connection.prepareStatement("select status, participants from contest "
																				 + "where sub_category = ? and (status = 1 or status = 2)");
				count_contests.setString(1, sub_category_code);
				ResultSet rs = count_contests.executeQuery();

				int 
				open_contests = 0,
				in_play_contests = 0;
				
				JSONObject sub_cat = new JSONObject();

				sub_cat.put("open", 0);
				sub_cat.put("in_play", 0);
				
				try{
					while (rs.next()){
						
						int contest_status = rs.getInt(1);
						
						// look for participants column, if public count it, if private check if user is able to view it
						boolean count_it = false;
						String participants = rs.getString(2);
						
						if(rs.wasNull())
							count_it = true;
						else{
							if(session.active()){
								JSONObject participants_json = new JSONObject(participants);
								JSONArray users = participants_json.getJSONArray("users");
								for(int index = 0; index < users.length(); index++){
									String user_id = users.getString(index);
									// user is allowed to see private contest
									if(user_id.equals(session.user_id())){
										count_it = true;
										break;
									}
								}
							}
						}
						
						if(count_it){
														
							switch (contest_status)
			                {
			                case 1: // Reg open
			                	open_contests++;
			                    break;
			                case 2: // In play
			                	in_play_contests++;
			                    break;
			                }
						}
					}
				}catch(Exception e){
					Server.exception(e);
				}
				
				Utils.log(sub_category_code +  ": " + open_contests + " open");
				Utils.log(sub_category_code +  ": " + in_play_contests + " in play");
				
				sub_cat.put("open", open_contests);
				sub_cat.put("in_play", in_play_contests);
				counts.put(sub_category_code, sub_cat);
			}
			
			output.put("counts", counts);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------
			} 
	
		if(session != null)
			method.response.send(output);
		
		}
	}