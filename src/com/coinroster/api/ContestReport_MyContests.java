package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;
import com.coinroster.internal.UpdateUserContestStatus;

public class ContestReport_MyContests extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public ContestReport_MyContests(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
//------------------------------------------------------------------------------------

			// get all contest IDs where 
			// 1) user has entries 
			// 2) contest status is either open for registration or in play
			// sort by
			// 1) contest status ascending (open for registration, then in play)
			// 2) entry id descending (show most recent entries first)
			
			String user_id = session.user_id();
			
			int contest_status = input.getInt("contest_status");
			
			new UpdateUserContestStatus(user_id, contest_status);
			
			PreparedStatement select_contest_ids = null;
			switch (contest_status)
				{
				case 1 : // open
					select_contest_ids = sql_connection.prepareStatement("select distinct(contest_id) from entry inner join contest on entry.contest_id = contest.id where entry.user_id = ? and contest.status = 1 order by contest.registration_deadline asc");
					select_contest_ids.setString(1, user_id);
					break;
				case 2 : // in play
					select_contest_ids = sql_connection.prepareStatement("select distinct(contest_id) from entry inner join contest on entry.contest_id = contest.id where entry.user_id = ? and contest.status = 2 order by contest.registration_deadline desc");
					select_contest_ids.setString(1, user_id);
					break;
				case 3 : // settled
					select_contest_ids = sql_connection.prepareStatement("select distinct(contest_id) from entry inner join contest on entry.contest_id = contest.id where entry.user_id = ? and contest.status = 3 order by contest.settled desc");
					select_contest_ids.setString(1, user_id);
					break;
				}
			ResultSet contest_id_rs = select_contest_ids.executeQuery();

			JSONArray user_contest_report = new JSONArray();
			
			while (contest_id_rs.next())
				{
				int contest_id = contest_id_rs.getInt(1);
				
				JSONObject contest = db.select_contest(contest_id);
				
				String category = contest.getString("category");
				String sub_category = contest.getString("sub_category");
				String contest_type = contest.getString("contest_type");
				String title = contest.getString("title");
				String settlement_type = contest.getString("description");
				double cost_per_entry = contest.getDouble("cost_per_entry");
				Long registration_deadline = contest.getLong("registration_deadline");
				int status = contest.getInt("status");
				
				PreparedStatement get_user_wagers = sql_connection.prepareStatement("select sum(amount) from transaction where contest_id = ? and from_account = ? and (trans_type = 'BTC-CONTEST-ENTRY' or trans_type = 'RC-CONTEST-ENTRY' or trans_type = 'BTC-ADJUSTMENT')");
				get_user_wagers.setInt(1, contest_id);
				get_user_wagers.setString(2, user_id);
				ResultSet user_wagers_rs = get_user_wagers.executeQuery();
				user_wagers_rs.next();

				PreparedStatement get_user_winnings = sql_connection.prepareStatement("select sum(amount) from transaction where contest_id = ? and to_account = ? and (trans_type = 'BTC-CONTEST-WINNINGS' or trans_type = 'BTC-ADJUSTMENT')");
				get_user_winnings.setInt(1, contest_id);
				get_user_winnings.setString(2, user_id);
				ResultSet user_winnings_rs = get_user_winnings.executeQuery();
				user_winnings_rs.next();
				
				double 
				
				user_wagers = user_wagers_rs.getDouble(1),
				user_winnings = user_winnings_rs.getDouble(1),
				user_entries_count = divide(user_wagers, cost_per_entry, 0);
				
				JSONObject contest_item = new JSONObject();
				
				contest_item.put("id", contest_id);
				contest_item.put("category", category);
				contest_item.put("sub_category", sub_category);
				contest_item.put("contest_type", contest_type);
				contest_item.put("category_description", db.get_category_description(category));
				contest_item.put("sub_category_description", db.get_sub_category_description(sub_category));
				contest_item.put("title", title);
				contest_item.put("settlement_type", settlement_type);
				contest_item.put("cost_per_entry", cost_per_entry);
				contest_item.put("registration_deadline", registration_deadline);
				contest_item.put("user_entries_count", user_entries_count);
				contest_item.put("user_wagers", user_wagers);
				contest_item.put("user_winnings", user_winnings);
				contest_item.put("status", status);
				
				user_contest_report.put(contest_item);
				}
			
			output.put("user_contest_report", user_contest_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}