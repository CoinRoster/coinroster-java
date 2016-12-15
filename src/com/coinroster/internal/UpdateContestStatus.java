package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UpdateContestStatus 
	{
	public UpdateContestStatus(Connection sql_connection, int contest_id, int new_status) throws Exception
		{
		PreparedStatement update_contest_status = sql_connection.prepareStatement("update contest set status = ? where id = ?");
		update_contest_status.setInt(1, new_status);
		update_contest_status.setInt(2, contest_id);
		update_contest_status.executeUpdate();
		}
	}
