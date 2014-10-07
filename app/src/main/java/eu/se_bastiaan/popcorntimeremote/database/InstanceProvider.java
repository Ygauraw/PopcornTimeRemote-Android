package eu.se_bastiaan.popcorntimeremote.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

import eu.se_bastiaan.popcorntimeremote.models.Instance;
import eu.se_bastiaan.popcorntimeremote.utils.LogUtils;

public class InstanceProvider extends ContentProvider {
    private InstanceDbHelper mDB;

    private static final int ALL_INSTANCES = 1,
                             ONE_INSTANCE = 2,
                             IP_INSTANCE = 3;

    private static final String AUTHORITY = "eu.se_bastiaan.popcorntimeremote.instanceprovider";

    // create content URIs from the authority by appending path to database
    // table
    public static final Uri INSTANCES_URI = Uri.parse("content://" + AUTHORITY
            + "/instances");
    public static final Uri IP_URI = Uri.parse("content://" + AUTHORITY
            + "/ip");

    // a content URI pattern matches content URIs using wildcard characters:
    // *: Matches a string of any valid characters of any length.
    // #: Matches a string of numeric characters of any length.
    private static final UriMatcher uriMatcher;

    static {
        uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        uriMatcher.addURI(AUTHORITY, "instances", ALL_INSTANCES);
        uriMatcher.addURI(AUTHORITY, "instances/*", ONE_INSTANCE);
        uriMatcher.addURI(AUTHORITY, "ip/*", IP_INSTANCE);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        if(uriMatcher.match(uri) == ONE_INSTANCE) {
            SQLiteDatabase db = mDB.getWritableDatabase();
            db.delete(Instance.TABLE_NAME, Instance._ID  + " = ?", new String[]{ uri.getLastPathSegment() });
            db.close();

            getContext().getContentResolver().notifyChange(uri, null);
            return 1;
        }
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        if(uriMatcher.match(uri) == ALL_INSTANCES) {
            SQLiteDatabase db = mDB.getWritableDatabase();
            db.insert(Instance.TABLE_NAME, null, values);
            db.close();

            getContext().getContentResolver().notifyChange(uri, null);
        }
        return uri;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] values) {
        if(uriMatcher.match(uri) == ALL_INSTANCES) {
            SQLiteDatabase db = mDB.getWritableDatabase();
            db.beginTransaction();

            try {
                for (ContentValues singleValue : values)
                    db.insert(Instance.TABLE_NAME, null, singleValue);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }

            db.close();

            getContext().getContentResolver().notifyChange(uri, null);
        }
        return super.bulkInsert(uri, values);
    }

    @Override
    public boolean onCreate() {
        mDB = new InstanceDbHelper(getContext());
        return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(Instance.TABLE_NAME);

        switch (uriMatcher.match(uri)) {
            case ONE_INSTANCE:
                queryBuilder.appendWhere(Instance._ID + " = " + uri.getLastPathSegment());
                break;
            case IP_INSTANCE:
                queryBuilder.appendWhere(Instance.COLUMN_NAME_IP + " = \"" + uri.getLastPathSegment() + "\"");
                break;
            default:
                queryBuilder.appendWhere("1");
                break;
        }

        Cursor cursor = queryBuilder.query(mDB.getReadableDatabase(), projection, selection, selectionArgs, null, null, sortOrder);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        if(uriMatcher.match(uri) == ONE_INSTANCE) {
            SQLiteDatabase db = mDB.getWritableDatabase();
            db.update(Instance.TABLE_NAME, values, Instance._ID  + " = ?", new String[]{ uri.getLastPathSegment() });
            db.close();

            getContext().getContentResolver().notifyChange(uri, null);
        }
        return 0;
    }

}
