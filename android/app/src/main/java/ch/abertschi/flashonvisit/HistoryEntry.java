package ch.abertschi.flashonvisit;

import android.provider.BaseColumns;

import java.util.Date;

/**
 * Model for history entries
 * <p>
 * Created by abertschi on 09.02.17.
 */
public class HistoryEntry {

    private Date date;
    private String message;

    public HistoryEntry(String message) {
        this.date = new Date();
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public HistoryEntry setMessage(String message) {
        this.message = message;
        return this;
    }

    public Date getDate() {
        return date;
    }

    public HistoryEntry setDate(Date date) {
        this.date = date;
        return this;
    }

    public static class DbEntry implements BaseColumns {
        public static final String TABLE_NAME = "history";
        public static final String COLUMN_NAME_DATE = "date";
        public static final String COLUMN_NAME_MESSAGE = "message";
    }
}
