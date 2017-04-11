package com.tutsplus.mysmartapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.kittinunf.fuel.Fuel;
import com.github.kittinunf.fuel.core.FuelError;
import com.github.kittinunf.fuel.core.Handler;
import com.github.kittinunf.fuel.core.Request;
import com.github.kittinunf.fuel.core.Response;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import kotlin.Pair;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public final static int MY_REQUEST_CODE = 1;

    public void takePicture(View view) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, MY_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MY_REQUEST_CODE && resultCode == RESULT_OK) {
            Bitmap picture = (Bitmap)data.getExtras().get("data");
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            picture.compress(Bitmap.CompressFormat.JPEG, 90, byteStream);

            ((ImageView)findViewById(R.id.previewImage)).setImageBitmap(picture);

            String base64Data = Base64.encodeToString(byteStream.toByteArray(), Base64.URL_SAFE);
            recognizeObjects(base64Data);
        }
    }

    private void recognizeObjects(String base64Data) {
        String requestURL = "https://vision.googleapis.com/v1/images:annotate?key=" +
                getResources().getString(R.string.mykey);

        try {
            JSONArray features = new JSONArray();
            JSONObject feature = new JSONObject();
            feature.put("type", "LABEL_DETECTION");
            features.put(feature);

            JSONObject imageContent = new JSONObject();
            imageContent.put("content", base64Data);

            JSONArray requests = new JSONArray();
            JSONObject request = new JSONObject();
            request.put("image", imageContent);
            request.put("features", features);
            requests.put(request);

            JSONObject postData = new JSONObject();
            postData.put("requests", requests);

            String body = postData.toString();

            Fuel.post(requestURL)
                    .header(new Pair<String, Object>("content-length", body.length()),
                            new Pair<String, Object>("content-type", "application/json"))
                    .body(body.getBytes()).responseString(new Handler<String>() {
                @Override
                public void success(@NotNull Request request, @NotNull Response response, String data) {
                    try {
                        JSONArray labels = new JSONObject(data).getJSONArray("responses").getJSONObject(0)
                                .getJSONArray("labelAnnotations");

                        String results = "";

                        for(int i=0;i<labels.length();i++) {
                            results = results + labels.getJSONObject(i).getString("description") + "\n";
                        }

                        ((TextView)findViewById(R.id.resultsText)).setText(results);
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void failure(@NotNull Request request, @NotNull Response response,
                                    @NotNull FuelError fuelError) {

                }
            });
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void translateToGerman(View view) {
        String requestURL = "https://translation.googleapis.com/language/translate/v2";
        List<Pair<String, String>> params = new ArrayList<>();
        params.add(new Pair<String, String>("key", getResources().getString(R.string.mykey)));
        params.add(new Pair<String, String>("source", "en"));
        params.add(new Pair<String, String>("target", "de"));

        String[] queries = ((TextView)findViewById(R.id.resultsText)).getText().toString().split("\n");
        for(String query:queries) {
            params.add(new Pair<String, String>("q", query));
        }

        Fuel.get(requestURL, params).responseString(new Handler<String>() {
            @Override
            public void success(@NotNull Request request, @NotNull Response response, String data) {
                try {
                    JSONArray translations = new JSONObject(data)
                            .getJSONObject("data")
                            .getJSONArray("translations");

                    String result = "";
                    for(int i=0;i<translations.length();i++) {
                        result += translations.getJSONObject(i).getString("translatedText") + "\n";
                    }

                    ((TextView)findViewById(R.id.resultsText)).setText(result);
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failure(@NotNull Request request, @NotNull Response response, @NotNull FuelError fuelError) {

            }
        });
    }
}
