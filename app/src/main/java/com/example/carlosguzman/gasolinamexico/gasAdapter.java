package com.example.carlosguzman.gasolinamexico;

/**
 * Created by 501820531 on 4/17/2016.
 */

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class gasAdapter extends ArrayAdapter{
    private List list= new ArrayList();
    public gasAdapter(Context context, int resource) {
        super(context, resource);
        // TODO Auto-generated constructor stub
    }
    public void addGas(gasolinaClass object) {
        // TODO Auto-generated method stub

        list.add(object);
        super.add(object);

    }
    static class ImgHolder
    {
        ImageView IMG;
        TextView NAME;
        TextView PRICE;
        TextView PREVPRICE;
        TextView NEXTPRICE;
    }
    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return this.list.size();
    }
    @Override
    public void clear() {
        // TODO Auto-generated method stub
        list.clear();

    }
    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return this.list.get(position);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View row;
        row = convertView;
        ImgHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            row = inflater.inflate(R.layout.list_view_gas, parent, false);
            holder = new ImgHolder();
            holder.IMG = (ImageView) row.findViewById(R.id.thumbImage);
            //holder.NAME = (TextView) row.findViewById(R.id.list_view_gas_text);
            holder.PRICE = (TextView) row.findViewById(R.id.list_view_gas_price);
            holder.PREVPRICE = (TextView) row.findViewById(R.id.list_view_gas_text);
            holder.NEXTPRICE = (TextView) row.findViewById(R.id.list_view_gas_nextprice);
            row.setTag(holder);
        } else {
            holder = (ImgHolder) row.getTag();

        }
        final String TAG = "MyActivity";
        gasolinaClass FR = (gasolinaClass) getItem(position);
        //holder.IMG.setImageResource(FR.getGas_resource());
        String imageName = FR.getGas_resource();
        int resID = this.getContext().getResources().getIdentifier(imageName, "drawable", this.getContext().getPackageName());
        holder.IMG.setImageResource(resID);
        holder.IMG.setTag(Integer.valueOf(resID));
        holder.NEXTPRICE.setText("...");
        if (FR.getGas_price() != "..." ){
            try {
                Float todayValue = Float.parseFloat(FR.getGas_price());
                Float nextValue = Float.parseFloat(FR.getNextGas_price());
                if (todayValue < nextValue) {
                    holder.NEXTPRICE.setTextColor(Color.parseColor("#FC0015"));
                } else {
                    holder.NEXTPRICE.setTextColor(Color.parseColor("#05953E"));
                }
                holder.NEXTPRICE.setText("Sig:" + FR.getNextGas_price());
            }   catch (NumberFormatException e) {}
        }
        holder.PREVPRICE.setText("Ant:"+FR.getPrevGas_price());

        holder.NEXTPRICE.setTypeface(null, Typeface.BOLD);
        holder.PRICE.setText(FR.getGas_price());

        return row;
    }

}