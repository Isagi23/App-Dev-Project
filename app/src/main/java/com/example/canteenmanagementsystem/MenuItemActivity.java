package com.example.canteenmanagementsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.example.canteenmanagementsystem.adapters.MenuItemAdapter;
import com.example.canteenmanagementsystem.api.SpoonacularService;
import com.example.canteenmanagementsystem.models.MenuItem;
import com.example.canteenmanagementsystem.utils.NetworkUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MenuItemActivity extends AppCompatActivity implements MenuItemAdapter.OnItemClickListener {

    private RecyclerView rvMenuItems;
    private MenuItemAdapter adapter;
    private List<MenuItem> menuItemsList;
    private List<MenuItem> filteredList;
    private FirebaseFirestore db;
    private CollectionReference menuRef;
    private SwipeRefreshLayout swipeRefresh;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private SpoonacularService spoonacularService;
    private final String API_KEY = "a6c0844309464a0dad862f50ddcaad8d";

    private Uri selectedImageUri;
    private ImageView ivDialogImage;
    private Button btnSelectImage;
    private android.widget.ProgressBar dialogProgressBar;
    private static final int PICK_IMAGE_REQUEST = 1;

    private TextView tvItemCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu_item);

        db = FirebaseFirestore.getInstance();
        menuRef = db.collection("menuItems");
        storage = FirebaseStorage.getInstance("gs://canteen-management-syste-12618.firebasestorage.app");
        storageRef = storage.getReference("menu_images");

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.spoonacular.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        spoonacularService = retrofit.create(SpoonacularService.class);

        rvMenuItems = findViewById(R.id.rvMenuItems);
        rvMenuItems.setLayoutManager(new LinearLayoutManager(this));
        menuItemsList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new MenuItemAdapter(filteredList, this);
        rvMenuItems.setAdapter(adapter);

        tvItemCount = findViewById(R.id.tvItemCount);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadMenuItems);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        View btnAdd = findViewById(R.id.btnAddMenuItemHeader);
        if (btnAdd != null) {
            btnAdd.setOnClickListener(v -> showAddEditDialog(null));
        }

        findViewById(R.id.btnFilterAll).setOnClickListener(v -> filterByCategory("All"));
        findViewById(R.id.btnFilterFood).setOnClickListener(v -> filterByCategory("Food"));
        findViewById(R.id.btnFilterDrinks).setOnClickListener(v -> filterByCategory("Drinks"));

        loadMenuItems();
    }

    private void filterByCategory(String category) {
        // Since MenuItem model doesn't have a category field yet, we'll just show all for now
        // or filter based on names as a placeholder if applicable.
        // Update UI of buttons
        updateFilterButtons(category);
        
        filteredList.clear();
        if (category.equals("All")) {
            filteredList.addAll(menuItemsList);
        } else {
            // Placeholder: filter by common keywords if category not in DB
            for (MenuItem item : menuItemsList) {
                String name = item.getName().toLowerCase();
                if (category.equals("Drinks")) {
                    if (name.contains("water") || name.contains("soda") || name.contains("juice") || name.contains("mismo") || name.contains("drink")) {
                        filteredList.add(item);
                    }
                } else if (category.equals("Food")) {
                    if (!name.contains("water") && !name.contains("soda") && !name.contains("juice") && !name.contains("mismo")) {
                        filteredList.add(item);
                    }
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateFilterButtons(String selected) {
        Button btnAll = findViewById(R.id.btnFilterAll);
        Button btnFood = findViewById(R.id.btnFilterFood);
        Button btnDrinks = findViewById(R.id.btnFilterDrinks);

        styleFilterButton(btnAll, selected.equals("All"));
        styleFilterButton(btnFood, selected.equals("Food"));
        styleFilterButton(btnDrinks, selected.equals("Drinks"));
    }

    private void styleFilterButton(Button btn, boolean isSelected) {
        if (isSelected) {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.primary_dark)));
            btn.setTextColor(getResources().getColor(R.color.white));
        } else {
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.TRANSPARENT));
            btn.setTextColor(getResources().getColor(R.color.text_secondary));
        }
    }

    private void filter(String text) {
        filteredList.clear();
        if (text.isEmpty()) {
            filteredList.addAll(menuItemsList);
        } else {
            for (MenuItem item : menuItemsList) {
                if (item.getName().toLowerCase().contains(text.toLowerCase())) {
                    filteredList.add(item);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadMenuItems() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            return;
        }
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        menuRef.addSnapshotListener((value, error) -> {
            if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
            if (error != null) {
                Toast.makeText(this, "Error loading menu: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            if (value != null) {
                menuItemsList.clear();
                for (QueryDocumentSnapshot doc : value) {
                    MenuItem item = doc.toObject(MenuItem.class);
                    item.setId(doc.getId());
                    menuItemsList.add(item);
                }
                
                if (tvItemCount != null) {
                    tvItemCount.setText(menuItemsList.size() + " items available");
                }

                filterByCategory("All");

                if (menuItemsList.isEmpty()) {
                    preLoadDefaultItems();
                }
            }
        });
    }

    private void preLoadDefaultItems() {
        String[][] defaults = {
                {"Rice", "15", "https://spoonacular.com/productImages/Rice.jpg"},
                {"Chicken", "55", "https://spoonacular.com/productImages/Chicken.jpg"},
                {"Fish", "45", "https://spoonacular.com/productImages/Fish.jpg"},
                {"Pork", "50", "https://spoonacular.com/productImages/Pork.jpg"},
                {"Soup", "20", "https://spoonacular.com/productImages/Soup.jpg"},
                {"Egg", "20", "https://spoonacular.com/productImages/Egg.jpg"},
                {"Soda", "15", "https://spoonacular.com/productImages/Soda.jpg"},
                {"Water", "10", "https://spoonacular.com/productImages/Water.jpg"}
        };

        for (String[] def : defaults) {
            MenuItem item = new MenuItem(null, def[0], Double.parseDouble(def[1]), def[2]);
            menuRef.add(item);
        }
    }

    private void showAddEditDialog(MenuItem item) {
        selectedImageUri = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_menu_item, null);
        AutoCompleteTextView etName = view.findViewById(R.id.etItemName);
        EditText etPrice = view.findViewById(R.id.etItemPrice);
        ivDialogImage = view.findViewById(R.id.ivDialogItemImage);
        btnSelectImage = view.findViewById(R.id.btnSelectImage);
        dialogProgressBar = view.findViewById(R.id.progressBar);

        if (item != null) {
            builder.setTitle("Edit Menu Item");
            etName.setText(item.getName());
            etPrice.setText(String.valueOf(item.getPrice()));
            if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
                Glide.with(this).load(item.getImageUrl()).into(ivDialogImage);
            }
        } else {
            builder.setTitle("Add Menu Item");
        }

        btnSelectImage.setOnClickListener(v -> openImagePicker());

        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 2) {
                    fetchSuggestions(s.toString(), etName);
                }
            }
        });

        builder.setView(view);
        builder.setPositiveButton("Save", null);
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String priceStr = etPrice.getText().toString().trim();

            if (TextUtils.isEmpty(name) || TextUtils.isEmpty(priceStr)) {
                Toast.makeText(this, "Fields cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double price = Double.parseDouble(priceStr);
            if (price <= 0) {
                Toast.makeText(this, "Price must be greater than zero", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dialogProgressBar != null) dialogProgressBar.setVisibility(View.VISIBLE);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

            if (selectedImageUri != null) {
                uploadImageAndSave(item, name, price, dialog);
            } else {
                saveToFirestore(item, name, price, item != null ? item.getImageUrl() : "", dialog);
            }
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            if (ivDialogImage != null) {
                ivDialogImage.setImageURI(selectedImageUri);
            }
        }
    }

    private void uploadImageAndSave(MenuItem item, String name, double price, AlertDialog dialog) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            
            // Resize image to keep it small for Firestore (max 1MB)
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 400, 400, true);
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream);
            byte[] byteArray = outputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);
            String imageUrl = "data:image/jpeg;base64," + base64Image;

            saveToFirestore(item, name, price, imageUrl, dialog);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            if (dialogProgressBar != null) dialogProgressBar.setVisibility(View.GONE);
            if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
    }

    private void saveToFirestore(MenuItem item, String name, double price, String imageUrl, AlertDialog dialog) {
        if (item == null) {
            MenuItem newItem = new MenuItem(null, name, price, imageUrl);
            menuRef.add(newItem).addOnSuccessListener(documentReference -> {
                if (dialogProgressBar != null) dialogProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Item added", Toast.LENGTH_SHORT).show();
                if (dialog != null) dialog.dismiss();
            }).addOnFailureListener(e -> {
                if (dialogProgressBar != null) dialogProgressBar.setVisibility(View.GONE);
                if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        } else {
            item.setName(name);
            item.setPrice(price);
            item.setImageUrl(imageUrl);
            menuRef.document(item.getId()).set(item).addOnSuccessListener(aVoid -> {
                if (dialogProgressBar != null) dialogProgressBar.setVisibility(View.GONE);
                Toast.makeText(this, "Item updated", Toast.LENGTH_SHORT).show();
                if (dialog != null) dialog.dismiss();
            }).addOnFailureListener(e -> {
                if (dialogProgressBar != null) dialogProgressBar.setVisibility(View.GONE);
                if (dialog != null) dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }
    }

    private void fetchSuggestions(String query, AutoCompleteTextView view) {
        spoonacularService.autocomplete(query, 5, API_KEY).enqueue(new Callback<List<SpoonacularService.AutocompleteResponse>>() {
            @Override
            public void onResponse(Call<List<SpoonacularService.AutocompleteResponse>> call, Response<List<SpoonacularService.AutocompleteResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<String> suggestions = new ArrayList<>();
                    for (SpoonacularService.AutocompleteResponse r : response.body()) {
                        suggestions.add(r.title);
                    }
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(MenuItemActivity.this, android.R.layout.simple_dropdown_item_1line, suggestions);
                    view.setAdapter(adapter);
                    view.showDropDown();
                }
            }

            @Override
            public void onFailure(Call<List<SpoonacularService.AutocompleteResponse>> call, Throwable t) {}
        });
    }

    @Override
    public void onItemClick(MenuItem item) {
        showAddEditDialog(item);
    }

    @Override
    public void onItemLongClick(MenuItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Item")
                .setMessage("Are you sure you want to delete " + item.getName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    menuRef.document(item.getId()).delete()
                            .addOnSuccessListener(aVoid -> Toast.makeText(this, "Item deleted", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}