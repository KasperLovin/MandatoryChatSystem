package Mandatory;

import java.io.Serializable;

// Denne class er til for at holde på flere beskeder fra flere operativ systemer.

public class MessageModel implements Serializable
{
    //Statiske variabler for at tilgå til andre klasser nemt uden at instantiere.
    static final int LIST = 0;
    static final int MESSAGE = 1;
    static final int QUIT = 2;
    static final int IMALIVE = 3;

    //variabler
    private int type;
    private String message;

    // construktor
    MessageModel(int type, String message)
    {
        this.type = type;
        this.message = message;
    }
    // getters
    int getType()
    {
        return type;
    }

    String getMessage()
    {
        return message;
    }
}