package com.yu.nowscan;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView textView = findViewById(R.id.resultTextView);
        Button closeButton = findViewById(R.id.closeButton);

        // 전달받은 데이터 표시
        String data = getIntent().getStringExtra("data");
        textView.setText(data);

        // 닫기 버튼 클릭 시 액티비티 종료
        closeButton.setOnClickListener(v -> finish());
    }
}
