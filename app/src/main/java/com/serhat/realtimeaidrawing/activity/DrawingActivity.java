package com.serhat.realtimeaidrawing.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;


import com.jaredrummler.android.colorpicker.ColorPickerDialog;
import com.jaredrummler.android.colorpicker.ColorPickerDialogListener;
import com.serhat.realtimeaidrawing.GalleryActivity;
import com.serhat.realtimeaidrawing.BeforeAfterSlider;
import com.serhat.realtimeaidrawing.R;
import com.serhat.realtimeaidrawing.Utils;
import com.serhat.realtimeaidrawing.ZoomageView;
import com.serhat.realtimeaidrawing.app.App;
import com.serhat.realtimeaidrawing.app.ApplicationPath;
import com.serhat.realtimeaidrawing.db.DatabaseHelper;
import com.serhat.realtimeaidrawing.drawing.DrawView;
import com.serhat.realtimeaidrawing.listener.DrawingListener;
import com.serhat.realtimeaidrawing.model.GalleryModel;
import com.serhat.realtimeaidrawing.preference.Prefs;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class DrawingActivity extends AppCompatActivity implements View.OnClickListener, ColorPickerDialogListener {

    private ZoomageView imageView;
    private TextInputEditText textInputEditText;

    private TextInputLayout textInputLayout;

    private MaterialToolbar materialToolbar;

    private MaterialCardView save_it_MC;

    private static final int DIALOG_ID = 0;
    private ImageButton generate_IB, settings_IB, undo_IB, redo_IB, pencil_IB, eraser_IB, colour_IB,
            fill_IB, clear_IB, hand_IB, help_IB;

    private static final String enc = "6kNPSta21BCXUA95";
    private static final String vect = "1826C9GDB1912D23";


    private DrawView drawView;

    private RangeSlider rangeSlider;
    private static final String TAG = DrawingActivity.class.getSimpleName();

    private boolean isBackground;

    private DatabaseHelper databaseHelper;

    private static final String AES_CBC_PKCS5_PADDING = "AES/CBC/PKCS5Padding";
    private static final String AES = "AES";


    private View pen_prew_color, fill_prew_color;

    private GalleryModel galleryModel;


    private Prefs prefs;
    private static final String FORMAT = "%02d:%02d:%02d";


    private Bitmap currOutBitmap;

    private String API_KEY = "";

    private float prevX, prevY;


    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.settings_IB) {
            openSettings();
        } else if (id == R.id.redo_IB) {
            drawView.redo();
        } else if (id == R.id.undo_IB) {
            drawView.undo();
        } else if (id == R.id.clear_IB) {
            MaterialAlertDialogBuilder newDialog = new MaterialAlertDialogBuilder(this);
            newDialog.setTitle("Clear Drawing");
            newDialog.setMessage("Clear your drawing? You wont be able to bring it back.");
            newDialog.setPositiveButton("Yes", (dialog, which) -> {
                Toast.makeText(this, "Cleared the drawing!", Toast.LENGTH_SHORT).show();
                drawView.clearCanvas();
                dialog.dismiss();
            });
            newDialog.setNegativeButton("No", (dialog, which) -> dialog.cancel());
            newDialog.show();
        } else if (id == R.id.eraser_IB) {
            drawView.setEraser(true);
            pencil_IB.clearColorFilter();
            eraser_IB.setColorFilter(ContextCompat.getColor(this, R.color.blue),
                    PorterDuff.Mode.SRC_IN);
        } else if (id == R.id.fill_IB) {
            isBackground = true;
            ColorPickerDialog.newBuilder()
                    .setDialogTitle(R.string.sel_back_color)
                    .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                    .setAllowPresets(true)
                    .setDialogId(DIALOG_ID)
                    .setColor(drawView.backgroundColour)
                    .setShowColorShades(true)
                    .setShowStroke(false)
                    .show(this);

        } else if (id == R.id.colour_IB) {
            isBackground = false;
            ColorPickerDialog.newBuilder()
                    .setDialogTitle(R.string.sel_pen_color)
                    .setDialogType(ColorPickerDialog.TYPE_PRESETS)
                    .setAllowPresets(true)
                    .setDialogId(DIALOG_ID)
                    .setColor(drawView.currentColour)
                    .setShowColorShades(true)
                    .setShowStroke(true)
                    .setStrokeWidth(drawView.strokeWidth)
                    .show(this);
        } else if (id == R.id.pencil_IB) {
            drawView.setEraser(false);
            eraser_IB.clearColorFilter();
            pencil_IB.setColorFilter(ContextCompat.getColor(this, R.color.blue),
                    PorterDuff.Mode.SRC_IN);
        } else if (id == R.id.hand_IB) {
            drawView.isZoom = !drawView.isZoom;
        } else if (id == R.id.help_IB) {
            showTutorial();
        }
    }


    private void saveImage() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View contentView = View.inflate(this, R.layout.save_image, null);
        bottomSheetDialog.setContentView(contentView);
        MaterialCardView down_orig_MC = contentView.findViewById(R.id.down_orig_MC);
        MaterialCardView upscale_MC = contentView.findViewById(R.id.upscale_MC);

        down_orig_MC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                saveCurrImg();
            }
        });
        upscale_MC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog.dismiss();
                openUpscaleSettings();

            }
        });


        bottomSheetDialog.show();

    }

    private void checkAndSave() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            saveImage();
            return;
        }
        if ((ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        ) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        } else {
            saveImage();
        }

    }


    private void saveCurrImg() {
        String filename = System.currentTimeMillis() + ".jpg";
        File imageFile = new File(ApplicationPath.savePath(), filename);
        try {
            FileOutputStream outStream = new FileOutputStream(imageFile);
            currOutBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            GalleryModel gModel = new GalleryModel();
            gModel.prompt = galleryModel.prompt;
            gModel.negative_prompt = "";
            gModel.width = galleryModel.width;
            gModel.height = galleryModel.height;
            gModel.path = imageFile.getAbsolutePath();
            Utils.i(TAG, "DB Image Path:" + gModel.path);
            if (databaseHelper.getSaved(gModel.path) == null) {
                databaseHelper.addSaved(gModel);
                Toast.makeText(DrawingActivity.this, "Image Saved", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Utils.i(TAG, e.getMessage());
        }
    }


    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean result) {
                    if (!result) {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(DrawingActivity.this,
                                "android.permission.WRITE_EXTERNAL_STORAGE")) {
                            new MaterialAlertDialogBuilder(DrawingActivity.this)
                                    .setTitle(getString(R.string.app_name) + " needs permission")
                                    .setMessage("This app requires WRITE_EXTERNAL_STORAGE permission to save the image to permanent storage")
                                    .setPositiveButton("Give Permission", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            checkAndSave();
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    }).show();

                        }
                    } else {
                        saveImage();
                    }
                }
            }
    );

    private void openUpscaleSettings() {

        if (TextUtils.isEmpty(API_KEY)) {
            Toast.makeText(this, "No api key found", Toast.LENGTH_LONG).show();
            showAPIkeyDialog();
            return;
        }

        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog b = (BottomSheetDialog) dialog;
                FrameLayout f = (FrameLayout) b.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (f != null) {
                    BottomSheetBehavior.from(f).setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });
        View contentView = View.inflate(this, R.layout.upscale, null);
        bottomSheetDialog.setContentView(contentView);
        final ImageView[] prew_IV = {contentView.findViewById(R.id.prew_IV)};
        Spinner model_spinner = contentView.findViewById(R.id.model_spinner);
        Spinner scale_spinner = contentView.findViewById(R.id.scale_spinner);
        SwitchMaterial face_upscale_SV = contentView.findViewById(R.id.face_upscale_SV);
        MaterialCardView upscale_image_MC = contentView.findViewById(R.id.upscale_image_MC);
        ImageButton upscale_model_info_IB = contentView.findViewById(R.id.upscale_model_info_IB);
        ImageButton rescale_info_IB = contentView.findViewById(R.id.rescale_info_IB);
        ImageButton face_info_IB = contentView.findViewById(R.id.face_info_IB);

        upscale_model_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "Select a model that fits your style and scale needs.");
            }
        });
        rescale_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "This settings determines image resolution." +
                        "\nFor example lets say you have 512x512 image and you selected rescale factor of 2" +
                        " Your upscaled image's resolution will be 1024x1024.\nIf you pick 1 then your resolution will stay same.");
            }
        });

        face_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "If your image contains a blurry face enable this setting.");
            }
        });


        prew_IV[0].setImageBitmap(currOutBitmap);

        final String[] modelName = {"RealESRGAN_x4plus"};
        final int[] rescale_factor = {2};
        final boolean[] face = {false};

        List<String> model_arr = Arrays.asList(getResources().getStringArray(R.array.upscaling_model_array));
        ArrayAdapter<String> models = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, model_arr);
        model_spinner.setAdapter(models);
        model_spinner.setSelection(0);
        model_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                modelName[0] = models.getItem(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        List<String> upscale_arr = Arrays.asList(getResources().getStringArray(R.array.rescale_array));
        ArrayAdapter<String> scale = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, upscale_arr);
        scale_spinner.setAdapter(scale);
        scale_spinner.setSelection(1);
        scale_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                rescale_factor[0] = Integer.parseInt(scale.getItem(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        face_upscale_SV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                face[0] = isChecked;
            }
        });

        upscale_image_MC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                upscaleWithAPI(modelName[0], rescale_factor[0], face[0]);
                bottomSheetDialog.dismiss();
            }
        });


        bottomSheetDialog.show();


    }

    @SuppressLint("ClickableViewAccessibility")
    private void setUpFloatingSave() {
        save_it_MC = findViewById(R.id.save_it_MC);
        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(@NonNull MotionEvent e) {
                if (currOutBitmap != null) {
                    checkAndSave();
                } else {
                    Toast.makeText(DrawingActivity.this, "No Image Found!", Toast.LENGTH_SHORT).show();
                }

                return true;
            }
        });
        save_it_MC.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                gestureDetector.onTouchEvent(event);

                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        prevX = event.getRawX();
                        prevY = event.getRawY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        float moveX = event.getRawX() - prevX;
                        save_it_MC.setX(save_it_MC.getX() + moveX);
                        prevX = event.getRawX();
                        float moveY = event.getRawY() - prevY;
                        save_it_MC.setY(save_it_MC.getY() + moveY);
                        prevY = event.getRawY();
                        float width = imageView.getMeasuredWidth();
                        float height = imageView.getMeasuredHeight();
                        if ((save_it_MC.getX() + save_it_MC.getWidth()) >= width
                                || save_it_MC.getX() <= 0) {
                            save_it_MC.setX(save_it_MC.getX() - moveX);
                        }
                        if ((save_it_MC.getY() + save_it_MC.getHeight()) >= height
                                || save_it_MC.getY() <= 0) {
                            save_it_MC.setY(save_it_MC.getY() - moveY);
                        }
                        break;
                }

                return true;
            }
        });
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.drawing_activity);
        prefs = Prefs.getInstance(getApplicationContext());

        String key = prefs.getString("api_key", "");
        if (!TextUtils.isEmpty(key)) {
            API_KEY = decrypt(key);
        } else {
            showAPIkeyDialog();
        }


        databaseHelper = DatabaseHelper.getInstance(getApplicationContext());


        if (!prefs.getBoolean("is_tutorial_showed", false)) {
            showTutorial();
            prefs.setBoolean("is_tutorial_showed", true);
        }


        materialToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(materialToolbar);

        imageView = findViewById(R.id.imageView);
        textInputLayout = findViewById(R.id.textInputLayout);
        textInputEditText = findViewById(R.id.textInputEditText);
        settings_IB = findViewById(R.id.settings_IB);
        help_IB = findViewById(R.id.help_IB);


        settings_IB.setOnClickListener(this);
        help_IB.setOnClickListener(this);

        setUpFloatingSave();

