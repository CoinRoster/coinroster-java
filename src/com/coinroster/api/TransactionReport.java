package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.json.JSONArray;
import org.json.JSONObject;

import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class TransactionReport extends Utils
	{
	public static String method_level = "standard";
	@SuppressWarnings("unused")
	public TransactionReport(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		method : {
			
//------------------------------------------------------------------------------------
		
			// !! SECURITY !! this method can be called by an admin OR standard user
			
			// admin_panel is also a criterion for admin report so that admin users can experience the normal report from their account panes
			
			String request_source = input.getString("request_source");
			
			boolean is_admin = session.user_level().equals("1") && request_source.equals("admin_panel");

			Long
			
			start_date_ms = input.getLong("start_date_ms"),
			end_date_ms = input.getLong("end_date_ms");

			PreparedStatement select_transaction;
			
			if (is_admin) 
				{
				
				boolean ignore_riskescrow = input.getBoolean("include_riskescrow");
				if(ignore_riskescrow){
					select_transaction = sql_connection.prepareStatement("select * from transaction where created > ? and created < ? and trans_type != 'FIXED-ODDS-RISK-ESCROW'");
				}else{
					select_transaction = sql_connection.prepareStatement("select * from transaction where created > ? and created < ?");
				}
				
				select_transaction.setLong(1, start_date_ms);
				select_transaction.setLong(2, end_date_ms);
				}
			else 
				{
				select_transaction = sql_connection.prepareStatement("select * from transaction where cancelled_flag = 0 and created > ? and created < ? and (from_account = ? or to_account = ?)");
				select_transaction.setLong(1, start_date_ms);
				select_transaction.setLong(2, end_date_ms);
				select_transaction.setString(3, session.user_id());
				select_transaction.setString(4, session.user_id());
				}

			ResultSet result_set = select_transaction.executeQuery();
			
			JSONArray transaction_report = new JSONArray();
	   
			while (result_set.next())
				{
				String 
				
				transaction_id = result_set.getString(1),
				created = result_set.getString(2),
				created_by = result_set.getString(3),
				trans_type = result_set.getString(4),
				from_account = result_set.getString(5),
				to_account = result_set.getString(6),
				amount = result_set.getString(7),
				from_currency = result_set.getString(8),
				to_currency = result_set.getString(9),
				memo = result_set.getString(10),
				pending_flag = result_set.getString(11),
				ext_address = result_set.getString(12);
				
				int contest_id = result_set.getInt(13);
				int cancelled_flag = result_set.getInt(14);

				JSONObject transaction = new JSONObject();

				transaction.put("created", created);
				transaction.put("trans_type", trans_type);
				transaction.put("from_currency", from_currency);
				transaction.put("memo", memo);
				transaction.put("pending_flag", pending_flag);
				transaction.put("contest_id", contest_id);
				transaction.put("transaction_id", transaction_id);
				
				if (is_admin) 
					{
					transaction.put("created_by", created_by);
					transaction.put("from_account", from_account);
					transaction.put("to_account", to_account);
					transaction.put("amount", amount);
					transaction.put("to_currency", to_currency);
					transaction.put("cancelled_flag", cancelled_flag);
					}
				else
					{
					if (session.user_id().equals(to_account)) transaction.put("amount", "+" + amount);
					else transaction.put("amount", "-" + amount);
					}
				
				transaction_report.put(transaction);
				}
			
			output.put("transaction_report", transaction_report);
			output.put("status", "1");
			
//------------------------------------------------------------------------------------

			} method.response.send(output);
		}
	}