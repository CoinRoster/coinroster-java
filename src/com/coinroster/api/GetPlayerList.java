package com.coinroster.api;

import java.awt.List;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.bots.*;

public class GetPlayerList extends Utils{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public GetPlayerList(MethodInstance method) throws Exception {
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;
		DB db = new DB(sql_connection);

		method : {	
//------------------------------------------------------------------------------------
			try{
				String sport = input.getString("sport");
				ArrayList<String> gameIDs = new ArrayList<String>();
				
				switch(sport){
				case "BASKETBALL":
					BasketballBot basketball_bot = new BasketballBot(sql_connection);
					basketball_bot.scrapeGameIDs();
					gameIDs = basketball_bot.getGameIDs();
					break;
				case "BASEBALL":
					BaseballBot baseball_bot = new BaseballBot(sql_connection);
					baseball_bot.scrapeGameIDs();
					gameIDs = baseball_bot.getGameIDs();
					break;
				case "GOLF":
					GolfBot golf_bot = new GolfBot(sql_connection);
					int today = getToday();
					golf_bot.scrapeTourneyID(today);
					gameIDs.add(golf_bot.getTourneyID());
					break;
				}
				
				JSONArray players = new JSONArray();
				
				for(String id : gameIDs){
					JSONArray p = db.get_all_players(sport, id);
					for(int i = 0; i < p.length(); i++){
						players.put(p.get(i));
					}
				}
				
				
				output.put("player_list", players);
				output.put("status", "1");
			}catch(Exception e){
				log(e.toString());
				output.put("error", e.toString());
				output.put("status", "0");
			}
//------------------------------------------------------------------------------------
		}
		method.response.send(output);
	}
	public static int getToday(){
		Calendar c = Calendar.getInstance();        		
		int today = c.get(Calendar.DAY_OF_WEEK);
		return today;
	}
}