//        account_IB.setOnClickListener(this);

        setUpPaintViews();


//        queryUserByUUID(fAuth.getCurrentUser().getUid());

        rangeSlider.setValues(80f);
        rangeSlider.setValueFrom(1f);
        rangeSlider.setValueTo(100f);

        rangeSlider.addOnChangeListener((slider, value, fromUser) ->
                drawView.setStrokeWidth((int) value));
        pencil_IB.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (rangeSlider.getVisibility() == View.VISIBLE) {
                    rangeSlider.setVisibility(View.GONE);
                } else {
                    rangeSlider.setVisibility(View.VISIBLE);
                }
                return false;
            }
        });


        textInputEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (TextUtils.isEmpty(textInputEditText.getText().toString())) {
                    textInputLayout.setError("Prompt is empty. Please write a prompt");
                } else {
                    textInputLayout.setErrorEnabled(false);
                    String base64 = drawView.captureCanvasAsBase64();
                    if (base64 != null) {
                        genWithCheck(textInputEditText.getText().toString(), base64);
                        Utils.hideKeyboard(DrawingActivity.this);
                        textInputEditText.clearFocus();
                    }
                }
                return false;
            }
        });


        drawView.setDrawingListener(new DrawingListener() {
            @Override
            public void onDrawingFinished(String base64Image) {
                genWithCheck(textInputEditText.getText().toString(), base64Image);
            }
        });


        ViewTreeObserver vto = drawView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                drawView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                setCanvasRatio(prefs.getInt("ratio_value", 0));
            }
        });
    }


    private void setCanvasRatio(int ratio) {
        switch (ratio) {
            case 0:
                drawView.setUpCanvas(1024, 576);
                break;
            case 1:
                drawView.setUpCanvas(640, 480);
                break;
            case 2:
                drawView.setUpCanvas(512, 512);
                break;
            case 3:
                drawView.setUpCanvas(576, 1024);
                break;
            case 4:
                drawView.setUpCanvas(480, 640);
                break;
        }
    }


    private void setUpPaintViews() {
        drawView = findViewById(R.id.draw_view);
        pen_prew_color = findViewById(R.id.pen_prew_color);
        fill_prew_color = findViewById(R.id.fill_prew_color);
        undo_IB = findViewById(R.id.undo_IB);
        redo_IB = findViewById(R.id.redo_IB);
        pencil_IB = findViewById(R.id.pencil_IB);
        eraser_IB = findViewById(R.id.eraser_IB);
        colour_IB = findViewById(R.id.colour_IB);
        fill_IB = findViewById(R.id.fill_IB);
        clear_IB = findViewById(R.id.clear_IB);
        rangeSlider = findViewById(R.id.range_slider);
        hand_IB = findViewById(R.id.hand_IB);
        undo_IB.setOnClickListener(this);
        redo_IB.setOnClickListener(this);
        pencil_IB.setOnClickListener(this);
        eraser_IB.setOnClickListener(this);
        colour_IB.setOnClickListener(this);
        fill_IB.setOnClickListener(this);
        clear_IB.setOnClickListener(this);
        hand_IB.setOnClickListener(this);
        pencil_IB.setColorFilter(ContextCompat.getColor(this, R.color.blue),
                PorterDuff.Mode.SRC_IN);

        pen_prew_color.setBackgroundColor(ContextCompat.getColor(this, R.color.pencil_color));
        fill_prew_color.setBackgroundColor(ContextCompat.getColor(this, R.color.paint_back_color));
    }


    private void openSettings() {
        BottomSheetDialog bottomSheetDialog =
                new BottomSheetDialog(DrawingActivity.this);
        bottomSheetDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                BottomSheetDialog b = (BottomSheetDialog) dialog;
                FrameLayout f = (FrameLayout)
                        b.findViewById(com.google.android.material.R.id.design_bottom_sheet);
                if (f != null) {
                    BottomSheetBehavior.from(f).setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        View contentView = View.inflate(DrawingActivity.this,
                R.layout.settings, null);
        Spinner spinner = contentView.findViewById(R.id.size_spinner);



        TextInputLayout neg_prompt_IL = contentView.findViewById(R.id.neg_prompt_IL);
        TextInputEditText neg_prompt_ET = contentView.findViewById(R.id.neg_prompt_ET);
        MaterialSwitch neg_prompt_SV = contentView.findViewById(R.id.neg_prompt_SV);
        MaterialSwitch save_history_SM = contentView.findViewById(R.id.save_history_SM);

        TextInputLayout custom_seed_IL = contentView.findViewById(R.id.custom_seed_IL);
        TextInputEditText custom_seed_ET = contentView.findViewById(R.id.custom_seed_ET);
        MaterialSwitch custom_seed_SV = contentView.findViewById(R.id.custom_seed_SV);


        ImageButton aspect_ratio_info_IB = contentView.findViewById(R.id.aspect_ratio_info_IB);
        ImageButton negative_prompt_info_IB = contentView.findViewById(R.id.negative_prompt_info_IB);
        ImageButton custom_seed_info_IB = contentView.findViewById(R.id.custom_seed_info_IB);
        ImageButton save_history_info_IB = contentView.findViewById(R.id.save_history_info_IB);


        TextInputLayout api_key_IL = contentView.findViewById(R.id.api_key_IL);
        TextInputEditText api_key_ET = contentView.findViewById(R.id.api_key_ET);
        ImageButton api_secret_IB = contentView.findViewById(R.id.api_secret_IB);


        List<String> res_values =
                Arrays.asList(getResources().getStringArray(R.array.resolution_values));
        List<String> res_arr =
                Arrays.asList(getResources().getStringArray(R.array.resolutions_array));

        ArrayAdapter<String> resolutions =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, res_arr);
        spinner.setAdapter(resolutions);
        spinner.setSelection(prefs.getInt("ratio_value", 0));
        final boolean[] userSelection = {false};
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (userSelection[0]) {
                    String value = res_values.get(position);
                    prefs.setInt("ratio_value", Integer.parseInt(value));
                    setCanvasRatio(Integer.parseInt(value));
                    bottomSheetDialog.dismiss();
                }
                userSelection[0] = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        String neg_prompt = prefs.getString("neg_prompt", "");
        if (!TextUtils.isEmpty(neg_prompt)) {
            neg_prompt_ET.setText(neg_prompt);
        }

        if (prefs.getBoolean("is_negative", false)) {
            neg_prompt_SV.setChecked(true);
            neg_prompt_IL.setEnabled(true);
        }


        long seed = prefs.getLong("custom_seed", 42);

        custom_seed_ET.setText("" + seed);

        if (!prefs.getBoolean("is_custom_seed", true)) {
            custom_seed_SV.setChecked(false);
            custom_seed_IL.setEnabled(false);
        }


        if (!TextUtils.isEmpty(API_KEY)) {
            api_key_ET.setText(API_KEY);
        }


        api_key_ET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String key = v.getText().toString();
                if (!TextUtils.isEmpty(key)) {
                    API_KEY = key;
                    prefs.setString("api_key", encrypt(key));
                    Utils.hideKeyboard(DrawingActivity.this);
                    api_key_ET.clearFocus();
                    api_key_IL.setErrorEnabled(false);
                } else {
                    api_key_IL.setError("API key cannot be empty");
                }
                return false;
            }
        });

        api_secret_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!v.isSelected()) {
                    api_key_ET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    api_secret_IB.setImageResource(R.drawable.eye_line);
                } else {
                    api_key_ET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    api_secret_IB.setImageResource(R.drawable.eye_close_line);
                }
                v.setSelected(!v.isSelected());

            }
        });


        aspect_ratio_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "Changes size of the paint canvas." +
                        "\nThe ai model used by this app is optimized for 512x512 images which is called Square (1:1) in the app." +
                        "\n Here's all aspect ratios to pixels:" +
                        "\n (16:9) = 1024x576"
                        + "\n (4:3) = 640x480"
                        + "\n (1:1) = 512x512"
                        + "\n (9:16) = 576x1024"
                        + "\n (3:4) = 480x640");
            }
        });

        negative_prompt_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "Negative prompt is a prompt for what you don't want to see in the result." +
                        "\n For example: 'blurry, ugly ...' .");
            }
        });

        custom_seed_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "Seeds are numerical values used to determine the starting point of image generation.\n" +
                        "Controlling the seed helps you generate reproducible images which is great for this type of app.\n" +
                        "If disabled every image will be generated with random seed which is not recommended");
            }
        });


        save_history_info_IB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createInfoWindow(v, "Disable this if you don't want to save every image generated to your device.");
            }
        });


        neg_prompt_ET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                prefs.setString("neg_prompt", neg_prompt_ET.getText().toString());
                Utils.hideKeyboard(DrawingActivity.this);
                neg_prompt_ET.clearFocus();
                return false;
            }
        });


        neg_prompt_SV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                neg_prompt_IL.setEnabled(isChecked);
                prefs.setBoolean("is_negative", isChecked);
            }
        });

        custom_seed_ET.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (!TextUtils.isEmpty(Objects.requireNonNull(custom_seed_ET.getText()).toString())) {
                    if (Long.parseLong(custom_seed_ET.getText().toString()) > 0) {
                        prefs.setLong("custom_seed", Long.parseLong(custom_seed_ET.getText().toString()));
                        Utils.hideKeyboard(DrawingActivity.this);
                        custom_seed_ET.clearFocus();
                        custom_seed_IL.setErrorEnabled(false);
                    } else {
                        custom_seed_IL.setError("Seed must be bigger than 0");
                    }
                } else {
                    custom_seed_IL.setError("Seed cannot be empty");
                }

                return false;
            }
        });

        custom_seed_SV.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                custom_seed_IL.setEnabled(isChecked);
                prefs.setBoolean("is_custom_seed", isChecked);
            }
        });


        save_history_SM.setChecked(prefs.getBoolean("save_history", true));

        save_history_SM.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.setBoolean("save_history", isChecked);
            }
        });


        bottomSheetDialog.setContentView(contentView);
        bottomSheetDialog.show();
    }

    private void genWithCheck(String prompt, String base64Image) {
        generateImage(prompt, base64Image);
    }

    private void createInfoWindow(View view, String info) {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.info_layout, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true;
        PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setContentView(popupView);
        TextView info_TV = popupView.findViewById(R.id.info_TV);
        info_TV.setText(info);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
            popupView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    popupWindow.dismiss();
                }
            });
        }


        popupWindow.showAsDropDown(view);
    }


    private void upscaleWithAPI(String model, int rescale_factor, boolean face) {
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this).create();
        View contentView = View.inflate(this, R.layout.progress_dialog, null);
        TextView loading_TV = contentView.findViewById(R.id.loadingTV);
        loading_TV.setText("Upscaling the image");
        progressDialog.setView(contentView);
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.show();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        currOutBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        String base64Image = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.DEFAULT);


        JSONObject postData = new JSONObject();
        try {
            postData.put("image_url", base64Image);
            postData.put("model", model);
            postData.put("scale", rescale_factor);
            postData.put("face", face);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    "https://fal.run/fal-ai/esrgan", postData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                Utils.i(TAG, response.toString());

                                JSONObject imageObject = response.getJSONObject("image");

                                String image_url = imageObject.getString("url");

                                int imageWidth = imageObject.getInt("width");
                                int imageHeight = imageObject.getInt("height");


                                prefs.setInt("up_pp", (prefs.getInt("up_pp", 0) - 1));

                                long currentExpire = prefs.getLong("up_expire", 0);
                                long milli = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(24);
                                if (currentExpire == 0) {
                                    prefs.setLong("up_expire", milli);
                                }


                                Glide.with(DrawingActivity.this).asBitmap().load(image_url)
                                        .into(new CustomTarget<Bitmap>() {
                                            @Override
                                            public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                                String filename = "Upscaled_" + System.currentTimeMillis() + ".png";
                                                File imageFile = new File(ApplicationPath.upscalePath(), filename);
                                                try {
                                                    FileOutputStream outStream = new FileOutputStream(imageFile);
                                                    resource.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                                                    outStream.flush();
                                                    outStream.close();
                                                    GalleryModel gModel = new GalleryModel();
                                                    gModel.prompt = "";
                                                    gModel.negative_prompt = "";
                                                    gModel.width = imageWidth;
                                                    gModel.height = imageHeight;
                                                    gModel.path = imageFile.getAbsolutePath();
                                                    Utils.i(TAG, "DB Image Path:" + gModel.path);
                                                    if (databaseHelper.getUpscaled(gModel.path) == null) {
                                                        databaseHelper.addUpscaled(gModel);
                                                        Toast.makeText(DrawingActivity.this, "Upscaled Image Saved", Toast.LENGTH_SHORT).show();
                                                        if (progressDialog.isShowing()) {
                                                            progressDialog.dismiss();
                                                        }
                                                        showBeforeAfterDialog(currOutBitmap, resource);
                                                    }
                                                } catch (IOException e) {
                                                    Utils.i(TAG, e.getMessage());
                                                }
                                            }

                                            @Override
                                            public void onLoadCleared(@Nullable Drawable placeholder) {

                                            }
                                        });


                            } catch (JSONException e) {
                                if (progressDialog.isShowing()) {
                                    progressDialog.dismiss();
                                }
                                Toast.makeText(DrawingActivity.this, "Error (JSONException): " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }

//                        System.out.println("Response: " + response.toString());
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (progressDialog.isShowing()) {
                                progressDialog.dismiss();
                            }
                            Utils.i(TAG, "Volley Error:" + error.getMessage());

                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Key " + API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };

            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Below android 7.1 regular volley request fails due to ssl issues with the api
            // So if device api is below android 7.1 it accepts everything
            // Not very secure for devices below 7.1
            // i should probably try implement it with okhttp or something maybe it wont have this problem.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Volley.newRequestQueue(DrawingActivity.this).add(jsonObjectRequest);
            } else {
                newRequestNoSSL().add(jsonObjectRequest);
            }


        } catch (Exception e) {
            Utils.e(TAG, e.getMessage());
        }


    }


    private void generateImage(String prompt, String base64Image) {

        if (TextUtils.isEmpty(API_KEY)) {
            Toast.makeText(this, "No api key found", Toast.LENGTH_LONG).show();
            showAPIkeyDialog();
            return;
        }

        if (drawView.isCanvasClear()) {
            Toast.makeText(this, "You didn't draw anything yet", Toast.LENGTH_SHORT).show();
            return;
        }

        JSONObject postData = new JSONObject();
        try {
            postData.put("enable_safety_checks", false);
            postData.put("prompt", prompt);
            if (prefs.getBoolean("is_negative", false)) {
                String neg_prompt = prefs.getString("neg_prompt", "");
                if (!TextUtils.isEmpty(neg_prompt)) {
                    postData.put("negative_prompt", neg_prompt);
                }
            }
            postData.put("image_url", base64Image);
            postData.put("sync_mode", true);
            if (prefs.getBoolean("is_custom_seed", true)) {
                long seed = prefs.getLong("custom_seed", 42);
                postData.put("seed", seed);
            }
//            postData.put("image_size","landscape_16_9");
            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST,
                    "https://fal.run/fal-ai/lcm-sd15-i2i", postData,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray imagesArray = response.getJSONArray("images");

                                JSONObject imageObject = imagesArray.getJSONObject(0);
                                String imageUrl = imageObject.getString("url");
                                int imageWidth = imageObject.getInt("width");
                                int imageHeight = imageObject.getInt("height");
//                                String contentType = imageObject.getString("content_type");

                                final String pureBase64Encoded = imageUrl.substring(imageUrl.indexOf(",") + 1);
                                byte[] imageByteArray = Base64.decode(pureBase64Encoded, Base64.DEFAULT);

                                currOutBitmap = BitmapFactory.decodeByteArray(imageByteArray, 0, imageByteArray.length);
                                imageView.setImageBitmap(currOutBitmap);


                                galleryModel = new GalleryModel();
                                galleryModel.prompt = prompt;
                                galleryModel.negative_prompt = "";
                                if (prefs.getBoolean("is_negative", false)) {
                                    String neg_prompt = prefs.getString("neg_prompt", "");
                                    if (!TextUtils.isEmpty(neg_prompt)) {
                                        galleryModel.negative_prompt = neg_prompt;
                                    }
                                }
                                galleryModel.width = imageWidth;
                                galleryModel.height = imageHeight;
                                if (response.has("seed")) {
                                    galleryModel.seed = response.getString("seed");
                                } else {
                                    galleryModel.seed = String.valueOf(prefs.getLong("custom_seed", 42));
                                }

                                if (prefs.getBoolean("save_history", true)) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {

                                            try {
                                                File cacheDir = getExternalFilesDir("history");
                                                if (cacheDir != null) {
                                                    File result = new File(cacheDir, "generation_history");
                                                    if (result.isDirectory() || result.mkdirs()) {
                                                        String filename = System.currentTimeMillis() + ".jpg";
                                                        File imageFile = new File(result, filename);
                                                        try {
                                                            FileOutputStream outStream = new FileOutputStream(imageFile);
                                                            currOutBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
                                                            outStream.flush();
                                                            outStream.close();
                                                            galleryModel.path = imageFile.getAbsolutePath();
                                                            Utils.i(TAG, "DB Image Path:" + galleryModel.path);
                                                            if (databaseHelper.getHistory(galleryModel.path) == null) {
                                                                databaseHelper.addHistory(galleryModel);
                                                            }
                                                        } catch (IOException e) {
                                                            Utils.i(TAG, e.getMessage());
                                                        }

                                                    }

                                                }
                                            } catch (Exception e) {
                                                Utils.e(TAG, e.getMessage());
                                            }
                                        }
                                    }).start();

                                }
                            } catch (JSONException e) {
                                Toast.makeText(DrawingActivity.this, "Error(JsonError): " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            Toast.makeText(DrawingActivity.this, "Error(Volley error): " + error.getMessage(), Toast.LENGTH_SHORT).show();

                        }
                    }) {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Authorization", "Key " + API_KEY);
                    headers.put("Content-Type", "application/json");
                    return headers;
                }
            };
            jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(
                    0,
                    0,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            // Below android 7.1 regular volley request fails due to ssl issues with the api
            // So if device api is below android 7.1 it accepts everything
            // Not very secure for devices below 7.1
            // i should probably try implement it with okhttp or something maybe it wont have this problem.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
                Volley.newRequestQueue(DrawingActivity.this).add(jsonObjectRequest);
            } else {
                newRequestNoSSL().add(jsonObjectRequest);
            }
        } catch (Exception e) {
            Utils.e(TAG, e.getMessage());
        }


    }

    private RequestQueue newRequestNoSSL() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, null);

            HurlStack hurlStack = new HurlStack(null, sslContext.getSocketFactory());

            return Volley.newRequestQueue(DrawingActivity.this, hurlStack);

        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            Utils.e(TAG, e.getMessage());
            return Volley.newRequestQueue(DrawingActivity.this);
        }
    }

    @Override
    public void onColorSelected(int dialogId, int color) {
        Utils.i(TAG, "Color:" + Integer.toHexString(color));
        if (dialogId == DIALOG_ID) {
            if (isBackground) {
                fill_prew_color.setBackgroundColor(color);
                drawView.changeBackground(color);
            } else {
                pen_prew_color.setBackgroundColor(color);
                drawView.setColour(color);
            }
        }
    }

    @Override
    public void onStrokeChanged(float stroke) {
        drawView.setStrokeWidth(stroke);
        rangeSlider.setValues(stroke);
    }


    @Override
    public void onDialogDismissed(int dialogId) {

    }


    private void showAPIkeyDialog() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this).create();
        alertDialog.setTitle("Enter API Key");
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        View contentView = View.inflate(this, R.layout.input_dialog, null);
        TextInputLayout api_key_IL = contentView.findViewById(R.id.api_key_IL);
        TextInputEditText api_key_ET = contentView.findViewById(R.id.api_key_ET);


        alertDialog.setView(contentView, 20, 20, 20, 20);

        alertDialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Get api key", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("https://fal.ai/"));
                startActivity(intent);
            }
        });

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, "Set it", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String key = api_key_ET.getText().toString();
                if (!TextUtils.isEmpty(key)) {
                    api_key_IL.setErrorEnabled(false);
                    API_KEY = key;
                    prefs.setString("api_key", encrypt(key));
                    Toast.makeText(DrawingActivity.this, "API key saved.", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(DrawingActivity.this, "No api key set", Toast.LENGTH_SHORT).show();
                }
            }
        });
        alertDialog.show();

    }

    public static String encrypt(String input) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
            SecretKeySpec keySpec = new SecretKeySpec(enc.getBytes(StandardCharsets.UTF_8), AES);
            IvParameterSpec ivSpec = new IvParameterSpec(vect.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedBytes = cipher.doFinal(input.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(encryptedBytes, Base64.DEFAULT);
        } catch (Exception e) {
            Utils.e(TAG, e.getMessage());
            return null;
        }
    }

    public static String decrypt(String encryptedInput) {
        try {
            Cipher cipher = Cipher.getInstance(AES_CBC_PKCS5_PADDING);
            SecretKeySpec keySpec = new SecretKeySpec(enc.getBytes(StandardCharsets.UTF_8), AES);
            IvParameterSpec ivSpec = new IvParameterSpec(vect.getBytes(StandardCharsets.UTF_8));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] encryptedBytes = Base64.decode(encryptedInput, Base64.DEFAULT);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            Utils.e(TAG, e.getMessage());
            return null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.history) {
            Intent hgm = new Intent(this, GalleryActivity.class);
            hgm.putExtra("tabPos", 0);
            mainActivityResultLauncher.launch(hgm);
        } else if (id == R.id.saved) {
            Intent hgm = new Intent(this, GalleryActivity.class);
            hgm.putExtra("tabPos", 1);
            mainActivityResultLauncher.launch(hgm);
        } else if (id == R.id.upscale) {
            Intent hgm = new Intent(this, GalleryActivity.class);
            hgm.putExtra("tabPos", 2);
            mainActivityResultLauncher.launch(hgm);
        } else if (id == R.id.settings_menu) {
            startActivity(new Intent(this, PrefsActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }


    public ActivityResultLauncher<Intent> mainActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {

                }
            });


    private void showBeforeAfterDialog(Bitmap foregroundBitmap, Bitmap backgroundBitmap) {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this).create();

        View contentView = View.inflate(this, R.layout.upscale_compare_dialog, null);
        BeforeAfterSlider beforeAfterSlider = contentView.findViewById(R.id.before_after_slider);
        beforeAfterSlider.setBack(backgroundBitmap);
        beforeAfterSlider.setFore(foregroundBitmap);
//        beforeAfterSlider.setTestImages();

        alertDialog.setView(contentView);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();
    }


    private void showTutorial() {
        AlertDialog alertDialog = new MaterialAlertDialogBuilder(this).create();
        alertDialog.setTitle("Tutorial");
        alertDialog.setCanceledOnTouchOutside(false);
        alertDialog.setCancelable(false);
        View contentView = View.inflate(this, R.layout.layout_image, null);
        alertDialog.setView(contentView);
        ImageView gif_IV = contentView.findViewById(R.id.gif_IV);
        Glide.with(this).asGif().load(R.raw.tutorial2).into(gif_IV);
        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        alertDialog.show();

    }


    private String getLeftText(long millisUntilFinished) {
        return String.format(FORMAT,
                TimeUnit.MILLISECONDS.toHours(millisUntilFinished),
                TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished) - TimeUnit.HOURS.toMinutes(
                        TimeUnit.MILLISECONDS.toHours(millisUntilFinished)),
                TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) - TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)));
    }


}

