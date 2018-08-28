package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;
import com.coinroster.MethodInstance;
import com.coinroster.Utils;

public class GetSiteStats extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public GetSiteStats(MethodInstance method) throws Exception 
		{
		JSONObject output = method.output;
	
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
			
			JSONArray all_stats = new JSONArray();
			ResultSet rs = null;
			PreparedStatement ps = null;
			JSONObject stat = new JSONObject();
			
			// Number of users
			int num_users = 0;
			ps  = sql_connection.prepareStatement("select count(id) from user");
			rs = ps.executeQuery();
			if(rs.next()){
				num_users = rs.getInt(1);
				stat.put("name", "Number of Users");
				stat.put("value", num_users);
				all_stats.put(stat);
			}
			
			// Number of bets placed
			int num_bets = 0;
			ps  = sql_connection.prepareStatement("SELECT COUNT( id ) FROM transaction "
												+ "WHERE trans_type IN ('BTC-CONTEST-ENTRY',  'RC-CONTEST-ENTRY')");
			rs = ps.executeQuery();
			stat = new JSONObject();
			if(rs.next()){
				num_bets = rs.getInt(1);
				stat.put("name", "Number of Bets Placed");
				stat.put("value", num_bets);
				all_stats.put(stat);
			}
			
			// Amount of BTC bet
			double btc_bet = 0.0;
			stat = new JSONObject();

			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'BTC-CONTEST-ENTRY'");
			rs = ps.executeQuery();
			if(rs.next()){
				btc_bet = rs.getDouble(1);
				stat.put("name", "Amount of BTC Bet");
				stat.put("value", btc_bet);
				all_stats.put(stat);
			}
			
			// Amount of RC bet
			double rc_bet = 0.0;
			stat = new JSONObject();
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'RC-CONTEST-ENTRY'");
			rs = ps.executeQuery();
			if(rs.next()){
				rc_bet = rs.getDouble(1);
				stat.put("name", "Amount of RC Bet");
				stat.put("value", rc_bet);
				all_stats.put(stat);
			}
			
			// Amount of BTC won
			double btc_won = 0.0;
			stat = new JSONObject();
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'BTC-CONTEST-WINNINGS'");
			rs = ps.executeQuery();
			if(rs.next()){
				btc_won = rs.getDouble(1);
				stat.put("name", "Amount of BTC Won");
				stat.put("value", btc_won);
				all_stats.put(stat);
			}
			
			// Amount of RC won
			double rc_won = 0.0;
			stat = new JSONObject();
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'RC-CONTEST-WINNINGS'");
			rs = ps.executeQuery();
			if(rs.next()){
				rc_won = rs.getDouble(1);
				stat.put("name", "Amound of RC Won");
				stat.put("value", rc_won);
				all_stats.put(stat);
			}
			
			// BTC bet for each sport 
			String[] sports = {"GOLF", "BASEBALL", "BASKETBALL", "HOCKEY"};
			for(int i = 0; i < sports.length; i++){
				String sport = sports[i];
				double bet = 0.0;
				stat = new JSONObject();
				ps  = sql_connection.prepareStatement("SELECT SUM(transaction.amount) FROM transaction "
													+ "INNER JOIN contest ON transaction.contest_id = contest.id "
													+ "WHERE contest.sub_category = ? AND transaction.trans_type = 'BTC-CONTEST-ENTRY'");
				ps.setString(1, sport);
				rs = ps.executeQuery();
				if(rs.next()){
					bet = rs.getDouble(1);
					String name = "Amount of BTC Bet on " + sport + " Contests";
					stat.put("value", bet);
					stat.put("name", name);
					all_stats.put(stat);
				}
			}
			
			// # contests settled
			int settled = 0;
			ps  = sql_connection.prepareStatement("SELECT COUNT(id) FROM contest WHERE status = 3");
			rs = ps.executeQuery();
			stat = new JSONObject();
			if(rs.next()){
				settled = rs.getInt(1);
				stat.put("name", "Number of Contests Settled");
				stat.put("value", settled);
				all_stats.put(stat);
			}
			
			// total transactions
			int total = 0;
			ps  = sql_connection.prepareStatement("SELECT COUNT(id) FROM transaction");
			rs = ps.executeQuery();
			stat = new JSONObject();
			if(rs.next()){
				total = rs.getInt(1);
				stat.put("name", "Total Number of Transactions");
				stat.put("value", total);
				all_stats.put(stat);
			}
						
			// user btc
			double btc = 0.0;
			stat = new JSONObject();
			ps  = sql_connection.prepareStatement("SELECT SUM(btc_balance) FROM user WHERE level = 0 or level = 1 or level = 3 ");
			rs = ps.executeQuery();
			if(rs.next()){
				btc = rs.getDouble(1);
				stat.put("name", "Amount of BTC on the Site");
				stat.put("value", btc);
				all_stats.put(stat);
			}
			
			// user rc
			double rc = 0.0;
			stat = new JSONObject();
			ps  = sql_connection.prepareStatement("SELECT SUM(rc_balance) FROM user WHERE level = 0 or level = 1 or level = 3 ");
			rs = ps.executeQuery();
			if(rs.next()){
				rc = rs.getDouble(1);
				stat.put("name", "Amount of RC on the Site");
				stat.put("value", rc);
				all_stats.put(stat);
			}
		
			output.put("stats", all_stats);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}