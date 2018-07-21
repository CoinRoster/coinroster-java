package com.coinroster.api;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.MethodInstance;
import com.coinroster.Session;
import com.coinroster.Utils;

public class AddToProgressive extends Utils
	{
	public static String method_level = "standard";
	public AddToProgressive(MethodInstance method) throws Exception 
		{
		JSONObject 
		
		input = method.input,
		output = method.output;
		
		Session session = method.session;
		
		Connection sql_connection = method.sql_connection;

		DB db = new DB(sql_connection);

		method : {
			
			JSONObject 
			
			internal_promotions = db.select_user("username", "internal_promotions"),			
			internal_progressive = db.select_user("username", "internal_progressive");
			
			String 

			created_by = null, // session.user_id(),
			code = input.getString("code"),
			internal_promotions_id = db.get_id_for_username("internal_promotions"),
			internal_progressive_id = db.get_id_for_username("internal_progressive"),
			ext_address = "",
			from_currency = "BTC",
			to_currency = "BTC";
			
			if (session.user_id() == null) {
				created_by = "Crowd-Contest-Bot";
			} else {
				created_by = session.user_id();
			}
			
			Long transaction_timestamp = System.currentTimeMillis();
			
			int pending_flag = 0;
			
			Double 
			
			amount_to_add = input.getDouble("amount_to_add"),
			promo_btc_balance = internal_promotions.getDouble("btc_balance"),
			progressive_btc_balance = internal_progressive.getDouble("btc_balance");
			
			if (amount_to_add == 0)
				{
				output.put("error", "this promotion exceeds the internal promotions account");
				break method;
				}			
			
			if (amount_to_add > promo_btc_balance)
				{
				output.put("error", "this promotion exceeds the internal promotions account");
				break method;
				}
			
		    PreparedStatement internal_transaction = sql_connection.prepareStatement("insert into transaction(created, created_by, trans_type, from_account, to_account, amount, from_currency, to_currency, memo, pending_flag, ext_address) values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS);				
			internal_transaction.setLong(1, transaction_timestamp);
			internal_transaction.setString(2, created_by);
			internal_transaction.setString(3, "BTC-WITHDRAWAL");
			internal_transaction.setString(4, internal_promotions_id);
			internal_transaction.setString(5, internal_progressive_id);
			internal_transaction.setDouble(6, amount_to_add);
			internal_transaction.setString(7, from_currency);
			internal_transaction.setString(8, to_currency);
			internal_transaction.setString(9, "Progressive update");
			internal_transaction.setInt(10, pending_flag);
			internal_transaction.setString(11, ext_address);
			internal_transaction.execute();
		
			Double new_promo_balance = subtract(promo_btc_balance, amount_to_add, 0);	
			db.update_btc_balance(internal_promotions_id, new_promo_balance);
			
			Double new_liability_balance = add(progressive_btc_balance, amount_to_add, 0);	
			db.update_btc_balance(internal_progressive_id, new_liability_balance);
			
			JSONObject progressive_for_code = db.select_progressive(code);
			Double new_balance_for_code = add(progressive_for_code.getDouble("balance"), amount_to_add, 0);
			
			PreparedStatement update_progressive_balance = sql_connection.prepareStatement("update progressive set balance = ? where code = ?");
			update_progressive_balance.setDouble(1, new_balance_for_code);
			update_progressive_balance.setString(2, code);
			update_progressive_balance.executeUpdate();

            output.put("status", "1");
            
			} method.response.send(output);
		}
	}