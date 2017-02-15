package ch.abertschi.flashonvisit.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ch.abertschi.flashonvisit.HistoryEntry;

/**
 * Created by abertschi on 15.02.17.
 */
public class HistoryDbHelper extends SQLiteOpenHelper {

    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "history.db";

    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + HistoryEntry.DbEntry.TABLE_NAME + " (" +
                    HistoryEntry.DbEntry._ID + " INTEGER PRIMARY KEY," +
                    HistoryEntry.DbEntry.COLUMN_NAME_DATE + " LONG," +
                    HistoryEntry.DbEntry.COLUMN_NAME_MESSAGE + " TEXT)";

    public HistoryDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
    }

    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void addHistoryEntry(SQLiteDatabase db, HistoryEntry entry) {
        ContentValues values = new ContentValues();
        values.put(HistoryEntry.DbEntry.COLUMN_NAME_DATE, entry.getDate().getTime());
        values.put(HistoryEntry.DbEntry.COLUMN_NAME_MESSAGE, entry.getMessage());
        long newRowId = db.insert(HistoryEntry.DbEntry.TABLE_NAME, null, values);
    }

    public void deleteAll(SQLiteDatabase db) {
        db.delete(HistoryEntry.DbEntry.TABLE_NAME, "", null);
    }

    public List<HistoryEntry> getAll(SQLiteDatabase db) {
        String[] projection = {
                HistoryEntry.DbEntry._ID,
                HistoryEntry.DbEntry.COLUMN_NAME_DATE,
                HistoryEntry.DbEntry.COLUMN_NAME_MESSAGE
        };

        String sortOrder =
                HistoryEntry.DbEntry.COLUMN_NAME_DATE + " DESC";

        Cursor cursor = db.query(
                HistoryEntry.DbEntry.TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                sortOrder
        );

        List<HistoryEntry> entries = new ArrayList<>();
        while (cursor.moveToNext()) {
            long timeInMillis = cursor.getLong(
                    cursor.getColumnIndexOrThrow(HistoryEntry.DbEntry.COLUMN_NAME_DATE));
            String msg = cursor.getString(cursor.getColumnIndexOrThrow(HistoryEntry.DbEntry.COLUMN_NAME_MESSAGE));

            Date date = new Date();
            date.setTime(timeInMillis);
            HistoryEntry entry = new HistoryEntry(msg).setDate(date);
            entries.add(entry);
        }
        cursor.close();
        return entries;
    }
}
