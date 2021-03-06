package edu.illinois.cs125.sagittario.sagittario;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class MinesweeperActivity extends AppCompatActivity implements Runnable {
    private SagittarioApplication app;
    // views
    protected ImageProvider provider;
    protected CanvasView view;
    private ProgressBar loading;
    // is the activity ready to play?
    protected boolean loaded = false;
    // instance info
    protected GameState state;
    protected MineSweeper sweeper;
    protected BitmapInfo info = new BitmapInfo();

    public static class BitmapInfo implements Serializable{
        public byte[] pixels;
        public Bitmap.Config cfg;
        public int width;
        public int height;
        public String searchStr;

    }
    // transient variables
    private Handler mHandler;
    protected Bitmap background;
    protected Drawable tile, uncovered;
    protected Bitmap flag;
    protected Bitmap bomb;


    public enum GameState{
        PLAYING, WON, LOST;
    }

    private void setupDrawCallback(){
        loaded = true;
        Runnable next = new Runnable() {
            @Override
            public void run() {
                try{
                    view.invalidate();
                    Log.v("DrawRunner", "Invalidated View!");
                } catch (Throwable t) {
                    Log.e("DrawRunner", "Exception: ", t);
                } finally {
                    if (state == GameState.PLAYING)
                        mHandler.postDelayed(this, 30);
                }
            }
        };
        mHandler.postDelayed(next, 10);
    }

    @Override
    public void run() {
        loading.setVisibility(View.INVISIBLE);
        Log.e("MineSweeper", "Re-entered Activity!");
        // create bitmap info
        background = provider.getBackground();
        if (background == null){
            Log.d("MineSweeper", "Bing did not load");
            info = new BitmapInfo();
            info.width = 1;
            info.height = 1;
            info.cfg = Bitmap.Config.ARGB_8888;
            info.pixels = new byte[4];
            createBackgroundFromInfo(info);
            setupDrawCallback();
            return;
        }
        int size = background.getRowBytes() * background.getHeight();
        ByteBuffer imageBuffer = ByteBuffer.allocate(size);
        background.copyPixelsToBuffer(imageBuffer);
        imageBuffer.rewind();
        info.pixels = imageBuffer.array();
        info.cfg = background.getConfig();
        info.width = background.getWidth();
        info.height = background.getHeight();
        info.searchStr = provider.searchString;
        // setup draw callback
        setupDrawCallback();
    }

    private void loadFromBundle(Bundle savedInstanceState){
        sweeper = (MineSweeper) savedInstanceState.getSerializable("sweeper");
        info = (BitmapInfo) savedInstanceState.getSerializable("bufferInfo");
        try {
            File outFile = new File(getCacheDir(), "image.dat");
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(outFile));
            info = (BitmapInfo)in.readObject();
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            // start the image search and load the background
            app = (SagittarioApplication) this.getApplication();
            provider = app.createImageProvider(info.searchStr, this);
            loading.setVisibility(View.VISIBLE);
            return;
        }
        createBackgroundFromInfo(info);
    }

    public void createBackgroundFromInfo(BitmapInfo info){
        background = Bitmap.createBitmap(info.width, info.height, info.cfg);
        ByteBuffer buff = ByteBuffer.wrap(info.pixels);
        background.copyPixelsFromBuffer(buff);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = GameState.PLAYING;
        setContentView(R.layout.activity_minesweeper);
        // find views
        loading = (ProgressBar) findViewById(R.id.loadingBar);
        view = (CanvasView) findViewById(R.id.canvasView);
        view.activity = this;
        // get the bitmaps
        loadBitmaps();
        tile = this.getDrawable(R.drawable.ic_tile);
        uncovered = this.getDrawable(R.drawable.ic_uncovered);
        // setup Handler
        mHandler = new Handler();
        // create or load instance state!
        if(savedInstanceState != null){
            this.loadFromBundle(savedInstanceState);
            this.setupDrawCallback();
            return;
        } else {
            // init settings and minesweeper
            int fieldSize = getIntent().getIntExtra("fieldSize", 8);
            int nBombs = getIntent().getIntExtra("nBombs", 10);
            String searchText = getIntent().getStringExtra("searchText");
            if (searchText == null) {
                searchText = "green";
            }
            sweeper = new MineSweeper(fieldSize, nBombs);
            // start the image search and load the background
            app = (SagittarioApplication) this.getApplication();
            provider = app.createImageProvider(searchText, this);
            loading.setVisibility(View.VISIBLE);
        }
        return;
    }

    /**
     * Load flag and bomb from resources.
     */
    public void loadBitmaps(){
        flag = BitmapFactory.decodeResource(this.getResources(), R.drawable.flag);
        bomb = BitmapFactory.decodeResource(this.getResources(), R.drawable.bomb);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        try {
            File outFile = new File(getCacheDir(), "image.dat");
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outFile));
            out.writeObject(info);
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        info.pixels = null;
        outState.putSerializable("bufferInfo", info);
        outState.putSerializable("sweeper", sweeper);
    }
}
