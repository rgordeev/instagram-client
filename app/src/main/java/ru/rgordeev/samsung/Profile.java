package ru.rgordeev.samsung;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Profile extends AppCompatActivity {

    private final int IMAGE_REQUEST_CODE = 12;

    @RequiresApi(api = Build.VERSION_CODES.P)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Uri selectedImage = data.getData();
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            try {
                Bitmap bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(this.getContentResolver(), selectedImage));
                Log.i("Image Selected", "FINE!!!!!!!!!!");
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                byte[] pic = outputStream.toByteArray();
                String fileName = UUID.randomUUID().toString();
                new UploadThread(pic, personId, fileName).execute();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.upload_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.upload) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, IMAGE_REQUEST_CODE);
            } else {
                getPhoto();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void getPhoto() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == IMAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getPhoto();
            }
        }
    }

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

    private class UploadThread extends AsyncTask<Void, Void, Void> {
        private final byte[] file;
        private final Long personId;
        private final String fileName;

        private UploadThread(byte[] file, Long personId, String fileName) {
            this.file = file;
            this.personId = personId;
            this.fileName = fileName;
        }


        @Override
        protected Void doInBackground(Void... voids) {
            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", fileName,
                            RequestBody.create(file, MediaType.get("image/png")))
                    .build();

            Request request = new Request.Builder()
                    .url("http://10.0.2.2:8080/files/?personId=" + personId)
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                Log.i("UPLOAD FILE", response.body().string());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
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
