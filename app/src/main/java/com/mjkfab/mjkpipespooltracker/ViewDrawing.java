package com.mjkfab.mjkpipespooltracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ViewDrawing extends AppCompatActivity {
    private byte[] data;
    private Long pipeID;
    private Bitmap bmp;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent pastIntent = getIntent();
        pipeID = pastIntent.getLongExtra(PipePage.PIPE_ID,-1);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_drawing);
        setTitle(pipeID.toString());
        new ViewDrawing.ImageRequest().execute();
    }
    private class ImageRequest extends AsyncTask<Void,Void,String> {
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private Socket socket;
        private String serverResponse;
        private String serverAddress;

        @Override
        protected String doInBackground(Void... params) {
            int serverPort = 60101;
            SharedPreferences pref = getSharedPreferences("CREDENTIALS", Context.MODE_PRIVATE);
            serverAddress = pref.getString("SERVER_IP","NOT_SET");
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddress, serverPort), 1000);
                //Send Server Command
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject("sendImage");
                outputStream.writeLong(pipeID);
                outputStream.flush();
                //read image
                inputStream = new ObjectInputStream(socket.getInputStream());
                String imageExsists = (String) inputStream.readObject();
                if(imageExsists.equals("false")){
                    serverResponse = "Image Not Found";
                    throw new IOException();
                }
                data = (byte[]) inputStream.readObject();
                serverResponse = "Image Received";



            } catch (SocketTimeoutException socketTimeout) {
                serverResponse = "time_out";
            } catch (IOException | ClassNotFoundException exception) {
                exception.printStackTrace();
            } finally {
                try {
                    if (socket != null)
                        socket.close();
                    if (inputStream != null)
                        inputStream.close();
                    if (outputStream != null)
                        outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return serverResponse;
        }

        @Override
        protected void onPostExecute(String result) {
            if(result.equals("Image Received")) {
                bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                ZoomableImageView touch = (ZoomableImageView) findViewById(R.id.imageView);
                touch.setImageBitmap(bmp);
                findViewById(R.id.loadingPanel).setVisibility(View.GONE);
            }else if(result.equals("Image Not Found")){
                Toast.makeText(ViewDrawing.this, "Drawing Not Found On Server", Toast.LENGTH_LONG).show();
            }else{
                goStart();
            }
            super.onPostExecute(result);
        }
    }
    //Action Bar - Back
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_default,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case android.R.id.home:
                if(bmp != null) {
                    bmp.recycle();
                }
                Intent backIntent = new Intent(this,PipePage.class);
                backIntent.putExtra(PipePage.PIPE_ID, pipeID);
                startActivity(backIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    private void goStart(){
        Toast.makeText(ViewDrawing.this, "Failed to connect to Server.\n Reconnecting...", Toast.LENGTH_LONG).show();
        startActivity(new Intent(this, StartActivity.class));
    }
}
