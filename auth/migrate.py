import sqlite3

def init():
    try:
        file = open(".db", "x")
        print("File created")
    except:
        print("File already exists")

def migrate():
    conn = sqlite3.connect(".db")
    drop_user = "DROP TABLE IF EXISTS user;"
    drop_auth_key = "DROP TABLE IF EXISTS auth_key;"
    users_table = """CREATE TABLE user (
        "id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,		
		"email" TEXT NOT NULL,
        "password" TEXT NOT NULL,
        "name" TEXT NOT NULL,
        "surname" TEXT NOT NULL,
        "auth_token" TEXT,
        "logged_in_count" integer NOT NULL
    );
    """
    auth_key_table = """CREATE TABLE auth_key (
    	"id" integer NOT NULL PRIMARY KEY AUTOINCREMENT,
    	"ip_addr" TEXT NOT NULL,
    	"user_token" TEXT NOT NULL,
    	"client_token" TEXT NOT NULL,
        "token_expiry_date" integer,
        "client_identifier" TEXT NOT NULL
    );
    """

    cur = conn.cursor()
    cur.execute(drop_user)
    cur.execute(drop_auth_key)
    cur.execute(users_table)
    cur.execute(auth_key_table)
    conn.commit()
    conn.close()

def main():
    init()
    migrate()

if __name__ == "__main__":
    main()
