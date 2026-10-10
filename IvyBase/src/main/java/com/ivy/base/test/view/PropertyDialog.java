package com.ivy.base.test.view;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ivy.base.R;

import java.util.LinkedHashMap;
import java.util.Map;

import java.util.Map;

public class PropertyDialog extends Dialog {

    public interface OnConfirmListener {
        void onConfirm(PropertyData data);
    }

    private EditText etEventName;
    private LinearLayout llPropertyContainer;
    private TextView btnAddProperty;
    private Button btnCancel;
    private Button btnConfirm;

    private final OnConfirmListener listener;

    public PropertyDialog(@NonNull Context context, OnConfirmListener listener) {
        super(context);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.func_property_dialog);

        // 设置窗口宽度和透明背景
        if (getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            int width = (int) (getContext().getResources().getDisplayMetrics().widthPixels * 0.9);
            getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        initViews();
        addPropertyRow(); // 默认显示一行属性
    }

    private void initViews() {
        etEventName = findViewById(R.id.etEventName);
        llPropertyContainer = findViewById(R.id.llPropertyContainer);
        btnAddProperty = findViewById(R.id.btnAddProperty);
        btnCancel = findViewById(R.id.btnCancel);
        btnConfirm = findViewById(R.id.btnConfirm);

        btnAddProperty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addPropertyRow();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                collectAndConfirm();
            }
        });
    }

    private void addPropertyRow() {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        final View row = inflater.inflate(R.layout.item_property, llPropertyContainer, false);

        ImageView btnDelete = row.findViewById(R.id.btnDelete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                llPropertyContainer.removeView(row);
            }
        });

        llPropertyContainer.addView(row);
    }

    private void collectAndConfirm() {
        String eventName = etEventName.getText().toString().trim();
        if (TextUtils.isEmpty(eventName)) {
            Toast.makeText(getContext(), "请输入事件名称", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        boolean hasError = false;

        for (int i = 0; i < llPropertyContainer.getChildCount(); i++) {
            View row = llPropertyContainer.getChildAt(i);
            EditText etKey = row.findViewById(R.id.etKey);
            EditText etValue = row.findViewById(R.id.etValue);
            Spinner etSpinner = row.findViewById(R.id.spType);

            String key = etKey.getText().toString().trim();
            String raw = etValue.getText().toString().trim();

            if (TextUtils.isEmpty(key)) {
                etKey.setError("Key 不能为空");
                hasError = true;
            } else {
                Object value;
                switch (etSpinner.getSelectedItem().toString()) {
                    case "Long":    value = Long.parseLong(raw); break;
                    case "Double":  value = Double.parseDouble(raw); break;
                    case "Boolean": value = "true".equalsIgnoreCase(raw); break;
                    default:        value = raw; break;
                }

                properties.put(key, value);
            }
        }

        if (hasError) return;

        if (listener != null) {
            listener.onConfirm(new PropertyData(eventName, properties));
        }
        dismiss();
    }
}