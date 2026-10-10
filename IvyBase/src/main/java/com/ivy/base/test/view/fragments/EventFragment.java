package com.ivy.base.test.view.fragments;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ivy.base.R;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.data.NecessaryMonitor;

import java.util.ArrayList;
import java.util.List;

public class EventFragment extends BaseFragment implements NecessaryMonitor.EventListener {

    private static final String ARG_TITLE = "title";

    private RecyclerView recyclerView;
    private Adapter adapter = null;

    public static EventFragment newInstance(String title) {
        EventFragment f = new EventFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_event, container);

        recyclerView = view.findViewById(R.id.recycler_list);
        adapter = new Adapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        TestManager.getInstance().getNecessaryMonitor().setListener(this);

        return view;
    }

    @Override
    public void onRefresh(List<NecessaryMonitor.Item> events, boolean state) {
        recyclerView.post(() -> {
            adapter.submit(events);
            if (state) {

            }
        });
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.MyViewHolder> {

        private final List<NecessaryMonitor.Item> data = new ArrayList<>();

        @SuppressLint("NotifyDataSetChanged")
        public void submit(List<NecessaryMonitor.Item> newData) {
            data.clear();
            data.addAll(newData);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public Adapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, null);
            return new Adapter.MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Adapter.MyViewHolder holder, int position) {
            NecessaryMonitor.Item item = data.get(position);
            holder.date.setText(item.tag);
            holder.info.setText(item.info.replace("事件:", ""));
            if (item.event == null) {
                holder.date.setTextColor(Color.RED);
                holder.info.setTextColor(Color.RED);
            } else {
                holder.date.setTextColor(Color.GREEN);
                holder.info.setTextColor(Color.GREEN);
            }
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (item.event != null) {
                        showDataDialog(item.event.info, item.event.data);
                    } else {
                        Toast.makeText(v.getContext(), "这条事件尚未捕获", Toast.LENGTH_SHORT).show();
                    }
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
