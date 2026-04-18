package com.mobile2.app;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

/**
 * DataDisplayActivity displays the database items for the LOGGED-IN user.
 */
public class DataDisplayActivity extends AppCompatActivity {

    private DatabaseHelper dbHelper;
    private EventAdapter adapter;
    private List<EventItem> eventList;
    private EditText nameInput, valueInput;
    private Button addDataButton;
    private int selectedId = -1;
    private String currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data_display);

        // Get the username passed from LoginActivity
        currentUser = getIntent().getStringExtra("CURRENT_USER");
        if (currentUser == null) {
            currentUser = "default_user";
        }

        dbHelper = new DatabaseHelper(this);
        eventList = new ArrayList<>();

        ListView listView = findViewById(R.id.data_list_view);
        nameInput = findViewById(R.id.new_data_name);
        valueInput = findViewById(R.id.new_data_value);
        addDataButton = findViewById(R.id.add_data_button);
        Button settingsButton = findViewById(R.id.settings_button);
        
        adapter = new EventAdapter();
        listView.setAdapter(adapter);

        addDataButton.setOnClickListener(v -> {
            if (selectedId == -1) {
                addNewEvent();
            } else {
                updateExistingEvent();
            }
        });
        
        settingsButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("CURRENT_USER", currentUser);
            startActivity(intent);
        });

        loadData();
    }

    private void loadData() {
        eventList.clear();
        // Only load data belonging to the current user
        Cursor cursor = dbHelper.getEventsForUser(currentUser);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_NAME));
                String value = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EVENT_VALUE));
                eventList.add(new EventItem(id, name, value));
            } while (cursor.moveToNext());
            cursor.close();
        }
        adapter.notifyDataSetChanged();
    }

    private void addNewEvent() {
        String name = nameInput.getText().toString().trim();
        String value = valueInput.getText().toString().trim();

        if (name.isEmpty() || value.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save with the current user's name
        if (dbHelper.addEvent(currentUser, name, value)) {
            clearInputs();
            loadData();
            Toast.makeText(this, "Event added", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateExistingEvent() {
        String name = nameInput.getText().toString().trim();
        String value = valueInput.getText().toString().trim();

        if (dbHelper.updateEvent(selectedId, name, value)) {
            clearInputs();
            loadData();
            Toast.makeText(this, "Event updated", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearInputs() {
        nameInput.setText("");
        valueInput.setText("");
        selectedId = -1;
        addDataButton.setText("Add to Grid");
    }

    private static class EventItem {
        int id;
        String name;
        String value;
        EventItem(int id, String name, String value) {
            this.id = id;
            this.name = name;
            this.value = value;
        }
    }

    private class EventAdapter extends BaseAdapter {
        @Override
        public int getCount() { return eventList.size(); }
        @Override
        public Object getItem(int position) { return eventList.get(position); }
        @Override
        public long getItemId(int position) { return eventList.get(position).id; }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(DataDisplayActivity.this)
                        .inflate(R.layout.item_event, parent, false);
            }

            EventItem item = eventList.get(position);
            TextView nameView = convertView.findViewById(R.id.event_name);
            TextView valueView = convertView.findViewById(R.id.event_date);
            Button deleteBtn = convertView.findViewById(R.id.delete_button);

            nameView.setText(item.name);
            valueView.setText(item.value);

            deleteBtn.setOnClickListener(v -> {
                if (dbHelper.deleteEvent(item.id)) {
                    loadData();
                    Toast.makeText(DataDisplayActivity.this, "Deleted", Toast.LENGTH_SHORT).show();
                }
            });

            convertView.setOnClickListener(v -> {
                selectedId = item.id;
                nameInput.setText(item.name);
                valueInput.setText(item.value);
                addDataButton.setText("Update Entry");
            });

            return convertView;
        }
    }
}
