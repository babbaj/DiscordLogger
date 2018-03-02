package com.babbaj;

import net.dv8tion.jda.core.*;

import javax.security.auth.login.LoginException;

/**
 * Created by Babbaj on 1/12/2018.
 */
public class SelfBot {

    public static JDA jda;

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Session token must be given as argument");
            System.exit(1);
        }
        final String sessionToken = args[0];
        try {
            jda = new JDABuilder(AccountType.CLIENT)
                    .setToken(sessionToken)
                    .setAutoReconnect(true)
                    .setStatus(OnlineStatus.ONLINE)
                    .buildBlocking();
        } catch (LoginException | InterruptedException e) {
            e.printStackTrace();
            System.err.println("failed to initialize bot");
            System.exit(1);
        }
        jda.addEventListener(new DeletedMessageListener());

        System.out.println("Initialized bot");
    }
    
}
