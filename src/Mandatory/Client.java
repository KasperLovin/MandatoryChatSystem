package Mandatory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;



public class Client
{
    //Variabler
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;
    private Socket socket;

    private String server;
    private String username;
    private String join;
    private int port;

    // Constructor
    Client(String server, String username, int port)
    {
        this.server = server;
        this.username = username;
        this.port = port;
        this.join = "JOIN";
    }

    public static void main(String[] args)
    {
        int portNumber = 0;
        String serverAddress = "";
        String username = "";

        // laver en ny client
        Client client = new Client(serverAddress, username, portNumber);
        // Hvis clienten ikke connectede til så får han lov at prøv igen
        if (!client.connectToServer())
        {
            return;
        }
        Scanner scanner = new Scanner(System.in);
        System.out.println("" +
                "Velkommen til Kasper & Matias' Chat rum\n" +
                "Du må MAKS skrive 250 karakter pr. besked\n" +
                "For at logge ud skriv: QUIT\n" +
                "For at se hvem der er online skriv: LIST\n" +
                "Skriv din første besked her: ");

        // Så længe inputtet er sandt kører det her loop og breaker kun igen når clienten quitter.
        while (true)
        {
            String input = scanner.nextLine();
            if (input.equalsIgnoreCase("QUIT"))
            {
                client.sendMessage(new MessageModel(MessageModel.QUIT, ""));
                break;

            } else if (input.equalsIgnoreCase("LIST"))
            {
                client.sendMessage(new MessageModel(MessageModel.LIST, ""));

            } else if (input.equalsIgnoreCase("IMALIVE"))
            {
                client.sendMessage(new MessageModel(MessageModel.IMALIVE, ""));

            } else
            {
                client.sendMessage(new MessageModel(MessageModel.MESSAGE, input));
            }
        }
        client.disconnet();
    }

    // metode til at connecte til serveren
    public boolean connectToServer()
    {
        Scanner input = new Scanner(System.in);
        boolean connected = false;
        while (!connected)
        {
            System.out.println("Skriv JOIN <<Username>>,<<LocalHost>>:<<8372>> for at connecte");
            String protocol = input.nextLine();
            String[] splitter = protocol.split("[\\s,:]+");

            try
            {
                join = splitter[0];
                username = splitter[1];
                server = splitter[2];
                port = Integer.parseInt(splitter[3]);
                if (join.equals("JOIN"))
                {
                    connected = true;
                }
            }
            catch (ArrayIndexOutOfBoundsException e)
            {
                System.out.println("Fejl ved protokol. Gør følgenden: ");

            }
        }
        while (username.length() > 12 || !username.matches("[-_a-åA-Å0-9]+"))
        {
            System.out.println("Dit username kan MAKS være på 12 karakter\n" +
                    "Skriv et nyt Username: ");
            username = input.nextLine();
        }

        try
        {
            socket = new Socket(server, port);
        } catch (Exception e)
        {
            printMessage("Kunne ikke oprette forbindelse til serveren " + e);
            return false;
        }

        String message = "Forbindelse oprettet " + socket.getInetAddress() + ":" + socket.getPort();
        printMessage(message);

        try
        {
            inputStream = new ObjectInputStream(socket.getInputStream());
            outputStream = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e)
        {
            printMessage("Fejl ved input/output " + e);
            return false;
        }

        try
        {
            while (true)
            {
                //skriver usernamet  til serveren som skriver "true" tilbage hvis det er unikt
                outputStream.writeObject(username);
                if (((String) inputStream.readObject()).equalsIgnoreCase("True"))
                {
                    break;
                }
                System.out.println("Skriv dit Nye Username: ");
                username = input.nextLine();
                System.out.println(username);
            }
        } catch (IOException e)
        {
            System.out.println("Fejl ved username ændring: " + e);
            disconnet();
            return false;
        } catch (ClassNotFoundException e)
        {
            System.out.println("Fandt ikke klassen");
        }

        new ServerCall().start();
        new Heartbeat().start();
        return true;
    }


    private void printMessage(String message)
    {
        System.out.println(message);
    }

    // metode som tjekker om beskeden er mere end 250 karakter lang
    private void sendMessage(MessageModel message)
    {
        try
        {
            if (message.getMessage().length() <= 250)
            {
                outputStream.writeObject(message);
            } else
            {
                System.out.println("Din besked var over 250 karakter\n" +
                        "Prøv at del din besked op.");
            }
        } catch (IOException e)
        {
            printMessage("Den fejlede i at skrive til serveren " + e);
        }
    }

    //metode dom lukke clientens input, output og socket
    private void disconnet()
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

    class ServerCall extends Thread
    {
        //når tråden kører læser den bedskeder fra severen som kommer fra andre clienter
        public void run()
        {
            while (true)
            {
                try
                {
                    String message = (String) inputStream.readObject();
                    System.out.print(message + "> ");
                } catch (IOException e)
                {
                    printMessage("Serveren har lukket forbindelsen " + e);
                    break;
                } catch (ClassNotFoundException e)
                {
                    System.out.println(e);
                }
            }
        }
    }

    // tråd som sender et heartbeat for hver client hvert min.
    class Heartbeat extends Thread
    {
        boolean isRunning = true;
        public void run()
        {
            while (isRunning)
            {
                try
                {
                    Thread.sleep(60000);
                    outputStream.writeObject(new MessageModel(3, "IMALIVE"));

                } catch (Exception e)
                {
                    System.out.println("Clienten er disconnected");
                    isRunning = false;
                    disconnet();
                }
            }
        }
    }
}