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
import ru.rgordeev.samsung.model.Person;

public class MainActivity extends AppCompatActivity {

    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.people);
//        List<String> people = new ArrayList<>();
//        people.add("Name");
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, people);
//        listView.setAdapter(adapter);
        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new HttpThread().execute();
            }
        });

    }

    private class HttpThread extends AsyncTask<Void, Void, List<Person>> {

        @Override
        protected void onPostExecute(List<Person> people) {

            PersonAdapter adapter = new PersonAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, people);
            listView.setAdapter(adapter);
        }

        @Override
        protected List<Person> doInBackground(Void... voids) {
            OkHttpClient httpClient = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/person/")
                    .get()
                    .build();

            List<Person> people = new ArrayList<>();

            try(Response response = httpClient.newCall(request).execute()) {
                GsonBuilder gsonBuilder = new GsonBuilder();
                people = Arrays.asList(gsonBuilder.create().fromJson(response.body().string(), Person[].class));
            } catch (IOException e) {
                e.printStackTrace();
            }

            return people;
        }
    }

    private static class PersonAdapter extends ArrayAdapter<Person> {

        public PersonAdapter(@NonNull Context context, int resource, @NonNull List<Person> objects) {
            super(context, resource, objects);
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            Person person = getItem(position);
            LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.person, null);

            PersonHolder holder = new PersonHolder();
            holder.personName = convertView.findViewById(R.id.personName);
            holder.personBio = convertView.findViewById(R.id.personBio);
            holder.avatar = convertView.findViewById(R.id.avatar);

            RequestOptions options = new RequestOptions()
                    .fitCenter()
                    .override(200, 200)
                    .centerCrop()
                    .transform(new RoundedCorners(10));

            String imageId = "EMPTY";
            if (person.getAvatar() != null) {
                imageId = person.getAvatar().getId().toString();
            }
            final String url = "http://10.0.2.2:8080/files/" + imageId;

            Glide.with(getContext())
                    .load(url)
                    .apply(options)
                    .into(holder.avatar);

            holder.personName.setText(String.format("%s %s", person.getName(), person.getLastName()));
            holder.personBio.setText(person.getBio());

            convertView.setTag(holder);
            return convertView;
        }
    }

    private static class PersonHolder {
        public ImageView avatar;
        public TextView personName;
        public TextView personBio;
    }
}
