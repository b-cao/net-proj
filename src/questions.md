Before I forget, just wanted to write down some things that I am unsure of as of the code posted today (170321).

Maybe someone else may know the answer or have some ideas and can let me know:

1) The decision of whether both client and server should be infinitely looping from the very start, or whether do manual send and receive for the handshaking
2) Should the TCP establish unique ports for each client, or should we use multithreading to handle multiple clients?
3) What is the rand_cookie value used for?
4) Debating whether a client should have the arraylists of all IDs, secretKeys, and encryptKeys or just their own values
