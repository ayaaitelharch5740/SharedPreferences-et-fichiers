package com.example.securestoragelabjava.ui;import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.securestoragejava.R;
import com.example.securestoragejava.cache.CacheStore;
import com.example.securestoragejava.files.InternalTextStore;
import com.example.securestoragejava.files.StudentsJsonStore;
import com.example.securestoragejava.model.Student;
import com.example.securestoragejava.prefs.AppPrefs;
import com.example.securestoragejava.prefs.SecurePrefs;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SecureStorageJava";
    private final List<String> langs = Arrays.asList("fr", "en", "ar");

    private EditText etName;
    private EditText etToken;
    private Spinner spLang;
    private Switch swDark;
    private TextView tvResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etName = findViewById(R.id.etName);
        etToken = findViewById(R.id.etToken);
        spLang = findViewById(R.id.spLang);
        swDark = findViewById(R.id.swDark);
        tvResult = findViewById(R.id.tvResult);

        setupLangSpinner();

        Button btnSavePrefs = findViewById(R.id.btnSavePrefs);
        Button btnLoadPrefs = findViewById(R.id.btnLoadPrefs);
        Button btnSaveJson = findViewById(R.id.btnSaveJson);
        Button btnLoadJson = findViewById(R.id.btnLoadJson);
        Button btnClear = findViewById(R.id.btnClear);

        btnSavePrefs.setOnClickListener(v -> savePrefs());
        btnLoadPrefs.setOnClickListener(v -> loadPrefsToUi());
        btnSaveJson.setOnClickListener(v -> saveJsonFile());
        btnLoadJson.setOnClickListener(v -> loadJsonFile());
        btnClear.setOnClickListener(v -> clearAll());

        loadPrefsToUi();
    }

    private void setupLangSpinner() {
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, langs);
        spLang.setAdapter(adapter);
    }

    private void savePrefs() {
        String name = etName.getText().toString().trim();
        String lang = langs.get(Math.max(0, spLang.getSelectedItemPosition()));
        String theme = swDark.isChecked() ? "dark" : "light";

        boolean ok = AppPrefs.save(this, name, lang, theme, false);

        String token = etToken.getText().toString();
        if (!token.isBlank()) {
            try {
                SecurePrefs.saveToken(this, token);
            } catch (Exception e) {
                tvResult.setText("Erreur chiffrement token : " + e.getMessage());
                return;
            }
        }

        // Ne pas logger le token : uniquement infos non sensibles
        Log.d(TAG, "Prefs sauvegardées ok=" + ok + ", name=" + name + ", lang=" + lang + ", theme=" + theme);

        try {
            CacheStore.write(this, "last_ui.txt", "name=" + name + ", lang=" + lang + ", theme=" + theme);
        } catch (Exception ignored) {}

        tvResult.setText(
                "Sauvegarde prefs terminée.\n" +
                        "name=" + name + "\n" +
                        "lang=" + lang + "\n" +
                        "theme=" + theme + "\n" +
                        "token: stocké chiffré si non vide (non affiché)."
        );
    }

    private void loadPrefsToUi() {
        AppPrefs.Triple triple = AppPrefs.load(this);

        etName.setText(triple.name);
        swDark.setChecked("dark".equals(triple.theme));

        int idx = langs.indexOf(triple.lang);
        spLang.setSelection(idx >= 0 ? idx : 0);

        int tokenLen = 0;
        try {
            String token = SecurePrefs.loadToken(this);
            tokenLen = token == null ? 0 : token.length();
        } catch (Exception ignored) {}

        tvResult.setText(
                "Chargement prefs terminé.\n" +
                        "name=" + triple.name + "\n" +
                        "lang=" + triple.lang + "\n" +
                        "theme=" + triple.theme + "\n" +
                        "tokenLength=" + tokenLen
        );

        Log.d(TAG, "Prefs chargées name=" + triple.name + ", lang=" + triple.lang + ", theme=" + triple.theme + ", tokenLength=" + tokenLen);
    }

    private void saveJsonFile() {
        List<Student> students = Arrays.asList(
                new Student(1, "Amina", 20),
                new Student(2, "Omar", 21),
                new Student(3, "Sara", 19)
        );

        try {
            StudentsJsonStore.save(this, students);
            InternalTextStore.writeUtf8(this, "note.txt", "Sauvegarde JSON effectuée (UTF-8).");
        } catch (Exception e) {
            tvResult.setText("Erreur sauvegarde JSON : " + e.getMessage());
            return;
        }

        Log.d(TAG, "Fichiers internes écrits: students.json, note.txt");
        tvResult.setText("Sauvegarde fichier JSON terminée. students=" + students.size());
    }

    private void loadJsonFile() {
        List<Student> students = StudentsJsonStore.load(this);

        String note;
        try {
            note = InternalTextStore.readUtf8(this, "note.txt");
        } catch (Exception e) {
            note = "(note.txt absent)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Chargement fichier JSON terminé.\n");
        sb.append("note=").append(note).append("\n");
        sb.append("students=").append(students.size()).append("\n");
        for (Student s : students) {
            sb.append(" - id=").append(s.id)
                    .append(", name=").append(s.name)
                    .append(", age=").append(s.age)
                    .append("\n");
        }

        tvResult.setText(sb.toString());
        Log.d(TAG, "Fichier JSON chargé: students=" + students.size());
    }

    private void clearAll() {
        AppPrefs.clear(this);

        try {
            SecurePrefs.clear(this);
        } catch (Exception ignored) {}

        StudentsJsonStore.delete(this);
        InternalTextStore.delete(this, "note.txt");

        int purged = CacheStore.purge(this);

        etName.setText("");
        etToken.setText("");
        swDark.setChecked(false);
        spLang.setSelection(0);

        tvResult.setText(
                "Nettoyage terminé.\n" +
                        "prefs: clear()\n" +
                        "secure_prefs: clear()\n" +
                        "students.json: delete\n" +
                        "note.txt: delete\n" +
                        "cache purgé: " + purged + " fichier(s)"
        );

        Log.d(TAG, "Nettoyage terminé (aucune donnée sensible loggée).");
    }
}