package com.coinroster.internal;

import java.util.concurrent.*;

import com.coinroster.Server;

public class ExpireSettlementWindow {
    
	public ExpireSettlementWindow(Long settlement_deadline, int contest_id) throws Exception 
		{
		     
		ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
		
		Runnable task = new Runnable() {
			public void run() {

				try {
					Server.log("Settlement deadline for contest " + contest_id + " has elapsed, notifying admin");

					String
					
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
	
					new NotifyAdmin(Server.sql_connection(), subject_admin, message_body_admin);
				} catch (Exception e) {
					Server.log("Exception occured while notifying admin: " + e.toString());
				}
			}
		};
		
		scheduler.schedule(task, settlement_deadline, TimeUnit.MILLISECONDS);
		scheduler.shutdown();
		}
	}