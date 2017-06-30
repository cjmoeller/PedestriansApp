package org.alternadev.pedestrians;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.stetho.Stetho;

import org.alternadev.pedestrians.api.PedestrianAPIController;
import org.alternadev.pedestrians.db.Pedestrian;
import org.alternadev.pedestrians.db.PedestrianImage;

import java.util.ArrayList;
import java.util.Iterator;

import static com.orm.SugarRecord.findAll;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private FloatingActionButton delete;
    private FloatingActionButton assign;

    private ArrayList<PedestrianImage> images;
    private PedestrianImage current;
    private PedestrianAPIController controller;
    public static String USER = "NONE";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Pedestrian.deleteAll(Pedestrian.class);
        //PedestrianImage.deleteAll(PedestrianImage.class);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Stetho.initializeWithDefaults(this);

        images = new ArrayList<PedestrianImage>();

        controller = new PedestrianAPIController(this);


        imageView = (ImageView) findViewById(R.id.imageView2);
        delete = (FloatingActionButton) findViewById(R.id.delete);
        assign = (FloatingActionButton) findViewById(R.id.assign);

        this.loadImages();
        this.nextPicture();

        ArrayList<PedestrianImage> list = new ArrayList<PedestrianImage>();
        Iterator<PedestrianImage> it = PedestrianImage.findAll(PedestrianImage.class);
        while(it.hasNext()){
            PedestrianImage ne = it.next();
            list.add(ne);
        }
        final SharedPreferences sharedPref = this.getPreferences(Context.MODE_PRIVATE);
        String user = sharedPref.getString("USER", null);
        if (user == null) {
            CharSequence names[] = {"Jean", "Julius"};
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Jean oder Christian?");
            builder.setItems(names, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    SharedPreferences.Editor editor = sharedPref.edit();
                    if (which == 0) {
                        editor.putString("USER", "A");
                        MainActivity.USER = "A";

                    } else {
                        editor.putString("USER", "B");
                        MainActivity.USER = "B";
                    }
                    editor.commit();
                }
            });
            builder.show();
        } else {
            MainActivity.USER = user;
        }




        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.this.current.setNoPedestrian(true);
                MainActivity.this.current.setAlreadyAnalyzed(true);
                MainActivity.this.current.save();
                MainActivity.this.nextPicture();
                Snackbar.make(view, "Kein Passant erkannt.", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                MainActivity.this.nextPicture();
            }
        });

        assign.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final PedestrianImage workingOn = MainActivity.this.current;
                Iterator<Pedestrian> it = findAll(Pedestrian.class);

                final ArrayList<Pedestrian> all = new ArrayList<Pedestrian>();

                while (it.hasNext()) {
                    all.add(it.next());
                }

                CharSequence names[] = new CharSequence[all.size() + 1];

                for (int i = 0; i < all.size(); i++) {
                    names[i] = all.get(i).getName();
                }
                names[all.size()] = "Neue Person...";
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Wer ist das?");
                builder.setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == all.size()) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setTitle("Neue Person hinzufügen:");

                            // Set up the input
                            final EditText input = new EditText(MainActivity.this);
                            // Specify the type of input expected
                            input.setInputType(InputType.TYPE_CLASS_TEXT);
                            builder.setView(input);

                            // Set up the buttons
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Pedestrian newPedestrian = new Pedestrian();
                                    newPedestrian.setName(input.getText().toString());
                                    newPedestrian.save();

                                    workingOn.setPedestrian(newPedestrian);
                                    workingOn.setAlreadyAnalyzed(true);
                                    workingOn.save();
                                    MainActivity.this.nextPicture();
                                }
                            });
                            builder.setNegativeButton("Abbrechen", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                            builder.show();
                        } else {
                            Pedestrian selected = all.get(which);
                            workingOn.setPedestrian(selected);
                            workingOn.setAlreadyAnalyzed(true);
                            workingOn.save();
                            MainActivity.this.nextPicture();
                        }

                    }
                });
                builder.show();

            }

        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public void nextPicture() {
        MainActivity.this.images.remove(current);
        TextView noImages = (TextView) findViewById(R.id.textView);
        if (this.images.size() == 0) {
            this.imageView.setVisibility(View.INVISIBLE);
            noImages.setVisibility(View.VISIBLE);
            assign.setVisibility(View.INVISIBLE);
            delete.setVisibility(View.INVISIBLE);
        } else {
            noImages.setVisibility(View.INVISIBLE);
            this.imageView.setVisibility(View.VISIBLE);
            assign.setVisibility(View.VISIBLE);
            delete.setVisibility(View.VISIBLE);


            this.current = this.images.get(0);
            imageView.setImageDrawable( getDrawableFromPath(current.getPath()));


        }
    }

    private Drawable getDrawableFromPath(String path) {
        Bitmap bmImg = BitmapFactory.decodeFile(path);
        Drawable result = new BitmapDrawable(getApplicationContext().getResources(), bmImg);
        return result;
    }

    public void loadImages() {
        this.images = new ArrayList<PedestrianImage>();
        Iterator<PedestrianImage> it = findAll(PedestrianImage.class);
        while (it.hasNext()) {
            PedestrianImage next = it.next();
            if (!next.isAlreadyAnalyzed())
                this.images.add(next);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();


        //noinspection SimplifiableIfStatement
        if (id == R.id.fetch_data) {
            controller.fetchImages();
            controller.getNames();
            return true;
        } else if (id == R.id.send_data) {
            controller.sendResults();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
