package eu.se_bastiaan.popcorntimeremote.models;

import android.provider.BaseColumns;

public class Instance implements BaseColumns {
    public static final String TABLE_NAME = "instances";
    public static final String COLUMN_NAME_NAME = "id";
    public static final String COLUMN_NAME_IP = "ip";
    public static final String COLUMN_NAME_PORT = "port";
    public static final String COLUMN_NAME_USERNAME = "username";
    public static final String COLUMN_NAME_PASSWORD = "password";

    public String name;
    public String ip;
    public String port;
    public String username;
    public String password;

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Instance))
            return false;

        Instance model = (Instance)obj;
        return this.ip.equals(model.ip) &&
               this.port.equals(model.port) &&
               !(this.username != null && model.username != null
                   && !this.username.equals(model.username)) &&
               !(this.password != null && model.password != null
                   && !this.password.equals(model.password));
    }
}
