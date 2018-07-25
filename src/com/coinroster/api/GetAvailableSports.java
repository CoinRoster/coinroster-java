package com.coinroster.api;

import java.sql.Connection;
import java.util.Calendar;

import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Utils;
import com.coinroster.bots.BaseballBot;
import com.coinroster.bots.BasketballBot;
import com.coinroster.bots.GolfBot;

public class GetAvailableSports extends Utils {
	
	public static String method_level = "guest";
	public GetAvailableSports(MethodInstance method) throws Exception {
		
		JSONObject 
		input = method.input,
		output = method.output;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
			Calendar c = Calendar.getInstance();
			Long now = c.getTimeInMillis();
			int hour = c.get(Calendar.HOUR_OF_DAY);
						
			// BASEBALL
			boolean baseball = false;
			BaseballBot baseball_bot = new BaseballBot(sql_connection);
			baseball_bot.scrapeGameIDs();
			if(baseball_bot.getGameIDs() != null){
				Long deadline = baseball_bot.getEarliestGame();
				if(hour >= 7 && now < deadline){
					baseball = true;
				}
			}
			
			// BASKETBALL
			boolean basketball = false;
			BasketballBot basketball_bot = new BasketballBot(sql_connection);
			basketball_bot.scrapeGameIDs();
			if(basketball_bot.getGameIDs() != null){
				Long deadline = basketball_bot.getEarliestGame();
				if(hour >= 7 && now < deadline){
					baseball = true;
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
			if(golf_bot.getTourneyID() != null){
				Long tournament_start = golf_bot.getDeadline();
				
				if(hour >= 6 && now < tournament_start){
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
			
			output.put("BASEBALL", baseball);
			output.put("BASKETBALL", basketball);
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
