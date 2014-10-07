package eu.se_bastiaan.popcorntimeremote.models;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class Instance implements BaseColumns, Parcelable {
    public static final String TABLE_NAME = "instances";
    public static final String COLUMN_NAME_NAME = "id";
    public static final String COLUMN_NAME_IP = "ip";
    public static final String COLUMN_NAME_PORT = "port";
    public static final String COLUMN_NAME_USERNAME = "username";
    public static final String COLUMN_NAME_PASSWORD = "password";

    public String id;
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
        return !(this.id != null && model.id != null
                && !this.id.equals(model.id)) && this.ip.equals(model.ip) &&
                this.port.equals(model.port) &&
                !(this.username != null && model.username != null
                        && !this.username.equals(model.username)) &&
                !(this.password != null && model.password != null
                        && !this.password.equals(model.password));
    }

    public Instance() {

    }

    protected Instance(Parcel in) {
        name = in.readString();
        ip = in.readString();
        port = in.readString();
        username = in.readString();
        password = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(ip);
        dest.writeString(port);
        dest.writeString(username);
        dest.writeString(password);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Instance> CREATOR = new Parcelable.Creator<Instance>() {
        @Override
        public Instance createFromParcel(Parcel in) {
            return new Instance(in);
        }

        @Override
        public Instance[] newArray(int size) {
            return new Instance[size];
        }
    };
}