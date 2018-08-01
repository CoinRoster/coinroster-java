package com.coinroster.api;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.coinroster.ContestMethods;
import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.bots.BaseballBot;
import com.coinroster.bots.BasketballBot;
import com.coinroster.bots.GolfBot;

public class SetupPropBet extends Utils{
	public static String method_level = "standard";

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
				
				JSONObject data = input.getJSONObject("data");
				
				String category = "FANTASYSPORTS";
				String contest_type = "PARI-MUTUEL";
				String settlement_type = "PARI-MUTUEL";
				String sub_category = data.getString("sub_category");
				String sport = sub_category.replace("PROPS", "");
				boolean priv = data.getBoolean("private");
				double cost_per_entry = 0.00001;
				double rake = 5.0;
				String progressive = "";
				JSONObject scoring_rules, prop_data;
				String gameIDs = "";
				String sport_title = "", contest_title = "", date_name_title = "";
				String desc = "Be careful not to include injured or inactive players<br>";
				try{
					scoring_rules = data.getJSONObject("scoring_rules");
				}catch(JSONException e){
					scoring_rules = new JSONObject();
				}
				
				try{
					prop_data = data.getJSONObject("prop_data");
				}catch(JSONException e){
					prop_data = new JSONObject();
				}
				
				Long deadline = null;
				switch(sport){
					
					case "BASEBALL":
						BaseballBot baseball_bot = new BaseballBot(sql_connection);
						baseball_bot.scrapeGameIDs();
						if(baseball_bot.getGameIDs() != null){
							deadline = baseball_bot.getEarliestGame();
							gameIDs = baseball_bot.getGameIDs().toString();
							date_name_title = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate().toString();
							
						}else{
							output.put("error", "Baseball contests are not currently available");
							break method;
						}
						break;
					
					case "GOLF":
						GolfBot golf_bot = new GolfBot(sql_connection);
						int today = ContestMethods.getToday();
						golf_bot.scrapeTourneyID(today);
						if(golf_bot.getTourneyID() != null){
							
							try{
								 scoring_rules = data.getJSONObject("scoring_rules");
								 sport_title = " | Multi-Stat";
								
							}catch(Exception e){
								sport_title = " | Score to Par";
							}
							
							try{
								// figure out deadline
								switch(prop_data.getString("when")){
									case "tournament":
										deadline = golf_bot.getDeadline();
										date_name_title = golf_bot.getTourneyName() + " | Full Tournament";
										break;
									case "1":
										deadline = golf_bot.getDeadline();
										date_name_title =golf_bot.getTourneyName() + " | Round 1";
										break;
									case "2":
										deadline = golf_bot.getDeadline() + 86400000;
										date_name_title = golf_bot.getTourneyName() + " | Round 2";
										break;
									case "3":
										deadline = golf_bot.getDeadline() + (2 * 86400000);
										date_name_title = golf_bot.getTourneyName() + " | Round 3";
										break;
									case "4":
										deadline = golf_bot.getDeadline() + (3 * 86400000);
										date_name_title = golf_bot.getTourneyName() + " | Round 4";
										break;
								}
							}catch(JSONException e){
								deadline = golf_bot.getDeadline();
								date_name_title = " | " + golf_bot.getTourneyName();
							}
							
						}else{
							output.put("error", "Golf contests are not currently available");
							break method;
						}
						break;
						
					case "BASKETBALL":
						BasketballBot basketball_bot = new BasketballBot(sql_connection);
						basketball_bot.scrapeGameIDs();
						if(basketball_bot.getGameIDs() != null){
							deadline = basketball_bot.getEarliestGame();
							gameIDs = basketball_bot.getGameIDs().toString();
							date_name_title = Instant.ofEpochMilli(deadline).atZone(ZoneId.systemDefault()).toLocalDate().toString();

						}else{
							output.put("error", "Basketball contests are not currently available");
							break method;
						}
						break;
						
					default:
						break;
				}
				
				String prop_type = prop_data.getString("prop_type");
				int auto_settle = 1;
				JSONArray option_table = new JSONArray();
				
