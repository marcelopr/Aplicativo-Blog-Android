package br.com.brainy.blogsaude;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SetupActivity extends AppCompatActivity {

    private ImageView mSetupImageBtn;
    private EditText mNameField, mFormationField;
    private Button mSubmitBtn;

    private static int GALLERY_REQUEST = 1;

    private Uri imageUri = null;
    private Uri resultUri = null;

    private DatabaseReference mDatabaseUsers;
    private FirebaseAuth mAuth;
    private StorageReference mStorageImage;

    private ProgressDialog mProgress;

    private Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        toolbar = (Toolbar) findViewById(R.id.tb_perfil);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");

        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);
        actionBar.setTitle("");

        mSetupImageBtn = (ImageView) findViewById(R.id.setupImageBtn);
        mNameField = (EditText) findViewById(R.id.setupNameField);
        mFormationField = (EditText) findViewById(R.id.setupFormationField);
        mSubmitBtn = (Button) findViewById(R.id.setupSubmitBtn);

        mStorageImage = FirebaseStorage.getInstance().getReference().child("Profile_images");
        mDatabaseUsers = FirebaseDatabase.getInstance().getReference().child("Users");
        mAuth = FirebaseAuth.getInstance();

        mProgress = new ProgressDialog(this);

        mProgress.setMessage("Carregando dados...");
        mProgress.setCanceledOnTouchOutside(false);
        mProgress.show();

        mDatabaseUsers.child(mAuth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                mNameField.setText((String) dataSnapshot.child("name").getValue());
                mFormationField.setText((String) dataSnapshot.child("formation").getValue());

                if (dataSnapshot.hasChild("image")) {
                    Picasso.with(SetupActivity.this).load((String) dataSnapshot.child("image").getValue()).into(mSetupImageBtn);
                }

                mProgress.dismiss();

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                mProgress.dismiss();
            }
        });

        mSetupImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                Intent galleryintent = new Intent(Intent.ACTION_GET_CONTENT);
//                galleryintent.setType("image/*");
//                startActivityForResult(galleryintent, GALLERY_REQUEST);

                Intent galeria = new Intent();
                galeria.setType("image/*");
                galeria.setAction(Intent.ACTION_GET_CONTENT);

                startActivityForResult(Intent.createChooser(galeria, "SELECT IMAGE"), GALLERY_REQUEST);

            }
        });

        mSubmitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startSetupAccount();

            }
        });

    }

    private void startSetupAccount() {

        final String nome = mNameField.getText().toString().trim();

        final String formacao = mFormationField.getText().toString().trim();

        final String user_id = mAuth.getCurrentUser().getUid();

        if (!TextUtils.isEmpty(nome) && !TextUtils.isEmpty(formacao)) {

            mProgress.setMessage("Atualizando perfil...");
            mProgress.setCanceledOnTouchOutside(false);
            mProgress.show();

            if (resultUri != null) {


                StorageReference filePath = mStorageImage.child(imageUri.getLastPathSegment());

                filePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        String downloadUrl = taskSnapshot.getDownloadUrl().toString();

                        HashMap setupMap = new HashMap();
                        setupMap.put("name", nome);
                        setupMap.put("formation", formacao);
                        setupMap.put("image", downloadUrl);
                        mProgress.dismiss();
                        atualizarPerfilDatabase(user_id, setupMap);

                    }
                });

            } else {

                HashMap setupMap = new HashMap();
                setupMap.put("name", nome);
                setupMap.put("formation", formacao);
                mProgress.dismiss();
                atualizarPerfilDatabase(user_id, setupMap);

            }

        } else {
            Toast.makeText(this, "Por favor, escolha uma imagem para o perfil e preencha os campos.", Toast.LENGTH_LONG).show();
        }

    }

    private void atualizarPerfilDatabase(String id, HashMap map) {

        mDatabaseUsers.child(id).updateChildren(map).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

                mProgress.show();

                Intent intent = new Intent(SetupActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);

                mProgress.dismiss();

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK) {
            imageUri = data.getData();
            CropImage.activity(imageUri).
                    setGuidelines(CropImageView.Guidelines.ON)
                    .setRequestedSize(500, 500).
                    setAspectRatio(1, 1).
                    start(this);

        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                resultUri = result.getUri();
                mSetupImageBtn.setImageURI(resultUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }

    }

}
