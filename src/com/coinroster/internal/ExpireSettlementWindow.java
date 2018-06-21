package com.coinroster.internal;

import java.sql.Connection;
import java.util.concurrent.*;

import org.json.JSONObject;

import com.coinroster.DB;
import com.coinroster.Server;

public class ExpireSettlementWindow {
    
	public ExpireSettlementWindow(Long settlement_deadline, int contest_id, Connection sql_connection) throws Exception 
		{
		     
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		
		Runnable task = new Runnable() {
			public void run() {
				
				DB db = new DB(sql_connection);

				try {
					Server.log("Settlement deadline for contest " + contest_id + " has elapsed, notifying admin");
					
					JSONObject cash_register = db.select_user("username", "internal_cash_register");	

					String
	
					cash_register_email_address = cash_register.getString("email_address"),
					cash_register_admin = "Cash Register Admin",
					
					subject_admin = "User generated Contest Expired!",
					message_body_admin = "";
					
					message_body_admin += "<br/>";
					message_body_admin += "<br/>";
					message_body_admin += "A user generated contest has expired!";
					message_body_admin += "<br/>";
					message_body_admin += "<br/>";
					message_body_admin += "Contest ID: <b>" + contest_id + "</b>";
					message_body_admin += "<br/>";
					message_body_admin += "<br/>";
					message_body_admin += "Please settle the contest from the admin panel.";
					message_body_admin += "<br/>";
	
					Server.send_mail("govi218mu@gmail.com", cash_register_admin, subject_admin, message_body_admin);
				} catch (Exception e) {
					Server.log("Exception occured while notifying admin: " + e.toString());
					e.printStackTrace();
				}
			}
		};
		
		scheduler.schedule(task, (long) 500, TimeUnit.MILLISECONDS);
		scheduler.shutdown();
		}
	}