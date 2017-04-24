package com.coinroster.bots;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javafx.application.Platform;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class RosterBot_NHL
    {
	static int contest_id = 88;
	
	static boolean 
	
	running = true,
	has_initialized = false;
	
	static String 
	
	cookie_string = null,
	score_board_url = "https://www.nhl.com/scores";
			
	static ConcurrentHashMap<String, Integer> teams_tonight = new ConcurrentHashMap<String, Integer>();
	static ConcurrentHashMap<String, Integer> player_ids = new ConcurrentHashMap<String, Integer>();
	static ConcurrentHashMap<String, Double> player_prices = new ConcurrentHashMap<String, Double>();
	
	static int hot_streak_games = 12;
	
	static int player_count = 0;
	static double grand_total_points = 0;
	static double grand_total_points_per_60 = 0;
	
	static TreeMap<Integer, String> player_names = new TreeMap<Integer, String>();
	static TreeMap<Integer, Integer> points_map = new TreeMap<Integer, Integer>();
	static TreeMap<Integer, Double> points_per_60_map = new TreeMap<Integer, Double>();
	static TreeMap<Integer, Double> hot_streak_map = new TreeMap<Integer, Double>();
	static TreeMap<Double, Integer> composite_rankings = new TreeMap<Double, Integer>(Collections.reverseOrder());
	static TreeMap<Double, Integer> hot_streak_rankings = new TreeMap<Double, Integer>(Collections.reverseOrder());
	
	static JSONObject 
	
	contest = null,
	
    team_cities = new JSONObject(),
    team_motifs = new JSONObject(),
    team_stats = new JSONObject();
	
    static void populate_teams() throws JSONException
    	{
    	team_cities.put("ANA", "Anaheim");
        team_motifs.put("ANA", "Ducks");
        team_stats.put("ANA", "http://www.espn.com/nhl/team/stats/_/name/ana");

        team_cities.put("ARI", "Arizona");
        team_motifs.put("ARI", "Coyotes");
        team_stats.put("ARI", "http://www.espn.com/nhl/team/stats/_/name/ari");

        team_cities.put("BOS", "Boston");
        team_motifs.put("BOS", "Bruins");
        team_stats.put("BOS", "http://www.espn.com/nhl/team/stats/_/name/bos");

        team_cities.put("BUF", "Buffalo");
        team_motifs.put("BUF", "Sabres");
        team_stats.put("BUF", "http://www.espn.com/nhl/team/stats/_/name/buf");

        team_cities.put("CAR", "Carolina");
        team_motifs.put("CAR", "Hurricanes");
        team_stats.put("CAR", "http://www.espn.com/nhl/team/stats/_/name/car");

        team_cities.put("CGY", "Calgary");
        team_motifs.put("CGY", "Flames");
        team_stats.put("CGY", "http://www.espn.com/nhl/team/stats/_/name/cgy");

        team_cities.put("CHI", "Chicago");
        team_motifs.put("CHI", "Blackhawks");
        team_stats.put("CHI", "http://www.espn.com/nhl/team/stats/_/name/chi");

        team_cities.put("CLB", "Columbus");
        team_motifs.put("CLB", "Blue Jackets");
        team_stats.put("CLB", "http://www.espn.com/nhl/team/stats/_/name/cbj");

        team_cities.put("COL", "Colorado");
        team_motifs.put("COL", "Avalanche");
        team_stats.put("COL", "http://www.espn.com/nhl/team/stats/_/name/col");

        team_cities.put("DAL", "Dallas");
        team_motifs.put("DAL", "Stars");
        team_stats.put("DAL", "http://www.espn.com/nhl/team/stats/_/name/dal");

        team_cities.put("DET", "Detroit");
        team_motifs.put("DET", "Red Wings");
        team_stats.put("DET", "http://www.espn.com/nhl/team/stats/_/name/det");

        team_cities.put("EDM", "Edmonton");
        team_motifs.put("EDM", "Oilers");
        team_stats.put("EDM", "http://www.espn.com/nhl/team/stats/_/name/edm");

        team_cities.put("FLA", "Florida");
        team_motifs.put("FLA", "Panthers");
        team_stats.put("FLA", "http://www.espn.com/nhl/team/stats/_/name/fla");

        team_cities.put("LAK", "Los Angeles");
        team_motifs.put("LAK", "Kings");
        team_stats.put("LAK", "http://www.espn.com/nhl/team/stats/_/name/la");

        team_cities.put("MIN", "Minnesota");
        team_motifs.put("MIN", "Wild");
        team_stats.put("MIN", "http://www.espn.com/nhl/team/stats/_/name/min");

        team_cities.put("MTL", "Montreal");
        team_motifs.put("MTL", "Canadiens");
        team_stats.put("MTL", "http://www.espn.com/nhl/team/stats/_/name/mtl");

        team_cities.put("NJD", "New Jersey");
        team_motifs.put("NJD", "Devils");
        team_stats.put("NJD", "http://www.espn.com/nhl/team/stats/_/name/nj");

        team_cities.put("NYI", "New York");
        team_motifs.put("NYI", "Islanders");
        team_stats.put("NYI", "http://www.espn.com/nhl/team/stats/_/name/nyi");

        team_cities.put("NSH", "Nashville");
        team_motifs.put("NSH", "Predators");
        team_stats.put("NSH", "http://www.espn.com/nhl/team/stats/_/name/nsh");

        team_cities.put("NYR", "New York");
        team_motifs.put("NYR", "Rangers");
        team_stats.put("NYR", "http://www.espn.com/nhl/team/stats/_/name/nyr");

        team_cities.put("OTT", "Ottawa");
        team_motifs.put("OTT", "Senators");
        team_stats.put("OTT", "http://www.espn.com/nhl/team/stats/_/name/ott");

        team_cities.put("PHI", "Philadelphia");
        team_motifs.put("PHI", "Flyers");
        team_stats.put("PHI", "http://www.espn.com/nhl/team/stats/_/name/phi");

        team_cities.put("PIT", "Pitssburgh");
        team_motifs.put("PIT", "Penguins");
        team_stats.put("PIT", "http://www.espn.com/nhl/team/stats/_/name/pit");

        team_cities.put("SJS", "San Jose");
        team_motifs.put("SJS", "Sharks");
        team_stats.put("SJS", "http://www.espn.com/nhl/team/stats/_/name/sj");

        team_cities.put("STL", "St. Louis");
        team_motifs.put("STL", "Blues");
        team_stats.put("STL", "http://www.espn.com/nhl/team/stats/_/name/stl");

        team_cities.put("TBL", "Tampa Bay");
        team_motifs.put("TBL", "Lightning");
        team_stats.put("TBL", "http://www.espn.com/nhl/team/stats/_/name/tb");

        team_cities.put("TOR", "Toronto");
        team_motifs.put("TOR", "Maple Leafs");
        team_stats.put("TOR", "http://www.espn.com/nhl/team/stats/_/name/tor");

        team_cities.put("VAN", "Vancouver");
        team_motifs.put("VAN", "Canucks");
        team_stats.put("VAN", "http://www.espn.com/nhl/team/stats/_/name/van");

        team_cities.put("WAS", "Washington");
        team_motifs.put("WAS", "Capitals");
        team_stats.put("WAS", "http://www.espn.com/nhl/team/stats/_/name/wsh");

        team_cities.put("WPG", "Winnipeg");
        team_motifs.put("WPG", "Jets");
        team_stats.put("WPG", "http://www.espn.com/nhl/team/stats/_/name/wpg");
    	}

    public static void main(String[] args) throws Exception
        {
        Platform.setImplicitExit(true);
        
        log("");
		log("Getting CoinRoster contest details: ");
		log("");
		
		/*JSONObject post_data = new JSONObject();
		
		post_data.put("username", "score_bot");
		post_data.put("password", "!cr_score_updater_4/15/2017");
		
		new RosterBot_NHL().post_page("https://www.coinroster.com/Login.api", post_data.toString());*/
		
		JSONObject post_data = new JSONObject();
		post_data.put("contest_id", contest_id);
		
		List<String> contest_JSON = new RosterBot_NHL().post_page("https://www.coinroster.com/GetContestDetails.api", post_data.toString());
		
		contest = new JSONObject(contest_JSON.get(0));
		
		String sport = contest.getString("sub_category");
		
		if (!sport.equals("HOCKEY"))
			{
			log("Contest is not a HOCKEY contest!");
			log("Please use a different live-scoring application or enter the correct contest ID");
			System.exit(0);
			}
		
        populate_teams();
        
		JSONArray option_table = new JSONArray(contest.getString("option_table"));
		
		int number_of_players = option_table.length();
				
		for (int i=0; i<number_of_players; i++)
			{
			JSONObject player = option_table.getJSONObject(i);
			
			int player_id = player.getInt("id");
			
			double price = player.getDouble("price");
			
			String 
			
			player_name_raw = player.getString("name").replaceAll(" +", " "),
			player_name = player_name_raw.substring(0, player_name_raw.lastIndexOf(" ")),
			player_key = get_player_key(player_name);
			
			log(player_key + " | " + price);
			
			String[] player_name_parts = player_name_raw.split(" ");
			int number_of_name_parts = player_name_parts.length; // get rid of team
			
			String team_acronym = player_name_parts[number_of_name_parts-1];
			
			if (!teams_tonight.containsKey(team_acronym)) teams_tonight.put(team_acronym, 0);

			if (player_ids.containsKey(player_key)) log("!!!!!!!! Collision: " + player_key);
			
			player_ids.put(player_key, player_id);
			player_prices.put(player_key, price);
			}

        log("");
		log("Cross-referencing injured list ");
		log("");
		
        String injured_list = get_formatted_page("https://www.espn.com/nhl/injuries");
        
        org.jsoup.nodes.Document doc = Jsoup.parse(injured_list);
        
        Elements injured_list_rows = doc.select("tr");
        
        for (Element element : injured_list_rows) 
        	{
        	Element first_cell = element.select("td").get(0);
    		String player_key = get_player_key(first_cell.text());
        	if (player_ids.containsKey(player_key)) 
        		{
        		log("!!! Injured: " + first_cell.text() + " (" + player_key + ")");
        		Element date_cell = element.select("td").get(2);
        		Element next_row = element.nextElementSibling();
        		log(date_cell.text() + " - " + next_row.text());
        		}
        	}
        
        log("");
        
        for (Map.Entry<String, Integer> entry : teams_tonight.entrySet())
        	{
        	String 
        	
        	team = entry.getKey(),
        	stats_url = team_stats.getString(team) + "/seasontype/2";
        	
        	log(team + " " + stats_url);
        	log("");
        	
        	String stats_page = get_formatted_page(stats_url);
        	
        	org.jsoup.nodes.Document page = Jsoup.parse(stats_page);
        	
        	Elements anchors = page.select("a");
        	
        	HashSet<String> players_already_matched = new HashSet<String>();
        	
        	for (Element anchor : anchors) 
        		{
        		String anchor_text = anchor.text();
        		if (!anchor_text.contains(" ")) continue;
        		String player_key = get_player_key(anchor_text);
        		if (player_ids.containsKey(player_key)) 
	        		{
        			if (!players_already_matched.contains(player_key))
	        			{
        				players_already_matched.add(player_key);
        				
        				String gamelog = anchor.attr("href").replace("/player/", "/player/gamelog/");
        				
        				get_player_stats(player_key, gamelog);
	        			}
	        		}
        		}
        	}
		

		double
		
		average_points = divide(grand_total_points, player_count, 0),
		average_points_per_60 = divide(grand_total_points_per_60, player_count, 0);

		/*log("");
		log("Average points: " + average_points);
		log("Average points per 60 mins: " + average_points_per_60);*/
		
		//double points_per_60_weighting = 0.5;
		
		for (Map.Entry<Integer, String> entry : player_names.entrySet()) 
			{
			int player_id = entry.getKey();
			//String player_name = entry.getValue();
			int points = points_map.get(player_id);
			double points_per_60 = points_per_60_map.get(player_id);
			double points_index = divide(points, average_points, 0);
			double points_per_60_index = divide(points_per_60, average_points_per_60, 0);
			double hot_streak_ppg = hot_streak_map.get(player_id);
			
			double player_composite = add(points_index, points_per_60_index, 0);
			player_composite = divide(player_composite, 2, 0);
			
			double hot_streak_index = multiply(player_composite, hot_streak_ppg, 0);
			
			if (hot_streak_index == 0) continue;
			
			/*log("");
			log("ID: " + player_id + " | " + player_name);
			log("Points: " + points);
			log("Points index: " + points_index);
			log("Points per 60: " + points_per_60);
			log("Points per 60 index: " + points_per_60_index);
			log("Composite: " + player_composite);*/
			
			composite_rankings.put(player_composite, player_id);
			hot_streak_rankings.put(hot_streak_index, player_id);
			}
		
		//int player_count = 0;

		log("");
		log("Composite: ");
		log("");
		
		for (Map.Entry<Double, Integer> entry : composite_rankings.entrySet()) 
			{
			player_count++;
			
			double player_composite = entry.getKey();
			int player_id = entry.getValue();
			String player_name = player_names.get(player_id);
			
			log(player_composite + "\t" + player_name);
			}

		log("");
		log("Composite + hot streak: ");
		log("");

		player_count = 0;
		
		for (Map.Entry<Double, Integer> entry : hot_streak_rankings.entrySet()) 
			{
			player_count++;
			
			double player_composite = entry.getKey();
			int player_id = entry.getValue();
			String player_name = player_names.get(player_id);			
			double player_price = player_prices.get(player_name);
			double value = divide(player_price, player_composite, 0);
			
			log(player_name + " | " + value);
			}

        }
	
	static void get_player_stats(String player_key, String gamelog_link)
		{
		log(player_key);
		
		int player_id = player_ids.get(player_key);
		
		String gamelog = get_formatted_page(gamelog_link);
		
		// find all table rows in gamelog:
		
		String row_open = "<tr";
		
		int index_of_row = gamelog.indexOf(row_open);
		
		// for now I'm using a strict mapping:
		
		int 
		
		rows_found = 0,
		cells_required = 17,
		
		/*

		 0 | DATE
		 1 | OPP
		 2 | RESULT
		 3 | G
		 4 | A
		 5 | PTS
		 6 | +/-
		 7 | PIM
		 8 | SOG
		 9 | %
		10 | PPG
		11 | PPA
		12 | SHG
		13 | SHA
		14 | GWG
		15 | TOI
		16 | PROD
		
		*/
		
		COL_DATE = 0,
		COL_GOALS = 3,
		COL_ASSISTS = 4,
		COL_POINTS = 5,
		COL_ICETIME = 15,
		
		game_counter = 0,
		total_goals = 0,
		total_assists = 0,
		total_points = 0,
		total_ice_time = 0;
		
		List<Integer> points_by_game = new ArrayList<Integer>();

		while (index_of_row >= 0) 
			{
			int search_index = index_of_row + 10;
			
			this_row : {
	            
				String row = get_row(gamelog, search_index);
				
				if (row == null) break this_row;

				String[] row_data = get_row_data(row);
				
				int number_of_cells = row_data.length;
				
				if (number_of_cells != cells_required) break this_row;

				rows_found++;
	
				// game rows have a date in the first column - check for slash
				
				String date = row_data[COL_DATE];

				if (date.indexOf("/") == -1) break this_row;
				
				game_counter++;

				String ice_time_string = row_data[COL_ICETIME];
				
				int
				
				goals = Integer.parseInt(row_data[COL_GOALS]),
				assists = Integer.parseInt(row_data[COL_ASSISTS]),
				points = Integer.parseInt(row_data[COL_POINTS]),
				ice_time = time_string_to_seconds(ice_time_string);
				
				total_goals += goals;
				total_assists += assists;
				total_points += points;
				total_ice_time += ice_time;
				
				points_by_game.add(points);
				}

			index_of_row = gamelog.indexOf(row_open, index_of_row + 1);
			}
		
		if (rows_found == 0)
			{
			log("No rows found for " + player_key + " gamelog");
			System.exit(0);
			}

		if (total_goals + total_assists != total_points)
			{
			log("Goals and assits don't add up to points for " + player_key);
			System.exit(0);
			}
		
		int hot_streak_points = 0;
		
		for (int i=game_counter-1; i>=game_counter-hot_streak_games; i--)
			{
			int points = points_by_game.get(i);
			hot_streak_points += points;
			}
		
		double hot_streak_ppg = divide(hot_streak_points, hot_streak_games, 0);
		
		hot_streak_map.put(player_id, hot_streak_ppg);
		
		double
		
		minutes_played = divide(total_ice_time, 60, 0),
		hours_played = divide(minutes_played, 60, 0),
		points_per_60 = divide(total_points, hours_played, 0);
		
		log("points per 60 " + points_per_60);
		log("hot streak ppg " + hot_streak_ppg);
		log("total points " + total_points);
		log("");

		grand_total_points = add(grand_total_points, total_points, 0);
		grand_total_points_per_60 = add(grand_total_points_per_60, points_per_60, 0);

		player_names.put(player_id, player_key);
		points_map.put(player_id, total_points);
		points_per_60_map.put(player_id, points_per_60);
		
		player_count++;
		}

//-----------------------------------------------------------------------------------------------------   
    	
    // CONVERT PLAYER NAME INTO KEY WITH FORMAT    LASTNAME,F    (F = first letter of first name)
    
    static String get_player_key(String player_name)
    	{
      	player_name = player_name.toUpperCase();
      	
      	String[] player_name_parts = player_name.split(" ");
  		int number_of_name_parts = player_name_parts.length;
  		
  		String 
  		
  		first_name = player_name_parts[0],
  		first_letter = first_name.substring(0, 1),
  		last_name = player_name_parts[number_of_name_parts-1];
  		
  		return last_name + "," + first_letter;
      	}

    // DUAL-USE API CALL: UpdateScores and SettleContest
    
    static void call_CoinRoster(String method)
    	{
    	try {
			/*JSONObject args = new JSONObject();
	    	
	    	args.put("contest_id", contest_id);
	    	args.put("normalization_scheme", "INTEGER");
	    	args.put("player_scores", scores);
				 
			List<String> response = new RosterBot_NHL().post_page("https://www.coinroster.com/" + method + ".api", args.toString());
			
			JSONObject response_JSON = new JSONObject(response.get(0));

			if (response_JSON.getString("status").equals("1")) log(method + ": OK");
			else log("!!!!! " + response_JSON.getString("error"));*/
	    	}
    	catch (Exception e)
    		{
    		e.printStackTrace();
    		}
    	}

//-----------------------------------------------------------------------------------------------------     
// HELPER FUNCTIONS BELOW 
//-----------------------------------------------------------------------------------------------------   

    // EVERYTHING TO DO WITH HTTP(S) GET/POST REQUESTS
    
	List<String> get_page(String url_string) 
		{
		try {
			URL url = new URL(url_string);
			
			if (url_string.startsWith("https://")) 
				{
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
				
				con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
				con.setRequestProperty("Connection", "keep-alive");
				con.setRequestProperty("User-Agent", "mozilla/5.0 (windows nt 6.1; wow64; rv:50.0) gecko/20100101 firefox/50.0|5.0 (Windows)|Win32");
				
				print_https_cert(con);

				if (con != null) 
					{
					BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					List<String> page = new ArrayList<String>();

					String line;

					while ((line = br.readLine()) != null) page.add(line);

					br.close();
					
					return page;
					}
				}
			else // regular http
				{
				HttpURLConnection con = (HttpURLConnection) url.openConnection();
				
				con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
				con.setRequestProperty("Connection", "keep-alive");
				con.setRequestProperty("User-Agent", "mozilla/5.0 (windows nt 6.1; wow64; rv:50.0) gecko/20100101 firefox/50.0|5.0 (Windows)|Win32");
				
				if (con != null) 
					{
					BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
					List<String> page = new ArrayList<String>();

					String line;

					while ((line = br.readLine()) != null) page.add(line);

					br.close();
					
					return page;
					}
				}
			} 
		catch (MalformedURLException e) 
			{
			e.printStackTrace();
			} 
		catch (IOException e) 
			{
			e.printStackTrace();
			}
		
		return null;
		}
	
	List<String> post_page(String https_url, String post_data) 
		{
		URL url;
		
		try {
			url = new URL(https_url);
			HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

			con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setRequestProperty("Connection", "keep-alive");
			if (cookie_string != null) con.setRequestProperty("Cookie", cookie_string);
			con.setRequestProperty("User-Agent", "mozilla/5.0 (windows nt 6.1; wow64; rv:50.0) gecko/20100101 firefox/50.0|5.0 (Windows)|Win32");
			con.setDoOutput(true);
			
			OutputStreamWriter out = new OutputStreamWriter(con.getOutputStream());
			out.write(post_data);
			out.close();
			
			Map<String, List<String>> map = con.getHeaderFields();

			for (Map.Entry<String, List<String>> entry : map.entrySet()) 
				{
				if (String.valueOf(entry.getKey()).contains("Set-Cookie")) 
					{
					List<String> set_cookie = entry.getValue();
					
					String 
					temp = set_cookie.get(0),
					session_token = temp.substring(temp.indexOf("=") + 1, temp.indexOf(";"));
					
					cookie_string = "session_token=" + session_token + ";";
					break;
					}
				}
			
			print_https_cert(con);

			if (con != null) 
				{
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				List<String> page = new ArrayList<String>();

				String line;

				while ((line = br.readLine()) != null) page.add(line);

				br.close();
				
				return page;
				}
			} 
		catch (MalformedURLException e) 
			{
			e.printStackTrace();
			} 
		catch (IOException e) 
			{
			e.printStackTrace();
			}
		
		return null;
		}

	private void print_https_cert(HttpsURLConnection con) 
		{
		if (con != null)
			{
			try {
				con.getResponseCode();
				con.getCipherSuite();
				Certificate[] certs = con.getServerCertificates();
				for (@SuppressWarnings("unused") Certificate cert : certs) 
					{
					// System.out.println("Cert Type : " + cert.getType());
					// System.out.println("Cert Hash Code : " +
					// cert.hashCode());
					// System.out.println("Cert Public Key Algorithm : "
					// + cert.getPublicKey().getAlgorithm());
					// System.out.println("Cert Public Key Format : "
					// + cert.getPublicKey().getFormat());
					// System.out.println("\n");
					}
				} 
			catch (SSLPeerUnverifiedException e) 
				{
				e.printStackTrace();
				} 
			catch (IOException e) 
				{
				e.printStackTrace();
				}
			}
		}

	static String get_formatted_page(String url)
		{
		List<String> page = new RosterBot_NHL().get_page(url);
		
		StringBuilder builder = new StringBuilder();
		
		for (int i=0; i<page.size(); i++) builder.append(page.get(i));

		String page_string = builder.toString();

		page_string = page_string.replaceAll("\n", " ");
		page_string = page_string.replaceAll("\t", " ");
		page_string = page_string.replaceAll(" +", " ");
		page_string = page_string.replaceAll("> ", ">");
		page_string = page_string.replaceAll(" >", ">");
		page_string = page_string.replaceAll("< ", "<");
		page_string = page_string.replaceAll(" <", "<");
        
        return page_string;
		}
    
//-----------------------------------------------------------------------------------------------------   

	// CONVERT ROW STRING TO LIST<STRING> OF CELL CONTENTS
	
	private static int time_string_to_seconds(String time_string)
		{
		String[] components = time_string.split(":");
		
		int
		
		hours = 0,
		mins = 0,
		secs = 0;
		
		if (components.length == 2) // minutes:seconds
			{
			mins = Integer.parseInt(components[0]);
			secs = Integer.parseInt(components[1]);
			}
		else if (components.length == 3) // hours:minutes:seconds
			{
			hours = Integer.parseInt(components[0]);
			mins = Integer.parseInt(components[1]);
			secs = Integer.parseInt(components[2]);
			}
		
		int total_seconds = hours * 3600 + mins * 60 + secs;
		
		return total_seconds;
		}

	private static String get_row(String html, int search_index)
       {
       String row = null;
       int
      
       tr_open_index = -1,
       tr_close_index = -1;
      
       for (int i=search_index; i >=0; i--)
           {
           if (
               String.valueOf(html.charAt(i)).equals("r") &&
               String.valueOf(html.charAt(i-1)).equals("t") &&
               String.valueOf(html.charAt(i-2)).equals("<")
               )
               {
               tr_open_index = i-2;
               break;
               }
           }
      
       int html_length = html.length();
      
       for (int i=search_index; i <=html_length; i++)
           {
           if (
               String.valueOf(html.charAt(i)).equals("<") &&
               String.valueOf(html.charAt(i+1)).equals("/") &&
               String.valueOf(html.charAt(i+2)).equals("t") &&
               String.valueOf(html.charAt(i+3)).equals("r") &&
               String.valueOf(html.charAt(i+4)).equals(">")
               )
               {
               tr_close_index = i+5;
               break;
               }
           }

       if (tr_open_index != -1 && tr_close_index != -1) row = html.substring(tr_open_index, tr_close_index);

       return row;           
       }

	private static String[] get_row_data(String row)
		{		
		int 
		
		index_of_closing_cell_tag = row.indexOf("</td>"),
		number_of_cells = 0;
		
		List<String> cell_data_list = new ArrayList<String>();
		
		while (index_of_closing_cell_tag > 0)
			{
			int 
            
            search_index = index_of_closing_cell_tag,
            left_diamond_index = -1;
            
            // scan left from slash:
            
            for (int j=search_index; j>=0; j--)
            	{
            	if (Character.valueOf(row.charAt(j)).equals('>'))
            		{
            		left_diamond_index = j + 1;
                	break;
                	}
            	}
            
            String cell_data = row.substring(left_diamond_index, index_of_closing_cell_tag).trim();
            
            cell_data_list.add(cell_data);

			// increment counter and get next closing tag:

			number_of_cells++;
			index_of_closing_cell_tag = row.indexOf("</td>", index_of_closing_cell_tag + 1);
			}
		
		String[] row_data = new String[number_of_cells];
		
		for (int i=0, limit=cell_data_list.size(); i<limit; i++) row_data[i] = cell_data_list.get(i);
		
		return row_data;
   		}

	final static int MAX_PRECISION = 8;
	public static double multiply(double in1, double in2, int max_precision)
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).multiply(BigDecimal.valueOf(in2));
		out = out.setScale(max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double divide(double in1, double in2, int max_precision) // requires rounding in the actual divide function
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).divide(BigDecimal.valueOf(in2), max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double add(double in1, double in2, int max_precision)
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).add(BigDecimal.valueOf(in2));
		out = out.setScale(max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	public static double subtract(double in1, double in2, int max_precision)
		{
		if (max_precision == 0) max_precision = MAX_PRECISION;
		BigDecimal out = BigDecimal.valueOf(in1).subtract(BigDecimal.valueOf(in2));
		out = out.setScale(max_precision, RoundingMode.HALF_UP);
		return out.doubleValue();
		}
	
//-----------------------------------------------------------------------------------------------------   

	static void log(Object msg)
		{
		System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
		}
    
//-----------------------------------------------------------------------------------------------------   

    }
