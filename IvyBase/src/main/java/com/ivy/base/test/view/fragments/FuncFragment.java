package com.ivy.base.test.view.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ivy.base.Callback3;
import com.ivy.base.R;
import com.ivy.base.test.TestManager;
import com.ivy.base.test.TraceManager;
import com.ivy.base.test.data.Event;
import com.ivy.base.test.data.EventState;
import com.ivy.base.test.view.PropertyData;
import com.ivy.base.test.view.PropertyDialog;
import com.ivy.base.util.IToast;
import com.ivy.base.util.rx.Action;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FuncFragment extends BaseFragment implements View.OnClickListener, TraceManager.EventListener {

    private static final String ARG_TITLE = "title";
    private int WARNING_COLOR = Color.parseColor("#FF8F00");
    private RecyclerView recyclerView;
    private Adapter adapter = null;

    public static FuncFragment newInstance(String title) {
        FuncFragment f = new FuncFragment();
        Bundle b = new Bundle();
        b.putString(ARG_TITLE, title);
        f.setArguments(b);
        return f;
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_func, container);
        view.findViewById(R.id.all).setOnClickListener(this);
        view.findViewById(R.id.mod_ad_revenue).setOnClickListener(this);
        view.findViewById(R.id.clear_reframe).setOnClickListener(this);
        view.findViewById(R.id.ab_info).setOnClickListener(this);
        view.findViewById(R.id.active_ab).setOnClickListener(this);
        view.findViewById(R.id.get_ab).setOnClickListener(this);
        view.findViewById(R.id.login_facebook).setOnClickListener(this);
        view.findViewById(R.id.logout_facebook).setOnClickListener(this);
        view.findViewById(R.id.login_google).setOnClickListener(this);
        view.findViewById(R.id.logout_google).setOnClickListener(this);
        view.findViewById(R.id.login_playgames).setOnClickListener(this);
        view.findViewById(R.id.logout_playgames).setOnClickListener(this);
        view.findViewById(R.id.playgames_stats).setOnClickListener(this);
        view.findViewById(R.id.playgames_event).setOnClickListener(this);
        view.findViewById(R.id.playgames_achievement_update).setOnClickListener(this);
        view.findViewById(R.id.playgames_achievement_unlock).setOnClickListener(this);
        view.findViewById(R.id.login_firebase).setOnClickListener(this);
        view.findViewById(R.id.logout_firebase).setOnClickListener(this);
        view.findViewById(R.id.unlink_firebase).setOnClickListener(this);
        view.findViewById(R.id.pay).setOnClickListener(this);
        view.findViewById(R.id.show_interstitial).setOnClickListener(this);
        view.findViewById(R.id.show_rewarded).setOnClickListener(this);
        view.findViewById(R.id.show_banner).setOnClickListener(this);
        view.findViewById(R.id.close_banner).setOnClickListener(this);
        view.findViewById(R.id.translate).setOnClickListener(this);
        view.findViewById(R.id.set_bx_id).setOnClickListener(this);

        recyclerView = view.findViewById(R.id.recycler_list);
        adapter = new Adapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        return view;
    }


    @Override
    public void onClick(View v) {
        TestManager.getInstance().endTrace();
        adapter.submit(new ArrayList<Event>());
        if (v.getId() == R.id.all) {
            TestManager.getInstance().startTrace("Appsflyer", "Firebase", "Facebook", "IVY");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            Bundle bundle = new Bundle();
            bundle.putFloat("revenue", 1);
            bundle.putString("currency", "USD");
            bundle.putString("event_src", "SDK");
            bundle.putString("event_type", "ad_revenue");
            bundle.putBoolean("mock", true);
            TestManager.getInstance().sendEvent("af_ad_revenue", bundle);

            Bundle bundle1 = new Bundle();
            bundle1.putInt("sdk_state", 1);
            bundle1.putString("price", "$US0.0");
            bundle1.putString("currency", "USD");
            bundle1.putString("price_amount", "0");
            bundle1.putString("conf_price", "0");
            bundle1.putBoolean("force", true);
            bundle1.putInt("pay_state", 3);
            bundle1.putString("event_src", "SDK");
            bundle1.putString("event_type", "purchase");
            bundle1.putBoolean("mock", true);
            TestManager.getInstance().sendEvent("sdk_iap_purchased", bundle1);
        } else if (v.getId() == R.id.clear_reframe) {
            TestManager.getInstance().clearReframeRecord();
        } else if (v.getId() == R.id.mod_ad_revenue) {
            showInputDialog("输入模拟广告收入值", "广告收入值", new Action<String>() {
                @Override
                public void call(String s) {
                    float revenue = 0;
                    try {
                        revenue = Float.parseFloat(s);
                    } catch (Throwable t) {
                    } finally {
                        if (revenue > 1) {
                            IToast.toastAnyway("模拟收入值过大！");
                            return;
                        }
                        TestManager.getInstance().startTrace("Appsflyer", "Firebase", "Facebook", "IVY");
                        TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                        Bundle bundle = new Bundle();
                        bundle.putFloat("revenue", revenue);
                        bundle.putString("currency", "USD");
                        bundle.putString("event_src", "SDK");
                        bundle.putString("event_type", "ad_revenue");
                        bundle.putBoolean("mock", true);
                        TestManager.getInstance().sendEvent("af_ad_revenue", bundle);
                    }
                }
            });
        } else if (v.getId() == R.id.ab_info) {
            String info = TestManager.getInstance().getActivedAbInfo();
            showInfoDialog("已激活AB", info);
        } else if (v.getId() == R.id.active_ab) {
            showInputDialog("激活指定AB实验", "输入实验 id", new Action<String>() {
                @Override
                public void call(String s) {
                    if (TextUtils.isEmpty(s)) {
                        Toast.makeText(v.getContext(), "无效的实验id", Toast.LENGTH_SHORT).show();
                    } else {
                        boolean result = TestManager.getInstance().enterAbVariants(s);
                        Toast.makeText(v.getContext(), "激活实验id" + (result ? "成功" : "失败"), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else if (v.getId() == R.id.get_ab) {
            String info = TestManager.getInstance().getAllAbConfig();
            showInfoDialog("所有可用的AB配置", info);
        } else if (v.getId() == R.id.login_facebook) {
            TestManager.getInstance().startTrace("Facebook_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().auth("facebook", 1);
        } else if (v.getId() == R.id.logout_facebook) {
            TestManager.getInstance().startTrace("Facebook_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().auth("facebook", 2);
        } else if (v.getId() == R.id.login_google) {
            TestManager.getInstance().startTrace("Google_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().auth("google", 1);
        } else if (v.getId() == R.id.logout_google) {
            TestManager.getInstance().startTrace("Google_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().auth("google", 2);
        } else if (v.getId() == R.id.login_playgames) {
            TestManager.getInstance().startTrace("PlayGames_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().auth("playgames", 1);
        } else if (v.getId() == R.id.logout_playgames) {
            TestManager.getInstance().startTrace("PlayGames_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().auth("playgames", 2);
        } else if (v.getId() == R.id.playgames_stats) {
            //playgames游戏数据分析
            new PropertyDialog(getContext(), new PropertyDialog.OnConfirmListener() {
                @Override
                public void onConfirm(PropertyData data) {
                    try {
                        String eventId = data.getName();
                        if (TextUtils.isEmpty(eventId)) {
                            IToast.toast("事件名称不正确");
                            return;
                        }
                        JSONObject json = new JSONObject();
                        Map<String, Object> map = data.getProperties();
                        if (map != null && !map.isEmpty()) {
                            String[] args = map.keySet().toArray(new String[0]);
                            for (String s : args) {
                                Object v = map.get(s);
                                if (v != null) {
                                    json.put(s, v);
                                }
                            }
                        }
                        TestManager.getInstance().startTrace("PlayGames");
                        TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                        TestManager.getInstance().onPlayGameStats(eventId, json);
                    } catch (Exception e) {
                        IToast.toast("失败:" + e.getMessage());
                    }
                }
            }).show();
        } else if (v.getId() == R.id.playgames_event) {
            //playgames 活动
        } else if (v.getId() == R.id.playgames_achievement_update) {
            showAchievementDialog("更新成就step", false, new Callback3<String, Integer>() {
                @Override
                public void onResult(String s, Integer integer) {
                    TestManager.getInstance().startTrace("PlayGames");
                    TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                    TestManager.getInstance().updateAchievement(s, integer, false);
                }
            });
        } else if (v.getId() == R.id.playgames_achievement_unlock) {
            showAchievementDialog("解锁成就", false, new Callback3<String, Integer>() {
                @Override
                public void onResult(String s, Integer integer) {
                    TestManager.getInstance().startTrace("PlayGames");
                    TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                    TestManager.getInstance().updateAchievement(s, integer, true);
                }
            });
        } else if (v.getId() == R.id.login_firebase) {
            showFirebaseLoginDialog("选择登录渠道", true, new Action<String>() {
                @Override
                public void call(String s) {
                    TestManager.getInstance().startTrace("Firebase_Auth");
                    TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                    TestManager.getInstance().firebaseAuth(s, 1);
                }
            });
        } else if (v.getId() == R.id.logout_firebase) {
            TestManager.getInstance().startTrace("Firebase_Auth");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().firebaseAuth("firebase", 2);
        } else if (v.getId() == R.id.unlink_firebase) {
            showFirebaseLoginDialog("选择登录渠道", false, new Action<String>() {
                @Override
                public void call(String s) {
                    TestManager.getInstance().startTrace("Firebase_Auth");
                    TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                    TestManager.getInstance().firebaseAuth(s, 3);
                }
            });
        } else if (v.getId() == R.id.pay) {
            TestManager.getInstance().startTrace("Payment");
            TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
            TestManager.getInstance().pay();
        } else if (v.getId() == R.id.translate) {
            showInputDialog("输入翻译内容", "", new Action<String>() {
                @Override
                public void call(String s) {
                    if (TextUtils.isEmpty(s)) return;
                    TestManager.getInstance().startTrace("Translate");
                    TestManager.getInstance().getCurrentTrace().setListener(FuncFragment.this);
                    TestManager.getInstance().translate(s);
                }
            });
        } else if (v.getId() == R.id.set_bx_id) {
            showInputDialog("输入变现ID（留空清除）", "当前值：" + TestManager.getInstance().getCurrentBxId(), new Action<String>() {
                @Override
                public void call(String s) {
                    TestManager.getInstance().setTestBxId(s);
                    Toast.makeText(v.getContext(), TextUtils.isEmpty(s.trim()) ? "已清除变现ID，重启后生效" : "变现ID设置成功，重启后生效", Toast.LENGTH_SHORT).show();
                }
            });
        } else if (v.getId() == R.id.show_interstitial) {
            TestManager.getInstance().endTrace();
            TestManager.getInstance().adTask(1);
        } else if (v.getId() == R.id.show_rewarded) {
            TestManager.getInstance().endTrace();
            TestManager.getInstance().adTask(2);
        } else if (v.getId() == R.id.show_banner) {
            TestManager.getInstance().endTrace();
            TestManager.getInstance().adTask(3);
        } else if (v.getId() == R.id.close_banner) {
            TestManager.getInstance().endTrace();
            TestManager.getInstance().adTask(4);
        }

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
        public Adapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log_item, null);
            return new Adapter.MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull Adapter.MyViewHolder holder, int position) {
            Event event = data.get(position);
            holder.date.setText(event.dateStr);
            holder.info.setText(event.tag + ">" + event.info);
            if (event.state == EventState.ERROR) {
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
