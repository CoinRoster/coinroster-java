package com.coinroster.internal;

import java.sql.Connection;

public class PoolSettlement
	{
	public PoolSettlement(Connection sql_connection, int pool_id, String settlement_type) throws Exception
		{
		switch (settlement_type)
			{
			case "HEADS-UP" :
				
				// do settlement here
				
				new UpdatePoolStatus(sql_connection, pool_id, 3);
				
				break;

			case "DOUBLE-UP" :

				// do settlement here
				
				new UpdatePoolStatus(sql_connection, pool_id, 3);
				
				break;

			case "JACKPOT" :

				// do settlement here
				
				new UpdatePoolStatus(sql_connection, pool_id, 3);
				
				break;
				
			case "UNDER-SUBSCRIBED" :

				// do settlement here
				
				new UpdatePoolStatus(sql_connection, pool_id, 4);
				
				break;
				
			case "CANCELLED" :

				// do settlement here
				
				new UpdatePoolStatus(sql_connection, pool_id, 5);
				
				break;
				
			}
		}
	}
