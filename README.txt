README

**IMPORTANT**
NOTE: Use the clientID values from the subscriptions.txt file.

NOTE2: The UDP servers use port numbers starting at 6556 and will increment (i.e. first UDP server uses port 6556, second UDP server uses port 6557, etc.)

NOTE3: The TCP servers use port numbers starting at 9000 and will increment (i.e. first TCP server uses port 9000, second TCP server uses port 9001, etc.)

NOTE4: Because of things mentioned in NOTE2 and NOTE3, it is recommended that you run with Main java file passing in a port number that does not conflict with these two port ranges.
(e.g. Use port 6555 when running the Main file)

------------------------------------------------

HOW TO COMPILE AND RUN THE PROGRAM:
1. Run server: javac Main.java, java Main <port_number>
2. Run client(s): javac Client.java, java Client <port_number>

Client Portion:
1. Enter "log on <valid_subscriber_id>"

2. A client can then enter in:
	Log off
	Chat <ClientID>
	History <ClientID>
	Available

If a client wants to receive any messages from other clients, it must first enter in "Available" in order to let other clients know that it is ready to chat.

When two clients are chatting, a client can only send one message at a time (i.e. clients cannot send more than one message in a row).

If a client wants to end the chat, the client will enter in "End chat".


