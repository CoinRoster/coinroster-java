package com.coinroster.bots;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.html.HTMLElement;

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
import java.io.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;

public class LiveScoring_GOLF extends Application
    {
	static int contest_id = 81;
	
	static boolean 
	
	running = true,
	has_initialized = false;
	
	static String 
	
	cookie_string = null,
	score_board_url = "http://www.pgatour.com/leaderboard.html";
			
	static ConcurrentHashMap<String, Integer> players_in_contest = new ConcurrentHashMap<String, Integer>();
	
	static ConcurrentHashMap<Integer, String> player_scores = new ConcurrentHashMap<Integer, String>();
	static ConcurrentHashMap<Integer, String> player_scores_stored = new ConcurrentHashMap<Integer, String>();
	
	static ConcurrentHashMap<String, String> out_of_contest_player_scores = new ConcurrentHashMap<String, String>();
	static ConcurrentHashMap<String, String> out_of_contest_player_scores_stored = new ConcurrentHashMap<String, String>();

	static JSONObject contest = null;
	
    public static void main(String[] args) throws Exception
        {
        Platform.setImplicitExit(true);
        
        log("");
		log("Getting CoinRoster contest details: ");
		log("");
		
		JSONObject post_data = new JSONObject();
		
		post_data.put("username", "score_bot");
		post_data.put("password", "!cr_score_updater_4/15/2017");
		
		new LiveScoring_GOLF().post_page("https://www.coinroster.com/Login.api", post_data.toString());
		
		post_data = new JSONObject();
		post_data.put("contest_id", contest_id);
		
		List<String> contest_JSON = new LiveScoring_GOLF().post_page("https://www.coinroster.com/GetContestDetails.api", post_data.toString());
		
		contest = new JSONObject(contest_JSON.get(0));
		
		String sport = contest.getString("sub_category");
		
		if (!sport.equals("GOLF"))
			{
			log("Contest is not a GOLF contest!");
			log("Please use a different live-scoring application or enter the correct contest ID");
			System.exit(0);
			}
	
		JSONArray option_table = new JSONArray(contest.getString("option_table"));
		
		int number_of_players = option_table.length();
				
		for (int i=0; i<number_of_players; i++)
			{
			JSONObject player = option_table.getJSONObject(i);
			
			int player_id = player.getInt("id");
			
			String 
			
			player_name_raw = player.getString("name").replaceAll(" +", " "),
			player_key = get_player_key(player_name_raw);
			
			log(player_key);
		
			if (players_in_contest.containsKey(player_key)) log("!!!!!!!! Collision: " + player_key);
			
			players_in_contest.put(player_key, player_id);
			player_scores.put(player_id, "--");
			}
		
		player_scores_stored = new ConcurrentHashMap<Integer, String>(player_scores);
		out_of_contest_player_scores_stored = new ConcurrentHashMap<String, String>(out_of_contest_player_scores);

		//Long registration_deadline = contest.getLong("registration_deadline");
		
		//log(registration_deadline - System.currentTimeMillis());
				
		log("");
		log("Starting GOLF live scoring");
		
		launch(args);
        }

//-----------------------------------------------------------------------------------------------------   
    	
    // CONVERT PLAYER NAME INTO KEY WITH FORMAT    LASTNAME,F    (F = first letter of first name)
    
    static String get_player_key(String player_name)
    	{
    	try
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
    	catch (Exception ignore)
    		{
    		return null;
    		}
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
		    			
						if (!player_scores.equals(player_scores_stored) || !out_of_contest_player_scores.equals(out_of_contest_player_scores_stored))
			    			{
				    		call_CoinRoster("UpdateScores");
				    		player_scores_stored = new ConcurrentHashMap<Integer, String>(player_scores);
				    		out_of_contest_player_scores_stored = new ConcurrentHashMap<String, String>(out_of_contest_player_scores);
			    			}
						
						scan_scoreboard(webengine);
						
						boolean done = exit_if_tournament_ended();
						
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
				
				List<HTMLElement> player_rows = new ArrayList<HTMLElement>();

				NodeList divs = webengine.getDocument().getElementsByTagName("div");
				
				for (int i=0, limit=divs.getLength(); i<limit; i++) 
					{
					Node div_node = divs.item(i);
					HTMLElement div_html = (HTMLElement) div_node;
					String class_name = div_html.getClassName();
					if (class_name != null && class_name.contains("leaderboard-item") && class_name.contains("player-row")) player_rows.add(div_html);
					}
				
				if (player_rows.size() == 0 && has_initialized) // catch appearance of "Error" message where game cards previously loaded
					{
					has_initialized = false;
					log("");
					log("Reloading...");
					webengine.reload();
					}
				else // there are game cards
					{
					has_initialized = true;
					
					for (HTMLElement player_row : player_rows)
						{
						NodeList cells = player_row.getElementsByTagName("td");
						
						for (int i=0, limit=cells.getLength(); i<limit; i++) 
							{
							String text_content = cells.item(i).getTextContent().trim();
							
							String player_key = get_player_key(text_content);
							
							if (player_key == null) continue;
							
							if (players_in_contest.containsKey(player_key)) 
								{
								int
								
								player_id = players_in_contest.get(player_key),
								index_of_WD_CUT = i - 3,
								index_of_score = i + 1;
								
								String 
								
								score_raw = null,
								WD_CUT = cells.item(index_of_WD_CUT).getTextContent().trim();
								
								if (WD_CUT.equals("CUT") || WD_CUT.equals("WD")) score_raw = WD_CUT;
								else score_raw = cells.item(index_of_score).getTextContent().trim();
								
								player_scores.put(player_id, score_raw);
								
								break;
								}
							}
						}
					}
				}
			});
    	}

