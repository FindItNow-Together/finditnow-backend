package com.finditnow.auth.dao;

import javax.sql.DataSource;

public class AuthDao {
    public AuthCredentialDao credDao;
    public AuthSessionDao sessionDao;
    public AuthOauthGoogleDao oauthDao;


    public AuthDao(DataSource ds){
        credDao = new AuthCredentialDao(ds);
        sessionDao = new AuthSessionDao(ds);
        oauthDao = new AuthOauthGoogleDao(ds);
    }
}
