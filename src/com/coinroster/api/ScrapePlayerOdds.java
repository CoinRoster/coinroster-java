package com.coinroster.api;

import java.sql.Connection;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class ScrapePlayerOdds extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public ScrapePlayerOdds(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
			String url = input.getString("url");
			
			// scrape odds from URL
			
			// if problem with URL, put url_ok -> 0
			
			output.put("url_ok", "1");
			
			JSONArray player_odds_list = new JSONArray();
			   
			int number_of_players = 30;
			
			for (int i=0; i<number_of_players; i++)
				{
				String 
				
				player_name = "Player " + i,
				player_odds = (i+5) + "/1";

				JSONObject player = new JSONObject();
				
				player.put("name", player_name);
				player.put("odds", player_odds);
				
				player_odds_list.put(player);
				}
			
			output.put("player_odds_list", player_odds_list);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}