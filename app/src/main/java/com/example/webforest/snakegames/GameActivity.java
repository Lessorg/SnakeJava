package com.example.webforest.snakegames;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.Random;

public class GameActivity extends Activity {

    Canvas canvas;
    SnakeView snakeView;

    //Textures
    Bitmap headBitmap;
    Bitmap headRightBitmap;
    Bitmap headUpBitmap;
    Bitmap headDownBitmap;
    Bitmap headLeftBitmap;

    Bitmap bodyBitmap;
    Bitmap bodyLeftRightBitmap;
    Bitmap bodyUpDownBitmap;

    Bitmap tailBitmap;
    Bitmap tailRightBitmap;
    Bitmap tailLeftBitmap;
    Bitmap tailUpBitmap;
    Bitmap tailDownBitmap;

    Bitmap flowerBitmap;

    Bitmap appleBitmap;

    Bitmap grassBitmap;

    //Sound
    //initialize sound variables
    private SoundPool soundPool;
    int sample1 = -1;
    int sample2 = -1;
    int sample3 = -1;
    int sample4 = -1;

    //for snake movement
    int directionOfTravel=0;
    //0 = up, 1 = right, 2 = down, 3= left

    //size of screen and score board
    int screenWidth;
    int screenHeight;
    int topGap;

    //stats
    long lastFrameTime;
    int fps;
    int score;
    int hi;

    //Game objects
    int [] snakeX;
    int [] snakeY;
    int snakeLength;
    int appleX;
    int appleY;
    int flowerX;
    int flowerY;

