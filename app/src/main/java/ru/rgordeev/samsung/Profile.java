package ru.rgordeev.samsung;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Profile extends AppCompatActivity {

    ListView listView;
    Long personId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        personId = getIntent().getLongExtra("personId", 0);
        listView = findViewById(R.id.images);
        new Profile.HttpThread().execute();
    }

    private class HttpThread extends AsyncTask<Void, Void, List<Long>> {

        @Override
        protected void onPostExecute(List<Long> files) {

            FilesAdapter adapter = new FilesAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, files);
            listView.setAdapter(adapter);
        }

        @Override
        protected List<Long> doInBackground(Void... voids) {
            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/files/person/" + personId)
                    .get()
                    .build();

            List<Long> filesIds = new ArrayList<>();

            try(Response response = httpClient.newCall(request).execute()) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                filesIds = Arrays.asList(gsonBuilder.create().fromJson(response.body().string(), Long[].class));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return filesIds;
        }
    }

    private static class FilesAdapter extends ArrayAdapter<Long> {

        public FilesAdapter(@NonNull Context context, int resource, @NonNull List<Long> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Long imageId = getItem(position);
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.images, null);

            ImageHolder holder = new ImageHolder();
            holder.image = convertView.findViewById(R.id.image);

            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .override(700, 700)
                    .centerCrop()
                    .transform(new RoundedCorners(5));

            final String url = "http://10.0.2.2:8080/files/id/" + imageId;

            Glide.with(getContext())
                    .load(url)
                    .apply(options)
                    .into(holder.image);

            convertView.setTag(holder);
            return convertView;
        }
    }

    private static class ImageHolder {
        public ImageView image;
    }
}
