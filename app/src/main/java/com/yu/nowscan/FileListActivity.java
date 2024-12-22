package com.yu.nowscan;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_list);

        ListView fileListView = findViewById(R.id.fileListView);
        File[] files = getFilesDir().listFiles();

        List<String> fileNames = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
                fileNames.add(file.getName());
            }
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, fileNames);
        fileListView.setAdapter(adapter);

        fileListView.setOnItemClickListener((parent, view, position, id) -> {
            String fileName = fileNames.get(position);
            showFileContentDialog(fileName);
        });
    }

    private void showFileContentDialog(String fileName) {
        try {
            // 파일 내용 읽기
            File file = new File(getFilesDir(), fileName);
            FileReader reader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(reader);

            StringBuilder content = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                content.append(line).append("\n");
            }
            bufferedReader.close();

            // 팝업창 생성
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(fileName);

            // 스크롤 가능한 TextView 추가
            ScrollView scrollView = new ScrollView(this);
            TextView textView = new TextView(this);
            textView.setText(content.toString());
            textView.setPadding(16, 16, 16, 16);
            scrollView.addView(textView);
            builder.setView(scrollView);

            // 삭제 버튼 추가
            builder.setNegativeButton("삭제", (dialog, which) -> {
                delete(fileName);
                dialog.dismiss();
            });

            // 닫기 버튼 추가
            builder.setPositiveButton("닫기", (dialog, which) -> dialog.dismiss());

            builder.show();

        } catch (IOException e) {
            Toast.makeText(this, "파일을 읽는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void delete(String fileName) {
        File file = new File(getFilesDir(), fileName);
        if (file.exists() && file.delete()) {
            Toast.makeText(this, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
            recreate(); // 파일 삭제 후 리스트 갱신
        } else {
            Toast.makeText(this, "파일 삭제에 실패했습니다.", Toast.LENGTH_SHORT).show();
        }
    }
}
