package com.coinroster.bots;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class LiveScoring_HOCKEY extends Application
    {
	static String cookie_string = null;
			
	static int contest_id = 77;
	
	static String score_board_url = null;
	
	static JSONObject 
	
	contest = null,
    team_cities = new JSONObject(),
    team_motifs = new JSONObject();

	static HashMap<String, Integer> teams_tonight = new HashMap<String, Integer>();
	static HashMap<String, Integer> player_map = new HashMap<String, Integer>();
	static HashMap<Integer, Integer> player_scores = new HashMap<Integer, Integer>();
	
    public static void main(String[] args) throws Exception
        {
        Platform.setImplicitExit(true);
        
        log("");
		log("Getting CoinRoster contest details: ");
		log("");
		
		JSONObject post_data = new JSONObject();
		
		post_data.put("username", "score_bot");
		post_data.put("password", "!cr_score_updater_4/15/2017");
		
		new LiveScoring_HOCKEY().post_page("https://www.coinroster.com/Login.api", post_data.toString());
		
		post_data = new JSONObject();
		
		post_data.put("contest_id", contest_id);
		
		List<String> contest_JSON = new LiveScoring_HOCKEY().post_page("https://www.coinroster.com/GetContestDetails.api", post_data.toString());
		
		contest = new JSONObject(contest_JSON.get(0));
		
		String sport = contest.getString("sub_category");
		
		if (!sport.equals("HOCKEY"))
			{
			log("Contest is not a HOCKEY contest!");
			log("Please use a different live-scoring application or enter the correct contest ID");
			System.exit(0);
			}
		
		Date registration_deadline = new Date(contest.getLong("registration_deadline"));
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd"); 
		score_board_url = "http://www.sportsnet.ca/hockey/nhl/scores/?datepicker-date=" + df.format(registration_deadline);
		
        populate_teams();
        
		log("Starting NHL live scoring");
		log("");
		
		JSONArray option_table = new JSONArray(contest.getString("option_table"));
		
		int number_of_players = option_table.length();
				
		for (int i=0; i<number_of_players; i++)
			{
			JSONObject player = option_table.getJSONObject(i);
			
			int player_id = player.getInt("id");
			
			String 
			
			player_name_raw = player.getString("name").replaceAll(" +", " "),
			player_name = player_name_raw.substring(0, player_name_raw.lastIndexOf(" ")),
			player_key = get_player_key(player_name);
			
			String[] player_name_parts = player_name_raw.split(" ");
			int number_of_name_parts = player_name_parts.length; // get rid of team
			
			String 
			
			team_acronym = player_name_parts[number_of_name_parts-1],
			team_motif = team_motifs.getString(team_acronym).toUpperCase();
			
			if (!teams_tonight.containsKey(team_motif)) teams_tonight.put(team_motif, 0);

			player_map.put(player_key, player_id);
			player_scores.put(player_id, 0);
			}
		
		try{
			launch(args);
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
        }

//-----------------------------------------------------------------------------------------------------   
          
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
    
	static String get_formatted_page(String url)
		{
		List<String> page = new LiveScoring_HOCKEY().get_page(url);
		
		StringBuilder builder = new StringBuilder();
		
		for (int i=0; i<page.size(); i++) builder.append(page.get(i));

		String page_string = builder.toString();

		page_string = page_string.replaceAll("\n", " ");
		page_string = page_string.replaceAll("\t", " ");
		page_string = page_string.replaceAll(" +", " ");
		page_string = page_string.replaceAll("> ", ">");
		page_string = page_string.replaceAll(" <", "<");
        
        return page_string;
		}
	
	List<String> get_page(String url_string) 
		{
		URL url;
		
		try {
			url = new URL(url_string);
			
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
			else // regular htp
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
	
	/*private static String full_encode(String in)
		{
		in = in.replace(" ", "%20");
		in = in.replace("!", "%21");
		in = in.replace("\"", "%22");
		in = in.replace("#", "%23");
		in = in.replace("$", "%24");
		in = in.replace("%", "%25");
		in = in.replace("&", "%26");
		in = in.replace("'", "%27");
		in = in.replace("\\(", "%28");
		in = in.replace("\\)", "%29");
		in = in.replace("\\*", "%2A");
		in = in.replace("\\+", "%2B");
		in = in.replace(",", "%2C");
		in = in.replace("-", "%2D");
		in = in.replace("\\.", "%2E");
		in = in.replace("/", "%2F");
		in = in.replace(":", "%3A");
		in = in.replace(";", "%3B");
		in = in.replace("<", "%3C");
		in = in.replace("=", "%3D");
		in = in.replace(">", "%3E");
		in = in.replace("\\?", "%3F");
		in = in.replace("@", "%40");
		in = in.replace("\\[", "%5B");
		in = in.replace("\\\\", "%5C");
		in = in.replace("\\]", "%5D");
		in = in.replace("^", "%5E");
		in = in.replace("_", "%5F");
		in = in.replace("`", "%60");
		in = in.replace("\\{", "%7B");
		in = in.replace("|", "%7C");
		in = in.replace("\\}", "%7D");
		in = in.replace("~", "%7E");
		in = in.replace("`", "%80");
		
		return in;
		}*/

	private void print_https_cert(HttpsURLConnection con) {
		if (con != null) {
			try {
				con.getResponseCode();
				con.getCipherSuite();
				Certificate[] certs = con.getServerCertificates();
				for (@SuppressWarnings("unused") Certificate cert : certs) {
					// System.out.println("Cert Type : " + cert.getType());
					// System.out.println("Cert Hash Code : " +
					// cert.hashCode());
					// System.out.println("Cert Public Key Algorithm : "
					// + cert.getPublicKey().getAlgorithm());
					// System.out.println("Cert Public Key Format : "
					// + cert.getPublicKey().getFormat());
					// System.out.println("\n");
				}
			} catch (SSLPeerUnverifiedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@SuppressWarnings("unused")
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
	
	@SuppressWarnings("unused")
	private static int count_occurrences(String str, String findStr)
		{
		int count = 0;
		int lastIndex = 0;

		while(lastIndex != -1)
			{
		    lastIndex = str.indexOf(findStr,lastIndex);

		    if(lastIndex != -1)
		    	{
		        count ++;
		        lastIndex += findStr.length();
		    	}
			}
		
		return count;
		}
	
	@SuppressWarnings("unused")
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
	
	@SuppressWarnings("unused")
	private static String get_attribute(String line, String attribute)
		{
		attribute += "=\"";
		
		line = line.substring(line.indexOf(attribute) + attribute.length(), line.length());
		line = line.substring(0,line.indexOf("\""));
		
		return line;
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
	
	static void log(Object msg)
		{
		System.out.println(new SimpleDateFormat("MMM d h:mm:ss a").format(new Date()) + " : " + String.valueOf(msg));
		}
   
    @Override
    public void start(Stage stage) throws Exception
        {    	
    	System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        WebView webview = new WebView();
        final WebEngine webengine = webview.getEngine();
        
        new Thread() 
			{
	    	@Override
			public void run() 
	        	{
				while (true)
					{
					try {
						Platform.runLater(new Runnable()
							{
							@Override
							public void run() 
								{
								if (webengine.getDocument() != null && webengine.getDocument().getDocumentElement() != null)
									{
									NodeList divs = webengine.getDocument().getElementsByTagName("div");
									List<HTMLElement> game_cards = new ArrayList<HTMLElement>();
									
									for (int i=0, limit=divs.getLength(); i<limit; i++) 
										{
										Node div_node = divs.item(i);
										HTMLElement div_html = (HTMLElement) div_node;
										String class_name = div_html.getClassName();
										if (class_name != null && class_name.equals("game-card-container")) game_cards.add(div_html);
										}
									
									int number_of_games = game_cards.size();
									
									if (number_of_games == 0)
										{
										log("Looks like there are 0 games - please check the link and verify");
										System.exit(0);
										}
	
									int
									
									games_in_contest = 0,
									games_ended = 0;
									
									boolean update_scores = false;
									
									for (HTMLElement game_card : game_cards)
										{
										String 
										
										game_card_id = game_card.getId(),
										game_id = game_card_id.substring(game_card_id.lastIndexOf("_") + 1, game_card_id.length()),
										game_card_content = game_card.getTextContent().replaceAll(" +", " ");
										
										String[] lines = game_card_content.split("\n");
										
										int team_counter = 0;
										boolean game_has_ended = false;
										
										String[] teams = new String[2];
										int[] scores = new int[2];
										
										for (String line : lines)
											{
											if (line.equals("") || line.equals(" ")) continue;
											
											line = line.trim().toUpperCase();
										
											if (teams_tonight.containsKey(line)) 
												{
												team_counter++;
												teams[team_counter - 1] = line;
												}
											
											try {
												int score = Integer.parseInt(line);
												scores[team_counter - 1] = score;
												}
											catch (Exception ignore) {}
											
											if (line.contains("FINAL")) game_has_ended = true;
											}
										
										if (team_counter == 2)
											{
											games_in_contest++;
											
											boolean process_game = false;
											
											for (int i=0; i<2; i++)
												{
												String team_motif = teams[i];
												
												int 
												
												score_now = scores[i],
												score_stored = teams_tonight.get(team_motif);
	
												if (score_now != score_stored)
													{
													log("");
													log(team_motif + " | " + score_stored + " -> " + score_now);
													teams_tonight.put(team_motif, score_now);
													process_game = true;
													}
												}
											
											if (process_game) 
												{
												log("");
												log("Fetching player scores for game " + game_id);
												update_scores = true;
												process_game(game_id, teams[0], teams[1]);
												}

											if (game_has_ended) games_ended++;
											}
										else if (team_counter == 1) log("!!!! ERROR !!!! Only one team found in matchup - something is wrong");
										}
									
									if (update_scores) update_scores();
									
									if (games_ended == games_in_contest) 
										{
										log("All games have ended");
										System.exit(0);
										}
									}
								}
							});
						Thread.sleep(1000);
						}
					catch (Exception e) 
						{
						e.printStackTrace();
			            }
			        }
				}
			}.start();
        
        webengine.setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:52.0) Gecko/20100101 Firefox/52.0");
        webengine.load(score_board_url);
        
        stage.setScene(new Scene(webview, 800, 800));
        stage.show();
        }
    
    static void process_game(String game_id, String team_1, String team_2)
    	{
    	try {
	    	String game_page = get_formatted_page("http://www.sportsnet.ca/hockey/nhl/games/" + game_id);
	    	
	    	Pattern tr_open = Pattern.compile("(<tr)");
			Matcher tr_match = tr_open.matcher(game_page);
			
			while (tr_match.find()) 
				{
				int row_start = tr_match.start();
				int row_end = game_page.indexOf("</tr>", row_start);
				String row = game_page.substring(row_start, row_end + 5);
				
				List<String> row_data = get_row_data(row);
				
				int number_of_cells = row_data.size();
				
				if (number_of_cells != 11) continue;
				
				String 
				
				player_name = row_data.get(0).replaceAll(" +", " "),
				player_key = get_player_key(player_name);

				if (player_map.containsKey(player_key))
					{
					int 
					
					player_id = player_map.get(player_key),
					score_old = player_scores.get(player_id),
					score_new = Integer.parseInt(row_data.get(3));
					
					if (score_old != score_new) 
						{
						log(player_key + " | " + score_old + " -> " + score_new);
						player_scores.put(player_id, score_new);
						}
					}
				}
	    	}
    	catch (Exception e)
    		{
    		e.printStackTrace();
    		}
    	}
    
    static String get_player_key(String player_name)
    	{
    	player_name = player_name.toUpperCase();
    	
    	String[] player_name_parts = player_name.split(" ");
		int number_of_name_parts = player_name_parts.length;
		
		String 
		
		first_name = player_name_parts[0],
		first_letter = first_name.substring(0, 1),
		last_name = player_name_parts[number_of_name_parts-1];
		
		return first_letter + "-" + last_name;
    	}
    
    static void update_scores()
    	{
    	try {
			log("");
	    	log("Updating scores");
	    	
	    	JSONArray scores = new JSONArray();
	    	
	    	for (Entry<Integer, Integer> entry : player_scores.entrySet())
				{
			    int
			    
			    player_id = entry.getKey(),
			    player_score = entry.getValue();
			    
			    /*String
			    
			    first_name = player_first_names.get(player_id),
			    last_name = player_last_names.get(player_id);*/
			    
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
	    	
	    	JSONObject post_data = new JSONObject();
			
			post_data.put("contest_id", contest_id);
			
			List<String> response = new LiveScoring_HOCKEY().post_page("https://www.coinroster.com/UpdateScores.api", args.toString());
			
			JSONObject response_JSON = new JSONObject(response.get(0));
			
			if (response_JSON.getString("status").equals("0")) log("!!!!! " + response_JSON.getString("error"));
	    	}
    	catch (Exception e)
    		{
    		e.printStackTrace();
    		}
    	}
    }

