package com.test.reddit.redditsimpleclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Date;

/**
 * Created by seba on 02/05/16.
 */
public class ItemView extends LinearLayout {

    private final Context context;
    private final TextView titleView;
    private final TextView userView;
    private final TextView dateView;
    private final ImageView pictureView;
    private final TextView commentsView;

    public ItemView(Context context){
        super(context);
        this.context = context;

        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.item_layout,this,true);

        titleView = (TextView)findViewById(R.id.item_title);
        userView = (TextView)findViewById(R.id.item_user);
        dateView = (TextView)findViewById(R.id.item_date);
        pictureView = (ImageView)findViewById(R.id.item_img);
        commentsView = (TextView)findViewById(R.id.item_comments);
    }

    public void setTitle(String title) {
        this.titleView.setText(title);
    }

    public void setUser(String user) {
        this.userView.setText(user);
    }

    public void setDate(Double date) {
        Date created = new Date(date.longValue() * 1000);
        int hours = (int)(System.currentTimeMillis() - created.getTime())/1000/60/60;

        StringBuilder b = new StringBuilder();
        b.append(context.getText(R.string.submitted));
        if( hours >= 24 ){
            int days = hours/24;
            b.append(" ").append(days).append(" ");
            b.append((days == 1)?context.getText(R.string.day):context.getText(R.string.days));
        } else {
            b.append(" ").append(hours).append(" ");
            b.append((hours == 1) ? context.getText(R.string.hour) : context.getText(R.string.hours));
        }
        b.append(" ").append(context.getText(R.string.by));

        dateView.setText(b.toString());
    }

    public void setComments(String comments){
        commentsView.setText(comments + " " +context.getText(R.string.comments));
    }

    public ImageView getPictureView() {
        return pictureView;
    }
}
