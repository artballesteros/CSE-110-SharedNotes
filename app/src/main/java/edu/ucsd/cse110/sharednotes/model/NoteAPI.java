package edu.ucsd.cse110.sharednotes.model;

import android.util.Log;

import androidx.annotation.AnyThread;
import androidx.annotation.MainThread;
import androidx.annotation.WorkerThread;

import com.google.gson.Gson;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class NoteAPI {

    private volatile static NoteAPI instance = null;

    private OkHttpClient client;

    public NoteAPI() {
        this.client = new OkHttpClient();
    }

    public static NoteAPI provide() {
        if (instance == null) {
            instance = new NoteAPI();
        }
        return instance;
    }

    /**
     * An example of sending a GET request to the server.
     *
     * The /echo/{msg} endpoint always just returns {"message": msg}.
     *
     * This method should can be called on a background thread (Android
     * disallows network requests on the main thread).
     */
    @WorkerThread
    public String echo(String msg) {
        // URLs cannot contain spaces, so we replace them with %20.
        String encodedMsg = msg.replace(" ", "%20");

        var request = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/echo/" + encodedMsg)
                .method("GET", null)
                .build();

        try (var response = client.newCall(request).execute()) {
            assert response.body() != null;
            var body = response.body().string();
            Log.i("ECHO", body);
            return body;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @AnyThread
    public Future<String> echoAsync(String msg) {
        var executor = Executors.newSingleThreadExecutor();
        var future = executor.submit(() -> echo(msg));

        // We can use future.get(1, SECONDS) to wait for the result.
        return future;
    }

    public Note postNote(Note note) {
        var json = note.toJSON();
        String encodedMsg = note.title.replace(" ", "%20");
        var req = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedMsg)
                .method("PUT", RequestBody.create(json, MediaType.parse("application/json")))
                .build();
        try (var res = client.newCall(req).execute()) {
            var resJson = res.body().string();
            return Note.fromJSON(resJson);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Note getNote(String title) {
        String encodedMsg = title.replace(" ", "%20");
        var req = new Request.Builder()
                .url("https://sharednotes.goto.ucsd.edu/notes/" + encodedMsg    )
                .method("GET", null)
                .build();
        try (var res = client.newCall(req).execute()) {
            var resStr = res.body().string();
            var gson = new Gson();
            Map<String, ?> map = gson.fromJson(resStr, Map.class);
            if (map.containsKey("detail")) {
                return null;
            }
            return Note.fromJSON(resStr);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}