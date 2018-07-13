package com.coinroster.api;

import java.sql.Connection;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.bots.BaseballBot;

public class SetupPropBet extends Utils{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public SetupPropBet(MethodInstance method) throws Exception {
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		DB db = new DB(sql_connection);

		method : {	
//------------------------------------------------------------------------------------
			try{
				
				String category = input.getString("category");
				String sub_category = input.getString("sub_category");
				String sport = sub_category.replace("PROPS", "");
				Long deadline = null;
				switch(sport){
					case "BASEBALL":
						BaseballBot baseball_bot = new BaseballBot(sql_connection);
						String gameID_array = baseball_bot.scrapeGameIDs();
						deadline = baseball_bot.getEarliestGame();
						break;
					case "GOLF":
						break;
					case "BASKETBALL":
						break;
					default:
						break;
				}
	            LocalDate date = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate();
				String contest_type = input.getString("contest_type");
				Long registration_deadline = deadline;
				String settlement_type = input.getString("settlement_type");
				double cost_per_entry = input.getDouble("cost_per_entry");
				double rake = 5.0;
				String progressive_code = "";
				JSONObject scoring_rules = input.getJSONObject("scoring_rules");
				JSONObject prop_data = input.getJSONObject("prop_data");
				String prop_type = prop_data.getString("prop_type");
				String title, description;
				JSONArray option_table = new JSONArray();
				
				//check prop_type and set option_table, title, description accordingly
				switch(prop_type){
					case "MATCH_PLAY":
						
						JSONObject tie = new JSONObject();
						tie.put("id", 1);
						tie.put("description", "Tie");
						option_table.put(tie);
						int index = 2;
						JSONArray players = prop_data.getJSONArray("players");
						for(int i=0; i < players.length(); i++){
							int player_id = players.getInt(i);
							String name = db.get_player_info(sport, player_id);
							JSONObject p = new JSONObject();
							p.put("description", name);
							p.put("id", index);
							p.put("player_id", player_id);
							option_table.put(p);
							index += 1;
						}
						
						//one stat game
						if(scoring_rules.length() == 1){
							String key = (String) scoring_rules.keys().next();
							title = sport + " Match Play: Who will get more " + key + "? | " + date.toString();
							description = "Place your bets on which player will get more " + key + "?";
						}
						//multi-stat game
						else{
							title = sport + " Match Play: Who will get more fantasy points? | " + date.toString();
							Iterator<?> keys = scoring_rules.keys();
							description = "Fantasy Scoring:<br>";
							while(keys.hasNext()){
								String key = (String) keys.next();
								String multiplier = String.valueOf(scoring_rules.getInt(key));
								description += key.toUpperCase() + ": " + " " + multiplier + "<br>";
							} 
						}
						break;
						
					case "OVER_UNDER":
						
						JSONObject over = new JSONObject();
						over.put("description", "Over " + String.valueOf(prop_data.getDouble("over_under_value")));
						over.put("id", 1);
						option_table.put(over);
						JSONObject under = new JSONObject();
						under.put("description", "Under " + String.valueOf(prop_data.getDouble("over_under_value")));
						under.put("id", 2);
						option_table.put(under);
						String name = db.get_player_info(sport, prop_data.getInt("player_id"));
						
						
						if(scoring_rules.length() == 1){
							String key = (String) scoring_rules.keys().next();
							title = sport + " " + date.toString() + " | " + name + " over/under " + String.valueOf(prop_data.getDouble("over_under_value")) + " " + key;
							description = "Place your bets on if " + name + " will get over/under "+ String.valueOf(prop_data.getDouble("over_under_value")) + " " + key + "?";
						}
						//multi-stat game
						else{
							title = sport + " " + date.toString() + " | " + name + " over/under " + String.valueOf(prop_data.getDouble("over_under_value")) + " Fantasy Points";							Iterator<?> keys = scoring_rules.keys();
							description = "Fantasy Scoring:<br>";
							while(keys.hasNext()){
								String key = (String) keys.next();
								String multiplier = String.valueOf(scoring_rules.getInt(key));
								description += key.toUpperCase() + ": " + " " + multiplier + "<br>";
							} 
						}
						break;
						
					default:
						log("Could not find prop bet with type = " + prop_type);
						output.put("error", "Could not find prop bet with type = " + prop_type);
						break;
				
				}
				
			}catch(Exception e){
				log(e.toString());
				output.put("error", e.toString());
				output.put("status", "0");
			}
//------------------------------------------------------------------------------------
		}
		method.response.send(output);
	}
}