//-----------------------------------------------------------------------------------------------------   
      
    // DUAL-USE API CALL: UpdateScores and SettleContest
    
    static void call_CoinRoster(String method)
    	{
    	try {
			log("");
	    	log(method);
			log("");
			
			int worst_integer_score = -99;
			
			for (Entry<Integer, String> entry : player_scores.entrySet())
				{
			    String score_raw = entry.getValue();

			    try {
			    	int integer_score = 0;
			    	
			    	if (score_raw.equals("E")) integer_score = 0;
			    	else integer_score = Integer.parseInt(score_raw);
			    	
			    	if (integer_score > worst_integer_score) worst_integer_score = integer_score;
			    	}
			    catch (Exception ignore)
			    	{
			    	continue;
			    	}
				}
			
	    	JSONArray scores = new JSONArray();
	    	
	    	for (Entry<Integer, String> entry : player_scores.entrySet())
				{
			    int 
			    
			    player_id = entry.getKey(),
			    score_normalized = 0;
			    
				String score_raw = entry.getValue();
				
			    try {
			    	int integer_score = 0;
			    	
			    	if (score_raw.equals("E")) integer_score = 0;
			    	else integer_score = Integer.parseInt(score_raw);

			    	// if we get here, the player has an integer score
			    	// otherwise it's "WD" or "CUT" or "--" and keeps the normalized score of 0 assigned above
			    	
			    	score_normalized = worst_integer_score - integer_score + 1;
			    	}
			    catch (Exception ignore) {}
			  
			    JSONObject player_item = new JSONObject();
			    
			    player_item.put("id", player_id);
			    player_item.put("score_normalized", score_normalized);
			    player_item.put("score_raw", score_raw);

			    scores.put(player_item);
				}

			JSONObject args = new JSONObject();
	    	
	    	args.put("contest_id", contest_id);
	    	args.put("normalization_scheme", "INTEGER-INVERT");
	    	args.put("player_scores", scores);
	    	
			List<String> response = new LiveScoring_GOLF().post_page("https://www.coinroster.com/" + method + ".api", args.toString());
			
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
      	 
    // CODE BLOCK THAT GETS RUN IF THE TOURNAMENT HAS ENDED
    
    static boolean exit_if_tournament_ended()
      	{
  		//call_CoinRoster("SettleContest");
  		
  		return false;
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
		List<String> page = new LiveScoring_GOLF().get_page(url);
		
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

	static void log(Object msg)
		{
		System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
		}
    
//-----------------------------------------------------------------------------------------------------   

    }
