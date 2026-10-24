
package git.artdeell.mojo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ModrinthActivity extends Activity {
    private final ExecutorService worker =
            Executors.newSingleThreadExecutor();

    private EditText query, version;
    private LinearLayout results;
    private TextView status;
    private ProgressBar progress;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24, 24, 24, 12);
        root.setBackgroundColor(0xFF071A15);

        TextView title = new TextView(this);
        title.setText("NeonCraft • Modrinth");
        title.setTextSize(23);
        title.setTextColor(0xFF35E6B8);
        root.addView(title);

        query = new EditText(this);
        query.setSingleLine(true);
        query.setHint("Название мода");
        query.setTextColor(0xFFFFFFFF);
        root.addView(query);

        version = new EditText(this);
        version.setSingleLine(true);
        version.setHint("Версия Minecraft, например 1.21.1");
        version.setText("1.21.1");
        version.setTextColor(0xFFFFFFFF);
        root.addView(version);

        Button search = new Button(this);
        search.setText("Найти моды Fabric");
        root.addView(search);
        search.setOnClickListener(v -> searchMods());

        progress = new ProgressBar(this);
        progress.setVisibility(View.GONE);
        root.addView(progress);

        status = new TextView(this);
        status.setTextColor(0xFFB7D8CE);
        root.addView(status);

        ScrollView scroll = new ScrollView(this);
        results = new LinearLayout(this);
        results.setOrientation(1);
        scroll.addView(results);
        root.addView(scroll, new LinearLayout.LayoutParams(
                -1, 0, 1));

        setContentView(root);
        searchMods();
    }

    private void searchMods() {
        String q = query.getText().toString().trim();
        String ver = version.getText().toString().trim();

        if (!ver.matches("\\d+\\.\\d+(\\.\\d+)?")) {
            version.setError("Неверный формат версии");
            return;
        }

        progress.setVisibility(View.VISIBLE);
        status.setText("Загружаем каталог...");
        results.removeAllViews();

        worker.execute(() -> {
            try {
                String facets =
                    "[[\"project_type:mod\"]," +
                    "[\"categories:fabric\"]," +
                    "[\"versions:" + ver + "\"]]";

                String address =
                    "https://api.modrinth.com/v2/search?query=" +
                    URLEncoder.encode(q, "UTF-8") +
                    "&facets=" +
                    URLEncoder.encode(facets, "UTF-8") +
                    "&limit=25&index=downloads";

                HttpURLConnection c = (HttpURLConnection)
                    new URL(address).openConnection();
                c.setConnectTimeout(15000);
                c.setReadTimeout(20000);
                c.setRequestProperty("User-Agent",
                    "NeonCraft/0.1.0");

                StringBuilder body = new StringBuilder();
                try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                        c.getInputStream(), "UTF-8"))) {
                    String line;
                    while ((line = reader.readLine()) != null)
                        body.append(line);
                } finally {
                    c.disconnect();
                }

                JSONArray hits =
                    new JSONObject(body.toString())
                        .getJSONArray("hits");

                runOnUiThread(() -> displayResults(hits, ver));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progress.setVisibility(View.GONE);
                    status.setText("Ошибка: " + e.getMessage());
                });
            }
        });
    }

    private void displayResults(JSONArray hits, String ver) {
        progress.setVisibility(View.GONE);
        results.removeAllViews();
        status.setText("Найдено: " + hits.length()
            + " • Fabric • Minecraft " + ver);

        for (int i = 0; i < hits.length(); i++) {
            JSONObject mod = hits.optJSONObject(i);
            if (mod == null) continue;

            TextView card = new TextView(this);
            card.setText(
                mod.optString("title", "Без названия") +
                "\n" + mod.optString("description", "") +
                "\nЗагрузок: " + mod.optLong("downloads", 0)
            );
            card.setTextColor(0xFFFFFFFF);
            card.setTextSize(15);
            card.setPadding(16, 16, 16, 16);
            card.setBackgroundColor(0xFF123B32);

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);
            params.bottomMargin = 12;
            results.addView(card, params);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        worker.shutdownNow();
    }
}
