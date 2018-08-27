package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONObject;
import com.coinroster.MethodInstance;
import com.coinroster.Utils;

public class GetSiteStats extends Utils
	{
	public static String method_level = "guest";
	@SuppressWarnings("unused")
	public GetSiteStats(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		output = method.output;
		
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
			
			JSONObject stats = new JSONObject();
			ResultSet rs = null;
			PreparedStatement ps = null;
			
			// Number of users
			int num_users = 0;
			ps  = sql_connection.prepareStatement("select count(id) from user");
			rs = ps.executeQuery();
			if(rs.next()){
				num_users = rs.getInt(1);
				stats.put("num_users", num_users);
			}
			
			// Number of bets placed
			int num_bets = 0;
			ps  = sql_connection.prepareStatement("SELECT COUNT( id ) FROM transaction "
												+ "WHERE trans_type IN ('BTC-CONTEST-ENTRY',  'RC-CONTEST-ENTRY')");
			rs = ps.executeQuery();
			if(rs.next()){
				num_bets = rs.getInt(1);
				stats.put("num_bets", num_bets);
			}
			
			// Amount of BTC bet
			double btc_bet = 0.0;
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'BTC-CONTEST-ENTRY'");
			rs = ps.executeQuery();
			if(rs.next()){
				btc_bet = rs.getDouble(1);
				stats.put("btc_bet", btc_bet);
			}
			
			// Amount of RC bet
			double rc_bet = 0.0;
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'RC-CONTEST-ENTRY'");
			rs = ps.executeQuery();
			if(rs.next()){
				rc_bet = rs.getDouble(1);
				stats.put("rc_bet", rc_bet);
			}
			
			// Amount of BTC won
			double btc_won = 0.0;
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'BTC-CONTEST-WINNINGS'");
			rs = ps.executeQuery();
			if(rs.next()){
				btc_won = rs.getDouble(1);
				stats.put("btc_won", btc_won);
			}
			
			// Amount of RC won
			double rc_won = 0.0;
			ps  = sql_connection.prepareStatement("SELECT SUM( amount ) FROM transaction WHERE trans_type = 'RC-CONTEST-WINNINGS'");
			rs = ps.executeQuery();
			if(rs.next()){
				rc_won = rs.getDouble(1);
				stats.put("rc_won", rc_won);
			}
			
			// BTC bet for each sport 
			String[] sports = {"GOLF", "BASEBALL", "BASKETBALL", "HOCKEY"};
			for(int i = 0; i < sports.length; i++){
				String sport = sports[i];
				double bet = 0.0;
				ps  = sql_connection.prepareStatement("SELECT SUM(transaction.amount) FROM transaction "
													+ "INNER JOIN contest ON transaction.contest_id = contest.id "
													+ "WHERE contest.sub_category = ? AND transaction.trans_type = 'BTC-CONTEST-ENTRY'");
				ps.setString(1, sport);
				rs = ps.executeQuery();
				if(rs.next()){
					bet = rs.getDouble(1);
					String var_name = sport.toLowerCase() + "_btc_bet";
					stats.put(var_name, bet);
				}
			}
			
			// # contests settled
			int settled = 0;
			ps  = sql_connection.prepareStatement("SELECT COUNT(id) FROM contest WHERE status = 3");
			rs = ps.executeQuery();
			if(rs.next()){
				settled = rs.getInt(1);
				stats.put("contests_settled", settled);
			}
			
			// total transactions
			int total = 0;
			ps  = sql_connection.prepareStatement("SELECT COUNT(id) FROM transaction");
			rs = ps.executeQuery();
			if(rs.next()){
				total = rs.getInt(1);
				stats.put("total_transactions", total);
			}
						
			// user btc
			double btc = 0.0;
			ps  = sql_connection.prepareStatement("SELECT SUM(btc_balance) FROM user WHERE level = 0 or level = 1 or level = 3 ");
			rs = ps.executeQuery();
			if(rs.next()){
				btc = rs.getDouble(1);
				stats.put("user_btc", btc);
			}
			
			// user rc
			double rc = 0.0;
			ps  = sql_connection.prepareStatement("SELECT SUM(rc_balance) FROM user WHERE level = 0 or level = 1 or level = 3 ");
			rs = ps.executeQuery();
			if(rs.next()){
				rc = rs.getDouble(1);
				stats.put("user_rc", rc);
			}
			
			
			output.put("stats", stats);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}