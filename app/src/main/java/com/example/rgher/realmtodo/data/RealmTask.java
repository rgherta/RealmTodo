package com.example.rgher.realmtodo.data;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

import static com.example.rgher.realmtodo.data.DatabaseContract.getColumnInt;
import static com.example.rgher.realmtodo.data.DatabaseContract.getColumnLong;
import static com.example.rgher.realmtodo.data.DatabaseContract.getColumnString;

/**
 * Created by rgher on 10/8/2017.
 */

public class RealmTask extends RealmObject implements Parcelable {

    @Required
    @PrimaryKey
    private Integer task_id;

    private String description;
    private Integer is_complete;
    private Integer is_priority;
    private Long due_date;

    public Integer getTask_id() {
        return task_id;
    }

    public void setTask_id(Integer task_id) {
        this.task_id = task_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getIs_complete() {
        return is_complete;
    }

    public void setIs_complete(Integer is_complete) {
        this.is_complete = is_complete;
    }

    public Integer getIs_priority() {
        return is_priority;
    }

    public void setIs_priority(Integer is_priority) {
        this.is_priority = is_priority;
    }

    public Long getDue_date() {
        return due_date;
    }

    public void setDue_date(Long due_date) {
        this.due_date = due_date;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id='" + task_id + '\'' +
                ", description='" + description + '\'' +
                ", complete=" + is_complete + '\'' +
                ", priority=" + is_priority + '\'' +
                ", duedate=" + due_date + '\'' +
                '}';
    }


    //PUBLIC CONSTRUCTORS

    /**
     * Create a new Task from discrete items
     */
    public static final long NO_DATE = Long.MAX_VALUE;
    public static final int NO_ID = -1;

    public RealmTask(String description, Integer isComplete, Integer isPriority, long dueDateMillis) {
        this.task_id = NO_ID; //Not set
        this.description = description;
        this.is_complete = isComplete;
        this.is_priority = isPriority;
        this.due_date = dueDateMillis;
    }

    /**
     * Create a new Task with no due date
     */
    public RealmTask(String description, Integer isComplete, Integer isPriority) {
        this.task_id = NO_ID; //Not set
        this.description = description;
        this.is_complete = isComplete;
        this.is_priority = isPriority;
        this.due_date = NO_DATE;
    }

    /**
     * Create a new task from a database Cursor
     */
    public RealmTask(Cursor cursor) {
        this.task_id = getColumnInt(cursor, DatabaseContract.TaskColumns._ID);
        this.description = getColumnString(cursor, DatabaseContract.TaskColumns.DESCRIPTION);
        this.is_complete = getColumnInt(cursor, DatabaseContract.TaskColumns.IS_COMPLETE);
        this.is_priority = getColumnInt(cursor, DatabaseContract.TaskColumns.IS_PRIORITY);
        this.due_date = getColumnLong(cursor, DatabaseContract.TaskColumns.DUE_DATE);
    }

    /**
     * Return true if a due date has been set on this task.
     */
    public boolean hasDueDate() {
        return this.due_date != Long.MAX_VALUE;
    }

    public RealmTask() {

    }


    /**
     * Create a new Task from a data Parcel
     */
    protected RealmTask(Parcel in) {
        this.setTask_id(in.readInt());
        this.setDescription(in.readString());
        this.setIs_complete(in.readInt());
        this.setIs_priority(in.readInt());
        this.setDue_date(in.readLong());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.getTask_id());
        dest.writeString(this.getDescription());
        dest.writeInt(this.getIs_complete());
        dest.writeInt(this.getIs_priority());
        dest.writeLong(this.getDue_date());
    }

    public static final Creator<RealmTask> CREATOR = new Creator<RealmTask>() {
        @Override
        public RealmTask createFromParcel(Parcel in) {
            return new RealmTask(in);
        }

        @Override
        public RealmTask[] newArray(int size) {
            return new RealmTask[size];
        }
    };
}