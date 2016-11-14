package com.coinroster.api;

import java.sql.Connection;
import java.text.DecimalFormat;

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
			
			JSONArray player_odds_array = new JSONArray();
			   
			int number_of_players = 30;
			
			for (int i=0; i<number_of_players; i++)
				{
				String player_name = "Player " + i;
				
				double player_odds = 1 + 1 / (double)(i+5);
				
				DecimalFormat F = new DecimalFormat("#####.###");

				JSONObject player = new JSONObject();
				
				player.put("name", player_name);
				player.put("odds", F.format(player_odds));
				
				player_odds_array.put(player);
				}
			
			output.put("player_odds_array", player_odds_array);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}