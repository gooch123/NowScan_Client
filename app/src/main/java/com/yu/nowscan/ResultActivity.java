package com.yu.nowscan;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResultActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        TextView textView = findViewById(R.id.resultTextView);
        Button closeButton = findViewById(R.id.closeButton);
        Button saveButton = findViewById(R.id.saveButton);

        // 전달받은 데이터 표시
        String data = getIntent().getStringExtra("data");
        textView.setText(data);

        // 닫기 버튼 클릭 시 액티비티 종료
        closeButton.setOnClickListener(v -> finish());
        closeButton.setBackgroundColor(Color.RED);

        // 저장 버튼 클릭 시 저장 다이얼로그 표시
        saveButton.setOnClickListener(v -> showSaveDialog(data));
        saveButton.setBackgroundColor(Color.CYAN);
    }

    private void showSaveDialog(String data) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("번역된 텍스트 저장");

        // 제목 입력 EditText 추가
        EditText input = new EditText(this);
        input.setHint("파일 이름을 입력하세요");
        builder.setView(input);

        // Save 버튼 클릭 시 파일 저장
        builder.setPositiveButton("저장", (dialog, which) -> {
            String fileName = input.getText().toString().trim();
            if (!fileName.isEmpty()) {
                saveToFile(fileName, data);
            } else {
                Toast.makeText(this, "파일 이름을 입력하세요", Toast.LENGTH_SHORT).show();
            }
        });

        // Cancel 버튼 클릭 시 다이얼로그 닫기
        builder.setNegativeButton("취소", (dialog, which) -> dialog.dismiss());

        builder.show();
    }

    private void saveToFile(String fileName, String content) {
        try {
            File file = new File(getFilesDir(), fileName + ".txt");
            FileWriter writer = new FileWriter(file);
            writer.write(content);
            writer.close();
            Toast.makeText(this, "파일이 저장되었습니다: " + fileName, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "파일 저장 실패", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }
}
