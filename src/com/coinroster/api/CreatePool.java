package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class CreatePool extends Utils
	{
	public static String method_level = "admin";
	@SuppressWarnings("unused")
	public CreatePool(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(method);

		method : {
			
//------------------------------------------------------------------------------------
		
            String
            
            category = input.getString("category"),
            sub_category = input.getString("sub_category"),
            title = input.getString("title"),
            description = input.getString("description"),
            settlement_type = input.getString("settlement_type"),
            entries_per_user_STRING = input.getString("entries_per_user");
            
            if (title.length() > 255)
            	{
            	output.put("error", "Title is too long");
                break method;
            	}
            
            Long registration_deadline = input.getLong("registration_deadline");
            
            if (registration_deadline - System.currentTimeMillis() < 12 * 60 * 60 * 1000)
            	{
            	output.put("error", "Registration deadline must be at least 12 hours from now");
                break method;
            	}
            
            double
            
            rake = input.getDouble("rake"),
            cost_per_entry = input.getDouble("cost_per_entry"),
            salary_cap = input.getDouble("salary_cap");

            if (rake < 0 || rake >= 100)
            	{
                output.put("error", "Rake cannot be < 0 or > 100");
                break method;
            	}
            
            rake = rake / 100; // convert to %
            
            if (cost_per_entry == 0)
            	{
            	output.put("error", "Cost per entry cannot be 0");
                break method;
            	}
            
            // validate cost_per_entry
            
            int
            
            min_users = input.getInt("min_users"),
            max_users = input.getInt("max_users"), // 0 = unlimited
            entries_per_user = 0;
            
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
            	
            switch (entries_per_user_STRING)
            	{
            	case "UNLIMITED" :
            		entries_per_user = 0; // 0 = unlimited
            		break;
            	case "ONE-ONLY" :
            		entries_per_user = 1;
            		break;
            	default:
            		output.put("error", "Invalid value for [entries per user]");
            		break method;
            	}

            JSONArray

            pay_table = input.getJSONArray("pay_table"),
            pay_table_final = new JSONArray(),
            odds_table = input.getJSONArray("odds_table");
            
            switch (settlement_type)
            	{
            	case "HEADS-UP":
            		if (min_users != 2 || max_users != 2)
            			{
            			output.put("error", "Invalid value(s) for number of users");
                		break method;
            			}
            		break;
            	case "DOUBLE-UP":
            		break;
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
            				total_payout += payout;
            				
            				payout = payout/100;
            				
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
            try {
	            for (int i=0; i<odds_table.length(); i++)
					{
					JSONObject line = odds_table.getJSONObject(i);
					
					String 
					
					name = line.getString("name"),
					odds = line.getString("odds");
					
					if (name == "" || name == null)
						{
						output.put("error", "Pay table row " + (i+1) + ": no name entered");
                		break method;
						}
					if (odds == "" || odds == null) // consider adding more validation here
						{
						output.put("error", "Pay table row " + (i+1) + ": no odds entered");
	            		break method;
						}
					
					double price = line.getDouble("price");
					
					if (price == 0)
						{
						output.put("error", "Pay table row " + (i+1) + ": invalid odds or price");
	            		break method;
						}
					if (price > salary_cap)
						{
						output.put("error", "Pay table row " + (i+1) + ": price cannot be greater than salary cap");
	            		break method;
						}
					}
	            }
    		catch (Exception e)
    			{
    			output.put("error", "Invalid odds table");
        		break method;
    			}
            
            log("Pool parameters:");
            
            log("category: " + category);
            log("sub_category: " + sub_category);
            log("title: " + title);
            log("description: " + description);
            log("registration_deadline: " + registration_deadline);
            log("rake: " + rake);
            log("cost_per_entry: " + cost_per_entry);
            log("settlement_type: " + settlement_type);
            log("min_users: " + min_users);
            log("max_users: " + max_users);
            log("entries_per_user: " + entries_per_user);
            log("pay_table: " + pay_table);
            log("salary_cap: " + salary_cap);
            log("odds_table: " + odds_table);
            
			PreparedStatement create_pool = sql_connection.prepareStatement("insert into pool(category, sub_category, title, description, registration_deadline, rake, cost_per_entry, settlement_type, min_users, max_users, entries_per_user, pay_table, salary_cap, odds_table, created, created_by) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
			create_pool.setString(1, category);
			create_pool.setString(2, sub_category);
			create_pool.setString(3, title);
			create_pool.setString(4, description);
			create_pool.setLong(5, registration_deadline);
			create_pool.setDouble(6, rake);
			create_pool.setDouble(7, cost_per_entry);
			create_pool.setString(8, settlement_type);
			create_pool.setInt(9, min_users);
			create_pool.setInt(10, max_users);
			create_pool.setInt(11, entries_per_user);
			create_pool.setString(12, pay_table_final.toString());
			create_pool.setDouble(13, salary_cap);
			create_pool.setString(14, odds_table.toString());
			create_pool.setLong(15, System.currentTimeMillis());
			create_pool.setString(16, session.user_id());
			create_pool.executeUpdate();
            
            output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}