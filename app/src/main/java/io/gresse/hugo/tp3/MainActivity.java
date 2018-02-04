package io.gresse.hugo.tp3;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import android.widget.Toolbar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.plattysoft.leonids.ParticleSystem;

import java.util.ArrayList;
import java.util.List;

/**
 * First app open
 * 1. blank SP
 * 2. MainActivty avec SP vide
 *
 *
 *
 * Display a simple chat connected to Firebase
 */
public class MainActivity extends AppCompatActivity implements ValueEventListener, MessageAdapter.Listener {




    public static final String TAG = MainActivity.class.getSimpleName();

    EditText       mInputEditText;
    ImageButton    mSendButton;
    MessageAdapter mMessageAdapter;
    User user;
    DatabaseReference mDatabaseReference;
    RecyclerView recyclerView;
    private final Handler handler = new Handler();

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);
        switch (item.getItemId()){
            case R.id.logout:
                UserStorage.clear(getApplicationContext());
                Intent log = new Intent(MainActivity.this, NamePickerActivity.class);
                startActivity(log);
                finish();
                return true;
            default:
                return false;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (!UserStorage.isUserLoggedIn(this)) {
            Intent intent = new Intent(this, NamePickerActivity.class);
            this.startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_main);


        recyclerView = findViewById(R.id.recyclerView);
        mInputEditText = findViewById(R.id.inputEditText);
        mSendButton = findViewById(R.id.sendButton);

        user = UserStorage.getUserInfo(this);
        System.out.println("email: "+user.email);
        mMessageAdapter = new MessageAdapter(this, new ArrayList<Message>(), user);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(mMessageAdapter);

        connectAndListenToFirebase();

        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                submitNewMessage(mInputEditText.getText().toString());
                mInputEditText.setText("");

                new ParticleSystem(MainActivity.this, 10, R.drawable.confeti1, 5000)
                        .setSpeedModuleAndAngleRange(0f, 0.3f, 60, 120)
                        .setRotationSpeed(150)
                        .setAcceleration(0.00005f, 90)
                        .oneShot(findViewById(R.id.card_view), 8);

                new ParticleSystem(MainActivity.this, 10, R.drawable.confeti3, 5000)
                        .setSpeedModuleAndAngleRange(0f, 0.3f, 60, 120)
                        .setRotationSpeed(150)
                        .setAcceleration(0.00005f, 90)
                        .oneShot(findViewById(R.id.card_view), 8);
                }

        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        mDatabaseReference.removeEventListener(this);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        List<Message> items = new ArrayList<>();
        for (DataSnapshot messageSnapshot : dataSnapshot.getChildren()) {
                Message message = messageSnapshot.getValue(Message.class);
                message.key = messageSnapshot.getKey();
                items.add(message);
        }
        mMessageAdapter.setData(items);
        recyclerView.scrollToPosition(mMessageAdapter.getItemCount() - 1);

    }

    @Override
    public void onCancelled(DatabaseError databaseError) {
        Toast.makeText(this, "Error: " + databaseError, Toast.LENGTH_SHORT).show();

    }

    private void connectAndListenToFirebase() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mDatabaseReference = database.getReference(Constant.FIREBASE_PATH);

        mDatabaseReference.addValueEventListener(this);
    }

    private void submitNewMessage(String message) {
        User user = UserStorage.getUserInfo(this);
        if (message.isEmpty() || user == null) {
            return;
        }
        DatabaseReference newData  = mDatabaseReference.push();
        newData.setValue(
                new Message(message,
                        user.name,
                        user.email,
                        System.currentTimeMillis()));
    }

    @Override
    public void onItemClick(int position, Message message) {
        mDatabaseReference.child(message.key).removeValue();
    }

    @Override
    public void onItemLongClick(int position, final Message message){

        AlertDialog.Builder optionDialog = new AlertDialog.Builder(this);
        final AlertDialog.Builder updateDialog = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(message.content);
        updateDialog.setView(input);

        optionDialog.setTitle("Options");
        optionDialog.setItems(R.array.options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which){
                    case 0:
                        mDatabaseReference.child(message.key).removeValue();
                        break;
                    case 1:
                        updateDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String m_Text = input.getText().toString();
                                mDatabaseReference.child(message.key).setValue(new Message(m_Text,message.userName,
                                        message.userEmail, message.timestamp));
                            }
                        });
                        updateDialog.show();
                    default:
                        break;
                }
            }
        });
        optionDialog.show();
    }
}
