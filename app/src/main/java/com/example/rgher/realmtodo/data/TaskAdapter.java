package com.example.rgher.realmtodo.data;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Paint;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;


import com.example.rgher.realmtodo.R;
import com.example.rgher.realmtodo.views.TaskTitleView;

import java.util.Calendar;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {

    private Cursor mCursor;
    private OnItemClickListener mOnItemClickListener;
    private Context mContext;

    /* Callback for list item click events */
    public interface OnItemClickListener {
        void onItemClick(View v, int position);

        void onItemToggled(boolean active, int position);
    }

    /* ViewHolder for each task item */
    public class TaskHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public TaskTitleView nameView;
        public TextView dateView;
        public ImageView priorityView;
        public CheckBox checkBox;

        public TaskHolder(View itemView) {
            super(itemView);
            nameView = (TaskTitleView) itemView.findViewById(R.id.text_description);
            dateView = (TextView) itemView.findViewById(R.id.text_date);
            priorityView = (ImageView) itemView.findViewById(R.id.priority);
            checkBox = (CheckBox) itemView.findViewById(R.id.checkbox);

            itemView.setOnClickListener(this);
            checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v == checkBox) {
                completionToggled(this);
            } else{
                postItemClick(this);
            }
        }

    }

    public TaskAdapter(Context context) {
        mContext = context;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        mOnItemClickListener = listener;
    }

    private void completionToggled(TaskHolder holder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemToggled(holder.checkBox.isChecked(), holder.getAdapterPosition());
            if(holder.checkBox.isChecked()){
                holder.nameView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                holder.nameView.setPaintFlags(0);
            }
        }
    }

    private void postItemClick(TaskHolder holder) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(holder.itemView, holder.getAdapterPosition());
        }
    }

    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //mContext = parent.getContext();
        View itemView = LayoutInflater.from(mContext)
                .inflate(R.layout.list_item_task, parent, false);

        return new TaskHolder(itemView);
    }

    @Override
    public void onBindViewHolder(TaskHolder holder, int position) {

        //Bind the task data to the views
        mCursor.moveToPosition(position);
        RealmTask newTask =  new RealmTask(mCursor);

        //Set holder values
        holder.nameView.setText(newTask.getDescription());
        holder.checkBox.setChecked((newTask.getIs_complete()==1));

        if(newTask.getIs_priority()==1){
            holder.priorityView.setImageResource(R.drawable.ic_priority);
        } else {
            holder.priorityView.setImageResource(R.drawable.ic_not_priority);
        }

        if(newTask.getDue_date() != Long.MAX_VALUE){
            holder.dateView.setVisibility(View.VISIBLE);
            CharSequence formatedDate = DateUtils.getRelativeTimeSpanString(mContext, newTask.getDue_date());
            holder.dateView.setText(formatedDate);
        } else {
            holder.dateView.setVisibility(View.GONE);
        }

        Calendar c = Calendar.getInstance();
        Long now = c.getTimeInMillis();

        if(newTask.getDue_date() < now){
            holder.nameView.setState(TaskTitleView.OVERDUE);
        }else {
            holder.nameView.setState(TaskTitleView.NORMAL);
        }

        if(newTask.getIs_complete()==1){
            holder.nameView.setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.nameView.setPaintFlags(0);
        }

    }

    @Override
    public int getItemCount() {
        return (mCursor != null) ? mCursor.getCount() : 0;
    }


    public RealmTask getItem(int position) {
        if (!mCursor.moveToPosition(position)) {
            throw new IllegalStateException("Invalid item position requested");
        }

        return new RealmTask(mCursor);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getTask_id();
    }

    public void swapCursor(Cursor c) {
        if (mCursor != null) {
            mCursor.close();
        }
        mCursor = c;
        notifyDataSetChanged();
    }

}