				//check prop_type and set option_table, title, description accordingly
				switch(prop_type){
					case "MATCH_PLAY":
						
						contest_title = "Match Play";
						
						JSONObject tie = new JSONObject();
						tie.put("id", 1);
						tie.put("description", "Tie");
						option_table.put(tie);
						int index = 2;
						JSONArray players = prop_data.getJSONArray("players");
						for(int i=0; i < players.length(); i++){
							String player_id = players.getString(i);
							String name = db.get_player_info(sport, player_id);
							JSONObject p = new JSONObject();
							p.put("description", name);
							p.put("id", index);
							p.put("player_id", player_id);
							option_table.put(p);
							index += 1;
						}
						
						if(sport.equals("GOLF") && prop_data.getString("multi_stp").equals("score_to_par")){
							desc += "Place your bets on which of the following golfers will finish the contest with the top score to par";
						}
						else{
							//one stat game
							if(scoring_rules.length() == 1){
								String key = (String) scoring_rules.keys().next();
								desc += "Place your bets on which player will get more " + key + "?<br>";
								desc += appendDescription(scoring_rules);
								
							}
							//multi-stat game
							else{
								desc += appendDescription(scoring_rules);
							}
						}
						break;
						
					case "OVER_UNDER":
						
						String name = db.get_player_info(sport, prop_data.getString("player_id"));
						contest_title = name + " Over/Under";
						String o_u = String.valueOf(prop_data.getDouble("over_under_value"));
						
						JSONObject over = new JSONObject();
						over.put("description", "Over " + o_u);
						over.put("id", 1);
						option_table.put(over);
						JSONObject under = new JSONObject();
						under.put("description", "Under " + o_u);
						under.put("id", 2);
						option_table.put(under);
						
						if(sport.equals("GOLF") && prop_data.getString("multi_stp").equals("score_to_par")){
							desc += "Place your bets on if " + name + " will finish the contest over or under " + o_u ;
						}
						else{
							//single stat
							if(scoring_rules.length() == 1){
								String key = (String) scoring_rules.keys().next();
								desc += "Place your bets on if " + name + " will get over/under "+ String.valueOf(prop_data.getDouble("over_under_value")) + " " + key + "?";
								desc += appendDescription(scoring_rules);
							}
							//multi-stat game
							else{
								desc += appendDescription(scoring_rules);
							}
						}
						break;
						
					case "NUMBER_SHOTS":
						
						name = db.get_player_info(sport, prop_data.getString("player_id"));
						String when = prop_data.getString("when");
						
						// make option table
						JSONObject any_other = new JSONObject();
						any_other.put("description", "Any other number");
						any_other.put("id", 1);
						option_table.put(any_other);
						
						// tournament prop
						if(when.equals("tournament")){
							for(int i = 0; i <= 60; i++){
								JSONObject item = new JSONObject();
								item.put("id", i+2);
								item.put("description", String.valueOf(i));
								option_table.put(item);
							}
							desc = "Place your bets on the number of " + prop_data.getString("shot").toUpperCase() + " " + name + " will accumulate in the entire tournament";
						}
						
						// just one round
						else{
							for(int i = 0; i <= 18; i++){
								JSONObject item = new JSONObject();
								item.put("id", i+2);
								item.put("description", String.valueOf(i));
								option_table.put(item);
							}
							desc = "Place your bets on the number of " + prop_data.getString("shot").toUpperCase() + " " + name + " will accumulate in Round " + when;
						}
						sport_title = "";
						contest_title = name + " Number of " + prop_data.getString("shot").toUpperCase();
						break;
						
					case "MAKE_CUT":
						name = db.get_player_info(sport, prop_data.getString("player_id"));
						
						JSONObject yes = new JSONObject();
						yes.put("description", "Yes");
						yes.put("id", 1);
						option_table.put(yes);
						JSONObject no = new JSONObject();
						no.put("description", "No");
						no.put("id", 2);
						option_table.put(no);
						
						sport_title = "";
						contest_title = "Will " + name + " Make the Cut?";
						desc = "Place your bets on whether or not " + name + " will make the cut!";
						
						// pass in when: tournament
						prop_data.put("when", "tournament");
						break;
					
					
					default:
						log("Could not find prop bet with type = " + prop_type);
						output.put("error", "Could not find prop bet with type = " + prop_type);
						break;
				
				}
				
				String title = date_name_title + " | " + contest_title + sport_title;
				
				JSONObject prop = new JSONObject();
				prop.put("category", category);
				prop.put("sub_category", sub_category);
				prop.put("progressive", progressive);
				prop.put("contest_type", contest_type);
				prop.put("title", title);
				prop.put("description", desc);
				prop.put("registration_deadline", deadline);
				prop.put("rake", rake);
				prop.put("cost_per_entry", cost_per_entry);
				prop.put("settlement_type", settlement_type);
				prop.put("option_table", option_table);
				prop.put("auto_settle", auto_settle);
				prop.put("scoring_rules", scoring_rules.toString());
				prop.put("prop_data", prop_data.toString());
				prop.put("private", priv);
				prop.put("gameIDs", gameIDs);
				
				MethodInstance prop_method = new MethodInstance();
				JSONObject prop_output = new JSONObject("{\"status\":\"0\"}");
				prop_method.input = prop;
				prop_method.output = prop_output;
				prop_method.internal_caller = true;
				prop_method.session =  session;
				prop_method.sql_connection = sql_connection;
				
				try{
					Constructor<?> c = Class.forName("com.coinroster.api." + "CreateContest").getConstructor(MethodInstance.class);
					c.newInstance(prop_method);
					output = prop_method.output;
					output.put("status", "1");
				}
				catch(Exception e){
					output = prop_method.output;
					Server.exception(e);
				}
				
				
			}catch(Exception e){
				Server.exception(e);
				output.put("error", e.toString());
				output.put("status", "0");
			}
//------------------------------------------------------------------------------------
		}
		method.response.send(output);
	}
	
	public String appendDescription(JSONObject scoring_rules){
		String desc = "Scoring Rules<br>";
		Iterator<?> keys = scoring_rules.keys();
		while(keys.hasNext()){
			String key = (String) keys.next();
			try {
				String value_str = "";
				int value = scoring_rules.getInt(key);
				if(value < 0)
					value_str = String.valueOf(value);
				else
					value_str = "+" + String.valueOf(value);
				
				desc += key.toUpperCase() + ": " + value_str + "<br>";
			}catch (Exception e) {
			}	
		}
		return desc;
	}
	
}