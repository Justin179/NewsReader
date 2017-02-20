package com.example.justin179.newsreader;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> titles = new ArrayList<>();
    ArrayList<String> content = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase articlesDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ListView listView = (ListView) findViewById(R.id.listView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(getApplicationContext(),ArticleActivity.class);
                intent.putExtra("content",content.get(position));
                startActivity(intent);
            }
        });

        articlesDB = this.openOrCreateDatabase("Articles", MODE_PRIVATE,null);
        articlesDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, articleId INTEGER, title VARCHAR, content VARCHAR)");

        updateListView();

        // download the content of url
        DownloadTask task = new DownloadTask();

        try{
           // task.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        } catch(Exception e){
            e.printStackTrace();
        }


    }

    public void updateListView(){
        Cursor c = articlesDB.rawQuery("SELECT * FROM articles",null);

        int contentIndex = c.getColumnIndex("content");
        int titleIndex = c.getColumnIndex("title");

        if(c.moveToFirst()){
            titles.clear();
            content.clear();

            do {
                titles.add(c.getString(titleIndex));
                content.add(c.getString(contentIndex));
            } while(c.moveToNext());

            arrayAdapter.notifyDataSetChanged();

        }

    }

    // https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty

    public class DownloadTask extends AsyncTask<String,Void,String> {
        @Override
        protected String doInBackground(String... params) {

            String result = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(params[0]);

                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream in = urlConnection.getInputStream();
                InputStreamReader reader = new InputStreamReader(in);

                int data = reader.read();
                while(data!=-1){
                    char current = (char) data;
                    result += current;
                    data = reader.read();
                }

                Log.i("URLContent",result);

                // [ 13684368, 13683110, 13684387, 13684429, 13684303,...]
                JSONArray jsonArray = new JSONArray(result);

                int numberOfItems = 20;
                if(jsonArray.length()<20){
                    numberOfItems = jsonArray.length();
                }

                articlesDB.execSQL("DELETE FROM articles");

                for(int i = 0; i < numberOfItems; i++){

                    String articleId = jsonArray.getString(i);

                    url = new URL("https://hacker-news.firebaseio.com/v0/item/"+articleId+".json?print=pretty");

                    urlConnection = (HttpURLConnection) url.openConnection();
                    in = urlConnection.getInputStream();
                    reader = new InputStreamReader(in);

                    data = reader.read();

                    StringBuilder sb = new StringBuilder();

                    while(data!=-1){
                        char current = (char) data;
                        sb.append(current);
                        data = reader.read();
                    }

                    //Log.i("ArticleInfo", sb.toString());
                    JSONObject jsonObject = new JSONObject(sb.toString());

                    if(!jsonObject.isNull("title") && !jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        //Log.i("info: ",articleTitle + articleUrl);

                        url = new URL(articleUrl);

                        urlConnection = (HttpURLConnection) url.openConnection();
                        in = urlConnection.getInputStream();
                        reader = new InputStreamReader(in);

                        data = reader.read();

                        StringBuilder sb2 = new StringBuilder();

                        while(data!=-1){
                            char current = (char) data;
                            sb2.append(current);
                            data = reader.read();
                        }


                        String articleContent = sb2.toString();
                        Log.i("article content ", sb2.toString());

                        String sql = "INSERT INTO articles (articleId, title, content) VALUES (?,?,?)";
                        SQLiteStatement statement = articlesDB.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);
                        statement.execute();

                    }




                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }

}
