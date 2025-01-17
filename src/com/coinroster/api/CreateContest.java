package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Server;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.BuildLobby;
import com.coinroster.internal.NotifyAdmin;
import com.mysql.jdbc.Statement;

/**
 * Creates a new roster or pari-mutuel contest.
 * 
 * @custom.access standard
 *
 */
public class CreateContest extends Utils
	{
	public static String method_level = "standard";
	
	/**
	 * Creates a new roster or pari-mutuel contest.
	 * 
	 * @param method.input.category Contest category
	 * @param method.input.sub_category Contest sub-category
	 * @param method.input.contest_type Contest type (e.g. roster, pari-mutuel)
	 * @param method.input.progressive Progressive attached to the contest (if any)
	 * @param method.input.title Contest title
	 * @param method.input.description Contest description
	 * @param method.input.rake Percentage of funds to be raked for profit
	 * @param method.input.cost_per_entry Cost per user entry
	 * @param method.input.registration_deadline Deadline to register for contest
	 * @param method.input.option_table List of mappings between option and amount
	 * @param method.input.private True if contest is private
	 * @param method.input.settlement_type Settlement type of contest (e.g. user-settled, crowd-settled)
	 * @param method.input.settlement_deadline Deadline to settle contest by
	 * @param method.input.scoring_rules Exclusive to props
	 * @param method.input.prop_data Any additional data that props require
	 * @param method.input.game_IDs Any specific game IDs, if provided
	 * @throws Exception
	 */
	public CreateContest(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		boolean internal_caller = method.internal_caller;
		
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
            
            boolean is_private = false;
            boolean is_fixed_odds = false;
            
            try{
            	is_private = input.getBoolean("private");
            }catch(Exception e){
            }
            
            boolean is_admin = false;
			if (session != null){
				is_admin = session.user_level() != null && session.user_level().equals("1"); 
			}

			int contest_id = 0;
			double risk = 0;
            String settlement_type = input.getString("settlement_type");
            PreparedStatement create_contest = null;
            Long settlement_deadline = null;
            try{		
            	settlement_deadline = input.getLong("settlement_deadline");		
            }catch(Exception e){
            }
            
            String madeBy = "";
			String scoring_rules, prop_data;
			try{
				scoring_rules = input.getString("scoring_rules").toString();
				log("scoring rules: " + scoring_rules);
			}catch(Exception e){
				scoring_rules = "";
			}
			try{
				prop_data = input.getString("prop_data");
				log("prop data: " + prop_data);
			}catch(Exception e){
				e.printStackTrace(System.out);
				prop_data = "";
			}
			
			if (prop_data.contains("risk")) is_fixed_odds = true;
            
			String gameIDs = null;
			if(input.has("gameIDs"))
				gameIDs = input.getString("gameIDs");
			
            // validate common fields
            
            if (title.length() > 255)
            	{
            	output.put("error", "Title is too long");
                break method;
            	}

            if(settlement_deadline != null){
	            if (settlement_deadline - registration_deadline < 1 * 60 * 60 * 1000 && category != "FINANCIAL" )
	            	{
	            	output.put("error", "Settlement deadline must be at least 1 hour from resgistration deadline");
	                break method;
	            	}
            }
            
            if (registration_deadline - System.currentTimeMillis() < 1 * 60 * 60 * 1000 && category != "FINANCIAL")
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
            log("settlement_type: " + settlement_type);
            log("scoring_rules: " + scoring_rules);
            log("prop_data: " + prop_data);
            log("private: " + is_private);
            
            // if private, generate hash for unique url and add user as a participant
            JSONObject participants = new JSONObject();
            if (is_private) {
            	JSONArray users = new JSONArray();
            	users.put(session.user_id());
            	
            	String code = Utils.SHA1(title + registration_deadline).substring(0, 8);
            	
            	participants.put("code", code);
            	participants.put("users", users);
            }
            if (contest_type.equals("ROSTER"))
	            {
	            int salary_cap = input.getInt("salary_cap");
	            int min_users = input.getInt("min_users");
	            int max_users = input.getInt("max_users"); // 0 = unlimited
	            int entries_per_user = input.getInt("entries_per_user");
	            int roster_size = input.getInt("roster_size");
	            String score_header = input.getString("score_header");
	            JSONArray pay_table = new JSONArray(input.getString("pay_table"));
	            JSONArray pay_table_final = new JSONArray();
	            
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
	            
	            create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, progressive, contest_type, title, description, registration_deadline, "
	            		+ "rake, cost_per_entry, settlement_type, min_users, max_users, entries_per_user, pay_table, salary_cap, option_table, created, created_by, roster_size, "
	            		+ "odds_source, score_header, gameIDs, scoring_rules, settlement_deadline, status, prop_data, participants) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
	            		Statement.RETURN_GENERATED_KEYS);				


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
				
				if(session == null){
					madeBy = "ContestBot";
					create_contest.setNull(24, java.sql.Types.BIGINT);
					create_contest.setInt(25, 1);
				}
				else{
					madeBy = session.user_id();
					if (settlement_type.equals("USER-SETTLED") || settlement_type.equals("CROWD-SETTLED")) {
						
						create_contest.setLong(24, settlement_deadline);
		            	
						// exclude admins from seeking approval
		            	if(is_admin) 
		            		create_contest.setInt(25, 1);            		
		            	else
		            		create_contest.setInt(25, 5);     
					}
					else{
						create_contest.setInt(25, 1);     
						create_contest.setNull(24, java.sql.Types.BIGINT);
					}					
				}
				
				create_contest.setString(18, madeBy);
				create_contest.setInt(19, roster_size);
				create_contest.setString(20, odds_source);
				create_contest.setString(21, score_header);
				create_contest.setString(22, gameIDs);
				create_contest.setString(23, scoring_rules);
				create_contest.setString(26, prop_data);

				if (is_private) {
					create_contest.setString(27, participants.toString());
				} else {
					create_contest.setNull(27, java.sql.Types.VARCHAR);
				}
				
	            create_contest.executeUpdate();
	            
            	
            	ResultSet rs = create_contest.getGeneratedKeys();

            	if(rs.next()) {
            		output.put("contest_id", rs.getInt(1));
            		contest_id = rs.getInt(1);
            	} else {
            		log("Generated keys query failed");
            	}

	        }
            else if (contest_type.equals("PARI-MUTUEL"))
            	{
            	
            	// check if user has enough to cover risk if fixed odds
            	
            	if(is_fixed_odds) {
            		JSONObject prop_data_json = new JSONObject(prop_data); 
            		risk = prop_data_json.getDouble("risk");
            		log("risk: " + risk);
            		if (risk > db.select_user("id", session.user_id()).getDouble("btc_balance")) {
            			log("Risk exceeds user funds");
            			output.put("error", "Risk exceeds total balance");
            			break method;
            		}
            		
            		/*// adjust amount_left by rake
            		prop_data_json.put("amount_left", format_btc(subtract(risk, multiply(risk, rake, 0), 0)));*/
            		
            		prop_data_json.put("amount_left", risk);
            		prop_data = prop_data_json.toString();
            	}
            	
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
						log(output.get("error"));
	            		break method;
						}

					if (is_fixed_odds) {
						if (line.getDouble("odds") < 1) {
							output.put("error", "Odds must be 1 or greater");
							break method;
						}
						
						/*
						// Odds adjusted for risk
						double odds = line.getDouble("odds");
						odds = multiply(odds, subtract(1, rake, 0), 0);
						line.remove("odds");
						line.put("odds", odds);
						log(String.format("odds for option %d : %f", line.getInt("id"), odds));
						*/
					}
					
					last_id = id;
					
					String option_description = line.getString("description");
					
					if (option_description.equals(""))
						{
						output.put("error", "Option " + id + " has no description");
						log(output.get("error"));
	            		break method;
						}
					}
            	
            	create_contest = sql_connection.prepareStatement("insert into contest(category, sub_category, progressive, contest_type, title, "
            																		+ "description, registration_deadline, rake, cost_per_entry, settlement_type, "
            																		+ "option_table, created, created_by, auto_settle, status, settlement_deadline, "
            																		+ "scoring_rules, prop_data, participants, min_users, gameIDs) "
            																		+ "values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				

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
				int auto = 0;
				
				// contestBot made contest
				if(session == null){
					madeBy = "ContestBot";
					auto=1;
					create_contest.setInt(15, 1);
					create_contest.setNull(16, java.sql.Types.BIGINT);
					
				}

				else{
					madeBy = session.user_id();
					
					// auto_settle is set in SetupPropBet api (user generated but auto-settle)
					int auto_settle = 0;
					try{
						auto_settle = input.getInt("auto_settle");
					}catch(Exception e){
					}
					if(auto_settle == 1){
						auto = 1;
						create_contest.setInt(15, 1);
						create_contest.setNull(16, java.sql.Types.BIGINT);
					}
					
					// user or crowd settlement
					else if (settlement_type.equals("USER-SETTLED") || settlement_type.equals("CROWD-SETTLED")) {
						
						create_contest.setLong(16, settlement_deadline);
		            	
						// exclude admins from seeking approval
		            	if(is_admin) 
		            		create_contest.setInt(15, 1);            		
		            	else
		            		create_contest.setInt(15, 5);     
					}
					
					// admin created contest
					else{
						create_contest.setInt(15, 1);     
						create_contest.setNull(16, java.sql.Types.BIGINT);
					}		
            	}
				create_contest.setString(13, madeBy);
				create_contest.setInt(14, auto);

				create_contest.setString(17, scoring_rules);
				create_contest.setString(18, prop_data);
				
				if (is_private) create_contest.setString(19, participants.toString());
				else create_contest.setNull(19, java.sql.Types.VARCHAR);
				
				if (is_fixed_odds) create_contest.setInt(20, 1);
				else create_contest.setInt(20,2);
				
				create_contest.setString(21, gameIDs);
				
            	create_contest.executeUpdate();
            	
            	ResultSet rs = create_contest.getGeneratedKeys();
            	if(rs.next()) {
            		output.put("contest_id", rs.getInt(1));
            		contest_id = rs.getInt(1);
            	} else {
            		log("Generated keys query failed");
            	}
            	
            	// take away risk amount as escrow
            	if (is_fixed_odds) {
            		
            		JSONObject contest_account = db.select_user("username", "internal_contest_asset");

            		db.update_btc_balance(contest_account.getString("user_id"), add(contest_account.getDouble("btc_balance"), risk, 0));
            		db.update_btc_balance(session.user_id(), subtract(db.select_user("id", session.user_id()).getDouble("btc_balance"), risk, 0));
            		
					String 
					
					transaction_type = "FIXED-ODDS-RISK-ESCROW",
					from_account = session.user_id(),
					to_account = contest_account.getString("user_id"),
					from_currency = "BTC",
					to_currency = "BTC",
					memo = "Risk amount escrow for fixed odds contest";
					
					PreparedStatement create_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, contest_id) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");				
					create_transaction.setLong(1, System.currentTimeMillis());
					create_transaction.setString(2, session.user_id());
					create_transaction.setString(3, transaction_type);
					create_transaction.setString(4, from_account);
					create_transaction.setString(5, to_account);
					create_transaction.setDouble(6, risk);
					create_transaction.setString(7, from_currency);
					create_transaction.setString(8, to_currency);
					create_transaction.setString(9, memo);
					create_transaction.setInt(10, contest_id);
					create_transaction.executeUpdate();
            	}
            }
            
            if (settlement_type.equals("USER-SETTLED") || settlement_type.equals("CROWD-SETTLED")) {
				String
				
				subject_admin = "User Generated Contest Created!",
				message_body_admin = "";
				
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "A user generated contest has been created!";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Contest ID: <b>" + contest_id + "</b>";
				message_body_admin += "Title: <b>" + title + "</b>";
				message_body_admin += "Description: <b>" + description + "</b>";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";
				message_body_admin += "Click <a href=\"" + Server.host + "/admin/contests.html#7\">here</a> to see pending contests.";
				message_body_admin += "<br/>";
				message_body_admin += "<br/>";

				new NotifyAdmin(sql_connection, subject_admin, message_body_admin);
            }

            new BuildLobby(sql_connection);
            
            if (is_private) {
            	output.put("code", participants.getString("code"));
            	String private_url = Server.host + "/contest.html?id=" + contest_id + "&code=" + participants.getString("code");
            	output.put("url", private_url);
            	
            	private_url += "&ref=" + session.username() + "&from=email";
            	JSONObject user = db.select_user("id", session.user_id());
            	
            	// send email that can be forwarded
            	String
            	username = user.getString("username"),
            	email_address = user.getString("email_address"),
            	
            	subject = "Private Contest: " + title,

            	message_body = "Hello <b>" + username + "</b>!";
            	message_body += "<br><br>";
            	message_body += "Thanks for creating a private contest: <a href='" + private_url + "'>" + title + "</a><br><br>";
            	message_body += "Forward this email to your friends so they can join your private contest with the above link!";
            	message_body += "<br/><br/>";
            	message_body += "If for whatever reason that link doesn't work, copy and paste the following URL into your browser:<br/>";
            	message_body += private_url;
            	message_body += "<br><br>";
            	message_body += "Best of luck, and thank you for playing CoinRoster!";

            	Server.send_mail(email_address, username, subject, message_body);

            }
            else{
                output.put("url", Server.host + "/contest.html?id=" + contest_id);
            }
            
            output.put("status", "1");
            output.put("contest_id", contest_id);
		} 
		
	if(session != null && !internal_caller)
		method.response.send(output);
	
	}
}
	
