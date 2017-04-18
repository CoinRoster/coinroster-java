package com.coinroster.bots;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLElement;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class LiveScoring_HOCKEY2 extends Application
    {
	static int contest_id = 79;
	
	static boolean 
	
	running = true,
	has_initialized = false;
	
	static String 
	
	cookie_string = null,
	score_board_url = "https://www.nhl.com/scores";
			
	static ConcurrentHashMap<String, Integer> teams_tonight = new ConcurrentHashMap<String, Integer>();
	static ConcurrentHashMap<String, Integer> players_in_contest = new ConcurrentHashMap<String, Integer>();

	static ConcurrentHashMap<String, String> event_summary_urls = new ConcurrentHashMap<String, String>();
	
	static ConcurrentHashMap<Integer, Integer> player_scores = new ConcurrentHashMap<Integer, Integer>();
	static ConcurrentHashMap<Integer, Integer> player_scores_stored = new ConcurrentHashMap<Integer, Integer>();
	static ConcurrentHashMap<String, Integer> out_of_contest_player_scores = new ConcurrentHashMap<String, Integer>();
	
	static ConcurrentHashMap<String, Boolean> game_ended_flags = new ConcurrentHashMap<String, Boolean>();

	static JSONObject 
	
	contest_details = null,
	
    team_cities = new JSONObject(),
    team_motifs = new JSONObject();
	
    static void populate_teams() throws JSONException
    	{
    	team_cities.put("ANA", "Anaheim");
        team_motifs.put("ANA", "Ducks");

        team_cities.put("ARI", "Arizona");
        team_motifs.put("ARI", "Coyotes");

        team_cities.put("BOS", "Boston");
        team_motifs.put("BOS", "Bruins");

        team_cities.put("BUF", "Buffalo");
        team_motifs.put("BUF", "Sabres");

        team_cities.put("CAR", "Carolina");
        team_motifs.put("CAR", "Hurricanes");

        team_cities.put("CGY", "Calgary");
        team_motifs.put("CGY", "Flames");

        team_cities.put("CHI", "Chicago");
        team_motifs.put("CHI", "Blackhawks");

        team_cities.put("CLB", "Columbus");
        team_motifs.put("CLB", "Blue Jackets");

        team_cities.put("COL", "Colorado");
        team_motifs.put("COL", "Avalanche");

        team_cities.put("DAL", "Dallas");
        team_motifs.put("DAL", "Stars");

        team_cities.put("DET", "Detroit");
        team_motifs.put("DET", "Red Wings");

        team_cities.put("EDM", "Edmonton");
        team_motifs.put("EDM", "Oilers");

        team_cities.put("FLA", "Florida");
        team_motifs.put("FLA", "Panthers");

        team_cities.put("LAK", "Los Angeles");
        team_motifs.put("LAK", "Kings");

        team_cities.put("MIN", "Minnesota");
        team_motifs.put("MIN", "Wild");

        team_cities.put("MTL", "Montreal");
        team_motifs.put("MTL", "Canadiens");

        team_cities.put("NJD", "New Jersey");
        team_motifs.put("NJD", "Devils");

        team_cities.put("NYI", "New York");
        team_motifs.put("NYI", "Islanders");

        team_cities.put("NSH", "Nashville");
        team_motifs.put("NSH", "Predators");

        team_cities.put("NYR", "New York");
        team_motifs.put("NYR", "Rangers");

        team_cities.put("OTT", "Ottawa");
        team_motifs.put("OTT", "Senators");

        team_cities.put("PHI", "Philadelphia");
        team_motifs.put("PHI", "Flyers");

        team_cities.put("PIT", "Pitssburgh");
        team_motifs.put("PIT", "Penguins");

        team_cities.put("SJS", "San Jose");
        team_motifs.put("SJS", "Sharks");

        team_cities.put("STL", "St. Louis");
        team_motifs.put("STL", "Blues");

        team_cities.put("TBL", "Tampa Bay");
        team_motifs.put("TBL", "Lightning");

        team_cities.put("TOR", "Toronto");
        team_motifs.put("TOR", "Maple Leafs");

        team_cities.put("VAN", "Vancouver");
        team_motifs.put("VAN", "Canucks");

        team_cities.put("WAS", "Washington");
        team_motifs.put("WAS", "Capitals");

        team_cities.put("WPG", "Winnipeg");
        team_motifs.put("WPG", "Jets");
    	}
	
	static int[] scan_schedule = new int[]{
		15,
		10,
		5,
		2,
		2,
		2,
		2,
		2,
		2,
		2,
		2,
		2,
		2,
		5,
		5,
		10,
		10,
		10,
		20,
		20,
		20,
		30
	};

    public static void main(String[] args) throws Exception
        {
        Platform.setImplicitExit(true);
        
        log("");
		log("Getting CoinRoster contest details: ");
		log("");
		
		JSONObject post_data = new JSONObject();
		
		post_data.put("username", "score_bot");
		post_data.put("password", "!cr_score_updater_4/15/2017");
		
		new LiveScoring_HOCKEY2().post_page("https://www.coinroster.com/Login.api", post_data.toString());
		
		post_data = new JSONObject();
		post_data.put("contest_id", contest_id);
		
		List<String> contest_JSON = new LiveScoring_HOCKEY2().post_page("https://www.coinroster.com/GetContestDetails.api", post_data.toString());
		
		contest_details = new JSONObject(contest_JSON.get(0));
		
		String sport = contest_details.getString("sub_category");
		
		if (!sport.equals("HOCKEY"))
			{
			log("Contest is not a HOCKEY contest!");
			log("Please use a different live-scoring application or enter the correct contest ID");
			System.exit(0);
			}
		
        populate_teams();
        
		JSONArray option_table = new JSONArray(contest_details.getString("option_table"));
		
		int number_of_players = option_table.length();
				
		for (int i=0; i<number_of_players; i++)
			{
			JSONObject player = option_table.getJSONObject(i);
			
			int player_id = player.getInt("id");
			
			String 
			
			player_name_raw = player.getString("name").replaceAll(" +", " "),
			player_name = player_name_raw.substring(0, player_name_raw.lastIndexOf(" ")),
			player_key = get_player_key(player_name);
			
			log(player_key);
			
			String[] player_name_parts = player_name_raw.split(" ");
			int number_of_name_parts = player_name_parts.length; // get rid of team
			
			String 
			
			team_acronym = player_name_parts[number_of_name_parts-1],
			team_motif = team_motifs.getString(team_acronym).toUpperCase();
			
			if (!teams_tonight.containsKey(team_motif)) teams_tonight.put(team_motif, 0);

			if (players_in_contest.containsKey(player_key)) log("!!!!!!!! Collision: " + player_key);
			
			players_in_contest.put(player_key, player_id);
			player_scores.put(player_id, 0);
			}
		
		player_scores_stored = new ConcurrentHashMap<Integer, Integer>(player_scores);

		log("");
		log("Starting NHL live scoring");
		log("");
		
		launch(args);
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

//-----------------------------------------------------------------------------------------------------   
       
    // FETCH AND STORE EVENT SUMMARY LINK FOR EACH GAME - ONLY CALLED ONCE PER GAME
    
  	private void prepare_event_summary_link(String game_id) 
  		{
  		String game_page = get_formatted_page("https://www.nhl.com/gamecenter/" + game_id).replaceAll(" +", "");
      	
      	int 
      	
      	index_of_season = game_page.indexOf("\"season\":") + 10,
      	index_of_closing_quote = game_page.indexOf("\"", index_of_season);
      	
      	String
      	
      	season = game_page.substring(index_of_season, index_of_closing_quote),
      	
      	event_summary_id = "ES" + game_id.substring(4, game_id.length()) + ".HTM",
      	event_summary_url = "http://www.nhl.com/scores/htmlreports/" + season + "/" + event_summary_id;
      	
      	event_summary_urls.put(game_id, event_summary_url);
  		}

//-----------------------------------------------------------------------------------------------------   
         
    // MAIN ITERATIVE LOOP
    
    @Override
    public void start(Stage stage) throws Exception
        {    	
    	System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    	
        WebView webview = new WebView();
        
        final WebEngine webengine = webview.getEngine();
 
        webengine.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0");
        webengine.load(score_board_url);
        
        stage.setScene(new Scene(webview, 800, 1200));
        stage.show();
        
        // main iterative loop

        new Thread() 
			{
	    	@Override
			public void run() 
	        	{
	    		while (running)
		    		{
		    		try {
		    			Thread.sleep(1000);
		    			
						if (!player_scores.equals(player_scores_stored))
			    			{
				    		call_CoinRoster("UpdateScores");
				    		player_scores_stored = new ConcurrentHashMap<Integer, Integer>(player_scores);
			    			}
						
						scan_scoreboard(webengine);
						
						boolean done = exit_if_games_ended();
						
						if (done) System.exit(0);
			    		}
		    		catch (Exception e)
		    			{
		    			e.printStackTrace();
		    			}
		    		}
	        	}
			}.start();
        }

//-----------------------------------------------------------------------------------------------------   
     
    // SCAN SCOREBOARD, TRIGGER process_game() WHEN SCORES UPDATE
    
    private void scan_scoreboard(final WebEngine webengine)
    	{
		Platform.runLater(new Runnable()
			{
			@Override
			public void run() 
				{
				if (webengine.getDocument() == null || webengine.getDocument().getDocumentElement() == null) return;
					
				List<HTMLElement> game_cards = new ArrayList<HTMLElement>();

				NodeList divs = webengine.getDocument().getElementsByTagName("div");
				
				for (int i=0, limit=divs.getLength(); i<limit; i++) 
					{
					Node div_node = divs.item(i);
					HTMLElement div_html = (HTMLElement) div_node;
					String class_name = div_html.getClassName();
					if (class_name != null && class_name.contains("nhl-scores__game")) game_cards.add(div_html);
					}
				
				if (game_cards.size() == 0 && has_initialized) // catch appearance of "Error" message where game cards previously loaded
					{
					has_initialized = false;
					webengine.reload();
					}
				else // there are game cards
					{
					has_initialized = true;
					
					for (HTMLElement game_card : game_cards)
						{
						game_card : 
							{
							final String game_id = game_card.getAttribute("id");

							boolean game_has_ended = false;
							
							String[] teams = new String[2];
							int[] scores = new int[2];
							
							int 
							
							team_ctr = 0,
							scores_found = 0;
							
							NodeList gamecard_spans = game_card.getElementsByTagName("span");
							
							for (int i=0, limit=gamecard_spans.getLength(); i<limit; i++) 
								{
								Node node = gamecard_spans.item(i);
								HTMLElement node_html = (HTMLElement) node;
								
								String 
								
								class_name = node_html.getClassName(),
								text_content = node.getTextContent().trim();
								
								if (class_name == null || text_content == null) continue;
								
								if (class_name.contains("nhl-scores__team-name")) 
									{
									String team_motif = text_content.toUpperCase();
									
									if (teams_tonight.containsKey(team_motif)) 
										{
										team_ctr++;
										teams[team_ctr-1] = team_motif;
										}
									}
								else if (class_name.contains("nhl-scores__team-score")) 
									{
									if (team_ctr > 0)
										{
										try {
											int score = Integer.parseInt(text_content);
											scores[team_ctr-1] = score;
											scores_found++;
											}
										catch (Exception e)
											{
											e.printStackTrace();
											}
										}
									else break game_card;
									}
								else if (class_name.contains("nhl-scores__status-state-label")) 
									{
									if (text_content.toUpperCase().contains("FINAL")) game_has_ended = true;
									}
								}
							
							if (scores_found == 2)
								{
								if (!event_summary_urls.containsKey(game_id)) prepare_event_summary_link(game_id);

								game_ended_flags.put(game_id, game_has_ended);
								
								boolean process_game = false;
								
								for (int i=0; i<2; i++)
									{
									String team_motif = teams[i];

									int 
									
									score_now = scores[i],
									score_stored = teams_tonight.get(team_motif);

									if (score_now != score_stored)
										{
										log(team_motif + " | " + score_stored + " -> " + score_now);
										
										teams_tonight.put(team_motif, score_now);
										
										process_game = true;
										}
									}
								
								if (process_game) 
									{
									new Thread() 
										{
								    	@Override
										public void run() 
								        	{
								    		try {
												boolean full_scan_schedule = true;
												
												scan_event_summary(game_id, full_scan_schedule);
									    		}
								    		catch (Exception e)
								    			{
								    			e.printStackTrace();
								    			}
								        	}
										}.start();
									}
								}
							}
						}
					}
				}
			});
    	}

//-----------------------------------------------------------------------------------------------------   
	   
    // SCAN EVENT SUMMARY PAGE
    
    static void scan_event_summary(String game_id, boolean full_scan_schedule)
    	{
    	String event_summary_url = event_summary_urls.get(game_id);
    	
    	for (int secs : scan_schedule)
    		{
    		try {
		    	String event_summary = get_formatted_page(event_summary_url);

		    	Pattern tr_open = Pattern.compile("(<tr)");
				Matcher tr_match = tr_open.matcher(event_summary);
				
				while (tr_match.find()) 
					{
					int 
					
					row_start = tr_match.start(),
					row_end = event_summary.indexOf("</tr>", row_start);
					
					String row = event_summary.substring(row_start, row_end + 5);
					
					List<String> row_data = get_row_data(row);
			
					if (row_data.size() != 25) continue;
					
					String player_name_cell = row_data.get(2);
					
					if (!player_name_cell.contains(",")) continue;
					
					String[] player_name_parts = player_name_cell.split(", ");
					
					String 
					
					last_name = player_name_parts[0],
					first_name = player_name_parts[1],
					
					player_name = first_name + " " + last_name,
			    	player_key = get_player_key(player_name),
			    	
			    	score_raw = row_data.get(5);
					
					int score_new = 0;
					
					try {
						score_new = score_raw == null ? 0 : Integer.parseInt(score_raw);
						}
					catch (Exception e)
						{
						log("!!!!!! error for " + player_key + " with score_raw: " + score_raw);
						continue;
						}
					
					if (score_raw != null)
						{
						//log(player_key + " | " + score_new);
						
						if (players_in_contest.containsKey(player_key))
							{
							int 
							
							player_id = players_in_contest.get(player_key),
							score_old = player_scores.get(player_id);
							
							if (score_old != score_new) log("IN: " + player_key + " | " + score_old + " -> " + score_new);
	
							player_scores.put(player_id, score_new);
							}
						else // player is not in contest
							{
							if (out_of_contest_player_scores.containsKey(player_key))
								{
								int score_old = out_of_contest_player_scores.get(player_key);
								
								if (score_old != score_new) log("OUT: " + player_key + " | " + score_old + " -> " + score_new);
								}
							else log("OUT: " + player_key + " | " + 0 + " -> " + score_new);

							out_of_contest_player_scores.put(player_key, score_new);
							}
						}
					}
		    	}
	    	catch (Exception e)
	    		{
	    		e.printStackTrace();
	    		}
    		finally
    			{
    	    	if (!full_scan_schedule) return;
    	    	
		    	try {
					Thread.sleep(secs * 1000);
					} 
		    	catch (InterruptedException e) 
		    		{
					e.printStackTrace();
		    		}
    			}
    		}
    	}

//-----------------------------------------------------------------------------------------------------   
      
    // DUAL-USE API CALL: UpdateScores and SettleContest
    
    static void call_CoinRoster(String method)
    	{
    	try {
			log("");
	    	log(method);
			log("");
			
	    	JSONArray scores = new JSONArray();
	    	
	    	for (Entry<Integer, Integer> entry : player_scores.entrySet())
				{
			    int
			    
			    player_id = entry.getKey(),
			    player_score = entry.getValue();

			    JSONObject player_item = new JSONObject();
			    
			    player_item.put("id", player_id);
			    player_item.put("score_normalized", player_score);
			    player_item.put("score_raw", Integer.toString(player_score));

			    scores.put(player_item);
				}

			JSONObject args = new JSONObject();
	    	
	    	args.put("contest_id", contest_id);
	    	args.put("normalization_scheme", "INTEGER");
	    	args.put("player_scores", scores);
				 
			List<String> response = new LiveScoring_HOCKEY2().post_page("https://www.coinroster.com/" + method + ".api", args.toString());
			
			JSONObject response_JSON = new JSONObject(response.get(0));

			if (response_JSON.getString("status").equals("1")) log(method + ": OK");
			else log("!!!!! " + response_JSON.getString("error"));
	    	}
    	catch (Exception e)
    		{
    		e.printStackTrace();
    		}
    	}

//-----------------------------------------------------------------------------------------------------   
      	 
    // CODE BLOCK THAT GETS RUN IF ALL IN-CONTEST GAMES HAVE ENDED
    
    static boolean exit_if_games_ended()
      	{
      	if (game_ended_flags.isEmpty()) return false;
      	
  		for (Entry<String, Boolean> entry : game_ended_flags.entrySet()) 
  			{
  		    boolean game_has_ended = entry.getValue();
  		    if (!game_has_ended) return false;
  			}
  	
  		log("");
  		log("All games have ended - 2 min wait");
  		
  		running = false;
  		
  		try {
  			Thread.sleep(120000);
  			} 
  		catch (InterruptedException e) 
  			{
  			e.printStackTrace();
  			}
  		
  		log("");
  		log("Re-scanning Event Summaries");
  		log("");
  		
  		for (Entry<String, Boolean> entry : game_ended_flags.entrySet()) 
  			{
  		    String game_id = entry.getKey();
  		    
  		    boolean full_scan_schedule = false;
  		    
  			scan_event_summary(game_id, full_scan_schedule);
  			}
  		
  		call_CoinRoster("SettleContest");
  		
  		return true;
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
		List<String> page = new LiveScoring_HOCKEY2().get_page(url);
		
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
	
	private static List<String> get_row_data(String row) throws Exception
		{
		Pattern td_open = Pattern.compile("(<td)");
		Matcher td_match = td_open.matcher(row);
		
		List<String> cell_data = new ArrayList<String>();
		
		while (td_match.find()) 
			{
			int cell_start = td_match.start();
			int cell_end = row.indexOf("</td>", cell_start);
			String cell = row.substring(cell_start, cell_end + 5);

	        try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
		        builder.setErrorHandler(new ErrorHandler() 
		        	{
		            @Override
		            public void warning(SAXParseException exception) throws SAXException 
		            	{
		            	
		            	}
		            @Override
		            public void error(SAXParseException exception) throws SAXException 
		            	{
		            	
		            	}
		            @Override
		            public void fatalError(SAXParseException exception) throws SAXException 
		            	{
		            	
		            	}
		        	});
		        Document doc = builder.parse(new InputSource(new StringReader(cell)));
		        Node node = doc.getDocumentElement();
		        cell_data.add(node.getTextContent());
		        }
	        catch (Exception ignore)
	        	{
	        	cell_data.add(null);
	        	}
			}
		
		return cell_data;
   		}
    
//-----------------------------------------------------------------------------------------------------   

	static void log(Object msg)
		{
		System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
		}
    
//-----------------------------------------------------------------------------------------------------   

    }

