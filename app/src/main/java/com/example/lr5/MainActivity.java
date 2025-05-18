package com.example.lr5;
import android.net.Uri;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import androidx.documentfile.provider.DocumentFile;
import androidx.annotation.Nullable;
import android.widget.Spinner;
import android.widget.AdapterView;
import android.os.Handler;





public class MainActivity extends AppCompatActivity {

    private Handler slideshowHandler;
    private Runnable slideshowRunnable;
    private boolean isSlideshowRunning = false;

    private String currentSort = "Назвою";

    private TextView imageCounter;


    private static final int REQUEST_CODE_OPEN_DIRECTORY = 100;
    private ImageView imageView;

    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri imageUri;
    private List<Uri> imageList = new ArrayList<>();
    private int currentIndex = -1;
    private String currentFilter = "*";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        slideshowHandler = new Handler();

        findViewById(R.id.btnStartSlideshow).setOnClickListener(v -> startSlideshow());
        findViewById(R.id.btnStopSlideshow).setOnClickListener(v -> stopSlideshow());



        imageView = findViewById(R.id.imageView);
        imageCounter = findViewById(R.id.imageCounter);

        Spinner sortSpinner = findViewById(R.id.sortSpinner);

// 🔽 Додаємо значення в Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.sort_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(adapter);

// 🔽 Обробка вибору сортування
        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // нічого
            }
        });




        findViewById(R.id.btnAuthor).setOnClickListener(v -> showAuthorDialog());

        findViewById(R.id.btnSelectFolder).setOnClickListener(v -> openDirectory());


        findViewById(R.id.btnAll).setOnClickListener(v -> {
            currentFilter = "*";
            openDirectory(); // відкриває вибір папки
        });

        findViewById(R.id.btnJpeg).setOnClickListener(v -> {
            currentFilter = "jpeg";
            openDirectory();
        });

        findViewById(R.id.btnPng).setOnClickListener(v -> {
            currentFilter = "png";
            openDirectory();
        });

        findViewById(R.id.btnTiff).setOnClickListener(v -> {
            currentFilter = "tiff";
            openDirectory();
        });

        findViewById(R.id.btnNext).setOnClickListener(v -> showNextImage());
        findViewById(R.id.btnPrev).setOnClickListener(v -> showPreviousImage());
    }

    private void openDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_OPEN_DIRECTORY);
    }

    private void startSlideshow() {
        if (imageList.isEmpty()) {
            Toast.makeText(this, "Немає зображень для перегляду", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isSlideshowRunning) return;

        isSlideshowRunning = true;

        slideshowRunnable = new Runnable() {
            @Override
            public void run() {
                if (imageList.isEmpty()) return;

                if (currentIndex < imageList.size() - 1) {
                    currentIndex++;
                } else {
                    currentIndex = 0; // почати з початку
                }

                displayImage(imageList.get(currentIndex));
                slideshowHandler.postDelayed(this, 3000); // 3 секунди
            }
        };

        slideshowHandler.post(slideshowRunnable);
    }

    private void stopSlideshow() {
        if (isSlideshowRunning && slideshowHandler != null && slideshowRunnable != null) {
            slideshowHandler.removeCallbacks(slideshowRunnable);
            isSlideshowRunning = false;
            Toast.makeText(this, "Автоперегляд зупинено", Toast.LENGTH_SHORT).show();
        }
    }


    private void loadImagesFromDirectory(Uri treeUri) {
        imageList.clear();
        currentIndex = -1;

        DocumentFile pickedDir = DocumentFile.fromTreeUri(this, treeUri);

        if (pickedDir != null && pickedDir.isDirectory()) {
            DocumentFile[] files = pickedDir.listFiles();

            if (files != null) {
                List<DocumentFile> sortedFiles = Arrays.asList(files);

                // Сортування
                switch (currentSort) {
                    case "Назвою":
                        Collections.sort(sortedFiles, Comparator.comparing(DocumentFile::getName, String.CASE_INSENSITIVE_ORDER));
                        break;
                    case "Датою":
                        Collections.sort(sortedFiles, Comparator.comparingLong(DocumentFile::lastModified).reversed());
                        break;
                    case "Розміром":
                        Collections.sort(sortedFiles, Comparator.comparingLong(DocumentFile::length).reversed());
                        break;
                }

                // Фільтрація та додавання
                for (DocumentFile file : sortedFiles) {
                    if (file.isFile() && isImageFile(file.getName())) {
                        imageList.add(file.getUri());
                        Log.d("DEBUG_TAG", "Файл додано: " + file.getName());
                    }
                }
            }
        }

        if (!imageList.isEmpty()) {
            currentIndex = 0;
            displayImage(imageList.get(currentIndex));
            updateImageCounter();
        } else {
            String formatText;
            switch (currentFilter) {
                case "jpeg":
                    formatText = "JPEG (.jpg, .jpeg)";
                    break;
                case "png":
                    formatText = "PNG (.png)";
                    break;
                case "tiff":
                    formatText = "TIFF (.tif, .tiff)";
                    break;
                default:
                    formatText = "вибраного формату";
            }

            Toast.makeText(this, "У папці немає фото з форматом: " + formatText, Toast.LENGTH_LONG).show();
            imageCounter.setText("Фото 0 з 0");
            imageView.setImageDrawable(null); // очищаємо зображення
        }
    }


        private void updateImageCounter() {
        if (imageList.isEmpty() || currentIndex < 0) {
            imageCounter.setText("Фото 0 з 0");
        } else {
            String counterText = "Фото " + (currentIndex + 1) + " з " + imageList.size();
            imageCounter.setText(counterText);
        }
    }


    private void selectMultipleImages() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }



    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        fileName = fileName.toLowerCase();

        boolean matchesExtension = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                fileName.endsWith(".png") || fileName.endsWith(".tif") || fileName.endsWith(".tiff");

        if (!matchesExtension) return false;

        if (currentFilter.equals("*")) return true;

        if (currentFilter.equals("jpeg")) return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg");
        if (currentFilter.equals("png")) return fileName.endsWith(".png");
        if (currentFilter.equals("tiff")) return fileName.endsWith(".tif") || fileName.endsWith(".tiff");

        return false;
    }



    private void showAuthorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Автор");

        // Створюємо вертикальний layout для фото + текст + кнопка
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        // Фото
        ImageView imageView = new ImageView(this);
        imageView.setImageResource(R.drawable.author_photo);
        imageView.setAdjustViewBounds(true);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        layout.addView(imageView);

        // Текст
        TextView textView = new TextView(this);
        textView.setText("Програму створив Владислав ПР-4-1");
        textView.setTextSize(18);
        textView.setPadding(0, 16, 0, 16);
        layout.addView(textView);

        // Додаємо layout у діалог
        builder.setView(layout);

        // Кнопка Назад/Закрити
        builder.setNegativeButton("Назад", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }


    private void loadImages(String imageType) {
        currentFilter = imageType;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_OPEN_DIRECTORY && resultCode == RESULT_OK && data != null) {
            Uri treeUri = data.getData();

            final int takeFlags = data.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION;
            getContentResolver().takePersistableUriPermission(treeUri, takeFlags);

            loadImagesFromDirectory(treeUri);
        }

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK) {
            imageList.clear();
            currentIndex = -1;

            if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                for (int i = 0; i < count; i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    imageList.add(uri);
                }
            } else if (data.getData() != null) {
                imageList.add(data.getData());
            }

            if (!imageList.isEmpty()) {
                currentIndex = 0;
                displayImage(imageList.get(currentIndex));
                updateImageCounter();
            }
        }
    }




    private void displayImage(Uri uri) {
        imageView.setImageURI(uri);
        updateImageCounter();
    }

    private void showNextImage() {
        if (imageList.size() > 0 && currentIndex < imageList.size() - 1) {
            currentIndex++;
            displayImage(imageList.get(currentIndex));
        } else {
            Toast.makeText(this, "Це останнє зображення", Toast.LENGTH_SHORT).show();
        }
    }


    private void showPreviousImage() {
        if (imageList.size() > 0 && currentIndex > 0) {
            currentIndex--;
            displayImage(imageList.get(currentIndex));
        } else {
            Toast.makeText(this, "Це перше зображення", Toast.LENGTH_SHORT).show();
        }
    }
}
