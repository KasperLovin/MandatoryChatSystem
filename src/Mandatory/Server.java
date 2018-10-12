package Mandatory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class Server
{
    //variabler
    private static int uniqueThreadId;
    private int port;
    private SimpleDateFormat dateFormat;
    private boolean isRunning;
    private ArrayList<HandleClientThread> clientThreadArrayList;


    // vores main til serveren hvor den får tildelt en port
    public static void main(String[] args)
    {
        int portNumber = 8372;

        Server server = new Server(portNumber);
        // her starter vi severen
        server.start();
    }

    // serverens constructor
    public Server(int port)
    {
        this.port = port;
        // dataformort bruger vi til at få et time stamp på vores beskeder og så vi kan
        // holder styr på hvornår der sidst er sendt en besked
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");
        this.clientThreadArrayList = new ArrayList<HandleClientThread>();
    }

    //Metode for start af server
    public void start()
    {
        isRunning = true;
        try
        {
            //metoden opretter en ny socket for serveren på den valgte port
            ServerSocket serverSocket = new ServerSocket(port);
            //mens serveren kører venter den på en client
            while (isRunning)
            {
                serverDisplay("Serveren venter på clienter på port: " + port);
                //accepterer clienten på socketen
                Socket socket = serverSocket.accept();

                if (!isRunning)
                {
                    break;
                }
                // laver en ny thread til clienten med socketen der er blevet accepteret
                HandleClientThread handleClientThread = new HandleClientThread(socket);

                // her blever tråden tilføjet til arryet af clienter
                clientThreadArrayList.add(handleClientThread);
                // Starer handleclient tråden som håndtere clienter
                handleClientThread.start();
            }
            try
            {
                //Her lukker serveren socketen
                serverSocket.close();
                for (int i = 0; i < clientThreadArrayList.size() ; i++)
                {
                    HandleClientThread hct = clientThreadArrayList.get(i);
                    try
                    {
                        hct.inputStream.close();
                        hct.outputStream.close();
                        hct.socket.close();
                    }
                    catch (IOException e)
                    {
                        System.out.println("Uknowen command " + e);
                    }
                }
            }
            catch (Exception e)
            {
                serverDisplay("Kunne ikke lukke serveren og clienterne pga. " + e);
            }
        }
        catch (IOException e)
        {
            String message = dateFormat.format(new Date()) + "Fejl på ny server socket" + e + "\n";
            serverDisplay(message);
        }
    }

    // motode som printer et timestamp foran beskeden.
    private void serverDisplay(String message)
    {
        String timeStamp = dateFormat.format(new Date()) + " " + message;
        System.out.println(timeStamp);
    }

    class HandleClientThread extends Thread
    {
        // Variabler til clienten
        ObjectInputStream inputStream;
        ObjectOutputStream outputStream;
        String username;
        String date;
        MessageModel message;
        Socket socket;
        int id;
        boolean duplicate = false;


        // constructeren for cilenten
        HandleClientThread(Socket socket)
        {
            this.socket = socket;
            // autoincrement på id
            this.id = ++uniqueThreadId;

            System.out.println("Prøver at få fat i tråden");
            try
            {
                //gør det muligt for clienten at skrive og motage beskeder
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                inputStream = new ObjectInputStream(socket.getInputStream());

                // metode for at tjekke om et username allrede findes
                while (true)
                {
                    username = (String) inputStream.readObject();

                    for (int i = 0; i < clientThreadArrayList.size(); i++)
                    {
                        if (username.equalsIgnoreCase(clientThreadArrayList.get(i).username))
                        {
                            duplicate = true;
                        }
                    }
                    if (!duplicate)
                    {
                        break;
                    }
                    serverDisplay(username + " var allerede taget og skal laves om til et unikt");
                    messageHandler("SERVER SIGER: Dit Username: '" + username + "' er allerede taget");
                    outputStream.writeObject("false");
                    duplicate = false;

                }
                //serveren skriver "true" til clienten hvis usernamet er unikt.
                outputStream.writeObject("true");
                serverDisplay(username + " er tilsluttet serveren");

            } catch (IOException e)
            {
                serverDisplay("Der kunne ikke oprettes forbindelse fordi: " + e);
                return;

            } catch (ClassNotFoundException e)
            {

            }
            date = new Date().toString() + "\n";
        }

        public void run()
        {
            // starter en ny heartbeatchecker tråd
            new HeartbeatChecker().start();
            boolean isRunning = true;
            while (isRunning)
            {
                try
                {
                    message = (MessageModel) inputStream.readObject();
                } catch (IOException e)
                {
                    serverDisplay(username + " har en fejl som er: " + e);
                    break;
                } catch (ClassNotFoundException e)
                {
                    break;
                }

                String innerMessage = this.message.getMessage();


                // switch case som tager imod alle typer af beskeder
                switch (this.message.getType())
                {
                    case MessageModel.MESSAGE:
                        broadCast(username + ": " + innerMessage);
                        break;
                    case MessageModel.QUIT:
                        serverDisplay(username + " Quit");
                        isRunning = false;
                        break;
                    case MessageModel.LIST:
                        messageHandler("Listen af brugere der er tilsluttet siden " +
                                dateFormat.format(new Date()) + "\n");
                        for (int i = 0; i < clientThreadArrayList.size(); i++)
                        {
                            HandleClientThread handleClientThread = clientThreadArrayList.get(i);
                            messageHandler((i + 1) + ") " + handleClientThread.username + " har været online siden " +
                                    handleClientThread.date);
                        }
                        break;
                    case MessageModel.IMALIVE:
                        serverDisplay("*[HEARTBEAT]*: " + username + " er i live!");
                        break;
                }
            }
            close();
        }

        // metode som lukker for at sende og modtage beskeder + socketen
        private void close()
        {
            try
            {
                if (outputStream != null)
                {
                    outputStream.close();
                }
            } catch (Exception e)
            {

            }
            try
            {
                if (inputStream != null)
                {
                    inputStream.close();
                }
            } catch (Exception e)
            {

            }
            try
            {
                if (socket != null)
                {
                    socket.close();
                }
            } catch (Exception e)
            {

            }
        }

        // for at hånbtere beskeder til cilenter så vi ikke skal skrive den flere gange.
        private boolean messageHandler(String message)
        {
            if (!socket.isConnected())
            {
                close();
                return false;
            }
            try
            {
                outputStream.writeObject(message);
            } catch (IOException e)
            {
                serverDisplay("Kunne ikke sende besked til " + username);
                serverDisplay(e.toString());
            }
            return true;
        }

        //metode som fortæller serveren at en clent er logget ud
        private synchronized void broadCast(String message)
        {
            String timeStamp = dateFormat.format(new Date());
            String messageWithStamp = timeStamp + " " + message + "\n";
            System.out.print(messageWithStamp);
            for (int i = clientThreadArrayList.size(); --i >= 0; )
            {
                HandleClientThread hct = clientThreadArrayList.get(i);
                if (!hct.messageHandler(messageWithStamp))
                {
                    clientThreadArrayList.remove(i);
                    serverDisplay(hct.username + " Er logget ud");
                }
            }
        }

        //en tråd som sletter clenter hvis de ikke har været aktive i 1 min.
        class HeartbeatChecker extends Thread
        {
            public void run()
            {
                while(true)
                {
                    try
                    {
                        Thread.sleep(60000);
                    } catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                    for (int i = 0; i < clientThreadArrayList.size(); i++)
                    {
                        HandleClientThread hct = clientThreadArrayList.get(i);
                        if (!clientThreadArrayList.get(i).isAlive())
                        {
                            serverDisplay("*[SERVER-TIMEOUT]*: " + clientThreadArrayList.get(i).username + " Timed-out af Serveren");
                            clientThreadArrayList.remove(i);
                            return;
                        }
                    }
                }
            }
        }
    }
}