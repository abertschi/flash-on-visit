package ch.abertschi.flashonvisit;

import java.util.Date;

/**
 * Model for history entries
 *
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
}