    //The size in pixels of a place on the game board
    int blockSize;
    int numBlocksWide;
    int numBlocksHigh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadSound();
        configureDisplay();
        snakeView = new SnakeView(this);
        setContentView(snakeView);

    }

    class SnakeView extends SurfaceView implements Runnable {
        Thread ourThread = null;
        SurfaceHolder ourHolder;
        volatile boolean playingSnake;
        Paint paint;

        public SnakeView(Context context) {
            super(context);
            ourHolder = getHolder();
            paint = new Paint();

            //get a snake this long
            snakeX = new int[200];
            snakeY = new int[200];

            //starting snake
            getSnake();
            //get apple
            getApple();
        }

        public void getSnake(){
            snakeLength = 3;
            //start snake head in the middle of screen
            snakeX[0] = numBlocksWide/2;
            snakeY[0] = numBlocksHigh /2;

            //Then the body
            snakeX[1] = snakeX[0]-1;
            snakeY[1] = snakeY[0];

            //And the tail
            snakeX[2] = snakeX[1]-1;
            snakeY[2] = snakeY[0];
        }

        public void getApple(){
            Random random = new Random();
            appleX = random.nextInt(numBlocksWide-1)+1;
            appleY = random.nextInt(numBlocksHigh-1)+1;
        }

        public void getFlower(){
            flowerX = appleX;
            flowerY = appleY;
        }

        @Override
        public void run() {
            while (playingSnake) {
                updateGame();
                drawGame();
                controlFPS();

            }

        }

        public void updateGame() {

            //Did the player get the apple
            if(snakeX[0] == appleX && snakeY[0] == appleY){
                //grow the snake
                snakeLength++;
                //replace the flower
                getFlower();
                //replace the apple
                getApple();
                //add to the score
                score = score + snakeLength;
                soundPool.play(sample1, 1, 1, 0, 0, 1);
            }

            //move the body - starting at the back
            for(int i=snakeLength; i >0 ; i--){
                snakeX[i] = snakeX[i-1];
                snakeY[i] = snakeY[i-1];
            }

            //Move the head in the appropriate direction
            switch (directionOfTravel){
                case 0://up
                    snakeY[0]  --;
                    break;

                case 1://right
                    snakeX[0] ++;
                    break;

                case 2://down
                    snakeY[0] ++;
                    break;

                case 3://left
                    snakeX[0] --;
                    break;
            }

            //accident
            boolean dead = false;
            //with a wall
            if(snakeX[0] == -1)dead=true;
            if(snakeX[0] >= numBlocksWide)dead=true;
            if(snakeY[0] == -1)dead=true;
            if(snakeY[0] == numBlocksHigh)dead=true;
            //eaten ourselves
            for (int i = snakeLength-1; i > 0; i--) {
                if (i > 4 && snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                    dead = true;
                    break;
                }
            }


            if(dead){
                //start again
                soundPool.play(sample4, 1, 1, 0, 0, 1);

                if (score > hi) {
                    hi = score;
                }

                directionOfTravel = 1;
                score = 0;
                getSnake();

            }

        }

        public void drawGame() {

            if (ourHolder.getSurface().isValid()) {
                canvas = ourHolder.lockCanvas();
                //Paint paint = new Paint();
                canvas.drawColor(Color.BLACK);//the background
                paint.setColor(Color.argb(255, 255, 255, 255));
                paint.setTextSize(topGap/2);
                canvas.drawText("Score:" + score + "  Top score:" + hi, 10, topGap-6, paint);

                //draw a border - 4 lines, top right, bottom , left
                canvas.drawBitmap(grassBitmap, 0, topGap + 4, paint);

                //draw a border - 4 lines, top right, bottom , left
                paint.setStrokeWidth(3);//border width
                canvas.drawLine(1,topGap,screenWidth-1,topGap,paint);
                canvas.drawLine(screenWidth-1,topGap,screenWidth-1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(screenWidth-1,topGap+(numBlocksHigh*blockSize),1,topGap+(numBlocksHigh*blockSize),paint);
                canvas.drawLine(1,topGap, 1,topGap+(numBlocksHigh*blockSize), paint);

                //Draw the snake
                if (directionOfTravel == 0) {
                    headBitmap = headUpBitmap;
                    bodyBitmap = bodyUpDownBitmap;
                }
                else if (directionOfTravel == 1){
                    headBitmap = headRightBitmap;
                }
                else if (directionOfTravel == 2){
                    headBitmap = headDownBitmap;
                }
                else {
                    headBitmap = headLeftBitmap;
                }

                //draw the snake head
                canvas.drawBitmap(headBitmap, snakeX[0] * blockSize, (snakeY[0] * blockSize) + topGap, paint);

                for(int i = 1; i < snakeLength-1;i++){
                    if (snakeY[i] == snakeY[i-1]) {
                        canvas.drawBitmap(bodyLeftRightBitmap, snakeX[i] * blockSize, (snakeY[i] * blockSize) + topGap, paint);
                        if(snakeX[i] > snakeX[i-1]) {
                            tailBitmap = tailLeftBitmap;
                        }
                        else{
                            tailBitmap = tailRightBitmap;
                        }
                    }
                    else{
                        canvas.drawBitmap(bodyUpDownBitmap, snakeX[i] * blockSize, (snakeY[i] * blockSize) + topGap, paint);
                        if(snakeY[i] > snakeY[i-1]) {
                            tailBitmap = tailUpBitmap;
                        }
                        else{
                            tailBitmap = tailDownBitmap;
                        }                    }
                }

                //draw the tail
                canvas.drawBitmap(tailBitmap, snakeX[snakeLength-1]*blockSize, (snakeY[snakeLength-1]*blockSize)+topGap, paint);
                //draw the apple
                canvas.drawBitmap(appleBitmap, appleX*blockSize, (appleY*blockSize)+topGap, paint);
                //draw the flower
                canvas.drawBitmap(flowerBitmap, flowerX*blockSize, (flowerY*blockSize)+topGap, paint);

                ourHolder.unlockCanvasAndPost(canvas);
            }

        }

        public void controlFPS() {
            long timeThisFrame = (System.currentTimeMillis() - lastFrameTime);
            long timeToSleep = 200 - timeThisFrame;
            if (timeThisFrame > 0) {
                fps = (int) (1000 / timeThisFrame);
            }
            if (timeToSleep > 0) {

                try {
                    ourThread.sleep(timeToSleep);
                } catch (InterruptedException e) {
                    //Print an error message to the console
                    Log.e("error", "failed to load sound files");
                }

            }

            lastFrameTime = System.currentTimeMillis();
        }


        public void pause() {
            playingSnake = false;
            try {
                ourThread.join();
            } catch (InterruptedException e) {
            }

        }

        public void resume() {
            playingSnake = true;
            ourThread = new Thread(this);
            ourThread.start();
        }


        @Override
        public boolean onTouchEvent(MotionEvent motionEvent) {

            switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_UP:
                    if (motionEvent.getX() >= screenWidth / 2) {
                        //turn right
                        directionOfTravel ++;
                        if(directionOfTravel == 4) {//no such direction
                            //loop back to 0(up)
                            directionOfTravel = 0;
                        }
                    } else {
                        //turn left
                        directionOfTravel--;
                        if(directionOfTravel == -1) {//no such direction
                            //loop back to 0(up)
                            directionOfTravel = 3;
                        }
                    }
            }
            return true;
        }


    }

    @Override
    protected void onStop() {
        super.onStop();

        while (true) {
            snakeView.pause();
            break;
        }

        finish();
    }


    @Override
    protected void onResume() {
        super.onResume();
        snakeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        snakeView.pause();
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {

            snakeView.pause();

            Intent i = new Intent(this, MainActivity.class);
            startActivity(i);
            finish();
            return true;
        }
        return false;
    }

    public void loadSound(){
        soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 0);
        try {
            //Create objects of the 2 required classes
            AssetManager assetManager = getAssets();
            AssetFileDescriptor descriptor;

            //create our three fx in memory ready for use
            descriptor = assetManager.openFd("sample1.ogg");
            sample1 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample2.ogg");
            sample2 = soundPool.load(descriptor, 0);


            descriptor = assetManager.openFd("sample3.ogg");
            sample3 = soundPool.load(descriptor, 0);

            descriptor = assetManager.openFd("sample4.ogg");
            sample4 = soundPool.load(descriptor, 0);


        } catch (IOException e) {
            //Print an error message to the console
            Log.e("error", "failed to load sound files");

        }
    }

    public void configureDisplay(){
        //find out the width and height of the screen
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;
        topGap = screenHeight/14;

        //Determine the size of each block/place on the game board
        blockSize = screenWidth/15;

        //Determine how many game blocks will fit into the height and width
        //Leave one block for the score at the top
        numBlocksWide = 15;
        numBlocksHigh = ((screenHeight - topGap ))/blockSize;

        //Load and scale bitmaps
        grassBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.grass);

        headRightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headright);
        headLeftBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headleft);
        headDownBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headdown);
        headUpBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.headup);

        bodyLeftRightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.body);
        bodyUpDownBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.bodyupdown);

        tailRightBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tailrigth);
        tailLeftBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tailleft);
        tailUpBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tailup);
        tailDownBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.taildown);

        appleBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.apple);

        flowerBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.flower_sprite_sheet);

        grassBitmap = Bitmap.createScaledBitmap(grassBitmap, screenWidth, screenHeight - topGap, false);

        //scale the bitmaps to match the block size
        headLeftBitmap = Bitmap.createScaledBitmap(headLeftBitmap, blockSize, blockSize, false);
        headDownBitmap = Bitmap.createScaledBitmap(headDownBitmap, blockSize, blockSize, false);
        headUpBitmap = Bitmap.createScaledBitmap(headUpBitmap, blockSize, blockSize, false);
        headRightBitmap = Bitmap.createScaledBitmap(headRightBitmap, blockSize, blockSize, false);

        bodyLeftRightBitmap = Bitmap.createScaledBitmap(bodyLeftRightBitmap, blockSize, blockSize, false);
        bodyUpDownBitmap = Bitmap.createScaledBitmap(bodyUpDownBitmap, blockSize, blockSize, false);

        tailRightBitmap = Bitmap.createScaledBitmap(tailRightBitmap, blockSize, blockSize, false);
        tailLeftBitmap = Bitmap.createScaledBitmap(tailLeftBitmap, blockSize, blockSize, false);
        tailUpBitmap = Bitmap.createScaledBitmap(tailUpBitmap, blockSize, blockSize, false);
        tailDownBitmap = Bitmap.createScaledBitmap(tailDownBitmap, blockSize, blockSize, false);

        appleBitmap = Bitmap.createScaledBitmap(appleBitmap, blockSize, blockSize, false);

        flowerBitmap = Bitmap.createScaledBitmap(flowerBitmap, 4 * blockSize, 4 * blockSize, false);
    }


}
