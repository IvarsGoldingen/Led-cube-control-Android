package com.example.ivars.cubecontrol;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;

/**
 * Created by Ivars on 2017.07.04..
 */

public class LedSceneAdapter extends ArrayAdapter<LedScene> {

    public LedSceneAdapter(Context context, List<LedScene> ledScenes) {
        super(context, 0, ledScenes);
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        //check f an existing view has something in it,
        //else inflate the whole list
        View listItemView = convertView;
        ViewHolder holder;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(
                    R.layout.scene_list_item, parent, false);
            //If the listview was not yet created we assign and initialize viewHolder to it
            //so findViewById will not be needed each time
            holder = new ViewHolder();
            holder.numberTextview = listItemView.findViewById(R.id.textview_scene_number);
            holder.sceneTextview = listItemView.findViewById(R.id.textview_scene);
            holder.sceneLenghtTextview = listItemView.findViewById(R.id.textview_scene_lenght);
            //tell the View to hold an arbitrary object
            listItemView.setTag(holder);
        } else {
            //get the ViewHolder object from the current view, so we can use it's views
            holder = (ViewHolder) listItemView.getTag();
        }

        LedScene currentLedScene = getItem(position);

        holder.numberTextview.setText(String.valueOf(currentLedScene.getSceneNumber()));

        holder.sceneTextview.setText("");
        byte[] currentScene = currentLedScene.getLevels();
        int i = 0;
        while (currentScene.length > i) {
            //loop through the bytes in the scene
            //holder.sceneTextview.append(String.valueOf(currentScene[i]));
            for (int x = 7; x >= 0; x--) {
                //loop through the current byte
                if (((currentScene[i] >> x) & 1) == 1) {
                    holder.sceneTextview.append("1");
                } else {
                    holder.sceneTextview.append("0");
                }

            }
            holder.sceneTextview.append(" ");
            i++;
            if (i == 4) {
                holder.sceneTextview.append("\n");
            }
        }
        NumberFormat timeFormat = new DecimalFormat("#0.0");
        holder.sceneLenghtTextview.setText(String.valueOf(
                timeFormat.format((double) (currentLedScene.getSceneTimeInMillis()) / 1000)));
        return listItemView;

    }

    static class ViewHolder {
        //a viewholder is used to make the adapter more efficient. Using a viewHolder allows
        //us to not to search views with findViewByID each time
        TextView numberTextview;
        TextView sceneTextview;
        TextView sceneLenghtTextview;
    }
}
