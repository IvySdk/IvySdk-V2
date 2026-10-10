package com.ivy.base.test.view.fragments;

import android.content.DialogInterface;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.alibaba.fastjson.JSON;
import com.ivy.base.Callback3;
import com.ivy.base.util.Util;
import com.ivy.base.util.rx.Action;

class BaseFragment extends Fragment {


    private String formatJson(String json) {
        try {
            Object obj = JSON.parse(json);
            return JSON.toJSONString(obj, true); // true = pretty format
        } catch (Exception e) {
            return json;
        }
    }

    protected void showDataDialog(String title, String data) {
        if (TextUtils.isEmpty(data)) return;

        String prettyJson = formatJson(data);

        TextView textView = new TextView(getContext());
        textView.setText(prettyJson);
        textView.setPadding(40, 40, 40, 40);
        textView.setTextIsSelectable(true);
//        textView.setMovementMethod(new android.text.method.ScrollingMovementMethod());
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.addView(textView);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton("复制", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Util.copyTxt(data);
                    }
                })
                .setNegativeButton("关闭", null)
                .create();

        dialog.show();
    }

    protected void showInputDialog(String title, String hint, Action<String> callback) {
        EditText editText = new EditText(getContext());
        editText.setHint(hint);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(editText)
                .setPositiveButton("确定", (dialog1, which) -> {
                    String txt = editText.getText().toString();
                    if (callback != null) callback.call(txt);
                    dialog1.dismiss();
                })
                .setNegativeButton("关闭", null)
                .create();

        dialog.show();
    }

    protected void showInfoDialog(String title, String msg) {
        String prettyJson = formatJson(msg);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setMessage(prettyJson)
                .setPositiveButton("确定", null)
                .create();

        dialog.show();
    }

    protected void showFirebaseLoginDialog(String title, boolean withAn, Action<String> callback) {
        LinearLayout linear = new LinearLayout(getContext());
        linear.setOrientation(1);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(linear)
                .setNegativeButton("关闭", null)
                .create();

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tag = (String) v.getTag();
                if (callback != null) callback.call(tag);
                if (dialog != null) dialog.dismiss();
            }
        };
        Button google_btn = new Button(getContext());
        google_btn.setText("Google");
        google_btn.setTag("Google");
        google_btn.setOnClickListener(listener);
        linear.addView(google_btn);

        Button playgames_btn = new Button(getContext());
        playgames_btn.setText("PlayGames");
        playgames_btn.setTag("PlayGames");
        playgames_btn.setOnClickListener(listener);
        linear.addView(playgames_btn);

        Button facebook_btn = new Button(getContext());
        facebook_btn.setText("Facebook");
        facebook_btn.setTag("Facebook");
        facebook_btn.setOnClickListener(listener);
        linear.addView(facebook_btn);

        Button email_btn = new Button(getContext());
        email_btn.setText("Email");
        email_btn.setTag("Email");
        email_btn.setOnClickListener(listener);
        linear.addView(email_btn);

        if (withAn) {
            Button noname_btn = new Button(getContext());
            noname_btn.setText("匿名");
            noname_btn.setTag("匿名");
            noname_btn.setOnClickListener(listener);
            linear.addView(noname_btn);
        }

        dialog.show();
    }

    protected void showAchievementDialog(String title, boolean unlock, Callback3<String, Integer> callback) {
        LinearLayout linear = new LinearLayout(getContext());
        linear.setOrientation(1);

        TextView error = new TextView(getContext());
        error.setTextSize(16);
        error.setTextColor(Color.RED);
        linear.addView(error);

        EditText idEditor = new EditText(getContext());
        idEditor.setHint("成就id");
        linear.addView(idEditor);
        EditText stepEditor;
        if (!unlock) {
            TextView tip = new TextView(getContext());
            tip.setTextSize(14);
            tip.setText("按照step更新进度\n如后台配置此成就需要100step,则step最大值为100\n此处输入值为当前最大step值，非增量！！！");
            stepEditor = new EditText(getContext());
            stepEditor.setHint("新进度");
            stepEditor.setInputType(EditorInfo.TYPE_NUMBER_FLAG_DECIMAL);
            linear.addView(stepEditor);
        } else {
            stepEditor = null;
        }
        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(linear)
                .setNegativeButton("关闭", null)
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String achievement_id = idEditor.getText().toString();
                        if (TextUtils.isEmpty(achievement_id)) {
                            error.setText("成就id无效");
                            return;
                        }
                        int step = 0;
                        if (!unlock) {
                            try {
                                String step_str = stepEditor.getText().toString();
                                step = Integer.parseInt(step_str);
                                if (step <= 0) {
                                    error.setText("step 输入无效");
                                    return;
                                }
                            } catch (Exception ignored) {
                                error.setText("step 输入无效");
                                return;
                            }
                        }
                        if (callback != null) {
                            callback.onResult(achievement_id, step);
                        }
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

}
