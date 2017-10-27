package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;

public class CheckPendingWithdrawals extends Utils
	{
	public CheckPendingWithdrawals() // called by hourly cron
		{
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			
			DB db = new DB(sql_connection);

			Statement statement = sql_connection.createStatement();
			statement.execute("lock tables user write, transaction write");
			
			try {
				lock : {
					PreparedStatement select_cgs_addresses = sql_connection.prepareStatement("select distinct from_account from transaction where trans_type = 'BTC-WITHDRAWAL' and pending_flag = 1");
					ResultSet result_set = select_cgs_addresses.executeQuery();
					
					// all promos in result set have expired and have not yet been cancelled
					
					while (result_set.next())
						{
						String user_id = result_set.getString(1);
						
						JSONObject user = db.select_user("id", user_id);
						
						String cgs_address = user.getString("cgs_address");
						
						// CallCGS ----------------------------------------------------------------------------
						
						JSONObject rpc_call = new JSONObject();
						
						rpc_call.put("method", "getBalance");
						
						JSONObject rpc_method_params = new JSONObject();

						rpc_method_params.put("address", cgs_address);
						rpc_method_params.put("type", "btc");
						
						rpc_call.put("params", rpc_method_params);
						
						CallCGS call = new CallCGS(rpc_call);
						
						JSONObject 
						
						result = call.get_result(),
						balance = null;
						
						if (result != null) 
							{
							log(result.toString());
							balance = result.getJSONObject("balance");
							}
						else
							{
							log(call.get_error().toString());
							break lock;
							}

						// end CallCGS ------------------------------------------------------------------------
						
						double 
						
						cgs_last_balance = user.getDouble("cgs_last_balance"),
						cgs_current_balance = balance.getDouble("bitcoin_cnf");
						
						if (cgs_current_balance < cgs_last_balance)
							{
							log("Withdrawal confirmed for " + user.getString("username"));
							
							db.update_cgs_balance(user_id, cgs_current_balance);
							
							PreparedStatement update_transactions = sql_connection.prepareStatement("update transaction set pending_flag = 0 where trans_type = 'BTC-WITHDRAWAL' and from_account = ?");
							update_transactions.setString(1, user_id);
							update_transactions.executeUpdate();
							}
						}
					}
				}
			catch (Exception e)
				{
				Server.exception(e);
				}
			finally
				{
				statement.execute("unlock tables");
				}
			
			}
		catch (Exception e)
			{
			Server.exception(e);
			}
		finally
			{
			if (sql_connection != null)
				{
				try {sql_connection.close();} 
				catch (SQLException ignore) {}
				}
			}
		}
	}
