The following fields are necessary for the BasketballContests.csv:
****THE CSV IS ONLY FOR ROSTER CONTESTS****
Every field should be seperated by a semicolon (;)

settlement_type: one of {HEADS-UP, DOUBLE-UP, JACKPOT}
name: Anything you want but note that " | YYYY-MM-DD" will be appended
desc: description string
rake: The integer between 0-100. for example, 5 means 5% rake
cost_per_entry: bitcoin cost per entry, ex: 0.001
salary_cap: integer value (ex: 1000)
min_users: For DOUBLE-UP or JACKPOT, must be >= 2, but for HEADS-UP must be 2
max_users: For DOUBLE-UP or JACKPOT, must be > min_users or 0 if unlimited, but for HEADS-UP must be 2
entries_per: # entries allowed for user: 0 = unlimited. For HEADS_UP, set entries_per = 1
roster_size: number of players allowed in roster (somewhere between 5-10 usually)
score_header: for now enter Points
payouts: 3+ payouts, seperated by commas (no spaces). MUST ADD UP TO 100. For example: 60,30,10. If HEADS-UP or DOUBLE-UP leave blank
