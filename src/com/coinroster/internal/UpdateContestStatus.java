package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;

import com.coinroster.DB;

public class UpdateContestStatus 
	{
	public UpdateContestStatus(Connection sql_connection, int contest_id, int new_status) throws Exception
		{
		DB db = new DB(sql_connection);
		
		PreparedStatement update_contest_status = sql_connection.prepareStatement("update contest set status = ? where id = ?");
		update_contest_status.setInt(1, new_status);
		update_contest_status.setInt(2, contest_id);
		update_contest_status.executeUpdate();
		
		if (db.is_voting_contest(contest_id)) {
			PreparedStatement update_contest_status_voting = sql_connection.prepareStatement("update voting set status = ? where id = ?");
			update_contest_status_voting.setInt(1, new_status);
			update_contest_status_voting.setInt(2, contest_id);
			update_contest_status_voting.executeUpdate();
		}
		
		new BuildLobby(sql_connection);
		}
	}
