package com.ivy.base.test.view.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ivy.base.R;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.Event;
import com.ivy.base.test.data.EventMonitor;
import com.ivy.base.test.data.EventState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommonFragment extends BaseFragment implements EventMonitor.EventListener {

    private static final String ARG_TITLE = "title";
    private RecyclerView recyclerView;

    private Adapter adapter = null;

    private int WARNING_COLOR = Color.parseColor("#FF8F00");



    public static CommonFragment newInstance(String title) {
        CommonFragment f = new CommonFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_common, container);
        recyclerView = view.findViewById(R.id.recycler_list);
        adapter = new Adapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        TestManager.getInstance().getCommonEventMonitor().setListener(this);
        return view;
    }

    @Override
    public void onRefresh(List<Event> e) {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                adapter.submit(e);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        TestManager.getInstance()
                .getCommonEventMonitor()
                .setListener(null);
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {

        private final List<Event> data = new ArrayList<>();

        public void submit(List<Event> newData) {

            DiffUtil.DiffResult diff = DiffUtil.calculateDiff(
                    new Diff(data, newData)
            );

            data.clear();
            data.addAll(newData);

            diff.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, null);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Event event = data.get(position);
            holder.date.setText(event.dateStr);
            holder.info.setText(event.tag + ">" + event.info);
            if (event.state == EventState.ERROR){
                holder.date.setTextColor(Color.RED);
                holder.info.setTextColor(Color.RED);
            } else if (event.state == EventState.WARNING) {
                holder.date.setTextColor(WARNING_COLOR);
                holder.info.setTextColor(WARNING_COLOR);
            } else {
                holder.date.setTextColor(Color.DKGRAY);
                holder.info.setTextColor(Color.BLACK);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDataDialog(event.info, event.data);
                }
            });
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private class MyViewHolder extends RecyclerView.ViewHolder {
            TextView date = null;
            TextView info = null;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                date = itemView.findViewById(R.id.date);
                info = itemView.findViewById(R.id.info);
            }
        }
    }

}