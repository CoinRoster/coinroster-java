package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;

public class CreateContest extends Utils
	{
	public static String method_level = "standard";
	public CreateContest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);
		
		method : {
			
//------------------------------------------------------------------------------------
		
			// get common fields:
			String category = input.getString("category");
			String sub_category = input.getString("sub_category");
			String contest_type = input.getString("contest_type");
			String progressive_code = input.getString("progressive");
			String title = input.getString("title");
			String description = input.getString("description");
            double rake = input.getDouble("rake");
            double cost_per_entry = input.getDouble("cost_per_entry");
            Long registration_deadline = input.getLong("registration_deadline");
            JSONArray option_table = input.getJSONArray("option_table");
            PreparedStatement create_contest = null;
            Long settlement_deadline = null;
            
            // validate common fields
            
            if (title.length() > 255)
            	{
            	output.put("error", "Title is too long");
                break method;
            	}
            
            if (category.equals("USERGENERATED")) 
            	{
                settlement_deadline = input.getLong("settlement_deadline");
	            if (settlement_deadline - registration_deadline < 1 * 60 * 60 * 1000)
	            	{
	            	output.put("error", "Settlement deadline must be at least 1 hour from resgistration deadline");
	                break method;
	            	}
            	}
            
            if (registration_deadline - System.currentTimeMillis() < 1 * 60 * 60 * 1000)
	        	{
	        	output.put("error", "Registration deadline must be at least 1 hour from now");
	            break method;
	        	}
            
            if (rake < 0 || rake >= 100)
            	{
                output.put("error", "Rake cannot be < 0 or > 100");
                break method;
            	}
            
            rake = divide(rake, 100, 0); // convert to %
            
            if (cost_per_entry == 0)
            	{
            	output.put("error", "Cost per entry cannot be 0");
                break method;
            	}
            
            if (progressive_code.equals("")) progressive_code = null; // default value
            else
            	{
            	JSONObject progressive = db.select_progressive(progressive_code);
            	
            	if (progressive == null)
            		{
            		output.put("error", "Invalid Progressive");
                    break method;
            		}
            	
            	if (!progressive.getString("category").equals(category) || !progressive.getString("sub_category").equals(sub_category))
            		{
            		output.put("error", "Progressive belongs to a different category");
                    break method;
            		}
            	}

            log("Contest parameters:");
            
            log("category: " + category);
            log("sub_category: " + sub_category);
            log("progressive: " + progressive_code);
            log("contest_type: " + contest_type);
            log("title: " + title);
            log("description: " + description);
            log("registration_deadline: " + registration_deadline);
            log("rake: " + rake);
            log("cost_per_entry: " + cost_per_entry);
            
            if (contest_type.equals("ROSTER"))
	            {
    			String settlement_type = input.getString("settlement_type");
	            int salary_cap = input.getInt("salary_cap");
	            int min_users = input.getInt("min_users");
	            int max_users = input.getInt("max_users"); // 0 = unlimited
	            int entries_per_user = input.getInt("entries_per_user");
	            int roster_size = input.getInt("roster_size");
	            String score_header = input.getString("score_header");
	            JSONArray pay_table = input.getJSONArray("pay_table");
	            JSONArray pay_table_final = new JSONArray();
	            String tourneyID;
	            try{
	            	tourneyID = input.getString("tourneyID");
	            }
	            catch(Exception e){
	            	tourneyID = "";
	            }
	            	            
	            if (min_users < 2)
	            	{
	        		output.put("error", "Invalid value for [min users]");
	        		break method;
	            	}
	            if (max_users < min_users && max_users != 0) // 0 = unlimited
		        	{
		    		output.put("error", "Invalid value for [max users]");
		    		break method;
		        	}
	            
	            if (roster_size < 0)
	            	{
	            	output.put("error", "Roster size cannobe be negative");
	        		break method;
	            	}
	            
	            if (entries_per_user < 0)
	            	{
	            	output.put("error", "Invalid value for [entries per user]");
	        		break method;
	            	}
	            
				if (score_header.equals(""))
					{
					output.put("error", "Please choose a score column header");
					break method;
					}
	            
	            // validate settlement instructions
	            switch (settlement_type)
	            	{
	            	case "HEADS-UP":
	            		            		
	            		if (min_users != 2 || max_users != 2)
	            			{
	            			output.put("error", "Invalid value(s) for number of users");
	                		break method;
	            			}
	            		break;
	            		
	            	case "DOUBLE-UP": break;
	            		
	            	case "JACKPOT":
	            		
	            		try {
	            			int number_of_lines = pay_table.length();
	            			if (number_of_lines < 3)
	            				{
	            				output.put("error", "Pay table must have at least 3 ranks");
	                    		break method;
	            				}
	            			
	            			int last_rank = 0;
	            			double total_payout = 0;
	            				            			
	            			for (int i=0; i<pay_table.length(); i++)
	            				{
	            				JSONObject line = pay_table.getJSONObject(i);
	            				int rank = line.getInt("rank");
	            				if (rank - last_rank != 1)
	            					{
	            					output.put("error", "Invalid ranks in pay table");
	                        		break method;
	            					}
	            				last_rank = rank;
	            				
	            				double payout = line.getDouble("payout");
	            				if (payout == 0 || payout >= 100)
	            					{
	            					output.put("error", "Pay table rank " + rank + ": payout cannot be " + payout);
	                        		break method;
	            					}
	            				
	            				total_payout = add(total_payout, payout, 0);
	            				
	            				payout = divide(payout, 100, 0);
	            				
	            				JSONObject new_line = new JSONObject();
	            				
	            				new_line.put("rank", rank);
	            				new_line.put("payout", payout);
	            				
	            				pay_table_final.put(new_line);
	            				}
	            			
	            			if (total_payout < 100)
	            				{
	            				output.put("error", "Pay table under-allocated: " + total_payout);
	                    		break method;
	            				}
	            			else if (total_payout > 100)
		        				{
		        				output.put("error", "Pay table over-allocated: " + total_payout);
		                		break method;
		        				}
	            			}
	            		catch (Exception e)
	            			{
	            			
	            			output.put("error", "Invalid pay table");
	                		break method;
	            			}
	            		break;
	            		
	            	default:
	            		
	            		output.put("error", "Invalid value for [settlement type]");
	            		break method;
	            		
	            	}

	            // validate player table
	            try {
		            for (int i=0; i<option_table.length(); i++)
						{
						JSONObject line = option_table.getJSONObject(i);
						
						String 
						
						name = line.getString("name").trim();
						//odds = line.getString("odds");
												
						if (name.equals("") || name == null)
							{
							output.put("error", "Player table row " + (i+1) + ": no name entered");
	                		break method;
							}
						/*if (odds == "" || odds == null)
							{
							output.put("error", "Player table row " + (i+1) + ": no odds entered");
		            		break method;
							}*/
						
						int price = line.getInt("price"); // force price to integer
						
						if (price == 0)
							{
							output.put("error", "Player table row " + (i+1) + ": invalid odds or price");
							log(output.get("error").toString());
		            		break method;
							}
						if (price > salary_cap)
							{
							output.put("error", "Player table row " + (i+1) + ": price cannot be greater than salary cap");
							log(output.get("error").toString());
		            		break method;
							}

						line.put("name", name);
						line.put("price", price);
						line.put("count", 0);
						
						option_table.put(i, line);
						}
		            }
	    		catch (Exception e)
	    			{
	    			output.put("error", "Invalid player table");
	    			log(output.get("error").toString());
	        		break method;
	    			}
	
				String odds_source = input.getString("odds_source");
	            
	            log("settlement_type: " + settlement_type);
	            log("min_users: " + min_users);
	            log("max_users: " + max_users);
	            log("entries_per_user: " + entries_per_user);
	            log("roster_size: " + roster_size);
	            log("salary_cap: " + salary_cap);
	            //log("pay_table: " + pay_table);
	            //log("option_table: " + option_table);
	            
				create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, progressive, contest_type, title, description, registration_deadline, rake, cost_per_entry, settlement_type, min_users, max_users, entries_per_user, pay_table, salary_cap, option_table, created, created_by, roster_size, odds_source, score_header, gameIDs) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				create_contest.setString(1, category);
				create_contest.setString(2, sub_category);
				create_contest.setString(3, progressive_code);
				create_contest.setString(4, contest_type);
				create_contest.setString(5, title);
				create_contest.setString(6, description);
				create_contest.setLong(7, registration_deadline);
				create_contest.setDouble(8, rake);
				create_contest.setDouble(9, cost_per_entry);
				create_contest.setString(10, settlement_type);
				create_contest.setInt(11, min_users);
				create_contest.setInt(12, max_users);
				create_contest.setInt(13, entries_per_user);
				create_contest.setString(14, pay_table_final.toString());
				create_contest.setDouble(15, salary_cap);
				create_contest.setString(16, option_table.toString());

				create_contest.setLong(17, System.currentTimeMillis());
				String madeBy = null;
				String gameIDs = "";
				if(session == null){
					madeBy = "ContestBot";
					gameIDs = tourneyID;
				}
				else
					madeBy = session.user_id();
				
				create_contest.setString(18, madeBy);
				create_contest.setInt(19, roster_size);
				create_contest.setString(20, odds_source);
				log(odds_source);
				create_contest.setString(21, score_header);
				log(score_header);
				create_contest.setString(22, gameIDs);
	            }
            else if (contest_type.equals("PARI-MUTUEL"))
            	{
            	// validate option table:
            	
            	int number_of_options = option_table.length();
            	
            	if (number_of_options < 2)
            		{
            		output.put("error", "There must be 2 or more options");
            		break method;
            		}
            	
            	int last_id = 0;
            	
            	for (int i=0; i<number_of_options; i++)
					{
					JSONObject line = option_table.getJSONObject(i);
					
					int id = line.getInt("id");
					if (id - last_id != 1)
						{
						output.put("error", "Invalid ids in option table");
	            		break method;
						}
					last_id = id;
					
					String option_description = line.getString("description");
					
					if (option_description.equals(""))
						{
						output.put("error", "Option " + id + " has no description");
	            		break method;
						}
					}
            	
            	String settlement_type = "PARI-MUTUEL";
            	
            	create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, progressive, contest_type, title, description, registration_deadline, rake, cost_per_entry, settlement_type, option_table, created, created_by, auto_settle, status, settlement_deadline) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
				create_contest.setString(1, category);
				create_contest.setString(2, sub_category);
				create_contest.setString(3, progressive_code);
				create_contest.setString(4, contest_type);
				create_contest.setString(5, title);
				create_contest.setString(6, description);
				create_contest.setLong(7, registration_deadline);
				create_contest.setDouble(8, rake);
				create_contest.setDouble(9, cost_per_entry);
				create_contest.setString(10, settlement_type);
				create_contest.setString(11, option_table.toString());
				create_contest.setLong(12, System.currentTimeMillis());
				String madeBy = "";
				int auto = 0;
				if(session == null){
					madeBy = "ContestBot";
					auto=1;
				}
				else
					madeBy = session.user_id();
				create_contest.setString(13, madeBy);
				create_contest.setInt(14, auto);
            	}

            if (category.equals("USERGENERATED")) {
            	create_contest.setInt(15, 5);
            	create_contest.setLong(16, settlement_deadline);
    			create_contest.executeUpdate();
            } else {
            	create_contest.setString(15, "NULL");
            	create_contest.setString(16, "NULL");
            	create_contest.executeUpdate();
    			new BuildLobby(sql_connection);
            }
			
            output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
			
		}
	}