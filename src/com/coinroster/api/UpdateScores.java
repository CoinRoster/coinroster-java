package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;

public class UpdateScores extends Utils
	{
	public static String method_level = "score_bot";
	@SuppressWarnings("unused")
	public UpdateScores(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------
		
			int contest_id = input.getInt("contest_id");
			
			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables contest write, player read");

			try {
				lock : {
				
					// initial contest validation
		
					JSONObject contest = db.select_contest(contest_id);
					
					String 
					
					contest_title = contest.getString("title"),
					contest_type = contest.getString("contest_type");
		
					if (contest == null)
						{
						String error = "Invalid contest id: " + contest_id;
						
						log(error);
						output.put("error", error);
						
						break lock;
						}
					
					if (contest.getInt("status") != 2)
						{
						String error = "Contest " + contest_id + " is not in play";
						
						log(error);
						output.put("error", error);
						
						break lock;
						}

					if (!contest.getString("contest_type").equals("ROSTER"))
						{
						String error = "Contest " + contest_id + " is not a roster contest";
						
						log(error);
						output.put("error", error);
						
						break lock;
						}
					
					// ROSTER
					
					JSONArray player_scores;
					
					Map<String, Double> score_map = new TreeMap<String, Double>();
					Map<String, String> raw_score_map = new TreeMap<String, String>();

					String normalization_scheme = input.getString("normalization_scheme");

					JSONArray option_table = new JSONArray(contest.getString("option_table"));
					
					// populate map of player scores 
						
					player_scores = input.getJSONArray("player_scores");
					
					for (int i=0, limit=player_scores.length(); i<limit; i++)
						{
						JSONObject player = player_scores.getJSONObject(i);
						
						String player_id = player.getString("id");
						double score_normalized = player.getDouble("score_normalized");
						String score_raw = player.getString("score_raw");
						
						score_map.put(player_id, score_normalized);
						raw_score_map.put(player_id, score_raw);
						}
						
					// loop through system players to make sure all players have been assigned a score
					// add player scores to JSONObject to be written back into contest
						
					for (int i=0, limit=option_table.length(); i<limit; i++)
						{
						JSONObject player = option_table.getJSONObject(i);
						
						String player_id = player.getString("id");
						
						if (!score_map.containsKey(player_id))
						{
							String error = "No score provided for " + player.getString("name");
							log(error);
							
							// pga tour withdrawal
							player.put("score", 0);
							player.put("score_raw", "WD");
							
							option_table.put(i, player);
							
						}
						else{
				
							player.put("score", score_map.get(player_id));
							player.put("score_raw", raw_score_map.get(player_id));
							
							option_table.put(i, player);
							}
						}
					
					PreparedStatement update_contest = sql_connection.prepareStatement("update contest set option_table = ?, scores_updated = ?, scoring_scheme = ? where id = ?");
					update_contest.setString(1, option_table.toString());
					update_contest.setLong(2, System.currentTimeMillis());
					update_contest.setString(3, normalization_scheme);
					update_contest.setInt(4, contest_id);
					update_contest.executeUpdate();
					
					output.put("status", "1");
					}
				}
			catch (Exception e)
				{
				output.put("error", e.getMessage());
				Server.exception(e);
				}
			finally
				{
				statement.execute("unlock tables");
				}
			
			
				
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}