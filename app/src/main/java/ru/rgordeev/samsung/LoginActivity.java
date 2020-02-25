package ru.rgordeev.samsung;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import ru.rgordeev.samsung.model.Person;

public class LoginActivity extends AppCompatActivity {

    private EditText login;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        Button button = findViewById(R.id.signIn);
        login = findViewById(R.id.login);
        password = findViewById(R.id.password);
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
            String l = login.getText().toString();
            String p = password.getText().toString();
            if (isSignIn(people, l, p)) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(getApplicationContext(), "Login or password is wrong!", Toast.LENGTH_SHORT).show();
            }
        }

        private boolean isSignIn(List<Person> people, String login, String password) {
            for (Person person: people) {
                if (Objects.equals(login, person.getLogin()) && Objects.equals(password, person.getPassword()))
                    return true;
            }
            return false;
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
}
