package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class UpdatePoolStatus 
	{
	public UpdatePoolStatus(Connection sql_connection, int pool_id, int new_status) throws Exception
		{
		PreparedStatement update_pool_status = sql_connection.prepareStatement("update pool set status = ? where id = ?");
		update_pool_status.setInt(1, new_status);
		update_pool_status.setInt(2, pool_id);
		update_pool_status.executeUpdate();
		}
	}
