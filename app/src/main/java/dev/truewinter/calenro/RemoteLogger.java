package dev.truewinter.calenro;

import android.content.Context;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/*
    The RemoteLogger class is used for debugging during development during times where it is
    impractical or impossible to have my phone connected to my computer to get the app logs,
    such as when doing testing overnight or when I'm not in my office.

    It is not used in the public version of the app.
*/
public class RemoteLogger {
    public static void log(final JSONObject data, Context context) {
        String loggerUrl = null;

        try {
            String[] assetList = context.getResources().getAssets().list("");
            if (assetList == null) return;
            if (!Arrays.asList(assetList).contains("datalog.json")) return;

            BufferedReader in = new BufferedReader(new InputStreamReader(context.getResources()
                    .getAssets().open("datalog.json")));

            StringBuilder jsonString = new StringBuilder();

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                jsonString.append(inputLine);
            }

            JSONObject json = new JSONObject(jsonString.toString());
            loggerUrl = json.getString("url");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (loggerUrl == null) return;

        String finalLoggerUrl = loggerUrl;
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(finalLoggerUrl);
                    HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());

                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
                    writer.write(data.toString());
                    writer.flush();
                    writer.close();
                    out.close();

                    try(BufferedReader br = new BufferedReader(
                            new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine = null;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        System.out.println(response.toString());
                    }

                    urlConnection.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();
    }
}
