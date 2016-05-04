package com.test.reddit.redditsimpleclient;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private JSONArray posts;
    private ProgressBar progressBar;
    private int page = 0;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        progressBar = (ProgressBar)findViewById(R.id.main_progress);
        scrollView = (ScrollView)findViewById(R.id.main_scroll);
        new RedditTask().execute("50");
    }

    /**
     *  Show the content stored in the array
     */
    private void showContent(){

        int from  = page * 10;
        int to = (page+1) * 10;

        LinearLayout l = (LinearLayout)findViewById(R.id.main_container);
        l.removeAllViews();

        for (int i = from; i < to; i++){
            try {
                JSONObject obj = (JSONObject)posts.get(i);
                final JSONObject data = (JSONObject)obj.get("data");

                ItemView v = new ItemView(this);
                v.setTitle(data.get("title").toString());
                v.setDate((Double) data.get("created_utc"));
                v.setUser(data.get("author").toString());
                v.setComments(data.get("num_comments").toString());
                String thumb = data.get("thumbnail").toString();
                if(!thumb.isEmpty() && !thumb.equals("self") && !thumb.equals("nsfw")){
                    new DownloadImageTask(v.getPictureView()).execute(thumb,"thumb");

                    v.getPictureView().setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                if (data.get("preview") != null) {
                                    JSONArray array = (JSONArray) ((JSONObject) data.get("preview")).get("images");
                                    String url = ((JSONObject) ((JSONObject) array.get(0)).get("source")).get("url").toString();
                                    Dialog d = new Dialog(MainActivity.this);
                                    d.requestWindowFeature(Window.FEATURE_NO_TITLE);
                                    d.setContentView(R.layout.dialog_layout);
                                    ImageView dialogImageView = (ImageView) d.findViewById(R.id.dialog_image);
                                    new DownloadImageTask(dialogImageView).execute(url);
                                    d.show();
                                }

                            } catch (Exception e) {
                                Log.d(TAG, e.getMessage(), e);
                            }
                        }
                    });
                }else{
                    v.getPictureView().setImageResource(R.drawable.ic_no_image);
                }
                l.addView(v);

            }catch (Exception ex) {
                Log.d(TAG,ex.getMessage(),ex);
            }
        }
    }

    /**
     *  Parse the content of an inputstream
     *  and return a string
     * @param in InputStream
     * @return String
     */
    private String parseContent(InputStream in){
        StringBuilder b = new StringBuilder();
        String line;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while ((line=br.readLine())!=null){
                b.append(line);
            }
        }catch (Exception ex){
            Log.d(TAG,ex.getMessage(),ex);
        }
        return b.toString();
    }

    /**
     *  move to next page
     * @param view
     */
    public void next(View view){
        move(++page);
    }

    /**
     *  move to previous page
     * @param view
     */
    public void previous(View view){
        move(--page);
    }

    /**
     * Move to a particular page number
     * @param page
     */
    private void move(int page){

        scrollView.fullScroll(ScrollView.FOCUS_UP);
        showContent();

        findViewById(R.id.main_next).setEnabled(page != 4);
        findViewById(R.id.main_previous).setEnabled(page != 0);
    }

    /**
     * Once the use tap the image, save it into the gallery
     * @param view
     */
    public void saveImage(View view){
        ImageView imageView = (ImageView)view;
        imageView.setDrawingCacheEnabled(true);
        Bitmap b = imageView.getDrawingCache();
        MediaStore.Images.Media.insertImage(getContentResolver(), b,"", getText(R.string.image_saved).toString());
        Toast.makeText(this,getText(R.string.image_saved_success),Toast.LENGTH_LONG).show();
    }

    /**
     *  Async task to get results from reddit
     */
    private class RedditTask extends AsyncTask<String, Float, Integer>{

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Integer doInBackground(String... params) {
            String uri = "https://www.reddit.com/top.json?count=0&limit=" + params[0];
            try {
                URL url = new URL(uri);
                url.openConnection();
                String content = parseContent((InputStream) url.getContent());
                JSONObject root = new JSONObject(content);
                posts = (JSONArray)((JSONObject)root.get("data")).get("children");
            }catch (Exception ex) {
                Log.d(TAG, ex.getMessage(), ex);
            }
            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            showContent();
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Async task to download images on the background
     */
    private class DownloadImageTask extends AsyncTask<String,Void,Bitmap>{

        private ImageView dest;

        public DownloadImageTask(ImageView dest){
            this.dest = dest;
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            Bitmap ret = null;
            try {
                InputStream in = new java.net.URL(url).openStream();
                if(params.length > 1){
                    ret = BitmapFactory.decodeStream(in);
                }else{
                    //only when loading images
                    final BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    ret = BitmapFactory.decodeStream(in,null,options);
                    options.inJustDecodeBounds = false;

                    in = new java.net.URL(url).openStream();
                    //prevent images too large
                    if(options.outHeight > 1024 || options.outWidth > 1024){
                        options.inSampleSize = calculateInSampleSize(options, 1024, 1024);
                        ret = BitmapFactory.decodeStream(in,null,options);
                    }else{
                        ret = BitmapFactory.decodeStream(in,null,options);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
            return ret;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            dest.setImageBitmap(bitmap);
        }
    }

    /**
     * Calculate the sample size for image resizing
     * @param options
     * @param reqWidth
     * @param reqHeight
     * @return
     */
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }
}
