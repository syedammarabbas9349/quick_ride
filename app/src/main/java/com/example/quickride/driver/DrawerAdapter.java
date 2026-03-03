package com.example.quickride.driver;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.quickride.R;

public class DrawerAdapter extends BaseAdapter {

    private Context context;
    private String[] items;
    private int[] icons;
    private int selectedPosition = 0;

    public DrawerAdapter(Context context, String[] items, int[] icons) {
        this.context = context;
        this.items = items;
        this.icons = icons;
    }

    @Override
    public int getCount() {
        return items.length;
    }

    @Override
    public Object getItem(int position) {
        return items[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_drawer, parent, false);
        }

        ImageView icon = convertView.findViewById(R.id.drawer_icon);
        TextView text = convertView.findViewById(R.id.drawer_text);

        icon.setImageResource(icons[position]);
        text.setText(items[position]);

        // Highlight selected item
        if (position == selectedPosition) {
            convertView.setBackgroundColor(context.getResources().getColor(R.color.grey_200));
            text.setTextColor(context.getResources().getColor(R.color.colorPrimary));
        } else {
            convertView.setBackgroundColor(android.R.color.transparent);
            text.setTextColor(context.getResources().getColor(R.color.grey_800));
        }

        return convertView;
    }

    public void setSelectedPosition(int position) {
        this.selectedPosition = position;
        notifyDataSetChanged();
    }
}