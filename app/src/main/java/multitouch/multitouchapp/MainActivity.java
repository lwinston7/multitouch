package multitouch.multitouchapp;

import android.app.Dialog;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private CanvasView customCanvas;
    private ImageButton currPaint, drawBtn, eraseBtn;
    private float smallBrush, mediumBrush, largeBrush;
    private RadioButton btnLine, btnRect, btnCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        customCanvas = (CanvasView) findViewById(R.id.mainCanvas);
        LinearLayout paintLayout = (LinearLayout) findViewById(R.id.paintColor);

        currPaint = (ImageButton) paintLayout.getChildAt(0);
        currPaint.setImageDrawable(ContextCompat.getDrawable(getApplicationContext()
                ,R.drawable.paint_pressed));

        smallBrush = getResources().getInteger(R.integer.small_size);
        mediumBrush = getResources().getInteger(R.integer.medium_size);
        largeBrush = getResources().getInteger(R.integer.large_size);

        drawBtn = (ImageButton)findViewById(R.id.buttonDraw);
        drawBtn.setOnClickListener(this);

        customCanvas.setBrushSize(mediumBrush);

        eraseBtn = (ImageButton)findViewById(R.id.buttonErase);
        eraseBtn.setOnClickListener(this);

        btnLine = (RadioButton)findViewById(R.id.btnLine);
        btnLine.setChecked(true);
        btnLine.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCanvas.setLine(true);
            }
        });

        btnRect = (RadioButton)findViewById(R.id.btnRect);
        btnRect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCanvas.setRect(true);
            }
        });

        btnCircle = (RadioButton)findViewById(R.id.btnCircle);
        btnCircle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customCanvas.setCircle(true);
            }
        });

    }

    public void clearCanvas(View v) {
        customCanvas.clearCanvas();
    }

    public void paintClicked(View view) {
        customCanvas.setErase(false);
        customCanvas.setBrushSize(customCanvas.getLastBrushSize());
        if (view != currPaint) {
            ImageButton imgView = (ImageButton)view;
            String color = view.getTag().toString();
            customCanvas.setColor(color);
            imgView.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.paint_pressed));
            currPaint.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.paint));
            currPaint=(ImageButton)view;

        }
    }


    public void New(View view) {


    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.buttonDraw) {
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Brush size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    customCanvas.setErase(false);
                    customCanvas.setBrushSize(smallBrush);
                    customCanvas.setLastBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    customCanvas.setErase(false);
                    customCanvas.setBrushSize(mediumBrush);
                    customCanvas.setLastBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });

            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    customCanvas.setErase(false);
                    customCanvas.setBrushSize(largeBrush);
                    customCanvas.setLastBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            brushDialog.show();
        } else if(view.getId()==R.id.buttonErase){
            //switch to erase - choose size
            final Dialog brushDialog = new Dialog(this);
            brushDialog.setTitle("Eraser size:");
            brushDialog.setContentView(R.layout.brush_chooser);
            ImageButton smallBtn = (ImageButton)brushDialog.findViewById(R.id.small_brush);
            smallBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    customCanvas.setErase(true);
                    customCanvas.setBrushSize(smallBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton mediumBtn = (ImageButton)brushDialog.findViewById(R.id.medium_brush);
            mediumBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    customCanvas.setErase(true);
                    customCanvas.setBrushSize(mediumBrush);
                    brushDialog.dismiss();
                }
            });
            ImageButton largeBtn = (ImageButton)brushDialog.findViewById(R.id.large_brush);
            largeBtn.setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v) {
                    customCanvas.setErase(true);
                    customCanvas.setBrushSize(largeBrush);
                    brushDialog.dismiss();
                }
            });
            brushDialog.show();
        }
    }

    public void Open(View view) {
        // Open a new file
    }

    public void Save(View view) {
        // Save file
    }
}
