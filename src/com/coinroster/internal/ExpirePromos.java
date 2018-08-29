package com.coinroster.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Server;
import com.coinroster.Utils;

public class ExpirePromos extends Utils
	{
	public ExpirePromos() // called by hourly cron
		{
		Connection sql_connection = null;
		
		try {
			sql_connection = Server.sql_connection();
			
			DB db = new DB(sql_connection);
			
			PreparedStatement select_expired_promos = sql_connection.prepareStatement("select id from promo where expires > 0 and expires < ? and cancelled = 0");
			select_expired_promos.setLong(1, System.currentTimeMillis());
			ResultSet result_set = select_expired_promos.executeQuery();
			
			// all promos in result set have expired and have not yet been cancelled
			
			while (result_set.next())
				{
				try {
					int promo_id = result_set.getInt(1);
					
					JSONObject promo = db.select_promo(promo_id);

					PreparedStatement update_promo = sql_connection.prepareStatement("update promo set cancelled = ?, cancelled_reason = ? where id = ?");
					update_promo.setLong(1, System.currentTimeMillis());
					update_promo.setString(2, "Expired");
					update_promo.setInt(3, promo_id);
					update_promo.executeUpdate();
					
					if (!promo.isNull("referrer")) // update referral_promo_code on user
						{
						String 
						
						promo_code = promo.getString("promo_code"),
						referrer_id = promo.getString("referrer");
						
						PreparedStatement update_user = sql_connection.prepareStatement("update user set referral_promo_code = null where id = ?");
						update_user.setString(1, referrer_id);
						update_user.executeUpdate();
						
						JSONObject referrer = db.select_user("id", referrer_id);
						
						String subject = "Your Affiliate promo code has expired", 
						
						message_body = "Hi <b><!--USERNAME--></b>";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += "Your promo code <b>" + promo_code + "</b> has expired.";
						message_body += "<br/>";
						message_body += "<br/>";
						message_body += "<a href='" + Server.host + "/account/affiliate_request.html'>Click here</a> to request a new one.";
						
						new UserMail(referrer, subject, message_body);
						}
					}
				catch (Exception e1)
					{
					Server.exception(e1);
					}
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
