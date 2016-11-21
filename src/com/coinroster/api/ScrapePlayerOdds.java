package com.coinroster.api;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
			
			//url = url.replace("https://", "http://");
			
			String html = get_html(url);
	           
            html = html.replaceAll(" +", " ");
            html = html.replaceAll("> ", ">");
            html = html.replaceAll(" <", "<");
            
            Pattern negative_number_regex = Pattern.compile("(?:\\d+\\s+)?\\d/\\d");
            
            HashSet<String> row_set = new HashSet<String>();
            
            List<String> player_rows = new ArrayList<String>();
            
            JSONArray player_odds_array = new JSONArray();
            
            Map<String, Integer> 
        	
        	left_map = new TreeMap<String, Integer>(),
        	right_map = new TreeMap<String, Integer>();

            String
            
            master_left_markup = "",
            master_right_markup = "";
            
            double previous_player_odds = 0;

            for (int step = 1; step <=2; step++)
	            {
            	// step 1 is finding the most common left and right flanking markup for fractions
            	// step 2 is grabbing all rows that satisfy all matching requirements
            	
            	if (step == 2)
            		{
            		Map<String, Integer> 
            		
            		sorted_left_map = sortByValue(left_map),
            		sorted_right_map = sortByValue(right_map);
    
            		// get most common left markup:
            		
            		for (Map.Entry<String, Integer> entry : sorted_left_map.entrySet())
	                    {
            			master_left_markup = entry.getKey();
            			break;
	                    }

            		// get most common right markup:
            		
            		for (Map.Entry<String, Integer> entry : sorted_right_map.entrySet())
	                    {
            			master_right_markup = entry.getKey();
	                    break;
	                    }
            		}
            	
	            Matcher matcher = negative_number_regex.matcher(html);
	
	            while (matcher.find())
	                {
	                int 
	                
	                fraction_slash_index = matcher.start() + 1,
	                left_diamond_index = -1,
	                right_diamond_index = -1;
	                
	                // scan left from slash:
	                
	                for (int i=fraction_slash_index - 1; i>=0; i--)
		            	{
	                	if (!Character.isDigit(html.charAt(i)))
		                	{
	                		if (Character.valueOf(html.charAt(i)).equals('>')) left_diamond_index = i;
		                	break;
		                	}
		            	}
	
	                // scan right from slash:
	                
	                for (int i=fraction_slash_index + 1; i<html.length(); i++)
	                	{
	                	if (!Character.isDigit(html.charAt(i)))
		                	{
	                		if (Character.valueOf(html.charAt(i)).equals('<')) right_diamond_index = i;
		                	break;
		                	}
	                	}
	
	                if (left_diamond_index != -1 && right_diamond_index != -1)
	                	{
	                	String
                		
                		left_markup = html.substring(left_diamond_index - 10, left_diamond_index),
                		right_markup = html.substring(right_diamond_index, right_diamond_index + 10);
	                	
	                	if (step == 1)
	                		{
	                		if (left_map.containsKey(left_markup))
	                			{
	                			int ctr = left_map.get(left_markup);
	                			left_map.put(left_markup, ctr + 1);
	                			}
	                		else left_map.put(left_markup, 1);
	                		
	                		if (right_map.containsKey(right_markup))
	                			{
	                			int ctr = right_map.get(right_markup);
	                			right_map.put(right_markup, ctr + 1);
	                			}
	                		else right_map.put(right_markup, 1);
	                		}
	                	
	                	else if (step == 2)
	                		{
	                		if (left_markup.equals(master_left_markup) && right_markup.equals(master_right_markup))
	                			{
	                			String 
	    	                	
	    	                	fractional_odds = html.substring(left_diamond_index + 1, right_diamond_index),
	    	                	row = get_row(html, fraction_slash_index);
	                			
	                			if (!row_set.contains(row))
	                				{
	                				String[] fraction_components = fractional_odds.split("/");
	                				
	                				double
	                				
	                				numerator = Double.parseDouble(fraction_components[0]),
	                				denominator = Double.parseDouble(fraction_components[1]),
	                				
	                				player_odds = 1 + numerator / denominator;
	                				
	                				if (player_odds >= previous_player_odds)
		                				{
		                				row_set.add(row);
		                				previous_player_odds = player_odds;
	                			
            		                    String cell =  row.split("</td>")[0];
            		                   
            		                    boolean 
            		                    
            		                    append = false,
            		                    have_something = false;
            		                   
            		                    StringBuilder builder = new StringBuilder();
            		                   
            		                    for (int j=0; j<cell.length(); j++)
            		                        {
            		                        String s = String.valueOf(cell.charAt(j));
            		                        if (s.equals("<")) 
            		                        	{
            		                        	if (have_something) break;
            		                        	append = false;
            		                        	}
            		                        else if (s.equals(">")) append = true;
            		                        else if (append) 
            		                        	{
            		                        	have_something = true;
            		                        	builder.append(s);
            		                        	}
            		                        }

            		                    String player_name = builder.toString();
		                				
		                				JSONObject player = new JSONObject();
		                				
		                				player.put("name", player_name);
		                				player.put("odds", player_odds);
		                				
		                				player_odds_array.put(player);
		                				}
	                				else break;
	                				}
	                			}
	                		}
	                	}
	                }
	            }

			output.put("player_odds_array", player_odds_array);
			
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	
   private static String get_html(String url_string) throws Exception
       {
       URL url = new URL(url_string);
       URLConnection conn;
       conn = url.openConnection();
       BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
      
       String line;
       StringBuilder builder = new StringBuilder();
      
       while ((line = rd.readLine()) != null) builder.append(line);

       rd.close();
       return builder.toString();
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
  
   @SuppressWarnings("unused")
   private static int number_of_cells_in_row(String row)
       {
       int
      
       number_of_cells = 0,
       cell_close_index = 0;
      
       while (cell_close_index != -1)
           {
           cell_close_index = row.indexOf("</td>");
           if (cell_close_index > 0)
               {
               row = row.substring(cell_close_index + 1, row.length());
               number_of_cells++;
               }
           }
      
       return number_of_cells;
       }
   
   	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue( Map<K, V> map )
	    {
	    List<Map.Entry<K, V>> list = new LinkedList<Map.Entry<K, V>>( map.entrySet() );
	    Collections.sort( list, new Comparator<Map.Entry<K, V>>()
	        {
	        public int compare( Map.Entry<K, V> o1, Map.Entry<K, V> o2 )
	            {
	            return ( o2.getValue() ).compareTo( o1.getValue() );
	            }
	        } );
	
	    Map<K, V> result = new LinkedHashMap<K, V>();
	    for (Map.Entry<K, V> entry : list)
	        {
	        result.put( entry.getKey(), entry.getValue() );
	        }
	    return result;
	    }
	}