package com.coinroster.api;

import java.sql.Connection;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Utils;
import com.coinroster.bots.BaseballBot;
import com.coinroster.bots.BasketballBot;
import com.coinroster.bots.GolfBot;
import com.coinroster.bots.HockeyBot;

public class GetAvailableSports extends Utils {
	
	public static String method_level = "guest";
	
	@SuppressWarnings("unused")
	public GetAvailableSports(MethodInstance method) throws Exception {
		
		JSONObject 
		input = method.input,
		output = method.output;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
			Calendar c = Calendar.getInstance();
			Long now = c.getTimeInMillis();
			int hour = c.get(Calendar.HOUR_OF_DAY);
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("YYYY-MM-dd");
			String today_str = LocalDate.now().format(formatter);
						
			// BASEBALL
			boolean baseball = false;
			JSONArray games_to_offer = new JSONArray();
			BaseballBot baseball_bot = new BaseballBot(sql_connection);
			baseball_bot.scrapeGameIDs();
			if(baseball_bot.getGameIDs() != null){
				JSONArray games = baseball_bot.getGames();
				for(int i = 0; i < games.length(); i++){
					JSONObject game = games.getJSONObject(i);
					if(hour >= 7 && game.getLong("date_milli") > (now + 5400000)){
						games_to_offer.put(game);
						baseball = true;
						output.put("baseball_contest", "MLB | " + today_str);
						output.put("baseball_games", games_to_offer);
					}
				}
			}
			
			// BASKETBALL
			boolean basketball = false;
			games_to_offer = new JSONArray();
			BasketballBot basketball_bot = new BasketballBot(sql_connection);
			basketball_bot.scrapeGameIDs();
			if(basketball_bot.getGameIDs() != null){
				JSONArray games = basketball_bot.getGames();
				for(int i = 0; i < games.length(); i++){
					JSONObject game = games.getJSONObject(i);
					if(hour >= 7 && game.getLong("date_milli") > (now + 5400000)){
						games_to_offer.put(game);
						baseball = true;
						output.put("basketball_contest", "NBA | " + today_str);
						output.put("basketball_games", games_to_offer);
					}
				}
			}
			
			// HOCKEY
			boolean hockey = false;
			games_to_offer = new JSONArray();
			HockeyBot hockey_bot = new HockeyBot(sql_connection);
			hockey_bot.scrapeGameIDs();
			if(hockey_bot.getGameIDs() != null){
				JSONArray games = hockey_bot.getGames();
				for(int i = 0; i < games.length(); i++){
					JSONObject game = games.getJSONObject(i);
					if(hour >= 7 && game.getLong("date_milli") > (now + 5400000)){
						games_to_offer.put(game);
						hockey = true;
						output.put("hockey_contest", "NHL | " + today_str);
						output.put("hockey_games", games_to_offer);
					}
				}
			}
		
			// GOLF
			boolean golf_tournament = false;
			boolean r1 = false;
			boolean r2 = false;
			boolean r3 = false;
			boolean r4 = false;
			GolfBot golf_bot = new GolfBot(sql_connection);
			int today = getToday();
			golf_bot.scrapeTourneyID(today);
			
			// if MONDAY, make sure its past 6am
			if(today == 2){
				if(hour >= 6){
					if(golf_bot.getTourneyID() != null){
						Long tournament_start = golf_bot.getDeadline();
						if(now < tournament_start){
							golf_tournament = true;
							r1 = true; 
							r2 = true; 
							r3 = true; 
							r4 = true;
						}
					}
				}
			}
			else{
	
				if(golf_bot.getTourneyID() != null){
				
					Long tournament_start = golf_bot.getDeadline();
	
					if(now < tournament_start){
						golf_tournament = true;
						r1 = true; 
						r2 = true; 
						r3 = true; 
						r4 = true;
					}
					else if(now >= tournament_start && now < tournament_start + 86400000){
						r2 = true;
						r3 = true;
						r4 = true;
					}
					else if(now >= tournament_start + 86400000 && now < tournament_start + (2 * 86400000)){
						r3 = true;
						r4 = true;
					}
					else if(now >= tournament_start + (2 * 86400000) && now < tournament_start + (3 * 86400000)){
						r4 = true;
					}
				}
			}
			
			if(r4){
				output.put("golf_contest", golf_bot.getTourneyName());
			}
			output.put("BASEBALL", baseball);
			output.put("BASKETBALL", basketball);
			output.put("HOCKEY", hockey);
			output.put("GOLF_TOURNAMENT", golf_tournament);
			output.put("GOLF_1", r1);
			output.put("GOLF_2", r2);
			output.put("GOLF_3", r3);
			output.put("GOLF_4", r4);

            output.put("status", "1");
            
		} 
		method.response.send(output);
	}
	
	public static int getToday(){
		Calendar c = Calendar.getInstance();        		
		int today = c.get(Calendar.DAY_OF_WEEK);
		return today;
	}
	
}
