package dev.truewinter.calenro;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        TextView aboutVersion = findViewById(R.id.about_version);
        aboutVersion.setText(getResources().getString(R.string.app_version)
                .replace("{{version}}", BuildConfig.VERSION_NAME));

        TextView githubLink = findViewById(R.id.about_github_link);
        githubLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://github.com/TrueWinter/calenro"));
                startActivity(intent);
            }
        });

        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(getResources().getAssets().open("licenses.txt")));

            StringBuilder licenses = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                licenses.append(inputLine);
                licenses.append("\n");
            }

            // Remove unnecessary line breaks in licenses
            String licensesToShow = licenses.toString();
            // Breaks between paragraphs are ok, so replace them with something else
            licensesToShow = licensesToShow.replace("\n\n", "{{br}}")
                    // Breaks within a paragraph are not, so remove them
                    .replace("\n", " ")
                    // And finally, add paragraph breaks back in
                    .replace("{{br}}", "\n\n")
                    // Also, allow for singular line breaks to be manually placed
                    .replace("{{br1}}", "\n");

            ((TextView) findViewById(R.id.about_licenses_view)).setText(licensesToShow);
        } catch (IOException e) {
            e.printStackTrace();
            ((TextView) findViewById(R.id.about_licenses_view)).setText(getText(R.string.about_licenses_failed));
        }
    }
}
