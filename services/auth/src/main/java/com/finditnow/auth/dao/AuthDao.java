package com.finditnow.auth.dao;

import javax.sql.DataSource;

public class AuthDao {
    public final AuthCredentialDao credDao;
    public final AuthSessionDao sessionDao;
    public final AuthOauthGoogleDao oauthDao;
    private final DataSource dataSource;

    public AuthDao(DataSource ds) {
        this.dataSource = ds;
        this.credDao = new AuthCredentialDao(ds);
        this.sessionDao = new AuthSessionDao(ds);
        this.oauthDao = new AuthOauthGoogleDao(ds);
    }

    public DataSource getDataSource() {
        return dataSource;
    }
}
